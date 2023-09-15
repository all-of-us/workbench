package org.pmiops.workbench.compliancetraining;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessModuleNameMapper;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessSyncService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.dao.ComplianceTrainingVerificationDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbComplianceTrainingVerification;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.moodle.MoodleService;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceTrainingServiceImpl implements ComplianceTrainingService {
  private static final Logger log = Logger.getLogger(ComplianceTrainingServiceImpl.class.getName());
  private final MoodleService moodleService;
  private final AccessModuleService accessModuleService;
  private final AccessModuleNameMapper accessModuleNameMapper;
  private final AccessSyncService accessSyncService;
  private final Clock clock;
  private final Provider<DbUser> userProvider;
  private final UserService userService;
  private final ComplianceTrainingVerificationDao complianceTrainingVerificationDao;

  @Autowired
  public ComplianceTrainingServiceImpl(
      MoodleService moodleService,
      AccessModuleService accessModuleService,
      AccessModuleNameMapper accessModuleNameMapper,
      AccessSyncService accessSyncService,
      Clock clock,
      Provider<DbUser> userProvider,
      UserService userService,
      ComplianceTrainingVerificationDao complianceTrainingVerificationDao) {
    this.moodleService = moodleService;
    this.accessModuleService = accessModuleService;
    this.accessModuleNameMapper = accessModuleNameMapper;
    this.accessSyncService = accessSyncService;
    this.clock = clock;
    this.userProvider = userProvider;
    this.userService = userService;
    this.complianceTrainingVerificationDao = complianceTrainingVerificationDao;
  }

  /** Syncs the current user's training status from Moodle. */
  public DbUser syncComplianceTrainingStatus()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    DbUser user = userProvider.get();
    return syncComplianceTrainingStatus(user, Agent.asUser(user));
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
   *
   * @param dbUser
   * @param agent
   */
  @Transactional
  public DbUser syncComplianceTrainingStatus(DbUser dbUser, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    // Skip sync for service account user rows.
    if (userService.isServiceAccount(dbUser)) {
      return dbUser;
    }

    try {
      Map<MoodleService.BadgeName, BadgeDetailsV2> userBadgesByName =
          moodleService.getUserBadgesByBadgeName(dbUser.getUsername());

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
      Function<MoodleService.BadgeName, Optional<Timestamp>> determineCompletionTime =
          (badgeName) -> {
            Optional<BadgeDetailsV2> badge =
                Optional.ofNullable(userBadgesByName.get(badgeName))
                    .filter(BadgeDetailsV2::isValid);

            if (badge.isEmpty()) {
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

      Map<DbAccessModule.DbAccessModuleName, Optional<Timestamp>> completionTimes =
          Arrays.stream(MoodleService.BadgeName.values())
              .collect(
                  Collectors.toMap(
                      accessModuleNameMapper::moduleFromBadge, determineCompletionTime));

      completionTimes.forEach(
          (accessModuleName, timestamp) -> {
            var updatedUserAccessModule =
                accessModuleService.updateCompletionTime(
                    dbUser, accessModuleName, timestamp.orElse(null));
            // This is null, for example:
            // - If the user has not completed any trainings yet
            // - For CT if the user has just completed RT
            if (updatedUserAccessModule.getCompletionTime() != null) {
              var verification = retrieveVerificationOrCreate(updatedUserAccessModule);
              verification.setComplianceTrainingVerificationSystem(
                  DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);
              complianceTrainingVerificationDao.save(verification);
            }
          });

      return accessSyncService.updateUserAccessTiers(dbUser, agent);
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

  private Timestamp clockNow() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

  private DbComplianceTrainingVerification retrieveVerificationOrCreate(
      DbUserAccessModule userAccessModule) {
    return complianceTrainingVerificationDao
        .getByUserAccessModule(userAccessModule)
        .orElse(new DbComplianceTrainingVerification().setUserAccessModule(userAccessModule));
  }
}
