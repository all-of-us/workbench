package org.pmiops.workbench.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.Cloudbilling.BillingAccounts;
import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;

public class TestMockFactory {
  public static final String WORKSPACE_BUCKET_NAME = "fc-secure-111111-2222-AAAA-BBBB-000000000000";
  private static final String CDR_VERSION_ID = "1";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";
  public static final String WORKSPACE_NAMESPACE = "aou-rw-local1-aaaa0000";
  public static final String WORKSPACE_CREATOR_USERNAME = "jay@unit-test-research-aou.org";

  /**
   * Create an initial workspace (like the kind that comes from the UI for createWorkspace.
   *
   * Many properties are not provided by the UI by filled in by WorkspacesController.createWorkspace():
   * @param workspaceName User-created Workspace name.
   * @return
   */
  public Workspace buildWorkspaceModelForCreate(String workspaceName) {
    return new Workspace()
        .name(workspaceName)
        .dataAccessLevel(DataAccessLevel.PROTECTED)
        .cdrVersionId(CDR_VERSION_ID)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .creator(WORKSPACE_CREATOR_USERNAME)
        .researchPurpose(
            new ResearchPurpose()
                .additionalNotes("additional notes")
                .additionalNotes(null)
                .ancestry(true)
                .anticipatedFindings("anticipated findings")
                .approved(false)
                .commercialPurpose(true)
                .controlSet(true)
                .diseaseFocusedResearch(true)
                .diseaseOfFocus("cancer")
                .disseminateResearchFindingList(ImmutableList.of(
                    DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES,
                    DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS))
                .drugDevelopment(true)
                .educational(true)
                .intendedStudy("intended study")
                .methodsDevelopment(true)
                .populationDetails(Collections.emptyList())
                .populationHealth(true)
                .reasonForAllOfUs("reason for aou")
                .researchOutcomeList(ImmutableList.of(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT))
                .reviewRequested(true)
                .socialBehavioral(true));
  }

  public FirecloudWorkspace createFirecloudWorkspace(String namespace, String firecloudWorkspaceName, String creatorUsername) {
    return new FirecloudWorkspace()
      .namespace(namespace)
      .workspaceId(namespace)
      .name(firecloudWorkspaceName)
      .createdBy(creatorUsername)
      .bucketName(WORKSPACE_BUCKET_NAME);
  }

  public ListClusterResponse createFirecloudListClusterResponse() {
    ListClusterResponse listClusterResponse =
        new ListClusterResponse()
            .clusterName("cluster")
            .googleProject("google-project")
            .status(ClusterStatus.STOPPED);
    return listClusterResponse;
  }

  public void stubCreateFcWorkspace(FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              String capturedWorkspaceName = (String) invocation.getArguments()[1];
              String capturedWorkspaceNamespace = (String) invocation.getArguments()[0];
              FirecloudWorkspace fcWorkspace =
                  createFirecloudWorkspace(capturedWorkspaceNamespace, capturedWorkspaceName, null);

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
              //noinspection ResultOfMethodCallIgnored
              doReturn(WORKSPACE_NAMESPACE).when(entry).getFireCloudProjectName();
              return entry;
            })
        .when(billingProjectBufferService)
        .assignBillingProject(any());
  }

  public static Cloudbilling createMockedCloudbilling() {
    Cloudbilling mockCloudbilling = mock(Cloudbilling.class);
    Cloudbilling.Projects projects = mock(Cloudbilling.Projects.class);

    try {
      doAnswer(
              invocation -> {
                ProjectBillingInfo projectBillingInfo = invocation.getArgument(1);

                Cloudbilling.Projects.UpdateBillingInfo updateBillingInfo =
                    mock(Cloudbilling.Projects.UpdateBillingInfo.class);
                doReturn(projectBillingInfo).when(updateBillingInfo).execute();

                return updateBillingInfo;
              })
          .when(projects)
          .updateBillingInfo(anyString(), any(ProjectBillingInfo.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    doReturn(projects).when(mockCloudbilling).projects();

    BillingAccounts billingAccounts = mock(BillingAccounts.class);
    BillingAccounts.Get getRequest = mock(BillingAccounts.Get.class);
    BillingAccounts.List listRequest = mock(BillingAccounts.List.class);

    try {
      doReturn(new BillingAccount().setOpen(true)).when(getRequest).execute();
      doReturn(getRequest).when(billingAccounts).get(anyString());

      doReturn(new ListBillingAccountsResponse()).when(listRequest).execute();
      doReturn(listRequest).when(billingAccounts).list();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    doReturn(billingAccounts).when(mockCloudbilling).billingAccounts();
    return mockCloudbilling;
  }
}
