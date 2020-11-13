package org.pmiops.workbench.reporting

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.TableResult
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.model.ReportingSnapshot
import org.pmiops.workbench.reporting.ReportingUploadServiceDmlImpl
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.DmlInsertJobBuilder
import org.pmiops.workbench.reporting.insertion.InstitutionColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor
import org.pmiops.workbench.utils.LogFormatters
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.logging.Logger
import javax.inject.Provider

@Service("REPORTING_UPLOAD_SERVICE_DML_IMPL")
@Primary
class ReportingUploadServiceDmlImpl(
        private val bigQueryService: BigQueryService,
        private val stopwatchProvider: Provider<Stopwatch>,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>) : ReportingUploadService {
    private data class JobInfo(val tableName: String, val rowCount: Int, val queryJobConfiguration: QueryJobConfiguration)

    override fun uploadSnapshot(reportingSnapshot: ReportingSnapshot) {
        val insertJobs = getJobs(reportingSnapshot)
        val uploadStopwatch = stopwatchProvider.get().start()
        val jobStopwatch = stopwatchProvider.get()
        for (jobInfo in insertJobs) {
            jobStopwatch.start()
            executeWithTimeout(jobInfo.queryJobConfiguration)
            jobStopwatch.stop()
            logger.info(
                    LogFormatters.rate(String.format("Insert into %s", jobInfo.tableName),
                            jobStopwatch.elapsed(),
                            jobInfo.rowCount.toDouble(),
                            "Rows"))
            jobStopwatch.reset()
        }
        uploadStopwatch.stop()
        logger.info(
                LogFormatters.rate(
                        "DML Insert", uploadStopwatch.elapsed(), insertJobs.size.toDouble(), "Insert Queries"))
    }

    private fun executeWithTimeout(job: QueryJobConfiguration): TableResult {
        return bigQueryService.executeQuery(job, MAX_WAIT_TIME)
    }

    private fun getJobs(reportingSnapshot: ReportingSnapshot): List<JobInfo> {
        val buildJobsStopwatch = stopwatchProvider.get().start()
        val snapshotTimestamp = getTimestampValue(reportingSnapshot)
        val resultBuilder = ImmutableList.Builder<JobInfo>()
        resultBuilder.addAll(
                getJobsForDTOs(
                        CohortColumnValueExtractor.TABLE_NAME,
                        cohortJobBuilder,
                        snapshotTimestamp,
                        reportingSnapshot.cohorts))
        resultBuilder.addAll(
                getJobsForDTOs(
                        InstitutionColumnValueExtractor.TABLE_NAME,
                        institutionJobBuilder,
                        snapshotTimestamp,
                        reportingSnapshot.institutions))
        resultBuilder.addAll(
                getJobsForDTOs(
                        UserColumnValueExtractor.TABLE_NAME,
                        userJobBuilder,
                        snapshotTimestamp,
                        reportingSnapshot.users))
        resultBuilder.addAll(
                getJobsForDTOs(
                        WorkspaceColumnValueExtractor.TABLE_NAME,
                        workspaceJobBuilder,
                        snapshotTimestamp,
                        reportingSnapshot.workspaces))
        val result: List<JobInfo> = resultBuilder.build()
        buildJobsStopwatch.stop()
        logger.info(
                LogFormatters.duration(String.format("Built %d Jobs", result.size), buildJobsStopwatch.elapsed()))
        return result
    }

    private fun <T> getJobsForDTOs(
            tableName: String,
            jobBuilder: DmlInsertJobBuilder<T>,
            snapshotTimestamp: QueryParameterValue,
            dtos: List<T>): List<JobInfo> {
        val partitionSize = workbenchConfigProvider.get().reporting.maxRowsPerInsert
        return Lists.partition(dtos, partitionSize).stream()
                .map { batch: List<T> ->
                    JobInfo(
                            tableName,
                            batch.size,
                            jobBuilder.build(qualifyTableName(tableName), batch, snapshotTimestamp))
                }
                .collect(ImmutableList.toImmutableList())
    }

    private fun getTimestampValue(reportingSnapshot: ReportingSnapshot): QueryParameterValue {
        return QueryParameterValue.int64(reportingSnapshot.captureTimestamp)
    }

    private fun qualifyTableName(tableName: String): String {
        return String.format(
                "`%s.%s.%s`",
                workbenchConfigProvider.get().server.projectId,
                workbenchConfigProvider.get().reporting.dataset,
                tableName)
    }

    companion object {
        private val logger = Logger.getLogger(ReportingUploadServiceDmlImpl::class.java.name)
        private val MAX_WAIT_TIME = Duration.ofSeconds(60).toMillis()
        private val userJobBuilder = DmlInsertJobBuilder { UserColumnValueExtractor.values() }
        private val cohortJobBuilder = DmlInsertJobBuilder { CohortColumnValueExtractor.values() }
        private val institutionJobBuilder = DmlInsertJobBuilder { InstitutionColumnValueExtractor.values() }
        private val workspaceJobBuilder = DmlInsertJobBuilder { WorkspaceColumnValueExtractor.values() }
    }
}
