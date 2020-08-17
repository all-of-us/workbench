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
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.ReportingInsertionJobBuilder;
import org.pmiops.workbench.reporting.insertion.ResearcherParameter;
import org.pmiops.workbench.reporting.insertion.WorkspaceParameter;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ReportingUploadServiceImpl implements ReportingUploadService {
  private static final Logger logger = Logger.getLogger("ReportingUploadServiceInsertQueryImpl");
  private static final long MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis();

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final ReportingInsertionJobBuilder<ReportingResearcher> researcherJobBuilder =
      ResearcherParameter::values;
  private static final ReportingInsertionJobBuilder<ReportingWorkspace> workspaceJobBuilder =
      WorkspaceParameter::values;

  public ReportingUploadServiceImpl(
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

    // TODO: pipe more useful diagnostic info to the log
    insertResults.forEach(
        (job, result) -> logger.info(String.format("Inserted %d rows", result.getTotalRows())));

    return ReportingJobResult.SUCCEEDED;
  }

  private TableResult executeWithTimeout(QueryJobConfiguration job) {
    return bigQueryService.executeQuery(job, MAX_WAIT_TIME);
  }

  private List<QueryJobConfiguration> getJobs(ReportingSnapshot reportingSnapshot) {
    final QueryParameterValue snapshotTimestamp = getTimestampValue(reportingSnapshot);
    final ImmutableList.Builder<QueryJobConfiguration> resultBuilder = new Builder<>();
    final int partitionSize = workbenchConfigProvider.get().reporting.maxRowsPerInsert;

    Lists.partition(reportingSnapshot.getResearchers(), partitionSize).stream()
        .map(
            batch ->
                researcherJobBuilder.build(
                    qualifyTableName("researcher"), batch, snapshotTimestamp))
        .forEach(resultBuilder::add);
    Lists.partition(reportingSnapshot.getWorkspaces(), partitionSize).stream()
        .map(
            batch ->
                workspaceJobBuilder.build(qualifyTableName("workspace"), batch, snapshotTimestamp))
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
