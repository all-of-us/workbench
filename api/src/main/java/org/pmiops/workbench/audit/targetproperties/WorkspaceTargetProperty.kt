package org.pmiops.workbench.audit.targetproperties

import org.pmiops.workbench.model.Workspace

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class WorkspaceTargetProperty(val propertyName: String, private val extractor: (Workspace) -> String?) {
    NAME("name", Workspace::getName),
    INTENDED_STUDY("intended_study", { it.researchPurpose.intendedStudy }),
    CREATOR("creator", Workspace::getCreator),
    ADDITIONAL_NOTES("additional_notes", { it.researchPurpose.additionalNotes }),
    ANTICIPATED_FINDINGS(
            "anticipated_findings", { it.researchPurpose.anticipatedFindings }),
    DISEASE_OF_FOCUS("disease_of_focus", { it.researchPurpose.diseaseOfFocus }),
    REASON_FOR_ALL_OF_US("reason_for_all_of_us", { it.researchPurpose.reasonForAllOfUs }),
    NAMESPACE("namespace", Workspace::getNamespace),
    CDR_VERSION_ID("cdr_version_id", Workspace::getCdrVersionId) ;

    fun extract(workspace: Workspace?): String? {
        return if (workspace == null) {
            null
        } else extractor.invoke(workspace)
    }

    companion object {
        @JvmStatic
        fun getPropertyValuesByName(workspace: Workspace): Map<String, String> {
            return values()
                    .filter { it.extract(workspace) != null }
                    .map { it.propertyName to it.extract(workspace)!! }
                    .toMap()
        }

        @JvmStatic
        fun getChangedValuesByName(
            previousWorkspace: Workspace,
            newWorkspace: Workspace
        ): Map<String, PreviousNewValuePair> {
            return values()
                    .map { it.propertyName to PreviousNewValuePair(previousValue = it.extract(previousWorkspace), newValue = it.extract(newWorkspace)) }
                    .filter { it.second.valueChanged }
                    .toMap()
        }
    }
}
