package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.Workspace

class TargetPropertyExtractor {
    companion object {
        @JvmStatic
        fun <T : Any, E : ModelBackedTargetProperty<T>> getPropertyValuesByName(
            enumValues: Array<E>,
            target: T,
        ): Map<String, String> {
            return enumValues
                .filter { it.extractor.invoke(target) != null }
                .map { it.propertyName to it.extractor.invoke(target)!! }
                .toMap()
        }

        @JvmStatic
        fun <T : Any, E : ModelBackedTargetProperty<T>> getChangedValuesByName(
            enumValues: Array<E>,
            previousTarget: T,
            newTarget: T,
        ): Map<String, PreviousNewValuePair> {
            return enumValues
                .map {
                    it.propertyName to
                        PreviousNewValuePair(
                            previousValue = it.extractor.invoke(previousTarget),
                            newValue = it.extractor.invoke(newTarget),
                        )
                }
                .filter { it.second.valueChanged }
                .toMap()
        }

        @JvmStatic
        fun getTargetPropertyEnum(targetClass: Class<out Any>): Class<out Any> {
            val map = getTargetPropertyEnumByTargetClass()
            return map[targetClass] ?: error("Source class $targetClass not found")
        }

        @JvmStatic
        fun getTargetPropertyEnumByTargetClass(): Map<Class<out Any>, Class<out Any>> {
            return mapOf(
                Workspace::class.java to WorkspaceTargetProperty::class.java,
                Profile::class.java to ProfileTargetProperty::class.java,
            )
        }
    }
}
