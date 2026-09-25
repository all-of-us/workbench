package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline notebook runtime API. Handles cronjobs for cleanup and upgrade of older runtimes. Methods
 * here should be access restricted as, unlike RuntimeController, these endpoints run as the
 * Workbench service account.
 */
@RestController
public class OfflineEnvironmentsController implements OfflineEnvironmentsApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineEnvironmentsController.class.getName());
  private final TaskQueueService taskQueueService;
  private final WorkspaceService workspaceService;

  @Autowired
  OfflineEnvironmentsController(
      TaskQueueService taskQueueService, WorkspaceService workspaceService) {
    this.taskQueueService = taskQueueService;
    this.workspaceService = workspaceService;
  }

  /**
   * deleteOldRuntimes deletes older runtimes in order to force an upgrade on the next researcher
   * login. This method is meant to be restricted to invocation by App Engine cron.
   *
   * <p>The runtime deletion policy here aims to strike a balance between enforcing upgrades, cost
   * savings, and minimizing user disruption. To this point, our goal is to only upgrade idle
   * runtimes if possible, as doing so gives us an assurance that a researcher is not actively using
   * it. We delete runtimes in the following cases:
   *
   * <ol>
   *   <li>It exceeds the max runtime age. Per environment, but O(weeks).
   *   <li>It is idle and exceeds the max idle runtime age. Per environment, smaller than (1).
   * </ol>
   *
   * <p>As an App Engine cron endpoint, the runtime of this method may not exceed 10 minutes.
   */
  @Override
  public ResponseEntity<Void> deleteOldRuntimes() {
    log.info("deleteOldRuntimes endpoint is decommissioned");
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> checkPersistentDisks() {
    log.info("checkPersistentDisks endpoint is decommissioned");
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteUnsharedWorkspaceEnvironments() {
    List<String> activeNamespaces = workspaceService.getActiveWorkspaceNamespacesAsService();

    log.info(
        String.format(
            "Queuing %d active workspaces in batches for deletion of unshared resources",
            activeNamespaces.size()));

    taskQueueService.groupAndPushDeleteWorkspaceEnvironmentTasks(activeNamespaces);
    return ResponseEntity.noContent().build();
  }
}
