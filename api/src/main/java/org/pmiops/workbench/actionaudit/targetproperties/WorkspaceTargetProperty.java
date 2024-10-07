package org.pmiops.workbench.actionaudit.targetproperties;

import jakarta.validation.constraints.NotNull;
import java.util.function.Function;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
public enum WorkspaceTargetProperty implements ModelBackedTargetProperty<Workspace> {
  ETAG("etag", Workspace::getEtag),
  NAME("name", Workspace::getName),
  TERRA_NAME("terra_name", Workspace::getTerraName),
  NAMESPACE("namespace", Workspace::getNamespace),
  CDR_VERSION_ID("cdr_version_id", Workspace::getCdrVersionId),
  CREATOR("creator", Workspace::getCreator),
  ACCESS_TIER_SHORT_NAME("access_tier_short_name", Workspace::getAccessTierShortName),

  // all fields below here relate to research purpose

  ADDITIONAL_NOTES(
      "additional_notes", workspace -> workspace.getResearchPurpose().getAdditionalNotes()),
  APPROVED("approved", rpStringOrNull(ResearchPurpose::isApproved)),
  ANCESTRY("ancestry", rpStringOrNull(ResearchPurpose::isAncestry)),
  ANTICIPATED_FINDINGS(
      "anticipated_findings", workspace -> workspace.getResearchPurpose().getAnticipatedFindings()),
  COMMERCIAL_PURPOSE("commercial_purpose", rpStringOrNull(ResearchPurpose::isCommercialPurpose)),
  CONTROL_SET("control_set", rpStringOrNull(ResearchPurpose::isControlSet)),
  DISEASE_FOCUSED_RESEARCH(
      "disease_focused_research", rpStringOrNull(ResearchPurpose::isDiseaseFocusedResearch)),
  DISEASE_OF_FOCUS(
      "disease_of_focus", workspace -> workspace.getResearchPurpose().getDiseaseOfFocus()),
  DRUG_DEVELOPMENT("drug_development", rpStringOrNull(ResearchPurpose::isDrugDevelopment)),
  EDUCATIONAL("educational", rpStringOrNull(ResearchPurpose::isEducational)),
  INTENDED_STUDY("intended_study", workspace -> workspace.getResearchPurpose().getIntendedStudy()),
  METHODS_DEVELOPMENT("methods_development", rpStringOrNull(ResearchPurpose::isMethodsDevelopment)),
  OTHER_POPULATION_DETAILS(
      "other_population_details",
      workspace -> workspace.getResearchPurpose().getOtherPopulationDetails()),
  POPULATION_DETAILS(
      "population_details",
      workspace -> {
        var populationDetails = workspace.getResearchPurpose().getPopulationDetails();
        if (populationDetails == null) return null;
        Iterable<String> pdIterable =
            () -> populationDetails.stream().map(SpecificPopulationEnum::toString).iterator();
        return String.join(", ", pdIterable);
      }),
  POPULATION_HEALTH("population_health", rpStringOrNull(ResearchPurpose::isPopulationHealth)),
  REASON_FOR_ALL_OF_US(
      "reason_for_all_of_us", workspace -> workspace.getResearchPurpose().getReasonForAllOfUs()),
  REVIEW_REQUESTED("review_requested", rpStringOrNull(ResearchPurpose::isReviewRequested)),
  SOCIAL_BEHAVIORAL("social_behavioral", rpStringOrNull(ResearchPurpose::isSocialBehavioral)),
  TIME_REQUESTED("time_requested", rpStringOrNull(ResearchPurpose::getTimeRequested)),
  TIME_REVIEWED("time_reviewed", rpStringOrNull(ResearchPurpose::getTimeReviewed));

  private final String propertyName;
  private final Function<Workspace, String> extractor;

  WorkspaceTargetProperty(String propertyName, Function<Workspace, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  // a specialized version of stringOrNull that extracts from a ResearchPurpose
  static Function<Workspace, String> rpStringOrNull(Function<ResearchPurpose, Object> rpExtractor) {
    return PropertyUtils.stringOrNull(
        workspace -> rpExtractor.apply(workspace.getResearchPurpose()));
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
