package org.pmiops.workbench.user;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VwbUserService {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserService.class);

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public VwbUserService(
      VwbUserManagerClient vwbUserManagerClient,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void createUser(String email) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserCreation) {
      return;
    }
    OrganizationMember organizationMember = vwbUserManagerClient.getOrganizationMember(email);
    if (organizationMember.getUserDescription() != null) {
      logger.info("User already exists in VWB with email {}", email);
      return;
    }
    vwbUserManagerClient.createUser(email);
  }
}
