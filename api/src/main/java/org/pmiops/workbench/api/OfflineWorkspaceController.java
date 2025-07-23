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
  private final TaskQueueService taskQueueService;
  private final WorkspaceService workspaceService;

  @Autowired
  public OfflineWorkspaceController(
      TaskQueueService taskQueueService, WorkspaceService workspaceService) {
    this.taskQueueService = taskQueueService;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Void> cleanupOrphanedWorkspaces() {
    List<String> orphanedNamespaces = workspaceService.getOrphanedWorkspaceNamespacesAsService();
    taskQueueService.groupAndPushCleanupOrphanedWorkspacesTasks(orphanedNamespaces);

    return ResponseEntity.noContent().build();
  }
}
