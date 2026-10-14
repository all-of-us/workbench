package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskWorkspacesController implements CloudTaskWorkspacesApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskWorkspacesController.class.getName());
  private final WorkspaceMigrationService workspaceMigrationService;

  @Autowired
  public CloudTaskWorkspacesController(WorkspaceMigrationService workspaceMigrationService) {
    this.workspaceMigrationService = workspaceMigrationService;
  }

  @Override
  public ResponseEntity<Void> deleteTestUserWorkspacesBatch(List<TestUserWorkspace> request) {
    LOGGER.info(
        String.format(
            "deleteTestUserWorkspacesBatch is decommissioned. %d workspaces skipped...",
            request.size()));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteTestUserWorkspacesInRawlsBatch(
      List<TestUserRawlsWorkspace> request) {
    LOGGER.info(
        String.format(
            "deleteTestUserWorkspacesInRawlsBatch is decommissioned. %d workspaces skipped...",
            request.size()));
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> cleanupOrphanedWorkspacesBatch(List<String> request) {
    LOGGER.info(
        String.format(
            "cleanupOrphanedWorkspacesBatch is decommissioned. %d skipped...", request.size()));
    return ResponseEntity.ok().build();
  }

  /**
   * Process a workspace user cache task by fetching the current ACLs from Terra and updating the
   * workspace_user_cache table. Cached ACLs is not to be used for authorization.
   *
   * @param workspaces the workspaces to process
   */
  @Override
  public ResponseEntity<Void> processWorkspaceUserCacheQueueTask(
      List<WorkspaceUserCacheQueueWorkspace> workspaces) {
    LOGGER.info("processWorkspaceUserCacheQueueTask is decommissioned...");
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> checkWorkspaceRecoveryStatus(
      CheckWorkspaceRecoveryStatusRequest request) {

    workspaceMigrationService.checkRecoveryStatus(request.getWorkspaceNamespace());

    return ResponseEntity.ok().build();
  }
}
