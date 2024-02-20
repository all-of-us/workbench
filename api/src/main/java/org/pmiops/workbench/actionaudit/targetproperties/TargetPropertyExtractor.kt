package org.pmiops.workbench.actionaudit.targetproperties

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
                        PreviousNewValuePair(it.extractor.invoke(previousTarget), it.extractor.invoke(newTarget))
                }
                .filter { it.second.hasValueChanged() }
                .toMap()
        }
    }
}
