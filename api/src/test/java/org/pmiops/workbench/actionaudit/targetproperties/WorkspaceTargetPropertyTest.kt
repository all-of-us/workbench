package org.pmiops.workbench.actionaudit.targetproperties

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Workspace
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class WorkspaceTargetPropertyTest {

    private var workspace1: Workspace? = null
    private var workspace2: Workspace? = null
    private var emptyWorkspace: Workspace? = null

    @Before
    fun setUp() {
        val researchPurpose1 = ResearchPurpose()
                .apply { intendedStudy = "stubbed toes" }
                .apply { additionalNotes = "I really like the cloud." }

        val researchPurpose2 = ResearchPurpose()
                .apply { intendedStudy = "broken dreams" }
                .apply { additionalNotes = "I changed my mind" }
                .apply { anticipatedFindings = "a 4-leaf clover" }

        val now = System.currentTimeMillis()

        workspace1 = Workspace()
            .apply { name = "Workspace 1" }
            .apply { id = "fc-id-1" }
            .apply { namespace = "aou-rw-local1-c4be869a" }
            .apply { creator = "user@fake-research-aou.org" }
            .apply { cdrVersionId = "1" }
            .apply { researchPurpose = researchPurpose1 }
            .apply { creationTime = now }
            .apply { lastModifiedTime = now }
            .apply { etag = "etag_1" }
            .apply { dataAccessLevel = DataAccessLevel.REGISTERED }
            .apply { published = false }

        workspace2 = Workspace()
            .apply { name = "Workspace 2" }
            .apply { id = "fc-id-1" }
            .apply { namespace = "aou-rw-local1-c4be869a" }
            .apply { creator = "user@fake-research-aou.org" }
            .apply { cdrVersionId = "33" }
            .apply { researchPurpose = researchPurpose2 }
            .apply { creationTime = now }
            .apply { lastModifiedTime = now }
            .apply { etag = "etag_1" }
            .apply { dataAccessLevel = DataAccessLevel.REGISTERED }
            .apply { published = false }

        emptyWorkspace = Workspace().apply { researchPurpose = ResearchPurpose() }
    }

    @Test
    fun testExtractsStringPropertiesFromWorkspace() {
        val propertiesByName = TargetPropertyExtractor.getPropertyValuesByName(
                WorkspaceTargetProperty.values(),
                workspace1!!)

        assertThat(propertiesByName).hasSize(6)
        assertThat(propertiesByName[WorkspaceTargetProperty.INTENDED_STUDY.propertyName])
                .isEqualTo("stubbed toes")
        assertThat(propertiesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName])
                .isEqualTo("1")
        assertThat(propertiesByName[WorkspaceTargetProperty.REASON_FOR_ALL_OF_US.propertyName])
                .isNull()
    }

    @Test
    fun testEmptyWorkspaceGivesEmptyMap() {
        assertThat(TargetPropertyExtractor.getPropertyValuesByName(
                WorkspaceTargetProperty.values(), emptyWorkspace!!))
                .isEmpty()
    }

    @Test
    fun testMapsChanges() {
        val changesByPropertyName = TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(), workspace1!!, workspace2!!)

        assertThat(changesByPropertyName).hasSize(5)

        assertThat(
                changesByPropertyName[WorkspaceTargetProperty.ADDITIONAL_NOTES.propertyName]
                        ?.previousValue)
                .isEqualTo("I really like the cloud.")

        assertThat(
                changesByPropertyName[WorkspaceTargetProperty.ADDITIONAL_NOTES.propertyName]
                        ?.newValue)
                .isEqualTo("I changed my mind")
    }

    @Test
    fun testHandlesMissingValues() {
        workspace2!!.cdrVersionId = null
        val changesByName = TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(), workspace1!!, workspace2!!)
        assertThat(
                changesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        ?.previousValue)
                .isEqualTo("1")
        assertThat(
                changesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        ?.newValue)
                .isNull()

        val reverseChangesByName = TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(), workspace2!!, workspace1!!)
        assertThat(
                reverseChangesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        ?.previousValue)
                .isNull()
        assertThat(
                reverseChangesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        ?.newValue)
                .isEqualTo("1")
    }

    @Test
    fun testComparisonToSelfIsEmpty() {
        assertThat(TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(), workspace1!!, workspace1!!)).isEmpty()
    }
}
