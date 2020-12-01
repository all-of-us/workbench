package org.pmiops.workbench.reporting.insertion;

import static com.google.cloud.bigquery.QueryParameterValue.bool;
import static com.google.cloud.bigquery.QueryParameterValue.int64;
import static com.google.cloud.bigquery.QueryParameterValue.string;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspace;

/*
 * Column data and metadata convertors for BigQuery workspace table in reporting dataset.
 */
public enum WorkspaceColumnValueExtractor implements ColumnValueExtractor<ReportingWorkspace> {
  BILLING_ACCOUNT_TYPE(
      "billing_account_type",
      w -> enumToString(w.getBillingAccountType()),
      w -> enumToQpv(w.getBillingAccountType())),
  BILLING_STATUS(
      "billing_status",
      w -> enumToString(w.getBillingStatus()),
      w -> enumToQpv(w.getBillingStatus())),
  CDR_VERSION_ID(
      "cdr_version_id", ReportingWorkspace::getCdrVersionId, w -> int64(w.getCdrVersionId())),
  CREATION_TIME(
      "creation_time",
      w -> toInsertRowString(w.getCreationTime()),
      w -> toTimestampQpv(w.getCreationTime())),
  CREATOR_ID("creator_id", ReportingWorkspace::getCreatorId, w -> int64(w.getCreatorId())),
  DISSEMINATE_RESEARCH_OTHER(
      "disseminate_research_other",
      ReportingWorkspace::getDisseminateResearchOther,
      w -> string(w.getDisseminateResearchOther())),
  LAST_ACCESSED_TIME(
      "last_accessed_time",
      w -> toInsertRowString(w.getLastAccessedTime()),
      w -> toTimestampQpv(w.getLastAccessedTime())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      w -> toInsertRowString(w.getLastModifiedTime()),
      w -> toTimestampQpv(w.getLastModifiedTime())),
  NAME("name", ReportingWorkspace::getName, w -> string(w.getName())),
  NEEDS_RP_REVIEW_PROMPT(
      "needs_rp_review_prompt",
      ReportingWorkspace::getNeedsRpReviewPrompt,
      w -> int64(w.getNeedsRpReviewPrompt())),
  PUBLISHED("published", ReportingWorkspace::getPublished, w -> bool(w.getPublished())),
  RP_ADDITIONAL_NOTES(
      "rp_additional_notes",
      ReportingWorkspace::getRpAdditionalNotes,
      w -> string(w.getRpAdditionalNotes())),
  RP_ANCESTRY("rp_ancestry", ReportingWorkspace::getRpAncestry, w -> bool(w.getRpAncestry())),
  RP_ANTICIPATED_FINDINGS(
      "rp_anticipated_findings",
      ReportingWorkspace::getRpAnticipatedFindings,
      w -> string(w.getRpAnticipatedFindings())),
  RP_APPROVED("rp_approved", ReportingWorkspace::getRpApproved, w -> bool(w.getRpApproved())),
  RP_COMMERCIAL_PURPOSE(
      "rp_commercial_purpose",
      ReportingWorkspace::getRpCommercialPurpose,
      w -> bool(w.getRpCommercialPurpose())),
  RP_CONTROL_SET(
      "rp_control_set", ReportingWorkspace::getRpControlSet, w -> bool(w.getRpControlSet())),
  RP_DISEASE_FOCUSED_RESEARCH(
      "rp_disease_focused_research",
      ReportingWorkspace::getRpDiseaseFocusedResearch,
      w -> bool(w.getRpDiseaseFocusedResearch())),
  RP_DISEASE_OF_FOCUS(
      "rp_disease_of_focus",
      ReportingWorkspace::getRpDiseaseOfFocus,
      w -> string(w.getRpDiseaseOfFocus())),
  RP_DRUG_DEVELOPMENT(
      "rp_drug_development",
      ReportingWorkspace::getRpDrugDevelopment,
      w -> bool(w.getRpDrugDevelopment())),
  RP_EDUCATIONAL(
      "rp_educational", ReportingWorkspace::getRpEducational, w -> bool(w.getRpEducational())),
  RP_ETHICS("rp_ethics", ReportingWorkspace::getRpEthics, w -> bool(w.getRpEthics())),
  RP_INTENDED_STUDY(
      "rp_intended_study",
      ReportingWorkspace::getRpIntendedStudy,
      w -> string(w.getRpIntendedStudy())),
  RP_METHODS_DEVELOPMENT(
      "rp_methods_development",
      ReportingWorkspace::getRpMethodsDevelopment,
      w -> bool(w.getRpMethodsDevelopment())),
  RP_OTHER_POPULATION_DETAILS(
      "rp_other_population_details",
      ReportingWorkspace::getRpOtherPopulationDetails,
      w -> string(w.getRpOtherPopulationDetails())),
  RP_OTHER_PURPOSE(
      "rp_other_purpose", ReportingWorkspace::getRpOtherPurpose, w -> bool(w.getRpOtherPurpose())),
  RP_OTHER_PURPOSE_DETAILS(
      "rp_other_purpose_details",
      ReportingWorkspace::getRpOtherPurposeDetails,
      w -> string(w.getRpOtherPurposeDetails())),
  RP_POPULATION_HEALTH(
      "rp_population_health",
      ReportingWorkspace::getRpPopulationHealth,
      w -> bool(w.getRpPopulationHealth())),
  RP_REASON_FOR_ALL_OF_US(
      "rp_reason_for_all_of_us",
      ReportingWorkspace::getRpReasonForAllOfUs,
      w -> string(w.getRpReasonForAllOfUs())),
  RP_REVIEW_REQUESTED(
      "rp_review_requested",
      ReportingWorkspace::getRpReviewRequested,
      w -> bool(w.getRpReviewRequested())),
  RP_SCIENTIFIC_APPROACH(
      "rp_scientific_approach",
      ReportingWorkspace::getRpScientificApproach,
      w -> string(w.getRpScientificApproach())),
  RP_SOCIAL_BEHAVIORAL(
      "rp_social_behavioral",
      ReportingWorkspace::getRpSocialBehavioral,
      w -> bool(w.getRpSocialBehavioral())),
  RP_TIME_REQUESTED(
      "rp_time_requested",
      w -> toInsertRowString(w.getRpTimeRequested()),
      w -> toTimestampQpv(w.getRpTimeRequested())),
  WORKSPACE_ID("workspace_id", ReportingWorkspace::getWorkspaceId, w -> int64(w.getWorkspaceId()));

  private static final String TABLE_NAME = "workspace";
  private final String parameterName;
  private final Function<ReportingWorkspace, Object> rowToInsertValueFunction;
  private final Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction;

  WorkspaceColumnValueExtractor(
      String parameterName,
      Function<ReportingWorkspace, Object> rowToInsertValueFunction,
      Function<ReportingWorkspace, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
    this.parameterValueFunction = parameterValueFunction;
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

  @Override
  public Function<ReportingWorkspace, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
