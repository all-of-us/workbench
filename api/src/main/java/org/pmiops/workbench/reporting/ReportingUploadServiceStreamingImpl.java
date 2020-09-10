package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestBuilder;
import org.pmiops.workbench.reporting.insertion.UserParameter;
import org.pmiops.workbench.reporting.insertion.WorkspaceParameter;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
public class ReportingUploadServiceStreamingImpl implements ReportingUploadService {
  private static final Logger log =
      Logger.getLogger(ReportingUploadServiceStreamingImpl.class.getName());
  private static final InsertAllRequestBuilder<BqDtoUser> researcherRequestBuilder =
      UserParameter::values;
  private static final InsertAllRequestBuilder<BqDtoWorkspace> workspaceRequestBuilder =
      WorkspaceParameter::values;

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<Stopwatch> stopwatchProvider;

  public ReportingUploadServiceStreamingImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> configProvider,
      Provider<Stopwatch> stopwatchProvider) {
    this.bigQueryService = bigQueryService;
    this.configProvider = configProvider;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  public ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final Stopwatch stopwatch = stopwatchProvider.get();
    final ImmutableMap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMap.builder();
    for (InsertAllRequest request : getInsertAllRequests(reportingSnapshot)) {
      stopwatch.start();
      final InsertAllResponse currentResponse = bigQueryService.insertAll(request);
      responseMapBuilder.put(request.getTable(), currentResponse);
      stopwatch.stop();
      log.info(
          LogFormatters.rate(
              String.format("Streaming upload into %s table", request.getTable().getTable()),
              stopwatch.elapsed(),
              request.getRows().size(),
              "rows"));
      stopwatch.reset();
    }
    return computeOverallResult(responseMapBuilder.build());
  }

  public void logDuration(Duration duration, String description) {
    final long millis = duration.toMillis();
    log.info(String.format("%s: %d.%d seconds", description, millis / 1000, millis % 1000));
  }

  private List<InsertAllRequest> getInsertAllRequests(ReportingSnapshot reportingSnapshot) {
    final String projectId = configProvider.get().server.projectId;
    final String dataset = configProvider.get().reporting.dataset;
    final Map<String, Object> fixedValues =
        ImmutableMap.of("snapshot_timestamp", reportingSnapshot.getCaptureTimestamp());

    return ImmutableList.of(
            researcherRequestBuilder.build(
                TableId.of(projectId, dataset, "researcher"),
                reportingSnapshot.getResearchers(),
                fixedValues),
            workspaceRequestBuilder.build(
                TableId.of(projectId, dataset, "workspace"),
                reportingSnapshot.getWorkspaces(),
                fixedValues))
        .stream()
        .filter(r -> r.getRows().size() > 0)
        .collect(ImmutableList.toImmutableList());
  }

  private ReportingJobResult computeOverallResult(
      Map<TableId, InsertAllResponse> tableIdToResponse) {
    final boolean anyErrors =
        tableIdToResponse.values().stream().anyMatch(InsertAllResponse::hasErrors);

    if (!anyErrors) {
      return ReportingJobResult.SUCCEEDED;
    } else {
      for (Map.Entry<TableId, InsertAllResponse> entry : tableIdToResponse.entrySet()) {
        final TableId tableId = entry.getKey();
        final InsertAllResponse response = entry.getValue();
        if (response.hasErrors()) {
          final Map<Long, List<BigQueryError>> rowIdToErrors = response.getInsertErrors();
          rowIdToErrors.forEach(
              (rowId, errors) -> {
                log.warning(
                    String.format(
                        "%d errors inserting row %d in table %s",
                        errors.size(), rowId, tableId.getTable()));
                errors.forEach(
                    error ->
                        log.warning(
                            String.format(
                                "  %s at %s: %s",
                                error.getReason(), error.getLocation(), error.getMessage())));
              });
        }
      }
    }
    return ReportingJobResult.PARTIAL_WRITE;
  }
}
