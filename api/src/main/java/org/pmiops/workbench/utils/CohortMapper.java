package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Cohort;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CohortMapper {

  @Mapping(target = "cohortReviews", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "cohortId", source = "id")
  DbCohort clientToDbModel(Cohort source);

  @Mapping(target = "id", source = "cohortId")
  @Mapping(target = "etag", ignore = true)
  Cohort dbModelToClient(DbCohort destination);

  default DbUser DbUserToCreatorEmail(String creator) {
    final DbUser result = new DbUser();
    result.setEmail(creator);
    return result;
  }

  default String DbUserToCreatorEmail(DbUser creator) {
    return creator.getEmail();
  }
}
