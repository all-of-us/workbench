package org.pmiops.workbench.reporting

import com.google.common.base.Stopwatch
import org.pmiops.workbench.cohorts.CohortService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.institution.InstitutionService
import org.pmiops.workbench.model.ReportingSnapshot
import org.pmiops.workbench.reporting.ReportingSnapshotServiceImpl
import org.pmiops.workbench.utils.LogFormatters
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.logging.Logger
import javax.inject.Provider

@Service
class ReportingSnapshotServiceImpl(
        private val clock: Clock,
        private val cohortService: CohortService,
        private val institutionService: InstitutionService,
        private val reportingMapper: ReportingMapper,
        private val stopwatchProvider: Provider<Stopwatch>,
        private val userService: UserService,
        private val workspaceService: WorkspaceService) : ReportingSnapshotService {
    // Retrieve all the data we need from the MySQL database in a single transaction for
    // consistency.
    @Transactional(readOnly = true)
    override fun takeSnapshot(): ReportingSnapshot? {
        val queryResultBundle = applicationDbData
        return convertToReportingSnapshot(queryResultBundle)
    }

    fun convertToReportingSnapshot(queryResultBundle: QueryResultBundle?): ReportingSnapshot? {
        val stopwatch = stopwatchProvider.get().start()
        val result = reportingMapper.toReportingSnapshot(queryResultBundle!!, clock.millis())
        stopwatch.stop()
        log.info(LogFormatters.duration("Conversion to ReportingSnapshot", stopwatch.elapsed()))
        return result
    }

    /*
   * Gather all the projection instances from the DB rows needed for the current snapshot. Bundle
   * them all together for conversion all at once, and also to allow timing the downloads separately
   * from the conversion
   */
    private val applicationDbData: QueryResultBundle
        private get() {
            val stopwatch = stopwatchProvider.get().start()
            val result = QueryResultBundle(
                    cohortService.reportingCohorts,
                    institutionService.reportingInstitutions,
                    userService.reportingUsers,
                    workspaceService.reportingWorkspaces)
            stopwatch.stop()
            log.info(LogFormatters.duration("Application DB Queries", stopwatch.elapsed()))
            return result
        }

    companion object {
        private val log = Logger.getLogger(ReportingSnapshotServiceImpl::class.java.name)
    }
}
