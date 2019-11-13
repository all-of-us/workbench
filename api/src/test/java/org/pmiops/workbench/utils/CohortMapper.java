package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Cohort;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class, UserDao.class})
public interface CohortMapper {

  @Mapping(target = "cohortReviews", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "cohortId", source = "id")
  DbCohort clientToDbModel(Cohort source);

  @Mapping(target = "id", source = "cohortId")
  @Mapping(target = "etag", ignore = true)
  Cohort dbModelToClient(DbCohort destination);

  default String dbUserToCreatorEmail(DbUser creator) {
    return creator.getEmail();
  }
}
