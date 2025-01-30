package org.pmiops.workbench.api;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to user management. */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineUserController.class.getName());

  private final UserService userService;
  private final TaskQueueService taskQueueService;

  @Autowired
  public OfflineUserController(UserService userService, TaskQueueService taskQueueService) {
    this.userService = userService;
    this.taskQueueService = taskQueueService;
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
  public ResponseEntity<Void> groupAndPushSynchronizeAccessTasks() {
    taskQueueService.groupAndPushSynchronizeAccessTasks(userService.getAllUserIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> sendAccessExpirationEmails() {
    var users = userService.getAllUsers();
    log.info(
        String.format("Checking %d users for sending access expiration emails...", users.size()));
    users.forEach(userService::maybeSendAccessTierExpirationEmails);
    log.info("Done checking users for sending access expiration emails.");
    return ResponseEntity.noContent().build();
  }

  public ResponseEntity<Void> checkInitialCreditsExpiration() {
    taskQueueService.groupAndPushCheckInitialCreditExpirationTasks(
        userService.getAllUsersWithActiveInitialCredits().stream()
            .map(DbUser::getUserId)
            .collect(Collectors.toList()));
    return ResponseEntity.noContent().build();
  }
}
