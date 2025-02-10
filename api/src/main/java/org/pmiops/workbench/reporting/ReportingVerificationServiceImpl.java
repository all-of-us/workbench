package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.Streams;
import jakarta.inject.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.stereotype.Service;

@Service
public class ReportingVerificationServiceImpl implements ReportingVerificationService {
  private static final Logger logger =
      Logger.getLogger(ReportingVerificationServiceImpl.class.getName());

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ReportingTableService reportingTableService;

  public ReportingVerificationServiceImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ReportingTableService reportingTableService) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.reportingTableService = reportingTableService;
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
                  long sourceCount = table.getRwbTableCountFn().getAsInt();
                  long destCount = getBigQueryRowCount(bqTableName, captureSnapshotTime);
                  return verifyCount(bqTableName, sourceCount, destCount, sb);
                });

    logger.log(verified ? Level.INFO : Level.WARNING, sb.toString());
    return verified;
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
