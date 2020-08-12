package org.pmiops.workbench.db.dto;

import java.sql.Timestamp;

// Data transfer object projection interface for User.
// We do not provide a friendly method to fetch the enum here, as
// that would prevent query optimizations. We can handle that step in
// MapStruct.
// See https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#projections.interfaces.open
// Any joined columns will be declared in a DTO extending this one.
public interface DtoUser extends DtoUserCore {
  int getVersion();
  Long getCreationNonce();
  String getContactEmail();
  Short getDataAccessLevel();
  String getGivenName();
  String getFamilyName();
  String getPhoneNumber();
  String getCurrentPosition();
  String getOrganization();
  Double getFreeTierCreditsLimitDollarsOverride();
  Short getFreeTierCreditsLimitDaysOverride();
  Timestamp getLastFreeTierCreditsTimeCheck();
  Timestamp getFirstSignInTime();
  Timestamp getFirstRegistrationCompletionTime();
  Boolean getIdVerificationIsValid();
  String getClusterConfigDefaultRaw();
  Timestamp getDemographicSurveyCompletionTime();
  boolean getDisabled();
  Short getEmailVerificationStatus();
  String getAboutYou();
  String getAreaOfResearch();
  Integer getClusterCreateRetries();
  Integer getBillingProjectRetries();
  Timestamp getBetaAccessRequestTime();
  Integer getMoodleId();
  String getEraCommonsLinkedNihUsername();
  Timestamp getEraCommonsLinkExpireTime();
  Timestamp getEraCommonsCompletionTime();
  Timestamp getDataUseAgreementCompletionTime();
  Timestamp getDataUseAgreementBypassTime();
  Integer getDataUseAgreementSignedVersion();
  Timestamp getComplianceTrainingCompletionTime();
  Timestamp getComplianceTrainingBypassTime();
  Timestamp getComplianceTrainingExpirationTime();
  Timestamp getBetaAccessBypassTime();
  Timestamp getEmailVerificationCompletionTime();
  Timestamp getEmailVerificationBypassTime();
  Timestamp getEraCommonsBypassTime();
  Timestamp getIdVerificationCompletionTime();
  Timestamp getIdVerificationBypassTime();
  Timestamp getTwoFactorAuthCompletionTime();
  Timestamp getTwoFactorAuthBypassTime();
  Timestamp getLastModifiedTime();
  Timestamp getCreationTime();
  String getProfessionalUrl();
}
