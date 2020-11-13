package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryParameterValue
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues
import org.pmiops.workbench.model.BillingAccountType
import org.pmiops.workbench.model.BillingStatus
import org.pmiops.workbench.model.ReportingWorkspace
import java.util.function.Function

/*
* Column data and metadata convertors for BigQuery workspace table in reporting dataset.
*/
enum class WorkspaceColumnValueExtractor(
        override val parameterName: String,
        override val rowToInsertValueFunction: Function<ReportingWorkspace, Any?>,
        override val queryParameterValueFunction: Function<ReportingWorkspace, QueryParameterValue?>) : ColumnValueExtractor<ReportingWorkspace?> {
    BILLING_ACCOUNT_TYPE(
            "billing_account_type",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.enumToString<BillingAccountType>(w.billingAccountType) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.enumToQpv<BillingAccountType>(w.billingAccountType) }),
    BILLING_STATUS(
            "billing_status",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.enumToString<BillingStatus>(w.billingStatus) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.enumToQpv<BillingStatus>(w.billingStatus) }),
    CDR_VERSION_ID(
            "cdr_version_id", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.cdrVersionId }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.int64(w.cdrVersionId) }),
    CREATION_TIME(
            "creation_time",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.toInsertRowString(w.creationTime) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.toTimestampQpv(w.creationTime) }),
    CREATOR_ID("creator_id", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.creatorId }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.int64(w.creatorId) }), DISSEMINATE_RESEARCH_OTHER(
            "disseminate_research_other", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.disseminateResearchOther },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.disseminateResearchOther) }),
    LAST_ACCESSED_TIME(
            "last_accessed_time",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.toInsertRowString(w.lastAccessedTime) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.toTimestampQpv(w.lastAccessedTime) }),
    LAST_MODIFIED_TIME(
            "last_modified_time",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.toInsertRowString(w.lastModifiedTime) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.toTimestampQpv(w.lastModifiedTime) }),
    NAME("name", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.name }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.name) }), NEEDS_RP_REVIEW_PROMPT(
            "needs_rp_review_prompt", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.needsRpReviewPrompt },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.int64(w.needsRpReviewPrompt) }),
    PUBLISHED("published", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.published }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.published) }), RP_ADDITIONAL_NOTES(
            "rp_additional_notes", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpAdditionalNotes },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpAdditionalNotes) }),
    RP_ANCESTRY("rp_ancestry", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpAncestry }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpAncestry) }), RP_ANTICIPATED_FINDINGS(
            "rp_anticipated_findings", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpAnticipatedFindings },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpAnticipatedFindings) }),
    RP_APPROVED("rp_approved", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpApproved }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpApproved) }), RP_COMMERCIAL_PURPOSE(
            "rp_commercial_purpose", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpCommercialPurpose },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpCommercialPurpose) }),
    RP_CONTROL_SET(
            "rp_control_set", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpControlSet }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpControlSet) }),
    RP_DISEASE_FOCUSED_RESEARCH(
            "rp_disease_focused_research", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpDiseaseFocusedResearch },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpDiseaseFocusedResearch) }),
    RP_DISEASE_OF_FOCUS(
            "rp_disease_of_focus", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpDiseaseOfFocus },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpDiseaseOfFocus) }),
    RP_DRUG_DEVELOPMENT(
            "rp_drug_development", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpDrugDevelopment },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpDrugDevelopment) }),
    RP_EDUCATIONAL(
            "rp_educational", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpEducational }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpEducational) }),
    RP_ETHICS("rp_ethics", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpEthics }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpEthics) }), RP_INTENDED_STUDY(
            "rp_intended_study", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpIntendedStudy },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpIntendedStudy) }),
    RP_METHODS_DEVELOPMENT(
            "rp_methods_development", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpMethodsDevelopment },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpMethodsDevelopment) }),
    RP_OTHER_POPULATION_DETAILS(
            "rp_other_population_details", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpOtherPopulationDetails },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpOtherPopulationDetails) }),
    RP_OTHER_PURPOSE(
            "rp_other_purpose", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpOtherPurpose }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpOtherPurpose) }),
    RP_OTHER_PURPOSE_DETAILS(
            "rp_other_purpose_details", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpOtherPurposeDetails },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpOtherPurposeDetails) }),
    RP_POPULATION_HEALTH(
            "rp_population_health", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpPopulationHealth },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpPopulationHealth) }),
    RP_REASON_FOR_ALL_OF_US(
            "rp_reason_for_all_of_us", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpReasonForAllOfUs },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpReasonForAllOfUs) }),
    RP_REVIEW_REQUESTED(
            "rp_review_requested", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpReviewRequested },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpReviewRequested) }),
    RP_SCIENTIFIC_APPROACH(
            "rp_scientific_approach", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpScientificApproach },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.string(w.rpScientificApproach) }),
    RP_SOCIAL_BEHAVIORAL(
            "rp_social_behavioral", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.rpSocialBehavioral },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.bool(w.rpSocialBehavioral) }),
    RP_TIME_REQUESTED(
            "rp_time_requested",
            Function<ReportingWorkspace, Any?> { w: ReportingWorkspace -> QueryParameterValues.toInsertRowString(w.rpTimeRequested) },
            Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValues.toTimestampQpv(w.rpTimeRequested) }),
    WORKSPACE_ID("workspace_id", Function<ReportingWorkspace, Any?> { obj: ReportingWorkspace -> obj.workspaceId }, Function<ReportingWorkspace, QueryParameterValue?> { w: ReportingWorkspace -> QueryParameterValue.int64(w.workspaceId) });

    companion object {
        const val TABLE_NAME = "workspace"
    }
}
