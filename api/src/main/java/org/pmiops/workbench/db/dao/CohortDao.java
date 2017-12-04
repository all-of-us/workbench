package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortDefinition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CohortDao extends CrudRepository<Cohort, Long> {

    /**
     * Using an interface based projection to return only the cohort definition.
     *
     * @param cohortId
     * @param workspaceId
     * @return
     */
    CohortDefinition findCohortByCohortIdAndWorkspaceId(@Param("cohortId") long cohortId, @Param("workspaceId") long workspaceId);
}
