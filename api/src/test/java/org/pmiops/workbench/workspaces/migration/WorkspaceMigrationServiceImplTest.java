package org.pmiops.workbench.workspaces.migration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.storagetransfer.v1.proto.TransferTypes;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig.CdrVersionForMigration;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceBucketArchiveDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.StorageTransferClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceArchiveStatus;
import org.pmiops.workbench.model.WorkspaceRecoveryStatus;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.wsmanager.ApiException;
import org.pmiops.workbench.wsmanager.model.CloneControlledGcpBigQueryDatasetResult;
import org.pmiops.workbench.wsmanager.model.CreatedControlledGcpGcsBucket;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketAttributes;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketResource;
import org.pmiops.workbench.wsmanager.model.JobReport;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMigrationServiceImplTest {

  private static final String NAMESPACE = "test-ns";
  private static final String JOB_NAME = "transferJobs/migration-" + NAMESPACE;
  private static final String TERRA_NAME = "test-ws";
  private static final String POD_ID = "pod-123";
  private static final String RESEARCH_PURPOSE = "[{}]";
  private static final String GOOGLE_PROJECT = "gcp-project-123";
  private static final String SERVER_PROJECT = "test-lobby-project";
  private static final String SERVICE_ACCOUNT_EMAIL =
      "all-of-us-workbench-test@appspot.gserviceaccount.com";
  private static final String SOURCE_BUCKET = "source-bucket";
  private static final String DEST_BUCKET = "dest-bucket";
  private static final String ARCHIVE_BUCKET = "all-of-us-archive-ct-bucket";
  private static final String ARCHIVE_PATH = "gs://" + ARCHIVE_BUCKET + "/test-ns/";
  private static final String RECOVERY_JOB_NAME = "transferJobs/migration-recovery-" + NAMESPACE;
  private static final String JOB_ID = UUID.randomUUID().toString();
  private static final CloneControlledGcpBigQueryDatasetResult CLONED_DATASET_RESULT =
      new CloneControlledGcpBigQueryDatasetResult().jobReport(new JobReport().id(JOB_ID));
  private static final CreatedControlledGcpGcsBucket CREATED_BUCKET =
      new CreatedControlledGcpGcsBucket()
          .gcpBucket(
              new GcpGcsBucketResource()
                  .attributes(new GcpGcsBucketAttributes().bucketName(DEST_BUCKET)));
  private static final List<String> SELECTED_FOLDERS = List.of("notebooks/", "data/");
  private static WorkbenchConfig config = new WorkbenchConfig();

  @Mock private WsmClient wsmClient;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private WorkspaceMapper workspaceMapper;
  @Mock private UserDao userDao;
  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private FireCloudService fireCloudService;
  @Mock private InitialCreditsService initialCreditsService;
  @Mock private StorageTransferClient storageTransferClient;
  @Mock private TaskQueueService taskQueueService;
  @Mock private WorkspaceBucketArchiveDao workspaceBucketArchiveDao;
  @Mock private MailService mailService;
  @Mock private WorkspaceService workspaceService;
  // Clock MOCK
  @Mock private Clock clock;

  @InjectMocks private WorkspaceMigrationServiceImpl service;

  private DbWorkspace dbWorkspace;
  private Workspace workspace;
  private RawlsWorkspaceDetails rawlsWorkspace;

  @BeforeEach
  void setup() {

    lenient().when(clock.instant()).thenReturn(Instant.now());
    lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    DbCdrVersion cdrVersion =
        createDefaultCdrVersion(9).setAccessTier(new DbAccessTier().setShortName("controlled"));
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

    config = WorkbenchConfig.createEmptyConfig();
    config.vwb.defaultPodId = "default-pod";
    CdrVersionForMigration cdrVersionForMigration = new CdrVersionForMigration();
    cdrVersionForMigration.cdrVersionId = 9;
    cdrVersionForMigration.workspaceId = "ct-data-collection-wsid";
    cdrVersionForMigration.resourceId = UUID.randomUUID().toString();
    config.vwb.cdrVersionsForMigration = List.of(cdrVersionForMigration);
    config.server.projectId = SERVER_PROJECT;
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
    lenient()
        .when(
            wsmClient.cloneBQDataset(
                vwbWorkspace.getId(),
                config.vwb.cdrVersionsForMigration.get(0).workspaceId,
                UUID.fromString(config.vwb.cdrVersionsForMigration.get(0).resourceId),
                JOB_ID))
        .thenReturn(CLONED_DATASET_RESULT);
    try {
      when(wsmClient.createControlledBucket(any(), any())).thenReturn(CREATED_BUCKET);
    } catch (ApiException e) {
      throw new RuntimeException("Bucket test", e);
    }
    when(wsmClient.getWorkspaceAsService(workspace.getNamespace())).thenReturn(null, vwbWorkspace);

    when(storageTransferClient.createTransferJob(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn("transferJobs/migration-" + SERVER_PROJECT);
  }

  @Test
  void startWorkspaceMigration_setsStateToStarting() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(
        NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID, RESEARCH_PURPOSE);

    verify(workspaceDao, times(2))
        .save(argThat(ws -> MigrationState.STARTING.name().equals(ws.getMigrationState())));
  }

  @Test
  void startWorkspaceMigration_usesProvidedPodId_whenGiven() {
    setupStartMigrationStubs();

    String customPodId = "custom-pod";

    service.startWorkspaceMigration(
        NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, customPodId, RESEARCH_PURPOSE);

    verify(wsmClient).createWorkspaceAsService(workspace, customPodId);
  }

  @Test
  void startWorkspaceMigration_fallsBackToUserPod_whenPodIdNull() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(
        NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, null, RESEARCH_PURPOSE);

    verify(wsmClient).createWorkspaceAsService(workspace, POD_ID);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithSelectedFolders() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(
        NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID, RESEARCH_PURPOSE);

    verify(storageTransferClient)
        .createTransferJob(
            SOURCE_BUCKET,
            null,
            DEST_BUCKET,
            null,
            NAMESPACE,
            SERVER_PROJECT,
            SELECTED_FOLDERS,
            SERVICE_ACCOUNT_EMAIL,
            false);
  }

  @Test
  void startWorkspaceMigration_startsStsTransferWithEmptyFolders_migratesEntireBucket() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(NAMESPACE, TERRA_NAME, List.of(), POD_ID, RESEARCH_PURPOSE);

    verify(storageTransferClient)
        .createTransferJob(
            SOURCE_BUCKET,
            null,
            DEST_BUCKET,
            null,
            NAMESPACE,
            SERVER_PROJECT,
            List.of(),
            SERVICE_ACCOUNT_EMAIL,
            false);
  }

  @Test
  void startWorkspaceMigration_pushesStatusTask() {
    setupStartMigrationStubs();

    service.startWorkspaceMigration(
        NAMESPACE, TERRA_NAME, SELECTED_FOLDERS, POD_ID, RESEARCH_PURPOSE);

    verify(taskQueueService).pushWorkspaceMigrationStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void checkMigrationStatus_requeueTaskIfStillRunning() {
    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.IN_PROGRESS)
            .build();
    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, JOB_NAME))
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
    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, JOB_NAME))
        .thenReturn(transferOperation);

    service.checkMigrationStatus(NAMESPACE, TERRA_NAME);

    assertThat(dbWorkspace.getMigrationState()).isEqualTo(MigrationState.FINISHED.name());
    verify(workspaceDao)
        .save(argThat(ws -> MigrationState.FINISHED.name().equals(ws.getMigrationState())));
  }

  @Test
  void startWorkspaceArchive_startsArchiveSuccessfully() {

    when(fireCloudService.getWorkspaceAsService(NAMESPACE, TERRA_NAME))
        .thenReturn(new RawlsWorkspaceResponse().workspace(rawlsWorkspace));

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of());

    when(workspaceBucketArchiveDao.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    when(storageTransferClient.createTransferJob(
            any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn("transferJobs/migration-archive-" + NAMESPACE);

    service.startWorkspaceArchive(NAMESPACE, TERRA_NAME);

    verify(storageTransferClient)
        .createTransferJob(
            eq(SOURCE_BUCKET),
            isNull(),
            eq("all-of-us-archive-ct-bucket-wb-blazing-lime-5817"),
            eq(NAMESPACE + "/"),
            eq("archive-" + NAMESPACE),
            eq(SERVER_PROJECT),
            isNull(),
            eq(SERVICE_ACCOUNT_EMAIL),
            eq(false));

    verify(storageTransferClient)
        .runTransferJob(SERVER_PROJECT, "transferJobs/migration-archive-" + NAMESPACE);

    verify(taskQueueService).pushWorkspaceArchiveStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void startWorkspaceArchive_skipsIfAlreadyArchived() {

    DbWorkspaceBucketArchive archive =
        new DbWorkspaceBucketArchive().setStatus(WorkspaceArchiveStatus.ARCHIVED.toString());

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of(archive));

    service.startWorkspaceArchive(NAMESPACE, TERRA_NAME);

    verify(storageTransferClient, never())
        .createTransferJob(any(), any(), any(), any(), any(), any(), any(), any(), any());

    verify(taskQueueService, never()).pushWorkspaceArchiveStatusTask(any(), any());
  }

  @Test
  void checkArchiveStatus_requeuesWhenStillRunning() {

    DbWorkspaceBucketArchive archive =
        new DbWorkspaceBucketArchive().setStatus(WorkspaceArchiveStatus.IN_PROGRESS.toString());

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of(archive));

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.IN_PROGRESS)
            .build();

    when(storageTransferClient.getTransferJobStatus(
            SERVER_PROJECT, "transferJobs/migration-archive-" + NAMESPACE))
        .thenReturn(transferOperation);

    service.checkArchiveStatus(NAMESPACE, TERRA_NAME);

    verify(taskQueueService).pushWorkspaceArchiveStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void checkArchiveStatus_marksArchiveCompleted() {

    DbWorkspaceBucketArchive archive =
        new DbWorkspaceBucketArchive().setStatus(WorkspaceArchiveStatus.IN_PROGRESS.toString());

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of(archive));

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.SUCCESS)
            .build();

    when(storageTransferClient.getTransferJobStatus(
            SERVER_PROJECT, "transferJobs/migration-archive-" + NAMESPACE))
        .thenReturn(transferOperation);

    service.checkArchiveStatus(NAMESPACE, TERRA_NAME);

    assertThat(archive.getStatus()).isEqualTo(WorkspaceArchiveStatus.ARCHIVED.toString());

    verify(workspaceBucketArchiveDao).save(archive);

    verify(storageTransferClient)
        .deleteTransferJob(SERVER_PROJECT, "transferJobs/migration-archive-" + NAMESPACE);
  }

  @Test
  void checkArchiveStatus_marksArchiveFailed() {

    DbWorkspaceBucketArchive archive =
        new DbWorkspaceBucketArchive().setStatus(WorkspaceArchiveStatus.IN_PROGRESS.toString());

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of(archive));

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.FAILED)
            .build();

    when(storageTransferClient.getTransferJobStatus(
            SERVER_PROJECT, "transferJobs/migration-archive-" + NAMESPACE))
        .thenReturn(transferOperation);

    service.checkArchiveStatus(NAMESPACE, TERRA_NAME);

    assertThat(archive.getStatus()).isEqualTo(WorkspaceArchiveStatus.FAILED.toString());

    verify(workspaceBucketArchiveDao).save(archive);
  }

  //  private void setupRecoveryStubs() {
  //
  //    // Rawls workspace lookup
  //    when(fireCloudService.getWorkspace(anyString(), anyString()))
  //        .thenReturn(new RawlsWorkspaceResponse().workspace(rawlsWorkspace));
  //
  //    // Workspace returned from mapper/service
  //    when(workspaceMapper.toApiWorkspace(
  //            eq(dbWorkspace), any(RawlsWorkspaceDetails.class), eq(initialCreditsService)))
  //        .thenReturn(workspace);
  //
  //    // User pod lookup
  //    DbUser dbUser = new DbUser();
  //
  //    DbVwbUserPod pod = new DbVwbUserPod();
  //    pod.setVwbPodId(POD_ID);
  //
  //    dbUser.setVwbUserPod(pod);
  //
  //    when(userDao.findUserByUsername(any())).thenReturn(dbUser);
  //
  //    // New VWB workspace creation
  //    WorkspaceDescription vwbWorkspace = new WorkspaceDescription();
  //
  //    vwbWorkspace.setId(UUID.randomUUID());
  //
  //    when(wsmClient.createWorkspaceAsService(any(), any())).thenReturn(vwbWorkspace);
  //
  //    // BQ clone
  //    when(wsmClient.cloneBQDataset(any(), any(), any(),
  // any())).thenReturn(CLONED_DATASET_RESULT);
  //
  //    // Archive lookup
  //    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong()))
  //        .thenReturn(
  //            List.of(
  //                new DbWorkspaceBucketArchive()
  //                    .setStatus(WorkspaceArchiveStatus.ARCHIVED.toString())
  //                    .setGcsPath(ARCHIVE_PATH)));
  //
  //    // Recovery transfer job
  //    when(storageTransferClient.createTransferJob(
  //            any(), any(), any(), any(), any(), any(), any(), any()))
  //        .thenReturn(RECOVERY_JOB_NAME);
  //  }

  @Test
  void startWorkspaceRecovery_findsArchiveMetadata() {

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong()))
        .thenReturn(
            List.of(
                new DbWorkspaceBucketArchive()
                    .setStatus(WorkspaceArchiveStatus.ARCHIVED.toString())
                    .setGcsPath(ARCHIVE_PATH)));

    assertThat(workspaceBucketArchiveDao.findByLegacyWorkspaceId(dbWorkspace.getWorkspaceId()))
        .hasSize(1);

    verify(workspaceBucketArchiveDao, times(1)).findByLegacyWorkspaceId(anyLong());
  }

  @Test
  void startWorkspaceRecovery_failsIfArchiveMissing() {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(123L);
    dbWorkspace.setRecoveryState(WorkspaceRecoveryStatus.REQUESTED.name());

    when(workspaceDao.getRequired(eq(NAMESPACE), eq(TERRA_NAME))).thenReturn(dbWorkspace);

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> service.startWorkspaceRecovery(NAMESPACE, TERRA_NAME, RESEARCH_PURPOSE));

    assertThat(ex.getMessage()).contains("Recovery failed to start");
    assertThat(ex.getCause()).isNotNull();
    assertThat(ex.getCause().getMessage()).contains("Archive metadata not found");
  }

  @Test
  void startWorkspaceRecovery_failsIfRecoveryNotRequested() {
    workspace.setRecoveryState(WorkspaceRecoveryStatus.NOT_STARTED);

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> service.startWorkspaceRecovery(NAMESPACE, TERRA_NAME, RESEARCH_PURPOSE));

    assertThat(ex.getMessage())
        .contains("Workspace recovery can only start when state is REQUESTED");
  }

  @Test
  void checkRecoveryStatus_requeuesWhenStillRunning() {

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.IN_PROGRESS)
            .build();

    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, RECOVERY_JOB_NAME))
        .thenReturn(transferOperation);

    service.checkRecoveryStatus(NAMESPACE, TERRA_NAME);

    verify(taskQueueService).pushWorkspaceRecoveryStatusTask(NAMESPACE, TERRA_NAME);
  }

  @Test
  void checkRecoveryStatus_marksRecoveredWhenSuccessful() throws MessagingException {

    when(workbenchConfigProvider.get()).thenReturn(config);

    when(workspaceService.getWorkspaceOwnerList(any(DbWorkspace.class)))
        .thenReturn(List.of(new DbUser()));

    doNothing().when(mailService).sendWorkspaceUnarchivedEmail(any(), anyList());

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.SUCCESS)
            .build();

    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, RECOVERY_JOB_NAME))
        .thenReturn(transferOperation);

    service.checkRecoveryStatus(NAMESPACE, TERRA_NAME);

    verify(workspaceDao)
        .save(
            argThat(ws -> WorkspaceRecoveryStatus.RECOVERED.name().equals(ws.getRecoveryState())));

    verify(storageTransferClient).deleteTransferJob(SERVER_PROJECT, RECOVERY_JOB_NAME);
  }

  @Test
  void checkRecoveryStatus_marksFailed() {

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.FAILED)
            .build();

    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, RECOVERY_JOB_NAME))
        .thenReturn(transferOperation);

    service.checkRecoveryStatus(NAMESPACE, TERRA_NAME);

    verify(workspaceDao)
        .save(argThat(ws -> WorkspaceRecoveryStatus.FAILED.name().equals(ws.getRecoveryState())));

    verify(storageTransferClient).deleteTransferJob(SERVER_PROJECT, RECOVERY_JOB_NAME);
  }
}
