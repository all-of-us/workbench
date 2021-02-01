package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspace;

/*
 * Column data and metadata convertors for BigQuery workspace table in reporting dataset.
 */
public enum WorkspaceColumnValueExtractor implements ColumnValueExtractor<ReportingWorkspace> {
  BILLING_ACCOUNT_TYPE("billing_account_type", w -> enumToString(w.getBillingAccountType())),
  BILLING_STATUS("billing_status", w -> enumToString(w.getBillingStatus())),
  CDR_VERSION_ID("cdr_version_id", ReportingWorkspace::getCdrVersionId),
  CREATION_TIME("creation_time", w -> toInsertRowString(w.getCreationTime())),
  CREATOR_ID("creator_id", ReportingWorkspace::getCreatorId),
  DISSEMINATE_RESEARCH_OTHER(
      "disseminate_research_other", ReportingWorkspace::getDisseminateResearchOther),
  LAST_ACCESSED_TIME("last_accessed_time", w -> toInsertRowString(w.getLastAccessedTime())),
  LAST_MODIFIED_TIME("last_modified_time", w -> toInsertRowString(w.getLastModifiedTime())),
  NAME("name", ReportingWorkspace::getName),
  NEEDS_RP_REVIEW_PROMPT("needs_rp_review_prompt", ReportingWorkspace::getNeedsRpReviewPrompt),
  PUBLISHED("published", ReportingWorkspace::getPublished),
  RP_ADDITIONAL_NOTES("rp_additional_notes", ReportingWorkspace::getRpAdditionalNotes),
  RP_ANCESTRY("rp_ancestry", ReportingWorkspace::getRpAncestry),
  RP_ANTICIPATED_FINDINGS("rp_anticipated_findings", ReportingWorkspace::getRpAnticipatedFindings),
  RP_APPROVED("rp_approved", ReportingWorkspace::getRpApproved),
  RP_COMMERCIAL_PURPOSE("rp_commercial_purpose", ReportingWorkspace::getRpCommercialPurpose),
  RP_CONTROL_SET("rp_control_set", ReportingWorkspace::getRpControlSet),
  RP_DISEASE_FOCUSED_RESEARCH(
      "rp_disease_focused_research", ReportingWorkspace::getRpDiseaseFocusedResearch),
  RP_DISEASE_OF_FOCUS("rp_disease_of_focus", ReportingWorkspace::getRpDiseaseOfFocus),
  RP_DRUG_DEVELOPMENT("rp_drug_development", ReportingWorkspace::getRpDrugDevelopment),
  RP_EDUCATIONAL("rp_educational", ReportingWorkspace::getRpEducational),
  RP_ETHICS("rp_ethics", ReportingWorkspace::getRpEthics),
  RP_INTENDED_STUDY("rp_intended_study", ReportingWorkspace::getRpIntendedStudy),
  RP_METHODS_DEVELOPMENT("rp_methods_development", ReportingWorkspace::getRpMethodsDevelopment),
  RP_OTHER_POPULATION_DETAILS(
      "rp_other_population_details", ReportingWorkspace::getRpOtherPopulationDetails),
  RP_OTHER_PURPOSE("rp_other_purpose", ReportingWorkspace::getRpOtherPurpose),
  RP_OTHER_PURPOSE_DETAILS(
      "rp_other_purpose_details", ReportingWorkspace::getRpOtherPurposeDetails),
  RP_POPULATION_HEALTH("rp_population_health", ReportingWorkspace::getRpPopulationHealth),
  RP_REASON_FOR_ALL_OF_US("rp_reason_for_all_of_us", ReportingWorkspace::getRpReasonForAllOfUs),
  RP_REVIEW_REQUESTED("rp_review_requested", ReportingWorkspace::getRpReviewRequested),
  RP_SCIENTIFIC_APPROACH("rp_scientific_approach", ReportingWorkspace::getRpScientificApproach),
  RP_SOCIAL_BEHAVIORAL("rp_social_behavioral", ReportingWorkspace::getRpSocialBehavioral),
  RP_TIME_REQUESTED("rp_time_requested", w -> toInsertRowString(w.getRpTimeRequested())),
  WORKSPACE_ID("workspace_id", ReportingWorkspace::getWorkspaceId),
  WORKSPACE_NAMESPACE("workspace_namespace", ReportingWorkspace::getWorkspaceNamespace);

  public static final String TABLE_NAME = "workspace";
  private final String parameterName;
  private final Function<ReportingWorkspace, Object> rowToInsertValueFunction;

  WorkspaceColumnValueExtractor(
      String parameterName, Function<ReportingWorkspace, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getBigQueryTableName() {
    return TABLE_NAME;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspace, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
