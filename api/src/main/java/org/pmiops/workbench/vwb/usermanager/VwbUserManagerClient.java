package org.pmiops.workbench.vwb.usermanager;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.user.api.OrganizationV2Api;
import org.pmiops.workbench.vwb.user.api.UserV2Api;
import org.pmiops.workbench.vwb.user.api.WorkbenchGroupApi;
import org.pmiops.workbench.vwb.user.model.GroupRole;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.Principal;
import org.pmiops.workbench.vwb.user.model.PrincipalUser;
import org.pmiops.workbench.vwb.user.model.SetAccessOperation;
import org.pmiops.workbench.vwb.user.model.SetAccessRequest;
import org.pmiops.workbench.vwb.user.model.UserCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class VwbUserManagerClient {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserManagerClient.class);

  private final Provider<UserV2Api> userV2ApiProvider;

  private final Provider<OrganizationV2Api> organizationV2ApiProvider;
  private final Provider<WorkbenchGroupApi> groupApiProvider;

  private final VwbUserManagerRetryHandler vwbUserManagerRetryHandler;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public VwbUserManagerClient(
      @Qualifier(VwbUserManagerConfig.VWB_SERVICE_ACCOUNT_USER_API)
          Provider<UserV2Api> userV2ApiProvider,
      Provider<OrganizationV2Api> organizationV2ApiProvider,
      Provider<WorkbenchGroupApi> groupApiProvider,
      VwbUserManagerRetryHandler vwbUserManagerRetryHandler,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userV2ApiProvider = userV2ApiProvider;
    this.organizationV2ApiProvider = organizationV2ApiProvider;
    this.groupApiProvider = groupApiProvider;
    this.vwbUserManagerRetryHandler = vwbUserManagerRetryHandler;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public OrganizationMember getOrganizationMember(String userName) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    return vwbUserManagerRetryHandler.run(
        context ->
            organizationV2ApiProvider.get().getOrganizationMemberV2(organizationId, userName));
  }

  public void createUser(String email) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    logger.info("Creating user in VWB with email {}", email);
    vwbUserManagerRetryHandler.run(
        context ->
            userV2ApiProvider
                .get()
                .createUserV2(new UserCreateRequest().email(email), organizationId));
  }

  /** Adds a user into VWB user group. */
  public void addUserToGroup(String groupName, String email) {
    logger.info("Adding user in VWB group {}, with email {}", groupName, email);
    updateGroupMembership(groupName, email, SetAccessOperation.GRANT);
  }

  /** Removes a user into VWB user group. */
  public void removeUserFromGroup(String groupName, String email) {
    logger.info("Removing user in VWB group {}, with email {}", groupName, email);
    updateGroupMembership(groupName, email, SetAccessOperation.REVOKE);
  }

  private void updateGroupMembership(String groupName, String email, SetAccessOperation operation) {
    String organizationId = workbenchConfigProvider.get().vwb.organizationId;
    SetAccessRequest setAccessRequest =
        new SetAccessRequest()
            .role(GroupRole.MEMBER)
            .operation(operation)
            .principal(new Principal().userPrincipal(new PrincipalUser().email(email)));
    vwbUserManagerRetryHandler.run(
        context -> {
          groupApiProvider.get().setGroupAccess(setAccessRequest, groupName, organizationId);
          return null;
        });
  }
}
