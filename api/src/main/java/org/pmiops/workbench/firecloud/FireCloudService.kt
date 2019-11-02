package org.pmiops.workbench.firecloud

import java.io.IOException
import org.pmiops.workbench.firecloud.model.BillingProjectMembership
import org.pmiops.workbench.firecloud.model.BillingProjectStatus
import org.pmiops.workbench.firecloud.model.JWTWrapper
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.firecloud.model.Me
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.firecloud.model.Workspace
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList
import org.pmiops.workbench.firecloud.model.WorkspaceResponse

/**
 * Encapsulate Firecloud API interaction details and provide a simple/mockable interface for
 * internal use.
 */
interface FireCloudService {

    /** @return true if firecloud is okay, false if firecloud is down.
     */
    val firecloudStatus: Boolean

    /** @return the FireCloud profile for the requesting user.
     */
    val me: Me

    /** Retrieves all billing project memberships for the user from FireCloud.  */
    val billingProjectMemberships: List<BillingProjectMembership>

    /**
     * Fetches the status of the currently-authenticated user's linkage to NIH's eRA Commons system.
     *
     *
     * Returns null if the FireCloud user is not found or if the user has no NIH linkage.
     */
    val nihStatus: NihStatus

    /**
     * Registers the user in Firecloud.
     *
     * @param contactEmail an email address that can be used to contact this user
     * @param firstName the user's first name
     * @param lastName the user's last name
     */
    fun registerUser(contactEmail: String, firstName: String, lastName: String)

    /** Creates a billing project owned by AllOfUs.  */
    fun createAllOfUsBillingProject(projectName: String)

    /** Get Billing Project Status  */
    fun getBillingProjectStatus(projectName: String): BillingProjectStatus

    /** Adds the specified user to the specified billing project.  */
    fun addUserToBillingProject(email: String, projectName: String)

    /**
     * Removes the specified user from the specified billing project.
     *
     *
     * Only used for errored billing projects
     */
    fun removeUserFromBillingProject(email: String, projectName: String)

    /** Adds the specified user as an owner to the specified billing project.  */
    fun addOwnerToBillingProject(ownerEmail: String, projectName: String)

    /**
     * Removes the specified user as an owner from the specified billing project. Since FireCloud
     * users cannot remove themselves, we need to supply the credential of a different user which will
     * retain ownership to make the call
     *
     *
     * Only used for billing project garbage collection
     */
    fun removeOwnerFromBillingProject(
            projectName: String, ownerEmailToRemove: String, callerAccessToken: String)

    /** Creates a new FC workspace.  */
    fun createWorkspace(projectName: String, workspaceName: String): Workspace

    fun cloneWorkspace(fromProject: String, fromName: String, toProject: String, toName: String): Workspace

    fun getWorkspaceAcl(projectName: String, workspaceName: String): WorkspaceACL

    fun updateWorkspaceACL(
            projectName: String, workspaceName: String, aclUpdates: List<WorkspaceACLUpdate>): WorkspaceACLUpdateResponseList

    /**
     * Requested field options specified here:
     * https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#heading=h.xgjl2srtytjt
     */
    fun getWorkspace(projectName: String, workspaceName: String): WorkspaceResponse

    fun getWorkspaces(fields: List<String>): List<WorkspaceResponse>

    fun deleteWorkspace(projectName: String, workspaceName: String)

    fun getGroup(groupname: String): ManagedGroupWithMembers

    fun createGroup(groupName: String): ManagedGroupWithMembers

    fun addUserToGroup(email: String, groupName: String)

    fun removeUserFromGroup(email: String, groupName: String)

    fun isUserMemberOfGroup(email: String, groupName: String): Boolean

    fun staticNotebooksConvert(notebook: ByteArray): String

    fun postNihCallback(wrapper: JWTWrapper): NihStatus

    @Throws(IOException::class)
    fun getApiClientWithImpersonation(email: String): ApiClient

    companion object {

        val WORKSPACE_DELIMITER = "__"
    }
}
