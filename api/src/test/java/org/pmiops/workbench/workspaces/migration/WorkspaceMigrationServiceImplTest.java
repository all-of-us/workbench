package org.pmiops.workbench.workspaces.migration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

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
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
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
import org.pmiops.workbench.wsmanager.model.CreatedControlledGcpGcsBucket;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketAttributes;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketResource;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMigrationServiceImplTest {

  private static final String NAMESPACE = "test-ns";
  private static final String TERRA_NAME = "test-ws";
  private static final String POD_ID = "pod-123";
  private static final String GOOGLE_PROJECT = "gcp-project-123";
  private static final String SERVER_PROJECT = "test-lobby-project";
  private static final String SERVICE_ACCOUNT_EMAIL =
      "all-of-us-workbench-test@appspot.gserviceaccount.com";
  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String DEST_BUCKET = "dest-bucket";
  private static final CreatedControlledGcpGcsBucket CREATED_BUCKET =
      new CreatedControlledGcpGcsBucket()
          .gcpBucket(
              new GcpGcsBucketResource()
                  .attributes(new GcpGcsBucketAttributes().bucketName(DEST_BUCKET)));
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
    DbCdrVersion cdrVersion =
        createDefaultCdrVersion(1).setAccessTier(new DbAccessTier().setShortName("controlled"));
    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(NAMESPACE);
    dbWorkspace.setFirecloudName(TERRA_NAME);
    dbWorkspace.setGoogleProject(GOOGLE_PROJECT);
    dbWorkspace.setCdrVersion(cdrVersion);

    workspace = new Workspace();
    workspace.setNamespace(NAMESPACE);
    workspace.setName(TERRA_NAME);
    workspace.setCreator("user@test.com");

    rawlsWorkspace = new RawlsWorkspaceDetails();
    rawlsWorkspace.setBucketName(SOURCE_BUCKET);
    rawlsWorkspace.setGoogleProject(GOOGLE_PROJECT);

    WorkbenchConfig config = new WorkbenchConfig();
    config.vwb = new WorkbenchConfig.VwbConfig();
    config.vwb.defaultPodId = "default-pod";
    config.server = new WorkbenchConfig.ServerConfig();
    config.server.projectId = SERVER_PROJECT;
    config.auth = new WorkbenchConfig.AuthConfig();
    config.auth.serviceAccountApiUsers = List.of(SERVICE_ACCOUNT_EMAIL);

    lenient().when(workbenchConfigProvider.get()).thenReturn(config);
    lenient().when(workspaceDao.getRequired(NAMESPACE, TERRA_NAME)).thenReturn(dbWorkspace);
  }

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

    lenient().when(userDao.findUserByUsername(any())).thenReturn(dbUser);

    WorkspaceDescription vwbWorkspace = new WorkspaceDescription();
    vwbWorkspace.setId(UUID.randomUUID());

    when(wsmClient.createWorkspaceAsService(any(), any())).thenReturn(vwbWorkspace);
    when(wsmClient.createControlledBucket(any(), any())).thenReturn(CREATED_BUCKET);

    when(storageTransferClient.createTransferJob(any(), any(), any(), any(), any(), any()))
        .thenReturn("transferJobs/migration-" + SERVER_PROJECT);
  }

  @Test
  void startWorkspaceMigration_setsStateToStarting() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID);

    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.STARTING.name().equals(ws.getMigrationState())));
  }

  @Test
  void startWorkspaceMigration_usesProvidedPodId_whenGiven() {
    setupStartMigrationStubs();

    String customPodId = "custom-pod";

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, customPodId);

    verify(wsmClient).createWorkspaceAsService(workspace, customPodId);
  }

  @Test
  void startWorkspaceMigration_fallsBackToUserPod_whenPodIdNull() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, null);

    verify(wsmClient).createWorkspaceAsService(workspace, POD_ID);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithSelectedFolders() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID);

    verify(storageTransferClient)
        .createTransferJob(
            SOURCE_BUCKET,
            DEST_BUCKET,
            NAMESPACE,
            SERVER_PROJECT,
            SELECTED_FOLDERS,
            SERVICE_ACCOUNT_EMAIL);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithEmptyFolders_migratesEntireBucket() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, List.of(), POD_ID);

    verify(storageTransferClient)
        .createTransferJob(
            SOURCE_BUCKET,
            DEST_BUCKET,
            NAMESPACE,
            SERVER_PROJECT,
            List.of(),
            SERVICE_ACCOUNT_EMAIL);
  }

  @Test
  void startWorkspaceMigration_pushesStatusTask() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID);

    verify(taskQueueService).pushWorkspaceMigrationStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void checkMigrationStatus_requeueTaskIfStillRunning() {
    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.IN_PROGRESS)
            .build();
    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, NAMESPACE))
        .thenReturn(transferOperation);

    service.checkMigrationStatus(NAMESPACE, TERRA_NAME);

    verify(taskQueueService).pushWorkspaceMigrationStatusTask(NAMESPACE, TERRA_NAME);
    assertThat(dbWorkspace.getMigrationState()).isNotEqualTo(MigrationState.FINISHED.name());
  }

  @Test
  void checkMigrationStatus_setsFinishedWhenComplete() {
    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.SUCCESS)
            .build();
    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, NAMESPACE))
        .thenReturn(transferOperation);

    service.checkMigrationStatus(NAMESPACE, TERRA_NAME);

    assertThat(dbWorkspace.getMigrationState()).isEqualTo(MigrationState.FINISHED.name());
    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.FINISHED.name().equals(ws.getMigrationState())));
  }
}
