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
  ABOUT_YOU("about_you", ReportingUser::getAboutYou),
  AREA_OF_RESEARCH("area_of_research", ReportingUser::getAreaOfResearch),
  COMPLIANCE_TRAINING_BYPASS_TIME(
      "compliance_training_bypass_time",
      u -> toInsertRowString(u.getComplianceTrainingBypassTime())),
  COMPLIANCE_TRAINING_COMPLETION_TIME(
      "compliance_training_completion_time",
      u -> toInsertRowString(u.getComplianceTrainingCompletionTime())),
  COMPLIANCE_TRAINING_EXPIRATION_TIME(
      "compliance_training_expiration_time",
      u -> toInsertRowString(u.getComplianceTrainingExpirationTime())),
  CONTACT_EMAIL("contact_email", ReportingUser::getContactEmail),
  CREATION_TIME("creation_time", u -> toInsertRowString(u.getCreationTime())),
  CURRENT_POSITION("current_position", ReportingUser::getCurrentPosition),
  DATA_ACCESS_LEVEL("data_access_level", u -> enumToString(u.getDataAccessLevel())),
  DATA_USE_AGREEMENT_BYPASS_TIME(
      "data_use_agreement_bypass_time", u -> toInsertRowString(u.getDataUseAgreementBypassTime())),
  DATA_USE_AGREEMENT_COMPLETION_TIME(
      "data_use_agreement_completion_time",
      u -> toInsertRowString(u.getDataUseAgreementCompletionTime())),
  DATA_USE_AGREEMENT_SIGNED_VERSION(
      "data_use_agreement_signed_version", ReportingUser::getDataUseAgreementSignedVersion),
  DEMOGRAPHIC_SURVEY_COMPLETION_TIME(
      "demographic_survey_completion_time",
      u -> toInsertRowString(u.getDemographicSurveyCompletionTime())),
  DISABLED("disabled", ReportingUser::getDisabled),
  ERA_COMMONS_BYPASS_TIME(
      "era_commons_bypass_time", u -> toInsertRowString(u.getEraCommonsBypassTime())),
  ERA_COMMONS_COMPLETION_TIME(
      "era_commons_completion_time", u -> toInsertRowString(u.getEraCommonsCompletionTime())),
  FAMILY_NAME("family_name", ReportingUser::getFamilyName),
  FIRST_REGISTRATION_COMPLETION_TIME(
      "first_registration_completion_time",
      u -> toInsertRowString(u.getFirstRegistrationCompletionTime())),
  FIRST_SIGN_IN_TIME("first_sign_in_time", u -> toInsertRowString(u.getFirstSignInTime())),
  FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE(
      "free_tier_credits_limit_days_override", ReportingUser::getFreeTierCreditsLimitDaysOverride),
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
  CITY("city", ReportingUser::getCity),
  COUNTRY("country", ReportingUser::getCountry),
  STATE("state", ReportingUser::getState),
  STREET_ADDRESS_1("street_address_1", ReportingUser::getStreetAddress1),
  STREET_ADDRESS_2("street_address_2", ReportingUser::getStreetAddress2),
  ZIP_CODE("zip_code", ReportingUser::getZipCode),
  INSTITUTION_ID("institution_id", ReportingUser::getInstitutionId),
  INSTITUTIONAL_ROLE_ENUM(
      "institutional_role_enum", u -> enumToString(u.getInstitutionalRoleEnum())),
  INSTITUTIONAL_ROLE_OTHER_TEXT(
      "institutional_role_other_text", ReportingUser::getInstitutionalRoleOtherText),
  DISABILITY("disability", u -> enumToString(u.getDisability())),
  DEGREES("degrees", ReportingUser::getDegrees),
  ETHNICITY("ethnicity", u -> enumToString(u.getEthnicity())),
  GENDER_IDENTIFIES("gender_identifies", ReportingUser::getGenderIdentifies),
  HIGHEST_EDUCATION("highest_education", u -> enumToString(u.getHighestEducation())),
  IDENTIFIES_AS_LGBTQ(
      "identifies_as_lgbtq",
      u -> u.getIdentifiesAsLgbtq() == null ? null : u.getIdentifiesAsLgbtq().toString()),
  LGBTQ_IDENTITY("lgbtq_identity", ReportingUser::getLgbtqIdentity),
  SEXES_AT_BIRTH("sexes_at_birth", ReportingUser::getSexesAtBirth),
  RACES("races", ReportingUser::getRaces),
  YEAR_OF_BIRTH(
      "year_of_birth", u -> u.getYearOfBirth() == null ? null : u.getYearOfBirth().intValue());

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  public static final String TABLE_NAME = "user";
  private final String parameterName;
  private final Function<ReportingUser, Object> objectValueFunction;

  UserColumnValueExtractor(
      String parameterName, Function<ReportingUser, Object> objectValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
  }

  @Override
  public String getBigQueryTableName() {
    return TABLE_NAME;
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
