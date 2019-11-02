package org.pmiops.workbench.workspaces

import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.sql.Timestamp
import java.time.Clock
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import kotlin.collections.Map.Entry
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.DataSet
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.model.UserRole
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.pmiops.workbench.model.WorkspaceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.String.Companion

/**
 * Workspace manipulation and shared business logic which can't be represented by automatic query
 * generation in WorkspaceDao, or convenience aliases.
 *
 *
 * This needs to implement an interface to support Transactional
 */
@Service
class WorkspaceServiceImpl @Autowired
constructor(
        private val clock: Clock,
        // Note: Cannot use an @Autowired constructor with this version of Spring
        // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
        private val cohortCloningService: CohortCloningService,
        private val conceptSetService: ConceptSetService,
        private val dataSetService: DataSetService,
        override val fireCloudService: FireCloudService,
        private val userDao: UserDao,
        private val userProvider: Provider<User>,
        private val userRecentWorkspaceDao: UserRecentWorkspaceDao,
        /**
         * Clients wishing to use the auto-generated methods from the DAO interface may directly access it
         * here.
         */
        override val dao: WorkspaceDao) : WorkspaceService {

    override val workspaces: List<WorkspaceResponse>
        get() = workspacesAndPublicWorkspaces.stream()
                .filter { workspaceResponse ->
                    (workspaceResponse.getAccessLevel() === WorkspaceAccessLevel.OWNER
                            || workspaceResponse.getAccessLevel() === WorkspaceAccessLevel.WRITER
                            || !workspaceResponse.getWorkspace().getPublished())
                }
                .collect(Collectors.toList<Any>())

    override val publishedWorkspaces: List<WorkspaceResponse>
        get() = workspacesAndPublicWorkspaces.stream()
                .filter { workspaceResponse -> workspaceResponse.getWorkspace().getPublished() }
                .collect(Collectors.toList<Any>())

    override val workspacesAndPublicWorkspaces: List<WorkspaceResponse>
        get() {
            val fcWorkspaces = getFirecloudWorkspaces(ImmutableList.of("accessLevel", "workspace.workspaceId"))
            val dbWorkspaces = dao.findAllByFirecloudUuidIn(fcWorkspaces.keys)

            return dbWorkspaces.stream()
                    .filter { dbWorkspace -> dbWorkspace.workspaceActiveStatusEnum === WorkspaceActiveStatus.ACTIVE }
                    .map<Any> { dbWorkspace ->
                        val fcWorkspaceAccessLevel = fcWorkspaces[dbWorkspace.firecloudUuid].getAccessLevel()
                        val currentWorkspace = WorkspaceResponse()
                        currentWorkspace.setWorkspace(WorkspaceConversionUtils.toApiWorkspace(dbWorkspace))
                        currentWorkspace.setAccessLevel(
                                WorkspaceConversionUtils.toApiWorkspaceAccessLevel(fcWorkspaceAccessLevel))
                        currentWorkspace
                    }
                    .collect<List<WorkspaceResponse>, Any>(Collectors.toList())
        }

    override val recentWorkspaces: List<UserRecentWorkspace>
        @Transactional
        get() {
            val userId = userProvider.get().userId
            val userRecentWorkspaces = userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId)
            return pruneInaccessibleRecentWorkspaces(userRecentWorkspaces, userId)
        }

    override fun get(ns: String, firecloudName: String): Workspace? {
        return dao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
                ns,
                firecloudName,
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)
    }

    @Transactional
    override fun getWorkspace(workspaceNamespace: String, workspaceId: String): WorkspaceResponse {
        val dbWorkspace = getRequired(workspaceNamespace, workspaceId)

        val fcResponse: org.pmiops.workbench.firecloud.model.WorkspaceResponse
        val fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace

        val workspaceResponse = WorkspaceResponse()

        // This enforces access controls.
        fcResponse = fireCloudService.getWorkspace(workspaceNamespace, workspaceId)
        fcWorkspace = fcResponse.getWorkspace()

        if (fcResponse.getAccessLevel().equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
            // We don't expose PROJECT_OWNER in our API; just use OWNER.
            workspaceResponse.setAccessLevel(WorkspaceAccessLevel.OWNER)
        } else {
            workspaceResponse.setAccessLevel(WorkspaceAccessLevel.fromValue(fcResponse.getAccessLevel()))
            if (workspaceResponse.getAccessLevel() == null) {
                throw ServerErrorException("Unsupported access level: " + fcResponse.getAccessLevel())
            }
        }
        workspaceResponse.setWorkspace(
                WorkspaceConversionUtils.toApiWorkspace(dbWorkspace, fcWorkspace))

        return workspaceResponse
    }

    private fun getFirecloudWorkspaces(fields: List<String>): Map<String, org.pmiops.workbench.firecloud.model.WorkspaceResponse> {
        // fields must include at least "workspace.workspaceId", otherwise
        // the map creation will fail
        return fireCloudService.getWorkspaces(fields).stream()
                .collect(
                        Collectors.toMap<Any, Any, Any>(
                                { fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId() },
                                { fcWorkspace -> fcWorkspace }))
    }

    override fun getFirecloudWorkspaceAcls(
            workspaceNamespace: String?, firecloudName: String?): Map<String, WorkspaceAccessEntry> {
        val aclResp = fireCloudService.getWorkspaceAcl(workspaceNamespace, firecloudName)

        // Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
        // instead. Run this through a typed Gson conversion process to parse into the desired type.
        val accessEntryType = object : TypeToken<Map<String, WorkspaceAccessEntry>>() {

        }.type
        val gson = Gson()
        return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType)
    }

    /**
     * This is an internal method used by createWorkspace and cloneWorkspace endpoints, to check the
     * existence of ws name. Currently does not return a conflict if user is checking the name of a
     * deleted ws.
     */
    override fun getByName(ns: String, name: String): Workspace {
        return dao.findByWorkspaceNamespaceAndNameAndActiveStatus(
                ns, name, StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)
    }

    override fun getRequired(ns: String, firecloudName: String): Workspace {
        return get(ns, firecloudName)
                ?: throw NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName))
    }

    @Transactional
    override fun getRequiredWithCohorts(ns: String, firecloudName: String): Workspace {
        return dao.findByFirecloudNameAndActiveStatusWithEagerCohorts(
                ns,
                firecloudName,
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)
                ?: throw NotFoundException(String.format("Workspace %s/%s not found.", ns, firecloudName))
    }

    override fun saveWithLastModified(workspace: Workspace): Workspace {
        return saveWithLastModified(workspace, Timestamp(clock.instant().toEpochMilli()))
    }

    private fun saveWithLastModified(workspace: Workspace, ts: Timestamp): Workspace {
        workspace.lastModifiedTime = ts
        try {
            return dao.save(workspace)
        } catch (e: ObjectOptimisticLockingFailureException) {
            log.log(Level.WARNING, "version conflict for workspace update", e)
            throw ConflictException("Failed due to concurrent workspace modification")
        }

    }

    override fun findForReview(): List<Workspace> {
        return dao.findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested()
    }

    override fun setResearchPurposeApproved(ns: String, firecloudName: String, approved: Boolean) {
        val workspace = getRequired(ns, firecloudName)
        if (workspace.reviewRequested == null || !workspace.reviewRequested) {
            throw BadRequestException(
                    String.format("No review requested for workspace %s/%s.", ns, firecloudName))
        }
        if (workspace.approved != null) {
            throw BadRequestException(
                    String.format(
                            "Workspace %s/%s already %s.",
                            ns, firecloudName, if (workspace.approved) "approved" else "rejected"))
        }
        val now = Timestamp(clock.instant().toEpochMilli())
        workspace.approved = approved
        saveWithLastModified(workspace, now)
    }

    override fun updateFirecloudAclsOnUser(
            updatedAccess: WorkspaceAccessLevel, currentUpdate: WorkspaceACLUpdate): WorkspaceACLUpdate {
        if (updatedAccess === WorkspaceAccessLevel.OWNER) {
            currentUpdate.setCanShare(true)
            currentUpdate.setCanCompute(true)
            currentUpdate.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())
        } else if (updatedAccess === WorkspaceAccessLevel.WRITER) {
            currentUpdate.setCanShare(false)
            currentUpdate.setCanCompute(true)
            currentUpdate.setAccessLevel(WorkspaceAccessLevel.WRITER.toString())
        } else if (updatedAccess === WorkspaceAccessLevel.READER) {
            currentUpdate.setCanShare(false)
            currentUpdate.setCanCompute(false)
            currentUpdate.setAccessLevel(WorkspaceAccessLevel.READER.toString())
        } else {
            currentUpdate.setCanShare(false)
            currentUpdate.setCanCompute(false)
            currentUpdate.setAccessLevel(WorkspaceAccessLevel.NO_ACCESS.toString())
        }
        return currentUpdate
    }

    override fun updateWorkspaceAcls(
            workspace: Workspace,
            updatedAclsMap: Map<String, WorkspaceAccessLevel>,
            registeredUsersGroup: String): Workspace {
        // userRoleMap is a map of the new permissions for ALL users on the ws
        val aclsMap = getFirecloudWorkspaceAcls(workspace.workspaceNamespace, workspace.firecloudName)

        // Iterate through existing roles, update/remove them
        val updateACLRequestList = ArrayList<WorkspaceACLUpdate>()
        val toAdd = HashMap(updatedAclsMap)
        for ((currentUserEmail) in aclsMap) {
            val updatedAccess = toAdd.get(currentUserEmail)
            if (updatedAccess != null) {
                var currentUpdate = WorkspaceACLUpdate()
                currentUpdate.setEmail(currentUserEmail)
                currentUpdate = updateFirecloudAclsOnUser(updatedAccess, currentUpdate)
                updateACLRequestList.add(currentUpdate)
                toAdd.remove(currentUserEmail)
            } else {
                // This is how to remove a user from the FireCloud ACL:
                // Pass along an update request with NO ACCESS as the given access level.
                // Note: do not do groups.  Unpublish will pass the specific NO_ACCESS acl
                // TODO [jacmrob] : have all users pass NO_ACCESS explicitly? Handle filtering on frontend?
                if (currentUserEmail != registeredUsersGroup) {
                    var removedUser = WorkspaceACLUpdate()
                    removedUser.setEmail(currentUserEmail)
                    removedUser = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, removedUser)
                    updateACLRequestList.add(removedUser)
                }
            }
        }

        // Iterate through remaining new roles; add them
        for (remainingRole in toAdd.entries) {
            var newUser = WorkspaceACLUpdate()
            newUser.setEmail(remainingRole.key)
            newUser = updateFirecloudAclsOnUser(remainingRole.value, newUser)
            updateACLRequestList.add(newUser)
        }
        val fireCloudResponse = fireCloudService.updateWorkspaceACL(
                workspace.workspaceNamespace, workspace.firecloudName, updateACLRequestList)
        if (fireCloudResponse.getUsersNotFound().size() !== 0) {
            var usersNotFound = ""
            for (i in 0 until fireCloudResponse.getUsersNotFound().size()) {
                if (i > 0) {
                    usersNotFound += ", "
                }
                usersNotFound += fireCloudResponse.getUsersNotFound().get(i).getEmail()
            }
            throw BadRequestException(usersNotFound)
        }

        // Finally, keep OWNER and billing project users in lock-step. In Rawls, OWNER does not grant
        // canCompute on the workspace / billing project, nor does it grant the ability to grant
        // canCompute to other users. See RW-3009 for details.
        for (email in Sets.union(updatedAclsMap.keys, aclsMap.keys)) {
            val fromAccess = (aclsMap as java.util.Map<String, WorkspaceAccessEntry>).getOrDefault(email, WorkspaceAccessEntry().accessLevel("")).getAccessLevel()
            val toAccess = (updatedAclsMap as java.util.Map<String, WorkspaceAccessLevel>).getOrDefault(email, WorkspaceAccessLevel.NO_ACCESS)
            if (FC_OWNER_ROLE == fromAccess && WorkspaceAccessLevel.OWNER !== toAccess) {
                log.info(
                        String.format(
                                "removing user '%s' from billing project '%s'",
                                email, workspace.workspaceNamespace))
                fireCloudService.removeUserFromBillingProject(email, workspace.workspaceNamespace)
            } else if (FC_OWNER_ROLE != fromAccess && WorkspaceAccessLevel.OWNER === toAccess) {
                log.info(
                        String.format(
                                "adding user '%s' to billing project '%s'",
                                email, workspace.workspaceNamespace))
                fireCloudService.addUserToBillingProject(email, workspace.workspaceNamespace)
            }
        }

        return this.saveWithLastModified(workspace)
    }

    @Transactional
    override fun saveAndCloneCohortsConceptSetsAndDataSets(from: Workspace, to: Workspace): Workspace {
        var to = to
        // Save the workspace first to allocate an ID.
        to = dao.save(to)
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(to.cdrVersion)
        val cdrVersionChanged = from.cdrVersion!!.cdrVersionId != to.cdrVersion!!.cdrVersionId
        val fromCohortIdToToCohortId = HashMap<Long, Long>()
        for (fromCohort in from.cohorts) {
            fromCohortIdToToCohortId[fromCohort.cohortId] = cohortCloningService.cloneCohortAndReviews(fromCohort, to).cohortId
        }
        val fromConceptSetIdToToConceptSetId = HashMap<Long, Long>()
        for (fromConceptSet in conceptSetService.getConceptSets(from)) {
            fromConceptSetIdToToConceptSetId[fromConceptSet.conceptSetId] = conceptSetService
                    .cloneConceptSetAndConceptIds(fromConceptSet, to, cdrVersionChanged)
                    .conceptSetId
        }
        for (dataSet in dataSetService.getDataSets(from)) {
            dataSetService.cloneDataSetToWorkspace(
                    dataSet,
                    to,
                    fromCohortIdToToCohortId.entries.stream()
                            .filter { cohortIdEntry -> dataSet.cohortIds!!.contains(cohortIdEntry.key) }
                            .map<Long>(Function<Entry<Long, Long>, Long> { it.value })
                            .collect<Set<Long>, Any>(Collectors.toSet()),
                    fromConceptSetIdToToConceptSetId.entries.stream()
                            .filter { conceptSetId -> dataSet.conceptSetIds!!.contains(conceptSetId.key) }
                            .map<Long>(Function<Entry<Long, Long>, Long> { it.value })
                            .collect<Set<Long>, Any>(Collectors.toSet()))
        }
        return to
    }

    override fun getWorkspaceAccessLevel(
            workspaceNamespace: String?, workspaceId: String?): WorkspaceAccessLevel {
        val workspaceACL = fireCloudService.getWorkspaceAcl(workspaceNamespace, workspaceId)
        val workspaceAccessEntry = Optional.of(workspaceACL.getAcl().get(userProvider.get().email))
                .orElseThrow(
                        {
                            NotFoundException(
                                    String.format(
                                            "Workspace %s/%s not found", workspaceNamespace, workspaceId))
                        })
        val userAccess = workspaceAccessEntry.getAccessLevel()

        return if (userAccess == WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL) {
            WorkspaceAccessLevel.OWNER
        } else WorkspaceAccessLevel.fromValue(userAccess)
                ?: throw ServerErrorException("Unrecognized access level: $userAccess")
    }

    override fun enforceWorkspaceAccessLevel(
            workspaceNamespace: String?, workspaceId: String?, requiredAccess: WorkspaceAccessLevel): WorkspaceAccessLevel {
        val access = getWorkspaceAccessLevel(workspaceNamespace, workspaceId)
        return if (requiredAccess.compareTo(access) > 0) {
            throw ForbiddenException(
                    String.format(
                            "You do not have sufficient permissions to access workspace %s/%s",
                            workspaceNamespace, workspaceId))
        } else {
            access
        }
    }

    override fun getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace: String, workspaceId: String, workspaceAccessLevel: WorkspaceAccessLevel): Workspace {
        enforceWorkspaceAccessLevel(workspaceNamespace, workspaceId, workspaceAccessLevel)
        val workspace = getRequired(workspaceNamespace, workspaceId)
        // Because we've already checked that the user has access to the workspace in question,
        // we don't need to check their membership in the authorization domain for the CDR version
        // associated with the workspace.
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(workspace.cdrVersion)
        return workspace
    }

    override fun findByWorkspaceId(workspaceId: Long): Workspace {
        val workspace = dao.findOne(workspaceId)
        if (workspace == null || workspace.workspaceActiveStatusEnum !== WorkspaceActiveStatus.ACTIVE) {
            throw NotFoundException(String.format("Workspace %s not found.", workspaceId))
        }
        return workspace
    }

    override fun convertWorkspaceAclsToUserRoles(
            rolesMap: Map<String, WorkspaceAccessEntry>): List<UserRole> {
        val userRoles = ArrayList<UserRole>()
        for ((key, value) in rolesMap) {
            // Filter out groups
            val user = userDao.findUserByEmail(key)
            if (user == null) {
                log.log(Level.WARNING, "No user found for $key")
            } else {
                userRoles.add(WorkspaceConversionUtils.toApiUserRole(user, value))
            }
        }
        return userRoles.stream()
                .sorted(
                        Comparator.comparing(Function<Any, Any> { UserRole.getRole() }).thenComparing(???({ UserRole.getEmail() })).reversed())
        .collect(Collectors.toList<Any>())
    }

    override fun setPublished(
            workspace: Workspace, publishedWorkspaceGroup: String, publish: Boolean): Workspace {
        val updateACLRequestList = ArrayList<WorkspaceACLUpdate>()
        var currentUpdate = WorkspaceACLUpdate()
        currentUpdate.setEmail(publishedWorkspaceGroup)

        if (publish) {
            currentUpdate = updateFirecloudAclsOnUser(WorkspaceAccessLevel.READER, currentUpdate)
            workspace.published = true
        } else {
            currentUpdate = updateFirecloudAclsOnUser(WorkspaceAccessLevel.NO_ACCESS, currentUpdate)
            workspace.published = false
        }

        updateACLRequestList.add(currentUpdate)
        fireCloudService.updateWorkspaceACL(
                workspace.workspaceNamespace, workspace.firecloudName, updateACLRequestList)

        return this.saveWithLastModified(workspace)
    }

    private fun pruneInaccessibleRecentWorkspaces(
            recentWorkspaces: List<UserRecentWorkspace>, userId: Long): List<UserRecentWorkspace> {
        val dbWorkspaces = dao.findAllByWorkspaceIdIn(
                recentWorkspaces.stream()
                        .map<Long>(Function<UserRecentWorkspace, Long> { it.getWorkspaceId() })
                        .collect<List<Long>, Any>(Collectors.toList()))

        val workspaceIdsToDelete = dbWorkspaces.stream()
                .filter { workspace ->
                    try {
                        enforceWorkspaceAccessLevel(
                                workspace.workspaceNamespace,
                                workspace.firecloudName,
                                WorkspaceAccessLevel.READER)
                    } catch (e: ForbiddenException) {
                        return@dbWorkspaces.stream()
                                .filter true
                    } catch (e: NotFoundException) {
                        return@dbWorkspaces.stream()
                                .filter true
                    }

                    false
                }
                .map<Long>(Function<Workspace, Long> { it.getWorkspaceId() })
                .collect<Set<Long>, Any>(Collectors.toSet())

        if (!workspaceIdsToDelete.isEmpty()) {
            userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(userId, workspaceIdsToDelete)
        }

        return recentWorkspaces.stream()
                .filter { recentWorkspace -> !workspaceIdsToDelete.contains(recentWorkspace.workspaceId) }
                .collect<List<UserRecentWorkspace>, Any>(Collectors.toList())
    }

    override fun updateRecentWorkspaces(
            workspace: Workspace, userId: Long, lastAccessDate: Timestamp): UserRecentWorkspace {
        val maybeRecentWorkspace = userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspace.workspaceId, userId)
        val matchingRecentWorkspace = maybeRecentWorkspace
                .map { recentWorkspace ->
                    recentWorkspace.lastAccessDate = lastAccessDate
                    recentWorkspace
                }
                .orElseGet { UserRecentWorkspace(workspace.workspaceId, userId, lastAccessDate) }
        userRecentWorkspaceDao.save(matchingRecentWorkspace)
        handleWorkspaceLimit(userId)
        return matchingRecentWorkspace
    }

    @Transactional
    override fun updateRecentWorkspaces(workspace: Workspace): UserRecentWorkspace {
        return updateRecentWorkspaces(
                workspace, userProvider.get().userId, Timestamp(clock.instant().toEpochMilli()))
    }

    private fun handleWorkspaceLimit(userId: Long) {
        val userRecentWorkspaces = userRecentWorkspaceDao.findByUserIdOrderByLastAccessDateDesc(userId)

        val idsToDelete = ArrayList<Long>()
        while (userRecentWorkspaces.size > RECENT_WORKSPACE_COUNT) {
            idsToDelete.add(userRecentWorkspaces[userRecentWorkspaces.size - 1].workspaceId)
            userRecentWorkspaces.removeAt(userRecentWorkspaces.size - 1)
        }
        userRecentWorkspaceDao.deleteByUserIdAndWorkspaceIdIn(userId, idsToDelete)
    }

    override
            /** Returns true if anything was deleted from user_recent_workspaces, false if nothing was  */
    fun maybeDeleteRecentWorkspace(workspaceId: Long): Boolean {
        val userId = userProvider.get().userId
        val maybeRecentWorkspace = userRecentWorkspaceDao.findFirstByWorkspaceIdAndUserId(workspaceId, userId)
        if (maybeRecentWorkspace.isPresent) {
            userRecentWorkspaceDao.delete(maybeRecentWorkspace.get())
            return true
        } else {
            return false
        }
    }

    companion object {

        private val FC_OWNER_ROLE = "OWNER"
        val RECENT_WORKSPACE_COUNT = 4
        private val log = Logger.getLogger(WorkspaceService::class.java.name)
    }
}
