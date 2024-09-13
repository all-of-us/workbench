package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_APP_TYPE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.appTypeToLabelValue;
import static org.pmiops.workbench.utils.BillingUtils.fullBillingAccountName;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudbilling.model.BillingAccount;
import com.google.api.services.cloudbilling.model.ListBillingAccountsResponse;
import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudContext;
import org.broadinstitute.dsde.workbench.client.leonardo.model.CloudProvider;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.javers.common.collections.Lists;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.DemographicSurveyV2;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.EducationV2;
import org.pmiops.workbench.model.EthnicCategory;
import org.pmiops.workbench.model.GenderIdentityV2;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SexAtBirthV2;
import org.pmiops.workbench.model.SexualOrientationV2;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.YesNoPreferNot;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;

public class TestMockFactory {
  public static final String WORKSPACE_BUCKET_NAME = "fc-secure-111111-2222-AAAA-BBBB-000000000000";
  public static final String WORKSPACE_BILLING_ACCOUNT_NAME =
      fullBillingAccountName("00000-AAAAA-BBBBB");
  private static final String CDR_VERSION_ID = "1";
  public static final String DEFAULT_GOOGLE_PROJECT = "aou-rw-test-123";
  public static final String WORKSPACE_TERRA_UUID = "ws-uuid-123";

  /**
   * Populate the list of expected Access Modules with appropriate properties. See
   * http://broad.io/aou-access-modules-2021.
   *
   * <p>TWO_FACTOR_AUTH and ERA_COMMONS: not subject to AAR (expirable)
   *
   * <p>DATA_USER_CODE_OF_CONDUCT, RT_COMPLIANCE_TRAINING, PROFILE_CONFIRMATION and
   * PUBLICATION_CONFIRMATION: expirable
   *
   * <p>When considering new modules, the simplest option for implementation purposes is to make
   * them non-expirable. So we should default to this, unless required for product reasons.
   *
   * <p>RAS_LOGIN_GOV is non-expirable for this reason.
   */
  public static final List<DbAccessModule> DEFAULT_ACCESS_MODULES =
      List.of(
          new DbAccessModule().setName(DbAccessModuleName.TWO_FACTOR_AUTH).setExpirable(false),
          new DbAccessModule().setName(DbAccessModuleName.ERA_COMMONS).setExpirable(false),
          new DbAccessModule().setName(DbAccessModuleName.RAS_ID_ME).setExpirable(false),
          new DbAccessModule().setName(DbAccessModuleName.RAS_LOGIN_GOV).setExpirable(false),
          new DbAccessModule().setName(DbAccessModuleName.IDENTITY).setExpirable(false),
          new DbAccessModule()
              .setName(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT)
              .setExpirable(true),
          new DbAccessModule()
              .setName(DbAccessModuleName.RT_COMPLIANCE_TRAINING)
              .setExpirable(true),
          new DbAccessModule()
              .setName(DbAccessModuleName.CT_COMPLIANCE_TRAINING)
              .setExpirable(true),
          new DbAccessModule().setName(DbAccessModuleName.PROFILE_CONFIRMATION).setExpirable(true),
          new DbAccessModule()
              .setName(DbAccessModuleName.PUBLICATION_CONFIRMATION)
              .setExpirable(true));

  public static Workspace createWorkspace(
      String workspaceNamespace, String workspaceDisplayName, String workspaceTerraName) {
    List<DisseminateResearchEnum> disseminateResearchEnumsList = new ArrayList<>();
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES);
    disseminateResearchEnumsList.add(DisseminateResearchEnum.PRESENTATION_ADVISORY_GROUPS);

    List<ResearchOutcomeEnum> ResearchOutcomeEnumsList = new ArrayList<>();
    ResearchOutcomeEnumsList.add(ResearchOutcomeEnum.IMPROVED_RISK_ASSESMENT);

    User creator = new User();
    creator.setUserName("jay@unit-test-research-aou.org");
    creator.setGivenName("Jay");
    creator.setFamilyName("Tester");

    return new Workspace()
        .etag("\"1\"")
        .name(workspaceDisplayName)
        .displayName(workspaceDisplayName)
        .terraName(workspaceTerraName)
        .namespace(workspaceNamespace)
        .cdrVersionId(CDR_VERSION_ID)
        .usesTanagra(false)
        .googleBucketName(WORKSPACE_BUCKET_NAME)
        .billingAccountName(WORKSPACE_BILLING_ACCOUNT_NAME)
        .googleProject(DEFAULT_GOOGLE_PROJECT)
        .creationTime(1588097211621L)
        .creatorUser(creator)
        .creationTime(Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli())
        .lastModifiedTime(1588097211621L)
        .googleProject(DEFAULT_GOOGLE_PROJECT)
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

  public static RawlsWorkspaceDetails createTerraWorkspace(
      String namespace, String terraName, String creator) {
    return new RawlsWorkspaceDetails()
        .namespace(namespace)
        .name(terraName)
        .createdBy(creator)
        .workspaceId(WORKSPACE_TERRA_UUID)
        .bucketName(WORKSPACE_BUCKET_NAME)
        .googleProject(DEFAULT_GOOGLE_PROJECT);
  }

  public static LeonardoListRuntimeResponse createLeonardoListRuntimesResponse() {
    return new LeonardoListRuntimeResponse()
        .runtimeName("runtime")
        .cloudContext(
            new LeonardoCloudContext()
                .cloudProvider(LeonardoCloudProvider.GCP)
                .cloudResource("google-project"))
        .status(LeonardoRuntimeStatus.STOPPED);
  }

  public static void stubCreateFcWorkspace(FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              String capturedWorkspaceNamespace = (String) invocation.getArguments()[0];
              String capturedWorkspaceTerraName = (String) invocation.getArguments()[1];
              RawlsWorkspaceDetails fcWorkspace =
                  createTerraWorkspace(
                      capturedWorkspaceNamespace, capturedWorkspaceTerraName, null);

              RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
              fcResponse.setWorkspace(fcWorkspace);
              fcResponse.setAccessLevel(RawlsWorkspaceAccessLevel.OWNER);

              doReturn(fcResponse)
                  .when(fireCloudService)
                  .getWorkspace(capturedWorkspaceNamespace, capturedWorkspaceTerraName);
              return fcWorkspace;
            })
        .when(fireCloudService)
        .createWorkspace(anyString(), anyString(), anyString());
  }

  public static void stubCreateFcWorkspace(
      WorkspaceService workspaceService, FireCloudService fireCloudService) {
    doAnswer(
            invocation -> {
              Workspace capturedWorkspace = (Workspace) invocation.getArguments()[0];
              RawlsWorkspaceDetails fcWorkspace =
                  createTerraWorkspace(
                      capturedWorkspace.getNamespace(), capturedWorkspace.getName(), null);

              RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
              fcResponse.setWorkspace(fcWorkspace);
              fcResponse.setAccessLevel(RawlsWorkspaceAccessLevel.OWNER);

              doReturn(fcResponse)
                  .when(fireCloudService)
                  .getWorkspace(capturedWorkspace.getNamespace(), capturedWorkspace.getName());
              return fcWorkspace;
            })
        .when(workspaceService)
        .createWorkspace(any(Workspace.class), any(DbCdrVersion.class));
  }

  public static void stubCreateBillingProject(FireCloudService fireCloudService) {
    stubCreateBillingProject(fireCloudService, UUID.randomUUID().toString());
  }

  public static void stubCreateBillingProject(
      FireCloudService fireCloudService, String billingProjectId) {
    doReturn(billingProjectId).when(fireCloudService).createBillingProjectName();
  }

  public static void stubPollCloudBillingLinked(
      CloudBillingClient cloudBillingClient, String billingAccountName) {
    try {
      doReturn(new ProjectBillingInfo().setBillingEnabled(true).setName(billingAccountName))
          .when(cloudBillingClient)
          .pollUntilBillingAccountLinked(anyString(), anyString());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static Cloudbilling createMockedCloudbilling() {
    Cloudbilling cloudbilling = mock(Cloudbilling.class);
    Cloudbilling.Projects projects = mock(Cloudbilling.Projects.class);

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
    dbWorkspace.setName(workspace.getDisplayName());
    dbWorkspace.setWorkspaceNamespace(workspace.getNamespace());
    // a.k.a. RawlsWorkspaceDetails.name
    dbWorkspace.setFirecloudName(workspace.getTerraName()); // DB_WORKSPACE_FIRECLOUD_NAME
    ResearchPurpose researchPurpose = workspace.getResearchPurpose();
    dbWorkspace.setDiseaseFocusedResearch(researchPurpose.isDiseaseFocusedResearch());
    dbWorkspace.setDiseaseOfFocus(researchPurpose.getDiseaseOfFocus());
    dbWorkspace.setMethodsDevelopment(researchPurpose.isMethodsDevelopment());
    dbWorkspace.setControlSet(researchPurpose.isControlSet());
    dbWorkspace.setAncestry(researchPurpose.isAncestry());
    dbWorkspace.setCommercialPurpose(researchPurpose.isCommercialPurpose());
    dbWorkspace.setSocialBehavioral(researchPurpose.isSocialBehavioral());
    dbWorkspace.setPopulationHealth(researchPurpose.isPopulationHealth());
    dbWorkspace.setEducational(researchPurpose.isEducational());
    dbWorkspace.setDrugDevelopment(researchPurpose.isDrugDevelopment());

    dbWorkspace.setSpecificPopulationsEnum(new HashSet<>(researchPurpose.getPopulationDetails()));
    dbWorkspace.setAdditionalNotes(researchPurpose.getAdditionalNotes());
    dbWorkspace.setReasonForAllOfUs(researchPurpose.getReasonForAllOfUs());
    dbWorkspace.setIntendedStudy(researchPurpose.getIntendedStudy());
    dbWorkspace.setAnticipatedFindings(researchPurpose.getAnticipatedFindings());
    dbWorkspace.setGoogleProject(workspace.getGoogleProject());
    dbWorkspace.setFeaturedCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);
    return dbWorkspace;
  }

  public static DbAccessTier createRegisteredTier() {
    return new DbAccessTier()
        .setAccessTierId(1)
        .setShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
        .setDisplayName("Registered Tier")
        .setAuthDomainName("Registered Tier Auth Domain")
        .setAuthDomainGroupEmail("rt-users@fake-research-aou.org")
        .setServicePerimeter("registered/tier/perimeter")
        .setEnableUserWorkflows(false);
  }

  public static DbAccessTier createControlledTier() {
    return new DbAccessTier()
        .setAccessTierId(2)
        .setShortName("controlled")
        .setDisplayName("Controlled Tier")
        .setAuthDomainName("Controlled Tier Auth Domain")
        .setAuthDomainGroupEmail("ct-users@fake-research-aou.org")
        .setServicePerimeter("controlled/tier/perimeter")
        .setEnableUserWorkflows(true);
  }

  public static void removeControlledTierForTests(AccessTierDao accessTierDao) {
    DbAccessTier controlledAccessTier =
        accessTierDao.findOneByShortName(AccessTierService.CONTROLLED_TIER_SHORT_NAME).get();
    accessTierDao.delete(controlledAccessTier);
  }

  /** Prepare AccessModules inmemory cache. */
  public static List<DbAccessModule> createAccessModules(AccessModuleDao accessModuleDao) {
    accessModuleDao.saveAll(DEFAULT_ACCESS_MODULES);
    return accessModuleDao.findAll();
  }

  public static DbCdrVersion createDefaultCdrVersion(long id) {
    final DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setCdrVersionId(id);
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(createRegisteredTier());
    cdrVersion.setTanagraEnabled(Boolean.FALSE);
    return cdrVersion;
  }

  public static DbCdrVersion createDefaultCdrVersion() {
    return createDefaultCdrVersion(1);
  }

  public static DbCdrVersion createControlledTierCdrVersion(long id) {
    DbCdrVersion cdrVersion = createDefaultCdrVersion(id);
    DbAccessTier controlledTier = createControlledTier();
    cdrVersion.setAccessTier(controlledTier);
    return cdrVersion;
  }

  public static DbUserCodeOfConductAgreement createDuccAgreement(
      DbUser dbUser, int signedVersion, Timestamp completionTime) {
    DbUserCodeOfConductAgreement ducc = new DbUserCodeOfConductAgreement();
    ducc.setUser(dbUser);
    ducc.setSignedVersion(signedVersion);
    ducc.setUserFamilyName(dbUser.getFamilyName());
    ducc.setUserGivenName(dbUser.getGivenName());
    ducc.setUserInitials("XYZ");
    ducc.setCompletionTime(completionTime);
    return ducc;
  }

  public static DemographicSurveyV2 createDemoSurveyV2AllCategories() {
    return new DemographicSurveyV2()
        .ethnicCategories(Lists.asList(EthnicCategory.values()))
        .ethnicityAiAnOtherText("ethnicityAiAnOtherText")
        .ethnicityAsianOtherText("ethnicityAsianOtherText")
        .ethnicityBlackOtherText("ethnicityBlackOtherText")
        .ethnicityHispanicOtherText("ethnicityHispanicOtherText")
        .ethnicityMeNaOtherText("ethnicityMeNaOtherText")
        .ethnicityNhPiOtherText("ethnicityNhPiOtherText")
        .ethnicityWhiteOtherText("ethnicityWhiteOtherText")
        .ethnicityOtherText("ethnicityOtherText")
        .genderIdentities(Lists.asList(GenderIdentityV2.values()))
        .genderOtherText("genderOtherText")
        .sexualOrientations(Lists.asList(SexualOrientationV2.values()))
        .orientationOtherText("orientationOtherText")
        .sexAtBirth(SexAtBirthV2.OTHER)
        .sexAtBirthOtherText("sexAtBirthOtherText")
        .yearOfBirth(2000)
        .yearOfBirthPreferNot(false)
        .disabilityHearing(YesNoPreferNot.NO)
        .disabilitySeeing(YesNoPreferNot.YES)
        .disabilityConcentrating(YesNoPreferNot.PREFER_NOT_TO_ANSWER)
        .disabilityWalking(YesNoPreferNot.NO)
        .disabilityDressing(YesNoPreferNot.YES)
        .disabilityErrands(YesNoPreferNot.PREFER_NOT_TO_ANSWER)
        .disabilityOtherText("disabilityOtherText")
        .education(EducationV2.PREFER_NOT_TO_ANSWER)
        .disadvantaged(YesNoPreferNot.PREFER_NOT_TO_ANSWER);
  }

  public static void assertEqualDemographicSurveys(
      DemographicSurveyV2 survey1, DemographicSurveyV2 survey2) {
    assertThat(normalizeLists(survey1)).isEqualTo(normalizeLists(survey2));
  }

  public static ListPersistentDiskResponse createListPersistentDiskResponse(
      String pdName,
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus status,
      String date,
      String googleProjectId,
      DbUser user,
      @Nullable AppType appType) {
    ListPersistentDiskResponse response =
        new ListPersistentDiskResponse()
            .name(pdName)
            .size(300)
            .diskType(org.broadinstitute.dsde.workbench.client.leonardo.model.DiskType.STANDARD)
            .status(status)
            .auditInfo(new AuditInfo().createdDate(date).creator(user.getUsername()))
            .cloudContext(
                new CloudContext().cloudProvider(CloudProvider.GCP).cloudResource(googleProjectId));
    if (appType != null) {
      Map<String, String> label = new HashMap<>();
      label.put(LEONARDO_LABEL_APP_TYPE, appTypeToLabelValue(appType));
      response.labels(label);
    }
    return response;
  }

  public static ListPersistentDiskResponse createLeonardoRuntimePDResponse(
      String pdName,
      org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus status,
      String date,
      String googleProjectId,
      DbUser user) {
    return createListPersistentDiskResponse(
        pdName, status, date, googleProjectId, user, /*appType*/ null);
  }

  private static Disk createDisk(
      String pdName, DiskStatus status, String date, String googleProject, DbUser user) {
    return new Disk()
        .name(pdName)
        .size(300)
        .diskType(DiskType.STANDARD)
        .status(status)
        .createdDate(date)
        .googleProject(googleProject)
        .creator(user.getUsername());
  }

  public static Disk createAppDisk(
      String pdName,
      DiskStatus status,
      String date,
      String googleProject,
      DbUser user,
      AppType appType) {
    return createDisk(pdName, status, date, googleProject, user).appType(appType);
  }

  public static Disk createRuntimeDisk(
      String pdName, DiskStatus status, String date, String googleProject, DbUser user) {
    return createDisk(pdName, status, date, googleProject, user).gceRuntime(true);
  }

  // we make no guarantees about the order of the lists in DemographicSurveyV2
  // so let's normalize them for comparison
  private static DemographicSurveyV2 normalizeLists(DemographicSurveyV2 rawSurvey) {
    return rawSurvey
        .ethnicCategories(rawSurvey.getEthnicCategories().stream().sorted().toList())
        .genderIdentities(rawSurvey.getGenderIdentities().stream().sorted().toList())
        .sexualOrientations(rawSurvey.getSexualOrientations().stream().sorted().toList());
  }
}
