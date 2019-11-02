package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.Cohort
import org.springframework.data.repository.CrudRepository

interface CohortDao : CrudRepository<Cohort, Long> {

    /** Returns the cohort in the workspace with the specified name, or null if there is none.  */
    fun findCohortByNameAndWorkspaceId(name: String, workspaceId: Long): Cohort

    fun findAllByCohortIdIn(cohortIds: Collection<Long>): List<Cohort>
}
