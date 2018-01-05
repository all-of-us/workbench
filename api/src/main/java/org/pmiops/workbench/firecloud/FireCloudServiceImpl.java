package org.pmiops.workbench.firecloud;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership;
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest;
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

  @Autowired
  public FireCloudServiceImpl(Provider<WorkbenchConfig> configProvider,
      Provider<ProfileApi> profileApiProvider,
      Provider<BillingApi> billingApiProvider,
      Provider<GroupsApi> groupsApiProvider,
      Provider<WorkspacesApi> workspacesApiProvider) {
    this.configProvider = configProvider;
    this.profileApiProvider = profileApiProvider;
    this.billingApiProvider = billingApiProvider;
    this.groupsApiProvider = groupsApiProvider;
    this.workspacesApiProvider = workspacesApiProvider;
  }

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    try {
      Me me = profileApi.me();
      // Users can only use FireCloud if the Google and LDAP flags are enabled.
      return me.getEnabled() != null
          && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
    } catch (ApiException e) {
      if (e.getCode() == NOT_FOUND.value()) {
        return false;
      }
      throw e;
    }
  }

  @Override
  public Me getMe() throws ApiException {
    return profileApiProvider.get().me();
  }

  @Override
  public void registerUser(String contactEmail, String firstName, String lastName)
      throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    Profile profile = new Profile();
    profile.setContactEmail(contactEmail);
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

    profileApi.setProfile(profile);
  }

  @Override
  public void createAllOfUsBillingProject(String projectName) throws ApiException {
    BillingApi billingApi = billingApiProvider.get();
    CreateRawlsBillingProjectFullRequest request = new CreateRawlsBillingProjectFullRequest();
    request.setBillingAccount("billingAccounts/"+configProvider.get().firecloud.billingAccountId);
    request.setProjectName(projectName);
    billingApi.createBillingProjectFull(request);
  }

  @Override
  public void addUserToBillingProject(String email, String projectName) throws ApiException {
    BillingApi billingApi = billingApiProvider.get();
    billingApi.addUserToBillingProject(projectName, "user", email);
  }

  @Override
  public void createWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    WorkspaceIngest workspaceIngest = new WorkspaceIngest();
    workspaceIngest.setName(workspaceName);
    workspaceIngest.setNamespace(projectName);
    // TODO: set authorization domain here
    workspacesApi.createWorkspace(workspaceIngest);
  }

  @Override
  public void cloneWorkspace(String fromProject, String fromName, String toProject, String toName) {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    WorkspaceIngest workspaceIngest = new WorkspaceIngest();
    workspaceIngest.setNamespace(toProject);
    workspaceIngest.setName(toName);
    try {
      workspacesApi.cloneWorkspace(fromProject, fromName, workspaceIngest);
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      log.log(
          Level.SEVERE,
          String.format(
              "Error cloning FC workspace %s/%s: %s",
              fromProject,
              fromName,
              e.getResponseBody()),
          e);
      if (e.getCode() == 403) {
        throw new ForbiddenException(e.getResponseBody());
      } else if (e.getCode() == 409) {
        throw new ConflictException(e.getResponseBody());
      } else {
        throw new ServerErrorException(e.getResponseBody());
      }
    }
  }


  @Override
  public List<BillingProjectMembership> getBillingProjectMemberships() throws ApiException {
    return profileApiProvider.get().billing();
  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }

  @Override
  public WorkspaceACLUpdateResponseList updateWorkspaceACL(String projectName, String workspaceName, List<WorkspaceACLUpdate> aclUpdates) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    // TODO: set authorization domain here
    return workspacesApi.updateWorkspaceACL(projectName, workspaceName, false, aclUpdates);
  }

  @Override
  public WorkspaceResponse getWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    return workspacesApi.getWorkspace(projectName, workspaceName);
  }

  public void deleteWorkspace(String projectName, String workspaceName) throws ApiException {
    WorkspacesApi workspacesApi = workspacesApiProvider.get();
    workspacesApi.deleteWorkspace(projectName, workspaceName);
  }

  @Override
  public ManagedGroupWithMembers createGroup(String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    return groupsApi.createGroup(groupName);
  }

  @Override
  public void addUserToGroup(String email, String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    groupsApi.addUserToGroup(groupName, "member", email);
  }

  @Override
  public void removeUserFromGroup(String email, String groupName) throws ApiException {
    GroupsApi groupsApi = groupsApiProvider.get();
    groupsApi.removeUserFromGroup(groupName, "member", email);
  }
}
