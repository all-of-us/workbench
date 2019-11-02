package org.pmiops.workbench.cohorts

import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace

interface CohortFactory {

    fun createCohort(apiCohort: org.pmiops.workbench.model.Cohort, creator: User, workspaceId: Long): Cohort

    fun duplicateCohort(newName: String, creator: User, targetWorkspace: Workspace, original: Cohort): Cohort

    fun duplicateCohortReview(original: CohortReview, targetCohort: Cohort): CohortReview
}
