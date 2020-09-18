package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.BqDtoUser;

public enum UserParameterColumn implements QueryParameterColumn<BqDtoUser> {
  ABOUT_YOU("about_you", BqDtoUser::getAboutYou, u -> QueryParameterValue.string(u.getAboutYou())),
  AREA_OF_RESEARCH(
      "area_of_research",
      BqDtoUser::getAreaOfResearch,
      u -> QueryParameterValue.string(u.getAreaOfResearch())),
  BETA_ACCESS_BYPASS_TIME(
      "beta_access_bypass_time",
      u -> toInsertRowString(u.getBetaAccessBypassTime()),
      u -> toTimestampQpv(u.getBetaAccessBypassTime())),
  BETA_ACCESS_REQUEST_TIME(
      "beta_access_request_time",
      u -> toInsertRowString(u.getBetaAccessRequestTime()),
      u -> toTimestampQpv(u.getBetaAccessRequestTime())),
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
  CONTACT_EMAIL(
      "contact_email",
      BqDtoUser::getContactEmail,
      u -> QueryParameterValue.string(u.getContactEmail())),
  CREATION_TIME(
      "creation_time",
      u -> toInsertRowString(u.getCreationTime()),
      u -> toTimestampQpv(u.getCreationTime())),
  CURRENT_POSITION(
      "current_position",
      BqDtoUser::getCurrentPosition,
      u -> QueryParameterValue.string(u.getCurrentPosition())),
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
      BqDtoUser::getDataUseAgreementSignedVersion,
      u -> QueryParameterValue.int64(u.getDataUseAgreementSignedVersion())),
  DEMOGRAPHIC_SURVEY_COMPLETION_TIME(
      "demographic_survey_completion_time",
      u -> toInsertRowString(u.getDemographicSurveyCompletionTime()),
      u -> toTimestampQpv(u.getDemographicSurveyCompletionTime())),
  DISABLED("disabled", BqDtoUser::getDisabled, u -> QueryParameterValue.bool(u.getDisabled())),
  EMAIL_VERIFICATION_BYPASS_TIME(
      "email_verification_bypass_time",
      u -> toInsertRowString(u.getEmailVerificationBypassTime()),
      u -> toTimestampQpv(u.getEmailVerificationBypassTime())),
  EMAIL_VERIFICATION_COMPLETION_TIME(
      "email_verification_completion_time",
      u -> toInsertRowString(u.getEmailVerificationCompletionTime()),
      u -> toTimestampQpv(u.getEmailVerificationCompletionTime())),
  EMAIL_VERIFICATION_STATUS(
      "email_verification_status",
      u -> enumToString(u.getEmailVerificationStatus()),
      u -> enumToQpv(u.getEmailVerificationStatus())),
  ERA_COMMONS_BYPASS_TIME(
      "era_commons_bypass_time",
      u -> toInsertRowString(u.getEraCommonsBypassTime()),
      u -> toTimestampQpv(u.getEraCommonsBypassTime())),
  ERA_COMMONS_COMPLETION_TIME(
      "era_commons_completion_time",
      u -> toInsertRowString(u.getEraCommonsCompletionTime()),
      u -> toTimestampQpv(u.getEraCommonsCompletionTime())),
  ERA_COMMONS_LINK_EXPIRE_TIME(
      "era_commons_link_expire_time",
      u -> toInsertRowString(u.getEraCommonsLinkExpireTime()),
      u -> toTimestampQpv(u.getEraCommonsLinkExpireTime())),
  FAMILY_NAME(
      "family_name", BqDtoUser::getFamilyName, u -> QueryParameterValue.string(u.getFamilyName())),
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
      BqDtoUser::getFreeTierCreditsLimitDaysOverride,
      u -> QueryParameterValue.int64(u.getFreeTierCreditsLimitDaysOverride())),
  FREE_TIER_CREDITS_LIMIT_DOLLARS_OVERRIDE(
      "free_tier_credits_limit_dollars_override",
      BqDtoUser::getFreeTierCreditsLimitDollarsOverride,
      u -> QueryParameterValue.float64(u.getFreeTierCreditsLimitDollarsOverride())),
  GIVEN_NAME(
      "given_name", BqDtoUser::getGivenName, u -> QueryParameterValue.string(u.getGivenName())),
  ID_VERIFICATION_BYPASS_TIME(
      "id_verification_bypass_time",
      u -> toInsertRowString(u.getIdVerificationBypassTime()),
      u -> toTimestampQpv(u.getIdVerificationBypassTime())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      u -> toInsertRowString(u.getLastModifiedTime()),
      u -> toTimestampQpv(u.getLastModifiedTime())),
  ORGANIZATION(
      "organization",
      BqDtoUser::getOrganization,
      u -> QueryParameterValue.string(u.getOrganization())),
  PHONE_NUMBER(
      "phone_number",
      BqDtoUser::getPhoneNumber,
      u -> QueryParameterValue.string(u.getPhoneNumber())),
  PROFESSIONAL_URL(
      "professional_url",
      BqDtoUser::getProfessionalUrl,
      u -> QueryParameterValue.string(u.getProfessionalUrl())),
  TWO_FACTOR_AUTH_BYPASS_TIME(
      "two_factor_auth_bypass_time",
      u -> toInsertRowString(u.getTwoFactorAuthBypassTime()),
      u -> toTimestampQpv(u.getTwoFactorAuthBypassTime())),
  TWO_FACTOR_AUTH_COMPLETION_TIME(
      "two_factor_auth_completion_time",
      u -> toInsertRowString(u.getTwoFactorAuthCompletionTime()),
      u -> toTimestampQpv(u.getTwoFactorAuthCompletionTime())),
  USER_ID("user_id", BqDtoUser::getUserId, u -> QueryParameterValue.int64(u.getUserId())),
  USERNAME("username", BqDtoUser::getUsername, u -> QueryParameterValue.string(u.getUsername())),
  CITY("city", BqDtoUser::getCity, a -> QueryParameterValue.string(a.getCity())),
  COUNTRY("country", BqDtoUser::getCountry, a -> QueryParameterValue.string(a.getCountry())),
  STATE("state", BqDtoUser::getState, a -> QueryParameterValue.string(a.getState())),
  STREET_ADDRESS_1(
      "street_address_1",
      BqDtoUser::getStreetAddress1,
      a -> QueryParameterValue.string(a.getStreetAddress1())),
  STREET_ADDRESS_2(
      "street_address_2",
      BqDtoUser::getStreetAddress2,
      a -> QueryParameterValue.string(a.getStreetAddress2())),
  ZIP_CODE("zip_code", BqDtoUser::getZipCode, a -> QueryParameterValue.string(a.getZipCode()));

  private final String parameterName;
  private final Function<BqDtoUser, Object> objectValueFunction;
  private final Function<BqDtoUser, QueryParameterValue> parameterValueFunction;

  UserParameterColumn(
      String parameterName,
      Function<BqDtoUser, Object> objectValueFunction,
      Function<BqDtoUser, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.objectValueFunction = objectValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<BqDtoUser, Object> getRowToInsertValueFunction() {
    return objectValueFunction;
  }

  @Override
  public Function<BqDtoUser, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
