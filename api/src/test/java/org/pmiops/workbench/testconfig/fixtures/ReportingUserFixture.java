package org.pmiops.workbench.testconfig.fixtures;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.SexAtBirth;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Test class helper methods for types associated with the user table.
 *
 * <p>This was a static utility class, but it's handy to have all of the reporting types implement a
 * common interface.
 */
@Qualifier("REPORTING_USER_TEST_FIXTURE")
@Service
public class ReportingUserFixture implements ReportingTestFixture<DbUser, ReportingUser> {
  // All constant values, mocking statements, and assertions in this file are generated. The values
  // are chosen so that errors with transposed columns can be caught.
  // Mapping Short values with valid enums can be tricky, and currently there are
  // a handful of places where we have to use a Short in the projection interface but an Enum
  //  type in the model class. An example of such a manual fix is the following:
  // .duccSignedVersion(USER__DATA_USER_CODE_OF_CONDUCT_SIGNED_VERSION.longValue())

  public static final String USER__AREA_OF_RESEARCH = "foo_1";
  public static final Timestamp USER__COMPLIANCE_TRAINING_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-07T00:00:00.00Z"));
  public static final Timestamp USER__COMPLIANCE_TRAINING_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-08T00:00:00.00Z"));
  public static final String USER__CONTACT_EMAIL = "foo_5";
  public static final Timestamp USER__CREATION_TIME =
      Timestamp.from(Instant.parse("2015-05-11T00:00:00.00Z"));
  public static final String USER__ACCESS_TIER_SHORT_NAMES =
      AccessTierService.REGISTERED_TIER_SHORT_NAME;
  public static final Timestamp USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-14T00:00:00.00Z"));
  public static final Timestamp USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-15T00:00:00.00Z"));
  public static final Integer USER__DATA_USER_CODE_OF_CONDUCT_SIGNED_VERSION = 11;
  public static final Timestamp USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-17T00:00:00.00Z"));
  public static final Boolean USER__DISABLED = false;
  public static final Timestamp USER__ERA_COMMONS_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-19T00:00:00.00Z"));
  public static final Timestamp USER__ERA_COMMONS_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-20T00:00:00.00Z"));
  public static final Timestamp USER__IDENTITY_BYPASS_TIME =
      Timestamp.from(Instant.parse("2015-05-21T00:00:00.00Z"));
  public static final Timestamp USER__IDENTITY_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-22T00:00:00.00Z"));
  public static final String USER__FAMILY_NAME = "foo_16";
  public static final Timestamp USER__FIRST_SIGN_IN_TIME =
      Timestamp.from(Instant.parse("2015-05-23T00:00:00.00Z"));
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
  public static final Long USER__INSTITUTION_ID = 0L;
  public static final InstitutionalRole USER__INSTITUTIONAL_ROLE_ENUM =
      InstitutionalRole.UNDERGRADUATE;
  public static final String USER__INSTITUTIONAL_ROLE_OTHER_TEXT = "foo_2";

  // Those enums are manually added
  public static Ethnicity USER__ETHNICITY = Ethnicity.PREFER_NO_ANSWER;
  public static Disability USER__DISABILITY = Disability.PREFER_NO_ANSWER;
  public static SexAtBirth USER__SEX_AT_BIRTH = SexAtBirth.PREFER_NO_ANSWER;
  public static Education USER__EDUCATION = Education.PREFER_NO_ANSWER;
  public static GenderIdentity USER__GENDER_IDENTITY = GenderIdentity.PREFER_NO_ANSWER;
  public static Race USER__RACE = Race.PREFER_NO_ANSWER;
  public static BigDecimal USER__YEAR_OF_BIRTH = BigDecimal.valueOf(2021);
  public static String USER__LGBTQ_IDENTITY = "foo_28";
  public static boolean USER__IDENTIFIES_AS_LGBTQ = false;
  public static List<Degree> USER__DEGREES = List.of(Degree.BA, Degree.ME);
  public static String USER__INITIALS = "foo_29";
  public static final Timestamp DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME =
      Timestamp.from(Instant.parse("2015-05-31T00:00:00.00Z"));

  @Override
  public void assertDTOFieldsMatchConstants(ReportingUser user) {
    assertThat(user.getAreaOfResearch()).isEqualTo(USER__AREA_OF_RESEARCH);
    assertTimeApprox(user.getComplianceTrainingBypassTime(), USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    assertTimeApprox(
        user.getComplianceTrainingCompletionTime(), USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    assertThat(user.getContactEmail()).isEqualTo(USER__CONTACT_EMAIL);
    assertThat(user.getAccessTierShortNames()).isEqualTo(USER__ACCESS_TIER_SHORT_NAMES);
    assertTimeApprox(
        user.getDuccBypassTime(), USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME);
    assertTimeApprox(user.getDuccCompletionTime(), USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME);
    assertThat(user.getDuccSignedVersion())
        .isEqualTo(USER__DATA_USER_CODE_OF_CONDUCT_SIGNED_VERSION);
    assertTimeApprox(
        user.getDemographicSurveyCompletionTime(), USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    assertThat(user.isDisabled()).isEqualTo(USER__DISABLED);
    assertTimeApprox(user.getEraCommonsBypassTime(), USER__ERA_COMMONS_BYPASS_TIME);
    assertTimeApprox(user.getEraCommonsCompletionTime(), USER__ERA_COMMONS_COMPLETION_TIME);
    assertThat(user.getFamilyName()).isEqualTo(USER__FAMILY_NAME);
    assertTimeApprox(user.getFirstSignInTime(), USER__FIRST_SIGN_IN_TIME);
    assertThat(user.getFreeTierCreditsLimitDollarsOverride())
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    assertThat(user.getGivenName()).isEqualTo(USER__GIVEN_NAME);
    assertThat(user.getProfessionalUrl()).isEqualTo(USER__PROFESSIONAL_URL);
    assertTimeApprox(user.getTwoFactorAuthBypassTime(), USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    assertTimeApprox(user.getTwoFactorAuthCompletionTime(), USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);
    assertThat(user.getInstitutionalRoleEnum()).isEqualTo(USER__INSTITUTIONAL_ROLE_ENUM);
    assertThat(user.getInstitutionalRoleOtherText()).isEqualTo(USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
    assertThat(user.getDisability()).isEqualTo(USER__DISABILITY);
    assertThat(user.getHighestEducation()).isEqualTo(USER__EDUCATION);
    assertThat(user.getSexesAtBirth()).isEqualTo(USER__SEX_AT_BIRTH.toString());
    assertThat(user.getYearOfBirth()).isEqualTo(USER__YEAR_OF_BIRTH);
    assertThat(user.getEthnicity()).isEqualTo(USER__ETHNICITY);
    assertThat(user.getDegrees())
        .isEqualTo(USER__DEGREES.stream().map(Degree::toString).collect(Collectors.joining(",")));
    assertThat(user.getRaces()).isEqualTo(USER__RACE.toString());
    assertThat(user.getGenderIdentities()).isEqualTo(USER__GENDER_IDENTITY.toString());
    assertThat(user.isIdentifiesAsLgbtq()).isEqualTo(USER__IDENTIFIES_AS_LGBTQ);
    assertThat(user.getLgbtqIdentity()).isEqualTo(USER__LGBTQ_IDENTITY);

    // Simple null check only. These are autopopulated by Hibernate, so if the entity is saved to
    // the database, these values will not match what was specified in this text fixture. We
    // populate values when creating the DTO to support tests that may not persist to the DB layer.
    assertThat(user.getCreationTime()).isNotNull();
    assertThat(user.getLastModifiedTime()).isNotNull();
  }

  @Override
  public DbUser createEntity() {
    final DbUser user = new DbUser();
    user.setAreaOfResearch(USER__AREA_OF_RESEARCH);
    user.setContactEmail(USER__CONTACT_EMAIL);
    user.setCreationTime(USER__CREATION_TIME);
    user.setDemographicSurveyCompletionTime(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    user.setDisabled(USER__DISABLED);
    user.setFamilyName(USER__FAMILY_NAME);
    user.setFirstSignInTime(USER__FIRST_SIGN_IN_TIME);
    user.setInitialCreditsLimitOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    user.setGivenName(USER__GIVEN_NAME);
    user.setLastModifiedTime(USER__LAST_MODIFIED_TIME);
    user.setProfessionalUrl(USER__PROFESSIONAL_URL);
    user.setUsername(USER__USERNAME);
    user.setDegreesEnum(USER__DEGREES);

    DbDemographicSurvey dbDemographicSurvey = createDbDemographicSurvey();
    dbDemographicSurvey.setUser(user);
    user.setDemographicSurvey(dbDemographicSurvey);

    DbUserCodeOfConductAgreement duccAgreement = createDbUserCodeOfConductAgreement();
    duccAgreement.setUser(user);
    user.setDuccAgreement(duccAgreement);

    return user;
  }

  @Override
  public ReportingUser createDto() {
    return new ReportingUser()
        .areaOfResearch(USER__AREA_OF_RESEARCH)
        .complianceTrainingBypassTime(offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_BYPASS_TIME))
        .complianceTrainingCompletionTime(
            offsetDateTimeUtc(USER__COMPLIANCE_TRAINING_COMPLETION_TIME))
        .contactEmail(USER__CONTACT_EMAIL)
        .creationTime(offsetDateTimeUtc(USER__CREATION_TIME))
        .duccBypassTime(offsetDateTimeUtc(USER__DATA_USER_CODE_OF_CONDUCT_AGREEMENT_BYPASS_TIME))
        .duccCompletionTime(offsetDateTimeUtc(USER__DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME))
        .duccSignedVersion(USER__DATA_USER_CODE_OF_CONDUCT_SIGNED_VERSION)
        .demographicSurveyCompletionTime(
            offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME))
        .disabled(USER__DISABLED)
        .eraCommonsBypassTime(offsetDateTimeUtc(USER__ERA_COMMONS_BYPASS_TIME))
        .eraCommonsCompletionTime(offsetDateTimeUtc(USER__ERA_COMMONS_COMPLETION_TIME))
        .familyName(USER__FAMILY_NAME)
        .firstSignInTime(offsetDateTimeUtc(USER__FIRST_SIGN_IN_TIME))
        .freeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .givenName(USER__GIVEN_NAME)
        .lastModifiedTime(offsetDateTimeUtc(USER__LAST_MODIFIED_TIME))
        .professionalUrl(USER__PROFESSIONAL_URL)
        .twoFactorAuthBypassTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_BYPASS_TIME))
        .twoFactorAuthCompletionTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_COMPLETION_TIME))
        .userId(USER__USER_ID)
        .username(USER__USERNAME)
        .institutionId(USER__INSTITUTION_ID)
        .institutionalRoleEnum(USER__INSTITUTIONAL_ROLE_ENUM)
        .institutionalRoleOtherText(USER__INSTITUTIONAL_ROLE_OTHER_TEXT)
        .degrees(USER__DEGREES.stream().map(Degree::toString).collect(Collectors.joining(",")))
        .ethnicity(USER__ETHNICITY)
        .disability(USER__DISABILITY)
        .highestEducation(USER__EDUCATION)
        .sexesAtBirth(USER__SEX_AT_BIRTH.toString())
        .races(USER__RACE.toString())
        .yearOfBirth(USER__YEAR_OF_BIRTH)
        .genderIdentities(USER__GENDER_IDENTITY.toString())
        .lgbtqIdentity(USER__LGBTQ_IDENTITY)
        .identifiesAsLgbtq(USER__IDENTIFIES_AS_LGBTQ);
  }

  /** Creates a {@link DbDemographicSurvey}. */
  public DbDemographicSurvey createDbDemographicSurvey() {
    final DbDemographicSurvey demographicSurvey = new DbDemographicSurvey();
    demographicSurvey.setEducation(DbStorageEnums.educationToStorage(USER__EDUCATION));
    demographicSurvey.setDisability(DbStorageEnums.disabilityToStorage(USER__DISABILITY));
    demographicSurvey.setEthnicity(DbStorageEnums.ethnicityToStorage(USER__ETHNICITY));
    demographicSurvey.setGenderIdentityList(
        List.of(DbStorageEnums.genderIdentityToStorage(USER__GENDER_IDENTITY)));
    demographicSurvey.setRace(List.of(DbStorageEnums.raceToStorage(USER__RACE)));
    demographicSurvey.setSexAtBirth(
        List.of(DbStorageEnums.sexAtBirthToStorage(USER__SEX_AT_BIRTH)));
    demographicSurvey.setYearOfBirth(USER__YEAR_OF_BIRTH.intValue());
    demographicSurvey.setIdentifiesAsLgbtq(USER__IDENTIFIES_AS_LGBTQ);
    demographicSurvey.setLgbtqIdentity(USER__LGBTQ_IDENTITY);
    return demographicSurvey;
  }

  public DbUserCodeOfConductAgreement createDbUserCodeOfConductAgreement() {
    DbUserCodeOfConductAgreement duccAgreement = new DbUserCodeOfConductAgreement();
    duccAgreement.setSignedVersion(USER__DATA_USER_CODE_OF_CONDUCT_SIGNED_VERSION);
    duccAgreement.setUserFamilyName(USER__FAMILY_NAME);
    duccAgreement.setUserGivenName(USER__GIVEN_NAME);
    duccAgreement.setUserInitials(USER__INITIALS);
    duccAgreement.setCompletionTime(DATA_USER_CODE_OF_CONDUCT_COMPLETION_TIME);
    return duccAgreement;
  }
}
