package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;
import org.pmiops.workbench.model.BqDtoWorkspace;

public enum WorkspaceParameterColumn implements QueryParameterColumn<BqDtoWorkspace> {
  ACTIVE_STATUS(
      "active_status",
      BqDtoWorkspace::getActiveStatus,
      w -> QueryParameterValue.int64(w.getActiveStatus())),
  BILLING_ACCOUNT_TYPE(
      "billing_account_type",
      BqDtoWorkspace::getBillingAccountType,
      w -> enumToQpv(w.getBillingAccountType())),
  BILLING_STATUS(
      "billing_status", BqDtoWorkspace::getBillingStatus, w -> enumToQpv(w.getBillingStatus())),
  CDR_VERSION_ID(
      "cdr_version_id",
      BqDtoWorkspace::getCdrVersionId,
      w -> QueryParameterValue.int64(w.getCdrVersionId())),
  CREATION_TIME(
      "creation_time",
      w -> toInsertRowString(w.getCreationTime()),
      w -> toTimestampQpv(w.getCreationTime())),
  CREATOR_ID(
      "creator_id", BqDtoWorkspace::getCreatorId, w -> QueryParameterValue.int64(w.getCreatorId())),
  DATA_ACCESS_LEVEL(
      "data_access_level",
      BqDtoWorkspace::getDataAccessLevel,
      w -> enumToQpv(w.getDataAccessLevel())),
  DISSEMINATE_RESEARCH_OTHER(
      "disseminate_research_other",
      BqDtoWorkspace::getDisseminateResearchOther,
      w -> QueryParameterValue.string(w.getDisseminateResearchOther())),
  FIRECLOUD_NAME(
      "firecloud_name",
      BqDtoWorkspace::getFirecloudName,
      w -> QueryParameterValue.string(w.getFirecloudName())),
  FIRECLOUD_UUID(
      "firecloud_uuid",
      BqDtoWorkspace::getFirecloudUuid,
      w -> QueryParameterValue.string(w.getFirecloudUuid())),
  LAST_ACCESSED_TIME(
      "last_accessed_time",
      w -> toInsertRowString(w.getLastAccessedTime()),
      w -> toTimestampQpv(w.getLastAccessedTime())),
  LAST_MODIFIED_TIME(
      "last_modified_time",
      w -> toInsertRowString(w.getLastModifiedTime()),
      w -> toTimestampQpv(w.getLastModifiedTime())),
  NAME("name", BqDtoWorkspace::getName, w -> QueryParameterValue.string(w.getName())),
  NEEDS_RP_REVIEW_PROMPT(
      "needs_rp_review_prompt",
      BqDtoWorkspace::getNeedsRpReviewPrompt,
      w -> QueryParameterValue.int64(w.getNeedsRpReviewPrompt())),
  PUBLISHED(
      "published", BqDtoWorkspace::getPublished, w -> QueryParameterValue.bool(w.getPublished())),
  RP_ADDITIONAL_NOTES(
      "rp_additional_notes",
      BqDtoWorkspace::getRpAdditionalNotes,
      w -> QueryParameterValue.string(w.getRpAdditionalNotes())),
  RP_ANCESTRY(
      "rp_ancestry",
      BqDtoWorkspace::getRpAncestry,
      w -> QueryParameterValue.bool(w.getRpAncestry())),
  RP_ANTICIPATED_FINDINGS(
      "rp_anticipated_findings",
      BqDtoWorkspace::getRpAnticipatedFindings,
      w -> QueryParameterValue.string(w.getRpAnticipatedFindings())),
  RP_APPROVED(
      "rp_approved",
      BqDtoWorkspace::getRpApproved,
      w -> QueryParameterValue.bool(w.getRpApproved())),
  RP_COMMERCIAL_PURPOSE(
      "rp_commercial_purpose",
      BqDtoWorkspace::getRpCommercialPurpose,
      w -> QueryParameterValue.bool(w.getRpCommercialPurpose())),
  RP_CONTROL_SET(
      "rp_control_set",
      BqDtoWorkspace::getRpControlSet,
      w -> QueryParameterValue.bool(w.getRpControlSet())),
  RP_DISEASE_FOCUSED_RESEARCH(
      "rp_disease_focused_research",
      BqDtoWorkspace::getRpDiseaseFocusedResearch,
      w -> QueryParameterValue.bool(w.getRpDiseaseFocusedResearch())),
  RP_DISEASE_OF_FOCUS(
      "rp_disease_of_focus",
      BqDtoWorkspace::getRpDiseaseOfFocus,
      w -> QueryParameterValue.string(w.getRpDiseaseOfFocus())),
  RP_DRUG_DEVELOPMENT(
      "rp_drug_development",
      BqDtoWorkspace::getRpDrugDevelopment,
      w -> QueryParameterValue.bool(w.getRpDrugDevelopment())),
  RP_EDUCATIONAL(
      "rp_educational",
      BqDtoWorkspace::getRpEducational,
      w -> QueryParameterValue.bool(w.getRpEducational())),
  RP_ETHICS(
      "rp_ethics", BqDtoWorkspace::getRpEthics, w -> QueryParameterValue.bool(w.getRpEthics())),
  RP_INTENDED_STUDY(
      "rp_intended_study",
      BqDtoWorkspace::getRpIntendedStudy,
      w -> QueryParameterValue.string(w.getRpIntendedStudy())),
  RP_METHODS_DEVELOPMENT(
      "rp_methods_development",
      BqDtoWorkspace::getRpMethodsDevelopment,
      w -> QueryParameterValue.bool(w.getRpMethodsDevelopment())),
  RP_OTHER_POPULATION_DETAILS(
      "rp_other_population_details",
      BqDtoWorkspace::getRpOtherPopulationDetails,
      w -> QueryParameterValue.string(w.getRpOtherPopulationDetails())),
  RP_OTHER_PURPOSE(
      "rp_other_purpose",
      BqDtoWorkspace::getRpOtherPurpose,
      w -> QueryParameterValue.bool(w.getRpOtherPurpose())),
  RP_OTHER_PURPOSE_DETAILS(
      "rp_other_purpose_details",
      BqDtoWorkspace::getRpOtherPurposeDetails,
      w -> QueryParameterValue.string(w.getRpOtherPurposeDetails())),
  RP_POPULATION_HEALTH(
      "rp_population_health",
      BqDtoWorkspace::getRpPopulationHealth,
      w -> QueryParameterValue.bool(w.getRpPopulationHealth())),
  RP_REASON_FOR_ALL_OF_US(
      "rp_reason_for_all_of_us",
      BqDtoWorkspace::getRpReasonForAllOfUs,
      w -> QueryParameterValue.string(w.getRpReasonForAllOfUs())),
  RP_REVIEW_REQUESTED(
      "rp_review_requested",
      BqDtoWorkspace::getRpReviewRequested,
      w -> QueryParameterValue.bool(w.getRpReviewRequested())),
  RP_SCIENTIFIC_APPROACH(
      "rp_scientific_approach",
      BqDtoWorkspace::getRpScientificApproach,
      w -> QueryParameterValue.string(w.getRpScientificApproach())),
  RP_SOCIAL_BEHAVIORAL(
      "rp_social_behavioral",
      BqDtoWorkspace::getRpSocialBehavioral,
      w -> QueryParameterValue.bool(w.getRpSocialBehavioral())),
  RP_TIME_REQUESTED(
      "rp_time_requested",
      w -> toInsertRowString(w.getRpTimeRequested()),
      w -> toTimestampQpv(w.getRpTimeRequested())),
  WORKSPACE_ID(
      "workspace_id",
      BqDtoWorkspace::getWorkspaceId,
      w -> QueryParameterValue.int64(w.getWorkspaceId())),
  WORKSPACE_NAMESPACE(
      "workspace_namespace",
      BqDtoWorkspace::getWorkspaceNamespace,
      w -> QueryParameterValue.string(w.getWorkspaceNamespace()));
  private final String parameterName;
  private final Function<BqDtoWorkspace, Object> rowToInsertValueFunction;
  private final Function<BqDtoWorkspace, QueryParameterValue> parameterValueFunction;

  WorkspaceParameterColumn(
      String parameterName,
      Function<BqDtoWorkspace, Object> rowToInsertValueFunction,
      Function<BqDtoWorkspace, QueryParameterValue> parameterValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
    this.parameterValueFunction = parameterValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<BqDtoWorkspace, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }

  @Override
  public Function<BqDtoWorkspace, QueryParameterValue> getQueryParameterValueFunction() {
    return parameterValueFunction;
  }
}
