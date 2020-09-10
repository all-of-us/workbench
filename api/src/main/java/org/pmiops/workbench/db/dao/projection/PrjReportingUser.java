package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;

/** Order must match SELECT query order exactly for projection to work properly. */
public interface PrjReportingUser {
  String getAboutYou();
  String getAreaOfResearch();
  Timestamp getBetaAccessBypassTime();
  Timestamp getBetaAccessRequestTime();
  Timestamp getComplianceTrainingBypassTime();
  Timestamp getComplianceTrainingCompletionTime();
  Timestamp getComplianceTrainingExpirationTime();
  String getContactEmail();
  Timestamp getCreationTime();
  String getCurrentPosition();
  long getDataAccessLevel();
  Timestamp getDataUseAgreementBypassTime();
  Timestamp getDataUseAgreementCompletionTime();
  long getDataUseAgreementSignedVersion();
  Timestamp getDemographicSurveyCompletionTime();
  boolean getDisabled();
  String getEmail();
  Timestamp getEmailVerificationBypassTime();
  Timestamp getEmailVerificationCompletionTime();
  long getEmailVerificationStatus();
  Timestamp getEraCommonsBypassTime();
  Timestamp getEraCommonsCompletionTime();
  Timestamp getEraCommonsLinkExpireTime();
  String getFamilyName();
  Timestamp getFirstRegistrationCompletionTime();
  Timestamp getFirstSignInTime();
  long getFreeTierCreditsLimitDaysOverride();
  double getFreeTierCreditsLimitDollarsOverride();
  String getGivenName();
  Timestamp getIdVerificationBypassTime();
  Timestamp getIdVerificationCompletionTime();
  Timestamp getLastModifiedTime();
  String getOrganization();
  String getPhoneNumber();
  String getProfessionalUrl();
  Timestamp getTwoFactorAuthBypassTime();
  Timestamp getTwoFactorAuthCompletionTime();
  long getUserId();
  String getCity();
  String getCountry();
  String getState();
  String getStreetAddress1();
  String getStreetAddress2();
  String getZipCode();
}
