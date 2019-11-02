package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.DbCohort;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<DbCohort, Long> {

  /** Returns the cohort in the workspace with the specified name, or null if there is none. */
  DbCohort findCohortByNameAndWorkspaceId(String name, long workspaceId);

  List<DbCohort> findAllByCohortIdIn(Collection<Long> cohortIds);
}
