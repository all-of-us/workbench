package org.pmiops.workbench.user;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
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

  /**
   * Create a user in VWB if the user does not already exist.
   *
   * @param email The email of the user to create.
   */
  public void createUser(String email) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserAndPodCreation) {
      return;
    }
    OrganizationMember organizationMember = vwbUserManagerClient.getOrganizationMember(email);
    if (organizationMember.getUserDescription() != null) {
      logger.info("User already exists in VWB with email {}", email);
      return;
    }
    vwbUserManagerClient.createUser(email);
  }

  /**
   * Create an initial credits pod for a user if the user does not already have a pod.
   *
   * @param dbUser The user to create a pod for.
   * @return The created pod, or null if the pod could not be created.
   */
  public DbVwbUserPod createInitialCreditsPodForUser(DbUser dbUser) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserAndPodCreation) {
      return null;
    }
    String email = dbUser.getUsername();
    if (dbUser.getVwbUserPod() != null) {
      logger.info("User already has a pod with email {}", email);
      return dbUser.getVwbUserPod();
    }

    PodDescription initialCreditsPodForUser =
        vwbUserManagerClient.createInitialCreditsPodForUser(email);
    try {
      vwbUserManagerClient.sharePodWithUserWithRole(
          initialCreditsPodForUser.getPodId(), email, PodRole.ADMIN);

      DbVwbUserPod dbVwbUserPod =
          new DbVwbUserPod()
              .setVwbPodId(initialCreditsPodForUser.getPodId().toString())
              .setUser(dbUser)
              .setInitialCreditsActive(true);
      dbUser.setVwbUserPod(dbVwbUserPod);
      userDao.save(dbUser);

      return dbVwbUserPod;
    } catch (Throwable e) {
      logger.error("Error creating pod for user with email, deleting the pod {}", email, e);
      vwbUserManagerClient.deletePod(initialCreditsPodForUser.getPodId());
      return null;
    }
  }
}
