package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.Cohort;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CohortMapper {
  DbCohort clientToDbModel(Cohort source);
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
