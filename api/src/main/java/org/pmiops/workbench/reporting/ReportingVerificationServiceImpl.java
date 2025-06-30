package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.Streams;
import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ReportingUploadVerificationDao;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.stereotype.Service;

@Service
public class ReportingVerificationServiceImpl implements ReportingVerificationService {
  private static final Logger logger =
      Logger.getLogger(ReportingVerificationServiceImpl.class.getName());

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ReportingTableService reportingTableService;
  private final ReportingUploadVerificationDao reportingUploadVerificationDao;

  public ReportingVerificationServiceImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ReportingTableService reportingTableService,
      ReportingUploadVerificationDao reportingUploadVerificationDao) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingTableService = reportingTableService;
    this.reportingUploadVerificationDao = reportingUploadVerificationDao;
  }

  @Override
  public boolean verifyBatchesAndLog(long captureSnapshotTime) {
    final StringBuilder sb =
        new StringBuilder(String.format("Verifying batches at %d:\n", captureSnapshotTime));

    sb.append("Table\tSource\tDestination\tDifference(%)\n");

    // fails-fast due to allMatch() so logs may be incomplete on failure
    boolean verified =
        reportingTableService.getAll().stream()
            .allMatch(
                table -> {
                  final String bqTableName = table.bqTableName();
                  long sourceCount = table.rwbTableCountFn().getAsInt();
                  long destCount = getBigQueryRowCount(bqTableName, captureSnapshotTime);
                  return verifyCount(bqTableName, sourceCount, destCount, sb);
                });

    logger.log(verified ? Level.INFO : Level.WARNING, sb.toString());
    return verified;
  }

  @Override
  public void verifyBatchesAndLog(List<String> tables, long captureSnapshotTime) {
    final StringBuilder sb =
        new StringBuilder(String.format("Verifying batches at %d:\n", captureSnapshotTime));

    sb.append("Table\tSource\tDestination\tDifference(%)\n");

    reportingTableService
        .getAll(tables)
        .forEach(
            table -> {
              final String bqTableName = table.bqTableName();
              long sourceCount = table.rwbTableCountFn().getAsInt();
              long destCount = getBigQueryRowCount(bqTableName, captureSnapshotTime);
              boolean uploadOutcome = verifyCount(bqTableName, sourceCount, destCount, sb);
              reportingUploadVerificationDao.updateUploadedStatus(
                  bqTableName, new Timestamp(captureSnapshotTime), uploadOutcome);
            });
    logger.log(Level.INFO, sb.toString());
  }

  @Override
  public boolean verifySnapshot(long captureSnapshotTime) {
    var tablesInSnapshot =
        reportingUploadVerificationDao.findBySnapshotTimestamp(new Timestamp(captureSnapshotTime));
    return !tablesInSnapshot.isEmpty()
        && // todo: may want to throw if no tables are found for a given snapshot
        tablesInSnapshot.stream().allMatch(record -> Boolean.TRUE.equals(record.getUploaded()));
  }

  /** Verifies source count equals to destination count. Returns {@code true} of match. */
  private static boolean verifyCount(
      String bqTableName, Long sourceCount, Long destinationCount, StringBuilder sb) {
    final long delta = destinationCount - sourceCount;
    final String relativeDelta;
    if (sourceCount == 0) {
      relativeDelta = "";
    } else {
      relativeDelta = String.format(" (%.3f%%)", 100.0 * delta / sourceCount);
    }
    sb.append(
        String.format(
            "%s\t%d\t%d\t%d%s\n",
            bqTableName, sourceCount, destinationCount, delta, relativeDelta));
    return destinationCount.equals(sourceCount);
  }

  public String getBigqueryDataset() {
    return workbenchConfigProvider.get().reporting.dataset;
  }

  public String getProjectId() {
    return workbenchConfigProvider.get().server.projectId;
  }

  private Long getBigQueryRowCount(String tableName, long snapshotTimestamp) {
    final QueryJobConfiguration queryJobConfiguration = buildJob(tableName, snapshotTimestamp);
    return Streams.stream(bigQueryService.executeQuery(queryJobConfiguration).getValues())
        .findFirst()
        .flatMap(fv -> FieldValues.getLong(fv, "actual_count"))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "Failed to obtain verification count for table %s at snapshot %d",
                        tableName, snapshotTimestamp)));
  }

  private QueryJobConfiguration buildJob(String tableName, long snapshotTimestamp) {
    return QueryJobConfiguration.newBuilder(buildQuery(tableName))
        .addNamedParameter("snapshot_timestamp", QueryParameterValue.int64(snapshotTimestamp))
        .build();
  }

  private String buildQuery(String tableName) {
    final String queryTemplate =
        "SELECT\n"
            + "  COUNT(*) AS actual_count\n"
            + "FROM\n"
            + "  `%s.%s.%s` \n"
            + "WHERE\n"
            + "  snapshot_timestamp =  @snapshot_timestamp;";
    return String.format(queryTemplate, getProjectId(), getBigqueryDataset(), tableName);
  }
}
