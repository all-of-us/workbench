package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Handles offline / cron-based API requests related to workspace management */
@RestController
public class OfflineWorkspacesController implements OfflineWorkspacesApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineWorkspacesController.class.getName());

  private final WorkspaceUserCacheService workspaceUserCacheService;
  private final TaskQueueService taskQueueService;

  @Autowired
  OfflineWorkspacesController(
      WorkspaceUserCacheService workspaceUserCacheService, TaskQueueService taskQueueService) {
    this.workspaceUserCacheService = workspaceUserCacheService;
    this.taskQueueService = taskQueueService;
  }

  @Override
  public ResponseEntity<Void> cacheWorkspaceAcls() {
    log.info("Starting cacheWorkspaceAcls cron job");
    List<DbWorkspace> workspaces =
        workspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate();
    workspaceUserCacheService.removeInactiveWorkspaces();
    taskQueueService.pushWorkspaceUserCacheTask(workspaces);

    return ResponseEntity.ok().build();
  }
}
