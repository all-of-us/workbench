package org.pmiops.workbench.actionaudit.targetproperties

import org.pmiops.workbench.model.Profile
import org.pmiops.workbench.model.Workspace
import kotlin.reflect.KClass

class TargetPropertyUtils {
    companion object {

        @JvmStatic
        inline fun <T, E: TargetProperty<T>> getPropertyValuesByName(
                valuesGetter: () -> Array<E>,
                target: T): Map<String, String> {
            // TODO(jaycarlton): find a way to grab the type of the target
            // and look it up in the map below, so we only need one argument here.
            // these don't work:
//            val enumClass = getTargetPropertyEnumByTargetClass()[T::class]
//            val targetPropertyEnum = getTargetPropertyEnum(T::class)
            return valuesGetter.invoke()
                    .filter { it.extract(target) != null }
                    .map { it.propertyName to it.extract(target)!! }
                    .toMap()
        }

        @JvmStatic
        fun <T, E: TargetProperty<T>> getChangedValuesByName(
                valuesGetter: () -> Array<E>,
                previousTarget: T,
                newTarget: T
        ): Map<String, PreviousNewValuePair> {
            return valuesGetter.invoke()
                    .map { it.propertyName to PreviousNewValuePair(
                            previousValue = it.extract(previousTarget),
                            newValue = it.extract(newTarget)) }
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
