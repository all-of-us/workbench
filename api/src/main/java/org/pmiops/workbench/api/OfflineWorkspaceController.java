package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.workspaces.TemporaryInitialCreditsRelinkService;
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
  private final TemporaryInitialCreditsRelinkService temporaryInitialCreditsRelinkService;

  @Autowired
  public OfflineWorkspaceController(
      TaskQueueService taskQueueService,
      WorkspaceService workspaceService,
      WorkspaceUserCacheService workspaceUserCacheService,
      TemporaryInitialCreditsRelinkService temporaryInitialCreditsRelinkService) {
    this.taskQueueService = taskQueueService;
    this.workspaceService = workspaceService;
    this.workspaceUserCacheService = workspaceUserCacheService;
    this.temporaryInitialCreditsRelinkService = temporaryInitialCreditsRelinkService;
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
    log.info("Finding all workspaces in need of a cache update");
    List<WorkspaceDao.WorkspaceUserCacheView> workspaces =
        workspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate();
    log.info("Found " + workspaces.size() + " workspaces in need of a cache update");
    log.info("Removing inactive workspaces from cache");
    workspaceUserCacheService.removeInactiveWorkspaces();
    log.info("Pushing workspaces onto task queue");
    taskQueueService.pushWorkspaceUserCacheTask(workspaces);
    log.info("Finished cacheWorkspaceAcls cron job");

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> cleanupTemporarilyRelinkedWorkspaces() {
    log.info("Starting cleanup temporarilyRelinkedWorkspaces");
    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    return ResponseEntity.ok().build();
  }
}
