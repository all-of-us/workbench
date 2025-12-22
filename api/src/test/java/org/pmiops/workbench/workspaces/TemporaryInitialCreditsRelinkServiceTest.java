package org.pmiops.workbench.workspaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.inject.Provider;
import java.io.IOException;
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
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class TemporaryInitialCreditsRelinkServiceTest {

  @Mock private FireCloudService fireCloudService;
  @Mock private CloudBillingClient cloudBillingClient;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private TemporaryInitialCreditsRelinkWorkspaceDao temporaryInitialCreditsRelinkWorkspaceDao;
  @Mock private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Mock private WorkbenchConfig workbenchConfig;

  private TemporaryInitialCreditsRelinkService temporaryInitialCreditsRelinkService;

  private DbUser testUser1;
  private DbUser testUser2;

  @BeforeEach
  public void setUp() {
    temporaryInitialCreditsRelinkService =
        new TemporaryInitialCreditsRelinkServiceImpl(
            fireCloudService,
            cloudBillingClient,
            workspaceDao,
            temporaryInitialCreditsRelinkWorkspaceDao,
            workbenchConfigProvider);

    testUser1 = new DbUser().setUserId(101L).setUsername("user1@example.com");

    testUser2 = new DbUser().setUserId(102L).setUsername("user2@example.com");
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);
  }

  @Test
  public void testInitiateTemporaryRelinking() throws IOException, InterruptedException {
    var billingAccount = "test-account-id";

    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.billing = new WorkbenchConfig.BillingConfig();
    workbenchConfig.billing.accountId = billingAccount;
    when(workbenchConfigProvider.get()).thenReturn(workbenchConfig);

    var sourceWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace("sourceWorkspaceNamespace")
            .setGoogleProject("googleProject");
    var destinationWorkspace = new Workspace().namespace("destinationWorkspaceNamespace");

    InOrder inOrder =
        Mockito.inOrder(
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
            sourceWorkspace.getWorkspaceNamespace(), "billingAccounts/" + billingAccount);
    inOrder
        .verify(cloudBillingClient)
        .pollUntilBillingAccountLinked(
            sourceWorkspace.getGoogleProject(), "billingAccounts/" + billingAccount);
  }
}
