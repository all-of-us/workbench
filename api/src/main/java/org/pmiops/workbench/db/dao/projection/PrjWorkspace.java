package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;

public interface PrjWorkspace {
  long getWorkspaceId();

  String getName();

  String getWorkspaceNamespace();

  String getFirecloudName();

  long getDataAccessLevel();

  long getCdrVersionId();

  long getCreatorId();

  Timestamp getCreationTime();

  Timestamp getLastModifiedTime();

  String getRpIntendedStudy();

  boolean getRpDiseaseFocusedResearch();

  String getRpDiseaseOfFocus();

  boolean getRpMethodsDevelopment();

  boolean getRpControlSet();

  boolean getRpAncestry();

  boolean getRpCommercialPurpose();

  String getRpAdditionalNotes();

  boolean getRpReviewRequested();

  boolean getRpApproved();

  Timestamp getRpTimeRequested();

  long getVersion();

  String getFirecloudUuid();

  Timestamp getLastAccessedTime();

  long getActiveStatus();

  boolean getRpSocialBehavioral();

  boolean getRpPopulationHealth();

  boolean getRpEducational();

  boolean getRpOtherPurpose();

  boolean getRpDrugDevelopment();

  String getRpOtherPurposeDetails();

  String getRpReasonForAllOfUs();

  String getRpAnticipatedFindings();

  long getBillingMigrationStatus();

  String getRpOtherPopulationDetails();

  boolean getPublished();

  long getBillingStatus();

  long getBillingAccountType();

  String getBillingAccountName();

  boolean getRpEthics();

  String getDisseminateResearchOther();

  String getRpScientificApproach();

  long getNeedsRpReviewPrompt();
}
