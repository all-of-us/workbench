package org.pmiops.workbench.testconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import java.sql.Timestamp;
import java.time.Instant;
import org.pmiops.workbench.db.dao.projection.PrjUser;
import org.pmiops.workbench.db.dao.projection.PrjWorkspace;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;

public class ReportingTestUtils {
  public static final String USER__ABOUT_YOU = "foo_0";
  public static final String USER__AREA_OF_RESEARCH = "foo_1";
  public static final Timestamp USER__BETA_ACCESS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-07T00:00:00.00Z"));
  public static final Timestamp USER__BETA_ACCESS_REQUEST_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-09T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-10T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_EXPIRATION_TIME =
      Timestamp.from(Instant.parse("2015-05-11T00:00:00.00Z"));
  public static final String USER__CONTACT_EMAIL = "foo_7";
  public static final Timestamp USER__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-13T00:00:00.00Z"));
  public static final String USER__CURRENT_POSITION = "foo_9";
  public static final long USER__DATA_ACCESS_LEVEL = 10L;
  public static final Timestamp USER__DATA_USE_AGREEMENT_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-16T00:00:00.00Z"));
  public static final Timestamp USER__DATA_USE_AGREEMENT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-17T00:00:00.00Z"));
  public static final long USER__DATA_USE_AGREEMENT_SIGNED_VERSION = 13L;
  public static final Timestamp USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-19T00:00:00.00Z"));
  public static final boolean USER__DISABLED = false;
  public static final Timestamp USER__EMAIL_VERIFICATION_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-21T00:00:00.00Z"));
  public static final Timestamp USER__EMAIL_VERIFICATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-22T00:00:00.00Z"));
  public static final long USER__EMAIL_VERIFICATION_STATUS = 18L;
  public static final Timestamp USER__ERA_COMMONS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-24T00:00:00.00Z"));
  public static final Timestamp USER__ERA_COMMONS_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-25T00:00:00.00Z"));
  public static final Timestamp USER__ERA_COMMONS_LINK_EXPIRE_TIME =
      Timestamp.from(Instant.parse("2015-05-26T00:00:00.00Z"));
  public static final String USER__FAMILY_NAME = "foo_22";
  public static final Timestamp USER__FIRST_REGISTRATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-28T00:00:00.00Z"));
  public static final Timestamp USER__FIRST_SIGN_IN_TIME =
      Timestamp.from(Instant.parse("2015-05-29T00:00:00.00Z"));
  public static final long USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE = 25L;
  public static final double USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE = 26.500000;
  public static final String USER__GIVEN_NAME = "foo_27";
  public static final Timestamp USER__ID_VERIFICATION_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-06-02T00:00:00.00Z"));
  public static final Timestamp USER__ID_VERIFICATION_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-06-03T00:00:00.00Z"));
  public static final Timestamp USER__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-06-04T00:00:00.00Z"));
  public static final String USER__ORGANIZATION = "foo_31";
  public static final String USER__PHONE_NUMBER = "foo_32";
  public static final String USER__PROFESSIONAL_URL = "foo_33";
  public static final Timestamp USER__TWO_FACTOR_AUTH_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-06-08T00:00:00.00Z"));
  public static final Timestamp USER__TWO_FACTOR_AUTH_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-06-09T00:00:00.00Z"));
  public static final long USER__USER_ID = 36L;
  public static final String USER__USERNAME = "foo_37";
  public static final String USER__CITY = "foo_0";
  public static final String USER__COUNTRY = "foo_1";
  public static final String USER__STATE = "foo_2";
  public static final String USER__STREET_ADDRESS_1 = "foo_3";
  public static final String USER__STREET_ADDRESS_2 = "foo_4";
  public static final String USER__ZIP_CODE = "foo_5";

  public static final long WORKSPACE__ACTIVE_STATUS = 0L;
  public static final String WORKSPACE__BILLING_ACCOUNT_NAME = "foo_1";
  public static final long WORKSPACE__BILLING_ACCOUNT_TYPE = 2L;
  public static final long WORKSPACE__BILLING_MIGRATION_STATUS = 3L;
  public static final long WORKSPACE__BILLING_STATUS = 4L;
  public static final long WORKSPACE__CDR_VERSION_ID = 5L;
  public static final Timestamp WORKSPACE__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-11T00:00:00.00Z"));
  public static final long WORKSPACE__CREATOR_ID = 7L;
  public static final long WORKSPACE__DATA_ACCESS_LEVEL = 8L;
  public static final String WORKSPACE__DISSEMINATE_RESEARCH_OTHER = "foo_9";
  public static final String WORKSPACE__FIRECLOUD_NAME = "foo_10";
  public static final String WORKSPACE__FIRECLOUD_UUID = "foo_11";
  public static final Timestamp WORKSPACE__LAST_ACCESSED_TIME =
      Timestamp.from(Instant.parse("2015-05-17T00:00:00.00Z"));
  public static final Timestamp WORKSPACE__LAST_MODIFIED_TIME =
      Timestamp.from(Instant.parse("2015-05-18T00:00:00.00Z"));
  public static final String WORKSPACE__NAME = "foo_14";
  public static final long WORKSPACE__NEEDS_RP_REVIEW_PROMPT = 15L;
  public static final boolean WORKSPACE__PUBLISHED = true;
  public static final String WORKSPACE__RP_ADDITIONAL_NOTES = "foo_17";
  public static final boolean WORKSPACE__RP_ANCESTRY = true;
  public static final String WORKSPACE__RP_ANTICIPATED_FINDINGS = "foo_19";
  public static final boolean WORKSPACE__RP_APPROVED = true;
  public static final boolean WORKSPACE__RP_COMMERCIAL_PURPOSE = false;
  public static final boolean WORKSPACE__RP_CONTROL_SET = true;
  public static final boolean WORKSPACE__RP_DISEASE_FOCUSED_RESEARCH = false;
  public static final String WORKSPACE__RP_DISEASE_OF_FOCUS = "foo_24";
  public static final boolean WORKSPACE__RP_DRUG_DEVELOPMENT = false;
  public static final boolean WORKSPACE__RP_EDUCATIONAL = true;
  public static final boolean WORKSPACE__RP_ETHICS = false;
  public static final String WORKSPACE__RP_INTENDED_STUDY = "foo_28";
  public static final boolean WORKSPACE__RP_METHODS_DEVELOPMENT = false;
  public static final String WORKSPACE__RP_OTHER_POPULATION_DETAILS = "foo_30";
  public static final boolean WORKSPACE__RP_OTHER_PURPOSE = false;
  public static final String WORKSPACE__RP_OTHER_PURPOSE_DETAILS = "foo_32";
  public static final boolean WORKSPACE__RP_POPULATION_HEALTH = false;
  public static final String WORKSPACE__RP_REASON_FOR_ALL_OF_US = "foo_34";
  public static final boolean WORKSPACE__RP_REVIEW_REQUESTED = false;
  public static final String WORKSPACE__RP_SCIENTIFIC_APPROACH = "foo_36";
  public static final boolean WORKSPACE__RP_SOCIAL_BEHAVIORAL = false;
  public static final Timestamp WORKSPACE__RP_TIME_REQUESTED =
      Timestamp.from(Instant.parse("2015-06-12T00:00:00.00Z"));
  public static final long WORKSPACE__VERSION = 39L;
  public static final long WORKSPACE__WORKSPACE_ID = 40L;
  public static final String WORKSPACE__WORKSPACE_NAMESPACE = "foo_41";

  public static void assertUserFields(BqDtoUser user) {
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
    assertThat(user.getCity()).isEqualTo(USER__CITY);
    assertThat(user.getCountry()).isEqualTo(USER__COUNTRY);
    assertThat(user.getState()).isEqualTo(USER__STATE);
    assertThat(user.getStreetAddress1()).isEqualTo(USER__STREET_ADDRESS_1);
    assertThat(user.getStreetAddress2()).isEqualTo(USER__STREET_ADDRESS_2);
    assertThat(user.getZipCode()).isEqualTo(USER__ZIP_CODE);
  }

  public static void assertWorkspaceFields(BqDtoWorkspace workspace) {
    assertThat(workspace.getActiveStatus()).isEqualTo(WORKSPACE__ACTIVE_STATUS);
    assertThat(workspace.getBillingAccountName()).isEqualTo(WORKSPACE__BILLING_ACCOUNT_NAME);
    assertThat(workspace.getBillingAccountType()).isEqualTo(WORKSPACE__BILLING_ACCOUNT_TYPE);
    assertThat(workspace.getBillingMigrationStatus())
        .isEqualTo(WORKSPACE__BILLING_MIGRATION_STATUS);
    assertThat(workspace.getBillingStatus()).isEqualTo(WORKSPACE__BILLING_STATUS);
    assertThat(workspace.getCdrVersionId()).isEqualTo(WORKSPACE__CDR_VERSION_ID);
    assertTimeApprox(workspace.getCreationTime(), WORKSPACE__CREATION_TIME);
    assertThat(workspace.getCreatorId()).isEqualTo(WORKSPACE__CREATOR_ID);
    assertThat(workspace.getDataAccessLevel()).isEqualTo(WORKSPACE__DATA_ACCESS_LEVEL);
    assertThat(workspace.getDisseminateResearchOther())
        .isEqualTo(WORKSPACE__DISSEMINATE_RESEARCH_OTHER);
    assertThat(workspace.getFirecloudName()).isEqualTo(WORKSPACE__FIRECLOUD_NAME);
    assertThat(workspace.getFirecloudUuid()).isEqualTo(WORKSPACE__FIRECLOUD_UUID);
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
    assertThat(workspace.getVersion()).isEqualTo(WORKSPACE__VERSION);
    assertThat(workspace.getWorkspaceId()).isEqualTo(WORKSPACE__WORKSPACE_ID);
    assertThat(workspace.getWorkspaceNamespace()).isEqualTo(WORKSPACE__WORKSPACE_NAMESPACE);
  }

  public static PrjUser mockUserProjection() {
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
    doReturn(USER__CITY).when(mockUser).getCity();
    doReturn(USER__COUNTRY).when(mockUser).getCountry();
    doReturn(USER__STATE).when(mockUser).getState();
    doReturn(USER__STREET_ADDRESS_1).when(mockUser).getStreetAddress1();
    doReturn(USER__STREET_ADDRESS_2).when(mockUser).getStreetAddress2();
    doReturn(USER__ZIP_CODE).when(mockUser).getZipCode();
    return mockUser;
  }

  public static PrjWorkspace mockWorkspace() {
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

    return mockWorkspace;
  }

  public static BqDtoUser createDtoUser() {
    return new BqDtoUser()
        .aboutYou(USER__ABOUT_YOU)
        .areaOfResearch(USER__AREA_OF_RESEARCH)
        .betaAccessBypassTime(offsetDateTimeUtc(USER__BETA_ACCESS_BYPASS_TIME))
        .betaAccessRequestTime(offsetDateTimeUtc(USER__BETA_ACCESS_REQUEST_TIME))
        .complianceTrainingBypassTime(offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_BYPASS_TIME))
        .complianceTrainingCompletionTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_COMPLETION_TIME))
        .complianceTrainingExpirationTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME))
        .contactEmail(USER__CONTACT_EMAIL)
        .creationTime(offsetDateTimeUtc(USER__CREATION_TIME))
        .currentPosition(USER__CURRENT_POSITION)
        .dataAccessLevel(USER__DATA_ACCESS_LEVEL)
        .dataUseAgreementBypassTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_BYPASS_TIME))
        .dataUseAgreementCompletionTime(offsetDateTimeUtc(USER__DATA_USE_AGREEMENT_COMPLETION_TIME))
        .dataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION)
        .demographicSurveyCompletionTime(
            offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME))
        .disabled(USER__DISABLED)
        .emailVerificationBypassTime(offsetDateTimeUtc(USER__EMAIL_VERIFICATION_BYPASS_TIME))
        .emailVerificationCompletionTime(
            offsetDateTimeUtc(USER__EMAIL_VERIFICATION_COMPLETION_TIME))
        .emailVerificationStatus(USER__EMAIL_VERIFICATION_STATUS)
        .eraCommonsBypassTime(offsetDateTimeUtc(USER__ERA_COMMONS_BYPASS_TIME))
        .eraCommonsCompletionTime(offsetDateTimeUtc(USER__ERA_COMMONS_COMPLETION_TIME))
        .eraCommonsLinkExpireTime(offsetDateTimeUtc(USER__ERA_COMMONS_LINK_EXPIRE_TIME))
        .familyName(USER__FAMILY_NAME)
        .firstRegistrationCompletionTime(
            offsetDateTimeUtc(USER__FIRST_REGISTRATION_COMPLETION_TIME))
        .firstSignInTime(offsetDateTimeUtc(USER__FIRST_SIGN_IN_TIME))
        .freeTierCreditsLimitDaysOverride(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE)
        .freeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .givenName(USER__GIVEN_NAME)
        .idVerificationBypassTime(offsetDateTimeUtc(USER__ID_VERIFICATION_BYPASS_TIME))
        .idVerificationCompletionTime(offsetDateTimeUtc(USER__ID_VERIFICATION_COMPLETION_TIME))
        .lastModifiedTime(offsetDateTimeUtc(USER__LAST_MODIFIED_TIME))
        .organization(USER__ORGANIZATION)
        .phoneNumber(USER__PHONE_NUMBER)
        .professionalUrl(USER__PROFESSIONAL_URL)
        .twoFactorAuthBypassTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_BYPASS_TIME))
        .twoFactorAuthCompletionTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_COMPLETION_TIME))
        .userId(USER__USER_ID)
        .username(USER__USERNAME);
  }

  public static BqDtoWorkspace createDtoWorkspace() {
    return new BqDtoWorkspace()
        .activeStatus(WORKSPACE__ACTIVE_STATUS)
        .billingAccountName(WORKSPACE__BILLING_ACCOUNT_NAME)
        .billingAccountType(WORKSPACE__BILLING_ACCOUNT_TYPE)
        .billingMigrationStatus(WORKSPACE__BILLING_MIGRATION_STATUS)
        .billingStatus(WORKSPACE__BILLING_STATUS)
        .cdrVersionId(WORKSPACE__CDR_VERSION_ID)
        .creationTime(offsetDateTimeUtc(WORKSPACE__CREATION_TIME))
        .creatorId(WORKSPACE__CREATOR_ID)
        .dataAccessLevel(WORKSPACE__DATA_ACCESS_LEVEL)
        .disseminateResearchOther(WORKSPACE__DISSEMINATE_RESEARCH_OTHER)
        .firecloudName(WORKSPACE__FIRECLOUD_NAME)
        .firecloudUuid(WORKSPACE__FIRECLOUD_UUID)
        .lastAccessedTime(offsetDateTimeUtc(WORKSPACE__LAST_ACCESSED_TIME))
        .lastModifiedTime(offsetDateTimeUtc(WORKSPACE__LAST_MODIFIED_TIME))
        .name(WORKSPACE__NAME)
        .needsRpReviewPrompt(WORKSPACE__NEEDS_RP_REVIEW_PROMPT)
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
        .version(WORKSPACE__VERSION)
        .workspaceId(WORKSPACE__WORKSPACE_ID)
        .workspaceNamespace(WORKSPACE__WORKSPACE_NAMESPACE);
  }
}
