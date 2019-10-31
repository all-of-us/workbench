package org.pmiops.workbench.audit.targetproperties

class PreviousNewValuePair constructor(var previousValue: String?, var newValue: String?) {

    fun valueChanged(): Boolean {
        return previousValue == null && newValue != null ||
                newValue == null && previousValue != null ||
                previousValue != newValue
    }

    override fun toString(): String {
        return ("PreviousNewValuePair{" +
                "previousValue='" +
                previousValue +
                '\''.toString() +
                ", newValue='" +
                newValue +
                '\''.toString() +
                '}'.toString())
    }
}
