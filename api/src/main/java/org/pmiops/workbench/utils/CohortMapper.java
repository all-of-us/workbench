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
  DbCohort sourceToDestination(Cohort source);
  Cohort destinationToSource(DbCohort destination);

  default DbUser map(String creator) {
    final DbUser result = new DbUser();
    result.setEmail(creator);
    return result;
  }

  default String map(DbUser creator) {
    return creator.getEmail();
  }
}
