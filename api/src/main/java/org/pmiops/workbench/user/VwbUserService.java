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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class VwbUserService {

  private static final Logger logger = LoggerFactory.getLogger(VwbUserService.class);

  private final VwbUserManagerClient vwbUserManagerClient;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final VwbUserPodDao vwbUserPodDao;

  @Autowired
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

  /**
   * Checks if the user already exists in VWB
   *
   * @param email the email/username to check
   * @return true if the user exists in VWB, false otherwise
   */
  public boolean doesUserExist(String email) {
    try {
      OrganizationMember organizationMember = vwbUserManagerClient.getOrganizationMember(email);
      return organizationMember.getUserDescription() != null;

    } catch (Exception e) {
      logger.warn(
          "Cache operation failed for user "
              + email
              + ", falling back to direct call: "
              + e.getMessage());
    }
    return false;
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

    // Get the latest pod state from database (not from the passed-in user object)
    DbVwbUserPod existingPod = vwbUserPodDao.findByUserUserId(dbUser.getUserId());

    // Check if pod already exists and has a pod_id (not just a lock)
    if (existingPod != null && existingPod.getVwbPodId() != null) {
      logger.info("User already has a pod with email {}", email);
      return existingPod;
    }

    // If there's a lock row but no pod yet, we need to create the pod
    PodDescription initialCreditsPodForUser = vwbUserManagerClient.createPodForUserWithEmail(email);
    try {
      vwbUserManagerClient.sharePodWithUserWithRole(
          initialCreditsPodForUser.getPodId(), email, PodRole.ADMIN);

      // If there's an existing lock row (pod with null pod_id), update it
      if (existingPod != null) {
        // We have a lock row, update it with the actual pod_id
        existingPod.setVwbPodId(initialCreditsPodForUser.getPodId().toString());
        existingPod.setInitialCreditsActive(true);
        vwbUserPodDao.save(existingPod);
        logger.info(
            "Updated VWB pod lock with actual pod ID {} for user {}",
            initialCreditsPodForUser.getPodId(),
            email);
        return existingPod;
      } else {
        // No existing pod at all, create new (shouldn't happen with new locking)
        // This path should rarely be taken since AccessSyncServiceImpl creates the lock first
        DbVwbUserPod dbVwbUserPod =
            new DbVwbUserPod()
                .setVwbPodId(initialCreditsPodForUser.getPodId().toString())
                .setUser(dbUser)
                .setInitialCreditsActive(true);
        vwbUserPodDao.save(dbVwbUserPod);
        logger.info(
            "Created new VWB pod {} for user {} (no lock found)",
            initialCreditsPodForUser.getPodId(),
            email);
        return dbVwbUserPod;
      }
    } catch (DataIntegrityViolationException e) {
      // This should rarely happen now with the locking mechanism
      // If it does, it means another task managed to create a full pod record
      logger.warn("Unexpected duplicate key error for user {}, checking for existing pod", email);

      // Delete the pod we just created since we couldn't save it
      vwbUserManagerClient.deletePod(initialCreditsPodForUser.getPodId());

      // Refresh and return the existing pod
      DbVwbUserPod latestPod = vwbUserPodDao.findByUserUserId(dbUser.getUserId());
      if (latestPod != null && latestPod.getVwbPodId() != null) {
        logger.info("Found existing pod {} for user {}", latestPod.getVwbPodId(), email);
        return latestPod;
      } else {
        logger.error("Failed to find existing pod after duplicate key error for user {}", email);
        return null;
      }
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
    // If pod doesn't have a pod_id yet (just a lock), it can't be using initial credits
    if (pod.getVwbPodId() == null) {
      return false;
    }
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
    if (vwbUserPod == null
        || !vwbUserPod.isInitialCreditsActive()
        || vwbUserPod.getVwbPodId() == null) {
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
