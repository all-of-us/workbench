package org.pmiops.workbench.audit.targetproperties

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
        researchPurpose1.setIntendedStudy("stubbed toes")
        researchPurpose1.setAdditionalNotes("I really like the cloud.")

        val researchPurpose2 = ResearchPurpose()
        researchPurpose2.setIntendedStudy("broken dreams")
        researchPurpose2.setAdditionalNotes("I changed my mind")
        researchPurpose2.setAnticipatedFindings("a 4-leaf clover")

        val now = System.currentTimeMillis()

        workspace1 = Workspace()
        workspace1!!.setName("Workspace 1")
        workspace1!!.setId("fc-id-1")
        workspace1!!.setNamespace("aou-rw-local1-c4be869a")
        workspace1!!.setCreator("user@fake-research-aou.org")
        workspace1!!.setCdrVersionId("1")
        workspace1!!.setResearchPurpose(researchPurpose1)
        workspace1!!.setCreationTime(now)
        workspace1!!.setLastModifiedTime(now)
        workspace1!!.setEtag("etag_1")
        workspace1!!.setDataAccessLevel(DataAccessLevel.REGISTERED)
        workspace1!!.setPublished(false)

        workspace2 = Workspace()
        workspace2!!.setName("Workspace 2")
        workspace2!!.setId("fc-id-1")
        workspace2!!.setNamespace("aou-rw-local1-c4be869a")
        workspace2!!.setCreator("user@fake-research-aou.org")
        workspace2!!.setCdrVersionId("33")
        workspace2!!.setResearchPurpose(researchPurpose2)
        workspace2!!.setCreationTime(now)
        workspace2!!.setLastModifiedTime(now)
        workspace2!!.setEtag("etag_1")
        workspace2!!.setDataAccessLevel(DataAccessLevel.REGISTERED)
        workspace2!!.setPublished(false)

        emptyWorkspace = Workspace()
        emptyWorkspace!!.setResearchPurpose(ResearchPurpose())
    }

    @Test
    fun testExtractsStringPropertiesFromWorkspace() {
        val propertiesByName = WorkspaceTargetProperty.getPropertyValuesByName(workspace1)

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
        assertThat(WorkspaceTargetProperty.getPropertyValuesByName(emptyWorkspace)).isEmpty()
        assertThat(WorkspaceTargetProperty.getPropertyValuesByName(null)).isEmpty()
    }

    @Test
    fun testMapsChanges() {
        val changesByPropertyName = WorkspaceTargetProperty.getChangedValuesByName(workspace1, workspace2)

        assertThat(changesByPropertyName).hasSize(5)

        assertThat(
                changesByPropertyName[WorkspaceTargetProperty.ADDITIONAL_NOTES.propertyName]
                        .previousValue)
                .isEqualTo("I really like the cloud.")

        assertThat(
                changesByPropertyName[WorkspaceTargetProperty.ADDITIONAL_NOTES.propertyName]
                        .newValue)
                .isEqualTo("I changed my mind")
    }

    @Test
    fun testHandlesMissingValues() {
        workspace2!!.setCdrVersionId(null)
        val changesByName = WorkspaceTargetProperty.getChangedValuesByName(workspace1, workspace2)
        assertThat(
                changesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        .previousValue)
                .isEqualTo("1")
        assertThat(
                changesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        .newValue)
                .isNull()

        val reverseChangesByName = WorkspaceTargetProperty.getChangedValuesByName(workspace2, workspace1)
        assertThat(
                reverseChangesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        .previousValue)
                .isNull()
        assertThat(
                reverseChangesByName[WorkspaceTargetProperty.CDR_VERSION_ID.propertyName]
                        .newValue)
                .isEqualTo("1")
    }

    @Test
    fun testComparisonToSelfIsEmpty() {
        assertThat(WorkspaceTargetProperty.getChangedValuesByName(workspace1, workspace1)).isEmpty()
        assertThat(WorkspaceTargetProperty.getChangedValuesByName(null, null)).isEmpty()
    }

    @Test
    fun testComparisonToNullMatchesAllProperties() {
        assertThat(WorkspaceTargetProperty.getChangedValuesByName(workspace1, null)).hasSize(6)
    }
}
