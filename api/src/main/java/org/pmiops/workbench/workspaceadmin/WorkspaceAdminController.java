package org.pmiops.workbench.workspaceadmin;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.WorkspaceAdminApiDelegate;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.AdminWorkspaceResourcesWorkspaceObjects;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceAdminController implements WorkspaceAdminApiDelegate {
  private final FireCloudService fireCloudService;
  private final WorkspaceAdminService workspaceAdminService;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspaceAdminController(
      FireCloudService fireCloudService,
      WorkspaceAdminService workspaceAdminService,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService
  ) {
    this.fireCloudService = fireCloudService;
    this.workspaceAdminService = workspaceAdminService;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
  }

  @Override
  @AuthorityRequired({Authority.WORKSPACES_VIEW})
  public ResponseEntity<AdminFederatedWorkspaceDetailsResponse> getFederatedWorkspaceDetails(
      String workspaceNamespace) {
    Optional<DbWorkspace> workspaceMaybe = workspaceAdminService.getFirstWorkspaceByNamespace(workspaceNamespace);
    if(workspaceMaybe.isPresent()) {
      DbWorkspace dbWorkspace = workspaceMaybe.get();

      String workspaceFirecloudName = dbWorkspace.getFirecloudName();
      List<UserRole> collaborators =
          workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName);

      AdminWorkspaceResourcesWorkspaceObjects adminWorkspaceObjects = workspaceAdminService.getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId());
      AdminWorkspaceResources resources = new AdminWorkspaceResources()
          .workspaceObjects(adminWorkspaceObjects);

      FirecloudWorkspace fcWorkspace =
          fireCloudService.getWorkspace(workspaceNamespace, workspaceFirecloudName).getWorkspace();
      AdminFederatedWorkspaceDetailsResponse response = new AdminFederatedWorkspaceDetailsResponse()
          .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace))
          .collaborators(collaborators)
          .resources(resources);
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
