package org.pmiops.workbench.audit.targetproperties

import java.util.function.Function
import org.pmiops.workbench.model.Workspace

internal data class WorkspaceTargetPropertyNameValue(val propertyName: String, val value: String)


// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class WorkspaceTargetProperty private constructor(val propertyName: String, private val extractor: Function<Workspace, String>) {
    NAME(propertyName = "name", extractor = Function<Workspace, String> { it.name }),
    INTENDED_STUDY(propertyName = "intended_study", extractor = Function<Workspace, String> { it.researchPurpose.intendedStudy }),
    CREATOR(propertyName = "creator", extractor = Function<Workspace, String> { it.creator }),
    ADDITIONAL_NOTES(propertyName = "additional_notes", extractor = Function<Workspace, String> { it.researchPurpose.additionalNotes }),
    ANTICIPATED_FINDINGS(
            propertyName = "anticipated_findings", extractor = Function<Workspace, String> { w -> w.getResearchPurpose().getAnticipatedFindings() }),
    DISEASE_OF_FOCUS(propertyName = "disease_of_focus", extractor = Function<Workspace, String> { w -> w.getResearchPurpose().getDiseaseOfFocus() }),
    REASON_FOR_ALL_OF_US(propertyName = "reason_for_all_of_us", extractor = Function<Workspace, String> { w -> w.getResearchPurpose().getReasonForAllOfUs() }),
    NAMESPACE(propertyName = "namespace", extractor = Function<Workspace, String> { it.namespace }),
    CDR_VERSION_ID(propertyName = "cdr_version_id", extractor = Function<Workspace, String> { it.cdrVersionId });

    fun extract(workspace: Workspace?): String? {
        return if (workspace == null) {
            null
        } else extractor.apply(workspace)
    }

    companion object {
        @JvmStatic
        fun getPropertyValuesByName(workspace: Workspace?): Map<String, String> {
            return values()
                    .filter { it.extract(workspace) != null }
                    .map { it.propertyName to it.extract(workspace)!! }
                    .toMap()
        }

        @JvmStatic
        fun getChangedValuesByName(
            previousWorkspace: Workspace?,
            newWorkspace: Workspace?
        ): Map<String, PreviousNewValuePair> {
            return values()
                    .map { it.propertyName to PreviousNewValuePair(previousValue = it.extract(previousWorkspace), newValue = it.extract(newWorkspace)) }
                    .filter { it.second.valueChanged() }
                    .toMap()
        }
    }
}
