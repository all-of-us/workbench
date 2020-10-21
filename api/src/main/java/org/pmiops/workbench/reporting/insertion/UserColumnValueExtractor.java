package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.*;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingUser;

/*
 * BigQuery user table columns. This list contains some values that are in other tables
 * but joined in the user projection, and omits others that are not exposed in the analytics
 * dataset.
 */
public enum UserColumnValueExtractor implements ColumnValueExtractor<ReportingUser> {
  ABOUT_YOU("about_you", ReportingUser::getAboutYou, u -> string(u.getAboutYou())),
  AREA_OF_RESEARCH(
      "area_of_research", ReportingUser::getAreaOfResearch, u -> string(u.getAreaOfResearch())),
  COMPLIANCE_TRAINING_BYPASS_TIME(
      "compliance_training_bypass_time",
      u -> toInsertRowString(u.getComplianceTrainingBypassTime()),
      u -> toTimestampQpv(u.getComplianceTrainingBypassTime())),
  COMPLIANCE_TRAINING_COMPLETION_TIME(
      "compliance_training_completion_time",
      u -> toInsertRowString(u.getComplianceTrainingCompletionTime()),
      u -> toTimestampQpv(u.getComplianceTrainingCompletionTime())),
  COMPLIANCE_TRAINING_EXPIRATION_TIME(
      "compliance_training_expiration_time",
      u -> toInsertRowString(u.getComplianceTrainingExpirationTime()),
      u -> toTimestampQpv(u.getComplianceTrainingExpirationTime())),
  CONTACT_EMAIL("contact_email", ReportingUser::getContactEmail, u -> string(u.getContactEmail())),
  CREATION_TIME(
      "creation_time",
      u -> toInsertRowString(u.getCreationTime()),
      u -> toTimestampQpv(u.getCreationTime())),
  CURRENT_POSITION(
      "current_position", ReportingUser::getCurrentPosition, u -> string(u.getCurrentPosition())),
  DATA_ACCESS_LEVEL(
      "data_access_level",
      u -> enumToString(u.getDataAccessLevel()),
      u -> enumToQpv(u.getDataAccessLevel())),
  DATA_USE_AGREEMENT_BYPASS_TIME(
      "data_use_agreement_bypass_time",
      u -> toInsertRowString(u.getDataUseAgreementBypassTime()),
      u -> toTimestampQpv(u.getDataUseAgreementBypassTime())),
  DATA_USE_AGREEMENT_COMPLETION_TIME(
      "data_use_agreement_completion_time",
      u -> toInsertRowString(u.getDataUseAgreementCompletionTime()),
      u -> toTimestampQpv(u.getDataUseAgreementCompletionTime())),
  DATA_USE_AGREEMENT_SIGNED_VERSION(
      "data_use_agreement_signed_version",
      ReportingUser::getDataUseAgreementSignedVersion,
      u -> int64(u.getDataUseAgreementSignedVersion())),
  DEMOGRAPHIC_SURVEY_COMPLETION_TIME(
      "demographic_survey_completion_time",
      u -> toInsertRowString(u.getDemographicSurveyCompletionTime()),
      u -> toTimestampQpv(u.getDemographicSurveyCompletionTime())),
  DISABLED("disabled", ReportingUser::getDisabled, u -> bool(u.getDisabled())),
  ERA_COMMONS_BYPASS_TIME(
      "era_commons_bypass_time",
      u -> toInsertRowString(u.getEraCommonsBypassTime()),
      u -> toTimestampQpv(u.getEraCommonsBypassTime())),
  ERA_COMMONS_COMPLETION_TIME(
      "era_commons_completion_time",
      u -> toInsertRowString(u.getEraCommonsCompletionTime()),
      u -> toTimestampQpv(u.getEraCommonsCompletionTime())),
  FAMILY_NAME("family_name", ReportingUser::getFamilyName, u -> string(u.getFamilyName())),
  FIRST_REGISTRATION_COMPLETION_TIME(
      "first_registration_completion_time",
      u -> toInsertRowString(u.getFirstRegistrationCompletionTime()),
      u -> toTimestampQpv(u.getFirstRegistrationCompletionTime())),
  FIRST_SIGN_IN_TIME(
      "first_sign_in_time",
      u -> toInsertRowString(u.getFirstSignInTime()),
      u -> toTimestampQpv(u.getFirstSignInTime())),
  FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE(
      "free_tier_credits_limit_days_override",
      ReportingUser::getFreeTierCreditsLimitDaysOverride,
      u -> int64(u.getFreeTierCreditsLimitDaysOverride())),
  FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE(
      "free_tier_credits_limit_dollars_override",
      ReportingUser::getFreeTierCreditsLimitDollarsOverride,
      u -> float64(u.getFreeTierCreditsLimitDollarsOverride())),
  GIVEN_NAME("given_name", ReportingUser::getGivenName, u -> string(u.getGivenName())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      u -> toInsertRowString(u.getLastModifiedTime()),
      u -> toTimestampQpv(u.getLastModifiedTime())),
  PROFESSIONAL_URL(
      "professional_url", ReportingUser::getProfessionalUrl, u -> string(u.getProfessionalUrl())),
  TWO_FACTOR_AUTH_BYPASS_TIME(
      "two_factor_auth_bypass_time",
      u -> toInsertRowString(u.getTwoFactorAuthBypassTime()),
      u -> toTimestampQpv(u.getTwoFactorAuthBypassTime())),
  TWO_FACTOR_AUTH_COMPLETION_TIME(
      "two_factor_auth_completion_time",
      u -> toInsertRowString(u.getTwoFactorAuthCompletionTime()),
      u -> toTimestampQpv(u.getTwoFactorAuthCompletionTime())),
  USER_ID("user_id", ReportingUser::getUserId, u -> int64(u.getUserId())),
  USERNAME("username", ReportingUser::getUsername, u -> string(u.getUsername())),
  CITY("city", ReportingUser::getCity, a -> string(a.getCity())),
  COUNTRY("country", ReportingUser::getCountry, a -> string(a.getCountry())),
  STATE("state", ReportingUser::getState, a -> string(a.getState())),
  STREET_ADDRESS_1(
      "street_address_1", ReportingUser::getStreetAddress1, a -> string(a.getStreetAddress1())),
  STREET_ADDRESS_2(
      "street_address_2", ReportingUser::getStreetAddress2, a -> string(a.getStreetAddress2())),
  ZIP_CODE("zip_code", ReportingUser::getZipCode, a -> string(a.getZipCode())),
  INSTITUTION_ID(
      "institution_id", ReportingUser::getInstitutionId, u -> int64(u.getInstitutionId())),
  INSTITUTIONAL_ROLE_ENUM(
      "institutional_role_enum",
      u -> enumToString(u.getInstitutionalRoleEnum()),
      u -> enumToQpv(u.getInstitutionalRoleEnum())),
  INSTITUTIONAL_ROLE_OTHER_TEXT(
      "institutional_role_other_text",
      ReportingUser::getInstitutionalRoleOtherText,
      u -> string(u.getInstitutionalRoleOtherText()));

  // Much of the repetitive boilerplate below (constructor, setters, etc) can't really be helped,
  // as enums can't be abstract or extend abstract classes.
  public static final String TABLE_NAME = "user";
  private final String parameterName;
  private final Function<ReportingUser, Object> objectValueFunction;
  private final Function<ReportingUser, QueryParameterValue> parameterValueFunction;

  UserColumnValueExtractor(
      String parameterName,
      Function<ReportingUser, Object> objectValueFunction,
      Function<ReportingUser, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingUser, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<ReportingUser, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
