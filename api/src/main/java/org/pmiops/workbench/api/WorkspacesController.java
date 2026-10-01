package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import java.time.Clock;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.FolderSyncTransferDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.db.model.DbWorkspaceOperation.DbWorkspaceOperationStatus;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceFactory;
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspacesController implements WorkspacesApiDelegate {

  private static final Logger log = Logger.getLogger(WorkspacesController.class.getName());
  private final InitialCreditsService initialCreditsService;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;
  private final WorkspaceOperationDao workspaceOperationDao;
  private final WorkspaceService workspaceService;
  private final WorkspaceMigrationService workspaceMigrationService;
  private final VwbUserService vwbUserService;
  private final WorkspaceServiceFactory workspaceServiceFactory;
  private final WsmClient wsmClient;
  private final VwbAdminQueryService vwbAdminQueryService;

  @Autowired
  public WorkspacesController(
      CdrVersionDao cdrVersionDao,
      Clock clock,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      TaskQueueService taskQueueService,
      UserDao userDao,
      WorkspaceAuditor workspaceAuditor,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper,
      WorkspaceOperationDao workspaceOperationDao,
      WorkspaceOperationMapper workspaceOperationMapper,
      WorkspaceService workspaceService,
      WorkspaceMigrationService workspaceMigrationService,
      WorkspaceServiceFactory workspaceServiceFactory,
      VwbUserService vwbUserService,
      WsmClient wsmClient,
      VwbAdminQueryService vwbAdminQueryService,
      FolderSyncTransferDao folderSyncTransferDao) {
    this.cdrVersionDao = cdrVersionDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
    this.taskQueueService = taskQueueService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAuditor = workspaceAuditor;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.workspaceMapper = workspaceMapper;
    this.workspaceOperationDao = workspaceOperationDao;
    this.workspaceOperationMapper = workspaceOperationMapper;
    this.workspaceService = workspaceService;
    this.workspaceMigrationService = workspaceMigrationService;
    this.workspaceServiceFactory = workspaceServiceFactory;
    this.vwbUserService = vwbUserService;
    this.wsmClient = wsmClient;
    this.folderSyncTransferDao = folderSyncTransferDao;
    this.vwbAdminQueryService = vwbAdminQueryService;
  }

  @Override
  public ResponseEntity<VwbWorkspaceListResponse> getVwbWorkspaces() {

    return ResponseEntity.ok(
        new VwbWorkspaceListResponse()
            .items(
                vwbAdminQueryService.queryAccessibleWorkspaces(userProvider.get().getUsername())));
  }

  private void processWorkspaceTask(long operationId, Supplier<Workspace> workspaceAction) {
    DbWorkspaceOperation operation =
        workspaceOperationDao
            .findById(operationId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Workspace Operation '%d' not found", operationId)));

    if (operation.getStatus() != DbWorkspaceOperationStatus.QUEUED) {
      log.warning(
          String.format(
              "processWorkspaceTask: exiting because operation %d is in %s state instead of QUEUED",
              operation.getId(), operation.getStatus().toString()));
      return;
    }

    try {
      log.info(
          String.format(
              "processWorkspaceTask: begin processing operation %d by transitioning from %s to %s",
              operation.getId(),
              operation.getStatus().toString(),
              DbWorkspaceOperationStatus.PROCESSING));
      operation =
          workspaceOperationDao.save(operation.setStatus(DbWorkspaceOperationStatus.PROCESSING));

      Workspace w = workspaceAction.get();
      long workspaceId =
          workspaceDao.getRequired(w.getNamespace(), w.getTerraName()).getWorkspaceId();
      log.info(
          String.format(
              "processWorkspaceTask: recording SUCCESS for operation %d - workspace ID %d",
              operation.getId(), workspaceId));
      operation.setStatus(DbWorkspaceOperationStatus.SUCCESS).setWorkspaceId(workspaceId);
    } catch (Exception e) {
      log.info(
          String.format(
              "processWorkspaceTask: recording ERROR for operation %d", operation.getId()));
      operation.setStatus(DbWorkspaceOperationStatus.ERROR);
      throw e;
    } finally {
      operation = workspaceOperationDao.save(operation);
    }
  }

  @Override
  public ResponseEntity<String> getWorkspaceAccess(String workspaceNamespace) {
    try {
      DbWorkspace workspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
      return ResponseEntity.ok(
          workspaceAuthService
              .enforceWorkspaceAccessLevel(
                  workspace.getWorkspaceNamespace(),
                  workspace.getFirecloudName(),
                  WorkspaceAccessLevel.READER)
              .toString());
    } catch (NotFoundException nfe) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(nfe.getMessage());
    } catch (ForbiddenException uae) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(uae.getMessage());
    }
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
