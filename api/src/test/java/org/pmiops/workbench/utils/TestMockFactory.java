package org.pmiops.workbench.utils;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.io.IOException;
import java.util.UUID;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public class TestMockFactory {
  public static final String BUCKET_NAME = "workspace-bucket";

  public FirecloudWorkspace createFcWorkspace(String ns, String name, String creator) {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
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
              FirecloudWorkspace fcWorkspace =
                  createFcWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null);

              FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
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

  public static Cloudbilling createMockedCloudbilling() {
    Cloudbilling cloudbilling = mock(Cloudbilling.class);
    Cloudbilling.Projects projects = mock(Cloudbilling.Projects.class);

    try {
      doAnswer(invocation -> {
        ProjectBillingInfo projectBillingInfo = invocation.getArgument(1);

        Cloudbilling.Projects.UpdateBillingInfo updateBillingInfo = mock(Cloudbilling.Projects.UpdateBillingInfo.class);
        doReturn(projectBillingInfo).when(updateBillingInfo).execute();

        return updateBillingInfo;
      }).when(projects).updateBillingInfo(anyString(), any(ProjectBillingInfo.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    doReturn(projects).when(cloudbilling).projects();
    return cloudbilling;
  }
}
