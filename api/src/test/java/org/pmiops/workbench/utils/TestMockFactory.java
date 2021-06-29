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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public class TestMockFactory {
  public static final String WORKSPACE_BUCKET_NAME = "fc-secure-111111-2222-AAAA-BBBB-000000000000";
  private static final String CDR_VERSION_ID = "1";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME = "billingAccounts/00000-AAAAA-BBBBB";
  private static final String WORKSPACE_FIRECLOUD_NAME =
      "gonewiththewind"; // should match workspace name w/o spaces
  public static final String DEFAULT_GOOGLE_PROJECT = "aou-rw-test-123";

  // TODO there's something off about how "workspaceName" here works.  Investigate.
  // For best results, use a lowercase-only workspaceName.
  // To me, this hints at a firecloudName/aouName discrepancy somewhere in here.
  public Workspace createWorkspace(String workspaceNameSpace, String workspaceName) {
    List<DisseminateResearchEnum> disseminateResearchEnumsList = new ArrayList<>();
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES);
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS);

    List<ResearchOutcomeEnum> ResearchOutcomeEnumsList = new ArrayList<>();
    ResearchOutcomeEnumsList.add(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT);

    return new Workspace()
        .id(WORKSPACE_FIRECLOUD_NAME)
        .etag("\"1\"")
        .name(workspaceName)
        .namespace(workspaceNameSpace)
        .cdrVersionId(CDR_VERSION_ID)
        .googleBucketName(WORKSPACE_BUCKET_NAME)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .billingAccountType(BillingAccountType.FREE_TIER)
        .googleProject(DEFAULT_GOOGLE_PROJECT)
        .creationTime(1588097211621L)
        .creator("jay@unit-test-research-aou.org")
        .creationTime(Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli())
        .lastModifiedTime(1588097211621L)
        .googleProject(DEFAULT_GOOGLE_PROJECT)
        .published(false)
        .researchPurpose(
            new ResearchPurpose()
                .additionalNotes(null)
                .diseaseFocusedResearch(true)
                .diseaseOfFocus("cancer")
                .methodsDevelopment(true)
                .controlSet(true)
                .ancestry(true)
                .commercialPurpose(true)
                .socialBehavioral(true)
                .populationHealth(true)
                .educational(true)
                .drugDevelopment(true)
                .populationDetails(Collections.emptyList())
                .additionalNotes("additional notes")
                .reasonForAllOfUs("reason for aou")
                .intendedStudy("intended study")
                .anticipatedFindings("anticipated findings")
                .timeRequested(1000L)
                .timeReviewed(1500L)
                .reviewRequested(true)
                .disseminateResearchFindingList(disseminateResearchEnumsList)
                .researchOutcomeList(ResearchOutcomeEnumsList)
                .approved(false));
  }

  public static FirecloudWorkspace createFirecloudWorkspace(
      String ns, String name, String creator) {
    return new FirecloudWorkspace()
        .namespace(ns)
        .workspaceId(ns)
        .name(name)
        .createdBy(creator)
        .bucketName(WORKSPACE_BUCKET_NAME)
        .googleProject(DEFAULT_GOOGLE_PROJECT);
  }

  public LeonardoListRuntimeResponse createLeonardoListRuntimesResponse() {
    return new LeonardoListRuntimeResponse()
        .runtimeName("runtime")
        .googleProject("google-project")
        .status(LeonardoRuntimeStatus.STOPPED);
  }

  public static void stubCreateFcWorkspace(FireCloudService fireCloudService) {
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
        .createWorkspace(anyString(), anyString(), anyString());
  }

  public void stubBufferBillingProject(BillingProjectBufferService billingProjectBufferService) {
    stubBufferBillingProject(billingProjectBufferService, UUID.randomUUID().toString());
  }

  public void stubBufferBillingProject(BillingProjectBufferService billingProjectBufferService, String billingProjectId) {
    doAnswer(
        invocation -> {
          DbBillingProjectBufferEntry entry = mock(DbBillingProjectBufferEntry.class);
          doReturn(billingProjectId).when(entry).getFireCloudProjectName();
          return entry;
        })
        .when(billingProjectBufferService)
        .assignBillingProject(any(), any());
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

  // TODO(jaycarlton) use  WorkspaceMapper.toDbWorkspace() once it's available RW 4803
  public static DbWorkspace createDbWorkspaceStub(Workspace workspace, long workspaceDbId) {
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(workspaceDbId);
    dbWorkspace.setName(workspace.getName());
    dbWorkspace.setWorkspaceNamespace(workspace.getNamespace());
    // a.k.a. FirecloudWorkspace.name
    dbWorkspace.setFirecloudName(workspace.getId()); // DB_WORKSPACE_FIRECLOUD_NAME
    ResearchPurpose researchPurpose = workspace.getResearchPurpose();
    dbWorkspace.setDiseaseFocusedResearch(researchPurpose.getDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(researchPurpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(researchPurpose.getMethodsDevelopment());
    dbWorkspace.setControlSet(researchPurpose.getControlSet());
    dbWorkspace.setAncestry(researchPurpose.getAncestry());
    dbWorkspace.setCommercialPurpose(researchPurpose.getCommercialPurpose());
    dbWorkspace.setSocialBehavioral(researchPurpose.getSocialBehavioral());
    dbWorkspace.setPopulationHealth(researchPurpose.getPopulationHealth());
    dbWorkspace.setEducational(researchPurpose.getEducational());
    dbWorkspace.setDrugDevelopment(researchPurpose.getDrugDevelopment());

    dbWorkspace.setSpecificPopulationsEnum(new HashSet<>(researchPurpose.getPopulationDetails()));
    dbWorkspace.setAdditionalNotes(researchPurpose.getAdditionalNotes());
    dbWorkspace.setReasonForAllOfUs(researchPurpose.getReasonForAllOfUs());
    dbWorkspace.setIntendedStudy(researchPurpose.getIntendedStudy());
    dbWorkspace.setAnticipatedFindings(researchPurpose.getAnticipatedFindings());
    dbWorkspace.setGoogleProject(workspace.getGoogleProject());
    return dbWorkspace;
  }

  public static DbAccessTier createRegisteredTierForTests(AccessTierDao accessTierDao) {
    final DbAccessTier accessTier =
        new DbAccessTier()
            .setAccessTierId(1)
            .setShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .setDisplayName("Registered Tier")
            .setAuthDomainName("Registered Tier Auth Domain")
            .setAuthDomainGroupEmail("rt-users@fake-research-aou.org")
            .setServicePerimeter("registered/tier/perimeter");
    return accessTierDao.save(accessTier);
  }

  public static DbAccessTier createControlledTierForTests(AccessTierDao accessTierDao) {
    return accessTierDao.save(
        new DbAccessTier()
            .setAccessTierId(2)
            .setShortName("controlled")
            .setDisplayName("Controlled Tier")
            .setAuthDomainName("Controlled Tier Auth Domain")
            .setAuthDomainGroupEmail("ct-users@fake-research-aou.org")
            .setServicePerimeter("controlled/tier/perimeter"));
  }

  public static DbCdrVersion createDefaultCdrVersion(
      CdrVersionDao cdrVersionDao, AccessTierDao accessTierDao) {
    return createDefaultCdrVersion(cdrVersionDao, accessTierDao, 1);
  }

  public static DbCdrVersion createDefaultCdrVersion(
      CdrVersionDao cdrVersionDao, AccessTierDao accessTierDao, long id) {
    final DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(id);
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(createRegisteredTierForTests(accessTierDao));
    return cdrVersionDao.save(cdrVersion);
  }
}
