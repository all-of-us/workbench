package org.pmiops.workbench.actionaudit.targetproperties

interface TargetProperty<T> {
    val propertyName: String
    val extractor: (T) -> String?
}
