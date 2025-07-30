package org.pmiops.workbench.api;

import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Offline workspace API. Handles cron jobs for workspace cache management and ACL synchronization.
 * Methods here should be access restricted as, unlike WorkspacesController, these endpoints run as
 * the Workbench service account.
 */
@RestController
@RequestMapping("/v1/cron")
public class OfflineWorkspacesController {
  private static final Logger log = Logger.getLogger(OfflineWorkspacesController.class.getName());

  private final WorkspaceUserCacheService workspaceUserCacheService;
  private final TaskQueueService taskQueueService;

  @Autowired
  OfflineWorkspacesController(
      WorkspaceUserCacheService workspaceUserCacheService, TaskQueueService taskQueueService) {
    this.workspaceUserCacheService = workspaceUserCacheService;
    this.taskQueueService = taskQueueService;
  }

  @PostMapping("/cacheWorkspaceAcls")
  public ResponseEntity<Void> cacheWorkspaceAcls() {
    log.info("Starting cacheWorkspaceAcls cron job");
    var workspaces = workspaceUserCacheService.findAllActiveWorkspaceNamespacesNeedingCacheUpdate();
    workspaceUserCacheService.removeInactiveWorkspaces();
    taskQueueService.pushWorkspaceUserCacheTask(workspaces);

    return ResponseEntity.ok().build();
  }
}
