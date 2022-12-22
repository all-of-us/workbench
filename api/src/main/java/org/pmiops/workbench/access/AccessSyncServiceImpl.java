package org.pmiops.workbench.access;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessUtils.REQUIRED_MODULES_FOR_CONTROLLED_TIER;
import static org.pmiops.workbench.access.AccessUtils.REQUIRED_MODULES_FOR_REGISTERED_TIER;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.javers.common.collections.Lists;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.ComplianceService.BadgeName;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccessSyncServiceImpl implements AccessSyncService {
  private static final Logger log = Logger.getLogger(AccessSyncServiceImpl.class.getName());

  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private final AccessTierService accessTierService;
  private final AccessModuleNameMapper accessModuleNameMapper;
  private final AccessModuleService accessModuleService;
  private final ComplianceService complianceService;
  private final Clock clock;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final InstitutionService institutionService;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;

  @Autowired
  public AccessSyncServiceImpl(
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      AccessTierService accessTierService,
      AccessModuleNameMapper accessModuleNameMapper,
      AccessModuleService accessModuleService,
      ComplianceService complianceService,
      Clock clock,
      DirectoryService directoryService,
      FireCloudService fireCloudService,
      InstitutionService institutionService,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor) {
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.accessTierService = accessTierService;
    this.accessModuleNameMapper = accessModuleNameMapper;
    this.accessModuleService = accessModuleService;
    this.complianceService = complianceService;
    this.clock = clock;
    this.directoryService = directoryService;
    this.fireCloudService = fireCloudService;
    this.institutionService = institutionService;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
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

    // add user to each Access Tier DB table and the tiers' Terra Auth Domains
    newAccessTiers.forEach(tier -> accessTierService.addUserToTier(dbUser, tier));

    // remove user from all other Access Tier DB tables and the tiers' Terra Auth Domains
    final List<DbAccessTier> tiersForRemoval =
        Lists.difference(accessTierService.getAllTiers(), newAccessTiers);
    tiersForRemoval.forEach(tier -> accessTierService.removeUserFromTier(dbUser, tier));

    return userDao.save(dbUser);
  }

  @Override
  public void syncTwoFactorAuthStatus() {
    DbUser user = userProvider.get();
    syncTwoFactorAuthStatus(user, Agent.asUser(user));
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent) {
    return syncTwoFactorAuthStatus(
        targetUser,
        agent,
        directoryService.getUserOrThrow(targetUser.getUsername()).getIsEnrolledIn2Sv());
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent, boolean isEnrolledIn2FA) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    if (isEnrolledIn2FA) {
      final boolean needsDbCompletionUpdate =
          accessModuleService
              .getAccessModuleStatus(targetUser, DbAccessModuleName.TWO_FACTOR_AUTH)
              .map(status -> status.getCompletionEpochMillis() == null)
              .orElse(true);

      if (needsDbCompletionUpdate) {
        accessModuleService.updateCompletionTime(
            targetUser, DbAccessModuleName.TWO_FACTOR_AUTH, clockNow());
      }
    } else {
      accessModuleService.updateCompletionTime(
          targetUser, DbAccessModuleName.TWO_FACTOR_AUTH, null);
    }

    return updateUserAccessTiers(targetUser, agent);
  }

  /** Syncs the current user's training status from Moodle. */
  @Override
  public DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    DbUser user = userProvider.get();
    return syncComplianceTrainingStatusV2(user, Agent.asUser(user));
  }

  /**
   * Updates the given user's training status from Moodle.
   *
   * <p>We can fetch Moodle data for arbitrary users since we use an API key to access Moodle,
   * rather than user-specific OAuth tokens.
   *
   * <p>Using the user's email, we can get their badges from Moodle's APIs. If the badges are marked
   * valid, we store their completion dates in the database. If they are marked invalid, we clear
   * the completion dates from the database as the user will need to complete a new training.
   */
  @Override
  public DbUser syncComplianceTrainingStatusV2(DbUser dbUser, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    // Skip sync for service account user rows.
    if (isServiceAccount(dbUser)) {
      return dbUser;
    }

    try {
      Map<BadgeName, BadgeDetailsV2> userBadgesByName =
          complianceService.getUserBadgesByBadgeName(dbUser.getUsername());

      /**
       * Determine the logical completion time for this user for the given compliance access module.
       * Three logical outcomes are possible:
       *
       * <ul>
       *   <li>Incomplete or invalid training badge: empty
       *   <li>Badge has been issued for the first time, or has been reissued since we last marked
       *       the training complete: now
       *   <li>Else: existing completion time, i.e. no change
       * </ul>
       */
      Function<BadgeName, Optional<Timestamp>> determineCompletionTime =
          (badgeName) -> {
            Optional<BadgeDetailsV2> badge =
                Optional.ofNullable(userBadgesByName.get(badgeName))
                    .filter(BadgeDetailsV2::getValid);

            if (!badge.isPresent()) {
              return Optional.empty();
            }

            if (badge.get().getLastissued() == null) {
              log.warning(
                  String.format(
                      "badge %s is indicated as valid by Moodle, but is missing the lastissued "
                          + "time, this is unexpected - treating this as an incomplete training",
                      badgeName));
              return Optional.empty();
            }
            Instant badgeTime = Instant.ofEpochSecond(badge.get().getLastissued());
            Instant dbCompletionTime =
                accessModuleService
                    .getAccessModuleStatus(
                        dbUser, accessModuleNameMapper.moduleFromBadge(badgeName))
                    .map(AccessModuleStatus::getCompletionEpochMillis)
                    .map(Instant::ofEpochMilli)
                    .orElse(Instant.EPOCH);

            if (badgeTime.isAfter(dbCompletionTime)) {
              // First-time badge or renewal: our system recognizes the user as having
              // completed training right now, though the badge has been issued some
              // time in the past.
              return Optional.of(clockNow());
            }

            // No change
            return Optional.of(Timestamp.from(dbCompletionTime));
          };

      Map<DbAccessModuleName, Optional<Timestamp>> completionTimes =
          Arrays.stream(BadgeName.values())
              .collect(
                  Collectors.toMap(
                      accessModuleNameMapper::moduleFromBadge, determineCompletionTime));

      completionTimes.forEach(
          (accessModuleName, timestamp) ->
              accessModuleService.updateCompletionTime(
                  dbUser, accessModuleName, timestamp.orElse(null)));

      return updateUserAccessTiers(dbUser, agent);
    } catch (NumberFormatException e) {
      log.severe("Incorrect date expire format from Moodle");
      throw e;
    } catch (org.pmiops.workbench.moodle.ApiException ex) {
      if (ex.getCode() == HttpStatus.NOT_FOUND.value()) {
        log.severe(
            String.format(
                "Error while querying Moodle for badges for %s: %s ",
                dbUser.getUsername(), ex.getMessage()));
        throw new NotFoundException(ex.getMessage());
      } else {
        log.severe(String.format("Error while syncing compliance training: %s", ex.getMessage()));
      }
      throw ex;
    }
  }

  @Override
  public DbUser syncDuccVersionStatus(DbUser targetUser, Agent agent) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    if (!accessModuleService.hasUserSignedACurrentDucc(targetUser)) {
      accessModuleService.updateCompletionTime(
          targetUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, null);
    }

    return updateUserAccessTiers(targetUser, agent);
  }

  @Override
  public DbUser syncEraCommonsStatus() {
    DbUser user = userProvider.get();
    FirecloudNihStatus nihStatus = fireCloudService.getNihStatus();

    // NihStatus should never come back from firecloud with an empty linked username.
    // If that is the case, there is an error with FC, because we should get a 404
    // in that case. Leaving the null checking in for code safety reasons

    if (nihStatus == null
        || nihStatus.getLinkedNihUsername() == null
        || nihStatus.getLinkExpireTime() == null) {
      accessModuleService.updateCompletionTime(user, DbAccessModuleName.ERA_COMMONS, null);
      return userDao.save(
          user.setEraCommonsLinkedNihUsername(null).setEraCommonsLinkExpireTime(null));
    } else {
      Timestamp nihLinkExpireTime =
          Timestamp.from(Instant.ofEpochSecond(nihStatus.getLinkExpireTime()));
      Timestamp eraCommonsCompletionTime =
          calculateEraCompletion(user, nihStatus, nihLinkExpireTime);

      accessModuleService.updateCompletionTime(
          user, DbAccessModuleName.ERA_COMMONS, eraCommonsCompletionTime);

      return userDao.save(
          user.setEraCommonsLinkedNihUsername(nihStatus.getLinkedNihUsername())
              .setEraCommonsLinkExpireTime(nihLinkExpireTime));
    }
  }

  private Timestamp calculateEraCompletion(
      DbUser user, FirecloudNihStatus nihStatus, Timestamp nihLinkExpireTime) {
    Timestamp eraCommonsCompletionTime =
        accessModuleService
            .getAccessModuleStatus(user, DbAccessModuleName.ERA_COMMONS)
            .map(AccessModuleStatus::getCompletionEpochMillis)
            .map(Timestamp::new)
            .orElse(null);

    Timestamp now = clockNow();

    if (!nihLinkExpireTime.equals(user.getEraCommonsLinkExpireTime())) {
      // If the link expiration time has changed, we treat this as a "new" completion of the
      // access requirement.
      eraCommonsCompletionTime = now;
    } else if (nihStatus.getLinkedNihUsername() != null
        && !nihStatus.getLinkedNihUsername().equals(user.getEraCommonsLinkedNihUsername())) {
      // If the linked username has changed, we treat this as a new completion time.
      eraCommonsCompletionTime = now;
    } else if (eraCommonsCompletionTime == null) {
      // If the user hasn't previously completed this access requirement, set the time to now.
      eraCommonsCompletionTime = now;
    }

    return eraCommonsCompletionTime;
  }

  private List<DbAccessTier> getUserAccessTiersList(DbUser dbUser) {
    // If user does NOT have access to RT, they should not have access to any TIER
    if (!shouldGrantUserTierAccess(
        dbUser, REQUIRED_MODULES_FOR_REGISTERED_TIER, REGISTERED_TIER_SHORT_NAME)) {
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
                  dbUser, REQUIRED_MODULES_FOR_CONTROLLED_TIER, CONTROLLED_TIER_SHORT_NAME)) {
                userAccessTiers.add(tier);
              }
            });

    return userAccessTiers;
  }

  private boolean shouldGrantUserTierAccess(
      DbUser user, List<DbAccessModuleName> requiredModules, String tierShortName) {
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

  private boolean isServiceAccount(DbUser user) {
    return workbenchConfigProvider.get().auth.serviceAccountApiUsers.contains(user.getUsername());
  }

  private Timestamp clockNow() {
    return new Timestamp(clock.instant().toEpochMilli());
  }
}
