package org.pmiops.workbench.access;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForControlledTierAccess;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForRegisteredTierAccess;

import jakarta.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.javers.common.collections.Lists;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;
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
  private final UserServiceAuditor userServiceAuditor;
  private final Clock clock;

  @Autowired
  public AccessSyncServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      AccessTierService accessTierService,
      AccessModuleService accessModuleService,
      InstitutionService institutionService,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      Clock clock) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.accessTierService = accessTierService;
    this.accessModuleService = accessModuleService;
    this.institutionService = institutionService;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
    this.clock = clock;
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

    addInitialCreditsExpirationIfAppropriate(dbUser, previousAccessTiers, newAccessTiers);

    // add user to each Access Tier DB table and the tiers' Terra Auth Domains
    newAccessTiers.forEach(tier -> accessTierService.addUserToTier(dbUser, tier));

    // remove user from all other Access Tier DB tables and the tiers' Terra Auth Domains
    final List<DbAccessTier> tiersForRemoval =
        Lists.difference(accessTierService.getAllTiers(), newAccessTiers);
    tiersForRemoval.forEach(tier -> accessTierService.removeUserFromTier(dbUser, tier));

    return userDao.save(dbUser);
  }

  private void addInitialCreditsExpirationIfAppropriate(
      DbUser dbUser, List<DbAccessTier> previousAccessTiers, List<DbAccessTier> newAccessTiers) {
    boolean enableInitialCreditsExpiration =
        workbenchConfigProvider.get().featureFlags.enableInitialCreditsExpiration;
    long initialCreditsValidityPeriodDays =
        workbenchConfigProvider.get().billing.initialCreditsValidityPeriodDays;

    if (enableInitialCreditsExpiration) {
      DbUserInitialCreditsExpiration maybeCreditsExpiration =
          dbUser.getUserInitialCreditsExpiration();

      // A user's credits should begin to expire when they gain access to their first tier.
      if (previousAccessTiers.isEmpty()
          && !newAccessTiers.isEmpty()
          && null == maybeCreditsExpiration) {

        Timestamp now = new Timestamp(clock.instant().toEpochMilli());
        Timestamp expirationTime =
            new Timestamp(now.getTime() + TimeUnit.DAYS.toMillis(initialCreditsValidityPeriodDays));
        dbUser.setUserInitialCreditsExpiration(
            new DbUserInitialCreditsExpiration()
                .setUser(dbUser)
                .setCreditStartTime(now)
                .setExpirationTime(expirationTime));
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
    boolean eraCompliant =
        accessModuleService.isModuleCompliant(user, DbAccessModuleName.ERA_COMMONS);

    boolean eRARequiredForTier = true;
    boolean institutionalEmailValidForTier = false;
    Optional<Institution> institution = institutionService.getByUser(user);
    if (institution.isPresent()) {
      // eRA is required when login.gov linking is not enabled or user institution requires that in
      // tier requirement.
      eRARequiredForTier =
          !workbenchConfigProvider.get().access.enableRasLoginGovLinking
              || institutionService.eRaRequiredForTier(institution.get(), tierShortName);
      institutionalEmailValidForTier =
          institutionService.validateInstitutionalEmail(
              institution.get(), user.getContactEmail(), tierShortName);
    } else {
      log.warning(String.format("Institution not found for user %s", user.getUsername()));
    }
    return !user.getDisabled()
        && (!eRARequiredForTier || eraCompliant)
        && institutionalEmailValidForTier
        && allStandardRequiredModulesCompliant;
  }
}
