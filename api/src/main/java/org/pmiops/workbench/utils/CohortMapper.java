package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.model.Cohort;

@Mapper
public interface CohortMapper {
  DbCohort sourceToDestination(Cohort source);
  Cohort destinationToSource(DbCohort destination);
}
