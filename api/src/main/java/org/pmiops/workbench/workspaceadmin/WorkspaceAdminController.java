package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.WorkspaceAdminApiDelegate;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class WorkspaceAdminController implements WorkspaceAdminApiDelegate {
  private final WorkspaceAdminService workspaceAdminService;

  @Autowired
  public WorkspaceAdminController(
      WorkspaceAdminService workspaceAdminService
  ) {
    this.workspaceAdminService = workspaceAdminService;
  }

  @Override
  @AuthorityRequired({Authority.WORKSPACES_VIEW})
  public ResponseEntity<AdminFederatedWorkspaceDetailsResponse> getFederatedWorkspaceDetails(
      String workspaceNamespace) {
    Optional<DbWorkspace> workspaceMaybe = workspaceAdminService.getFirstWorkspaceByNamespace(workspaceNamespace);
    if(workspaceMaybe.isPresent()) {
      AdminFederatedWorkspaceDetailsResponse response = new AdminFederatedWorkspaceDetailsResponse();
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
