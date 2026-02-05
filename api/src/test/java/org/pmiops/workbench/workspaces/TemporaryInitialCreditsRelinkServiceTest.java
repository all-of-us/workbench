package org.pmiops.workbench.workspaces;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.inject.Provider;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.TemporaryInitialCreditsRelinkWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbTemporaryInitialCreditsRelinkWorkspace;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;

@ExtendWith(MockitoExtension.class)
public class TemporaryInitialCreditsRelinkServiceTest {

  @Mock private FireCloudService fireCloudService;
  @Mock private CloudBillingClient cloudBillingClient;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private TemporaryInitialCreditsRelinkWorkspaceDao temporaryInitialCreditsRelinkWorkspaceDao;
  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private InitialCreditsService initialCreditsService;

  private TemporaryInitialCreditsRelinkService temporaryInitialCreditsRelinkService;
  String initialCreditsBillingAccountId = "test-account-id";

  @BeforeEach
  public void setUp() {
    temporaryInitialCreditsRelinkService =
        new TemporaryInitialCreditsRelinkServiceImpl(
            fireCloudService,
            cloudBillingClient,
            workspaceDao,
            temporaryInitialCreditsRelinkWorkspaceDao,
            workbenchConfigProvider,
            initialCreditsService);
  }

  @Test
  public void testInitiateTemporaryRelinking_success() throws IOException, InterruptedException {
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.billing = new WorkbenchConfig.BillingConfig();
    workbenchConfig.billing.accountId = initialCreditsBillingAccountId;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);

    var sourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace("sourceWorkspaceNamespace")
            .setGoogleProject("googleProject");
    var destinationWorkspace = new Workspace().namespace("destinationWorkspaceNamespace");

    InOrder inOrder =
        inOrder(temporaryInitialCreditsRelinkWorkspaceDao, fireCloudService, cloudBillingClient);
    when(cloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenReturn(new ProjectBillingInfo());

    temporaryInitialCreditsRelinkService.initiateTemporaryRelinking(
        sourceWorkspace, destinationWorkspace);

    inOrder
        .verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(
            new DbTemporaryInitialCreditsRelinkWorkspace()
                .setSourceWorkspaceId(sourceWorkspace.getWorkspaceId())
                .setDestinationWorkspaceNamespace(destinationWorkspace.getNamespace()));
    inOrder
        .verify(fireCloudService)
        .updateBillingAccountAsService(
            sourceWorkspace.getWorkspaceNamespace(),
            "billingAccounts/" + initialCreditsBillingAccountId);
    inOrder
        .verify(cloudBillingClient)
        .pollUntilBillingAccountLinked(
            sourceWorkspace.getGoogleProject(),
            "billingAccounts/" + initialCreditsBillingAccountId);
  }

  @Test
  public void testInitiateTemporaryRelinking_failure() throws IOException, InterruptedException {
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.billing = new WorkbenchConfig.BillingConfig();
    workbenchConfig.billing.accountId = initialCreditsBillingAccountId;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);

    var sourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace("sourceWorkspaceNamespace")
            .setGoogleProject("googleProject");
    var destinationWorkspace = new Workspace().namespace("destinationWorkspaceNamespace");

    when(cloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenThrow(new IOException());

    assertThrows(
        ServerErrorException.class,
        () ->
            temporaryInitialCreditsRelinkService.initiateTemporaryRelinking(
                sourceWorkspace, destinationWorkspace));

    // Verify that we clean up after a failure
    verify(temporaryInitialCreditsRelinkWorkspaceDao).delete(any());
  }

  @Test
  public void testCleanupTemporarilyRelinkedWorkspaces_success()
      throws IOException, InterruptedException {
    long sourceId1 = 1L;
    long sourceId2 = 2L;
    String sourceNs1 = "sourceNs1";
    String sourceNs2 = "sourceNs2";
    String destNs1 = "destNs1";
    String destNs2 = "destNs2";
    String destNs3 = "destNs3";

    DbTemporaryInitialCreditsRelinkWorkspace workspace1 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId1)
            .setDestinationWorkspaceNamespace(destNs1);
    DbTemporaryInitialCreditsRelinkWorkspace workspace2 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId2)
            .setDestinationWorkspaceNamespace(destNs2);
    DbTemporaryInitialCreditsRelinkWorkspace workspace3 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId1)
            .setDestinationWorkspaceNamespace(destNs3);
    var workspacesToCheck = List.of(workspace1, workspace2, workspace3);

    var cloneCompleted = OffsetDateTime.now();

    mockGetWorkspaceCalls(destNs1, cloneCompleted);
    mockGetWorkspaceCalls(destNs2, cloneCompleted);
    mockGetWorkspaceCalls(destNs3, null);

    DbUser creator = new DbUser();
    DbWorkspace sourceWorkspace2 =
        new DbWorkspace()
            .setWorkspaceNamespace(sourceNs2)
            .setInitialCreditsExhausted(true)
            .setCreator(creator);
    when(workspaceDao.findActiveByWorkspaceId(sourceId2)).thenReturn(Optional.of(sourceWorkspace2));
    when(initialCreditsService.areUserCreditsExpired(creator)).thenReturn(true);
    when(temporaryInitialCreditsRelinkWorkspaceDao.findByCloneCompletedIsNull())
        .thenReturn(workspacesToCheck);

    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    // All active clones from sourceNs2 are complete, so billing should be removed
    verify(fireCloudService).removeBillingAccountFromBillingProjectAsService(sourceNs2);
    // There is still an active clone from sourceNs1, so temporary relink should remain
    verify(fireCloudService, times(0)).removeBillingAccountFromBillingProjectAsService(sourceNs1);
    // workspace1 is done cloning, it should be marked as such
    verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(workspace1.setCloneCompleted(Timestamp.from(cloneCompleted.toInstant())));
    // workspace2 is done cloning, it should be marked as such
    verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(workspace2.setCloneCompleted(Timestamp.from(cloneCompleted.toInstant())));
    // workspace3 is still in progress, it should not be updated
    verify(temporaryInitialCreditsRelinkWorkspaceDao, times(0)).save(workspace3);
  }

  @Test
  public void testCleanupTemporarilyRelinkedWorkspaces_missingWorkspaces()
      throws IOException, InterruptedException {
    long sourceId = 1L;
    String sourceNs = "sourceNs";
    String missingInDatabaseTimedOutNs = "missingInDatabaseTimedOutNs";
    String missingInDatabaseNewNs = "missingInDatabaseNewNs";
    String missingInTerraTimedOutNs = "missingInTerraTimedOutNs";
    String missingInTerraNewNs = "missingInTerraNewNs";

    DbTemporaryInitialCreditsRelinkWorkspace missingInDatabaseTimedOut =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(missingInDatabaseTimedOutNs)
            .setCreated(Timestamp.from(Instant.now().minus(Duration.ofHours(2))));
    DbTemporaryInitialCreditsRelinkWorkspace missingInDatabaseNew =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(missingInDatabaseNewNs)
            .setCreated(Timestamp.from(Instant.now()));
    DbTemporaryInitialCreditsRelinkWorkspace missingInTerraTimedOut =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(missingInTerraTimedOutNs)
            .setCreated(Timestamp.from(Instant.now().minus(Duration.ofHours(2))));
    DbTemporaryInitialCreditsRelinkWorkspace missingInTerraNew =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(missingInTerraNewNs)
            .setCreated(Timestamp.from(Instant.now()));
    var workspacesToCheck =
        List.of(
            missingInDatabaseTimedOut,
            missingInDatabaseNew,
            missingInTerraTimedOut,
            missingInTerraNew);

    when(workspaceDao.getByNamespace(missingInDatabaseTimedOutNs)).thenReturn(Optional.empty());
    when(workspaceDao.getByNamespace(missingInDatabaseNewNs)).thenReturn(Optional.empty());
    when(workspaceDao.getByNamespace(missingInTerraTimedOutNs))
        .thenReturn(
            Optional.of(
                new DbWorkspace()
                    .setWorkspaceNamespace(missingInTerraTimedOutNs)
                    .setFirecloudName("fc-" + missingInTerraTimedOutNs)));
    when(fireCloudService.getWorkspaceAsService(
            missingInTerraTimedOutNs, "fc-" + missingInTerraTimedOutNs))
        .thenThrow(new RuntimeException("Missing workspace!"));
    when(workspaceDao.getByNamespace(missingInTerraNewNs))
        .thenReturn(
            Optional.of(
                new DbWorkspace()
                    .setWorkspaceNamespace(missingInTerraNewNs)
                    .setFirecloudName("fc-" + missingInTerraNewNs)));
    when(fireCloudService.getWorkspaceAsService(missingInTerraNewNs, "fc-" + missingInTerraNewNs))
        .thenThrow(new RuntimeException("Missing workspace!"));
    when(temporaryInitialCreditsRelinkWorkspaceDao.findByCloneCompletedIsNull())
        .thenReturn(workspacesToCheck);

    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    verify(fireCloudService, never()).removeBillingAccountFromBillingProjectAsService(sourceNs);
    verify(temporaryInitialCreditsRelinkWorkspaceDao, times(2)).save(any());
    verify(temporaryInitialCreditsRelinkWorkspaceDao, never()).save(missingInDatabaseNew);
    verify(temporaryInitialCreditsRelinkWorkspaceDao, never()).save(missingInTerraNew);
  }

  @Test
  public void testCleanupTemporarilyRelinkedWorkspaces_cleanupFailure()
      throws IOException, InterruptedException {
    long failedRemovalSourceId = 1L;
    long successfulRemovalSourceId = 2L;
    String failedRemovalSourceNs = "failedRemovalSourceNs";
    String successfulRemovalSourceNs = "successfulRemovalSourceNs";
    String destNs1 = "destNs1";
    String destNs2 = "destNs2";

    DbTemporaryInitialCreditsRelinkWorkspace failedRemovalWorkspace =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(failedRemovalSourceId)
            .setDestinationWorkspaceNamespace(destNs1);
    DbTemporaryInitialCreditsRelinkWorkspace successfulRemovalWorkspace =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(successfulRemovalSourceId)
            .setDestinationWorkspaceNamespace(destNs2);

    var workspacesToCheck = List.of(failedRemovalWorkspace, successfulRemovalWorkspace);

    var cloneCompleted = OffsetDateTime.now();

    mockGetWorkspaceCalls(destNs1, cloneCompleted);
    mockGetWorkspaceCalls(destNs2, cloneCompleted);

    DbUser creator1 = new DbUser();
    DbUser creator2 = new DbUser();
    DbWorkspace failedSourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(failedRemovalSourceNs)
            .setInitialCreditsExhausted(true)
            .setCreator(creator1);
    DbWorkspace successfulSourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(successfulRemovalSourceNs)
            .setInitialCreditsExhausted(true)
            .setCreator(creator2);

    when(workspaceDao.findActiveByWorkspaceId(successfulRemovalSourceId))
        .thenReturn(Optional.of(successfulSourceWorkspace));
    when(workspaceDao.findActiveByWorkspaceId(failedRemovalSourceId))
        .thenReturn(Optional.of(failedSourceWorkspace));
    when(initialCreditsService.areUserCreditsExpired(creator1)).thenReturn(true);
    when(initialCreditsService.areUserCreditsExpired(creator2)).thenReturn(true);
    when(temporaryInitialCreditsRelinkWorkspaceDao.findByCloneCompletedIsNull())
        .thenReturn(workspacesToCheck);
    doThrow(new RuntimeException("Failure!"))
        .when(fireCloudService)
        .removeBillingAccountFromBillingProjectAsService(failedRemovalSourceNs);

    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    // We should attempt cleanup on both source namespaces, even if one fails
    verify(fireCloudService).removeBillingAccountFromBillingProjectAsService(failedRemovalSourceNs);
    verify(fireCloudService)
        .removeBillingAccountFromBillingProjectAsService(successfulRemovalSourceNs);
    // We will not mark a temporary grant as completed until we verify that we've successfully
    // removed billing
    verify(temporaryInitialCreditsRelinkWorkspaceDao, never())
        .save(failedRemovalWorkspace.setCloneCompleted(Timestamp.from(cloneCompleted.toInstant())));
    // This workspace cleaned up correctly, mark it as such.
    verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(
            successfulRemovalWorkspace.setCloneCompleted(
                Timestamp.from(cloneCompleted.toInstant())));
  }

  @Test
  public void testCleanupTemporarilyRelinkedWorkspaces_creditsNotExhausted()
      throws IOException, InterruptedException {
    long sourceId = 1L;
    String sourceNs = "sourceNs";
    String destNs = "destNs";

    DbTemporaryInitialCreditsRelinkWorkspace workspace =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(destNs);
    var workspacesToCheck = List.of(workspace);

    var cloneCompleted = OffsetDateTime.now();

    mockGetWorkspaceCalls(destNs, cloneCompleted);

    DbUser creator = new DbUser();
    DbWorkspace sourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(sourceNs)
            .setInitialCreditsExhausted(false) // Credits NOT exhausted
            .setCreator(creator);
    when(workspaceDao.findActiveByWorkspaceId(sourceId)).thenReturn(Optional.of(sourceWorkspace));
    when(temporaryInitialCreditsRelinkWorkspaceDao.findByCloneCompletedIsNull())
        .thenReturn(workspacesToCheck);

    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    // Billing should NOT be removed because credits are not exhausted
    verify(fireCloudService, never()).removeBillingAccountFromBillingProjectAsService(sourceNs);
    // Workspace should still be marked as completed
    verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(workspace.setCloneCompleted(Timestamp.from(cloneCompleted.toInstant())));
  }

  @Test
  public void testCleanupTemporarilyRelinkedWorkspaces_creditsNotExpired()
      throws IOException, InterruptedException {
    long sourceId = 1L;
    String sourceNs = "sourceNs";
    String destNs = "destNs";

    DbTemporaryInitialCreditsRelinkWorkspace workspace =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceId(sourceId)
            .setDestinationWorkspaceNamespace(destNs);
    var workspacesToCheck = List.of(workspace);

    var cloneCompleted = OffsetDateTime.now();

    mockGetWorkspaceCalls(destNs, cloneCompleted);

    DbUser creator = new DbUser();
    DbWorkspace sourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(sourceNs)
            .setInitialCreditsExhausted(true)
            .setCreator(creator);
    when(workspaceDao.findActiveByWorkspaceId(sourceId)).thenReturn(Optional.of(sourceWorkspace));
    when(initialCreditsService.areUserCreditsExpired(creator))
        .thenReturn(false); // Credits NOT expired
    when(temporaryInitialCreditsRelinkWorkspaceDao.findByCloneCompletedIsNull())
        .thenReturn(workspacesToCheck);

    temporaryInitialCreditsRelinkService.cleanupTemporarilyRelinkedWorkspaces();

    // Billing should NOT be removed because credits are not expired
    verify(fireCloudService, never()).removeBillingAccountFromBillingProjectAsService(sourceNs);
    // Workspace should still be marked as completed
    verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(workspace.setCloneCompleted(Timestamp.from(cloneCompleted.toInstant())));
  }

  private void mockGetWorkspaceCalls(String namespace, OffsetDateTime cloneCompleted) {
    String fcName = "fc-" + namespace;
    when(workspaceDao.getByNamespace(namespace))
        .thenReturn(
            Optional.of(
                new DbWorkspace().setWorkspaceNamespace(namespace).setFirecloudName(fcName)));
    when(fireCloudService.getWorkspaceAsService(namespace, fcName))
        .thenReturn(
            new RawlsWorkspaceResponse()
                .workspace(
                    new RawlsWorkspaceDetails()
                        .namespace(namespace)
                        .name(fcName)
                        .completedCloneWorkspaceFileTransfer(cloneCompleted)));
  }
}
