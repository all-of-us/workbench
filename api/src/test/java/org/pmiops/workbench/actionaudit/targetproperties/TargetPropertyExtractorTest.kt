package org.pmiops.workbench.actionaudit.targetproperties

import com.google.common.truth.Truth.assertThat
import kotlin.reflect.KClass
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.pmiops.workbench.access.AccessTierService
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Workspace

class TargetPropertyExtractorTest {
    private var workspace: Workspace? = null
    private var researchPurpose1: ResearchPurpose? = null

    @BeforeEach
    fun setUp() {
        researchPurpose1 = ResearchPurpose()
            .apply { intendedStudy = "stubbed toes" }
            .apply { additionalNotes = "I really like the cloud." }

        val now = System.currentTimeMillis()

        workspace = Workspace()
            .apply { name = "DbWorkspace 1" }
            .apply { id = "fc-id-1" }
            .apply { namespace = "aou-rw-local1-c4be869a" }
            .apply { creator = "user@fake-research-aou.org" }
            .apply { cdrVersionId = "1" }
            .apply { researchPurpose = researchPurpose1 }
            .apply { creationTime = now }
            .apply { lastModifiedTime = now }
            .apply { etag = "etag_1" }
            .apply { accessTierShortName = AccessTierService.REGISTERED_TIER_SHORT_NAME }
            .published(false)
    }

    @Test fun testGetsWorkspaceProperties() {
        val propertyValuesByName =
                TargetPropertyExtractor.getPropertyValuesByName(
                        WorkspaceTargetProperty.values(),
                        workspace!!)
        assertThat(propertyValuesByName[WorkspaceTargetProperty.NAME.propertyName])
                .isEqualTo("DbWorkspace 1")
        assertThat(propertyValuesByName).hasSize(19)
    }

    @Test fun testGetTargetPropertyEnumByTargetClass() {
        val result: Map<KClass<out Any>, KClass<out Any>> =
                TargetPropertyExtractor.getTargetPropertyEnumByTargetClass()
        assertThat(result).hasSize(2)
        assertThat(result[Workspace::class]).isEqualTo(WorkspaceTargetProperty::class)
    }

    @Test fun testGetPropertyEnum() {
        val result: KClass<out Any> =
                TargetPropertyExtractor.getTargetPropertyEnum(Workspace::class)
        assertThat(result).isEqualTo(WorkspaceTargetProperty::class)
    }
}
