package org.pmiops.workbench.db.dao

import java.sql.Timestamp
import org.pmiops.workbench.db.model.UserRecentResource
import org.springframework.stereotype.Service

@Service
interface UserRecentResourceService {

    fun updateNotebookEntry(
            workspaceId: Long, userId: Long, notebookNameWithPath: String, lastAccessDateTime: Timestamp): UserRecentResource

    fun updateCohortEntry(
            workspaceId: Long, userId: Long, cohortId: Long, lastAccessDateTime: Timestamp)

    fun updateConceptSetEntry(
            workspaceId: Long, userId: Long, conceptSetId: Long, lastAccessDateTime: Timestamp)

    fun deleteNotebookEntry(workspaceId: Long, userId: Long, notebookName: String)

    fun findAllResourcesByUser(userId: Long): List<UserRecentResource>
}
