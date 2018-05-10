package org.pmiops.workbench.firecloud;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership;
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.ManagedGroupRef;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.firecloud.model.Profile;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList;
import org.pmiops.workbench.firecloud.model.WorkspaceIngest;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class FireCloudServiceImpl implements FireCloudService {
  private static final Logger log = Logger.getLogger(FireCloudServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<ProfileApi> profileApiProvider;
  private final Provider<BillingApi> billingApiProvider;
  private final Provider<GroupsApi> groupsApiProvider;
  private final Provider<WorkspacesApi> workspacesApiProvider;
  private final FirecloudRetryHandler retryHandler;

  private static final String STATUS_SUBSYSTEMS_KEY = "systems";

  private static final String USER_FC_ROLE = "user";
  private static final String THURLOE_STATUS_NAME = "Thurloe";
  private static final String SAM_STATUS_NAME = "Sam";
  private static final String RAWLS_STATUS_NAME = "Rawls";
  private static final String GOOGLE_BUCKETS_STATUS_NAME = "GoogleBuckets";

  @Autowired
  public FireCloudServiceImpl(Provider<WorkbenchConfig> configProvider,
      Provider<ProfileApi> profileApiProvider,
      Provider<BillingApi> billingApiProvider,
      Provider<GroupsApi> groupsApiProvider,
      Provider<WorkspacesApi> workspacesApiProvider,
      FirecloudRetryHandler retryHandler) {
    this.configProvider = configProvider;
    this.profileApiProvider = profileApiProvider;
    this.billingApiProvider = billingApiProvider;
    this.groupsApiProvider = groupsApiProvider;
    this.workspacesApiProvider = workspacesApiProvider;
    this.retryHandler = retryHandler;
  }

  private static boolean isServiceUnavailableException(Exception e) {
    if (e instanceof ApiException) {
      int code = ((ApiException) e).getCode();
      return (code > 500 && code < 600);
    }
    return false;
  }


  @Override
  public boolean getFirecloudStatus() {
    try {
      new StatusApi().status();
    } catch (ApiException e) {
      log.log(Level.WARNING, "Firecloud status check request failed", e);
      String response = e.getResponseBody();
      JSONObject errorBody = new JSONObject(response);
      JSONObject subSystemStatus = errorBody.getJSONObject(STATUS_SUBSYSTEMS_KEY);
      if (subSystemStatus != null) {
        return systemOkay(subSystemStatus, THURLOE_STATUS_NAME) &&
            systemOkay(subSystemStatus, SAM_STATUS_NAME) &&
            systemOkay(subSystemStatus, RAWLS_STATUS_NAME) &&
            systemOkay(subSystemStatus, GOOGLE_BUCKETS_STATUS_NAME);
      }
      return false;
    }
    return true;
  }

  private boolean systemOkay(JSONObject systemList, String systemName) {
    return systemList.getJSONObject(systemName).getBoolean("ok");
  }

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    try {
      Me me = retryHandler.runAndThrowChecked((context) -> profileApi.me());
      // Users can only use FireCloud if the Google and LDAP flags are enabled.
      return me.getEnabled() != null
          && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
    } catch (ApiException e) {
      if (e.getCode() == NOT_FOUND.value() || e.getCode() == UNAUTHORIZED.value()) {
        return false;
      }
      ExceptionUtils.convertFirecloudException(e);
      return false;
    }
  }

  @Override
  public Me getMe() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    return retryHandler.run((context) -> profileApi.me());
  }

  @Override
  public void registerUser(String contactEmail, String firstName, String lastName)
      throws ApiException {
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
  public void createAllOfUsBillingProject(String projectName) throws ApiException {
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
  public void addUserToBillingProject(String email, String projectName) throws ApiException {
    BillingApi billingApi = billingApiProvider.get();
    retryHandler.run((context) -> {
      billingApi.addUserToBillingProject(projectName, USER_FC_ROLE, email);
      return null;
    });
  }

  @Override
  public void createWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    WorkspaceIngest workspaceIngest = new WorkspaceIngest();
    workspaceIngest.setName(workspaceName);
    workspaceIngest.setNamespace(projectName);
    // TODO: add concept of controlled auth domain.
    if (configProvider.get().firecloud.enforceRegistered) {
      ArrayList<ManagedGroupRef> authDomain = new ArrayList<ManagedGroupRef>();
      ManagedGroupRef registeredDomain = new ManagedGroupRef();
      registeredDomain.setMembersGroupName(configProvider.get().firecloud.registeredDomainName);
      authDomain.add(registeredDomain);
      workspaceIngest.setAuthorizationDomain(authDomain);
    }
    retryHandler.run((context) -> {
      workspacesApi.createWorkspace(workspaceIngest);
      return null;
    });
  }

  @Override
  public void grantGoogleRoleToUser(String projectName, String role, String email) throws ApiException {
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
    retryHandler.run((context) -> {
      workspacesApi.cloneWorkspace(fromProject, fromName, workspaceIngest);
      return null;
    });
  }


  @Override
  public List<BillingProjectMembership> getBillingProjectMemberships() throws ApiException {
    return retryHandler.run((context) -> profileApiProvider.get().billing());
  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }

  @Override
  public WorkspaceACLUpdateResponseList updateWorkspaceACL(String projectName, String workspaceName, List<WorkspaceACLUpdate> aclUpdates) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    // TODO: set authorization domain here
    return retryHandler.run((context) ->
      workspacesApi.updateWorkspaceACL(projectName, workspaceName, false, aclUpdates));
  }

  @Override
  public WorkspaceResponse getWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    return retryHandler.run((context) ->
      workspacesApi.getWorkspace(projectName, workspaceName));
  }

  @Override
  public List<WorkspaceResponse> getWorkspaces() throws ApiException {
    return retryHandler.run((context) ->
        workspacesApiProvider.get().listWorkspaces());
  }

  @Override
  public void deleteWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    retryHandler.run((context) -> {
      workspacesApi.deleteWorkspace(projectName, workspaceName);
      return null;
    });
  }

  @Override
  public ManagedGroupWithMembers createGroup(String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) ->
      groupsApi.createGroup(groupName));
  }

  @Override
  public void addUserToGroup(String email, String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run((context) -> {
      groupsApi.addUserToGroup(groupName, "member", email);
      return null;
    });
  }

  @Override
  public void removeUserFromGroup(String email, String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run((context) -> {
      groupsApi.removeUserFromGroup(groupName, "member", email);
      return null;
    });
  }
}
