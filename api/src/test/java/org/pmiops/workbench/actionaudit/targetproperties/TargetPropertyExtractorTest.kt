package org.pmiops.workbench.actionaudit.targetproperties

import org.junit.Before
import org.junit.Test
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Workspace
import kotlin.reflect.KClass

class TargetPropertyExtractorTest {
    private var workspace: Workspace? = null
    private var researchPurpose1: ResearchPurpose? = null

    @Before
    fun setUp() {
        researchPurpose1 = ResearchPurpose()
        researchPurpose1?.intendedStudy = "stubbed toes"
        researchPurpose1?.additionalNotes = "I really like the cloud."
        val now = System.currentTimeMillis()

        workspace = Workspace()
        workspace?.name = "DbWorkspace 1"
        workspace?.id = "fc-id-1"
        workspace?.namespace = "aou-rw-local1-c4be869a"
        workspace?.creator = "user@fake-research-aou.org"
        workspace?.cdrVersionId = "1"
        workspace?.researchPurpose = researchPurpose1
        workspace?.creationTime = now
        workspace?.lastModifiedTime = now
        workspace?.etag = "etag_1"
        workspace?.dataAccessLevel = DataAccessLevel.REGISTERED
        workspace?.published = false
    }

    @Test fun testGetsWorkspaceProperties() {
        val propertyValuesByName =
                TargetPropertyExtractor.getPropertyValuesByName(
                        WorkspaceTargetProperty.values(),
                        workspace!!)
        assert(propertyValuesByName[WorkspaceTargetProperty.NAME.propertyName] == "DbWorkspace 1")
        assert(propertyValuesByName.size == 6)
    }

    @Test fun testGetTargetPropertyEnumByTargetClass() {
        val result: Map<KClass<out Any>, KClass<out Any>> =
                TargetPropertyExtractor.getTargetPropertyEnumByTargetClass()
        assert(result.size == 2)

        assert(result[Workspace::class] == WorkspaceTargetProperty::class)
    }

    @Test fun testGetPropertyEnum() {
        val result: KClass<out Any> =
                TargetPropertyExtractor.getTargetPropertyEnum(Workspace::class)
        assert(result == WorkspaceTargetProperty::class)
    }
}
