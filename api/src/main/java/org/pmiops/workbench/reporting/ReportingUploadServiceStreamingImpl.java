package org.pmiops.workbench.reporting;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
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
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
public class ReportingUploadServiceStreamingImpl implements ReportingUploadService {
  private static final InsertAllRequestPayloadTransformer<ReportingCohort> cohortRequestBuilder =
      CohortColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingInstitution>
      institutionRequestBuilder = InstitutionColumnValueExtractor::values;

  private static final Logger log =
      Logger.getLogger(ReportingUploadServiceStreamingImpl.class.getName());
  private static final InsertAllRequestPayloadTransformer<ReportingUser> userRequestBuilder =
      UserColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingWorkspace>
      workspaceRequestBuilder = WorkspaceColumnValueExtractor::values;

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
  public void uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final Stopwatch stopwatch = stopwatchProvider.get();
    final ImmutableMap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMap.builder();
    final List<InsertAllRequest> insertAllRequests = getInsertAllRequests(reportingSnapshot);
    for (InsertAllRequest request : insertAllRequests) {
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
    checkResults(responseMapBuilder.build());
  }

  private TableId getTableId(String tableName) {
    final String projectId = configProvider.get().server.projectId;
    final String dataset = configProvider.get().reporting.dataset;
    return TableId.of(projectId, dataset, tableName);
  }

  private List<InsertAllRequest> getInsertAllRequests(ReportingSnapshot reportingSnapshot) {
    final Map<String, Object> fixedValues =
        ImmutableMap.of("snapshot_timestamp", reportingSnapshot.getCaptureTimestamp());

    return ImmutableList.of(
            cohortRequestBuilder.build(
                getTableId(CohortColumnValueExtractor.TABLE_NAME),
                reportingSnapshot.getCohorts(),
                fixedValues),
            institutionRequestBuilder.build(
                getTableId(InstitutionColumnValueExtractor.TABLE_NAME),
                reportingSnapshot.getInstitutions(),
                fixedValues),
            userRequestBuilder.build(
                getTableId(UserColumnValueExtractor.TABLE_NAME),
                reportingSnapshot.getUsers(),
                fixedValues),
            workspaceRequestBuilder.build(
                getTableId(WorkspaceColumnValueExtractor.TABLE_NAME),
                reportingSnapshot.getWorkspaces(),
                fixedValues))
        .stream()
        .filter(r -> r.getRows().size() > 0)
        .collect(ImmutableList.toImmutableList());
  }

  private void checkResults(Map<TableId, InsertAllResponse> tableIdToResponse) {
    final boolean anyErrors =
        tableIdToResponse.values().stream().anyMatch(InsertAllResponse::hasErrors);

    if (anyErrors) {
      for (Map.Entry<TableId, InsertAllResponse> entry : tableIdToResponse.entrySet()) {
        final TableId tableId = entry.getKey();
        final InsertAllResponse response = entry.getValue();
        if (response.hasErrors()) {
          final Map<Long, List<BigQueryError>> rowIdToErrors = response.getInsertErrors();
          rowIdToErrors.forEach(
              (rowId, errors) -> {
                log.severe(
                    String.format(
                        "%d errors inserting row %d in table %s",
                        errors.size(), rowId, tableId.getTable()));
                errors.forEach(
                    error ->
                        log.severe(
                            String.format(
                                "  %s at %s: %s",
                                error.getReason(), error.getLocation(), error.getMessage())));
              });
        }
      }
      throw new RuntimeException(
          String.format(
              "Encountered errors in %d tables in upload to BigQuery.",
              tableIdToResponse.keySet().size()));
    }
  }
}
