package org.pmiops.workbench.actionaudit.targetproperties

import org.junit.Before
import org.junit.Test
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.utils.codegenhelpers.ResearchPurposeHelper
import org.pmiops.workbench.utils.codegenhelpers.WorkspaceHelper
import kotlin.reflect.KClass

class TargetPropertyExtractorTest {
    private var workspace: Workspace? = null
    private var researchPurpose1: ResearchPurpose? = null

    @Before
    fun setUp() {
        researchPurpose1 = ResearchPurposeHelper.getInstance().create()
            .apply { intendedStudy = "stubbed toes" }
            .apply { additionalNotes = "I really like the cloud." }

        val now = System.currentTimeMillis()

        workspace = WorkspaceHelper.getInstance().create()
            .apply { name = "DbWorkspace 1" }
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
