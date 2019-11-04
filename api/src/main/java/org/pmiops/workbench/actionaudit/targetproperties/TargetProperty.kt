package org.pmiops.workbench.actionaudit.targetproperties

interface TargetProperty<T> {
    val propertyName: String
    fun extract(target: T): String?
}
