package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUploadDetails;
import org.pmiops.workbench.model.ReportingUploadResult;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetConceptSetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.NewUserSatisfactionSurveyColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserGeneralDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserPartnerDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceFreeTierUsageColumnValueExtractor;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service
public class ReportingVerificationServiceImpl implements ReportingVerificationService {
  private static final Logger logger =
      Logger.getLogger(ReportingVerificationServiceImpl.class.getName());

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<Stopwatch> stopwatchProvider;
  private final ReportingQueryService reportingQueryService;

  public ReportingVerificationServiceImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<Stopwatch> stopwatchProvider,
      ReportingQueryService reportingQueryService) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.stopwatchProvider = stopwatchProvider;
    this.reportingQueryService = reportingQueryService;
  }

  @Override
  public boolean verifyAndLog(ReportingSnapshot reportingSnapshot) {
    // check each table. Note that for streaming inputs, not all rows may be immediately available.
    final ReportingUploadDetails uploadDetails = getUploadDetails(reportingSnapshot);
    final StringBuilder sb =
        new StringBuilder(
            String.format("Verifying Snapshot %d:\n", reportingSnapshot.getCaptureTimestamp()));

    sb.append("Table\tSource\tDestination\tDifference(%)\n");

    // fails-fast due to allMatch() so logs may be incomplete on failure
    boolean verified =
        uploadDetails.getUploads().stream()
            .allMatch(
                result ->
                    verifyCount(
                        result.getTableName(),
                        result.getSourceRowCount(),
                        result.getDestinationRowCount(),
                        sb));

    logger.log(verified ? Level.INFO : Level.WARNING, sb.toString());
    return verified;
  }

  @Override
  public boolean verifyBatchesAndLog(Set<String> batchTables, long captureSnapshotTime) {
    final StringBuilder sb =
        new StringBuilder(String.format("Verifying batches at %d:\n", captureSnapshotTime));

    sb.append("Table\tSource\tDestination\tDifference(%)\n");

    // alt: this could be a Map, but we don't need to reference it in that way
    List<Map.Entry<String, Integer>> tableCounters =
        List.of(
            Map.entry(
                WorkspaceColumnValueExtractor.TABLE_NAME,
                reportingQueryService.getTableRowCount(WorkspaceColumnValueExtractor.TABLE_NAME)),
            Map.entry(UserColumnValueExtractor.TABLE_NAME, reportingQueryService.getTableRowCount(NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME)),
            Map.entry(CohortColumnValueExtractor.TABLE_NAME, reportingQueryService.getTableRowCount(NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME)),
            Map.entry(
                NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME,
                reportingQueryService.getTableRowCount(NewUserSatisfactionSurveyColumnValueExtractor.TABLE_NAME)),
            Map.entry(
                UserGeneralDiscoverySourceColumnValueExtractor.TABLE_NAME,
                reportingQueryService.getTableRowCount(UserGeneralDiscoverySourceColumnValueExtractor.TABLE_NAME)),
            Map.entry(
                UserPartnerDiscoverySourceColumnValueExtractor.TABLE_NAME,
                reportingQueryService.getTableRowCount(UserPartnerDiscoverySourceColumnValueExtractor.TABLE_NAME)));

    // fails-fast due to allMatch() so logs may be incomplete on failure
    boolean verified =
        tableCounters.stream()
            .allMatch(
                entry -> {
                  final String tableName = entry.getKey();
                  long sourceCount = entry.getValue();
                  long actualCount = getActualRowCount(tableName, captureSnapshotTime);
                  return verifyCount(tableName, sourceCount, actualCount, sb);
                });

    logger.log(verified ? Level.INFO : Level.WARNING, sb.toString());
    return verified;
  }

  private ReportingUploadDetails getUploadDetails(ReportingSnapshot snapshot) {
    final Stopwatch verifyStopwatch = stopwatchProvider.get();
    verifyStopwatch.start();
    final ReportingUploadDetails result =
        new ReportingUploadDetails()
            .snapshotTimestamp(snapshot.getCaptureTimestamp())
            .projectId(getProjectId())
            .dataset(getDataset())
            .uploads(
                ImmutableList.of(
                    getUploadResult(
                        snapshot,
                        WorkspaceFreeTierUsageColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getWorkspaceFreeTierUsage),
                    getUploadResult(
                        snapshot,
                        InstitutionColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getInstitutions),
                    getUploadResult(
                        snapshot,
                        DatasetColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getDatasets),
                    getUploadResult(
                        snapshot,
                        DatasetCohortColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getDatasetCohorts),
                    getUploadResult(
                        snapshot,
                        DatasetDomainColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getDatasetDomainIdValues),
                    getUploadResult(
                        snapshot,
                        DatasetConceptSetColumnValueExtractor.TABLE_NAME,
                        ReportingSnapshot::getDatasetConceptSets)));
    verifyStopwatch.stop();
    logger.info(LogFormatters.duration("Verification queries", verifyStopwatch.elapsed()));
    return result;
  }

  /** Verifies source count equals to destination count. Returns {@code true} of match. */
  private static boolean verifyCount(
      String tableName, Long sourceCount, Long destinationCount, StringBuilder sb) {
    final long delta = destinationCount - sourceCount;
    final String relativeDelta;
    if (sourceCount == 0) {
      relativeDelta = "";
    } else {
      relativeDelta = String.format(" (%.3f%%)", 100.0 * delta / sourceCount);
    }
    sb.append(
        String.format(
            "%s\t%d\t%d\t%d%s\n", tableName, sourceCount, destinationCount, delta, relativeDelta));
    return destinationCount.equals(sourceCount);
  }

  public String getDataset() {
    return workbenchConfigProvider.get().reporting.dataset;
  }

  public String getProjectId() {
    return workbenchConfigProvider.get().server.projectId;
  }

  private <T> ReportingUploadResult getUploadResult(
      ReportingSnapshot snapshot,
      String tableName,
      Function<ReportingSnapshot, List<T>> collectionExtractor) {

    return new ReportingUploadResult()
        .tableName(tableName)
        .sourceRowCount((long) collectionExtractor.apply(snapshot).size())
        .destinationRowCount(getActualRowCount(tableName, snapshot.getCaptureTimestamp()));
  }

  private Long getActualRowCount(String tableName, long snapshotTimestamp) {
    final QueryJobConfiguration queryJobConfiguration = buildJob(tableName, snapshotTimestamp);
    return StreamSupport.stream(
            bigQueryService.executeQuery(queryJobConfiguration).getValues().spliterator(), false)
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
    return String.format(queryTemplate, getProjectId(), getDataset(), tableName);
  }
}
