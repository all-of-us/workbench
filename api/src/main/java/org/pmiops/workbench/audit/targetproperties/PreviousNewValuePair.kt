package org.pmiops.workbench.audit.targetproperties

class PreviousNewValuePair constructor(var previousValue: String?, var newValue: String?) {

    val valueChanged
        get(): Boolean {
        return previousValue == null && newValue != null ||
                newValue == null && previousValue != null ||
                previousValue != newValue
    }
}
