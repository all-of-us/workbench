package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestBuilder;
import org.pmiops.workbench.reporting.insertion.ResearcherParameter;
import org.pmiops.workbench.reporting.insertion.WorkspaceParameter;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
public class ReportingUploadServiceStreamingImpl implements ReportingUploadService {
  private static final Logger log =
      Logger.getLogger(ReportingUploadServiceStreamingImpl.class.getName());
  private static final InsertAllRequestBuilder<ReportingResearcher> researcherRequestBuilder =
      ResearcherParameter::values;
  private static final InsertAllRequestBuilder<ReportingWorkspace> workspaceRequestBuilder =
      WorkspaceParameter::values;

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> configProvider;

  public ReportingUploadServiceStreamingImpl(
      BigQueryService bigQueryService, Provider<WorkbenchConfig> configProvider) {
    this.bigQueryService = bigQueryService;
    this.configProvider = configProvider;
  }

  @Override
  public ReportingJobResult uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final ImmutableMap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMap.builder();
    for (InsertAllRequest request : getInsertAllRequests(reportingSnapshot)) {
      final InsertAllResponse currentResponse = bigQueryService.insertAll(request);
      responseMapBuilder.put(request.getTable(), currentResponse);
    }
    return computeOverallResult(responseMapBuilder.build());
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
            fixedValues));
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
