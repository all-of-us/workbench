package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

public enum CohortColumnValueExtractor implements ColumnValueExtractor<ReportingCohort> {
  COHORT_ID("cohort_id", ReportingCohort::getCohortId, c -> int64(c.getCohortId())),
  CREATION_TIME(
      "creation_time",
      c -> toInsertRowString(c.getCreationTime()),
      c -> toTimestampQpv(c.getCreationTime())),
  CREATOR_ID("creator_id", ReportingCohort::getCreatorId, c -> int64(c.getCreatorId())),
  CRITERIA("criteria", ReportingCohort::getCriteria, c -> string(c.getCriteria())),
  DESCRIPTION("description", ReportingCohort::getDescription, c -> string(c.getDescription())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      c -> toInsertRowString(c.getLastModifiedTime()),
      c -> toTimestampQpv(c.getLastModifiedTime())),
  NAME("name", ReportingCohort::getName, c -> string(c.getName())),
  WORKSPACE_ID("workspace_id", ReportingCohort::getWorkspaceId, c -> int64(c.getWorkspaceId()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  public static final String TABLE_NAME = "cohort";
  private final String parameterName;
  private final Function<ReportingCohort, Object> objectValueFunction;
  private final Function<ReportingCohort, QueryParameterValue> parameterValueFunction;

  CohortColumnValueExtractor(
      String parameterName,
      Function<ReportingCohort, Object> objectValueFunction,
      Function<ReportingCohort, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingCohort, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingCohort, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }

  @Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
  @Primary
  public static class ReportingUploadServiceDmlImpl implements ReportingUploadService {

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
              TABLE_NAME,
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
      return int64(reportingSnapshot.getCaptureTimestamp());
    }

    private String qualifyTableName(String tableName) {
      return String.format(
          "`%s.%s.%s`",
          workbenchConfigProvider.get().server.projectId,
          workbenchConfigProvider.get().reporting.dataset,
          tableName);
    }
  }
}
