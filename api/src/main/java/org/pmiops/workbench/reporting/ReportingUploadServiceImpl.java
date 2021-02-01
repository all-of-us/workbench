package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.ColumnValueExtractorUtils.getBigQueryTableName;
import static org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer.generateInsertId;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.model.ReportingWorkspaceFreeTierUsage;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.ColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetCohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetConceptSetColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.DatasetDomainColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer;
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceFreeTierUsageColumnValueExtractor;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service
public class ReportingUploadServiceImpl implements ReportingUploadService {
  private static final Logger log = Logger.getLogger(ReportingUploadServiceImpl.class.getName());
  private static final InsertAllRequestPayloadTransformer<ReportingCohort> cohortRequestBuilder =
      CohortColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingDatasetCohort>
      datasetCohortRequestBuilder = DatasetCohortColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingDatasetConceptSet>
      datasetConceptSetRequestBuilder = DatasetConceptSetColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingDatasetDomainIdValue>
      datasetDomainIIdValueRequestBuilder = DatasetDomainColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingInstitution>
      institutionRequestBuilder = InstitutionColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingUser> userRequestBuilder =
      UserColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingWorkspace>
      workspaceRequestBuilder = WorkspaceColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingWorkspaceFreeTierUsage>
      workspaceFreeTierUsageRequestBuilder = WorkspaceFreeTierUsageColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingDataset> datasetRequestBuilder =
      DatasetColumnValueExtractor::values;

  /**
   * The verified–snapshot BigQuery name. It has no row other than the default snapshot_timestamp,
   * no need to create(also not compilable with the current model) ReportingSnapShot model with a
   * ColumnValueExtractor.
   */
  private static final String VERIFIED_SNAPSHOT_TABLE_NAME = "verified_snapshot";

  private final BigQueryService bigQueryService;
  private final ReportingVerificationService reportingVerificationService;
  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<Stopwatch> stopwatchProvider;

  public ReportingUploadServiceImpl(
      BigQueryService bigQueryService,
      ReportingVerificationService reportingVerificationService,
      Provider<WorkbenchConfig> configProvider,
      Provider<Stopwatch> stopwatchProvider) {
    this.bigQueryService = bigQueryService;
    this.reportingVerificationService = reportingVerificationService;
    this.configProvider = configProvider;
    this.stopwatchProvider = stopwatchProvider;
  }

  /**
   * @deprecated Retrieve data using stream approach then upload as batch.
   *     <p>Uploads {@link ReportingSnapshot} to bigquery and log&verify the performance. Returns
   *     {@code true} if upload success and snapshot count is verified.
   */
  @Deprecated
  @Override
  public boolean uploadSnapshot(ReportingSnapshot reportingSnapshot) {
    final Stopwatch stopwatch = stopwatchProvider.get();
    final ImmutableMultimap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMultimap.builder();
    final List<InsertAllRequest> insertAllRequests = getInsertAllRequests(reportingSnapshot);
    final StringBuilder performanceStringBuilder = new StringBuilder();
    for (InsertAllRequest request : insertAllRequests) {
      issueInsertAllRequest(stopwatch, responseMapBuilder, performanceStringBuilder, request);
    }
    log.info(performanceStringBuilder.toString());
    return checkResponseAndRowCounts(reportingSnapshot, responseMapBuilder.build());
  }

  /** Batch uploads {@link ReportingWorkspace}. */
  @Override
  public void uploadBatchWorkspace(List<ReportingWorkspace> batch, long captureTimestamp) {
    final Stopwatch stopwatch = stopwatchProvider.get();
    final ImmutableMultimap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMultimap.builder();
    final StringBuilder performanceStringBuilder = new StringBuilder();
    final InsertAllRequest insertAllRequest =
        workspaceRequestBuilder.build(
            getTableId(WorkspaceColumnValueExtractor.class),
            batch,
            getFixedValues(captureTimestamp));
    issueInsertAllRequest(
        stopwatch, responseMapBuilder, performanceStringBuilder, insertAllRequest);
    log.info(performanceStringBuilder.toString());
    // Check response and abort the process if any error happens. In this case, verify_snapshot
    // won't have the 'successful' record, hence we know that is a "bad" dataset.
    checkResponse(responseMapBuilder.build());
  }

  /** Uploads a record into verified_snapshot BigQuery table. */
  @Override
  public void uploadVerifiedSnapshot(long captureTimestamp) {
    final InsertAllRequest insertAllRequest =
        InsertAllRequest.newBuilder(getTableId(VERIFIED_SNAPSHOT_TABLE_NAME))
            .setIgnoreUnknownValues(false)
            .addRow(RowToInsert.of(generateInsertId(), getFixedValues(captureTimestamp)))
            .build();
    bigQueryService.insertAll(insertAllRequest);
    // No need to check response, either it success, we get the record, or it fails, no record
    // presents.
    log.info(String.format("Verify snapshot at %d", captureTimestamp));
  }

  /** Issues one {@link BigQueryService#insertAll(InsertAllRequest)}, then logs the performance. */
  private void issueInsertAllRequest(
      Stopwatch stopwatch,
      ImmutableMultimap.Builder<TableId, InsertAllResponse> responseMapBuilder,
      StringBuilder performanceStringBuilder,
      InsertAllRequest request) {
    stopwatch.start();
    final InsertAllResponse currentResponse = bigQueryService.insertAll(request);
    stopwatch.stop();
    responseMapBuilder.put(request.getTable(), currentResponse);
    performanceStringBuilder
        .append(
            LogFormatters.rate(
                String.format("Streaming upload into %s table", request.getTable().getTable()),
                stopwatch.elapsed(),
                request.getRows().size(),
                "rows"))
        .append("\n");
    stopwatch.reset();
  }

  private boolean checkResponseAndRowCounts(
      ReportingSnapshot reportingSnapshot,
      ImmutableMultimap<TableId, InsertAllResponse> responseMap) {
    checkResponse(responseMap);

    return reportingVerificationService.verifyAndLog(reportingSnapshot);
  }

  private <E extends Enum<E> & ColumnValueExtractor<?>> TableId getTableId(
      Class<E> columnValueExtractorClass) {
    final String projectId = configProvider.get().server.projectId;
    final String dataset = configProvider.get().reporting.dataset;

    return TableId.of(projectId, dataset, getBigQueryTableName(columnValueExtractorClass));
  }

  private <E extends Enum<E> & ColumnValueExtractor<?>> TableId getTableId(String tableName) {
    final String projectId = configProvider.get().server.projectId;
    final String dataset = configProvider.get().reporting.dataset;

    return TableId.of(projectId, dataset, tableName);
  }

  private List<InsertAllRequest> getInsertAllRequests(ReportingSnapshot reportingSnapshot) {
    final Map<String, Object> fixedValues = getFixedValues(reportingSnapshot.getCaptureTimestamp());
    final int batchSize = configProvider.get().reporting.maxRowsPerInsert;
    final ImmutableList.Builder<InsertAllRequest> resultBuilder = ImmutableList.builder();

    resultBuilder.addAll(
        cohortRequestBuilder.buildBatchedRequests(
            getTableId(CohortColumnValueExtractor.class),
            reportingSnapshot.getCohorts(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        datasetRequestBuilder.buildBatchedRequests(
            getTableId(DatasetColumnValueExtractor.class),
            reportingSnapshot.getDatasets(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        datasetCohortRequestBuilder.buildBatchedRequests(
            getTableId(DatasetCohortColumnValueExtractor.class),
            reportingSnapshot.getDatasetCohorts(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        datasetConceptSetRequestBuilder.buildBatchedRequests(
            getTableId(DatasetConceptSetColumnValueExtractor.class),
            reportingSnapshot.getDatasetConceptSets(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        datasetDomainIIdValueRequestBuilder.buildBatchedRequests(
            getTableId(DatasetDomainColumnValueExtractor.class),
            reportingSnapshot.getDatasetDomainIdValues(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        institutionRequestBuilder.buildBatchedRequests(
            getTableId(InstitutionColumnValueExtractor.class),
            reportingSnapshot.getInstitutions(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        userRequestBuilder.buildBatchedRequests(
            getTableId(UserColumnValueExtractor.class),
            reportingSnapshot.getUsers(),
            fixedValues,
            batchSize));
    resultBuilder.addAll(
        workspaceFreeTierUsageRequestBuilder.buildBatchedRequests(
            getTableId(WorkspaceFreeTierUsageColumnValueExtractor.class),
            reportingSnapshot.getWorkspaceFreeTierUsage(),
            fixedValues,
            batchSize));
    return resultBuilder.build().stream()
        .filter(r -> r.getRows().size() > 0)
        .collect(ImmutableList.toImmutableList());
  }

  private Map<String, Object> getFixedValues(long snapshotTimestamp) {
    return ImmutableMap.of("snapshot_timestamp", snapshotTimestamp);
  }

  private void checkResponse(Multimap<TableId, InsertAllResponse> tableIdToResponses) {
    final boolean anyErrors =
        tableIdToResponses.values().stream().anyMatch(InsertAllResponse::hasErrors);

    if (anyErrors) {
      for (Map.Entry<TableId, InsertAllResponse> entry : tableIdToResponses.entries()) {
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
              tableIdToResponses.keySet().size()));
    }
  }
}
