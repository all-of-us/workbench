package org.pmiops.workbench.audit.targetproperties

import org.pmiops.workbench.model.Workspace

// N.B. entries will rarely be referred to by name, but are accessed via values(). I.e. they are
// not safe to remove, even if an IDE indicates otherwise.
enum class WorkspaceTargetProperty(val propertyName: String, private val extractor: (Workspace) -> String?) {
    NAME(propertyName = "name", extractor = Workspace::getName),
    INTENDED_STUDY(propertyName = "intended_study", extractor = { it.researchPurpose.intendedStudy }),
    CREATOR(propertyName = "creator", extractor = Workspace::getCreator),
    ADDITIONAL_NOTES(propertyName = "additional_notes", extractor = { it.researchPurpose.additionalNotes }),
    ANTICIPATED_FINDINGS(
            propertyName = "anticipated_findings", extractor = { w -> w.researchPurpose.anticipatedFindings }),
    DISEASE_OF_FOCUS(propertyName = "disease_of_focus", extractor = { w -> w.researchPurpose.diseaseOfFocus }),
    REASON_FOR_ALL_OF_US(propertyName = "reason_for_all_of_us", extractor = { w -> w.researchPurpose.reasonForAllOfUs }),
    NAMESPACE(propertyName = "namespace", extractor = Workspace::getNamespace),
    CDR_VERSION_ID(propertyName = "cdr_version_id", extractor = Workspace::getCdrVersionId) ;

    fun extract(workspace: Workspace?): String? {
        return if (workspace == null) {
            null
        } else extractor.invoke(workspace)
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
                    .filter { it.second.valueChanged }
                    .toMap()
        }
    }
}
