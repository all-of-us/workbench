package org.pmiops.workbench.firecloud;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership;
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.JWTWrapper;
import org.pmiops.workbench.firecloud.model.ManagedGroupAccessResponse;
import org.pmiops.workbench.firecloud.model.ManagedGroupRef;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.firecloud.model.Profile;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.WorkspaceIngest;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class FireCloudServiceImpl implements FireCloudService {
  private static final Logger log = Logger.getLogger(FireCloudServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<ProfileApi> profileApiProvider;
  private final Provider<BillingApi> billingApiProvider;
  private final Provider<GroupsApi> groupsApiProvider;
  private final Provider<GroupsApi> endUserGroupsApiProvider;
  private final Provider<NihApi> nihApiProvider;
  private final Provider<WorkspacesApi> workspacesApiProvider;
  private final Provider<StatusApi> statusApiProvider;
  private final Provider<GoogleCredential> googleCredentialProvider;
  private final FirecloudRetryHandler retryHandler;
  private final Provider<GoogleCredential> fcAdminCredsProvider;
  private final ServiceAccounts serviceAccounts;

  private static final String MEMBER_ROLE = "member";
  private static final String STATUS_SUBSYSTEMS_KEY = "systems";

  private static final String USER_FC_ROLE = "user";
  private static final String THURLOE_STATUS_NAME = "Thurloe";
  private static final String SAM_STATUS_NAME = "Sam";
  private static final String RAWLS_STATUS_NAME = "Rawls";
  private static final String GOOGLE_BUCKETS_STATUS_NAME = "GoogleBuckets";

  // The set of Google OAuth scopes required for access to FireCloud APIs. If FireCloud ever changes
  // its API scopes (see https://api.firecloud.org/api-docs.yaml), we'll need to update this list.
  public static final List<String> FIRECLOUD_API_OAUTH_SCOPES = Arrays.asList(
      "openid",
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/cloud-billing");

  @Autowired
  public FireCloudServiceImpl(Provider<WorkbenchConfig> configProvider,
      Provider<ProfileApi> profileApiProvider,
      Provider<BillingApi> billingApiProvider,
      @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_GROUPS_API) Provider<GroupsApi> groupsApiProvider,
      @Qualifier(FireCloudConfig.END_USER_GROUPS_API) Provider<GroupsApi> endUserGroupsApiProvider,
      Provider<NihApi> nihApiProvider, Provider<WorkspacesApi> workspacesApiProvider,
      Provider<StatusApi> statusApiProvider,
      FirecloudRetryHandler retryHandler,
      ServiceAccounts serviceAccounts,
      @Qualifier(Constants.FIRECLOUD_ADMIN_CREDS) Provider<GoogleCredential> fcAdminCredsProvider) {
    this.configProvider = configProvider;
    this.profileApiProvider = profileApiProvider;
    this.billingApiProvider = billingApiProvider;
    this.groupsApiProvider = groupsApiProvider;
    this.endUserGroupsApiProvider = endUserGroupsApiProvider;
    this.nihApiProvider = nihApiProvider;
    this.workspacesApiProvider = workspacesApiProvider;
    this.statusApiProvider = statusApiProvider;
    this.googleCredentialProvider = googleCredentialProvider;
    this.workbenchConfig = workbenchConfig;
    this.retryHandler = retryHandler;
    this.serviceAccounts = serviceAccounts;
    this.fcAdminCredsProvider = fcAdminCredsProvider;
  }

  /**
   * Given an email address of an AoU user, generates a FireCloud ApiClient instance with an access
   * token suitable for accessing data on behalf of that user.
   * <p>
   * This relies on domain-wide delegation of authority in Google's OAuth flow; see
   * /api/docs/domain-wide-delegation.md for more details.
   *
   * @param userEmail
   * @return
   */
  public ApiClient getApiClientWithImpersonation(String userEmail) throws IOException {
    // Load credentials for the firecloud-admin Service Account. This account has been granted
    // domain-wide delegation for the OAuth scopes required by FireCloud.
    GoogleCredential googleCredential = fcAdminCredsProvider.get();

    GoogleCredential impersonatedUserCredential = serviceAccounts.getImpersonatedCredential(
        googleCredential, userEmail, FIRECLOUD_API_OAUTH_SCOPES
    );

    ApiClient apiClient = new ApiClient()
      .setDebugging(true)
      .setBasePath(configProvider.get().firecloud.baseUrl)
      .setDebugging(configProvider.get().firecloud.debugEndpoints)
      .addDefaultHeader(FireCloudConfig.X_APP_ID_HEADER, FireCloudConfig.X_APP_ID_HEADER_VALUE);
    apiClient.setAccessToken(impersonatedUserCredential.getAccessToken());
    return apiClient;
  }

  private void checkAndAddRegistered(WorkspaceIngest workspaceIngest) {
    // TODO: add concept of controlled auth domain.
    if (configProvider.get().firecloud.enforceRegistered) {
      ManagedGroupRef registeredDomain = new ManagedGroupRef();
      registeredDomain.setMembersGroupName(configProvider.get().firecloud.registeredDomainName);
      workspaceIngest.setAuthorizationDomain(ImmutableList.of(registeredDomain));
    }
  }

  @Override
  public boolean getFirecloudStatus() {
    try {
      statusApiProvider.get().status();
    } catch (ApiException e) {
      log.log(Level.WARNING, "Firecloud status check request failed", e);
      String response = e.getResponseBody();
      try {
        JSONObject errorBody = new JSONObject(response);
        JSONObject subSystemStatus = errorBody.getJSONObject(STATUS_SUBSYSTEMS_KEY);
        if (subSystemStatus != null) {
          return systemOkay(subSystemStatus, THURLOE_STATUS_NAME) &&
              systemOkay(subSystemStatus, SAM_STATUS_NAME) &&
              systemOkay(subSystemStatus, RAWLS_STATUS_NAME) &&
              systemOkay(subSystemStatus, GOOGLE_BUCKETS_STATUS_NAME);
        }
      } catch (JSONException ignored) {
        // noop - FC status has already failed at this point.
      }
      return false;
    }
    return true;
  }

  private boolean systemOkay(JSONObject systemList, String systemName) {
    return systemList.getJSONObject(systemName).getBoolean("ok");
  }

  @Override
  public Me getMe() {
    ProfileApi profileApi = profileApiProvider.get();
    return retryHandler.run((context) -> profileApi.me());
  }

  @Override
  public void registerUser(String contactEmail, String firstName, String lastName) {
    ProfileApi profileApi = profileApiProvider.get();
    Profile profile = new Profile();
    profile.setFirstName(firstName);
    profile.setLastName(lastName);
    // TODO: make these fields not required in Firecloud and stop passing them in, or prompt for
    // them (RW-29)
    profile.setTitle("None");
    profile.setInstitute("None");
    profile.setInstitutionalProgram("None");
    profile.setProgramLocationCity("None");
    profile.setProgramLocationState("None");
    profile.setProgramLocationCountry("None");
    profile.setPi("None");
    profile.setNonProfitStatus("None");

    retryHandler.run((context) -> {
      profileApi.setProfile(profile);
      return null;
    });
  }

  @Override
  public void createAllOfUsBillingProject(String projectName)  {
    BillingApi billingApi = billingApiProvider.get();
    CreateRawlsBillingProjectFullRequest request = new CreateRawlsBillingProjectFullRequest();
    request.setBillingAccount("billingAccounts/"+configProvider.get().firecloud.billingAccountId);
    request.setProjectName(projectName);
    retryHandler.run((context) -> {
      billingApi.createBillingProjectFull(request);
      return null;
    });
  }

  @Override
  public void addUserToBillingProject(String email, String projectName) {
    BillingApi billingApi = billingApiProvider.get();
    retryHandler.run((context) -> {
      billingApi.addUserToBillingProject(projectName, USER_FC_ROLE, email);
      return null;
    });
  }

  @Override
  public void removeUserFromBillingProject(String email, String projectName) {
    BillingApi billingApi = billingApiProvider.get();
    retryHandler.run((context) -> {
      billingApi.removeUserFromBillingProject(projectName, USER_FC_ROLE, email);
      return null;
    });
  }

  @Override
  public void createWorkspace(String projectName, String workspaceName) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    WorkspaceIngest workspaceIngest = new WorkspaceIngest();
    workspaceIngest.setName(workspaceName);
    workspaceIngest.setNamespace(projectName);
    checkAndAddRegistered(workspaceIngest);
    retryHandler.run((context) -> {
      workspacesApi.createWorkspace(workspaceIngest);
      return null;
    });
  }

  @Override
  public void grantGoogleRoleToUser(String projectName, String role, String email) {
    BillingApi billingApi = billingApiProvider.get();
    retryHandler.run((context) -> {
      billingApi.grantGoogleRoleToUser(projectName, role, email);
      return null;
    });
  }

  @Override
  public void cloneWorkspace(String fromProject, String fromName, String toProject, String toName) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    WorkspaceIngest workspaceIngest = new WorkspaceIngest();
    workspaceIngest.setNamespace(toProject);
    workspaceIngest.setName(toName);
    checkAndAddRegistered(workspaceIngest);
    retryHandler.run((context) -> {
      workspacesApi.cloneWorkspace(fromProject, fromName, workspaceIngest);
      return null;
    });
  }


  @Override
  public List<BillingProjectMembership> getBillingProjectMemberships() {
    return retryHandler.run((context) -> profileApiProvider.get().billing());
  }

  @Override
  public WorkspaceACLUpdateResponseList updateWorkspaceACL(String projectName, String workspaceName,
      List<WorkspaceACLUpdate> aclUpdates) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    // TODO: set authorization domain here
    return retryHandler.run((context) ->
      workspacesApi.updateWorkspaceACL(projectName, workspaceName, false, aclUpdates));
  }

  @Override
  public WorkspaceResponse getWorkspace(String projectName, String workspaceName) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    return retryHandler.run((context) ->
      workspacesApi.getWorkspace(projectName, workspaceName));
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() {
    return retryHandler.run((context) ->
        workspacesApiProvider.get().listWorkspaces());
  }

  @Override
  public void deleteWorkspace(String projectName, String workspaceName) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    retryHandler.run((context) -> {
      workspacesApi.deleteWorkspace(projectName, workspaceName);
      return null;
    });
  }

  @Override
  public ManagedGroupWithMembers createGroup(String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) ->
      groupsApi.createGroup(groupName));
  }

  @Override
  public void addUserToGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run((context) -> {
      groupsApi.addUserToGroup(groupName, MEMBER_ROLE, email);
      return null;
    });
  }

  @Override
  public void removeUserFromGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run((context) -> {
      groupsApi.removeUserFromGroup(groupName, MEMBER_ROLE, email);
      return null;
    });
  }

  @Override
  public boolean isUserMemberOfGroup(String groupName) {
    return retryHandler.run((context) -> {
      // There is no endpoint in FireCloud for checking whether a user is a member of a particular
      // group; so instead, fetch all the group memberships. (There won't be that many for our
      // users anyway.)
      for (ManagedGroupAccessResponse group : endUserGroupsApiProvider.get().getGroups()) {
        if (groupName.equals(group.getGroupName()) && MEMBER_ROLE.equalsIgnoreCase(group.getRole())) {
          return true;
        }
      }
      return false;
    });
  }

  @Override
  public NihStatus getNihStatus() {
    NihApi nihApi = nihApiProvider.get();
    return retryHandler.run((context) -> {
      try {
        return nihApi.nihStatus();
      } catch (ApiException e) {
        if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
          return null;
        } else {
          throw e;
        }
      }
    });
  }

  @Override
  public NihStatus postNihCallback(JWTWrapper wrapper) {
    NihApi nihApi = nihApiProvider.get();
    return retryHandler.run((context) -> {
      return nihApi.nihCallback(wrapper);
    });
  }

}
