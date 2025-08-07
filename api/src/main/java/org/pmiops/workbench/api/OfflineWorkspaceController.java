package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate {
  private static final Logger log = Logger.getLogger(OfflineWorkspaceController.class.getName());

  private final TaskQueueService taskQueueService;
  private final WorkspaceService workspaceService;
  private final WorkspaceUserCacheService workspaceUserCacheService;

  @Autowired
  public OfflineWorkspaceController(
      TaskQueueService taskQueueService,
      WorkspaceService workspaceService,
      WorkspaceUserCacheService workspaceUserCacheService) {
    this.taskQueueService = taskQueueService;
    this.workspaceService = workspaceService;
    this.workspaceUserCacheService = workspaceUserCacheService;
  }

  @Override
  public ResponseEntity<Void> cleanupOrphanedWorkspaces() {
    List<String> orphanedNamespaces = workspaceService.getOrphanedWorkspaceNamespacesAsService();
    taskQueueService.groupAndPushCleanupOrphanedWorkspacesTasks(orphanedNamespaces);

    return ResponseEntity.noContent().build();
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
