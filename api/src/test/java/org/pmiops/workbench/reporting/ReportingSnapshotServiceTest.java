package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.PrjUser;
import org.pmiops.workbench.db.dao.projection.PrjWorkspace;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingSnapshotServiceTest {
  //  private static final String FAMILY_NAME = "Bobberson";
  //  private static final String CONTACT_EMAIL = "bob@example.com";
  //  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  //  private static final String ORGANIZATION = "Test";
  //  private static final String CURRENT_POSITION = "Tester";
  //  private static final String RESEARCH_PURPOSE = "To test things";
  private static final long NOW_EPOCH_MILLI = 1594404482000L;
  private static final Instant NOW_INSTANT = Instant.ofEpochMilli(NOW_EPOCH_MILLI);
  private static final String USER_GIVEN_NAME_1 = "Marge";
  private static final boolean USER_DISABLED_1 = false;
  private static final long USER_ID_1 = 101L;

  private static final String USER__ABOUT_YOU = "foo";
  private static final String USER__AREA_OF_RESEARCH = "foo";
  private static final Timestamp USER__BETA_ACCESS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__BETA_ACCESS_REQUEST_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__COMPLIANCE_TRAINING_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__COMPLIANCE_TRAINING_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__COMPLIANCE_TRAINING_EXPIRATION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final String USER__CONTACT_EMAIL = "foo";
  private static final Timestamp USER__CREATION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final String USER__CURRENT_POSITION = "foo";
  private static final long USER__DATA_ACCESS_LEVEL = 1001;
  private static final Timestamp USER__DATA_USE_AGREEMENT_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__DATA_USE_AGREEMENT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long USER__DATA_USE_AGREEMENT_SIGNED_VERSION = 1001;
  private static final Timestamp USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final boolean USER__DISABLED = false;
  private static final Timestamp USER__EMAIL_VERIFICATION_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__EMAIL_VERIFICATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long USER__EMAIL_VERIFICATION_STATUS = 1001;
  private static final Timestamp USER__ERA_COMMONS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__ERA_COMMONS_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__ERA_COMMONS_LINK_EXPIRE_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final String USER__FAMILY_NAME = "foo";
  private static final Timestamp USER__FIRST_REGISTRATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__FIRST_SIGN_IN_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE = 1001;
  private static final double USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE = 5.2;
  private static final String USER__GIVEN_NAME = "foo";
  private static final Timestamp USER__ID_VERIFICATION_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__ID_VERIFICATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final String USER__ORGANIZATION = "foo";
  private static final String USER__PHONE_NUMBER = "foo";
  private static final String USER__PROFESSIONAL_URL = "foo";
  private static final Timestamp USER__TWO_FACTOR_AUTH_BYPASS_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp USER__TWO_FACTOR_AUTH_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long USER__USER_ID = 1001;
  private static final String USER__USERNAME = "foo";

  private static final long WORKSPACE__ACTIVE_STATUS = 1001;
  private static final String WORKSPACE__BILLING_ACCOUNT_NAME = "foo";
  private static final long WORKSPACE__BILLING_ACCOUNT_TYPE = 1001;
  private static final long WORKSPACE__BILLING_MIGRATION_STATUS = 1001;
  private static final long WORKSPACE__BILLING_STATUS = 1001;
  private static final long WORKSPACE__CDR_VERSION_ID = 1001;
  private static final Timestamp WORKSPACE__CREATION_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long WORKSPACE__CREATOR_ID = 1001;
  private static final long WORKSPACE__DATA_ACCESS_LEVEL = 1001;
  private static final String WORKSPACE__DISSEMINATE_RESEARCH_OTHER = "foo";
  private static final String WORKSPACE__FIRECLOUD_NAME = "foo";
  private static final String WORKSPACE__FIRECLOUD_UUID = "foo";
  private static final Timestamp WORKSPACE__LAST_ACCESSED_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final Timestamp WORKSPACE__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final String WORKSPACE__NAME = "foo";
  private static final long WORKSPACE__NEEDS_RP_REVIEW_PROMPT = 1001;
  private static final boolean WORKSPACE__PUBLISHED = false;
  private static final String WORKSPACE__RP_ADDITIONAL_NOTES = "foo";
  private static final boolean WORKSPACE__RP_ANCESTRY = false;
  private static final String WORKSPACE__RP_ANTICIPATED_FINDINGS = "foo";
  private static final boolean WORKSPACE__RP_APPROVED = false;
  private static final boolean WORKSPACE__RP_COMMERCIAL_PURPOSE = false;
  private static final boolean WORKSPACE__RP_CONTROL_SET = false;
  private static final boolean WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH = false;
  private static final String WORKSPACE__RP_DISEASE_OF_FOCUS = "foo";
  private static final boolean WORKSPACE__RP_DRUG_DEVELOPMENT = false;
  private static final boolean WORKSPACE__RP_EDUCATIONAL = false;
  private static final boolean WORKSPACE__RP_ETHICS = false;
  private static final String WORKSPACE__RP_INTENDED_STUDY = "foo";
  private static final boolean WORKSPACE__RP_METHODS_DEVELOPMENT = false;
  private static final String WORKSPACE__RP_OTHER_POPULATION_DETAILS = "foo";
  private static final boolean WORKSPACE__RP_OTHER_PURPOSE = false;
  private static final String WORKSPACE__RP_OTHER_PURPOSE_DETAILS = "foo";
  private static final boolean WORKSPACE__RP_POPULATION_HEALTH = false;
  private static final String WORKSPACE__RP_REASON_FOR_ALL_OF_US = "foo";
  private static final boolean WORKSPACE__RP_REVIEW_REQUESTED = false;
  private static final String WORKSPACE__RP_SCIENTIFIC_APPROACH = "foo";
  private static final boolean WORKSPACE__RP_SOCIAL_BEHAVIORAL = false;
  private static final Timestamp WORKSPACE__RP_TIME_REQUESTED =
      Timestamp.from(Instant.parse("2005-01-01T00:00:00.00Z"));
  private static final long WORKSPACE__VERSION = 1001;
  private static final long WORKSPACE__WORKSPACE_ID = 1001;
  private static final String WORKSPACE__WORKSPACE_NAMESPACE = "foo";

  @MockBean private Random mockRandom;
  @MockBean private UserService mockUserService;
  @MockBean private Stopwatch mockStopwatch;
  @MockBean private WorkspaceService mockWorkspaceService;

  @Autowired private ReportingSnapshotService reportingSnapshotService;

  private static final TestMockFactory TEST_MOCK_FACTORY = new TestMockFactory();

  @TestConfiguration
  @Import({
    CommonMappers.class,
    ReportingMapperImpl.class,
    ReportingSnapshotServiceImpl.class,
    ReportingUploadServiceDmlImpl.class
  })
  @MockBean({BigQueryService.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW_INSTANT);
    }
  }

  @Before
  public void setup() {
    // Return "random" numbers 100, 101, 102...
    doAnswer(
            new Answer<Long>() {
              private long lastValue = 100;

              public Long answer(InvocationOnMock invocation) {
                return lastValue++;
              }
            })
        .when(mockRandom)
        .nextLong();
    TestMockFactory.stubStopwatch(mockStopwatch, Duration.ofMillis(100));
  }

  public void mockWorkspaces() {
    final PrjWorkspace mockWorkspace = mock(PrjWorkspace.class);
    doReturn(WORKSPACE__ACTIVE_STATUS).when(mockWorkspace).getActiveStatus();
    doReturn(WORKSPACE__BILLING_ACCOUNT_NAME).when(mockWorkspace).getBillingAccountName();
    doReturn(WORKSPACE__BILLING_ACCOUNT_TYPE).when(mockWorkspace).getBillingAccountType();
    doReturn(WORKSPACE__BILLING_MIGRATION_STATUS).when(mockWorkspace).getBillingMigrationStatus();
    doReturn(WORKSPACE__BILLING_STATUS).when(mockWorkspace).getBillingStatus();
    doReturn(WORKSPACE__CDR_VERSION_ID).when(mockWorkspace).getCdrVersionId();
    doReturn(WORKSPACE__CREATION_TIME).when(mockWorkspace).getCreationTime();
    doReturn(WORKSPACE__CREATOR_ID).when(mockWorkspace).getCreatorId();
    doReturn(WORKSPACE__DATA_ACCESS_LEVEL).when(mockWorkspace).getDataAccessLevel();
    doReturn(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .when(mockWorkspace)
        .getDisseminateResearchOther();
    doReturn(WORKSPACE__FIRECLOUD_NAME).when(mockWorkspace).getFirecloudName();
    doReturn(WORKSPACE__FIRECLOUD_UUID).when(mockWorkspace).getFirecloudUuid();
    doReturn(WORKSPACE__LAST_ACCESSED_TIME).when(mockWorkspace).getLastAccessedTime();
    doReturn(WORKSPACE__LAST_MODIFIED_TIME).when(mockWorkspace).getLastModifiedTime();
    doReturn(WORKSPACE__NAME).when(mockWorkspace).getName();
    doReturn(WORKSPACE__NEEDS_RP_REVIEW_PROMPT).when(mockWorkspace).getNeedsRpReviewPrompt();
    doReturn(WORKSPACE__PUBLISHED).when(mockWorkspace).getPublished();
    doReturn(WORKSPACE__RP_ADDITIONAL_NOTES).when(mockWorkspace).getRpAdditionalNotes();
    doReturn(WORKSPACE__RP_ANCESTRY).when(mockWorkspace).getRpAncestry();
    doReturn(WORKSPACE__RP_ANTICIPATED_FINDINGS).when(mockWorkspace).getRpAnticipatedFindings();
    doReturn(WORKSPACE__RP_APPROVED).when(mockWorkspace).getRpApproved();
    doReturn(WORKSPACE__RP_COMMERCIAL_PURPOSE).when(mockWorkspace).getRpCommercialPurpose();
    doReturn(WORKSPACE__RP_CONTROL_SET).when(mockWorkspace).getRpControlSet();
    doReturn(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH)
        .when(mockWorkspace)
        .getRpDiseaseFocusedResearch();
    doReturn(WORKSPACE__RP_DISEASE_OF_FOCUS).when(mockWorkspace).getRpDiseaseOfFocus();
    doReturn(WORKSPACE__RP_DRUG_DEVELOPMENT).when(mockWorkspace).getRpDrugDevelopment();
    doReturn(WORKSPACE__RP_EDUCATIONAL).when(mockWorkspace).getRpEducational();
    doReturn(WORKSPACE__RP_ETHICS).when(mockWorkspace).getRpEthics();
    doReturn(WORKSPACE__RP_INTENDED_STUDY).when(mockWorkspace).getRpIntendedStudy();
    doReturn(WORKSPACE__RP_METHODS_DEVELOPMENT).when(mockWorkspace).getRpMethodsDevelopment();
    doReturn(WORKSPACE__RP_OTHER_POPULATION_DETAILS)
        .when(mockWorkspace)
        .getRpOtherPopulationDetails();
    doReturn(WORKSPACE__RP_OTHER_PURPOSE).when(mockWorkspace).getRpOtherPurpose();
    doReturn(WORKSPACE__RP_OTHER_PURPOSE_DETAILS).when(mockWorkspace).getRpOtherPurposeDetails();
    doReturn(WORKSPACE__RP_POPULATION_HEALTH).when(mockWorkspace).getRpPopulationHealth();
    doReturn(WORKSPACE__RP_REASON_FOR_ALL_OF_US).when(mockWorkspace).getRpReasonForAllOfUs();
    doReturn(WORKSPACE__RP_REVIEW_REQUESTED).when(mockWorkspace).getRpReviewRequested();
    doReturn(WORKSPACE__RP_SCIENTIFIC_APPROACH).when(mockWorkspace).getRpScientificApproach();
    doReturn(WORKSPACE__RP_SOCIAL_BEHAVIORAL).when(mockWorkspace).getRpSocialBehavioral();
    doReturn(WORKSPACE__RP_TIME_REQUESTED).when(mockWorkspace).getRpTimeRequested();
    doReturn(WORKSPACE__VERSION).when(mockWorkspace).getVersion();
    doReturn(WORKSPACE__WORKSPACE_ID).when(mockWorkspace).getWorkspaceId();
    doReturn(WORKSPACE__WORKSPACE_NAMESPACE).when(mockWorkspace).getWorkspaceNamespace();

    doReturn(ImmutableList.of(mockWorkspace)).when(mockWorkspaceService).getReportingWorkspaces();
  }

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat(snapshot.getCaptureTimestamp()).isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getResearchers()).isEmpty();
    assertThat(snapshot.getWorkspaces()).isEmpty();
  }

  @Test
  public void testGetSnapshot_someEntries() {
    mockUsers();
    mockWorkspaces();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertTimeApprox(snapshot.getCaptureTimestamp(), NOW_INSTANT.toEpochMilli());
    assertThat(snapshot.getResearchers()).hasSize(2);

    final BqDtoUser user = snapshot.getResearchers().get(0);
    assertThat(user.getAboutYou()).isEqualTo(USER__ABOUT_YOU);
    assertThat(user.getAreaOfResearch()).isEqualTo(USER__AREA_OF_RESEARCH);
    assertTimeApprox(user.getBetaAccessBypassTime(), USER__BETA_ACCESS_BYPASS_TIME);
    assertTimeApprox(user.getBetaAccessRequestTime(), USER__BETA_ACCESS_REQUEST_TIME);
    assertTimeApprox(user.getComplianceTrainingBypassTime(), USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    assertTimeApprox(
        user.getComplianceTrainingCompletionTime(), USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    assertTimeApprox(
        user.getComplianceTrainingExpirationTime(), USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    assertThat(user.getContactEmail()).isEqualTo(USER__CONTACT_EMAIL);
    assertTimeApprox(user.getCreationTime(), USER__CREATION_TIME);
    assertThat(user.getCurrentPosition()).isEqualTo(USER__CURRENT_POSITION);
    assertThat(user.getDataAccessLevel()).isEqualTo(USER__DATA_ACCESS_LEVEL);
    assertTimeApprox(user.getDataUseAgreementBypassTime(), USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    assertTimeApprox(
        user.getDataUseAgreementCompletionTime(), USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    assertThat(user.getDataUseAgreementSignedVersion())
        .isEqualTo(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    assertTimeApprox(
        user.getDemographicSurveyCompletionTime(), USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    assertThat(user.getDisabled()).isEqualTo(USER__DISABLED);
    assertTimeApprox(user.getEmailVerificationBypassTime(), USER__EMAIL_VERIFICATION_BYPASS_TIME);
    assertTimeApprox(
        user.getEmailVerificationCompletionTime(), USER__EMAIL_VERIFICATION_COMPLETION_TIME);
    assertThat(user.getEmailVerificationStatus()).isEqualTo(USER__EMAIL_VERIFICATION_STATUS);
    assertTimeApprox(user.getEraCommonsBypassTime(), USER__ERA_COMMONS_BYPASS_TIME);
    assertTimeApprox(user.getEraCommonsCompletionTime(), USER__ERA_COMMONS_COMPLETION_TIME);
    assertTimeApprox(user.getEraCommonsLinkExpireTime(), USER__ERA_COMMONS_LINK_EXPIRE_TIME);
    assertThat(user.getFamilyName()).isEqualTo(USER__FAMILY_NAME);
    assertTimeApprox(
        user.getFirstRegistrationCompletionTime(), USER__FIRST_REGISTRATION_COMPLETION_TIME);
    assertTimeApprox(user.getFirstSignInTime(), USER__FIRST_SIGN_IN_TIME);
    assertThat(user.getFreeTierCreditsLimitDaysOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    assertThat(user.getFreeTierCreditsLimitDollarsOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    assertThat(user.getGivenName()).isEqualTo(USER__GIVEN_NAME);
    assertTimeApprox(user.getIdVerificationBypassTime(), USER__ID_VERIFICATION_BYPASS_TIME);
    assertTimeApprox(user.getIdVerificationCompletionTime(), USER__ID_VERIFICATION_COMPLETION_TIME);
    assertTimeApprox(user.getLastModifiedTime(), USER__LAST_MODIFIED_TIME);
    assertThat(user.getOrganization()).isEqualTo(USER__ORGANIZATION);
    assertThat(user.getPhoneNumber()).isEqualTo(USER__PHONE_NUMBER);
    assertThat(user.getProfessionalUrl()).isEqualTo(USER__PROFESSIONAL_URL);
    assertTimeApprox(user.getTwoFactorAuthBypassTime(), USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    assertTimeApprox(user.getTwoFactorAuthCompletionTime(), USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    assertThat(user.getUserId()).isEqualTo(USER__USER_ID);
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);

    assertThat(snapshot.getWorkspaces()).hasSize(1);
    final BqDtoWorkspace workspace1 = snapshot.getWorkspaces().get(0);
    //    assertThat(workspace1.getWorkspaceId()).isEqualTo(101L);
    //    assertThat(workspace1.getName()).isEqualTo("A Tale of Two Cities");
    //    assertThat(workspace1.getCreatorId()).isNull(); // not stubbed
  }

  private void mockUsers() {
    final List<PrjUser> users =
        ImmutableList.of(
            mockUserProjection(USER_GIVEN_NAME_1, USER_DISABLED_1, USER_ID_1),
            mockUserProjection("Homer", false, 102L));
    doReturn(users).when(mockUserService).getRepotingUsers();
  }

  private PrjUser mockUserProjection(String givenName, boolean disabled, long userId) {
    final PrjUser mockUser = mock(PrjUser.class);
    doReturn(USER__ABOUT_YOU).when(mockUser).getAboutYou();
    doReturn(USER__AREA_OF_RESEARCH).when(mockUser).getAreaOfResearch();
    doReturn(USER__BETA_ACCESS_BYPASS_TIME).when(mockUser).getBetaAccessBypassTime();
    doReturn(USER__BETA_ACCESS_REQUEST_TIME).when(mockUser).getBetaAccessRequestTime();
    doReturn(USER__COMPLIANCE_TRAINING_BYPASS_TIME)
        .when(mockUser)
        .getComplianceTrainingBypassTime();
    doReturn(USER__COMPLIANCE_TRAINING_COMPLETION_TIME)
        .when(mockUser)
        .getComplianceTrainingCompletionTime();
    doReturn(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME)
        .when(mockUser)
        .getComplianceTrainingExpirationTime();
    doReturn(USER__CONTACT_EMAIL).when(mockUser).getContactEmail();
    doReturn(USER__CREATION_TIME).when(mockUser).getCreationTime();
    doReturn(USER__CURRENT_POSITION).when(mockUser).getCurrentPosition();
    doReturn(USER__DATA_ACCESS_LEVEL).when(mockUser).getDataAccessLevel();
    doReturn(USER__DATA_USE_AGREEMENT_BYPASS_TIME).when(mockUser).getDataUseAgreementBypassTime();
    doReturn(USER__DATA_USE_AGREEMENT_COMPLETION_TIME)
        .when(mockUser)
        .getDataUseAgreementCompletionTime();
    doReturn(USER__DATA_USE_AGREEMENT_SIGNED_VERSION)
        .when(mockUser)
        .getDataUseAgreementSignedVersion();
    doReturn(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME)
        .when(mockUser)
        .getDemographicSurveyCompletionTime();
    doReturn(USER__DISABLED).when(mockUser).getDisabled();
    doReturn(USER__EMAIL_VERIFICATION_BYPASS_TIME).when(mockUser).getEmailVerificationBypassTime();
    doReturn(USER__EMAIL_VERIFICATION_COMPLETION_TIME)
        .when(mockUser)
        .getEmailVerificationCompletionTime();
    doReturn(USER__EMAIL_VERIFICATION_STATUS).when(mockUser).getEmailVerificationStatus();
    doReturn(USER__ERA_COMMONS_BYPASS_TIME).when(mockUser).getEraCommonsBypassTime();
    doReturn(USER__ERA_COMMONS_COMPLETION_TIME).when(mockUser).getEraCommonsCompletionTime();
    doReturn(USER__ERA_COMMONS_LINK_EXPIRE_TIME).when(mockUser).getEraCommonsLinkExpireTime();
    doReturn(USER__FAMILY_NAME).when(mockUser).getFamilyName();
    doReturn(USER__FIRST_REGISTRATION_COMPLETION_TIME)
        .when(mockUser)
        .getFirstRegistrationCompletionTime();
    doReturn(USER__FIRST_SIGN_IN_TIME).when(mockUser).getFirstSignInTime();
    doReturn(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE)
        .when(mockUser)
        .getFreeTierCreditsLimitDaysOverride();
    doReturn(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .when(mockUser)
        .getFreeTierCreditsLimitDollarsOverride();
    doReturn(USER__GIVEN_NAME).when(mockUser).getGivenName();
    doReturn(USER__ID_VERIFICATION_BYPASS_TIME).when(mockUser).getIdVerificationBypassTime();
    doReturn(USER__ID_VERIFICATION_COMPLETION_TIME)
        .when(mockUser)
        .getIdVerificationCompletionTime();
    doReturn(USER__LAST_MODIFIED_TIME).when(mockUser).getLastModifiedTime();
    doReturn(USER__ORGANIZATION).when(mockUser).getOrganization();
    doReturn(USER__PHONE_NUMBER).when(mockUser).getPhoneNumber();
    doReturn(USER__PROFESSIONAL_URL).when(mockUser).getProfessionalUrl();
    doReturn(USER__TWO_FACTOR_AUTH_BYPASS_TIME).when(mockUser).getTwoFactorAuthBypassTime();
    doReturn(USER__TWO_FACTOR_AUTH_COMPLETION_TIME).when(mockUser).getTwoFactorAuthCompletionTime();
    doReturn(USER__USER_ID).when(mockUser).getUserId();
    doReturn(USER__USERNAME).when(mockUser).getUsername();
    return mockUser;
  }
}
