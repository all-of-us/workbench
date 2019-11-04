package org.pmiops.workbench.audit.targetproperties

import org.pmiops.workbench.model.Profile

enum class ProfileTargetProperty(val propertyName: String, private val extractor: (Profile) -> String?) {

}
