package org.pmiops.workbench.actionaudit.targetproperties

data class PreviousNewValuePair(var previousValue: String?, var newValue: String?) {

    val valueChanged: Boolean
        get() {
            return previousValue == null && newValue != null ||
                    newValue == null && previousValue != null ||
                    previousValue != newValue
        }
}
