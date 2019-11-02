package org.pmiops.workbench.firecloud

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpStatusCodes
import com.google.common.collect.ImmutableList
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.json.JSONException
import org.json.JSONObject
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.firecloud.api.BillingApi
import org.pmiops.workbench.firecloud.api.GroupsApi
import org.pmiops.workbench.firecloud.api.NihApi
import org.pmiops.workbench.firecloud.api.ProfileApi
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi
import org.pmiops.workbench.firecloud.api.StatusApi
import org.pmiops.workbench.firecloud.api.WorkspacesApi
import org.pmiops.workbench.firecloud.model.BillingProjectMembership
import org.pmiops.workbench.firecloud.model.BillingProjectStatus
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest
import org.pmiops.workbench.firecloud.model.JWTWrapper
import org.pmiops.workbench.firecloud.model.ManagedGroupRef
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.firecloud.model.Me
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.firecloud.model.Profile
import org.pmiops.workbench.firecloud.model.Workspace
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList
import org.pmiops.workbench.firecloud.model.WorkspaceIngest
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
// TODO: consider retrying internally when FireCloud returns a 503
class FireCloudServiceImpl @Autowired
constructor(
        private val configProvider: Provider<WorkbenchConfig>,
        private val profileApiProvider: Provider<ProfileApi>,
        private val billingApiProvider: Provider<BillingApi>,
        private val groupsApiProvider: Provider<GroupsApi>,
        private val nihApiProvider: Provider<NihApi>,
        @param:Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
        private val workspacesApiProvider: Provider<WorkspacesApi>,
        @param:Qualifier(FireCloudConfig.SERVICE_ACCOUNT_WORKSPACE_API)
        private val workspaceAclsApiProvider: Provider<WorkspacesApi>,
        private val statusApiProvider: Provider<StatusApi>,
        private val staticNotebooksApiProvider: Provider<StaticNotebooksApi>,
        private val retryHandler: FirecloudRetryHandler,
        private val serviceAccounts: ServiceAccounts,
        @param:Qualifier(Constants.FIRECLOUD_ADMIN_CREDS) private val fcAdminCredsProvider: Provider<GoogleCredential>) : FireCloudService {

    override// noop - FC status has already failed at this point.
    val firecloudStatus: Boolean
        get() {
            try {
                statusApiProvider.get().status()
            } catch (e: ApiException) {
                log.log(Level.WARNING, "Firecloud status check request failed", e)
                val response = e.getResponseBody()
                try {
                    val errorBody = JSONObject(response)
                    val subSystemStatus = errorBody.getJSONObject(STATUS_SUBSYSTEMS_KEY)
                    if (subSystemStatus != null) {
                        return (systemOkay(subSystemStatus!!, THURLOE_STATUS_NAME)
                                && systemOkay(subSystemStatus!!, SAM_STATUS_NAME)
                                && systemOkay(subSystemStatus!!, RAWLS_STATUS_NAME)
                                && systemOkay(subSystemStatus!!, GOOGLE_BUCKETS_STATUS_NAME))
                    }
                } catch (ignored: JSONException) {
                }

                return false
            }

            return true
        }

    override val me: Me
        get() {
            val profileApi = profileApiProvider.get()
            return retryHandler.run<Any> { context -> profileApi.me() }
        }

    override val billingProjectMemberships: List<BillingProjectMembership>
        get() = retryHandler.run<List<BillingProjectMembership>> { context -> profileApiProvider.get().billing() }

    override val nihStatus: NihStatus
        get() {
            val nihApi = nihApiProvider.get()
            return retryHandler.run<Any> { context ->
                try {
                    return@retryHandler.run nihApi . nihStatus ()
                } catch (e: ApiException) {
                    if (e.getCode() === HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
                        return@retryHandler.run null
                    } else {
                        throw e
                    }
                }
            }
        }

    /**
     * Given an email address of an AoU user, generates a FireCloud ApiClient instance with an access
     * token suitable for accessing data on behalf of that user.
     *
     *
     * This relies on domain-wide delegation of authority in Google's OAuth flow; see
     * /api/docs/domain-wide-delegation.md for more details.
     *
     * @param userEmail
     * @return
     */
    @Throws(IOException::class)
    override fun getApiClientWithImpersonation(userEmail: String): ApiClient {
        // Load credentials for the firecloud-admin Service Account. This account has been granted
        // domain-wide delegation for the OAuth scopes required by FireCloud.
        val googleCredential = fcAdminCredsProvider.get()

        val impersonatedUserCredential = serviceAccounts.getImpersonatedCredential(
                googleCredential, userEmail, FIRECLOUD_API_OAUTH_SCOPES)

        val apiClient = FireCloudConfig.buildApiClient(configProvider.get())
        apiClient.setAccessToken(impersonatedUserCredential.accessToken)
        return apiClient
    }

    private fun checkAndAddRegistered(workspaceIngest: WorkspaceIngest) {
        // TODO: add concept of controlled auth domain.
        val registeredDomain = ManagedGroupRef()
        registeredDomain.setMembersGroupName(configProvider.get().firecloud.registeredDomainName)
        workspaceIngest.setAuthorizationDomain(ImmutableList.of<E>(registeredDomain))
    }

    private fun systemOkay(systemList: JSONObject, systemName: String): Boolean {
        return systemList.getJSONObject(systemName).getBoolean("ok")
    }

    override fun registerUser(contactEmail: String, firstName: String, lastName: String) {
        val profileApi = profileApiProvider.get()
        val profile = Profile()
        profile.setFirstName(firstName)
        profile.setLastName(lastName)
        // TODO: make these fields not required in Firecloud and stop passing them in, or prompt for
        // them (RW-29)
        profile.setTitle("None")
        profile.setInstitute("None")
        profile.setInstitutionalProgram("None")
        profile.setProgramLocationCity("None")
        profile.setProgramLocationState("None")
        profile.setProgramLocationCountry("None")
        profile.setPi("None")
        profile.setNonProfitStatus("None")

        retryHandler.run<Any> { context ->
            profileApi.setProfile(profile)
            null
        }
    }

    override fun createAllOfUsBillingProject(projectName: String) {
        require(!projectName.contains(FireCloudService.WORKSPACE_DELIMITER)) {
            String.format(
                    "Attempting to create billing project with name (%s) that contains workspace delimiter (%s)",
                    projectName, FireCloudService.WORKSPACE_DELIMITER)
        }

        val enableVpcFlowLogs = configProvider.get().featureFlags.enableVpcFlowLogs
        val request = CreateRawlsBillingProjectFullRequest()
                .billingAccount("billingAccounts/" + configProvider.get().billing.accountId)
                .projectName(projectName)
                .highSecurityNetwork(enableVpcFlowLogs)
                .enableFlowLogs(enableVpcFlowLogs)

        val enableVpcServicePerimeter = configProvider.get().featureFlags.enableVpcServicePerimeter
        if (enableVpcServicePerimeter) {
            request.servicePerimeter(configProvider.get().firecloud.vpcServicePerimeterName)
        }

        val billingApi = billingApiProvider.get()
        retryHandler.run<Any> { context ->
            billingApi.createBillingProjectFull(request)
            null
        }
    }

    override fun getBillingProjectStatus(projectName: String): BillingProjectStatus {
        return retryHandler.run<Any> { context -> billingApiProvider.get().billingProjectStatus(projectName) }
    }

    private fun addRoleToBillingProject(email: String, projectName: String, role: String) {
        val billingApi = billingApiProvider.get()
        retryHandler.run<Any> { context ->
            billingApi.addUserToBillingProject(projectName, role, email)
            null
        }
    }

    override fun addUserToBillingProject(email: String, projectName: String) {
        addRoleToBillingProject(email, projectName, USER_FC_ROLE)
    }

    override fun removeUserFromBillingProject(email: String, projectName: String) {
        val billingApi = billingApiProvider.get()
        retryHandler.run<Any> { context ->
            billingApi.removeUserFromBillingProject(projectName, USER_FC_ROLE, email)
            null
        }
    }

    override fun addOwnerToBillingProject(ownerEmail: String, projectName: String) {
        addRoleToBillingProject(ownerEmail, projectName, OWNER_FC_ROLE)
    }

    override fun removeOwnerFromBillingProject(
            projectName: String, ownerEmailToRemove: String, callerAccessToken: String) {

        val apiClient = FireCloudConfig.buildApiClient(configProvider.get())
        apiClient.setAccessToken(callerAccessToken)

        // use a private instance of BillingApi instead of the provider
        // b/c we don't want to modify its ApiClient globally
        val billingApi = BillingApi()
        billingApi.setApiClient(apiClient)
        retryHandler.run<Any> { context ->
            billingApi.removeUserFromBillingProject(projectName, OWNER_FC_ROLE, ownerEmailToRemove)
            null
        }
    }

    override fun createWorkspace(projectName: String, workspaceName: String): Workspace {
        val workspacesApi = workspacesApiProvider.get()
        val workspaceIngest = WorkspaceIngest()
        workspaceIngest.setName(workspaceName)
        workspaceIngest.setNamespace(projectName)
        checkAndAddRegistered(workspaceIngest)
        return retryHandler.run<Any> { context -> workspacesApi.createWorkspace(workspaceIngest) }
    }

    override fun cloneWorkspace(
            fromProject: String, fromName: String, toProject: String, toName: String): Workspace {
        val workspacesApi = workspacesApiProvider.get()
        val workspaceIngest = WorkspaceIngest()
        workspaceIngest.setNamespace(toProject)
        workspaceIngest.setName(toName)
        checkAndAddRegistered(workspaceIngest)
        return retryHandler.run<Any> { context -> workspacesApi.cloneWorkspace(fromProject, fromName, workspaceIngest) }
    }

    override fun updateWorkspaceACL(
            projectName: String, workspaceName: String, aclUpdates: List<WorkspaceACLUpdate>): WorkspaceACLUpdateResponseList {
        val workspacesApi = workspacesApiProvider.get()
        // TODO: set authorization domain here
        return retryHandler.run<Any> { context -> workspacesApi.updateWorkspaceACL(projectName, workspaceName, false, aclUpdates) }
    }

    override fun getWorkspaceAcl(projectName: String, workspaceName: String): WorkspaceACL {
        val workspaceAclsApi = workspaceAclsApiProvider.get()
        return retryHandler.run<Any> { context -> workspaceAclsApi.getWorkspaceAcl(projectName, workspaceName) }
    }

    override fun getWorkspace(projectName: String, workspaceName: String): WorkspaceResponse {
        val workspacesApi = workspacesApiProvider.get()
        return retryHandler.run<Any> { context ->
            workspacesApi.getWorkspace(
                    projectName, workspaceName, FIRECLOUD_GET_WORKSPACE_REQUIRED_FIELDS)
        }
    }

    override fun getWorkspaces(fields: List<String>): List<WorkspaceResponse> {
        return retryHandler.run<List<WorkspaceResponse>> { context -> workspacesApiProvider.get().listWorkspaces(fields) }
    }

    override fun deleteWorkspace(projectName: String, workspaceName: String) {
        val workspacesApi = workspacesApiProvider.get()
        retryHandler.run<Any> { context ->
            workspacesApi.deleteWorkspace(projectName, workspaceName)
            null
        }
    }

    override fun getGroup(groupName: String): ManagedGroupWithMembers {
        val groupsApi = groupsApiProvider.get()
        return retryHandler.run<Any> { context -> groupsApi.getGroup(groupName) }
    }

    override fun createGroup(groupName: String): ManagedGroupWithMembers {
        val groupsApi = groupsApiProvider.get()
        return retryHandler.run<Any> { context -> groupsApi.createGroup(groupName) }
    }

    override fun addUserToGroup(email: String, groupName: String) {
        val groupsApi = groupsApiProvider.get()
        retryHandler.run<Any> { context ->
            groupsApi.addUserToGroup(groupName, MEMBER_ROLE, email)
            null
        }
    }

    override fun removeUserFromGroup(email: String, groupName: String) {
        val groupsApi = groupsApiProvider.get()
        retryHandler.run<Any> { context ->
            groupsApi.removeUserFromGroup(groupName, MEMBER_ROLE, email)
            null
        }
    }

    override fun isUserMemberOfGroup(email: String, groupName: String): Boolean {
        return retryHandler.run { context ->
            val group = groupsApiProvider.get().getGroup(groupName)
            group.getMembersEmails().contains(email) || group.getAdminsEmails().contains(email)
        }
    }

    override fun staticNotebooksConvert(notebook: ByteArray): String {
        return retryHandler.run { context -> staticNotebooksApiProvider.get().convertNotebook(notebook) }
    }

    override fun postNihCallback(wrapper: JWTWrapper): NihStatus {
        val nihApi = nihApiProvider.get()
        return retryHandler.run<Any> { context -> nihApi.nihCallback(wrapper) }
    }

    companion object {

        private val log = Logger.getLogger(FireCloudServiceImpl::class.java.name)

        private val MEMBER_ROLE = "member"
        private val STATUS_SUBSYSTEMS_KEY = "systems"

        private val USER_FC_ROLE = "user"
        private val OWNER_FC_ROLE = "owner"
        private val THURLOE_STATUS_NAME = "Thurloe"
        private val SAM_STATUS_NAME = "Sam"
        private val RAWLS_STATUS_NAME = "Rawls"
        private val GOOGLE_BUCKETS_STATUS_NAME = "GoogleBuckets"

        // The set of Google OAuth scopes required for access to FireCloud APIs. If FireCloud ever changes
        // its API scopes (see https://api.firecloud.org/api-docs.yaml), we'll need to update this list.
        val FIRECLOUD_API_OAUTH_SCOPES: List<String> = ImmutableList.of(
                "openid",
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/cloud-billing")

        // All options are defined in this document:
        // https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#
        val FIRECLOUD_GET_WORKSPACE_REQUIRED_FIELDS: List<String> = ImmutableList.of(
                "accessLevel",
                "workspace.workspaceId",
                "workspace.name",
                "workspace.namespace",
                "workspace.bucketName",
                "workspace.createdBy")
    }
}
