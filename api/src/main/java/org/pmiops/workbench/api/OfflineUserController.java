package org.pmiops.workbench.api;

import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.Logger;

/**
 * Handles offline / cron-based API requests related to user management.
 */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineUserController.class.getName());

  private final UserService userService;

  @Autowired
  public OfflineUserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Updates moodle information for all users in the database.
   *
   * This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncComplianceTrainingStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (User user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getComplianceTrainingCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        User updatedUser = userService.syncComplianceTrainingStatus(user);

        Timestamp newTime = updatedUser.getComplianceTrainingCompletionTime();
        DataAccessLevel newLevel = updatedUser.getDataAccessLevelEnum();

        if (newTime != oldTime || (newTime != null && !newTime.equals(oldTime))) {
          log.info(String.format(
              "Compliance training completion changed for user %s. Old %s, new %s",
              user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(String.format(
              "Data access level changed for user %s. Old %s, new %s",
              user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.moodle.ApiException | NotFoundException e) {
        errorCount++;
        log.severe(String.format("Error syncing compliance training status for user %s: %s",
            user.getEmail(), e.getMessage()));
      }
    }

    log.info(String.format(
        "Checked %d users, updated %d completion times, updated %d access levels",
        userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during compliance training sync", errorCount));
    }

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Updates eRA Commons information for all users in the database.
   *
   * This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncEraCommonsStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (User user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getEraCommonsCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        User updatedUser = userService.syncEraCommonsStatusUsingImpersonation(user);

        Timestamp newTime = updatedUser.getEraCommonsCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (newTime != oldTime || (newTime != null && !newTime.equals(oldTime))) {
          log.info(String.format(
              "eRA Commons completion changed for user %s. Old %s, new %s",
              user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(String.format(
              "Data access level changed for user %s. Old %s, new %s",
              user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (org.pmiops.workbench.firecloud.ApiException e) {
        errorCount++;
        log.severe(String.format("Error syncing eRA Commons status for user %s: %s",
            user.getEmail(), e.getMessage()));
      } catch (IOException e) {
        errorCount++;
        log.severe(String.format("Error fetching impersonated creds for user %s: %s",
            user.getEmail(), e.getMessage()));
      }
    }

    log.info(String.format(
        "Checked %d users, updated %d completion times, updated %d access levels",
        userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
          String.format("%d errors encountered during eRA Commons sync", errorCount));
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Updates 2FA information for all users in the database.
   *
   * This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncTwoFactorAuthStatus() {
    int errorCount = 0;
    int userCount = 0;
    int changeCount = 0;
    int accessLevelChangeCount = 0;

    for (User user : userService.getAllUsers()) {
      userCount++;
      try {
        Timestamp oldTime = user.getTwoFactorAuthCompletionTime();
        DataAccessLevel oldLevel = user.getDataAccessLevelEnum();

        User updatedUser = userService.syncTwoFactorAuthStatus(user);

        Timestamp newTime = updatedUser.getTwoFactorAuthCompletionTime();
        DataAccessLevel newLevel = user.getDataAccessLevelEnum();

        if (newTime != oldTime || (newTime != null && !newTime.equals(oldTime))) {
          log.info(String.format(
              "Two-factor auth completion changed for user %s. Old %s, new %s",
              user.getEmail(), oldTime, newTime));
          changeCount++;
        }
        if (oldLevel != newLevel) {
          log.info(String.format(
              "Data access level changed for user %s. Old %s, new %s",
              user.getEmail(), oldLevel.toString(), newLevel.toString()));
          accessLevelChangeCount++;
        }
      } catch (Exception e) {
        errorCount++;
        log.severe(String.format("Error syncing two-factor auth status for user %s: %s",
                user.getEmail(), e.getMessage()));
      }
    }

    log.info(String.format(
        "Checked %d users, updated %d completion times, updated %d access levels",
        userCount, changeCount, accessLevelChangeCount));

    if (errorCount > 0) {
      throw new ServerErrorException(
              String.format("%d errors encountered during two-factor auth sync", errorCount));
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
