package org.pmiops.workbench.testconfig.fixtures;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
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
  public static ImmutableList<Degree> USER__DEGREES = ImmutableList.of(Degree.BA, Degree.ME);

  @Override
  public void assertDTOFieldsMatchConstants(ReportingUser user) {
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
    assertThat(user.getUsername()).isEqualTo(USER__USERNAME);
    assertThat(user.getInstitutionalRoleEnum()).isEqualTo(USER__INSTITUTIONAL_ROLE_ENUM);
    assertThat(user.getInstitutionalRoleOtherText()).isEqualTo(USER__INSTITUTIONAL_ROLE_OTHER_TEXT);
    assertThat(user.getDisability()).isEqualTo(USER__DISABILITY);
    assertThat(user.getHighestEducation()).isEqualTo(USER__EDUCATION);
    assertThat(user.getSexAtBirth()).isEqualTo(USER__SEX_AT_BIRTH);
    assertThat(user.getYearOfBirth()).isEqualTo(USER__YEAR_OF_BIRTH);
    assertThat(user.getEthnicity()).isEqualTo(USER__ETHNICITY);
    assertThat(user.getDegrees())
        .isEqualTo(USER__DEGREES.stream().map(Degree::toString).collect(Collectors.joining(",")));
    assertThat(user.getRace()).isEqualTo(USER__RACE);
    assertThat(user.getGenderIdentity()).isEqualTo(USER__GENDER_IDENTITY);
    assertThat(user.getIdentifiesAsLgbtq()).isEqualTo(USER__IDENTIFIES_AS_LGBTQ);
    assertThat(user.getLgbtqIdentity()).isEqualTo(USER__LGBTQ_IDENTITY);
  }

  @Override
  public DbUser createEntity() {
    final DbUser user = new DbUser();
    user.setAboutYou(USER__ABOUT_YOU);
    user.setAreaOfResearch(USER__AREA_OF_RESEARCH);
    user.setComplianceTrainingBypassTime(USER__COMPLIANCE_TRAINING_BYPASS_TIME);
    user.setComplianceTrainingCompletionTime(USER__COMPLIANCE_TRAINING_COMPLETION_TIME);
    user.setComplianceTrainingExpirationTime(USER__COMPLIANCE_TRAINING_EXPIRATION_TIME);
    user.setContactEmail(USER__CONTACT_EMAIL);
    user.setCreationTime(USER__CREATION_TIME);
    user.setCurrentPosition(USER__CURRENT_POSITION);
    user.setDataAccessLevel(USER__DATA_ACCESS_LEVEL);
    user.setDataUseAgreementBypassTime(USER__DATA_USE_AGREEMENT_BYPASS_TIME);
    user.setDataUseAgreementCompletionTime(USER__DATA_USE_AGREEMENT_COMPLETION_TIME);
    user.setDataUseAgreementSignedVersion(USER__DATA_USE_AGREEMENT_SIGNED_VERSION);
    user.setDemographicSurveyCompletionTime(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME);
    user.setDisabled(USER__DISABLED);
    user.setEraCommonsBypassTime(USER__ERA_COMMONS_BYPASS_TIME);
    user.setEraCommonsCompletionTime(USER__ERA_COMMONS_COMPLETION_TIME);
    user.setFamilyName(USER__FAMILY_NAME);
    user.setFirstRegistrationCompletionTime(USER__FIRST_REGISTRATION_COMPLETION_TIME);
    user.setFirstSignInTime(USER__FIRST_SIGN_IN_TIME);
    user.setFreeTierCreditsLimitDaysOverride(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    user.setFreeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE);
    user.setGivenName(USER__GIVEN_NAME);
    user.setLastModifiedTime(USER__LAST_MODIFIED_TIME);
    user.setProfessionalUrl(USER__PROFESSIONAL_URL);
    user.setTwoFactorAuthBypassTime(USER__TWO_FACTOR_AUTH_BYPASS_TIME);
    user.setTwoFactorAuthCompletionTime(USER__TWO_FACTOR_AUTH_COMPLETION_TIME);
    user.setUsername(USER__USERNAME);
    user.setDegreesEnum(USER__DEGREES);
    DbDemographicSurvey dbDemographicSurvey = createDbDemographicSurvey();
    dbDemographicSurvey.setUser(user);
    user.setDemographicSurvey(dbDemographicSurvey);
    return user;
  }

  @Override
  public ReportingUser createDto() {
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
            USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE.intValue()) // manual adjustment
        .freeTierCreditsLimitDollarsOverride(USER__FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE)
        .givenName(USER__GIVEN_NAME)
        .lastModifiedTime(offsetDateTimeUtc(USER__LAST_MODIFIED_TIME))
        .professionalUrl(USER__PROFESSIONAL_URL)
        .twoFactorAuthBypassTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_BYPASS_TIME))
        .twoFactorAuthCompletionTime(offsetDateTimeUtc(USER__TWO_FACTOR_AUTH_COMPLETION_TIME))
        .userId(USER__USER_ID)
        .username(USER__USERNAME)
        .city(USER__CITY)
        .country(USER__COUNTRY)
        .state(USER__STATE)
        .streetAddress1(USER__STREET_ADDRESS_1)
        .streetAddress2(USER__STREET_ADDRESS_2)
        .zipCode(USER__ZIP_CODE)
        .institutionId(USER__INSTITUTION_ID)
        .institutionalRoleEnum(USER__INSTITUTIONAL_ROLE_ENUM)
        .institutionalRoleOtherText(USER__INSTITUTIONAL_ROLE_OTHER_TEXT)
        .degrees(USER__DEGREES.stream().map(Degree::toString).collect(Collectors.joining(",")))
        .ethnicity(USER__ETHNICITY)
        .disability(USER__DISABILITY)
        .highestEducation(USER__EDUCATION)
        .sexAtBirth(USER__SEX_AT_BIRTH)
        .race(USER__RACE)
        .yearOfBirth(USER__YEAR_OF_BIRTH)
        .genderIdentity(USER__GENDER_IDENTITY)
        .lgbtqIdentity(USER__LGBTQ_IDENTITY)
        .identifiesAsLgbtq(USER__IDENTIFIES_AS_LGBTQ);
  }

  /**
   * DbAddress is a bit special, because it feeds into the user table on the reporting side.
   *
   * @return
   */
  public DbAddress createDbAddress() {
    final DbAddress address = new DbAddress();
    address.setCity(USER__CITY);
    address.setCountry(USER__COUNTRY);
    address.setState(USER__STATE);
    address.setStreetAddress1(USER__STREET_ADDRESS_1);
    address.setStreetAddress2(USER__STREET_ADDRESS_2);
    address.setZipCode(USER__ZIP_CODE);
    return address;
  }

  /** Creates a {@link DbDemographicSurvey}. */
  public DbDemographicSurvey createDbDemographicSurvey() {
    final DbDemographicSurvey demographicSurvey = new DbDemographicSurvey();
    demographicSurvey.setEducation(DbStorageEnums.educationToStorage(USER__EDUCATION));
    demographicSurvey.setDisability(DbStorageEnums.disabilityToStorage(USER__DISABILITY));
    demographicSurvey.setEthnicity(DbStorageEnums.ethnicityToStorage(USER__ETHNICITY));
    demographicSurvey.setGenderIdentityList(
        ImmutableList.of(DbStorageEnums.genderIdentityToStorage(USER__GENDER_IDENTITY)));
    demographicSurvey.setRace(ImmutableList.of(DbStorageEnums.raceToStorage(USER__RACE)));
    demographicSurvey.setSexAtBirth(
        ImmutableList.of(DbStorageEnums.sexAtBirthToStorage(USER__SEX_AT_BIRTH)));
    demographicSurvey.setYear_of_birth(USER__YEAR_OF_BIRTH.intValue());
    demographicSurvey.setIdentifiesAsLgbtq(USER__IDENTIFIES_AS_LGBTQ);
    demographicSurvey.setLgbtqIdentity(USER__LGBTQ_IDENTITY);
    return demographicSurvey;
  }
}
