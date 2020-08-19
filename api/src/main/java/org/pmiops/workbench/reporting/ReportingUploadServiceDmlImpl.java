package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.DmlInsertJobBuilder;
import org.pmiops.workbench.reporting.insertion.ResearcherParameter;
import org.pmiops.workbench.reporting.insertion.WorkspaceParameter;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
@Primary
public class ReportingUploadServiceDmlImpl implements ReportingUploadService {
  private static final Logger log = Logger.getLogger("ReportingUploadServiceInsertQueryImpl");
  private static final long MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis();
  private static final long MISSING_ID = -1L;

  private final BigQueryService bigQueryService;
  private final Provider<Stopwatch> stopwatchProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final DmlInsertJobBuilder<ReportingResearcher> researcherJobBuilder =
      ResearcherParameter::values;
  private static final DmlInsertJobBuilder<ReportingWorkspace> workspaceJobBuilder =
      WorkspaceParameter::values;

  private static class DmlInsertJobInfo {
    private String tableName;
    private long rowCount;
    private long minRowId;
    private long maxRowId;
    private QueryJobConfiguration queryJobConfiguration;

    public DmlInsertJobInfo(
        String tableName,
        long rowCount,
        long minRowId,
        long maxRowId,
        QueryJobConfiguration queryJobConfiguration) {
      this.tableName = tableName;
      this.rowCount = rowCount;
      this.minRowId = minRowId;
      this.maxRowId = maxRowId;
      this.queryJobConfiguration = queryJobConfiguration;
    }

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public long getRowCount() {
      return rowCount;
    }

    public void setRowCount(long rowCount) {
      this.rowCount = rowCount;
    }

    public long getMinRowId() {
      return minRowId;
    }

    public void setMinRowId(long minRowId) {
      this.minRowId = minRowId;
    }

    public long getMaxRowId() {
      return maxRowId;
    }

    public void setMaxRowId(long maxRowId) {
      this.maxRowId = maxRowId;
    }

    public QueryJobConfiguration getQueryJobConfiguration() {
      return queryJobConfiguration;
    }

    public void setQueryJobConfiguration(QueryJobConfiguration queryJobConfiguration) {
      this.queryJobConfiguration = queryJobConfiguration;
    }

    @Override
    public String toString() {
      return "DmlInsertJobInfo{"
          + "tableName='"
          + tableName
          + '\''
          + ", rowCount="
          + rowCount
          + ", minRowId="
          + minRowId
          + ", maxRowId="
          + maxRowId
          + ", queryJobConfiguration parameter count="
          + queryJobConfiguration.getNamedParameters().entrySet().size()
          + '}';
    }
  }

  public ReportingUploadServiceDmlImpl(
      BigQueryService bigQueryService,
      Provider<Stopwatch> stopwatchProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.stopwatchProvider = stopwatchProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final List<DmlInsertJobInfo> insertJobs = getInsertJobs(reportingSnapshot);
    final ImmutableMap.Builder<DmlInsertJobInfo, TableResult> jobToTableResultBuilder =
        ImmutableMap.builder();
    final Stopwatch stopwatch = stopwatchProvider.get();

    for (DmlInsertJobInfo jobInfo : insertJobs) {
      stopwatch.start();
      final TableResult tableResult = executeWithTimeout(jobInfo.getQueryJobConfiguration());
      stopwatch.stop();
      log.info(
          LogFormatters.rate(
              String.format(
                  "DML Insert %d rows into %s", jobInfo.getRowCount(), jobInfo.getTableName()),
              stopwatch.elapsed(),
              jobInfo.getRowCount(),
              "rows"));
      stopwatch.reset();
      jobToTableResultBuilder.put(jobInfo, tableResult);
    }

    // TODO(jaycarlton): consider including this information in the return value for diagnostics
    // and performancde tracking (though this would be tricky to keep synchronized with the
    // streaming implementation).
    final ImmutableMap<DmlInsertJobInfo, TableResult> jobToTableResult =
        jobToTableResultBuilder.build();

    jobToTableResult.forEach(
        (jobInfo, tableResult) -> {
          log.info(
              String.format(
                  "%s -> %d rows inserted", jobInfo.toString(), tableResult.getTotalRows()));
        });

    return ReportingJobResult.SUCCEEDED;
  }

  private TableResult executeWithTimeout(QueryJobConfiguration job) {
    return bigQueryService.executeQuery(job, MAX_WAIT_TIME);
  }

  private List<DmlInsertJobInfo> getInsertJobs(ReportingSnapshot reportingSnapshot) {
    final QueryParameterValue snapshotTimestamp = getTimestampValue(reportingSnapshot);
    final ImmutableList.Builder<DmlInsertJobInfo> resultBuilder = new Builder<>();
    final int partitionSize = workbenchConfigProvider.get().reporting.maxRowsPerInsert;

    Lists.partition(reportingSnapshot.getResearchers(), partitionSize).stream()
        .map(batch -> buildResearcherBatch(batch, snapshotTimestamp))
        .forEach(resultBuilder::add);

    Lists.partition(reportingSnapshot.getWorkspaces(), partitionSize).stream()
        .map(batch -> buildWorkspaceBatch(batch, snapshotTimestamp))
        .forEach(resultBuilder::add);

    return resultBuilder.build();
  }

  private <T> DmlInsertJobInfo buildBatchInsert(
      DmlInsertJobBuilder<T> jobBuilder,
      List<T> batch,
      QueryParameterValue snapshotTimestamp,
      String tableName,
      Function<T, Long> entityIdExtractor) {
    final QueryJobConfiguration queryJobConfiguration =
        jobBuilder.build(qualifyTableName(tableName), batch, snapshotTimestamp);
    final List<Long> ids =
        batch.stream().map(entityIdExtractor).collect(ImmutableList.toImmutableList());
    return new DmlInsertJobInfo(
        tableName,
        batch.size(),
        ids.stream().min(Comparator.comparingLong(Long::longValue)).orElse(MISSING_ID),
        ids.stream().max(Comparator.comparingLong(Long::longValue)).orElse(MISSING_ID),
        queryJobConfiguration);
  }

  private DmlInsertJobInfo buildResearcherBatch(
      List<ReportingResearcher> batch, QueryParameterValue snapshotTimestamp) {
    return buildBatchInsert(
        researcherJobBuilder,
        batch,
        snapshotTimestamp,
        "researcher",
        ReportingResearcher::getResearcherId);
  }

  private DmlInsertJobInfo buildWorkspaceBatch(
      List<ReportingWorkspace> batch, QueryParameterValue snapshotTimestamp) {
    return buildBatchInsert(
        workspaceJobBuilder,
        batch,
        snapshotTimestamp,
        "workspace",
        ReportingWorkspace::getWorkspaceId);
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
