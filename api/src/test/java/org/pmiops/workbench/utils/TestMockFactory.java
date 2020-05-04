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
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.CreateWorkspaceRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.springframework.stereotype.Service;

@Service
public class TestMockFactory {
  public static final String WORKSPACE_BUCKET_NAME = "fc-secure-111111-2222-AAAA-BBBB-000000000000";
  private static final String CDR_VERSION_ID = "1";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";
  private static final String WORKSPACE_FIRECLOUD_NAME =
      "gonewiththewind"; // should match workspace name w/o spaces
  private static final Instant WORKSPACE_CREATION_INSTANT = Instant.parse("2020-02-10T09:30:00.00Z");
  private static final Instant WORKSPACE_MODIFIED_INSTANT = Instant.parse("2020-02-10T09:30:00.00Z");
  private static final Instant REVIEW_REQUESTED_INSTANT = Instant.parse("2020-05-01T09:30:00.00Z");
  private static final Instant REVIEWED_INSTANT = Instant.parse("2020-05-03T09:30:00.00Z");
  public static final ResearchPurpose RESEARCH_PURPOSE = new ResearchPurpose()
      .additionalNotes("additional notes")
      .additionalNotes(null)
      .ancestry(true)
      .anticipatedFindings("anticipated findings")
      .approved(false)
      .commercialPurpose(true)
      .controlSet(true)
      .diseaseFocusedResearch(true)
      .diseaseOfFocus("cancer")
      .disseminateResearchFindingList(ImmutableList.of(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES, DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS))
      .drugDevelopment(true)
      .educational(true)
      .intendedStudy("intended study")
      .methodsDevelopment(true)
      .populationDetails(Collections.emptyList())
      .populationHealth(true)
      .reasonForAllOfUs("reason for aou")
      .researchOutcomeList(ImmutableList.of(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT))
      .reviewRequested(true)
      .socialBehavioral(true)
      .timeRequested(REVIEW_REQUESTED_INSTANT.toEpochMilli())
      .timeReviewed(REVIEWED_INSTANT.toEpochMilli());
  public static final String WORKSPACE_CREATOR_USERNAME = "jay@unit-test-research-aou.org";
  public static final String WORKSPACE_ETAG = "\"2\"";
  public static final boolean WORKSPACE_PUBLISHED = false;
  public static final BillingAccountType BILLING_ACCOUNT_TYPE = BillingAccountType.FREE_TIER;
  public static final BillingStatus BILLING_STATUS = BillingStatus.ACTIVE;
  public static final DataAccessLevel DATA_ACCESS_LEVEL = DataAccessLevel.PROTECTED;
  private final WorkspaceMapper workspaceMapper;

  public TestMockFactory(WorkspaceMapper workspaceMapper) {
    this.workspaceMapper = workspaceMapper;
  }

  /**
   * Builds a fully-populated Workspace object, such as you get from calling CreateWorkspace
   */
  public Workspace makeCreatedWorkspace(String workspaceNamespace, String workspaceName) {
    final CreateWorkspaceRequest createWorkspaceRequest = makeCreateWorkspaceRequest(workspaceName);
    return workspaceMapper.toWorkspaceTestFixture(
        createWorkspaceRequest,
        BILLING_ACCOUNT_TYPE,
        BILLING_STATUS,
        WORKSPACE_CREATION_INSTANT.toEpochMilli(),
        WORKSPACE_ETAG,
        WORKSPACE_BUCKET_NAME,
        WORKSPACE_FIRECLOUD_NAME,
        WORKSPACE_MODIFIED_INSTANT.toEpochMilli(),
        workspaceNamespace,
        WORKSPACE_PUBLISHED);
  }

  public CreateWorkspaceRequest makeCreateWorkspaceRequest(String workspaceName) {
    return new CreateWorkspaceRequest()
        .name(workspaceName)
        .dataAccessLevel(DATA_ACCESS_LEVEL)
        .cdrVersionId(CDR_VERSION_ID)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .creator(WORKSPACE_CREATOR_USERNAME)
        .researchPurpose(RESEARCH_PURPOSE);
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
