package org.pmiops.workbench.reporting;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.utils.mappers.TimeMappers;

@Mapper(
    //    config = MapStructConfig.class,
    componentModel = "spring",
    uses = {TimeMappers.class})
public interface ReportingMapper {
  @Mapping(source = "dbUser.givenName", target = "firstName")
  @Mapping(source = "dbUser.userId", target = "researcherId")
  @Mapping(source = "dbUser.disabled", target = "isDisabled")
  ReportingResearcher toModel(DbUser dbUser, DbAddress dbAddress, DbInstitution dbInstitution);

  default List<ReportingResearcher> toReportingResearcherList(Collection<DbUser> dbUsers) {
    return dbUsers.stream()
        .map(u -> toModel(u, null, null))
        .collect(ImmutableList.toImmutableList());
  }

  @Mapping(source = "creator.userId", target = "creatorId")
  @Mapping(target = "fakeSize", ignore = true) // temp column for testing; not in mapper
  ReportingWorkspace toModel(DbWorkspace dbWorkspace);

  List<ReportingWorkspace> toReportingWorkspaceList(Collection<DbWorkspace> dbWorkspace);
}
