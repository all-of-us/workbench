package org.pmiops.workbench.audit.targetproperties

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMap.Builder
import java.util.AbstractMap
import java.util.Arrays
import java.util.Collections
import kotlin.collections.Map.Entry
import java.util.Optional
import java.util.function.Function
import org.pmiops.workbench.model.Workspace

enum class WorkspaceTargetProperty private constructor(val propertyName: String, private val extractor: Function<Workspace, String>) {
    NAME("name", Function<Workspace, String> { Workspace.getName() }),
    INTENDED_STUDY("intended_study", { w -> w.getResearchPurpose().getIntendedStudy() }),
    CREATOR("creator", Function<Workspace, String> { Workspace.getCreator() }),
    ADDITIONAL_NOTES("additional_notes", { w -> w.getResearchPurpose().getAdditionalNotes() }),
    ANTICIPATED_FINDINGS(
            "anticipated_findings", { w -> w.getResearchPurpose().getAnticipatedFindings() }),
    DISEASE_OF_FOCUS("disease_of_focus", { w -> w.getResearchPurpose().getDiseaseOfFocus() }),
    REASON_FOR_ALL_OF_US("reason_for_all_of_us", { w -> w.getResearchPurpose().getReasonForAllOfUs() }),
    NAMESPACE("namespace", Function<Workspace, String> { Workspace.getNamespace() }),
    CDR_VERSION_ID("cdr_version_id", Function<Workspace, String> { Workspace.getCdrVersionId() });

    fun extract(workspace: Workspace?): String? {
        return if (workspace == null) {
            null
        } else extractor.apply(workspace)
    }

    companion object {

        fun getPropertyValuesByName(workspace: Workspace?): Map<String, String> {
            return if (workspace == null) {
                emptyMap()
            } else Arrays.stream(WorkspaceTargetProperty.values())
                    .map<SimpleImmutableEntry<String, String>> { prop ->
                        AbstractMap.SimpleImmutableEntry<String, String>(
                                prop.propertyName, prop.extract(workspace))
                    }
                    .filter { entry -> entry.value != null }
                    .collect<ImmutableMap<String, String>, Any>(ImmutableMap.toImmutableMap<SimpleImmutableEntry<String, String>, String, String>({ it.key }, { it.value }))

        }

        fun getChangedValuesByName(
                previousWorkspace: Workspace, newWorkspace: Workspace): Map<String, PreviousNewValuePair> {
            val resultBuilder = Builder<String, PreviousNewValuePair>()
            for (property in WorkspaceTargetProperty.values()) {
                val previousValue = Optional.ofNullable(property.extract(previousWorkspace))
                val newValue = Optional.ofNullable(property.extract(newWorkspace))

                // put entry in the map for change, delete, or add on the property
                if (previousValue.isPresent && previousValue != newValue || newValue.isPresent && newValue != previousValue) {
                    resultBuilder.put(
                            property.propertyName,
                            PreviousNewValuePair.builder()
                                    .setNewValue(newValue.orElse(null))
                                    .setPreviousValue(previousValue.orElse(null))
                                    .build())
                }
            }
            return resultBuilder.build()
        }
    }
}
