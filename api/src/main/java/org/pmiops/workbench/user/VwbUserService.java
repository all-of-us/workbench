package org.pmiops.workbench.user;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.PodDescription;
import org.pmiops.workbench.vwb.user.model.PodRole;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VwbUserService {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserService.class);

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;

  public VwbUserService(
      VwbUserManagerClient vwbUserManagerClient,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
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

  public String createInitialCreditsPodForUser(DbUser dbUser) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserCreation) {
      return null;
    }
    String email = dbUser.getUsername();
    PodDescription initialCreditsPodForUser =
        vwbUserManagerClient.createInitialCreditsPodForUser(email);
    vwbUserManagerClient.sharePodWithUserWithRole(
        initialCreditsPodForUser.getPodId(), email, PodRole.ADMIN);

    return initialCreditsPodForUser.getPodId().toString();
  }
}
