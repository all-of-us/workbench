package org.pmiops.workbench.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
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
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";
  private static final String FIRECLOUD_PROJECT_NAME = "aou-rw-env-01234567";

  /**
   * Create an initial workspace (like the kind that comes from the UI for createWorkspace.
   *
   * The follwoing properties are not provided by the UI by filled in by WorkspacesController.createWorkspace():
   * additionalNotes
   * approved
   * billingAccountType
   * billingStatus
   * cdrVersionId
   * creationTime
   * creator
   * etag
   * googleBucketName
   * id
   * lastModifiedTime
   * namespace
   * published
   * timeRequested
   * timeReviewed
   * @param workspaceName User-created Workspace name.
   * @return
   */
  public Workspace buildWorkspaceModelForCreate(String workspaceName) {
    List<DisseminateResearchEnum> disseminateResearchEnumsList = new ArrayList<>();
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES);
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS);

    List<ResearchOutcomeEnum> researchOutcomes = new ArrayList<>();
    researchOutcomes.add(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT);

    return new Workspace()
        .name(workspaceName)
        .dataAccessLevel(DataAccessLevel.PROTECTED)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
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
                .disseminateResearchFindingList(disseminateResearchEnumsList)
                .drugDevelopment(true)
                .educational(true)
                .intendedStudy("intended study")
                .methodsDevelopment(true)
                .populationDetails(Collections.emptyList())
                .populationHealth(true)
                .reasonForAllOfUs("reason for aou")
                .researchOutcomeList(researchOutcomes)
                .reviewRequested(true)
                .socialBehavioral(true));
  }

  public FirecloudWorkspace createFirecloudWorkspace(String ns, String name, String creator) {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setWorkspaceId(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    fcWorkspace.setBucketName(WORKSPACE_BUCKET_NAME);
    return fcWorkspace;
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

  public void stubBufferBillingProject(BillingProjectBufferService mockBillingProjectBufferService) {
    doAnswer(
            invocation -> {
              DbBillingProjectBufferEntry entry = mock(DbBillingProjectBufferEntry.class);
              doReturn(FIRECLOUD_PROJECT_NAME).when(entry).getFireCloudProjectName();
              return entry;
            })
        .when(mockBillingProjectBufferService)
        .assignBillingProject(any());
  }

  public static Cloudbilling createMockedCloudbilling() {
    Cloudbilling cloudbilling = mock(Cloudbilling.class);
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
    doReturn(projects).when(cloudbilling).projects();

    Cloudbilling.BillingAccounts billingAccounts = mock(Cloudbilling.BillingAccounts.class);
    Cloudbilling.BillingAccounts.Get getRequest = mock(Cloudbilling.BillingAccounts.Get.class);
    Cloudbilling.BillingAccounts.List listRequest = mock(Cloudbilling.BillingAccounts.List.class);

    try {
      doReturn(new BillingAccount().setOpen(true)).when(getRequest).execute();
      doReturn(getRequest).when(billingAccounts).get(anyString());

      doReturn(new ListBillingAccountsResponse()).when(listRequest).execute();
      doReturn(listRequest).when(billingAccounts).list();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    doReturn(billingAccounts).when(cloudbilling).billingAccounts();
    return cloudbilling;
  }
}
