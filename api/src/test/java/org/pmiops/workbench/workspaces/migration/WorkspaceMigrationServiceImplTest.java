package org.pmiops.workbench.workspaces.migration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.storagetransfer.v1.proto.TransferTypes;
import jakarta.inject.Provider;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.StorageTransferClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMigrationServiceImplTest {

  private static final String NAMESPACE = "test-ns";
  private static final String TERRA_NAME = "test-ws";
  private static final String POD_ID = "pod-123";
  private static final String GOOGLE_PROJECT = "gcp-project-123";
  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String DEST_BUCKET = "dest-bucket";
  private static final List<String> SELECTED_FOLDERS = List.of("notebooks/", "data/");

  @Mock private WsmClient wsmClient;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private WorkspaceMapper workspaceMapper;
  @Mock private UserDao userDao;
  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private FireCloudService fireCloudService;
  @Mock private InitialCreditsService initialCreditsService;
  @Mock private StorageTransferClient storageTransferClient;
  @Mock private TaskQueueService taskQueueService;

  @InjectMocks private WorkspaceMigrationServiceImpl service;

  private DbWorkspace dbWorkspace;
  private Workspace workspace;
  private RawlsWorkspaceDetails rawlsWorkspace;

  @BeforeEach
  void setup() {
    // Only things used by ALL tests go here
    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(NAMESPACE);
    dbWorkspace.setFirecloudName(TERRA_NAME);
    dbWorkspace.setGoogleProject(GOOGLE_PROJECT);

    workspace = new Workspace();
    workspace.setNamespace(NAMESPACE);
    workspace.setName(TERRA_NAME);
    workspace.setCreator("user@test.com");

    rawlsWorkspace = new RawlsWorkspaceDetails();
    rawlsWorkspace.setBucketName(SOURCE_BUCKET);
    rawlsWorkspace.setGoogleProject(GOOGLE_PROJECT);

    lenient().when(workspaceDao.getRequired(NAMESPACE, TERRA_NAME)).thenReturn(dbWorkspace);
  }

  // ── helper: stubs needed by all startWorkspaceMigration tests ──────────────
  private void setupStartMigrationStubs() {
    when(fireCloudService.getWorkspace(NAMESPACE, TERRA_NAME))
        .thenReturn(new RawlsWorkspaceResponse().workspace(rawlsWorkspace));
    when(workspaceMapper.toApiWorkspace(
            eq(dbWorkspace), any(RawlsWorkspaceDetails.class), eq(initialCreditsService)))
        .thenReturn(workspace);

    DbUser dbUser = new DbUser();
    DbVwbUserPod pod = new DbVwbUserPod();
    pod.setVwbPodId(POD_ID);
    dbUser.setVwbUserPod(pod);
    when(userDao.findUserByUsername(any())).thenReturn(dbUser);

    WorkbenchConfig config = new WorkbenchConfig();

    config.vwb = new WorkbenchConfig.VwbConfig();
    config.vwb.defaultPodId = "default-pod";

    config.server = new WorkbenchConfig.ServerConfig();
    config.server.projectId = "test-lobby-project";

    when(workbenchConfigProvider.get()).thenReturn(config);

    WorkspaceDescription vwbWorkspace = new WorkspaceDescription();
    vwbWorkspace.setId(UUID.randomUUID());
    when(wsmClient.createWorkspaceAsService(any(), any())).thenReturn(vwbWorkspace);
    when(wsmClient.createControlledBucket(any(), any())).thenReturn(DEST_BUCKET);
    when(storageTransferClient.startBucketTransfer(any(), any(), any(), any()))
        .thenReturn("transferJobs/migration-" + GOOGLE_PROJECT);
  }

  @Test
  void startWorkspaceMigration_setsStateToStarting() {
    setupStartMigrationStubs();
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS);

    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.STARTING.name().equals(ws.getMigrationState())));
  }

  @Test
  void startWorkspaceMigration_createsVwbWorkspaceWithCorrectPod() {
    setupStartMigrationStubs();
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS);

    verify(wsmClient).createWorkspaceAsService(workspace, POD_ID);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithSelectedFolders() {
    setupStartMigrationStubs();
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS);

    verify(storageTransferClient)
        .startBucketTransfer(SOURCE_BUCKET, DEST_BUCKET, GOOGLE_PROJECT, SELECTED_FOLDERS);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithEmptyFolders_migratesEntireBucket() {
    setupStartMigrationStubs();
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, List.of());

    verify(storageTransferClient)
        .startBucketTransfer(SOURCE_BUCKET, DEST_BUCKET, GOOGLE_PROJECT, List.of());
  }

  @Test
  void startWorkspaceMigration_pushesStatusTask() {
    setupStartMigrationStubs();
    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS);

    verify(taskQueueService).pushWorkspaceMigrationStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void checkMigrationStatus_requeueTaskIfStillRunning() {
    TransferTypes.TransferJob runningJob =
        TransferTypes.TransferJob.newBuilder()
            .setStatus(TransferTypes.TransferJob.Status.ENABLED)
            .build();
    when(storageTransferClient.getTransferJob(GOOGLE_PROJECT)).thenReturn(runningJob);

    service.checkMigrationStatus(NAMESPACE, TERRA_NAME);

    verify(taskQueueService).pushWorkspaceMigrationStatusTask(NAMESPACE, TERRA_NAME);
    assertThat(dbWorkspace.getMigrationState()).isNotEqualTo(MigrationState.FINISHED.name());
  }

  @Test
  void checkMigrationStatus_setsFinishedWhenComplete() {
    TransferTypes.TransferJob completedJob =
        TransferTypes.TransferJob.newBuilder()
            .setStatus(TransferTypes.TransferJob.Status.DISABLED)
            .build();
    when(storageTransferClient.getTransferJob(GOOGLE_PROJECT)).thenReturn(completedJob);

    service.checkMigrationStatus(NAMESPACE, TERRA_NAME);

    assertThat(dbWorkspace.getMigrationState()).isEqualTo(MigrationState.FINISHED.name());
    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.FINISHED.name().equals(ws.getMigrationState())));
  }
}
