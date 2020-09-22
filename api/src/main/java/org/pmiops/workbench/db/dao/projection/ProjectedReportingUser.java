package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;
import org.pmiops.workbench.model.DataAccessLevel;

public interface ProjectedReportingUser {
  /*
   * User columns
   */
  String getAboutYou();

  String getAreaOfResearch();

  Timestamp getComplianceTrainingBypassTime();

  Timestamp getComplianceTrainingCompletionTime();

  Timestamp getComplianceTrainingExpirationTime();

  String getContactEmail();

  Timestamp getCreationTime();

  String getCurrentPosition();

  DataAccessLevel getDataAccessLevel();

  Timestamp getDataUseAgreementBypassTime();

  Timestamp getDataUseAgreementCompletionTime();

  Integer getDataUseAgreementSignedVersion();

  Timestamp getDemographicSurveyCompletionTime();

  Boolean getDisabled();

  Timestamp getEraCommonsBypassTime();

  Timestamp getEraCommonsCompletionTime();

  String getFamilyName();

  Timestamp getFirstRegistrationCompletionTime();

  Timestamp getFirstSignInTime();

  Short getFreeTierCreditsLimitDaysOverride();

  Double getFreeTierCreditsLimitDollarsOverride();

  String getGivenName();

  Timestamp getLastModifiedTime();

  String getProfessionalUrl();

  Timestamp getTwoFactorAuthBypassTime();

  Timestamp getTwoFactorAuthCompletionTime();

  Long getUserId();

  String getUsername();

  /*
   * Address columns
   */
  String getCity();

  String getCountry();

  String getState();

  String getStreetAddress1();

  String getStreetAddress2();

  String getZipCode();
}
