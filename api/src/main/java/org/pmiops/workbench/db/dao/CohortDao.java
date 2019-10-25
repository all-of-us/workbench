package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.CohortDataModel;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<CohortDataModel, Long> {

  /** Returns the cohort in the workspace with the specified name, or null if there is none. */
  CohortDataModel findCohortByNameAndWorkspaceId(String name, long workspaceId);

  List<CohortDataModel> findAllByCohortIdIn(Collection<Long> cohortIds);
}
