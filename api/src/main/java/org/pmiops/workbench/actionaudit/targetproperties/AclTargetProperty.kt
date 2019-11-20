package org.pmiops.workbench.actionaudit.targetproperties

// This is a bit of a one-off for workspace collaboration, where we have a
// target type of User but properties that don't really belong to a User model.
enum class AclTargetProperty
constructor(override val propertyName: String) : SimpleTargetProperty {
    ACCESS_LEVEL("access_level");
}
