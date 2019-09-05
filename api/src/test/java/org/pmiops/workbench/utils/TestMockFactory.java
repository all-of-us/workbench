package org.pmiops.workbench.utils;

import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * Created by brubenst on 9/4/19.
 */
public class TestMockFactory {
  private static final String BUCKET_NAME = "workspace-bucket";

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
          org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
              new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
          fcResponse.setWorkspace(
              createFcWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null));
          fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
          doReturn(fcResponse)
              .when(fireCloudService)
              .getWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName);
          return null;
        })
        .when(fireCloudService)
        .createWorkspace(anyString(), anyString());
  }
}
