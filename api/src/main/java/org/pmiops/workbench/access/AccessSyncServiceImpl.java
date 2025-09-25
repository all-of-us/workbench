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
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.user.VwbUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessSyncServiceImpl implements AccessSyncService {
  private static final Logger log = Logger.getLogger(AccessSyncServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final AccessTierService accessTierService;
  private final AccessModuleService accessModuleService;
  private final InstitutionService institutionService;
  private final UserDao userDao;
  private final InitialCreditsService initialCreditsService;
  private final UserServiceAuditor userServiceAuditor;
  private final VwbUserService vwbUserService;
  private final TaskQueueService taskQueueService;

  @Autowired
  public AccessSyncServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      AccessTierService accessTierService,
      AccessModuleService accessModuleService,
      InstitutionService institutionService,
      UserDao userDao,
      InitialCreditsService initialCreditsService,
      UserServiceAuditor userServiceAuditor,
      VwbUserService vwbUserService,
      TaskQueueService taskQueueService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.accessTierService = accessTierService;
    this.accessModuleService = accessModuleService;
    this.institutionService = institutionService;
    this.userDao = userDao;
    this.initialCreditsService = initialCreditsService;
    this.userServiceAuditor = userServiceAuditor;
    this.vwbUserService = vwbUserService;
    this.taskQueueService = taskQueueService;
  }

  /**
   * Ensures that the data access tiers for the user reflect the state of other fields on the user
   */
  @Override
  public DbUser updateUserAccessTiers(DbUser dbUser, Agent agent) {
    final List<DbAccessTier> previousAccessTiers = accessTierService.getAccessTiersForUser(dbUser);

    final List<DbAccessTier> newAccessTiers = getUserAccessTiersList(dbUser);

    if (!newAccessTiers.equals(previousAccessTiers)) {
      userServiceAuditor.fireUpdateAccessTiersAction(
          dbUser, previousAccessTiers, newAccessTiers, agent);
    }

    createVwbUserIfNeeded(dbUser, previousAccessTiers, newAccessTiers);

    addInitialCreditsExpirationIfAppropriate(dbUser, previousAccessTiers, newAccessTiers);

    // Tiers to add are those present in the new set of tiers but not in the previous set.
    // We use ListUtils.subtract() to find the difference: (new - previous) = toAdd.
    final List<DbAccessTier> tiersToAdd = ListUtils.subtract(newAccessTiers, previousAccessTiers);

    // Tiers to remove are those present in the previous set of tiers but not in the new set.
    // We use ListUtils.subtract() to find the difference: (previous - new) = toRemove.
    final List<DbAccessTier> tiersToRemove =
        ListUtils.subtract(previousAccessTiers, newAccessTiers);

    // remove user from all other Access Tier DB tables and the tiers' Terra Auth Domains
    tiersToRemove.forEach(tier -> accessTierService.removeUserFromTier(dbUser, tier));

    // add user to each Access Tier DB table and the tiers' Terra Auth Domains
    tiersToAdd.forEach(tier -> accessTierService.addUserToTier(dbUser, tier));

    return userDao.save(dbUser);
  }

  private void createVwbUserIfNeeded(
      DbUser dbUser, List<DbAccessTier> previousAccessTiers, List<DbAccessTier> newAccessTiers) {
    // This means that the user has been granted access to a tier for the first time. Then perform
    // the VWB creation logic.
    if (userHasFirstAccessToTiers(previousAccessTiers, newAccessTiers)) {
      // This call checks if the user already exists in VWB to avoid creating the user twice.
      // Creating the user here is necessary to ensure that the user is created in VWB before adding
      // them to the groups
      vwbUserService.createUser(dbUser.getUsername());
      // Create the pod asynchronously to avoid blocking the user
      taskQueueService.pushVwbPodCreationTask(dbUser.getUsername());
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
