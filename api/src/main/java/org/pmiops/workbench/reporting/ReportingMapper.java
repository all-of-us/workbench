package org.pmiops.workbench.reporting;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface ReportingMapper {
  @Mapping(source = "givenName", target = "firstName")
  @Mapping(source = "userId", target = "researcherId")
  @Mapping(source = "disabled", target = "isDisabled")
  ReportingResearcher toModel(DbUser dbUser);

  List<ReportingResearcher> toReportingResearcherList(Collection<DbUser> dbUsers);

  @Mapping(source = "creator", target = "creatorId")
  @Mapping(target = "fakeSize", ignore = true) // not defined yet
  ReportingWorkspace toModel(DbWorkspace dbWorkspace);

  List<ReportingWorkspace> toReportingWorkspaceList(Collection<DbWorkspace> dbWorkspace);
}
