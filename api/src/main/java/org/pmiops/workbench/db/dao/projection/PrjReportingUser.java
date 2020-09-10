package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;

/** Order must match SELECT query order exactly for projection to work properly. */
public interface PrjReportingUser {

  long getUserId();

  String getUsername();

  long getDataAccessLevel();

  String getContactEmail();

  String getGivenName();

  String getFamilyName();

  String getPhoneNumber();

  Timestamp getFirstSignInTime();

  boolean getIdVerificationIsValid();

  Timestamp getDemographicSurveyCompletionTime();

  boolean getDisabled();

  long getEmailVerificationStatus();

  String getAreaOfResearch();

  String getAboutYou();

  long getClusterCreateRetries();

  long getBillingProjectRetries();

  Timestamp getBetaAccessRequestTime();

  String getCurrentPosition();

  String getOrganization();

  String getEraCommonsLinkedNihUsername();

  Timestamp getEraCommonsLinkExpireTime();

  Timestamp getEraCommonsCompletionTime();

  Timestamp getDataUseAgreementCompletionTime();

  Timestamp getDataUseAgreementBypassTime();

  Timestamp getComplianceTrainingCompletionTime();

  Timestamp getComplianceTrainingBypassTime();

  Timestamp getBetaAccessBypassTime();

  Timestamp getEmailVerificationCompletionTime();

  Timestamp getEmailVerificationBypassTime();

  Timestamp getEraCommonsBypassTime();

  Timestamp getIdVerificationCompletionTime();

  Timestamp getIdVerificationBypassTime();

  Timestamp getTwoFactorAuthCompletionTime();

  Timestamp getTwoFactorAuthBypassTime();

  Timestamp getComplianceTrainingExpirationTime();

  long getDataUseAgreementSignedVersion();

  double getFreeTierCreditsLimitDollarsOverride();

  long getFreeTierCreditsLimitDaysOverride();

  Timestamp getLastFreeTierCreditsTimeCheck();

  Timestamp getFirstRegistrationCompletionTime();

  Timestamp getCreationTime();

  Timestamp getLastModifiedTime();

  String getProfessionalUrl();

  // fields from DbAddress
  String getStreetAddress1();

  String getStreetAddress2();

  String getZipCode();

  String getCity();

  String getState();

  String getCountry();
}
