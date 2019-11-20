package org.pmiops.workbench.actionaudit.targetproperties

// This interface is a contract to allow any model type T to support
// arbitrary String properties without any changes to T. Intended
// to be implemented by enum classes.
interface ModelBackedTargetProperty<T> : SimpleTargetProperty {
    override val propertyName: String
    val extractor: (T) -> String?
}
