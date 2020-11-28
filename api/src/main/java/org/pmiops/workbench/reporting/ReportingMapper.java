package org.pmiops.workbench.reporting;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.snapshot.QueryResultBundle;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ReportingMapper {

  ReportingInstitution toReportingInstitution(ProjectedReportingInstitution prjInstitution);

  List<ReportingInstitution> toReportingInstitutionList(
      Collection<ProjectedReportingInstitution> institutions);

  ReportingUser toReportingUser(ProjectedReportingUser prjUser);

  List<ReportingUser> toReportingUserList(Collection<ProjectedReportingUser> users);

  ReportingWorkspace toReportingWorkspace(ProjectedReportingWorkspace prjWorkspace);

  List<ReportingWorkspace> toReportingWorkspaceList(
      Collection<ProjectedReportingWorkspace> dbWorkspace);

  ReportingCohort toReportingCohort(ProjectedReportingCohort cohort);

  List<ReportingCohort> toReportingCohortList(Collection<ProjectedReportingCohort> cohorts);

  default ReportingSnapshot toReportingSnapshot(
      QueryResultBundle queryResultBundle, long snapshotTimestamp) {
    return new ReportingSnapshot()
        .captureTimestamp(snapshotTimestamp)
        .cohorts(toReportingCohortList(queryResultBundle.getCohorts()))
        .institutions(toReportingInstitutionList(queryResultBundle.getInstitutions()))
        .users(toReportingUserList(queryResultBundle.getUsers()))
        .workspaces(toReportingWorkspaceList(queryResultBundle.getWorkspaces()));
  }
}
