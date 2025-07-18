package org.pmiops.workbench.api;

import java.util.List;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to workspace management. */
@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate {
  //  private final Logger logger = LoggerFactory.getLogger(OfflineWorkspaceController.class);
  //
  private final TaskQueueService taskQueueService;
  private final WorkspaceService workspaceService;

  @Autowired
  public OfflineWorkspaceController(
      TaskQueueService taskQueueService, WorkspaceService workspaceService) {
    this.taskQueueService = taskQueueService;
    this.workspaceService = workspaceService;
  }

  /**
   * Audits GCP access for all users in the database.
   *
   * <p>This API method is called by a cron job and is not part of our normal user-facing surface.
   */
  @Override
  public ResponseEntity<Void> cleanupOrphanedWorkspaces() {
    List<String> orphanedNamespaces = workspaceService.getOrphanedWorkspaceNamespacesAsService();
    //    Stopwatch stopwatch = stopwatchProvider.get().start();
    //    List<Long> userIds = userService.getAllUserIdsWithActiveInitialCredits();
    //    Duration elapsed = stopwatch.stop().elapsed();
    //    logger.info(
    //        String.format(
    //            "checkInitialCreditsExpiration: Retrieved %d user IDs from DB in %s",
    //            userIds.size(), formatDurationPretty(elapsed)));
    //
    //    stopwatch.reset().start();
    //    List<String> taskIds =
    taskQueueService.groupAndPushCleanupOrphanedWorkspacesTasks(orphanedNamespaces);
    //    elapsed = stopwatch.stop().elapsed();
    //    logger.info(
    //        String.format(
    //            "checkInitialCreditsExpiration: Grouped and pushed %d user IDs into %d tasks in
    // %s",
    //            userIds.size(), taskIds.size(), formatDurationPretty(elapsed)));

    return ResponseEntity.noContent().build();
  }
}
