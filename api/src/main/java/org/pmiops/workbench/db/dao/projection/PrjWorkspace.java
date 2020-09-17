package org.pmiops.workbench.db.dao.projection;

import java.sql.Timestamp;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;

public interface PrjWorkspace {

  String REPORTING_WORKSPACES_QUERY =
      "SELECT\n"
          + "  w.activeStatus,\n"
          + "  w.billingAccountType,\n"
          + "  w.billingStatus,\n"
          + "  w.cdrVersion.cdrVersionId AS cdrVersionId,\n"
          + "  w.creationTime,\n"
          + "  w.creator.userId AS creatorId,\n"
          + "  w.dataAccessLevel,\n"
          + "  w.disseminateResearchOther,\n"
          + "  w.firecloudName,\n"
          + "  w.firecloudUuid,\n"
          + "  w.lastAccessedTime,\n"
          + "  w.lastModifiedTime,\n"
          + "  w.name,\n"
          + "  w.needsResearchPurposeReviewPrompt AS needsRpReviewPrompt,\n"
          + "  w.published,\n"
          + "  w.additionalNotes AS rpAdditionalNotes,\n"
          + "  w.ancestry AS rpAncestry,\n"
          + "  w.anticipatedFindings AS rpAnticipatedFindings,\n"
          + "  w.approved AS rpApproved,\n"
          + "  w.commercialPurpose AS rpCommercialPurpose,\n"
          + "  w.controlSet AS rpControlSet,\n"
          + "  w.diseaseFocusedResearch AS rpDiseaseFocusedResearch,\n"
          + "  w.diseaseOfFocus AS rpDiseaseOfFocus,\n"
          + "  w.drugDevelopment AS rpDrugDevelopment,\n"
          + "  w.educational AS rpEducational,\n"
          + "  w.ethics AS rpEthics,\n"
          + "  w.intendedStudy AS rpIntendedStudy,\n"
          + "  w.methodsDevelopment AS rpMethodsDevelopment,\n"
          + "  w.otherPopulationDetails AS rpOtherPopulationDetails,\n"
          + "  w.otherPurpose AS rpOtherPurpose,\n"
          + "  w.otherPurposeDetails AS rpOtherPurposeDetails,\n"
          + "  w.populationHealth AS rpPopulationHealth,\n"
          + "  w.reasonForAllOfUs AS rpReasonForAllOfUs,\n"
          + "  w.reviewRequested AS rpReviewRequested,\n"
          + "  w.scientificApproach AS rpScientificApproach,\n"
          + "  w.socialBehavioral AS rpSocialBehavioral,\n"
          + "  w.timeRequested AS rpTimeRequested,\n"
          + "  w.workspaceId,\n"
          + "  w.workspaceNamespace\n"
          + "FROM DbWorkspace w";
  Short getActiveStatus();
  BillingAccountType getBillingAccountType();
  BillingStatus getBillingStatus();
  Long getCdrVersionId();
  Timestamp getCreationTime();
  Long getCreatorId();
  DataAccessLevel getDataAccessLevel();
  String getDisseminateResearchOther();
  String getFirecloudName();
  String getFirecloudUuid();
  Timestamp getLastAccessedTime();
  Timestamp getLastModifiedTime();
  String getName();
  Short getNeedsRpReviewPrompt();
  Boolean getPublished();
  String getRpAdditionalNotes();
  Boolean getRpAncestry();
  String getRpAnticipatedFindings();
  Boolean getRpApproved();
  Boolean getRpCommercialPurpose();
  Boolean getRpControlSet();
  Boolean getRpDiseaseFocusedResearch();
  String getRpDiseaseOfFocus();
  Boolean getRpDrugDevelopment();
  Boolean getRpEducational();
  Boolean getRpEthics();
  String getRpIntendedStudy();
  Boolean getRpMethodsDevelopment();
  String getRpOtherPopulationDetails();
  Boolean getRpOtherPurpose();
  String getRpOtherPurposeDetails();
  Boolean getRpPopulationHealth();
  String getRpReasonForAllOfUs();
  Boolean getRpReviewRequested();
  String getRpScientificApproach();
  Boolean getRpSocialBehavioral();
  Timestamp getRpTimeRequested();
  Long getWorkspaceId();
  String getWorkspaceNamespace();
}
