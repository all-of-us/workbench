package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
public enum WorkspaceTargetProperty implements ModelBackedTargetProperty<Workspace> {
  ETAG("etag", Workspace::getEtag),
  NAME("name", Workspace::getName),
  NAMESPACE("namespace", Workspace::getNamespace),
  CDR_VERSION_ID("cdr_version_id", Workspace::getCdrVersionId),
  CREATOR("creator", Workspace::getCreator),
  ACCESS_TIER_SHORT_NAME("access_tier_short_name", Workspace::getAccessTierShortName),
  ADDITIONAL_NOTES(
      "additional_notes", workspace -> workspace.getResearchPurpose().getAdditionalNotes()),
  APPROVED("approved", rpToStringOrNull(ResearchPurpose::isApproved)),
  ANCESTRY("ancestry", rpToStringOrNull(ResearchPurpose::isAncestry)),
  ANTICIPATED_FINDINGS(
      "anticipated_findings", workspace -> workspace.getResearchPurpose().getAnticipatedFindings()),
  COMMERCIAL_PURPOSE("commercial_purpose", rpToStringOrNull(ResearchPurpose::isCommercialPurpose)),
  CONTROL_SET("control_set", rpToStringOrNull(ResearchPurpose::isControlSet)),
  DISEASE_FOCUSED_RESEARCH(
      "disease_focused_research", rpToStringOrNull(ResearchPurpose::isDiseaseFocusedResearch)),
  DISEASE_OF_FOCUS(
      "disease_of_focus", workspace -> workspace.getResearchPurpose().getDiseaseOfFocus()),
  DRUG_DEVELOPMENT("drug_development", rpToStringOrNull(ResearchPurpose::isDrugDevelopment)),
  EDUCATIONAL("educational", rpToStringOrNull(ResearchPurpose::isEducational)),
  INTENDED_STUDY("intended_study", workspace -> workspace.getResearchPurpose().getIntendedStudy()),
  METHODS_DEVELOPMENT(
      "methods_development", rpToStringOrNull(ResearchPurpose::isMethodsDevelopment)),
  OTHER_POPULATION_DETAILS(
      "other_population_details",
      workspace -> workspace.getResearchPurpose().getOtherPopulationDetails()),
  POPULATION_DETAILS(
      "population_details",
      workspace -> {
        var populationDetails = workspace.getResearchPurpose().getPopulationDetails();
        if (populationDetails == null) return null;

        var pdStrings = populationDetails.stream().map(SpecificPopulationEnum::toString).toList();
        return String.join(", ", pdStrings);
      }),
  POPULATION_HEALTH("population_health", rpToStringOrNull(ResearchPurpose::isPopulationHealth)),
  REASON_FOR_ALL_OF_US(
      "reason_for_all_of_us", workspace -> workspace.getResearchPurpose().getReasonForAllOfUs()),
  REVIEW_REQUESTED("review_requested", rpToStringOrNull(ResearchPurpose::isReviewRequested)),
  SOCIAL_BEHAVIORAL("social_behavioral", rpToStringOrNull(ResearchPurpose::isSocialBehavioral)),
  TIME_REQUESTED("time_requested", rpToStringOrNull(ResearchPurpose::getTimeRequested)),
  TIME_REVIEWED("time_reviewed", rpToStringOrNull(ResearchPurpose::getTimeReviewed)),
  PUBLISHED("published", workspace -> PropertyUtils.toStringOrNull(workspace.isPublished()));

  private final String propertyName;
  private final Function<Workspace, String> extractor;

  WorkspaceTargetProperty(String propertyName, Function<Workspace, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  static Function<Workspace, String> rpToStringOrNull(
      Function<ResearchPurpose, Object> rpExtractor) {
    return workspace ->
        PropertyUtils.toStringOrNull(rpExtractor.apply(workspace.getResearchPurpose()));
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @NotNull
  @Override
  public Function<Workspace, String> getExtractor() {
    return extractor;
  }
}
