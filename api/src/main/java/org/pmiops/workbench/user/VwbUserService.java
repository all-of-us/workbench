package org.pmiops.workbench.user;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VwbUserPodDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.vwb.user.model.OrganizationMember;
import org.pmiops.workbench.vwb.user.model.PodDescription;
import org.pmiops.workbench.vwb.user.model.PodRole;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class VwbUserService {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserService.class);

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final VwbUserPodDao vwbUserPodDao;

  public VwbUserService(
      VwbUserManagerClient vwbUserManagerClient,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      VwbUserPodDao vwbUserPodDao) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
    this.vwbUserPodDao = vwbUserPodDao;
  }

  /**
   * Create a user in VWB if the user does not already exist.
   *
   * @param email The email of the user to create.
   */
  public void createUser(String email) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBUserCreation) {
      return;
    }
    if (doesUserExist(email)) {
      logger.info("User already exists in VWB with email {}", email);
      return;
    }
    vwbUserManagerClient.createUser(email);
  }

  /** Checks if the user already exists in VWB */
  public boolean doesUserExist(String email) {
    OrganizationMember organizationMember = vwbUserManagerClient.getOrganizationMember(email);
    return organizationMember.getUserDescription() != null;
  }

  /**
   * Create an initial credits pod for a user if the user does not already have a pod.
   *
   * @param dbUser The user to create a pod for.
   * @return The created pod, or null if the pod could not be created.
   */
  public DbVwbUserPod createInitialCreditsPodForUser(DbUser dbUser) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBPodCreation) {
      return null;
    }
    String email = dbUser.getUsername();
    if (dbUser.getVwbUserPod() != null) {
      logger.info("User already has a pod with email {}", email);
      return dbUser.getVwbUserPod();
    }

    PodDescription initialCreditsPodForUser = vwbUserManagerClient.createPodForUserWithEmail(email);
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
    } catch (DataIntegrityViolationException e) {
      // Another task already created a pod for this user - that's ok!
      logger.info("Pod already created for user {} by another task, fetching existing pod", email);
      vwbUserManagerClient.deletePod(initialCreditsPodForUser.getPodId());
      // Refresh and return the existing pod
      DbUser refreshedUser = userDao.findById(dbUser.getUserId()).orElseThrow();
      return refreshedUser.getVwbUserPod();
    } catch (Throwable e) {
      logger.error("Error creating pod for user with email, deleting the pod {}", email, e);
      vwbUserManagerClient.deletePod(initialCreditsPodForUser.getPodId());
      return null;
    }
  }

  /**
   * Check if the pod is using initial credits billing account.
   *
   * @param pod The pod to check.
   * @return true if the pod is using initial credits billing account, false otherwise.
   */
  public boolean isPodUsingInitialCredits(DbVwbUserPod pod) {
    return workbenchConfigProvider
        .get()
        .billing
        .initialCreditsBillingAccountName()
        .equals(getBillingAccountForPod(pod.getVwbPodId()));
  }

  /**
   * Unlink the billing account for a user pod if it is using initial credits. This method will set
   * the initial credits active flag to false in the database.
   *
   * @param user The user whose pod's billing account should be unlinked.
   */
  public void unlinkBillingAccountForUserPod(DbUser user) {
    // If the user does not have a pod or the pod is not using initial credits, do nothing.
    DbVwbUserPod vwbUserPod = user.getVwbUserPod();
    if (vwbUserPod == null || !vwbUserPod.isInitialCreditsActive()) {
      return;
    }
    vwbUserManagerClient.unlinkBillingAccountFromPod(vwbUserPod.getVwbPodId());
    // At this point, the pod is unlinked from the initial credits billing account, so we should set
    // the initial credits active flag to false.
    DbVwbUserPod dbVwbUserPod = vwbUserPod.setInitialCreditsActive(false);
    vwbUserPodDao.save(dbVwbUserPod);
  }

  /**
   * Retrieves the billing account ID associated with the specified pod.
   *
   * @param podId the ID of the pod to look up
   * @return the billing account resource name (e.g., "billingAccounts/XXXX") if found, or
   *     "billingAccounts/" if not found
   */
  private String getBillingAccountForPod(String podId) {
    return "billingAccounts/"
        + vwbUserManagerClient
            .getPodById(podId)
            .map(
                podDescription ->
                    podDescription
                        .getEnvironmentData()
                        .getEnvironmentDataGcp()
                        .getBillingAccountId())
            .orElse("");
  }

  public void linkInitialCreditsBillingAccountToPod(DbVwbUserPod pod) {
    vwbUserManagerClient.updatePodBillingAccount(
        pod.getVwbPodId(), workbenchConfigProvider.get().billing.accountId);
  }
}
