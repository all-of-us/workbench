package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.Workspace
import kotlin.reflect.KClass

class TargetPropertyExtractor {
    companion object {

        @JvmStatic
        fun <T : Any, E : TargetProperty<T>> getPropertyValuesByName(
            enumValues: Array<E>,
            target: T
        ): Map<String, String> {
            return enumValues
                    .filter { it.extractor.invoke(target) != null }
                    .map { it.propertyName to it.extractor.invoke(target)!! }
                    .toMap()
        }

        @JvmStatic
        fun <T : Any, E : TargetProperty<T>> getChangedValuesByName(
            enumValues: Array<E>,
            previousTarget: T,
            newTarget: T
        ): Map<String, PreviousNewValuePair> {
            return enumValues
                    .map { it.propertyName to PreviousNewValuePair(
                            previousValue = it.extractor.invoke(previousTarget),
                            newValue = it.extractor.invoke(newTarget)) }
                    .filter { it.second.valueChanged }
                    .toMap()
        }

        @JvmStatic
        fun getTargetPropertyEnum(targetClass: KClass<out Any>): KClass<out Any> {
            val map: Map<KClass<out Any>, KClass<out Any>> =
                    getTargetPropertyEnumByTargetClass()
            return map[targetClass] ?: error("Source class $targetClass not found")
        }

        @JvmStatic
        fun getTargetPropertyEnumByTargetClass(): Map<KClass<out Any>, KClass<out Any>> {
            return mapOf(Workspace::class to WorkspaceTargetProperty::class,
                    Profile::class to ProfileTargetProperty::class)
        }
    }
}
