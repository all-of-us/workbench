package org.pmiops.workbench.actionaudit.targetproperties

import org.junit.Test
import org.pmiops.workbench.model.Workspace
import kotlin.reflect.KClass

class TargetPropertyUtilsTest {
    @Test fun testGetsWorkspaceProperties() {

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
