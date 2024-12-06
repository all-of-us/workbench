package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.pmiops.workbench.config.WorkbenchConfig.createEmptyConfig;
import static org.pmiops.workbench.utils.BillingUtils.fullBillingAccountName;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.initialcredits.WorkspaceInitialCreditUsageService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class WorkspaceMapperTest {
  private static final String FIRECLOUD_NAMESPACE = "aou-xxxxxxx";
  private static final String CREATOR_EMAIL = "ojc@verily.biz";
  private static final long CREATOR_USER_ID = 101L;
  private static final long WORKSPACE_DB_ID = 222L;
  private static final int WORKSPACE_VERSION = 2;
  private static final String WORKSPACE_AOU_NAME = "studyallthethings";
  private static final String WORKSPACE_FIRECLOUD_NAME = "aaaa-bbbb-cccc-dddd";
  private static final String BILLING_ACCOUNT_NAME = "billing-account";
  private static final String INITIAL_CREDITS_BILLING_ACCOUNT_NAME =
      "initial-credits-billing-account";
  private static final String GOOGLE_PROJECT = "google_project";

  private static final Timestamp DB_CREATION_TIMESTAMP =
      Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  private static final Timestamp INITIAL_CREDITS_EXPIRATION_TIMESTAMP =
      Timestamp.from(Instant.parse("2020-01-01T00:00:00.00Z"));
  private static final int CDR_VERSION_ID = 2;
  private static final String FIRECLOUD_BUCKET_NAME = "my-favorite-bucket";
  private static final Set<SpecificPopulationEnum> SPECIFIC_POPULATIONS =
      Set.of(SpecificPopulationEnum.DISABILITY_STATUS, SpecificPopulationEnum.GEOGRAPHY);
  private static final Set<ResearchOutcomeEnum> RESEARCH_OUTCOMES =
      Set.of(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN);
  private static final String DISSEMINATE_FINDINGS_OTHER = "Everywhere except MIT.";
  private static final String ACCESS_TIER_SHORT_NAME = "registered";

  private DbWorkspace sourceDbWorkspace;
  private RawlsWorkspaceDetails sourceFirecloudWorkspace;

  private static WorkbenchConfig workbenchConfig;

  @Autowired private InitialCreditsService initialCreditsService;
  @Autowired private WorkspaceMapper workspaceMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    InitialCreditsService.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    UserDao.class,
    WorkspaceDao.class,
    ConceptSetService.class,
    CohortService.class,
    MailService.class,
    LeonardoApiClient.class,
    InstitutionService.class,
    TaskQueueService.class,
    UserServiceAuditor.class,
    WorkspaceFreeTierUsageDao.class,
    WorkspaceInitialCreditUsageService.class
  })
  static class Configuration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    sourceFirecloudWorkspace =
        new RawlsWorkspaceDetails()
            .workspaceId(Long.toString(CREATOR_USER_ID))
            .bucketName(FIRECLOUD_BUCKET_NAME)
            .createdBy(CREATOR_EMAIL)
            .name(WORKSPACE_FIRECLOUD_NAME)
            .googleProject(GOOGLE_PROJECT);

    final DbUser creatorUser =
        new DbUser()
            .setUsername(CREATOR_EMAIL)
            .setUserId(CREATOR_USER_ID)
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration()
                    .setBypassed(false)
                    .setExpirationTime(INITIAL_CREDITS_EXPIRATION_TIMESTAMP));

    final DbAccessTier accessTier = new DbAccessTier().setShortName(ACCESS_TIER_SHORT_NAME);

    final DbCdrVersion cdrVersion =
        new DbCdrVersion().setCdrVersionId(CDR_VERSION_ID).setAccessTier(accessTier);

    sourceDbWorkspace =
        new DbWorkspace()
            .setWorkspaceId(WORKSPACE_DB_ID)
            .setVersion(WORKSPACE_VERSION)
            .setName(WORKSPACE_AOU_NAME)
            .setFirecloudName(WORKSPACE_FIRECLOUD_NAME)
            .setWorkspaceNamespace(FIRECLOUD_NAMESPACE)
            .setCdrVersion(cdrVersion)
            .setCreator(creatorUser)
            .setCreationTime(DB_CREATION_TIMESTAMP)
            .setLastModifiedTime(DB_CREATION_TIMESTAMP)
            .setCohorts(Collections.emptySet())
            .setConceptSets(Collections.emptySet())
            .setDataSets(Collections.emptySet())
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE)
            .setDiseaseFocusedResearch(true)
            .setDiseaseOfFocus("leukemia")
            .setMethodsDevelopment(false)
            .setControlSet(true)
            .setAncestry(false)
            .setCommercialPurpose(false)
            .setSpecificPopulationsEnum(
                Set.of(SpecificPopulationEnum.AGE_GROUPS, SpecificPopulationEnum.INCOME_LEVEL))
            .setSocialBehavioral(false)
            .setPopulationHealth(true)
            .setEducational(true)
            .setDrugDevelopment(false)
            .setOtherPurpose(true)
            .setOtherPurposeDetails("I want to discover a new disease.")
            .setOtherPopulationDetails(null)
            .setAdditionalNotes(null)
            .setReasonForAllOfUs("We can't get this data anywhere else.")
            .setIntendedStudy(null)
            .setReviewRequested(false)
            .setApproved(true)
            .setTimeRequested(DB_CREATION_TIMESTAMP)
            .setBillingAccountName(BILLING_ACCOUNT_NAME)
            .setSpecificPopulationsEnum(SPECIFIC_POPULATIONS)
            .setResearchOutcomeEnumSet(RESEARCH_OUTCOMES)
            .setDisseminateResearchEnumSet(Collections.emptySet())
            .setDisseminateResearchOther(DISSEMINATE_FINDINGS_OTHER)
            .setGoogleProject(GOOGLE_PROJECT);

    workbenchConfig = createEmptyConfig();
  }

  @Test
  public void testConvertsDbToApiWorkspace() {

    final Workspace ws =
        workspaceMapper.toApiWorkspace(
            sourceDbWorkspace, sourceFirecloudWorkspace, initialCreditsService);
    assertThat(ws.getTerraName()).isEqualTo(WORKSPACE_FIRECLOUD_NAME);
    assertThat(ws.getEtag()).isEqualTo(Etags.fromVersion(WORKSPACE_VERSION));
    assertThat(ws.getName()).isEqualTo(WORKSPACE_AOU_NAME);
    assertThat(ws.getNamespace()).isEqualTo(FIRECLOUD_NAMESPACE);
    assertThat(ws.getCdrVersionId()).isEqualTo(Long.toString(CDR_VERSION_ID));
    assertThat(ws.getCreator().getUserName()).isEqualTo(CREATOR_EMAIL);
    assertThat(ws.getInitialCredits().getExpirationEpochMillis())
        .isEqualTo(INITIAL_CREDITS_EXPIRATION_TIMESTAMP.getTime());
    assertThat(ws.getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
    assertThat(ws.getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);
    assertThat(ws.getAccessTierShortName()).isEqualTo(ACCESS_TIER_SHORT_NAME);
    assertThat(ws.getGoogleProject()).isEqualTo(GOOGLE_PROJECT);

    final ResearchPurpose rp = ws.getResearchPurpose();
    assertResearchPurposeMatches(rp);

    assertThat(ws.getCreationTime()).isEqualTo(DB_CREATION_TIMESTAMP.toInstant().toEpochMilli());
    assertThat(ws.getFeaturedCategory()).isNull();
  }

  @Test
  public void testConvertsFeaturedWorkspace() {
    sourceDbWorkspace.setFeaturedCategory(DbFeaturedWorkspace.DbFeaturedCategory.COMMUNITY);
    final Workspace ws =
        workspaceMapper.toApiWorkspace(
            sourceDbWorkspace, sourceFirecloudWorkspace, initialCreditsService);
    assertThat(ws.getFeaturedCategory()).isEqualTo(FeaturedWorkspaceCategory.COMMUNITY);
  }

  @Test
  public void testConvertsFirecloudResponseToApiResponse_2_param_version() {
    final WorkspaceResponse resp =
        workspaceMapper.toApiWorkspaceResponse(
            workspaceMapper.toApiWorkspace(
                sourceDbWorkspace, sourceFirecloudWorkspace, initialCreditsService),
            RawlsWorkspaceAccessLevel.PROJECT_OWNER);

    assertThat(resp.getAccessLevel()).isEqualTo(WorkspaceAccessLevel.OWNER);

    // Verify data came from the DB workspace.
    assertThat(resp.getWorkspace().getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);

    // Verify data came from the Firecloud workspace.
    assertThat(resp.getWorkspace().getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
  }

  @Test
  public void testConvertsFirecloudResponseToApiResponse_3_param_version() {
    RawlsWorkspaceListResponse rawlsResponse =
        new RawlsWorkspaceListResponse()
            .workspace(sourceFirecloudWorkspace)
            .accessLevel(RawlsWorkspaceAccessLevel.PROJECT_OWNER);
    final WorkspaceResponse resp =
        workspaceMapper.toApiWorkspaceResponse(
            sourceDbWorkspace, rawlsResponse, initialCreditsService);

    assertThat(resp.getAccessLevel()).isEqualTo(WorkspaceAccessLevel.OWNER);

    // Verify data came from the DB workspace.
    assertThat(resp.getWorkspace().getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);

    // Verify data came from the Firecloud workspace.
    assertThat(resp.getWorkspace().getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
  }

  @Test
  public void testConvertsFirecloudResponsesToApiResponseList_listVersion() {
    RawlsWorkspaceListResponse rawlsResponse =
        new RawlsWorkspaceListResponse()
            .workspace(sourceFirecloudWorkspace)
            .accessLevel(RawlsWorkspaceAccessLevel.PROJECT_OWNER);

    String fcUuid = sourceFirecloudWorkspace.getWorkspaceId();

    final List<WorkspaceResponse> result =
        workspaceMapper.toApiWorkspaceResponseList(
            List.of(sourceDbWorkspace.setFirecloudUuid(fcUuid)),
            Map.of(fcUuid, rawlsResponse),
            initialCreditsService);

    assertThat(result).hasSize(1);
    final WorkspaceResponse wsResp = result.get(0);

    // Verify data came from the DB workspace.
    assertThat(wsResp.getWorkspace().getBillingAccountName()).isEqualTo(BILLING_ACCOUNT_NAME);

    // Verify data came from the Firecloud workspace.
    assertThat(wsResp.getWorkspace().getGoogleBucketName()).isEqualTo(FIRECLOUD_BUCKET_NAME);
  }

  @Test
  public void testConvertsFirecloudResponsesToApiResponseList_listVersion_missing_aou() {
    RawlsWorkspaceListResponse rawlsResponse =
        new RawlsWorkspaceListResponse()
            .workspace(sourceFirecloudWorkspace)
            .accessLevel(RawlsWorkspaceAccessLevel.PROJECT_OWNER);

    String fcUuid = sourceFirecloudWorkspace.getWorkspaceId();

    final List<WorkspaceResponse> result =
        assertDoesNotThrow(
            () ->
                workspaceMapper.toApiWorkspaceResponseList(
                    Collections.emptyList(), Map.of(fcUuid, rawlsResponse), initialCreditsService));

    assertThat(result).isEmpty();
  }

  @Test
  public void testConvertsFirecloudResponsesToApiResponseList_listVersion_missing_rawls() {

    String fcUuid = sourceFirecloudWorkspace.getWorkspaceId();

    final List<WorkspaceResponse> result =
        assertDoesNotThrow(
            () ->
                workspaceMapper.toApiWorkspaceResponseList(
                    List.of(sourceDbWorkspace.setFirecloudUuid(fcUuid)),
                    Collections.emptyMap(),
                    initialCreditsService));

    assertThat(result).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dataForWorkspaceBilling")
  public void testToApiWorkspaceBillingStatus(
      String testName,
      String billingAccount,
      boolean exhausted,
      boolean expired,
      BillingStatus expectedStatus) {
    workbenchConfig.billing.accountId = INITIAL_CREDITS_BILLING_ACCOUNT_NAME;
    sourceDbWorkspace.setInitialCreditsExhausted(exhausted);
    sourceDbWorkspace.setInitialCreditsExpired(expired);
    sourceDbWorkspace.setBillingAccountName(fullBillingAccountName(billingAccount));
    Workspace x =
        workspaceMapper.toApiWorkspace(
            sourceDbWorkspace, sourceFirecloudWorkspace, initialCreditsService);
    assertThat(x.getBillingStatus()).isEqualTo(expectedStatus);
  }

  static Stream<Arguments> dataForWorkspaceBilling() {
    return Stream.of(
        Arguments.of(
            "If the workspace is using initial credits and the credits are neither exhausted or expired, the billing status is active.",
            INITIAL_CREDITS_BILLING_ACCOUNT_NAME,
            false,
            false,
            BillingStatus.ACTIVE),
        Arguments.of(
            "If the workspace is using initial credits and the credits are exhausted, the billing status is inactive.",
            INITIAL_CREDITS_BILLING_ACCOUNT_NAME,
            true,
            false,
            BillingStatus.INACTIVE),
        Arguments.of(
            "If the workspace is using initial credits and the credits are expired, the billing status is inactive.",
            INITIAL_CREDITS_BILLING_ACCOUNT_NAME,
            false,
            true,
            BillingStatus.INACTIVE),
        Arguments.of(
            "If the workspace is using initial credits and the credits are both exhausted and expired, the billing status is inactive.",
            INITIAL_CREDITS_BILLING_ACCOUNT_NAME,
            true,
            true,
            BillingStatus.INACTIVE),
        Arguments.of(
            "If the workspace is not using initial credits and the credits are neither exhausted or expired, the billing status is active.",
            BILLING_ACCOUNT_NAME,
            false,
            false,
            BillingStatus.ACTIVE),
        Arguments.of(
            "If the workspace is not using initial credits and the credits are exhausted, the billing status is active.",
            BILLING_ACCOUNT_NAME,
            true,
            false,
            BillingStatus.ACTIVE),
        Arguments.of(
            "If the workspace is not using initial credits and the credits are expired, the billing status is active.",
            BILLING_ACCOUNT_NAME,
            false,
            true,
            BillingStatus.ACTIVE),
        Arguments.of(
            "If the workspace is not using initial credits and the credits are both exhausted and expired, the billing status is active.",
            BILLING_ACCOUNT_NAME,
            true,
            true,
            BillingStatus.ACTIVE));
  }

  private void assertResearchPurposeMatches(ResearchPurpose rp) {
    assertThat(rp.getAdditionalNotes()).isEqualTo(sourceDbWorkspace.getAdditionalNotes());
    assertThat(rp.isApproved()).isEqualTo(sourceDbWorkspace.getApproved());
    assertThat(rp.isAncestry()).isEqualTo(sourceDbWorkspace.getAncestry());
    assertThat(rp.getAnticipatedFindings()).isEqualTo(sourceDbWorkspace.getAnticipatedFindings());
    assertThat(rp.isCommercialPurpose()).isEqualTo(sourceDbWorkspace.getCommercialPurpose());
    assertThat(rp.isControlSet()).isEqualTo(sourceDbWorkspace.getControlSet());
    assertThat(rp.isDiseaseFocusedResearch())
        .isEqualTo(sourceDbWorkspace.getDiseaseFocusedResearch());
    assertThat(rp.getDiseaseOfFocus()).isEqualTo(sourceDbWorkspace.getDiseaseOfFocus());
    assertThat(rp.isDrugDevelopment()).isEqualTo(sourceDbWorkspace.getDrugDevelopment());
    assertThat(rp.isEducational()).isEqualTo(sourceDbWorkspace.getEducational());
    assertThat(rp.getIntendedStudy()).isEqualTo(sourceDbWorkspace.getIntendedStudy());
    assertThat(rp.isMethodsDevelopment()).isEqualTo(sourceDbWorkspace.getMethodsDevelopment());
    assertThat(rp.getOtherPopulationDetails())
        .isEqualTo(sourceDbWorkspace.getOtherPopulationDetails());
    assertThat(rp.isOtherPurpose()).isEqualTo(sourceDbWorkspace.getOtherPurpose());
    assertThat(rp.getOtherPurposeDetails()).isEqualTo(sourceDbWorkspace.getOtherPurposeDetails());
    assertThat(rp.getPopulationDetails())
        .containsExactlyElementsIn(sourceDbWorkspace.getSpecificPopulationsEnum());
    assertThat(rp.isPopulationHealth()).isEqualTo(sourceDbWorkspace.getPopulationHealth());
    assertThat(rp.getReasonForAllOfUs()).isEqualTo(sourceDbWorkspace.getReasonForAllOfUs());
    assertThat(rp.isReviewRequested()).isEqualTo(sourceDbWorkspace.getReviewRequested());
    assertThat(rp.isSocialBehavioral()).isEqualTo(sourceDbWorkspace.getSocialBehavioral());
    assertThat(rp.getTimeRequested())
        .isEqualTo(sourceDbWorkspace.getTimeRequested().toInstant().toEpochMilli());
    assertThat(rp.getTimeReviewed()).isNull();

    assertThat(rp.getPopulationDetails()).containsExactlyElementsIn(SPECIFIC_POPULATIONS);
    assertThat(rp.getResearchOutcomeList()).containsExactlyElementsIn(RESEARCH_OUTCOMES);
    assertThat(rp.getDisseminateResearchFindingList()).isEmpty();
    assertThat(rp.getOtherDisseminateResearchFindings()).isEqualTo(DISSEMINATE_FINDINGS_OTHER);
  }
}
