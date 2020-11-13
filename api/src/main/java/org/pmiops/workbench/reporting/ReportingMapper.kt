package org.pmiops.workbench.reporting

import org.mapstruct.Mapper
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace
import org.pmiops.workbench.db.model.DbStorageEnums
import org.pmiops.workbench.model.ReportingCohort
import org.pmiops.workbench.model.ReportingInstitution
import org.pmiops.workbench.model.ReportingSnapshot
import org.pmiops.workbench.model.ReportingUser
import org.pmiops.workbench.model.ReportingWorkspace
import org.pmiops.workbench.utils.mappers.CommonMappers
import org.pmiops.workbench.utils.mappers.MapStructConfig

@Mapper(config = MapStructConfig::class, uses = [CommonMappers::class, DbStorageEnums::class])
interface ReportingMapper {
    fun toReportingInstitution(prjInstitution: ProjectedReportingInstitution?): ReportingInstitution?
    fun toReportingInstitutionList(
            institutions: Collection<ProjectedReportingInstitution?>?): List<ReportingInstitution?>?

    fun toReportingUser(prjUser: ProjectedReportingUser?): ReportingUser?
    fun toReportingUserList(users: Collection<ProjectedReportingUser?>?): List<ReportingUser?>?
    fun toReportingWorkspace(prjWorkspace: ProjectedReportingWorkspace?): ReportingWorkspace?
    fun toReportingWorkspaceList(
            dbWorkspace: Collection<ProjectedReportingWorkspace?>?): List<ReportingWorkspace?>?

    fun toReportingCohort(cohort: ProjectedReportingCohort?): ReportingCohort?
    fun toReportingCohortList(cohorts: Collection<ProjectedReportingCohort?>?): List<ReportingCohort?>?
    fun toReportingSnapshot(
            queryResultBundle: QueryResultBundle, snapshotTimestamp: Long): ReportingSnapshot? {
        return ReportingSnapshot()
                .captureTimestamp(snapshotTimestamp)
                .cohorts(toReportingCohortList(queryResultBundle.cohorts))
                .institutions(toReportingInstitutionList(queryResultBundle.institutions))
                .users(toReportingUserList(queryResultBundle.users))
                .workspaces(toReportingWorkspaceList(queryResultBundle.workspaces))
    }
}
