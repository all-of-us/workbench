package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.AccessModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to user management. */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineUserController.class.getName());
  private static final Map<AccessModule, String> accessModuleLogText =
      ImmutableMap.of(
          AccessModule.COMPLIANCE_TRAINING, "Compliance training",
          AccessModule.ERA_COMMONS, "eRA Commons",
          AccessModule.TWO_FACTOR_AUTH, "Two-factor auth");

  private final AccessTierService accessTierService;
  private final UserService userService;
  private final DirectoryService directoryService;
  private final TaskQueueService taskQueueService;

  @Autowired
  public OfflineUserController(
      AccessTierService accessTierService,
      UserService userService,
      DirectoryService directoryService,
      TaskQueueService taskQueueService) {
    this.accessTierService = accessTierService;
    this.userService = userService;
    this.directoryService = directoryService;
    this.taskQueueService = taskQueueService;
  }

  /**
   * Updates moodle information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   *
   * <p>This would only ever catch the following scenarios: 1. The user's compliance training
   * expires. The time scale on this expiration is O(years), so syncing nightly is entirely
   * acceptable.
   *
   * <p>2. The user completes compliance and doesn't return to the dashboard. We'd progress them in
   * the background. This won't make much of a difference and would have the same effect as if they
   * had navigated back to the dashboard.
   *
   * <p>3. Somehow the user manages to "uncomplete" training in Moodle. There is no direct process
   * for this today, but if it happened, it's certainly an edge case where nightly would be
   * acceptable latency
   */
  @Override
  public ResponseEntity<Void> bulkSyncComplianceTrainingStatus() {
    int errorCount = 0;
    int userCount = 0;
    int completionChangeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      try {
        Timestamp oldTime = user.getComplianceTrainingCompletionTime();
        List<String> oldTiers = accessTierService.getAccessTierShortNamesForUser(user);

        DbUser updatedUser = userService.syncComplianceTrainingStatusV2(user, Agent.asSystem());

        Timestamp newTime = updatedUser.getComplianceTrainingCompletionTime();
        List<String> newTiers = accessTierService.getAccessTierShortNamesForUser(user);

        completionChangeCount +=
            logCompletionChange(user, oldTime, newTime, AccessModule.COMPLIANCE_TRAINING);
        accessLevelChangeCount += logAccessTierChange(user, oldTiers, newTiers);
      } catch (org.pmiops.workbench.moodle.ApiException | NotFoundException e) {
        errorCount++;
        logSyncError(user, e, AccessModule.COMPLIANCE_TRAINING);
      }
    }

    logChangeTotals(userCount, completionChangeCount, accessLevelChangeCount);
    throwIfErrors(errorCount, AccessModule.COMPLIANCE_TRAINING);

    return ResponseEntity.noContent().build();
  }

  /**
   * Updates eRA Commons information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncEraCommonsStatus() {
    int errorCount = 0;
    int userCount = 0;
    int completionChangeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      try {
        // User accounts are registered with Terra on first sign-in. Users who have never signed in
        // are therefore unusable for impersonated calls to Terra to check on their eRA commons
        // status.
        if (user.getFirstSignInTime() == null) {
          continue;
        }

        Timestamp oldTime = user.getEraCommonsCompletionTime();
        List<String> oldTiers = accessTierService.getAccessTierShortNamesForUser(user);

        DbUser updatedUser =
            userService.syncEraCommonsStatusUsingImpersonation(user, Agent.asSystem());

        Timestamp newTime = updatedUser.getEraCommonsCompletionTime();
        List<String> newTiers = accessTierService.getAccessTierShortNamesForUser(user);

        completionChangeCount +=
            logCompletionChange(user, oldTime, newTime, AccessModule.ERA_COMMONS);
        accessLevelChangeCount += logAccessTierChange(user, oldTiers, newTiers);
      } catch (org.pmiops.workbench.firecloud.ApiException e) {
        errorCount++;
        logSyncError(user, e, AccessModule.ERA_COMMONS);
      } catch (IOException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error fetching impersonated creds for user %s: %s",
                user.getUsername(), e.getMessage()));
      }
    }

    logChangeTotals(userCount, completionChangeCount, accessLevelChangeCount);
    throwIfErrors(errorCount, AccessModule.ERA_COMMONS);

    return ResponseEntity.noContent().build();
  }

  /**
   * Updates 2FA information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncTwoFactorAuthStatus() {
    int errorCount = 0;
    int userCount = 0;
    int completionChangeCount = 0;
    int accessLevelChangeCount = 0;

    Map<String, Boolean> twoFactorAuthStatuses = directoryService.getAllTwoFactorAuthStatuses();
    for (DbUser user : userService.getAllUsersExcludingDisabled()) {
      userCount++;
      if (!twoFactorAuthStatuses.containsKey(user.getUsername())) {
        log.warning(
            String.format("user %s does not exist in gsuite, skipping", user.getUsername()));
        errorCount++;
        continue;
      }
      try {
        Timestamp oldTime = user.getTwoFactorAuthCompletionTime();
        List<String> oldTiers = accessTierService.getAccessTierShortNamesForUser(user);
        DbUser updatedUser =
            userService.syncTwoFactorAuthStatus(
                user, Agent.asSystem(), twoFactorAuthStatuses.get(user.getUsername()));

        Timestamp newTime = updatedUser.getTwoFactorAuthCompletionTime();
        List<String> newTiers = accessTierService.getAccessTierShortNamesForUser(user);

        completionChangeCount +=
            logCompletionChange(user, oldTime, newTime, AccessModule.TWO_FACTOR_AUTH);
        accessLevelChangeCount += logAccessTierChange(user, oldTiers, newTiers);
      } catch (Exception e) {
        errorCount++;
        logSyncError(user, e, AccessModule.TWO_FACTOR_AUTH);
      }
    }

    logChangeTotals(userCount, completionChangeCount, accessLevelChangeCount);
    throwIfErrors(errorCount, AccessModule.TWO_FACTOR_AUTH);

    return ResponseEntity.noContent().build();
  }

  /**
   * Audits GCP access for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkAuditProjectAccess() {
    taskQueueService.groupAndPushAuditProjectsTasks(userService.getAllUserIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> synchronizeUserAccess() {
    userService
        .getAllUsers()
        .forEach(
            user -> {
              // This will update the access tiers for a given user, based on compliance rules
              // If a user is no longer compliant it will remove them from an access tier
              userService.updateUserWithRetries(Function.identity(), user, Agent.asSystem());
            });
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> sendAccessExpirationEmails() {
    userService.getAllUsers().forEach(userService::maybeSendAccessExpirationEmail);
    return ResponseEntity.noContent().build();
  }

  private int logCompletionChange(
      DbUser user, Timestamp oldTime, Timestamp newTime, AccessModule module) {
    return logChange(user, oldTime, newTime, accessModuleLogText.get(module) + " completion");
  }

  private int logAccessTierChange(DbUser user, List<String> oldTiers, List<String> newTiers) {
    return logChange(user, oldTiers, newTiers, "Data access tiers");
  }

  private int logChange(DbUser user, Object oldValue, Object newValue, String initialText) {
    if (!Objects.equals(oldValue, newValue)) {
      log.info(
          String.format(
              "%s changed for user %s. Old %s, new %s",
              initialText, user.getUsername(), oldValue, newValue));
      return 1;
    }

    return 0;
  }

  private void logSyncError(DbUser user, Exception e, AccessModule module) {
    log.log(
        Level.SEVERE,
        String.format(
            "Error syncing %s status for user %s",
            accessModuleLogText.get(module), user.getUsername()),
        e);
  }

  private void logChangeTotals(
      int userCount, int completionChangeCount, int accessLevelChangeCount) {
    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, completionChangeCount, accessLevelChangeCount));
  }

  private void throwIfErrors(int errorCount, AccessModule module) {
    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format(
              "%d errors encountered during %s sync", errorCount, accessModuleLogText.get(module)));
    }
  }
}
