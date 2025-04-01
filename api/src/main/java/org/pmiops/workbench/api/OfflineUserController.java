package org.pmiops.workbench.api;

import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;

import com.google.common.base.Stopwatch;
import jakarta.inject.Provider;
import java.time.Duration;
import java.util.List;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to user management. */
@RestController
public class OfflineUserController implements OfflineUserApiDelegate {
  private final Logger logger = LoggerFactory.getLogger(OfflineUserController.class);

  private final Provider<Stopwatch> stopwatchProvider;
  private final TaskQueueService taskQueueService;
  private final UserService userService;

  @Autowired
  public OfflineUserController(
      Provider<Stopwatch> stopwatchProvider,
      TaskQueueService taskQueueService,
      UserService userService) {
    this.stopwatchProvider = stopwatchProvider;
    this.taskQueueService = taskQueueService;
    this.userService = userService;
  }

  /**
   * Audits GCP access for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> bulkAuditProjectAccess() {
    taskQueueService.groupAndPushAuditProjectsTasks(
        userService.getAllUserIdsWithCurrentTierAccess());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> synchronizeUserAccess() {
    taskQueueService.groupAndPushSynchronizeAccessTasks(userService.getAllUserIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> sendAccessExpirationEmails() {
    taskQueueService.groupAndPushAccessExpirationEmailTasks(userService.getAllUserIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> checkInitialCreditsExpiration() {
    // temp performance logging
    Stopwatch stopwatch = stopwatchProvider.get().start();
    List<Long> userIds = userService.getAllUserIdsWithActiveInitialCredits();
    Duration elapsed = stopwatch.stop().elapsed();
    logger.info(
        String.format(
            "checkInitialCreditsExpiration: Retrieved %d user IDs from DB in %s",
            userIds.size(), formatDurationPretty(elapsed)));

    stopwatch.reset().start();
    List<String> taskIds = taskQueueService.groupAndPushCheckInitialCreditExpirationTasks(userIds);
    elapsed = stopwatch.stop().elapsed();
    logger.info(
        String.format(
            "checkInitialCreditsExpiration: Grouped and pushed %d user IDs into %d tasks in %s",
            userIds.size(), taskIds.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.noContent().build();
  }
}
