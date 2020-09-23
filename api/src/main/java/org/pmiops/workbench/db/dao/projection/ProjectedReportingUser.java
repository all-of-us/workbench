package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;

// This is a Spring Data projection interface for the Hibernate entity
// class DbUser. The properties listed correspond to query results
// that will be mapped into BigQuery rows in a (mostly) 1:1 fashion.
// Fields may not be renamed or reordered or have their types
// changed unless both the entity class and any queries returning
// this projection type are in complete agreement.

// This code was generated using reporting-wizard.rb at 2020-09-22T12:02:29-04:00.
// Manual modification should be avoided if possible as this is a one-time generation
// and does not run on every build and updates must be merged manually for now.
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

  Short getDataAccessLevel();

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
