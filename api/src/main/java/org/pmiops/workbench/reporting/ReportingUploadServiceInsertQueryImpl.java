package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.ColumnValueExtractorUtils.getBigQueryTableName;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InsertQueryJobBuilder;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
@Primary
public class ReportingUploadServiceInsertQueryImpl implements ReportingUploadService {

  private static class JobInfo {
    private final String tableName;
    private final int rowCount;
    private final QueryJobConfiguration queryJobConfiguration;

    public JobInfo(String tableName, int rowCount, QueryJobConfiguration queryJobConfiguration) {
      this.tableName = tableName;
      this.rowCount = rowCount;
      this.queryJobConfiguration = queryJobConfiguration;
    }

    public String getTableName() {
      return tableName;
    }

    public int getRowCount() {
      return rowCount;
    }

    public QueryJobConfiguration getQueryJobConfiguration() {
      return queryJobConfiguration;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof JobInfo)) {
        return false;
      }
      JobInfo jobInfo = (JobInfo) o;
      return getRowCount() == jobInfo.getRowCount()
          && Objects.equals(getTableName(), jobInfo.getTableName())
          && Objects.equals(getQueryJobConfiguration(), jobInfo.getQueryJobConfiguration());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getTableName(), getRowCount(), getQueryJobConfiguration());
    }
  }

  private static final Logger logger =
      Logger.getLogger(ReportingUploadServiceInsertQueryImpl.class.getName());
  private static final long MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis();

  private final BigQueryService bigQueryService;
  private ReportingVerificationService reportingVerificationService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public ReportingUploadServiceInsertQueryImpl(
      BigQueryService bigQueryService,
      ReportingVerificationService reportingVerificationService,
      Provider<Stopwatch> stopwatchProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.reportingVerificationService = reportingVerificationService;
    this.stopwatchProvider = stopwatchProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final List<JobInfo> insertJobs = getJobs(reportingSnapshot);

    final Stopwatch uploadStopwatch = stopwatchProvider.get().start();
    final Stopwatch jobStopwatch = stopwatchProvider.get();
    for (JobInfo jobInfo : insertJobs) {
      jobStopwatch.start();
      executeWithTimeout(jobInfo.getQueryJobConfiguration());
      jobStopwatch.stop();

      logger.info(
          LogFormatters.rate(
              String.format("Insert into %s", jobInfo.tableName),
              jobStopwatch.elapsed(),
              jobInfo.rowCount,
              "Rows"));
      jobStopwatch.reset();
    }
    uploadStopwatch.stop();
    logger.info(
        LogFormatters.rate(
            "DML Insert", uploadStopwatch.elapsed(), insertJobs.size(), "Insert Queries"));

    // verify correct number of rows. I don't believe this should ever be off for this
    // implementation,
    // outside of an error condition that would have already thrown.
    reportingVerificationService.verifyAndLog(reportingSnapshot);
  }

  private TableResult executeWithTimeout(QueryJobConfiguration job) {

    return bigQueryService.executeQuery(job, MAX_WAIT_TIME);
  }

  private List<JobInfo> getJobs(ReportingSnapshot reportingSnapshot) {
    final Stopwatch buildJobsStopwatch = stopwatchProvider.get().start();

    final QueryParameterValue snapshotTimestamp = getTimestampValue(reportingSnapshot);
    final ImmutableList.Builder<JobInfo> resultBuilder = new Builder<>();

    final List<JobInfo> result =
        resultBuilder
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(CohortColumnValueExtractor.class),
                    CohortColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getCohorts()))
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(DatasetCohortColumnValueExtractor.class),
                    DatasetColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getDatasets()))
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(DatasetCohortColumnValueExtractor.class),
                    DatasetCohortColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getDatasetCohorts()))
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(InstitutionColumnValueExtractor.class),
                    InstitutionColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getInstitutions()))
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(UserColumnValueExtractor.class),
                    UserColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getUsers()))
            .addAll(
                getJobsForDTOs(
                    getBigQueryTableName(WorkspaceColumnValueExtractor.class),
                    WorkspaceColumnValueExtractor::values,
                    snapshotTimestamp,
                    reportingSnapshot.getWorkspaces()))
            .build();
    buildJobsStopwatch.stop();
    logger.info(
        LogFormatters.duration(
            String.format("Built %d Jobs", result.size()), buildJobsStopwatch.elapsed()));
    return result;
  }

  private <T> List<JobInfo> getJobsForDTOs(
      String tableName,
      InsertQueryJobBuilder<T> jobBuilder,
      QueryParameterValue snapshotTimestamp,
      List<T> dtos) {
    final int partitionSize = workbenchConfigProvider.get().reporting.maxRowsPerInsert;

    return Lists.partition(dtos, partitionSize).stream()
        .map(
            batch ->
                new JobInfo(
                    tableName,
                    batch.size(),
                    jobBuilder.build(qualifyTableName(tableName), batch, snapshotTimestamp)))
        .collect(ImmutableList.toImmutableList());
  }

  private QueryParameterValue getTimestampValue(ReportingSnapshot reportingSnapshot) {
    return QueryParameterValue.int64(reportingSnapshot.getCaptureTimestamp());
  }

  private String qualifyTableName(String tableName) {
    return String.format(
        "`%s.%s.%s`",
        workbenchConfigProvider.get().server.projectId,
        workbenchConfigProvider.get().reporting.dataset,
        tableName);
  }
}
