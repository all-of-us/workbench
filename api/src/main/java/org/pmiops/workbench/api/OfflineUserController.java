package org.pmiops.workbench.api;

import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
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
    List<User> allUsers = userService.getAllUsers();
    allUsers.parallelStream().forEach(user -> {
      try {
        userService.syncComplianceTrainingStatus(user);
        log.info(String.format("Updated compliance training status for user %s.", user.getEmail()));
      } catch (Exception e) {
        log.severe(String.format("Error syncing Moodle training status for user %s: %s",
            user.getEmail(), e.getMessage()));
      }
    });
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Updates eRA Commons information for all users in the database.
   *
   * This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkSyncEraCommonsStatus() {
    List<User> allUsers = userService.getAllUsers();
    allUsers.parallelStream().forEach(user -> {
      try {
        userService.syncEraCommonsStatusUsingImpersonation(user);
        log.info(String.format("Updated eRA Commons status for user %s.", user.getEmail()));
      } catch (org.pmiops.workbench.firecloud.ApiException e) {
        log.severe(String.format("Error syncing eRA Commons status for user %s: %s",
            user.getEmail(), e.getMessage()));
      } catch (IOException e) {
        log.severe(String.format("Error fetching impersonated creds for user %s: %s",
            user.getEmail(), e.getMessage()));
      }
    });
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
