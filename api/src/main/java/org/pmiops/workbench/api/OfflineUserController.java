package org.pmiops.workbench.api;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to user management. */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineUserController.class.getName());
  private static final List<String> WHITELISTED_ORG_IDS =
      Arrays.asList(
          "400176686919", // test.firecloud.org
          "386193000800", // firecloud.org
          "394551486437" // pmi-ops.org
          );

  private final CloudResourceManagerService cloudResourceManagerService;
  private final UserServiceImpl userService;

  @Autowired
  public OfflineUserController(
      CloudResourceManagerService cloudResourceManagerService, UserServiceImpl userService) {
    this.cloudResourceManagerService = cloudResourceManagerService;
    this.userService = userService;
  }

  private boolean timestampsEqual(Timestamp a, Timestamp b) {
    if (a != null) {
      return a.equals(b);
    } else if (b != null) {
      return b.equals(a);
    } else {
      return a == b;
    }
  }

  /**
   * Updates moodle information for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncComplianceTrainingStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getComplianceTrainingCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser = userService.syncComplianceTrainingStatus(user);

        Timestamp newTime = updatedUser.getComplianceTrainingCompletionTime();
        DataAccessLevel newLevel = updatedUser.getDataAccessLevelEnum();

        if (!timestampsEqual(newTime, oldTime)) {
          log.info(
              String.format(
                  "Compliance training completion changed for user %s. Old %s, new %s",
                  user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.moodle.ApiException | NotFoundException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error syncing compliance training status for user %s: %s",
                user.getEmail(), e.getMessage()));
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during compliance training sync", errorCount));
    }

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
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getEraCommonsCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser = userService.syncEraCommonsStatusUsingImpersonation(user);

        Timestamp newTime = updatedUser.getEraCommonsCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (!timestampsEqual(newTime, oldTime)) {
          log.info(
              String.format(
                  "eRA Commons completion changed for user %s. Old %s, new %s",
                  user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.firecloud.ApiException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error syncing eRA Commons status for user %s: %s",
                user.getEmail(), e.getMessage()));
      } catch (IOException e) {
        errorCount++;
        log.severe(
            String.format(
                "Error fetching impersonated creds for user %s: %s",
                user.getEmail(), e.getMessage()));
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during eRA Commons sync", errorCount));
    }
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
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (DbUser user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getTwoFactorAuthCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        DbUser updatedUser = userService.syncTwoFactorAuthStatus(user);

        Timestamp newTime = updatedUser.getTwoFactorAuthCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (!timestampsEqual(newTime, oldTime)) {
          log.info(
              String.format(
                  "Two-factor auth completion changed for user %s. Old %s, new %s",
                  user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(
              String.format(
                  "Data access level changed for user %s. Old %s, new %s",
                  user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (Exception e) {
        errorCount++;
        log.severe(
            String.format(
                "Error syncing two-factor auth status for user %s: %s",
                user.getEmail(), e.getMessage()));
      }
    }

    log.info(
        String.format(
            "Checked %d users, updated %d completion times, updated %d access levels",
            userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during two-factor auth sync", errorCount));
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Audits GCP access for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkAuditProjectAccess() {
    for (DbUser user : userService.getAllUsers()) {
      // TODO(RW-2062): Move to using the gcloud api for list all resources when it is available.
      List<String> unauthorizedLogs =
          cloudResourceManagerService.getAllProjectsForUser(user).stream()
              .filter(project -> !(WHITELISTED_ORG_IDS.contains(project.getParent().getId())))
              .map(project -> project.getName() + " in organization " + project.getParent().getId())
              .collect(Collectors.toList());
      if (unauthorizedLogs.size() > 0) {
        log.log(
            Level.WARNING,
            "User "
                + user.getEmail()
                + " has access to projects: "
                + String.join(", ", unauthorizedLogs));
      }
    }
    return ResponseEntity.noContent().build();
  }
}
