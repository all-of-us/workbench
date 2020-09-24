package org.pmiops.workbench.reporting;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class, DbStorageEnums.class})
public interface ReportingMapper {
  ReportingUser toDto(ProjectedReportingUser prjUser);

  List<ReportingUser> toReportingUserList(Collection<ProjectedReportingUser> users);

  ReportingWorkspace toDto(ProjectedReportingWorkspace prjWorkspace);

  List<ReportingWorkspace> toReportingWorkspaceList(
      Collection<ProjectedReportingWorkspace> dbWorkspace);
}
