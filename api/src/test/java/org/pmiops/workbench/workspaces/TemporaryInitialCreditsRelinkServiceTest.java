package org.pmiops.workbench.workspaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.inject.Provider;
import java.io.IOException;
import java.sql.Timestamp;
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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
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
            workbenchConfigProvider);
  }

  @Test
  public void testInitiateTemporaryRelinking() throws IOException, InterruptedException {
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
        inOrder(
            temporaryInitialCreditsRelinkWorkspaceDao, fireCloudService, cloudBillingClient);
    when(cloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenReturn(new ProjectBillingInfo());

    temporaryInitialCreditsRelinkService.initiateTemporaryRelinking(
        sourceWorkspace, destinationWorkspace);

    inOrder
        .verify(temporaryInitialCreditsRelinkWorkspaceDao)
        .save(
            new DbTemporaryInitialCreditsRelinkWorkspace()
                .setSourceWorkspaceNamespace(sourceWorkspace.getWorkspaceNamespace())
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
  public void testCleanupTemporarilyRelinkedWorkspaces() throws IOException, InterruptedException {
    String sourceNs1 = "sourceNs1";
    String sourceNs2 = "sourceNs2";
    String destNs1 = "destNs1";
    String destNs2 = "destNs2";
    String destNs3 = "destNs3";

    DbTemporaryInitialCreditsRelinkWorkspace workspace1 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceNamespace(sourceNs1)
            .setDestinationWorkspaceNamespace(destNs1);
    DbTemporaryInitialCreditsRelinkWorkspace workspace2 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceNamespace(sourceNs2)
            .setDestinationWorkspaceNamespace(destNs2);
    DbTemporaryInitialCreditsRelinkWorkspace workspace3 =
        new DbTemporaryInitialCreditsRelinkWorkspace()
            .setSourceWorkspaceNamespace(sourceNs1)
            .setDestinationWorkspaceNamespace(destNs3);
    var workspacesToCheck = List.of(workspace1, workspace2, workspace3);

    var cloneCompleted = OffsetDateTime.now();

    mockGetWorkspaceCalls(destNs1, cloneCompleted);
    mockGetWorkspaceCalls(destNs2, cloneCompleted);
    mockGetWorkspaceCalls(destNs3, null);
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
