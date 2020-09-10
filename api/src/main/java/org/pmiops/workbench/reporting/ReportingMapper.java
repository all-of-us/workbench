package org.pmiops.workbench.reporting;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.dao.projection.PrjUser;
import org.pmiops.workbench.db.dao.projection.PrjWorkspace;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface ReportingMapper {
  BqDtoUser toDto(PrjUser prjUser);

  List<BqDtoUser> toReportingResearcherList(Collection<PrjUser> users);

  BqDtoWorkspace toModel(PrjWorkspace prjWorkspace);

  List<BqDtoWorkspace> toReportingWorkspaceList(Collection<PrjWorkspace> dbWorkspace);
}
