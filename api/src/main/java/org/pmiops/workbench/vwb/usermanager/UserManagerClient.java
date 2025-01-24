package org.pmiops.workbench.vwb.usermanager;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.user.api.OrganizationV2Api;
import org.pmiops.workbench.vwb.user.api.UserV2Api;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.UserCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class UserManagerClient {

  private static final Logger logger = LoggerFactory.getLogger(UserManagerClient.class);

  private final Provider<UserV2Api> userV2ApiProvider;

  private final Provider<OrganizationV2Api> organizationV2ApiProvider;

  private final UserManagerRetryHandler userManagerRetryHandler;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public UserManagerClient(
      @Qualifier(UserManagerConfig.VWB_SERVICE_ACCOUNT_USER_API)
          Provider<UserV2Api> userV2ApiProvider,
      Provider<OrganizationV2Api> organizationV2ApiProvider,
      UserManagerRetryHandler userManagerRetryHandler,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.userV2ApiProvider = userV2ApiProvider;
    this.organizationV2ApiProvider = organizationV2ApiProvider;
    this.userManagerRetryHandler = userManagerRetryHandler;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public OrganizationMember getOrganizationMember(String organizationId, String userName) {
    return userManagerRetryHandler.run(
        context ->
            organizationV2ApiProvider.get().getOrganizationMemberV2(organizationId, userName));
  }

  public void createUser(UserCreateRequest email, String organizationId) {
    userManagerRetryHandler.run(
        context -> userV2ApiProvider.get().createUserV2(email, organizationId));
  }
}
