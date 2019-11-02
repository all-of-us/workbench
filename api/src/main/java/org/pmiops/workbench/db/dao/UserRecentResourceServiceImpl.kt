package org.pmiops.workbench.db.dao

import com.google.common.annotations.VisibleForTesting
import java.sql.Timestamp
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.UserRecentResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserRecentResourceServiceImpl : UserRecentResourceService {

    @Autowired
    @set:VisibleForTesting
    var dao: UserRecentResourceDao

    @Autowired
    internal var cohortDao: CohortDao

    @Autowired
    internal var conceptSetDao: ConceptSetDao

    val userEntryCount: Int
        @VisibleForTesting
        get() = USER_ENTRY_COUNT

    @VisibleForTesting
    fun setCohortDao(cohortDao: CohortDao) {
        this.cohortDao = cohortDao
    }

    @VisibleForTesting
    fun setConceptSetDao(conceptSetDao: ConceptSetDao) {
        this.conceptSetDao = conceptSetDao
    }

    /**
     * Checks if notebook for given workspace and user is already in table user_recent_resource if
     * yes, update the lastAccessDateTime only If no, check the number of resource entries for given
     * user if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add
     * a new entry
     */
    override fun updateNotebookEntry(
            workspaceId: Long, userId: Long, notebookNameWithPath: String, lastAccessDateTime: Timestamp): UserRecentResource {
        var recentResource: UserRecentResource? = dao
                .findByUserIdAndWorkspaceIdAndNotebookName(userId, workspaceId, notebookNameWithPath)
        if (recentResource == null) {
            handleUserLimit(userId)
            recentResource = UserRecentResource(workspaceId, userId, notebookNameWithPath, lastAccessDateTime)
        }
        recentResource.lastAccessDate = lastAccessDateTime
        dao.save(recentResource)
        return recentResource
    }

    /**
     * Checks if cohort for given workspace and user is already in table user_recent_resource if yes,
     * update the lastAccessDateTime only If no, check the number of resource entries for given user
     * if it is above config userEntrycount, delete the row(s) with least lastAccessTime and add a new
     * entry
     */
    override fun updateCohortEntry(
            workspaceId: Long, userId: Long, cohortId: Long, lastAccessDateTime: Timestamp) {
        val cohort = cohortDao.findOne(cohortId)
        var resource: UserRecentResource? = dao.findByUserIdAndWorkspaceIdAndCohort(userId, workspaceId, cohort)
        if (resource == null) {
            handleUserLimit(userId)
            resource = UserRecentResource(workspaceId, userId, lastAccessDateTime)
            resource.cohort = cohort
            resource.conceptSet = null
        }
        resource.lastAccessDate = lastAccessDateTime
        dao.save(resource)
    }

    override fun updateConceptSetEntry(
            workspaceId: Long, userId: Long, conceptSetId: Long, lastAccessDateTime: Timestamp) {
        val conceptSet = conceptSetDao.findOne(conceptSetId)
        var resource: UserRecentResource? = dao.findByUserIdAndWorkspaceIdAndConceptSet(userId, workspaceId, conceptSet)
        if (resource == null) {
            handleUserLimit(userId)
            resource = UserRecentResource(workspaceId, userId, lastAccessDateTime)
            resource.conceptSet = conceptSet
            resource.cohort = null
        }
        resource.lastAccessDate = lastAccessDateTime
        dao.save(resource)
    }

    /** Deletes notebook entry from user_recent_resource  */
    override fun deleteNotebookEntry(workspaceId: Long, userId: Long, notebookPath: String) {
        val resource = dao.findByUserIdAndWorkspaceIdAndNotebookName(userId, workspaceId, notebookPath)
        if (resource != null) {
            dao.delete(resource)
        }
    }

    /**
     * Retrieves the list of all resources recently accessed by user in descending order of last
     * access date
     *
     * @param userId : User id for whom the resources are returned
     */
    override fun findAllResourcesByUser(userId: Long): List<UserRecentResource> {
        return dao.findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(userId)
    }

    /**
     * Check number of entries in user_recent_resource for user, If it exceeds USER_ENTRY_COUNT,
     * delete the one with earliest lastAccessTime
     */
    private fun handleUserLimit(userId: Long) {
        var count = dao.countUserRecentResourceByUserId(userId)
        while (count-- >= USER_ENTRY_COUNT) {
            val resource = dao.findTopByUserIdOrderByLastAccessDate(userId)
            dao.delete(resource)
        }
    }

    companion object {

        private val USER_ENTRY_COUNT = 10
    }
}
