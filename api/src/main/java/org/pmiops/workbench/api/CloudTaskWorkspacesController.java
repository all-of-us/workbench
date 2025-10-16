package org.pmiops.workbench.api;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.model.TestUserRawlsWorkspace;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.pmiops.workbench.model.WorkspaceUserCacheQueueWorkspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskWorkspacesController implements CloudTaskWorkspacesApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskWorkspacesController.class.getName());

  private static final boolean DELETE_BILLING_PROJECTS = true;

  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceUserCacheService workspaceUserCacheService;

  @Autowired
  public CloudTaskWorkspacesController(
      ImpersonatedWorkspaceService impersonatedWorkspaceService,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceUserCacheService workspaceUserCacheService) {
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceUserCacheService = workspaceUserCacheService;
  }

  @Override
  public ResponseEntity<Void> deleteTestUserWorkspacesBatch(List<TestUserWorkspace> request) {
    LOGGER.info(String.format("Deleting a batch of %d workspaces...", request.size()));
    request.stream()
        .collect(Collectors.groupingBy(TestUserWorkspace::getUsername, Collectors.counting()))
        .forEach(
            (user, count) -> LOGGER.info(String.format("%d owned by test user %s", count, user)));

    request.forEach(
        workspace -> {
          try {
            impersonatedWorkspaceService.deleteWorkspace(
                workspace.getUsername(),
                workspace.getNamespace(),
                workspace.getTerraName(),
                DELETE_BILLING_PROJECTS);
          } catch (NotFoundException e) {
            LOGGER.info(
                String.format(
                    "Workspace %s/%s was not found",
                    workspace.getNamespace(), workspace.getTerraName()));
          }
        });

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> deleteTestUserWorkspacesInRawlsBatch(
      List<TestUserRawlsWorkspace> request) {
    LOGGER.info(String.format("Deleting a batch of %d workspaces in Rawls...", request.size()));
    request.stream()
        .collect(Collectors.groupingBy(TestUserRawlsWorkspace::getUsername, Collectors.counting()))
        .forEach(
            (user, count) -> LOGGER.info(String.format("%d owned by test user %s", count, user)));

    request.forEach(
        workspace -> {
          try {
            impersonatedWorkspaceService.deleteOrphanedRawlsWorkspace(
                workspace.getUsername(),
                workspace.getNamespace(),
                workspace.getGoogleProject(),
                workspace.getTerraName(),
                DELETE_BILLING_PROJECTS);
          } catch (NotFoundException e) {
            LOGGER.info(
                String.format(
                    "Workspace %s/%s was not found in Rawls",
                    workspace.getNamespace(), workspace.getTerraName()));
          }
        });

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> cleanupOrphanedWorkspacesBatch(List<String> request) {
    LOGGER.info(
        String.format(
            "Cleaning up %d orphaned workspaces in internal database...", request.size()));

    request.forEach(
        namespace -> {
          try {
            impersonatedWorkspaceService.cleanupWorkspace(
                namespace, "CleanupOrphanedWorkspaces Cron Job");
          } catch (NotFoundException e) {
            LOGGER.info(String.format("Workspace (%s) was not found in database", namespace));
          }
        });

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
    LOGGER.info("Processing workspace user cache queue task...");

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> wsAcls =
        workspaces.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceUserCacheQueueWorkspace::getWorkspaceId,
                    workspace ->
                        workspaceAuthService.getFirecloudWorkspaceAcl(
                            workspace.getWorkspaceNamespace(),
                            workspace.getWorkspaceFirecloudName())));

    LOGGER.info(String.format("Updating cache for %d workspaces...", wsAcls.size()));

    workspaceUserCacheService.updateWorkspaceUserCache(wsAcls);

    LOGGER.info("Finished processing workspace user cache queue task.");

    return ResponseEntity.ok().build();
  }
}
