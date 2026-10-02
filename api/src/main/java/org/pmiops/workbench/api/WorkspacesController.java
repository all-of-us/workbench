package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.util.*;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceFactory;
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspacesController implements WorkspacesApiDelegate {
  private final InitialCreditsService initialCreditsService;
  private final Provider<DbUser> userProvider;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceService workspaceService;
  private final WorkspaceMigrationService workspaceMigrationService;
  private final VwbUserService vwbUserService;
  private final WorkspaceServiceFactory workspaceServiceFactory;
  private final WsmClient wsmClient;
  private final VwbAdminQueryService vwbAdminQueryService;

  @Autowired
  public WorkspacesController(
      InitialCreditsService initialCreditsService,
      Provider<DbUser> userProvider,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceService workspaceService,
      WorkspaceMigrationService workspaceMigrationService,
      WorkspaceServiceFactory workspaceServiceFactory,
      VwbUserService vwbUserService,
      WsmClient wsmClient,
      VwbAdminQueryService vwbAdminQueryService) {
    this.initialCreditsService = initialCreditsService;
    this.userProvider = userProvider;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceService = workspaceService;
    this.workspaceMigrationService = workspaceMigrationService;
    this.workspaceServiceFactory = workspaceServiceFactory;
    this.vwbUserService = vwbUserService;
    this.wsmClient = wsmClient;
    this.vwbAdminQueryService = vwbAdminQueryService;
  }

  @Override
  public ResponseEntity<VwbWorkspaceListResponse> getVwbWorkspaces() {

    return ResponseEntity.ok(
        new VwbWorkspaceListResponse()
            .items(
                vwbAdminQueryService.queryAccessibleWorkspaces(userProvider.get().getUsername())));
  }

  @Override
  public ResponseEntity<WorkspaceResponse> getWorkspace(
      String workspaceNamespace, String workspaceTerraName) {
    DbWorkspace dbWorkspace = workspaceDao.getRequired(workspaceNamespace, workspaceTerraName);
    WorkspaceService workspaceService =
        workspaceServiceFactory.getWorkspaceService(dbWorkspace.isVwbWorkspace());
    return ResponseEntity.ok(workspaceService.getWorkspace(workspaceNamespace, workspaceTerraName));
  }

  @Override
  public ResponseEntity<Workspace> getWorkspaceByNamespace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceDao.findByWorkspaceNamespace(workspaceNamespace);
    return ResponseEntity.ok(
        workspaceMapper.toApiWorkspace(dbWorkspace, null, initialCreditsService));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
    return ResponseEntity.ok(
        new WorkspaceResponseListResponse().items(workspaceService.listWorkspaces()));
  }

  @Override
  public ResponseEntity<Void> startWorkspaceArchive(String workspaceNamespace, String terraName) {

    workspaceMigrationService.startWorkspaceArchive(workspaceNamespace, terraName);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> retryWorkspaceArchive(String status) {

    workspaceMigrationService.retryNextArchiveByStatus(status);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> startWorkspaceRecovery(
      String namespace, StartWorkspaceRecoveryRequest request) {

    workspaceMigrationService.startWorkspaceRecovery(
        namespace, request.getResearchPurpose(), request.getPodId());

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> requestWorkspaceRecovery(String namespace, String podId) {

    workspaceMigrationService.requestWorkspaceRecovery(namespace, podId);

    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Boolean> vwbWorkspaceExists(String namespace) {
    return ResponseEntity.ok(wsmClient.getWorkspaceAsService(namespace) != null);
  }

  @Override
  public ResponseEntity<MigrationBucketContentsResponse> getMigrationBucketContents(
      String namespace, String terraName) {

    MigrationBucketContentsResponse response =
        workspaceMigrationService.getBucketContents(namespace, terraName);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<List<VwbPodDescription>> getUserPods() {
    String userEmail = userProvider.get().getUsername();
    return ResponseEntity.ok(
        vwbUserService.getUserPods(userEmail).stream()
            .map(
                p -> {
                  VwbPodDescription pod = new VwbPodDescription();
                  pod.setPodId(UUID.fromString(p.getPodId().toString()));
                  pod.setUserFacingId(p.getUserFacingId());
                  pod.setDescription(p.getDescription());
                  return pod;
                })
            .toList());
  }
}
