package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DmlInsertJobBuilder;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
@Primary
public class ReportingUploadServiceDmlImpl implements ReportingUploadService {

  private static class JobInfo {
    private String tableName;
    private int rowCount;
    private QueryJobConfiguration queryJobConfiguration;

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
  }

  private static final Logger logger =
      Logger.getLogger(ReportingUploadServiceDmlImpl.class.getName());
  private static final long MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis();

  private final BigQueryService bigQueryService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final DmlInsertJobBuilder<ReportingUser> userJobBuilder =
      UserColumnValueExtractor::values;

  private static final DmlInsertJobBuilder<ReportingCohort> cohortJobBuilder =
      CohortColumnValueExtractor::values;
  private static final DmlInsertJobBuilder<ReportingInstitution> institutionJobBuilder =
      InstitutionColumnValueExtractor::values;

  private static final DmlInsertJobBuilder<ReportingWorkspace> workspaceJobBuilder =
      WorkspaceColumnValueExtractor::values;

  public ReportingUploadServiceDmlImpl(
      BigQueryService bigQueryService,
      Provider<Stopwatch> stopwatchProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
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
  }

  private TableResult executeWithTimeout(QueryJobConfiguration job) {

    return bigQueryService.executeQuery(job, MAX_WAIT_TIME);
  }

  private List<JobInfo> getJobs(ReportingSnapshot reportingSnapshot) {
    final Stopwatch buildJobsStopwatch = stopwatchProvider.get().start();

    final QueryParameterValue snapshotTimestamp = getTimestampValue(reportingSnapshot);
    final ImmutableList.Builder<JobInfo> resultBuilder = new Builder<>();

    resultBuilder.addAll(
        getJobsForDTOs(
            CohortColumnValueExtractor.TABLE_NAME,
            cohortJobBuilder,
            snapshotTimestamp,
            reportingSnapshot.getCohorts()));
    resultBuilder.addAll(
        getJobsForDTOs(
            InstitutionColumnValueExtractor.TABLE_NAME,
            institutionJobBuilder,
            snapshotTimestamp,
            reportingSnapshot.getInstitutions()));
    resultBuilder.addAll(
        getJobsForDTOs(
            UserColumnValueExtractor.TABLE_NAME,
            userJobBuilder,
            snapshotTimestamp,
            reportingSnapshot.getUsers()));
    resultBuilder.addAll(
        getJobsForDTOs(
            WorkspaceColumnValueExtractor.TABLE_NAME,
            workspaceJobBuilder,
            snapshotTimestamp,
            reportingSnapshot.getWorkspaces()));

    final List<JobInfo> result = resultBuilder.build();
    buildJobsStopwatch.stop();
    logger.info(
        LogFormatters.duration(
            String.format("Built %d Jobs", result.size()), buildJobsStopwatch.elapsed()));
    return result;
  }

  private <T> List<JobInfo> getJobsForDTOs(
      String tableName,
      DmlInsertJobBuilder<T> jobBuilder,
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
