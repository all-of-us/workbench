package org.pmiops.workbench.access;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForControlledTierAccess;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForRegisteredTierAccess;

import jakarta.inject.Provider;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.commons.collections4.ListUtils;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VwbUserPodDao;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AccessSyncServiceImpl implements AccessSyncService {
  private static final Logger log = Logger.getLogger(AccessSyncServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final AccessTierService accessTierService;
  private final AccessModuleService accessModuleService;
  private final InstitutionService institutionService;
  private final UserDao userDao;
  private final VwbUserPodDao vwbUserPodDao;
  private final InitialCreditsService initialCreditsService;
  private final UserServiceAuditor userServiceAuditor;
  private final TaskQueueService taskQueueService;

  @Autowired
  public AccessSyncServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      AccessTierService accessTierService,
      AccessModuleService accessModuleService,
      InstitutionService institutionService,
      UserDao userDao,
      VwbUserPodDao vwbUserPodDao,
      InitialCreditsService initialCreditsService,
      UserServiceAuditor userServiceAuditor,
      TaskQueueService taskQueueService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.accessTierService = accessTierService;
    this.accessModuleService = accessModuleService;
    this.institutionService = institutionService;
    this.userDao = userDao;
    this.vwbUserPodDao = vwbUserPodDao;
    this.initialCreditsService = initialCreditsService;
    this.userServiceAuditor = userServiceAuditor;
    this.taskQueueService = taskQueueService;
  }

  /**
   * Ensures that the data access tiers for the user reflect the state of other fields on the user
   */
  @Override
  public DbUser updateUserAccessTiers(DbUser dbUser, Agent agent) {
    final List<DbAccessTier> previousAccessTiers = accessTierService.getAccessTiersForUser(dbUser);
    final List<DbAccessTier> newAccessTiers = getUserAccessTiersList(dbUser);

    log.info(
        String.format(
            "Updating access tiers for user %s (id: %d). Previous tiers: %s, New tiers: %s",
            dbUser.getUsername(),
            dbUser.getUserId(),
            previousAccessTiers.stream()
                .map(DbAccessTier::getShortName)
                .collect(java.util.stream.Collectors.joining(", ")),
            newAccessTiers.stream()
                .map(DbAccessTier::getShortName)
                .collect(java.util.stream.Collectors.joining(", "))));

    if (!newAccessTiers.equals(previousAccessTiers)) {
      userServiceAuditor.fireUpdateAccessTiersAction(
          dbUser, previousAccessTiers, newAccessTiers, agent);
    }

    addInitialCreditsExpirationIfAppropriate(dbUser, previousAccessTiers, newAccessTiers);

    // Save the user first
    log.info("Saving user " + dbUser.getUsername() + " before updating tier memberships");
    DbUser savedUser;
    try {
      savedUser = userDao.save(dbUser);
      log.info("Successfully saved user " + savedUser.getUsername());
    } catch (DataIntegrityViolationException e) {
      log.warning(
          String.format(
              "Failed to save user %s due to DataIntegrityViolationException: %s. "
                  + "Tier operations not performed. Exception will propagate for retry.",
              dbUser.getUsername(), e.getMessage()));
      throw e;
    }

    // Tiers to add are those present in the new set of tiers but not in the previous set.
    // We use ListUtils.subtract() to find the difference: (new - previous) = toAdd.
    final List<DbAccessTier> tiersToAdd = ListUtils.subtract(newAccessTiers, previousAccessTiers);

    // Tiers to remove are those present in the previous set of tiers but not in the new set.
    // We use ListUtils.subtract() to find the difference: (previous - new) = toRemove.
    final List<DbAccessTier> tiersToRemove =
        ListUtils.subtract(previousAccessTiers, newAccessTiers);

    if (!tiersToRemove.isEmpty()) {
      log.info(
          String.format(
              "Removing user %s from tiers: %s",
              dbUser.getUsername(),
              tiersToRemove.stream()
                  .map(DbAccessTier::getShortName)
                  .collect(java.util.stream.Collectors.joining(", "))));
    }
    // remove user from all other Access Tier DB tables and the tiers' Terra Auth Domains
    tiersToRemove.forEach(tier -> accessTierService.removeUserFromTier(dbUser, tier));

    if (!tiersToAdd.isEmpty()) {
      log.info(
          String.format(
              "Adding user %s to tiers: %s",
              dbUser.getUsername(),
              tiersToAdd.stream()
                  .map(DbAccessTier::getShortName)
                  .collect(java.util.stream.Collectors.joining(", "))));
    }
    // add user to each Access Tier DB table and the tiers' Terra Auth Domains
    tiersToAdd.forEach(tier -> accessTierService.addUserToTier(dbUser, tier));

    // Only after successful save, create the pod lock and push the task
    if (userHasFirstAccessToTiers(previousAccessTiers, newAccessTiers)) {
      log.info(
          String.format(
              "User %s is gaining first-time tier access. Attempting to create VWB pod.",
              savedUser.getUsername()));
      // Try to create the lock row - only push task if we successfully created it
      boolean podLockCreated = createVwbUserLockIfNeeded(savedUser);
      if (podLockCreated) {
        log.info(
            String.format(
                "Successfully created VWB pod lock for user %s. Pushing pod creation task.",
                savedUser.getUsername()));
        taskQueueService.pushVwbPodCreationTask(savedUser.getUsername());
      } else {
        log.info(
            String.format(
                "VWB pod lock already exists for user %s. Skipping pod creation task.",
                savedUser.getUsername()));
      }
    } else {
      log.info(
          String.format(
              "User %s is not gaining first-time tier access (prev: %d tiers, new: %d tiers). Skipping pod creation.",
              savedUser.getUsername(), previousAccessTiers.size(), newAccessTiers.size()));
    }

    return savedUser;
  }

  private boolean createVwbUserLockIfNeeded(DbUser dbUser) {
    // Use database-based locking to prevent concurrent VWB user/pod creation across all GAE
    // instances.
    // We insert a row with null vwb_pod_id as a lock, then the async task will update it with the
    // actual pod_id.
    Long userId = dbUser.getUserId();

    // Check if a pod record already exists (even with null pod_id)
    DbVwbUserPod existingPod = vwbUserPodDao.findByUserUserId(userId);
    if (existingPod != null) {
      // Pod record already exists (either fully created or just a lock), skip
      log.info("VWB pod record already exists for user ID: " + userId + ", skipping");
      return false;
    }

    try {
      // Try to insert a "lock" row with null pod_id
      DbVwbUserPod lockPod =
          new DbVwbUserPod()
              .setUser(dbUser)
              .setVwbPodId(null) // Initially null, will be updated by the async task
              .setInitialCreditsActive(false); // Will be set to true when pod is actually created

      vwbUserPodDao.save(lockPod);
      log.info("Created VWB pod lock for user ID: " + userId);
      return true; // Successfully created the lock, should push task
    } catch (DataIntegrityViolationException e) {
      // Another request already created the lock, that's ok
      log.info("VWB pod lock already created for user ID: " + userId + " by another request");
      return false; // Someone else created the lock, don't push task
    }
  }

  private boolean userHasFirstAccessToTiers(
      List<DbAccessTier> previousAccessTiers, List<DbAccessTier> newAccessTiers) {
    return previousAccessTiers.isEmpty() && !newAccessTiers.isEmpty();
  }

  private void addInitialCreditsExpirationIfAppropriate(
      DbUser dbUser, List<DbAccessTier> previousAccessTiers, List<DbAccessTier> newAccessTiers) {
    boolean enableInitialCreditsExpiration =
        workbenchConfigProvider.get().featureFlags.enableInitialCreditsExpiration;

    if (enableInitialCreditsExpiration) {
      DbUserInitialCreditsExpiration maybeCreditsExpiration =
          dbUser.getUserInitialCreditsExpiration();

      // A user's credits should begin to expire when they gain access to their first tier.
      if (userHasFirstAccessToTiers(previousAccessTiers, newAccessTiers)
          && null == maybeCreditsExpiration) {
        initialCreditsService.createInitialCreditsExpiration(dbUser);
      }
    }
  }

  private List<DbAccessTier> getUserAccessTiersList(DbUser dbUser) {
    // If user does NOT have access to RT, they should not have access to any TIER
    if (!shouldGrantUserTierAccess(
        dbUser, getRequiredModulesForRegisteredTierAccess(), REGISTERED_TIER_SHORT_NAME)) {
      return Collections.emptyList();
    }

    // User is already qualified for RT
    List<DbAccessTier> userAccessTiers =
        com.google.common.collect.Lists.newArrayList(accessTierService.getRegisteredTierOrThrow());

    // Add Controlled Access Tier to the list, if user has completed/bypassed all CT Steps.
    accessTierService
        .getAccessTierByName(CONTROLLED_TIER_SHORT_NAME)
        .ifPresent(
            tier -> {
              if (shouldGrantUserTierAccess(
                  dbUser,
                  getRequiredModulesForControlledTierAccess(),
                  CONTROLLED_TIER_SHORT_NAME)) {
                userAccessTiers.add(tier);
              }
            });

    return userAccessTiers;
  }

  private boolean shouldGrantUserTierAccess(
      DbUser user, Collection<DbAccessModuleName> requiredModules, String tierShortName) {
    boolean allStandardRequiredModulesCompliant =
        requiredModules.stream()
            .allMatch(moduleName -> accessModuleService.isModuleCompliant(user, moduleName));

    boolean institutionalEmailValidForTier = false;
    Optional<Institution> institution = institutionService.getByUser(user);
    if (institution.isPresent()) {
      institutionalEmailValidForTier =
          institutionService.validateInstitutionalEmail(
              institution.get(), user.getContactEmail(), tierShortName);
    } else {
      log.warning(String.format("Institution not found for user %s", user.getUsername()));
    }
    return !user.getDisabled()
        && institutionalEmailValidForTier
        && allStandardRequiredModulesCompliant;
  }
}
