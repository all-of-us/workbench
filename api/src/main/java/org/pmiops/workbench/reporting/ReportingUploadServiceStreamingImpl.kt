package org.pmiops.workbench.reporting

import com.google.cloud.bigquery.BigQueryError
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.TableId
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.model.ReportingSnapshot
import org.pmiops.workbench.reporting.ReportingUploadServiceStreamingImpl
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor
import org.pmiops.workbench.utils.LogFormatters
import org.springframework.stereotype.Service
import java.util.function.Consumer
import java.util.logging.Logger
import javax.inject.Provider

@Service("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
class ReportingUploadServiceStreamingImpl(
        private val bigQueryService: BigQueryService,
        private val configProvider: Provider<WorkbenchConfig>,
        private val stopwatchProvider: Provider<Stopwatch>) : ReportingUploadService {
    override fun uploadSnapshot(reportingSnapshot: ReportingSnapshot) {
        val stopwatch = stopwatchProvider.get()
        val responseMapBuilder = ImmutableMap.builder<TableId, InsertAllResponse>()
        val insertAllRequests = getInsertAllRequests(reportingSnapshot)
        for (request in insertAllRequests) {
            stopwatch.start()
            val currentResponse = bigQueryService.insertAll(request)
            responseMapBuilder.put(request.table, currentResponse)
            stopwatch.stop()
            log.info(
                    LogFormatters.rate(String.format("Streaming upload into %s table", request.table.table),
                            stopwatch.elapsed(),
                            request.rows.size.toDouble(),
                            "rows"))
            stopwatch.reset()
        }
        checkResults(responseMapBuilder.build())
    }

    private fun getTableId(tableName: String): TableId {
        val projectId = configProvider.get().server.projectId
        val dataset = configProvider.get().reporting.dataset
        return TableId.of(projectId, dataset, tableName)
    }

    private fun getInsertAllRequests(reportingSnapshot: ReportingSnapshot): List<InsertAllRequest> {
        val fixedValues: Map<String, Any> = ImmutableMap.of<String, Any>("snapshot_timestamp", reportingSnapshot.captureTimestamp)
        return ImmutableList.of(
                cohortRequestBuilder.build(
                        getTableId(CohortColumnValueExtractor.TABLE_NAME),
                        reportingSnapshot.cohorts,
                        fixedValues),
                institutionRequestBuilder.build(
                        getTableId(InstitutionColumnValueExtractor.TABLE_NAME),
                        reportingSnapshot.institutions,
                        fixedValues),
                userRequestBuilder.build(
                        getTableId(UserColumnValueExtractor.TABLE_NAME),
                        reportingSnapshot.users,
                        fixedValues),
                workspaceRequestBuilder.build(
                        getTableId(WorkspaceColumnValueExtractor.TABLE_NAME),
                        reportingSnapshot.workspaces,
                        fixedValues))
                .stream()
                .filter { r: InsertAllRequest -> r.rows.size > 0 }
                .collect(ImmutableList.toImmutableList())
    }

    private fun checkResults(tableIdToResponse: Map<TableId, InsertAllResponse>) {
        val anyErrors = tableIdToResponse.values.stream().anyMatch { obj: InsertAllResponse -> obj.hasErrors() }
        if (anyErrors) {
            for ((tableId, response) in tableIdToResponse) {
                if (response.hasErrors()) {
                    val rowIdToErrors = response.insertErrors
                    rowIdToErrors.forEach { (rowId: Long?, errors: List<BigQueryError>) ->
                        log.severe(String.format(
                                "%d errors inserting row %d in table %s",
                                errors.size, rowId, tableId.table))
                        errors.forEach(
                                Consumer { error: BigQueryError ->
                                    log.severe(String.format(
                                            "  %s at %s: %s",
                                            error.reason, error.location, error.message))
                                })
                    }
                }
            }
            throw RuntimeException(String.format(
                    "Encountered errors in %d tables in upload to BigQuery.",
                    tableIdToResponse.keys.size))
        }
    }

    companion object {
        private val cohortRequestBuilder = InsertAllRequestPayloadTransformer { CohortColumnValueExtractor.values() }
        private val institutionRequestBuilder = InsertAllRequestPayloadTransformer { InstitutionColumnValueExtractor.values() }
        private val log = Logger.getLogger(ReportingUploadServiceStreamingImpl::class.java.name)
        private val userRequestBuilder = InsertAllRequestPayloadTransformer { UserColumnValueExtractor.values() }
        private val workspaceRequestBuilder = InsertAllRequestPayloadTransformer { WorkspaceColumnValueExtractor.values() }
    }
}
