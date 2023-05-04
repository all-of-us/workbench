package org.pmiops.workbench.api;

import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.model.DeleteTestUserWorkspacesRequest;
import org.pmiops.workbench.model.TestUserWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskWorkspacesController implements CloudTaskWorkspacesApiDelegate {
  private static final Logger LOGGER =
      Logger.getLogger(CloudTaskWorkspacesController.class.getName());

  private final ImpersonatedWorkspaceService impersonatedWorkspaceService;

  @Autowired
  public CloudTaskWorkspacesController(ImpersonatedWorkspaceService impersonatedWorkspaceService) {
    this.impersonatedWorkspaceService = impersonatedWorkspaceService;
  }

  @Override
  public ResponseEntity<Void> deleteTestUserWorkspaces(DeleteTestUserWorkspacesRequest request) {
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
                workspace.getWsNamespace(),
                workspace.getWsFirecloudId(),
                true);
          } catch (NotFoundException e) {
            LOGGER.info(
                String.format(
                    "Workspace %s/%s was not found - may have been concurrently deleted",
                    workspace.getWsNamespace(), workspace.getWsFirecloudId()));
          }
        });

    return ResponseEntity.ok().build();
  }
}
