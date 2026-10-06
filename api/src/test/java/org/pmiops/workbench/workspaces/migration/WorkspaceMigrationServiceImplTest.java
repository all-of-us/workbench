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
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.VwbConfig.CdrVersionForMigration;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceBucketArchiveDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.*;
import org.pmiops.workbench.google.StorageTransferClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceArchiveStatus;
import org.pmiops.workbench.model.WorkspaceRecoveryStatus;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.wsmanager.model.CloneControlledGcpBigQueryDatasetResult;
import org.pmiops.workbench.wsmanager.model.CreatedControlledGcpGcsBucket;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketAttributes;
import org.pmiops.workbench.wsmanager.model.GcpGcsBucketResource;
import org.pmiops.workbench.wsmanager.model.JobReport;

@ExtendWith(MockitoExtension.class)
public class WorkspaceMigrationServiceImplTest {

  private static final String NAMESPACE = "test-ns";
  private static final String JOB_NAME = "transferJobs/migration-" + NAMESPACE;
  private static final String TERRA_NAME = "test-ws";
  private static final String CREATOR = "user@test.com";
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
  @Mock private ActionAuditQueryService auditQueryService;
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
    workspace.setCreator(CREATOR);

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
    lenient().when(workspaceDao.findByWorkspaceNamespace(NAMESPACE)).thenReturn(dbWorkspace);
  }

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

    when(workspaceDao.findByWorkspaceNamespace(eq(NAMESPACE))).thenReturn(dbWorkspace);

    when(workspaceBucketArchiveDao.findByLegacyWorkspaceId(anyLong())).thenReturn(List.of());

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> service.startWorkspaceRecovery(NAMESPACE, RESEARCH_PURPOSE, POD_ID));

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
            () -> service.startWorkspaceRecovery(NAMESPACE, RESEARCH_PURPOSE, POD_ID));

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

    service.checkRecoveryStatus(NAMESPACE);

    verify(taskQueueService).pushWorkspaceRecoveryStatusTask(NAMESPACE);
  }

  @Test
  void checkRecoveryStatus_marksRecoveredWhenSuccessful() throws MessagingException {

    when(workbenchConfigProvider.get()).thenReturn(config);

    doNothing().when(mailService).sendWorkspaceUnarchivedEmail(any(), anyList());

    TransferTypes.TransferOperation transferOperation =
        TransferTypes.TransferOperation.newBuilder()
            .setStatus(TransferTypes.TransferOperation.Status.SUCCESS)
            .build();

    when(storageTransferClient.getTransferJobStatus(SERVER_PROJECT, RECOVERY_JOB_NAME))
        .thenReturn(transferOperation);

    service.checkRecoveryStatus(NAMESPACE);

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

    service.checkRecoveryStatus(NAMESPACE);

    verify(workspaceDao)
        .save(argThat(ws -> WorkspaceRecoveryStatus.FAILED.name().equals(ws.getRecoveryState())));

    verify(storageTransferClient).deleteTransferJob(SERVER_PROJECT, RECOVERY_JOB_NAME);
  }
}
