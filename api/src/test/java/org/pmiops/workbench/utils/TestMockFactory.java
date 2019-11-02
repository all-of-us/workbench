package org.pmiops.workbench.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

/** Created by brubenst on 9/4/19. */
public class TestMockFactory {
  public static final String BUCKET_NAME = "workspace-bucket";

  public org.pmiops.workbench.firecloud.model.Workspace createFcWorkspace(
      String ns, String name, String creator) {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setWorkspaceId(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    fcWorkspace.setBucketName(BUCKET_NAME);
    return fcWorkspace;
  }

  public void stubCreateFcWorkspace(FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              String capturedWorkspaceName = (String) invocation.getArguments()[1];
              String capturedWorkspaceNamespace = (String) invocation.getArguments()[0];
              org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
                  createFcWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null);

              org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
                  new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
              fcResponse.setWorkspace(fcWorkspace);
              fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());

              doReturn(fcResponse)
                  .when(fireCloudService)
                  .getWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName);
              return fcWorkspace;
            })
        .when(fireCloudService)
        .createWorkspace(anyString(), anyString());
  }

  public void stubBufferBillingProject(BillingProjectBufferService billingProjectBufferService) {
    doAnswer(
            invocation -> {
              DbBillingProjectBufferEntry entry = mock(DbBillingProjectBufferEntry.class);
              doReturn(UUID.randomUUID().toString()).when(entry).getFireCloudProjectName();
              return entry;
            })
        .when(billingProjectBufferService)
        .assignBillingProject(any());
  }
}
