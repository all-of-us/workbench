package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.DmlInsertJobBuilder;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
@Primary
public class ReportingUploadServiceDmlImpl implements ReportingUploadService {

  private static final Logger logger = Logger.getLogger("ReportingUploadServiceInsertQueryImpl");
  private static final long MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis();

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final DmlInsertJobBuilder<ReportingUser> userJobBuilder =
      UserColumnValueExtractor::values;
  private static final DmlInsertJobBuilder<ReportingWorkspace> workspaceJobBuilder =
      WorkspaceColumnValueExtractor::values;

  public ReportingUploadServiceDmlImpl(
      BigQueryService bigQueryService, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final List<QueryJobConfiguration> insertJobs = getJobs(reportingSnapshot);
    final Map<QueryJobConfiguration, TableResult> insertResults =
        insertJobs.stream()
            .collect(ImmutableMap.toImmutableMap(Function.identity(), this::executeWithTimeout));

    // TODO: pipe useful diagnostic info to the log. Apparently the total rows and destination table
    //   aren't populated in these structures in this usage.
    return ReportingJobResult.SUCCEEDED;
  }

  private TableResult executeWithTimeout(QueryJobConfiguration job) {

    return bigQueryService.executeQuery(job, MAX_WAIT_TIME);
  }

  private List<QueryJobConfiguration> getJobs(ReportingSnapshot reportingSnapshot) {
    final QueryParameterValue snapshotTimestamp = getTimestampValue(reportingSnapshot);
    final ImmutableList.Builder<QueryJobConfiguration> resultBuilder = new Builder<>();
    final int partitionSize = workbenchConfigProvider.get().reporting.maxRowsPerInsert;

    Lists.partition(reportingSnapshot.getUsers(), partitionSize).stream()
        .map(
            batch ->
                userJobBuilder.build(
                    qualifyTableName(UserColumnValueExtractor.TABLE_NAME),
                    batch,
                    snapshotTimestamp))
        .forEach(resultBuilder::add);

    Lists.partition(reportingSnapshot.getWorkspaces(), partitionSize).stream()
        .map(
            batch ->
                workspaceJobBuilder.build(
                    qualifyTableName(WorkspaceColumnValueExtractor.TABLE_NAME),
                    batch,
                    snapshotTimestamp))
        .forEach(resultBuilder::add);
    return resultBuilder.build();
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
