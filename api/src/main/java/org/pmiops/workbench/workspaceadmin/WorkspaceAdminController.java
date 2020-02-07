package org.pmiops.workbench.workspaceadmin;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.WorkspaceAdminApiDelegate;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.ClusterStatus;
import org.pmiops.workbench.model.ListClusterResponse;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceAdminController implements WorkspaceAdminApiDelegate {
  private final FireCloudService fireCloudService;
  private final LeonardoNotebooksClient leonardoNotebooksClient;
  private final WorkspaceAdminService workspaceAdminService;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public WorkspaceAdminController(
      FireCloudService fireCloudService,
      LeonardoNotebooksClient leonardoNotebooksClient,
      WorkspaceAdminService workspaceAdminService,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService) {
    this.fireCloudService = fireCloudService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.workspaceAdminService = workspaceAdminService;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
  }

  @Override
  @AuthorityRequired({Authority.WORKSPACES_VIEW})
  public ResponseEntity<AdminFederatedWorkspaceDetailsResponse> getFederatedWorkspaceDetails(
      String workspaceNamespace) {
    Optional<DbWorkspace> workspaceMaybe =
        workspaceAdminService.getFirstWorkspaceByNamespace(workspaceNamespace);
    if (workspaceMaybe.isPresent()) {
      DbWorkspace dbWorkspace = workspaceMaybe.get();

      String workspaceFirecloudName = dbWorkspace.getFirecloudName();
      List<UserRole> collaborators =
          workspaceService.getFirecloudUserRoles(workspaceNamespace, workspaceFirecloudName);

      AdminWorkspaceObjectsCounts adminWorkspaceObjects =
          workspaceAdminService.getAdminWorkspaceObjects(dbWorkspace.getWorkspaceId());

      AdminWorkspaceCloudStorageCounts adminWorkspaceCloudStorageCounts =
          workspaceAdminService.getAdminWorkspaceCloudStorageCounts(
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

      List<org.pmiops.workbench.notebooks.model.ListClusterResponse> fcClusters =
          leonardoNotebooksClient.listClustersByProjectAsAdmin(workspaceNamespace);
      List<ListClusterResponse> clusters =
          fcClusters.stream()
              .map(
                  fcCluster ->
                      new ListClusterResponse()
                          .clusterName(fcCluster.getClusterName())
                          .createdDate(fcCluster.getCreatedDate())
                          .dateAccessed(fcCluster.getDateAccessed())
                          .googleProject(fcCluster.getGoogleProject())
                          .labels(fcCluster.getLabels())
                          .status(ClusterStatus.fromValue(fcCluster.getStatus().toString())))
              .collect(Collectors.toList());

      AdminWorkspaceResources resources =
          new AdminWorkspaceResources()
              .workspaceObjects(adminWorkspaceObjects)
              .cloudStorage(adminWorkspaceCloudStorageCounts)
              .clusters(clusters);

      FirecloudWorkspace fcWorkspace =
          fireCloudService.getWorkspace(workspaceNamespace, workspaceFirecloudName).getWorkspace();
      AdminFederatedWorkspaceDetailsResponse response =
          new AdminFederatedWorkspaceDetailsResponse()
              .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace))
              .collaborators(collaborators)
              .resources(resources);
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
