package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.ColumnValueExtractorUtils.getBigQueryTableName;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUploadDetails;
import org.pmiops.workbench.model.ReportingUploadResult;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.ColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetConceptSetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service
public class ReportingVerificationServiceImpl implements ReportingVerificationService {
  private static final Logger logger =
      Logger.getLogger(ReportingVerificationServiceImpl.class.getName());

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private Provider<Stopwatch> stopwatchProvider;

  public ReportingVerificationServiceImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<Stopwatch> stopwatchProvider) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  public ReportingUploadDetails getUploadDetails(ReportingSnapshot snapshot) {
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
                        snapshot, UserColumnValueExtractor.class, ReportingSnapshot::getUsers),
                    getUploadResult(
                        snapshot, CohortColumnValueExtractor.class, ReportingSnapshot::getCohorts),
                    getUploadResult(
                        snapshot,
                        InstitutionColumnValueExtractor.class,
                        ReportingSnapshot::getInstitutions),
                    getUploadResult(
                        snapshot,
                        DatasetColumnValueExtractor.class,
                        ReportingSnapshot::getDatasets),
                    getUploadResult(
                        snapshot,
                        DatasetCohortColumnValueExtractor.class,
                        ReportingSnapshot::getDatasetCohorts),
                    getUploadResult(
                        snapshot,
                        DatasetDomainColumnValueExtractor.class,
                        ReportingSnapshot::getDatasetDomainIdValues),
                    getUploadResult(
                        snapshot,
                        DatasetConceptSetColumnValueExtractor.class,
                        ReportingSnapshot::getDatasetConceptSets)));
    verifyStopwatch.stop();
    logger.info(LogFormatters.duration("Verification queries", verifyStopwatch.elapsed()));
    return result;
  }

  @Override
  public ReportingUploadDetails verifyAndLog(ReportingSnapshot reportingSnapshot) {
    // check each table. Note that for streaming inputs, not all rows may be immediately available.
    final ReportingUploadDetails uploadDetails = getUploadDetails(reportingSnapshot);
    final StringBuilder sb =
        new StringBuilder(
            String.format("Verifying Snapshot %d:\n", reportingSnapshot.getCaptureTimestamp()));
    sb.append("Table\tUploaded\tExpected\tDifference(%)\n");
    Level detailsLogLevel = Level.INFO;
    for (final ReportingUploadResult result : uploadDetails.getUploads()) {
      final long delta = result.getDestinationRowCount() - result.getSourceRowCount();
      final String relativeDelta;
      if (result.getSourceRowCount() == 0) {
        relativeDelta = "";
      } else {
        relativeDelta = String.format(" (%.3f%%)", 100.0 * delta / result.getSourceRowCount());
      }
      sb.append(
          String.format(
              "%s\t%d\t%d\t%d%s\n",
              result.getTableName(),
              result.getSourceRowCount(),
              result.getDestinationRowCount(),
              delta,
              relativeDelta));
      if (!result.getDestinationRowCount().equals(result.getSourceRowCount())) {
        detailsLogLevel = Level.WARNING;
      }
    }
    logger.log(detailsLogLevel, sb.toString());
    return uploadDetails;
  }

  public String getDataset() {
    return workbenchConfigProvider.get().reporting.dataset;
  }

  public String getProjectId() {
    return workbenchConfigProvider.get().server.projectId;
  }

  private ReportingUploadResult getUploadResult(
      String tableName, long snapshotTimestamp, long sourceRowCount) {
    return new ReportingUploadResult()
        .tableName(tableName)
        .sourceRowCount(sourceRowCount)
        .destinationRowCount(getActualRowCount(tableName, snapshotTimestamp));
  }

  private <T, E extends Enum<E> & ColumnValueExtractor<T>> ReportingUploadResult getUploadResult(
      ReportingSnapshot snapshot,
      Class<E> extractorClass,
      Function<ReportingSnapshot, List<T>> collectionExtractor) {
    return getUploadResult(
        getBigQueryTableName(extractorClass),
        snapshot.getCaptureTimestamp(),
        collectionExtractor.apply(snapshot).size());
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
