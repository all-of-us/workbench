package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;

public interface PrjWorkspace {
  long getActiveStatus();

  String getBillingAccountName();

  long getBillingAccountType();

  long getBillingMigrationStatus();

  long getBillingStatus();

  long getCdrVersionId();

  Timestamp getCreationTime();

  long getCreatorId();

  long getDataAccessLevel();

  String getDisseminateResearchOther();

  String getFirecloudName();

  String getFirecloudUuid();

  Timestamp getLastAccessedTime();

  Timestamp getLastModifiedTime();

  String getName();

  long getNeedsRpReviewPrompt();

  boolean getPublished();

  String getRpAdditionalNotes();

  boolean getRpAncestry();

  String getRpAnticipatedFindings();

  boolean getRpApproved();

  boolean getRpCommercialPurpose();

  boolean getRpControlSet();

  boolean getRpDiseaseFocusedResearch();

  String getRpDiseaseOfFocus();

  boolean getRpDrugDevelopment();

  boolean getRpEducational();

  boolean getRpEthics();

  String getRpIntendedStudy();

  boolean getRpMethodsDevelopment();

  String getRpOtherPopulationDetails();

  boolean getRpOtherPurpose();

  String getRpOtherPurposeDetails();

  boolean getRpPopulationHealth();

  String getRpReasonForAllOfUs();

  boolean getRpReviewRequested();

  String getRpScientificApproach();

  boolean getRpSocialBehavioral();

  Timestamp getRpTimeRequested();

  long getVersion();

  long getWorkspaceId();

  String getWorkspaceNamespace();
}
