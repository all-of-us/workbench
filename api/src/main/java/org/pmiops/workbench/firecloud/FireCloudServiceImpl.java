package org.pmiops.workbench.firecloud;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.BillingV2Api;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectStatus;
import org.pmiops.workbench.firecloud.model.FirecloudCreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupRef;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudProfile;
import org.pmiops.workbench.firecloud.model.FirecloudUpdateRawlsBillingAccountRequest;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceIngest;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceRequestClone;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Service;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class FireCloudServiceImpl implements FireCloudService {

  private static final Logger log = Logger.getLogger(FireCloudServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> configProvider;

  private final Provider<BillingApi> billingApiProvider;
  private final Provider<BillingV2Api> serviceAccountBillingV2ApiProvider;
  private final Provider<BillingV2Api> endUserBillingV2ApiProvider;
  private final Provider<GroupsApi> groupsApiProvider;
  private final Provider<NihApi> nihApiProvider;
  private final Provider<ProfileApi> profileApiProvider;
  private final Provider<StatusApi> statusApiProvider;
  private final Provider<LoadingCache<String, FirecloudManagedGroupWithMembers>>
      requestScopedGroupCacheProvider;

  // We call some of the endpoints in these APIs with the user's credentials
  // and others with the app's Service Account credentials

  private final Provider<StaticNotebooksApi> endUserStaticNotebooksApiProvider;
  private final Provider<StaticNotebooksApi> serviceAccountStaticNotebooksApiProvider;

  private final Provider<WorkspacesApi> endUserWorkspacesApiProvider;
  private final Provider<WorkspacesApi> serviceAccountWorkspaceApiProvider;

  private final FirecloudRetryHandler retryHandler;
  private final IamCredentialsClient iamCredentialsClient;
  private final HttpTransport httpTransport;

  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "firecloud-admin";

  private static final String MEMBER_ROLE = "member";
  private static final String STATUS_SUBSYSTEMS_KEY = "systems";

  private static final String OWNER_FC_ROLE = "owner";
  private static final String THURLOE_STATUS_NAME = "Thurloe";
  private static final String SAM_STATUS_NAME = "Sam";
  private static final String RAWLS_STATUS_NAME = "Rawls";
  private static final String GOOGLE_BUCKETS_STATUS_NAME = "GoogleBuckets";

  // The set of Google OAuth scopes required for access to FireCloud APIs. If FireCloud ever changes
  // its API scopes (see https://api.firecloud.org/api-docs.yaml), we'll need to update this list.
  public static final List<String> FIRECLOUD_API_OAUTH_SCOPES =
      ImmutableList.of(
          "openid",
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing");

  // All options are defined in this document:
  // https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#
  public static final List<String> FIRECLOUD_WORKSPACE_REQUIRED_FIELDS =
      ImmutableList.of(
          "accessLevel",
          "workspace.workspaceId",
          "workspace.name",
          "workspace.namespace",
          "workspace.googleProject",
          "workspace.bucketName",
          "workspace.createdBy");

  @Autowired
  public FireCloudServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Provider<ProfileApi> profileApiProvider,
      Provider<BillingApi> billingApiProvider,
      @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_BILLING_V2_API)
          Provider<BillingV2Api> serviceAccountBillingV2ApiProvider,
      @Qualifier(FireCloudConfig.END_USER_STATIC_BILLING_V2_API)
          Provider<BillingV2Api> endUserBillingV2ApiProvider,
      Provider<GroupsApi> groupsApiProvider,
      Provider<NihApi> nihApiProvider,
      @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
          Provider<WorkspacesApi> endUserWorkspacesApiProvider,
      @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_WORKSPACE_API)
          Provider<WorkspacesApi> serviceAccountWorkspaceApiProvider,
      Provider<StatusApi> statusApiProvider,
      @Qualifier(FireCloudConfig.END_USER_STATIC_NOTEBOOKS_API)
          Provider<StaticNotebooksApi> endUserStaticNotebooksApiProvider,
      @Qualifier(FireCloudConfig.SERVICE_ACCOUNT_STATIC_NOTEBOOKS_API)
          Provider<StaticNotebooksApi> serviceAccountStaticNotebooksApiProvider,
      @Qualifier(FireCloudCacheConfig.SERVICE_ACCOUNT_REQUEST_SCOPED_GROUP_CACHE)
          Provider<LoadingCache<String, FirecloudManagedGroupWithMembers>>
              requestScopedGroupCacheProvider,
      FirecloudRetryHandler retryHandler,
      IamCredentialsClient iamCredentialsClient,
      HttpTransport httpTransport) {
    this.configProvider = configProvider;
    this.profileApiProvider = profileApiProvider;
    this.billingApiProvider = billingApiProvider;
    this.serviceAccountBillingV2ApiProvider = serviceAccountBillingV2ApiProvider;
    this.endUserBillingV2ApiProvider = endUserBillingV2ApiProvider;
    this.groupsApiProvider = groupsApiProvider;
    this.nihApiProvider = nihApiProvider;
    this.endUserWorkspacesApiProvider = endUserWorkspacesApiProvider;
    this.serviceAccountWorkspaceApiProvider = serviceAccountWorkspaceApiProvider;
    this.statusApiProvider = statusApiProvider;
    this.retryHandler = retryHandler;
    this.endUserStaticNotebooksApiProvider = endUserStaticNotebooksApiProvider;
    this.serviceAccountStaticNotebooksApiProvider = serviceAccountStaticNotebooksApiProvider;
    this.requestScopedGroupCacheProvider = requestScopedGroupCacheProvider;
    this.iamCredentialsClient = iamCredentialsClient;
    this.httpTransport = httpTransport;
  }

  /**
   * Given an email address of an AoU user, generates a FireCloud ApiClient instance with an access
   * token suitable for accessing data on behalf of that user.
   *
   * <p>This relies on domain-wide delegation of authority in Google's OAuth flow; see
   * /api/docs/domain-wide-delegation.md for more details.
   *
   * @param userEmail
   * @return
   */
  public ApiClient getApiClientWithImpersonation(String userEmail) throws IOException {
    final OAuth2Credentials delegatedCreds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail(
                ADMIN_SERVICE_ACCOUNT_NAME, configProvider.get().server.projectId),
            userEmail,
            FIRECLOUD_API_OAUTH_SCOPES,
            iamCredentialsClient,
            httpTransport);
    delegatedCreds.refreshIfExpired();

    ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
    apiClient.setAccessToken(delegatedCreds.getAccessToken().getTokenValue());
    return apiClient;
  }

  @Override
  @VisibleForTesting
  public String getApiBasePath() {
    return statusApiProvider.get().getApiClient().getBasePath();
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
          return systemOkay(subSystemStatus, THURLOE_STATUS_NAME)
              && systemOkay(subSystemStatus, SAM_STATUS_NAME)
              && systemOkay(subSystemStatus, RAWLS_STATUS_NAME)
              && systemOkay(subSystemStatus, GOOGLE_BUCKETS_STATUS_NAME);
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
  public FirecloudMe getMe() {
    ProfileApi profileApi = profileApiProvider.get();
    return retryHandler.run((context) -> profileApi.me());
  }

  @Override
  public void registerUser(String contactEmail, String firstName, String lastName) {
    ProfileApi profileApi = profileApiProvider.get();
    FirecloudProfile profile = new FirecloudProfile();
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

    retryHandler.run(
        (context) -> {
          profileApi.setProfile(profile);
          return null;
        });
  }

  @Override
  public void createAllOfUsBillingProject(String projectName, String servicePerimeter) {
    if (projectName.contains(WORKSPACE_DELIMITER)) {
      throw new IllegalArgumentException(
          String.format(
              "Attempting to create billing project with name (%s) that contains workspace delimiter (%s)",
              projectName, WORKSPACE_DELIMITER));
    }

    FirecloudCreateRawlsBillingProjectFullRequest request =
        new FirecloudCreateRawlsBillingProjectFullRequest()
            .billingAccount(configProvider.get().billing.freeTierBillingAccountName())
            .projectName(projectName)
            .highSecurityNetwork(true)
            .enableFlowLogs(true)
            .privateIpGoogleAccess(true)
            .servicePerimeter(servicePerimeter);

    if (isFireCloudBillingV2ApiEnabled()) {
      BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();
      retryHandler.run(
          (context) -> {
            billingV2Api.createBillingProjectFullV2(request);
            return null;
          });
    } else {
      BillingApi billingApi = billingApiProvider.get();
      retryHandler.run(
          (context) -> {
            billingApi.createBillingProjectFull(request);
            return null;
          });
    }
  }

  @Override
  public void deleteBillingProject(String billingProject) {
    if (isFireCloudBillingV2ApiEnabled()) {
      BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();
      retryHandler.run(
          (context) -> {
            billingV2Api.deleteBillingProject(billingProject);
            return null;
          });
    } else {
      BillingApi billingApi = billingApiProvider.get();
      retryHandler.run(
          (context) -> {
            billingApi.deleteBillingProject(billingProject);
            return null;
          });
    }
  }

  @Override
  public FirecloudBillingProjectStatus getBillingProjectStatus(String projectName) {
    return retryHandler.run(
        (context) -> billingApiProvider.get().billingProjectStatus(projectName));
  }

  @Override
  public void updateBillingAccount(String billingProject, String billingAccount) {
    retryHandler.run(
        (context) -> {
          endUserBillingV2ApiProvider
              .get()
              .updateBillingProjectBillingAccount(
                  new FirecloudUpdateRawlsBillingAccountRequest().billingAccount(billingAccount),
                  billingProject);
          return null;
        });
  }

  @Override
  public void updateBillingAccountAsService(String billingProject, String billingAccount) {
    retryHandler.run(
        (context) -> {
          serviceAccountBillingV2ApiProvider
              .get()
              .updateBillingProjectBillingAccount(
                  new FirecloudUpdateRawlsBillingAccountRequest().billingAccount(billingAccount),
                  billingProject);
          return null;
        });
  }

  private void addRoleToBillingProject(String email, String projectName, String role) {
    Preconditions.checkArgument(email.contains("@"));

    BillingApi billingApi = billingApiProvider.get();
    retryHandler.run(
        (context) -> {
          billingApi.addUserToBillingProject(projectName, role, email);
          return null;
        });
  }

  @Override
  public void addOwnerToBillingProject(String ownerEmail, String projectName) {
    addRoleToBillingProject(ownerEmail, projectName, OWNER_FC_ROLE);
  }

  @Override
  public void removeOwnerFromBillingProject(
      String ownerEmailToRemove, String projectName, Optional<String> callerAccessToken) {
    Preconditions.checkArgument(ownerEmailToRemove.contains("@"));

    final BillingApi scopedBillingApi;

    if (callerAccessToken.isPresent()) {
      // use a private instance of BillingApi instead of the provider
      // b/c we don't want to modify its ApiClient globally

      final ApiClient apiClient = FireCloudConfig.buildApiClient(configProvider.get());
      apiClient.setAccessToken(callerAccessToken.get());
      scopedBillingApi = new BillingApi();
      scopedBillingApi.setApiClient(apiClient);
    } else {
      scopedBillingApi = billingApiProvider.get();
    }

    retryHandler.run(
        (context) -> {
          scopedBillingApi.removeUserFromBillingProject(
              projectName, OWNER_FC_ROLE, ownerEmailToRemove);
          return null;
        });
  }

  @Override
  public FirecloudWorkspace createWorkspace(
      String projectName, String workspaceName, String authDomainName) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    FirecloudWorkspaceIngest workspaceIngest =
        new FirecloudWorkspaceIngest()
            .namespace(projectName)
            .name(workspaceName)
            .authorizationDomain(
                ImmutableList.of(new FirecloudManagedGroupRef().membersGroupName(authDomainName)));

    return retryHandler.run((context) -> workspacesApi.createWorkspace(workspaceIngest));
  }

  @Override
  public FirecloudWorkspace cloneWorkspace(
      String fromProject, String fromName, String toProject, String toName, String authDomainName) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    FirecloudWorkspaceRequestClone cloneRequest =
        new FirecloudWorkspaceRequestClone()
            .namespace(toProject)
            .name(toName)
            // We copy only the notebooks/ subdirectory as a heuristic to avoid unintentionally
            // propagating copies of large data files elswhere in the bucket.
            .copyFilesWithPrefix("notebooks/")
            .authorizationDomain(
                ImmutableList.of(new FirecloudManagedGroupRef().membersGroupName(authDomainName)));

    return retryHandler.run(
        (context) -> workspacesApi.cloneWorkspace(cloneRequest, fromProject, fromName));
  }

  @Override
  public List<FirecloudBillingProjectMembership> getBillingProjectMemberships() {
    return retryHandler.run((context) -> profileApiProvider.get().billing());
  }

  @Override
  public FirecloudWorkspaceACLUpdateResponseList updateWorkspaceACL(
      String projectName, String workspaceName, List<FirecloudWorkspaceACLUpdate> aclUpdates) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    // TODO: set authorization domain here
    return retryHandler.run(
        (context) ->
            workspacesApi.updateWorkspaceACL(aclUpdates, projectName, workspaceName, false));
  }

  @Override
  public FirecloudWorkspaceACL getWorkspaceAclAsService(String projectName, String workspaceName) {
    WorkspacesApi workspacesApi = serviceAccountWorkspaceApiProvider.get();
    return retryHandler.run((context) -> workspacesApi.getWorkspaceAcl(projectName, workspaceName));
  }

  @Override
  public FirecloudWorkspaceResponse getWorkspaceAsService(String projectName, String workspaceName)
      throws WorkbenchException {
    WorkspacesApi workspacesApi = serviceAccountWorkspaceApiProvider.get();
    return retryHandler.run(
        (context) ->
            workspacesApi.getWorkspace(
                projectName, workspaceName, FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public FirecloudWorkspaceResponse getWorkspace(String projectName, String workspaceName)
      throws WorkbenchException {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    return retryHandler.run(
        (context) ->
            workspacesApi.getWorkspace(
                projectName, workspaceName, FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public Optional<FirecloudWorkspaceResponse> getWorkspace(DbWorkspace dbWorkspace)
      throws WorkbenchException {
    try {
      final FirecloudWorkspaceResponse result =
          getWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
      return Optional.of(result);
    } catch (WorkbenchException e) {
      log.log(
          Level.INFO,
          e,
          () ->
              String.format(
                  "Exception encountered retrieving workspace with DbWorkspace %s",
                  dbWorkspace.toString()));
      return Optional.empty();
    }
  }

  @Override
  public List<FirecloudWorkspaceResponse> getWorkspaces() throws WorkbenchException {
    return retryHandler.run(
        (context) ->
            endUserWorkspacesApiProvider.get().listWorkspaces(FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public void deleteWorkspace(String projectName, String workspaceName) throws WorkbenchException {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    retryHandler.run(
        (context) -> {
          workspacesApi.deleteWorkspace(projectName, workspaceName);
          return null;
        });
  }

  @Override
  public FirecloudManagedGroupWithMembers getGroup(String groupName) throws WorkbenchException {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) -> groupsApi.getGroup(groupName));
  }

  @Override
  public FirecloudManagedGroupWithMembers createGroup(String groupName) throws WorkbenchException {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) -> groupsApi.createGroup(groupName));
  }

  @Override
  public void addUserToGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run(
        (context) -> {
          groupsApi.addUserToGroup(groupName, MEMBER_ROLE, email);
          return null;
        });
  }

  @Override
  public void removeUserFromGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run(
        (context) -> {
          groupsApi.removeUserFromGroup(groupName, MEMBER_ROLE, email);
          return null;
        });
  }

  @Override
  public boolean isUserMemberOfGroupWithCache(String email, String groupName) {
    return retryHandler.run(
        (context) -> {
          FirecloudManagedGroupWithMembers group = null;
          try {
            group = requestScopedGroupCacheProvider.get().get(groupName);
          } catch (ExecutionException e) {
            // This is not expected, but might be possible if we access an entry at the exact time
            // at which is is expiring from the cache. Just retry.
            throw new RetryException("cache concurrent access failure", e);
          }
          return group.getMembersEmails().contains(email)
              || group.getAdminsEmails().contains(email);
        });
  }

  @Override
  public String staticNotebooksConvert(byte[] notebook) {
    return retryHandler.run(
        (context) -> endUserStaticNotebooksApiProvider.get().convertNotebook(notebook));
  }

  @Override
  public String staticNotebooksConvertAsService(byte[] notebook) {
    return retryHandler.run(
        (context) -> serviceAccountStaticNotebooksApiProvider.get().convertNotebook(notebook));
  }

  @Override
  public FirecloudNihStatus getNihStatus() {
    NihApi nihApi = nihApiProvider.get();
    return retryHandler.run(
        (context) -> {
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

  private boolean isFireCloudBillingV2ApiEnabled() {
    return configProvider.get().featureFlags.enableFireCloudV2Billing;
  }
}
