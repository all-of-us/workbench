package org.pmiops.workbench.reporting;

import static org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer.generateInsertId;

import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.TableId;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingBase;
import org.pmiops.workbench.utils.LogFormatters;
import org.springframework.stereotype.Service;

@Service
public class ReportingUploadServiceImpl implements ReportingUploadService {
  private static final Logger log = Logger.getLogger(ReportingUploadServiceImpl.class.getName());

  /**
   * The verified–snapshot BigQuery name. It has no row other than the default snapshot_timestamp,
   * no need to create(also not compilable with the current model) ReportingSnapShot model with a
   * ColumnValueExtractor.
   */
  private static final String VERIFIED_SNAPSHOT_TABLE_NAME = "verified_snapshot";

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<Stopwatch> stopwatchProvider;

  public ReportingUploadServiceImpl(
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> configProvider,
      Provider<Stopwatch> stopwatchProvider) {
    this.bigQueryService = bigQueryService;
    this.configProvider = configProvider;
    this.stopwatchProvider = stopwatchProvider;
  }

  @Override
  public <T extends ReportingBase> void uploadBatch(
      ReportingTableParams<T> uploadBatchParams, List<T> batch, long captureTimestamp) {
    uploadBatchTable(
        uploadBatchParams
            .bqInsertionBuilder()
            .build(
                getTableId(uploadBatchParams.bqTableName()),
                batch,
                getFixedValues(captureTimestamp)));
    // This is a test to prove if these batched list are being cleaned up by garbage collection.
    batch = null;
  }

  /** Batch uploads a reporting table. */
  private void uploadBatchTable(InsertAllRequest insertAllRequest) {
    final Stopwatch stopwatch = stopwatchProvider.get();
    final ImmutableMultimap.Builder<TableId, InsertAllResponse> responseMapBuilder =
        ImmutableMultimap.builder();
    final StringBuilder performanceStringBuilder = new StringBuilder();
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
    final TableId tableId = getTableId(VERIFIED_SNAPSHOT_TABLE_NAME);
    final InsertAllRequest insertAllRequest =
        InsertAllRequest.newBuilder(tableId)
            .setIgnoreUnknownValues(false)
            .addRow(RowToInsert.of(generateInsertId(), getFixedValues(captureTimestamp)))
            .build();
    InsertAllResponse response = bigQueryService.insertAll(insertAllRequest);
    checkResponse(ImmutableMultimap.of(tableId, response));
    log.info(String.format("Verified snapshot at %d", captureTimestamp));
  }

  /** Issues one {@link BigQueryService#insertAll(InsertAllRequest)}, then logs the performance. */
  private void issueInsertAllRequest(
      Stopwatch stopwatch,
      ImmutableMultimap.Builder<TableId, InsertAllResponse> responseMapBuilder,
      StringBuilder performanceStringBuilder,
      InsertAllRequest request) {
    stopwatch.reset().start();
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

  private TableId getTableId(String tableName) {
    final String projectId = configProvider.get().server.projectId;
    final String dataset = configProvider.get().reporting.dataset;

    return TableId.of(projectId, dataset, tableName);
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
