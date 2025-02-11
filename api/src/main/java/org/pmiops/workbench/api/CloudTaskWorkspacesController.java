package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.model.TestUserRawlsWorkspace;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskWorkspacesController implements CloudTaskWorkspacesApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskWorkspacesController.class.getName());

  private static final boolean DELETE_BILLING_PROJECTS = true;

  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;

  @Autowired
  public CloudTaskWorkspacesController(ImpersonatedWorkspaceService impersonatedWorkspaceService) {
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
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
}
