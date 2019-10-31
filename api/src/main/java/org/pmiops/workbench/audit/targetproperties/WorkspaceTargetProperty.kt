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

internal data class WorkspaceTargetPropertyNameValue(val propertyName: String, val value: String)
enum class WorkspaceTargetProperty private constructor(val propertyName: String, private val extractor: Function<Workspace, String>) {
    NAME("name", Function<Workspace, String> { it.name }),
    INTENDED_STUDY("intended_study", Function<Workspace, String> { it.researchPurpose.intendedStudy }),
    CREATOR("creator", Function<Workspace, String> { it.creator }),
    ADDITIONAL_NOTES("additional_notes", Function<Workspace, String> { it.researchPurpose.additionalNotes }),
    ANTICIPATED_FINDINGS(
            "anticipated_findings", Function<Workspace, String> { w -> w.getResearchPurpose().getAnticipatedFindings() }),
    DISEASE_OF_FOCUS("disease_of_focus", Function<Workspace, String> { w -> w.getResearchPurpose().getDiseaseOfFocus() }),
    REASON_FOR_ALL_OF_US("reason_for_all_of_us", Function<Workspace, String> { w -> w.getResearchPurpose().getReasonForAllOfUs() }),
    NAMESPACE("namespace", Function<Workspace, String> { it.namespace }),
    CDR_VERSION_ID("cdr_version_id", Function<Workspace, String> { it.cdrVersionId });

    fun extract(workspace: Workspace?): String? {
        return if (workspace == null) {
            null
        } else extractor.apply(workspace)
    }

    companion object {

        fun getPropertyValuesByName(workspace: Workspace): Map<String, String> {
            return values()
                    .filter { it.extract(workspace) != null }
                    .map { it.propertyName to it.extract(workspace)!! }
                    .toMap()
        }


        fun getChangedValuesByName(
                previousWorkspace: Workspace, newWorkspace: Workspace): Map<String, PreviousNewValuePair> {
            return values()
                    .map({ it.propertyName to PreviousNewValuePair(previousValue = it.extract(previousWorkspace), newValue = it.extract(newWorkspace)) })
                    .filter({ it.second.valueChanged() })
                    .toMap()
        }
    }
}
