package org.pmiops.workbench.audit.targetproperties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.pmiops.workbench.model.Workspace;

public enum WorkspaceTargetProperty {
  NAME("name", Workspace::getName),
  INTENDED_STUDY("intended_study", w -> w.getResearchPurpose().getIntendedStudy()),
  CREATOR("creator", Workspace::getCreator),
  ADDITIONAL_NOTES("additional_notes", w -> w.getResearchPurpose().getAdditionalNotes()),
  ANTICIPATED_FINDINGS(
      "anticipated_findings", w -> w.getResearchPurpose().getAnticipatedFindings()),
  DISEASE_OF_FOCUS("disease_of_focus", w -> w.getResearchPurpose().getDiseaseOfFocus()),
  REASON_FOR_ALL_OF_US("reason_for_all_of_us", w -> w.getResearchPurpose().getReasonForAllOfUs()),
  NAMESPACE("namespace", Workspace::getNamespace),
  CDR_VERSION_ID("cdr_version_id", Workspace::getCdrVersionId);

  private String propertyName;
  private Function<Workspace, String> extractor;

  WorkspaceTargetProperty(String propertyName,
      Function<Workspace, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String extract(Workspace workspace) {
    if (workspace == null) {
      return null;
    }
    return extractor.apply(workspace);
  }

  public static Map<String, String> getPropertyValuesByName(Workspace workspace) {
    if (workspace == null) {
      return Collections.emptyMap();
    }

    return Arrays.stream(WorkspaceTargetProperty.values())
        .map(
            prop ->
                new AbstractMap.SimpleImmutableEntry<>(
                    prop.getPropertyName(), prop.extract(workspace)))
        .filter(entry -> entry.getValue() != null)
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  public static Map<String, PreviousNewValuePair> getChangedValuesByName(Workspace previousWorkspace,
      Workspace newWorkspace) {
    ImmutableMap.Builder<String, PreviousNewValuePair> resultBuilder = new Builder<>();
    for (WorkspaceTargetProperty property : WorkspaceTargetProperty.values()) {
      final String previousValue = property.extract(previousWorkspace);
      final String newValue = property.extract(newWorkspace);
      if (previousValue != null || newValue != null) {
        if (previousValue != null && !previousValue.equals(newValue)
            || !newValue.equals(previousValue)) {
          resultBuilder.put(property.propertyName, PreviousNewValuePair.builder()
              .setNewValue(newValue)
              .setPreviousValue(previousValue)
              .build());
        }
      }
    }
    return resultBuilder.build();
  }
}
