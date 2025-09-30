package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspace;

/*
 * Column data and metadata convertors for BigQuery workspace table in reporting dataset.
 */
public enum WorkspaceColumnValueExtractor implements ColumnValueExtractor<ReportingWorkspace> {
  ACCESS_TIER_SHORT_NAME("access_tier_short_name", ReportingWorkspace::getAccessTierShortName),
  BILLING_ACCOUNT_TYPE("billing_account_type", w -> enumToString(w.getBillingAccountType())),
  CDR_VERSION_ID("cdr_version_id", ReportingWorkspace::getCdrVersionId),
  CREATION_TIME("creation_time", w -> toInsertRowString(w.getCreationTime())),
  CREATOR_ID("creator_id", ReportingWorkspace::getCreatorId),
  DISSEMINATE_RESEARCH_OTHER(
      "disseminate_research_other", ReportingWorkspace::getDisseminateResearchOther),
  FEATURED_WORKSPACE_CATEGORY(
      "featured_workspace_category", ReportingWorkspace::getFeaturedWorkspaceCategory),
  LAST_MODIFIED_TIME("last_modified_time", w -> toInsertRowString(w.getLastModifiedTime())),
  NAME("name", ReportingWorkspace::getName),
  RP_ADDITIONAL_NOTES("rp_additional_notes", ReportingWorkspace::getRpAdditionalNotes),
  RP_AIAN_RESEARCH_TYPE("rp_aian_research_type", ReportingWorkspace::getRpAianResearchType),
  RP_AIAN_RESEARCH_DETAILS(
      "rp_aian_research_details", ReportingWorkspace::getRpAianResearchDetails),
  RP_ANCESTRY("rp_ancestry", ReportingWorkspace::isRpAncestry),
  RP_ANTICIPATED_FINDINGS("rp_anticipated_findings", ReportingWorkspace::getRpAnticipatedFindings),
  RP_APPROVED("rp_approved", ReportingWorkspace::isRpApproved),
  RP_COMMERCIAL_PURPOSE("rp_commercial_purpose", ReportingWorkspace::isRpCommercialPurpose),
  RP_CONTROL_SET("rp_control_set", ReportingWorkspace::isRpControlSet),
  RP_DISEASE_FOCUSED_RESEARCH(
      "rp_disease_focused_research", ReportingWorkspace::isRpDiseaseFocusedResearch),
  RP_DISEASE_OF_FOCUS("rp_disease_of_focus", ReportingWorkspace::getRpDiseaseOfFocus),
  RP_DRUG_DEVELOPMENT("rp_drug_development", ReportingWorkspace::isRpDrugDevelopment),
  RP_EDUCATIONAL("rp_educational", ReportingWorkspace::isRpEducational),
  RP_ETHICS("rp_ethics", ReportingWorkspace::isRpEthics),
  RP_INTENDED_STUDY("rp_intended_study", ReportingWorkspace::getRpIntendedStudy),
  RP_METHODS_DEVELOPMENT("rp_methods_development", ReportingWorkspace::isRpMethodsDevelopment),
  RP_OTHER_POPULATION_DETAILS(
      "rp_other_population_details", ReportingWorkspace::getRpOtherPopulationDetails),
  RP_OTHER_PURPOSE("rp_other_purpose", ReportingWorkspace::isRpOtherPurpose),
  RP_OTHER_PURPOSE_DETAILS(
      "rp_other_purpose_details", ReportingWorkspace::getRpOtherPurposeDetails),
  RP_POPULATION_HEALTH("rp_population_health", ReportingWorkspace::isRpPopulationHealth),
  RP_REASON_FOR_ALL_OF_US("rp_reason_for_all_of_us", ReportingWorkspace::getRpReasonForAllOfUs),
  RP_REVIEW_REQUESTED("rp_review_requested", ReportingWorkspace::isRpReviewRequested),
  RP_SCIENTIFIC_APPROACH("rp_scientific_approach", ReportingWorkspace::getRpScientificApproach),
  RP_SOCIAL_BEHAVIORAL("rp_social_behavioral", ReportingWorkspace::isRpSocialBehavioral),
  RP_TIME_REQUESTED("rp_time_requested", w -> toInsertRowString(w.getRpTimeRequested())),
  WORKSPACE_ID("workspace_id", ReportingWorkspace::getWorkspaceId),
  WORKSPACE_NAMESPACE("workspace_namespace", ReportingWorkspace::getWorkspaceNamespace),
  FOCUS_ON_UNDER_REPRESENTED_POPULATIONS("focusOnUnderrepresentedPopulations", ReportingWorkspace::isFocusOnUnderrepresentedPopulations);

  private final String parameterName;
  private final Function<ReportingWorkspace, Object> rowToInsertValueFunction;

  WorkspaceColumnValueExtractor(
      String parameterName, Function<ReportingWorkspace, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
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
