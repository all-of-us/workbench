package org.pmiops.workbench.workspaces

import java.sql.Timestamp
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.model.UserRole
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceResponse

interface WorkspaceService {

    val dao: WorkspaceDao

    val fireCloudService: FireCloudService

    val workspacesAndPublicWorkspaces: List<WorkspaceResponse>

    val workspaces: List<WorkspaceResponse>

    val publishedWorkspaces: List<WorkspaceResponse>

    val recentWorkspaces: List<UserRecentWorkspace>

    fun findByWorkspaceId(workspaceId: Long): Workspace

    operator fun get(ns: String, firecloudName: String): Workspace

    fun getWorkspace(workspaceNamespace: String, workspaceId: String): WorkspaceResponse

    fun getByName(ns: String, name: String): Workspace

    fun getRequired(ns: String, firecloudName: String): Workspace

    fun getRequiredWithCohorts(ns: String, firecloudName: String): Workspace

    fun saveWithLastModified(workspace: Workspace): Workspace

    fun findForReview(): List<Workspace>

    fun setResearchPurposeApproved(ns: String, firecloudName: String, approved: Boolean)

    fun updateWorkspaceAcls(
            workspace: Workspace,
            userRoleMap: Map<String, WorkspaceAccessLevel>,
            registeredUsersGroup: String): Workspace

    fun saveAndCloneCohortsConceptSetsAndDataSets(from: Workspace, to: Workspace): Workspace

    fun getWorkspaceAccessLevel(workspaceNamespace: String, workspaceId: String): WorkspaceAccessLevel

    fun enforceWorkspaceAccessLevel(
            workspaceNamespace: String, workspaceId: String, requiredAccess: WorkspaceAccessLevel): WorkspaceAccessLevel

    fun getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace: String, workspaceId: String, workspaceAccessLevel: WorkspaceAccessLevel): Workspace

    fun getFirecloudWorkspaceAcls(
            workspaceNamespace: String, firecloudName: String): Map<String, WorkspaceAccessEntry>

    fun convertWorkspaceAclsToUserRoles(rolesMap: Map<String, WorkspaceAccessEntry>): List<UserRole>

    fun updateFirecloudAclsOnUser(
            updatedAccess: WorkspaceAccessLevel, currentUpdate: WorkspaceACLUpdate): WorkspaceACLUpdate

    fun setPublished(workspace: Workspace, publishedWorkspaceGroup: String, publish: Boolean): Workspace

    fun updateRecentWorkspaces(
            workspace: Workspace, userId: Long, lastAccessDate: Timestamp): UserRecentWorkspace

    fun updateRecentWorkspaces(workspace: Workspace): UserRecentWorkspace

    fun maybeDeleteRecentWorkspace(workspaceId: Long): Boolean

    companion object {

        val PROJECT_OWNER_ACCESS_LEVEL = "PROJECT_OWNER"
    }
}
