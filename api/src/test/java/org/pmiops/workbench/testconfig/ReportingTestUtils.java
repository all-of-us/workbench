package org.pmiops.workbench.testconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import java.sql.Timestamp;
import java.time.Instant;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;

public class ReportingTestUtils {
  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION.longValue())

  // This code was generated using reporting-wizard.rb at 2020-09-23T15:56:47-04:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  public static final String USER__ABOUT_YOU = "foo_0";
  public static final String USER__AREA_OF_RESEARCH = "foo_1";
  public static final Timestamp USER__COMPLIANCE_TRAINING_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-07T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_EXPIRATION_TIME =
      Timestamp.from(Instant.parse("2015-05-09T00:00:00.00Z"));
  public static final String USER__CONTACT_EMAIL = "foo_5";
  public static final Timestamp USER__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-11T00:00:00.00Z"));
  public static final String USER__CURRENT_POSITION = "foo_7";
  public static final Short USER__DATA_ACCESS_LEVEL = 1;
  public static final Timestamp USER__DATA_USE_AGREEMENT_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-14T00:00:00.00Z"));
  public static final Timestamp USER__DATA_USE_AGREEMENT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-15T00:00:00.00Z"));
  public static final Integer USER__DATA_USE_AGREEMENT_SIGNED_VERSION = 11;
  public static final Timestamp USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-17T00:00:00.00Z"));
  public static final Boolean USER__DISABLED = false;
  public static final Timestamp USER__ERA_COMMONS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-19T00:00:00.00Z"));
  public static final Timestamp USER__ERA_COMMONS_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-20T00:00:00.00Z"));
  public static final String USER__FAMILY_NAME = "foo_16";
  public static final Timestamp USER__FIRST_REGISTRATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-22T00:00:00.00Z"));
  public static final Timestamp USER__FIRST_SIGN_IN_TIME =
      Timestamp.from(Instant.parse("2015-05-23T00:00:00.00Z"));
  public static final Short USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE = 19;
  public static final Double USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE = 20.500000;
  public static final String USER__GIVEN_NAME = "foo_21";
  public static final Timestamp USER__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-27T00:00:00.00Z"));
  public static final String USER__PROFESSIONAL_URL = "foo_23";
  public static final Timestamp USER__TWO_FACTOR_AUTH_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-29T00:00:00.00Z"));
  public static final Timestamp USER__TWO_FACTOR_AUTH_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-30T00:00:00.00Z"));
  public static final Long USER__USER_ID = 26L;
  public static final String USER__USERNAME = "foo_27";
  // Address fields - manually renamed
  public static final String USER__CITY = "foo_0";
  public static final String USER__COUNTRY = "foo_1";
  public static final String USER__STATE = "foo_2";
  public static final String USER__STREET_ADDRESS_1 = "foo_3";
  public static final String USER__STREET_ADDRESS_2 = "foo_4";
  public static final String USER__ZIP_CODE = "foo_5";

  public static final BillingAccountType WORKSPACE__BILLING_ACCOUNT_TYPE =
      BillingAccountType.FREE_TIER;
  public static final BillingStatus WORKSPACE__BILLING_STATUS = BillingStatus.ACTIVE;
  public static final Long WORKSPACE__CDR_VERSION_ID = 2L;
  public static final Timestamp WORKSPACE__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Long WORKSPACE__CREATOR_ID = 4L;
  public static final String WORKSPACE__DISSEMINATE_RESEARCH_OTHER = "foo_6";
  public static final Timestamp WORKSPACE__LAST_ACCESSED_TIME =
      Timestamp.from(Instant.parse("2015-05-12T00:00:00.00Z"));
  public static final Timestamp WORKSPACE__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-13T00:00:00.00Z"));
  public static final String WORKSPACE__NAME = "foo_9";
  public static final Short WORKSPACE__NEEDS_RP_REVIEW_PROMPT = 10;
  public static final Boolean WORKSPACE__PUBLISHED = false;
  public static final String WORKSPACE__RP_ADDITIONAL_NOTES = "foo_12";
  public static final Boolean WORKSPACE__RP_ANCESTRY = false;
  public static final String WORKSPACE__RP_ANTICIPATED_FINDINGS = "foo_14";
  public static final Boolean WORKSPACE__RP_APPROVED = false;
  public static final Boolean WORKSPACE__RP_COMMERCIAL_PURPOSE = true;
  public static final Boolean WORKSPACE__RP_CONTROL_SET = false;
  public static final Boolean WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH = true;
  public static final String WORKSPACE__RP_DISEASE_OF_FOCUS = "foo_19";
  public static final Boolean WORKSPACE__RP_DRUG_DEVELOPMENT = true;
  public static final Boolean WORKSPACE__RP_EDUCATIONAL = false;
  public static final Boolean WORKSPACE__RP_ETHICS = true;
  public static final String WORKSPACE__RP_INTENDED_STUDY = "foo_23";
  public static final Boolean WORKSPACE__RP_METHODS_DEVELOPMENT = true;
  public static final String WORKSPACE__RP_OTHER_POPULATION_DETAILS = "foo_25";
  public static final Boolean WORKSPACE__RP_OTHER_PURPOSE = true;
  public static final String WORKSPACE__RP_OTHER_PURPOSE_DETAILS = "foo_27";
  public static final Boolean WORKSPACE__RP_POPULATION_HEALTH = true;
  public static final String WORKSPACE__RP_REASON_FOR_ALL_OF_US = "foo_29";
  public static final Boolean WORKSPACE__RP_REVIEW_REQUESTED = true;
  public static final String WORKSPACE__RP_SCIENTIFIC_APPROACH = "foo_31";
  public static final Boolean WORKSPACE__RP_SOCIAL_BEHAVIORAL = true;
  public static final Timestamp WORKSPACE__RP_TIME_REQUESTED =
      Timestamp.from(Instant.parse("2015-06-07T00:00:00.00Z"));
  public static final Long WORKSPACE__WORKSPACE_ID = 34L;

  public static void assertDtoUserFields(ReportingUser user) {
    assertThat(user.getAboutYou()).isEqualTo(USER__ABOUT_YOU);
    assertThat(user.getAreaOfResearch()).isEqualTo(USER__AREA_OF_RESEARCH);
    assertTimeApprox(user.getComplianceTrainingBypassTime(), USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    assertTimeApprox(
        user.getComplianceTrainingCompletionTime(), USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    assertTimeApprox(
        user.getComplianceTrainingExpirationTime(), USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    assertThat(user.getContactEmail()).isEqualTo(USER__CONTACT_EMAIL);
    assertTimeApprox(user.getCreationTime(), USER__CREATION_TIME);
    assertThat(user.getCurrentPosition()).isEqualTo(USER__CURRENT_POSITION);
    assertThat(user.getDataAccessLevel())
        .isEqualTo(
            DbStorageEnums.dataAccessLevelFromStorage(
                USER__DATA_ACCESS_LEVEL)); // manual adjustment
    assertTimeApprox(user.getDataUseAgreementBypassTime(), USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    assertTimeApprox(
        user.getDataUseAgreementCompletionTime(), USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    assertThat(user.getDataUseAgreementSignedVersion())
        .isEqualTo(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    assertTimeApprox(
        user.getDemographicSurveyCompletionTime(), USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    assertThat(user.getDisabled()).isEqualTo(USER__DISABLED);
    assertTimeApprox(user.getEraCommonsBypassTime(), USER__ERA_COMMONS_BYPASS_TIME);
    assertTimeApprox(user.getEraCommonsCompletionTime(), USER__ERA_COMMONS_COMPLETION_TIME);
    assertThat(user.getFamilyName()).isEqualTo(USER__FAMILY_NAME);
    assertTimeApprox(
        user.getFirstRegistrationCompletionTime(), USER__FIRST_REGISTRATION_COMPLETION_TIME);
    assertTimeApprox(user.getFirstSignInTime(), USER__FIRST_SIGN_IN_TIME);
    assertThat(user.getFreeTierCreditsLimitDaysOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    assertThat(user.getFreeTierCreditsLimitDollarsOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    assertThat(user.getGivenName()).isEqualTo(USER__GIVEN_NAME);
    assertTimeApprox(user.getLastModifiedTime(), USER__LAST_MODIFIED_TIME);
    assertThat(user.getProfessionalUrl()).isEqualTo(USER__PROFESSIONAL_URL);
    assertTimeApprox(user.getTwoFactorAuthBypassTime(), USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    assertTimeApprox(user.getTwoFactorAuthCompletionTime(), USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    assertThat(user.getUserId()).isEqualTo(USER__USER_ID);
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);
  }

  public static void assertDtoWorkspaceFields(
      ReportingWorkspace workspace,
      long expectedWorkspaceId,
      long expectedCdrVersionId,
      long expectedCreatorId) {
    assertThat(workspace.getBillingAccountType()).isEqualTo(WORKSPACE__BILLING_ACCOUNT_TYPE);
    assertThat(workspace.getBillingStatus()).isEqualTo(WORKSPACE__BILLING_STATUS);
    assertThat(workspace.getCdrVersionId()).isEqualTo(expectedCdrVersionId);
    assertTimeApprox(workspace.getCreationTime(), WORKSPACE__CREATION_TIME);
    assertThat(workspace.getCreatorId()).isEqualTo(expectedCreatorId);
    assertThat(workspace.getDisseminateResearchOther())
        .isEqualTo(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    assertTimeApprox(workspace.getLastAccessedTime(), WORKSPACE__LAST_ACCESSED_TIME);
    assertTimeApprox(workspace.getLastModifiedTime(), WORKSPACE__LAST_MODIFIED_TIME);
    assertThat(workspace.getName()).isEqualTo(WORKSPACE__NAME);
    assertThat(workspace.getNeedsRpReviewPrompt()).isEqualTo(WORKSPACE__NEEDS_RP_REVIEW_PROMPT);
    assertThat(workspace.getPublished()).isEqualTo(WORKSPACE__PUBLISHED);
    assertThat(workspace.getRpAdditionalNotes()).isEqualTo(WORKSPACE__RP_ADDITIONAL_NOTES);
    assertThat(workspace.getRpAncestry()).isEqualTo(WORKSPACE__RP_ANCESTRY);
    assertThat(workspace.getRpAnticipatedFindings()).isEqualTo(WORKSPACE__RP_ANTICIPATED_FINDINGS);
    assertThat(workspace.getRpApproved()).isEqualTo(WORKSPACE__RP_APPROVED);
    assertThat(workspace.getRpCommercialPurpose()).isEqualTo(WORKSPACE__RP_COMMERCIAL_PURPOSE);
    assertThat(workspace.getRpControlSet()).isEqualTo(WORKSPACE__RP_CONTROL_SET);
    assertThat(workspace.getRpDiseaseFocusedResearch())
        .isEqualTo(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH);
    assertThat(workspace.getRpDiseaseOfFocus()).isEqualTo(WORKSPACE__RP_DISEASE_OF_FOCUS);
    assertThat(workspace.getRpDrugDevelopment()).isEqualTo(WORKSPACE__RP_DRUG_DEVELOPMENT);
    assertThat(workspace.getRpEducational()).isEqualTo(WORKSPACE__RP_EDUCATIONAL);
    assertThat(workspace.getRpEthics()).isEqualTo(WORKSPACE__RP_ETHICS);
    assertThat(workspace.getRpIntendedStudy()).isEqualTo(WORKSPACE__RP_INTENDED_STUDY);
    assertThat(workspace.getRpMethodsDevelopment()).isEqualTo(WORKSPACE__RP_METHODS_DEVELOPMENT);
    assertThat(workspace.getRpOtherPopulationDetails())
        .isEqualTo(WORKSPACE__RP_OTHER_POPULATION_DETAILS);
    assertThat(workspace.getRpOtherPurpose()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE);
    assertThat(workspace.getRpOtherPurposeDetails()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE_DETAILS);
    assertThat(workspace.getRpPopulationHealth()).isEqualTo(WORKSPACE__RP_POPULATION_HEALTH);
    assertThat(workspace.getRpReasonForAllOfUs()).isEqualTo(WORKSPACE__RP_REASON_FOR_ALL_OF_US);
    assertThat(workspace.getRpReviewRequested()).isEqualTo(WORKSPACE__RP_REVIEW_REQUESTED);
    assertThat(workspace.getRpScientificApproach()).isEqualTo(WORKSPACE__RP_SCIENTIFIC_APPROACH);
    assertThat(workspace.getRpSocialBehavioral()).isEqualTo(WORKSPACE__RP_SOCIAL_BEHAVIORAL);
    assertTimeApprox(workspace.getRpTimeRequested(), WORKSPACE__RP_TIME_REQUESTED);
    assertThat(workspace.getWorkspaceId()).isEqualTo(expectedWorkspaceId);
  }

  public static void assertDtoWorkspaceFields(ReportingWorkspace workspace) {
    assertDtoWorkspaceFields(
        workspace, WORKSPACE__WORKSPACE_ID, WORKSPACE__CDR_VERSION_ID, WORKSPACE__CREATOR_ID);
  }

  // TODO: put these override values into the scaffold script
  public static void assertPrjWorkspaceFields(
      ProjectedReportingWorkspace workspace,
      long expectedWorkspaceId,
      long expectedCdrVersionId,
      long expectedCreatorId) {
    assertThat(workspace.getBillingAccountType()).isEqualTo(WORKSPACE__BILLING_ACCOUNT_TYPE);
    assertThat(workspace.getBillingStatus()).isEqualTo(WORKSPACE__BILLING_STATUS);
    assertThat(workspace.getCdrVersionId()).isEqualTo(expectedCdrVersionId);
    assertTimeApprox(workspace.getCreationTime(), WORKSPACE__CREATION_TIME);
    assertThat(workspace.getCreatorId()).isEqualTo(expectedCreatorId);
    assertThat(workspace.getDisseminateResearchOther())
        .isEqualTo(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    assertTimeApprox(workspace.getLastAccessedTime(), WORKSPACE__LAST_ACCESSED_TIME);
    assertTimeApprox(workspace.getLastModifiedTime(), WORKSPACE__LAST_MODIFIED_TIME);
    assertThat(workspace.getName()).isEqualTo(WORKSPACE__NAME);
    assertThat(workspace.getNeedsRpReviewPrompt()).isEqualTo(WORKSPACE__NEEDS_RP_REVIEW_PROMPT);
    assertThat(workspace.getPublished()).isEqualTo(WORKSPACE__PUBLISHED);
    assertThat(workspace.getRpAdditionalNotes()).isEqualTo(WORKSPACE__RP_ADDITIONAL_NOTES);
    assertThat(workspace.getRpAncestry()).isEqualTo(WORKSPACE__RP_ANCESTRY);
    assertThat(workspace.getRpAnticipatedFindings()).isEqualTo(WORKSPACE__RP_ANTICIPATED_FINDINGS);
    assertThat(workspace.getRpApproved()).isEqualTo(WORKSPACE__RP_APPROVED);
    assertThat(workspace.getRpCommercialPurpose()).isEqualTo(WORKSPACE__RP_COMMERCIAL_PURPOSE);
    assertThat(workspace.getRpControlSet()).isEqualTo(WORKSPACE__RP_CONTROL_SET);
    assertThat(workspace.getRpDiseaseFocusedResearch())
        .isEqualTo(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH);
    assertThat(workspace.getRpDiseaseOfFocus()).isEqualTo(WORKSPACE__RP_DISEASE_OF_FOCUS);
    assertThat(workspace.getRpDrugDevelopment()).isEqualTo(WORKSPACE__RP_DRUG_DEVELOPMENT);
    assertThat(workspace.getRpEducational()).isEqualTo(WORKSPACE__RP_EDUCATIONAL);
    assertThat(workspace.getRpEthics()).isEqualTo(WORKSPACE__RP_ETHICS);
    assertThat(workspace.getRpIntendedStudy()).isEqualTo(WORKSPACE__RP_INTENDED_STUDY);
    assertThat(workspace.getRpMethodsDevelopment()).isEqualTo(WORKSPACE__RP_METHODS_DEVELOPMENT);
    assertThat(workspace.getRpOtherPopulationDetails())
        .isEqualTo(WORKSPACE__RP_OTHER_POPULATION_DETAILS);
    assertThat(workspace.getRpOtherPurpose()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE);
    assertThat(workspace.getRpOtherPurposeDetails()).isEqualTo(WORKSPACE__RP_OTHER_PURPOSE_DETAILS);
    assertThat(workspace.getRpPopulationHealth()).isEqualTo(WORKSPACE__RP_POPULATION_HEALTH);
    assertThat(workspace.getRpReasonForAllOfUs()).isEqualTo(WORKSPACE__RP_REASON_FOR_ALL_OF_US);
    assertThat(workspace.getRpReviewRequested()).isEqualTo(WORKSPACE__RP_REVIEW_REQUESTED);
    assertThat(workspace.getRpScientificApproach()).isEqualTo(WORKSPACE__RP_SCIENTIFIC_APPROACH);
    assertThat(workspace.getRpSocialBehavioral()).isEqualTo(WORKSPACE__RP_SOCIAL_BEHAVIORAL);
    assertTimeApprox(workspace.getRpTimeRequested(), WORKSPACE__RP_TIME_REQUESTED);
    assertThat(workspace.getWorkspaceId()).isEqualTo(expectedWorkspaceId);
  }

  public static ProjectedReportingUser mockProjectedUser() {
    // This code was generated using reporting-wizard.rb at 2020-09-23T15:56:47-04:00.
    // Manual modification should be avoided if possible as this is a one-time generation
    // and does not run on every build and updates must be merged manually for now.
    final ProjectedReportingUser mockUser = mock(ProjectedReportingUser.class);
    doReturn(USER__ABOUT_YOU).when(mockUser).getAboutYou();
    doReturn(USER__AREA_OF_RESEARCH).when(mockUser).getAreaOfResearch();
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
    doReturn(USER__ERA_COMMONS_BYPASS_TIME).when(mockUser).getEraCommonsBypassTime();
    doReturn(USER__ERA_COMMONS_COMPLETION_TIME).when(mockUser).getEraCommonsCompletionTime();
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
    doReturn(USER__LAST_MODIFIED_TIME).when(mockUser).getLastModifiedTime();
    doReturn(USER__PROFESSIONAL_URL).when(mockUser).getProfessionalUrl();
    doReturn(USER__TWO_FACTOR_AUTH_BYPASS_TIME).when(mockUser).getTwoFactorAuthBypassTime();
    doReturn(USER__TWO_FACTOR_AUTH_COMPLETION_TIME).when(mockUser).getTwoFactorAuthCompletionTime();
    doReturn(USER__USER_ID).when(mockUser).getUserId();
    doReturn(USER__USERNAME).when(mockUser).getUsername();
    // address fields
    doReturn(USER__CITY).when(mockUser).getCity();
    doReturn(USER__COUNTRY).when(mockUser).getCountry();
    doReturn(USER__STATE).when(mockUser).getState();
    doReturn(USER__STREET_ADDRESS_1).when(mockUser).getStreetAddress1();
    doReturn(USER__STREET_ADDRESS_2).when(mockUser).getStreetAddress2();
    doReturn(USER__ZIP_CODE).when(mockUser).getZipCode();
    return mockUser;
  }

  public static ProjectedReportingWorkspace mockProjectedWorkspace() {
    final ProjectedReportingWorkspace mockWorkspace = mock(ProjectedReportingWorkspace.class);
    doReturn(WORKSPACE__BILLING_ACCOUNT_TYPE).when(mockWorkspace).getBillingAccountType();
    doReturn(WORKSPACE__BILLING_STATUS).when(mockWorkspace).getBillingStatus();
    doReturn(WORKSPACE__CDR_VERSION_ID).when(mockWorkspace).getCdrVersionId();
    doReturn(WORKSPACE__CREATION_TIME).when(mockWorkspace).getCreationTime();
    doReturn(WORKSPACE__CREATOR_ID).when(mockWorkspace).getCreatorId();
    doReturn(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .when(mockWorkspace)
        .getDisseminateResearchOther();
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
    doReturn(WORKSPACE__WORKSPACE_ID).when(mockWorkspace).getWorkspaceId();
    return mockWorkspace;
  }

  public static ReportingUser createReportingUser() {
    return new ReportingUser()
        .aboutYou(USER__ABOUT_YOU)
        .areaOfResearch(USER__AREA_OF_RESEARCH)
        .complianceTrainingBypassTime(offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_BYPASS_TIME))
        .complianceTrainingCompletionTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_COMPLETION_TIME))
        .complianceTrainingExpirationTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME))
        .contactEmail(USER__CONTACT_EMAIL)
        .creationTime(offsetDateTimeUtc(USER__CREATION_TIME))
        .currentPosition(USER__CURRENT_POSITION)
        .dataAccessLevel(DbStorageEnums.dataAccessLevelFromStorage(USER__DATA_ACCESS_LEVEL))
        .dataUseAgreementBypassTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_BYPASS_TIME))
        .dataUseAgreementCompletionTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_COMPLETION_TIME))
        .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION)
        .demographicSurveyCompletionTime(
            offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME))
        .disabled(USER__DISABLED)
        .eraCommonsBypassTime(offsetDateTimeUtc(USER__ERA_COMMONS_BYPASS_TIME))
        .eraCommonsCompletionTime(offsetDateTimeUtc(USER__ERA_COMMONS_COMPLETION_TIME))
        .familyName(USER__FAMILY_NAME)
        .firstRegistrationCompletionTime(
            offsetDateTimeUtc(USER__FIRST_REGISTRATION_COMPLETION_TIME))
        .firstSignInTime(offsetDateTimeUtc(USER__FIRST_SIGN_IN_TIME))
        .freeTierCreditsLimitDaysOverride(
            USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE.longValue()) // manual adjustment
        .freeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .givenName(USER__GIVEN_NAME)
        .lastModifiedTime(offsetDateTimeUtc(USER__LAST_MODIFIED_TIME))
        .professionalUrl(USER__PROFESSIONAL_URL)
        .twoFactorAuthBypassTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_BYPASS_TIME))
        .twoFactorAuthCompletionTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_COMPLETION_TIME))
        .userId(USER__USER_ID)
        .username(USER__USERNAME);
  }

  public static ReportingWorkspace createDtoWorkspace() {
    return new ReportingWorkspace()
        .billingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE)
        .billingStatus(WORKSPACE__BILLING_STATUS)
        .cdrVersionId(WORKSPACE__CDR_VERSION_ID)
        .creationTime(offsetDateTimeUtc(WORKSPACE__CREATION_TIME))
        .creatorId(WORKSPACE__CREATOR_ID)
        .disseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .lastAccessedTime(offsetDateTimeUtc(WORKSPACE__LAST_ACCESSED_TIME))
        .lastModifiedTime(offsetDateTimeUtc(WORKSPACE__LAST_MODIFIED_TIME))
        .name(WORKSPACE__NAME)
        .needsRpReviewPrompt(WORKSPACE__NEEDS_RP_REVIEW_PROMPT.intValue()) // manual adjustment
        .published(WORKSPACE__PUBLISHED)
        .rpAdditionalNotes(WORKSPACE__RP_ADDITIONAL_NOTES)
        .rpAncestry(WORKSPACE__RP_ANCESTRY)
        .rpAnticipatedFindings(WORKSPACE__RP_ANTICIPATED_FINDINGS)
        .rpApproved(WORKSPACE__RP_APPROVED)
        .rpCommercialPurpose(WORKSPACE__RP_COMMERCIAL_PURPOSE)
        .rpControlSet(WORKSPACE__RP_CONTROL_SET)
        .rpDiseaseFocusedResearch(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH)
        .rpDiseaseOfFocus(WORKSPACE__RP_DISEASE_OF_FOCUS)
        .rpDrugDevelopment(WORKSPACE__RP_DRUG_DEVELOPMENT)
        .rpEducational(WORKSPACE__RP_EDUCATIONAL)
        .rpEthics(WORKSPACE__RP_ETHICS)
        .rpIntendedStudy(WORKSPACE__RP_INTENDED_STUDY)
        .rpMethodsDevelopment(WORKSPACE__RP_METHODS_DEVELOPMENT)
        .rpOtherPopulationDetails(WORKSPACE__RP_OTHER_POPULATION_DETAILS)
        .rpOtherPurpose(WORKSPACE__RP_OTHER_PURPOSE)
        .rpOtherPurposeDetails(WORKSPACE__RP_OTHER_PURPOSE_DETAILS)
        .rpPopulationHealth(WORKSPACE__RP_POPULATION_HEALTH)
        .rpReasonForAllOfUs(WORKSPACE__RP_REASON_FOR_ALL_OF_US)
        .rpReviewRequested(WORKSPACE__RP_REVIEW_REQUESTED)
        .rpScientificApproach(WORKSPACE__RP_SCIENTIFIC_APPROACH)
        .rpSocialBehavioral(WORKSPACE__RP_SOCIAL_BEHAVIORAL)
        .rpTimeRequested(offsetDateTimeUtc(WORKSPACE__RP_TIME_REQUESTED))
        .workspaceId(WORKSPACE__WORKSPACE_ID);
  }

  public static DbWorkspace createDbWorkspace(DbUser creator, DbCdrVersion cdrVersion) {
    final DbWorkspace workspace = new DbWorkspace();
    workspace.setBillingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE);
    workspace.setCdrVersion(cdrVersion);
    workspace.setCreationTime(WORKSPACE__CREATION_TIME);
    workspace.setCreator(creator);
    workspace.setDisseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    workspace.setLastAccessedTime(WORKSPACE__LAST_ACCESSED_TIME);
    workspace.setLastModifiedTime(WORKSPACE__LAST_MODIFIED_TIME);
    workspace.setName(WORKSPACE__NAME);
    workspace.setNeedsResearchPurposeReviewPrompt(WORKSPACE__NEEDS_RP_REVIEW_PROMPT);
    workspace.setPublished(WORKSPACE__PUBLISHED);
    workspace.setAdditionalNotes(WORKSPACE__RP_ADDITIONAL_NOTES);
    workspace.setAncestry(WORKSPACE__RP_ANCESTRY);
    workspace.setAnticipatedFindings(WORKSPACE__RP_ANTICIPATED_FINDINGS);
    workspace.setApproved(WORKSPACE__RP_APPROVED);
    workspace.setCommercialPurpose(WORKSPACE__RP_COMMERCIAL_PURPOSE);
    workspace.setControlSet(WORKSPACE__RP_CONTROL_SET);
    workspace.setDiseaseFocusedResearch(WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH);
    workspace.setDiseaseOfFocus(WORKSPACE__RP_DISEASE_OF_FOCUS);
    workspace.setDrugDevelopment(WORKSPACE__RP_DRUG_DEVELOPMENT);
    workspace.setEducational(WORKSPACE__RP_EDUCATIONAL);
    workspace.setEthics(WORKSPACE__RP_ETHICS);
    workspace.setIntendedStudy(WORKSPACE__RP_INTENDED_STUDY);
    workspace.setMethodsDevelopment(WORKSPACE__RP_METHODS_DEVELOPMENT);
    workspace.setOtherPopulationDetails(WORKSPACE__RP_OTHER_POPULATION_DETAILS);
    workspace.setOtherPurpose(WORKSPACE__RP_OTHER_PURPOSE);
    workspace.setOtherPurposeDetails(WORKSPACE__RP_OTHER_PURPOSE_DETAILS);
    workspace.setPopulationHealth(WORKSPACE__RP_POPULATION_HEALTH);
    workspace.setReasonForAllOfUs(WORKSPACE__RP_REASON_FOR_ALL_OF_US);
    workspace.setReviewRequested(WORKSPACE__RP_REVIEW_REQUESTED);
    workspace.setScientificApproach(WORKSPACE__RP_SCIENTIFIC_APPROACH);
    workspace.setSocialBehavioral(WORKSPACE__RP_SOCIAL_BEHAVIORAL);
    workspace.setTimeRequested(WORKSPACE__RP_TIME_REQUESTED);
    workspace.setWorkspaceId(WORKSPACE__WORKSPACE_ID);
    return workspace;
  }
}
