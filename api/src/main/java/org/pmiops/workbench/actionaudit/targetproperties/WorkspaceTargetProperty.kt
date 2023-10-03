package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Workspace

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class WorkspaceTargetProperty
constructor(
    override val propertyName: String,
    override val extractor: (Workspace) -> String?
) : ModelBackedTargetProperty<Workspace> {
    ETAG("etag",
            Workspace::getEtag),
    NAME("name",
            Workspace::getName),
    NAMESPACE("namespace",
            Workspace::getNamespace),
    CDR_VERSION_ID("cdr_version_id",
            Workspace::getCdrVersionId),
    CREATOR("creator",
            Workspace::getCreator),
    ACCESS_TIER_SHORT_NAME("access_tier_short_name",
        Workspace::getAccessTierShortName),
    ADDITIONAL_NOTES("additional_notes",
            { it.researchPurpose.additionalNotes }),
    APPROVED("approved",
            { it.researchPurpose.isApproved()?.toString() }),
    ANCESTRY("ancestry",
            { it.researchPurpose.isAncestry()?.toString() }),
    ANTICIPATED_FINDINGS("anticipated_findings",
            { it.researchPurpose.anticipatedFindings }),
    COMMERCIAL_PURPOSE("commercial_purpose",
            { it.researchPurpose.isCommercialPurpose()?.toString() }),
    CONTROL_SET("control_set",
            { it.researchPurpose.isControlSet()?.toString() }),
    DISEASE_FOCUSED_RESEARCH("disease_focused_research",
            { it.researchPurpose.isDiseaseFocusedResearch()?.toString() }),
    DISEASE_OF_FOCUS("disease_of_focus",
            { it.researchPurpose.diseaseOfFocus }),
    DRUG_DEVELOPMENT("drug_development",
            { it.researchPurpose.isDrugDevelopment()?.toString() }),
    EDUCATIONAL("educational",
            { it.researchPurpose.isEducational()?.toString() }),
    INTENDED_STUDY("intended_study",
            { it.researchPurpose.intendedStudy }),
    METHODS_DEVELOPMENT("methods_development",
            { it.researchPurpose.isMethodsDevelopment()?.toString() }),
    OTHER_POPULATION_DETAILS("other_population_details",
            { it.researchPurpose.otherPopulationDetails }),
    POPULATION_DETAILS("population_details",
            { it.researchPurpose.getPopulationDetails()
                ?.map(org.pmiops.workbench.model.SpecificPopulationEnum::toString)
                ?.joinToString { ", " } }),
    POPULATION_HEALTH("population_health",
            { it.researchPurpose.isPopulationHealth()?.toString() }),
    REASON_FOR_ALL_OF_US("reason_for_all_of_us",
            { it.researchPurpose.reasonForAllOfUs }),
    REVIEW_REQUESTED("review_requested",
            { it.researchPurpose.isReviewRequested()?.toString() }),
    SOCIAL_BEHAVIORAL("social_behavioral",
            { it.researchPurpose.isSocialBehavioral()?.toString() }),
    TIME_REQUESTED("time_requested",
            { it.researchPurpose.timeRequested?.toString() }),
    TIME_REVIEWED("time_reviewed",
            { it.researchPurpose.timeReviewed?.toString() }),
    PUBLISHED("published",
            { it.isPublished()?.toString() });
}
