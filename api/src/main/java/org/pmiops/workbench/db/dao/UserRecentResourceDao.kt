package org.pmiops.workbench.db.dao

import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.UserRecentResource
import org.springframework.data.repository.CrudRepository

interface UserRecentResourceDao : CrudRepository<UserRecentResource, Long> {

    fun countUserRecentResourceByUserId(userId: Long): Long

    fun findTopByUserIdOrderByLastAccessDate(userId: Long): UserRecentResource

    fun findByUserIdAndWorkspaceIdAndCohort(
            userId: Long, workspaceId: Long, cohort: Cohort): UserRecentResource

    fun findByUserIdAndWorkspaceIdAndNotebookName(
            userId: Long, workspaceId: Long, notebookPath: String): UserRecentResource

    fun findByUserIdAndWorkspaceIdAndConceptSet(
            userId: Long, workspaceId: Long, conceptSet: ConceptSet): UserRecentResource

    fun findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(userId: Long): List<UserRecentResource>
}
