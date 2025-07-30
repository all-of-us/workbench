package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingUser;

/*
 * BigQuery user table columns. This list contains some values that are in other tables
 * but joined in the user projection, and omits others that are not exposed in the analytics
 * dataset.
 */
public enum UserColumnValueExtractor implements ColumnValueExtractor<ReportingUser> {
  ACCESS_TIER_SHORT_NAMES("access_tier_short_names", ReportingUser::getAccessTierShortNames),
  AREA_OF_RESEARCH("area_of_research", ReportingUser::getAreaOfResearch),
  COMPLIANCE_TRAINING_BYPASS_TIME(
      "compliance_training_bypass_time",
      u -> toInsertRowString(u.getComplianceTrainingBypassTime())),
  COMPLIANCE_TRAINING_COMPLETION_TIME(
      "compliance_training_completion_time",
      u -> toInsertRowString(u.getComplianceTrainingCompletionTime())),
  CONTACT_EMAIL("contact_email", ReportingUser::getContactEmail),
  CREATION_TIME("creation_time", u -> toInsertRowString(u.getCreationTime())),
  DUCC_BYPASS_TIME("data_use_agreement_bypass_time", u -> toInsertRowString(u.getDuccBypassTime())),
  DUCC_COMPLETION_TIME(
      "data_use_agreement_completion_time", u -> toInsertRowString(u.getDuccCompletionTime())),
  DUCC_SIGNED_VERSION("data_use_agreement_signed_version", ReportingUser::getDuccSignedVersion),
  DEMOGRAPHIC_SURVEY_COMPLETION_TIME(
      "demographic_survey_completion_time",
      u -> toInsertRowString(u.getDemographicSurveyCompletionTime())),
  DISABLED("disabled", ReportingUser::isDisabled),
  ERA_COMMONS_BYPASS_TIME(
      "era_commons_bypass_time", u -> toInsertRowString(u.getEraCommonsBypassTime())),
  ERA_COMMONS_COMPLETION_TIME(
      "era_commons_completion_time", u -> toInsertRowString(u.getEraCommonsCompletionTime())),
  IDENTITY_BYPASS_TIME("identity_bypass_time", u -> toInsertRowString(u.getIdentityBypassTime())),
  IDENTITY_COMPLETION_TIME(
      "identity_completion_time", u -> toInsertRowString(u.getIdentityCompletionTime())),
  IDENTITY_VERIFICATION_SYSTEM(
      "identity_verification_system", ReportingUser::getIdentityVerificationSystem),

  FAMILY_NAME("family_name", ReportingUser::getFamilyName),
  FIRST_REGISTRATION_COMPLETION_TIME(
      "first_registration_completion_time",
      u -> toInsertRowString(u.getFirstRegistrationCompletionTime())),
  REGISTERED_TIER_FIRST_ENABLED_TIME(
      "registered_tier_first_enabled_time",
      u -> toInsertRowString(u.getRegisteredTierFirstEnabledTime())),
  CONTROLLED_TIER_FIRST_ENABLED_TIME(
      "controlled_tier_first_enabled_time",
      u -> toInsertRowString(u.getControlledTierFirstEnabledTime())),
  FIRST_SIGN_IN_TIME("first_sign_in_time", u -> toInsertRowString(u.getFirstSignInTime())),
  FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE(
      "free_tier_credits_limit_dollars_override",
      ReportingUser::getFreeTierCreditsLimitDollarsOverride),
  GIVEN_NAME("given_name", ReportingUser::getGivenName),
  LAST_MODIFIED_TIME("last_modified_time", u -> toInsertRowString(u.getLastModifiedTime())),
  PROFESSIONAL_URL("professional_url", ReportingUser::getProfessionalUrl),
  TWO_FACTOR_AUTH_BYPASS_TIME(
      "two_factor_auth_bypass_time", u -> toInsertRowString(u.getTwoFactorAuthBypassTime())),
  TWO_FACTOR_AUTH_COMPLETION_TIME(
      "two_factor_auth_completion_time",
      u -> toInsertRowString(u.getTwoFactorAuthCompletionTime())),
  USER_ID("user_id", ReportingUser::getUserId),
  USERNAME("username", ReportingUser::getUsername),
  INSTITUTION_ID("institution_id", ReportingUser::getInstitutionId),
  INSTITUTIONAL_ROLE_ENUM(
      "institutional_role_enum", u -> enumToString(u.getInstitutionalRoleEnum())),
  INSTITUTIONAL_ROLE_OTHER_TEXT(
      "institutional_role_other_text", ReportingUser::getInstitutionalRoleOtherText),
  DISABILITY("disability", u -> enumToString(u.getDisability())),
  DEGREES("degrees", ReportingUser::getDegrees),
  ETHNICITY("ethnicity", u -> enumToString(u.getEthnicity())),
  GENDER_IDENTIFIES("gender_identities", ReportingUser::getGenderIdentities),
  HIGHEST_EDUCATION("highest_education", u -> enumToString(u.getHighestEducation())),
  IDENTIFIES_AS_LGBTQ(
      "identifies_as_lgbtq",
      u -> u.isIdentifiesAsLgbtq() == null ? null : u.isIdentifiesAsLgbtq().toString()),
  LGBTQ_IDENTITY("lgbtq_identity", ReportingUser::getLgbtqIdentity),
  SEXES_AT_BIRTH("sexes_at_birth", ReportingUser::getSexesAtBirth),
  RACES("races", ReportingUser::getRaces),
  YEAR_OF_BIRTH(
      "year_of_birth", u -> u.getYearOfBirth() == null ? null : u.getYearOfBirth().intValue()),
  // --- Demographic Survey V2 fields ---
  DSV2_COMPLETION_TIME("dsv2_completion_time", u -> toInsertRowString(u.getDsv2CompletionTime())),
  DSV2_DISABILITY_CONCENTRATING(
      "dsv2_disability_concentrating", ReportingUser::getDsv2DisabilityConcentrating),
  DSV2_DISABILITY_DRESSING("dsv2_disability_dressing", ReportingUser::getDsv2DisabilityDressing),
  DSV2_DISABILITY_ERRANDS("dsv2_disability_errands", ReportingUser::getDsv2DisabilityErrands),
  DSV2_DISABILITY_HEARING("dsv2_disability_hearing", ReportingUser::getDsv2DisabilityHearing),
  DSV2_DISABILITY_OTHER_TEXT(
      "dsv2_disability_other_text", ReportingUser::getDsv2DisabilityOtherText),
  DSV2_DISABILITY_SEEING("dsv2_disability_seeing", ReportingUser::getDsv2DisabilitySeeing),
  DSV2_DISABILITY_WALKING("dsv2_disability_walking", ReportingUser::getDsv2DisabilityWalking),
  DSV2_DISADVANTAGED("dsv2_disadvantaged", ReportingUser::getDsv2Disadvantaged),
  DSV2_EDUCATION("dsv2_education", ReportingUser::getDsv2Education),
  DSV2_ETHNICITY_AI_AN_OTHER_TEXT(
      "dsv2_ethnicity_ai_an_other_text", ReportingUser::getDsv2EthnicityAiAnOtherText),
  DSV2_ETHNICITY_ASIAN_OTHER_TEXT(
      "dsv2_ethnicity_asian_other_text", ReportingUser::getDsv2EthnicityAsianOtherText),
  DSV2_ETHNICITY_BLACK_OTHER_TEXT(
      "dsv2_ethnicity_black_other_text", ReportingUser::getDsv2EthnicityBlackOtherText),
  DSV2_ETHNICITY_HISPANIC_OTHER_TEXT(
      "dsv2_ethnicity_hispanic_other_text", ReportingUser::getDsv2EthnicityHispanicOtherText),
  DSV2_ETHNICITY_ME_NA_OTHER_TEXT(
      "dsv2_ethnicity_me_na_other_text", ReportingUser::getDsv2EthnicityMeNaOtherText),
  DSV2_ETHNICITY_NH_PI_OTHER_TEXT(
      "dsv2_ethnicity_nh_pi_other_text", ReportingUser::getDsv2EthnicityNhPiOtherText),
  DSV2_ETHNICITY_OTHER_TEXT("dsv2_ethnicity_other_text", ReportingUser::getDsv2EthnicityOtherText),
  DSV2_ETHNICITY_WHITE_OTHER_TEXT(
      "dsv2_ethnicity_white_other_text", ReportingUser::getDsv2EthnicityWhiteOtherText),
  DSV2_GENDER_OTHER_TEXT("dsv2_gender_other_text", ReportingUser::getDsv2GenderOtherText),
  DSV2_ORIENTATION_OTHER_TEXT(
      "dsv2_orientation_other_text", ReportingUser::getDsv2OrientationOtherText),
  DSV2_SEX_AT_BIRTH("dsv2_sex_at_birth", ReportingUser::getDsv2SexAtBirth),
  DSV2_SEX_AT_BIRTH_OTHER_TEXT(
      "dsv2_sex_at_birth_other_text", ReportingUser::getDsv2SexAtBirthOtherText),
  DSV2_SURVEY_COMMENTS("dsv2_survey_comments", ReportingUser::getDsv2SurveyComments),
  DSV2_YEAR_OF_BIRTH("dsv2_year_of_birth", ReportingUser::getDsv2YearOfBirth),
  DSV2_YEAR_OF_BIRTH_PREFER_NOT(
      "dsv2_year_of_birth_prefer_not", ReportingUser::isDsv2YearOfBirthPreferNot),
  DSV2_ETHNIC_CATEGORY("dsv2_ethnic_category", ReportingUser::getDsv2EthnicCategory),
  DSV2_GENDER_IDENTITY("dsv2_gender_identity", ReportingUser::getDsv2GenderIdentity),
  DSV2_SEXUAL_ORIENTATION("dsv2_sexual_orientation", ReportingUser::getDsv2SexualOrientation);

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  private final String parameterName;
  private final Function<ReportingUser, Object> objectValueFunction;

  UserColumnValueExtractor(
      String parameterName, Function<ReportingUser, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingUser, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }
}
