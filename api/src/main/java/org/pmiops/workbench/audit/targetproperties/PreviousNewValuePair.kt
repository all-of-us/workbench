package org.pmiops.workbench.audit.targetproperties

import java.util.Objects

class PreviousNewValuePair private constructor(// TODO: make these nullable after Kotlin conversion
        var previousValue: String?, var newValue: String?) {

    fun valueChanged(): Boolean {
        return previousValue != newValue
    }

    override fun toString(): String {
        return ("PreviousNewValuePair{"
                + "previousValue='"
                + previousValue
                + '\''.toString()
                + ", newValue='"
                + newValue
                + '\''.toString()
                + '}'.toString())
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is PreviousNewValuePair) {
            return false
        }
        val that = o as PreviousNewValuePair?
        return previousValue == that!!.previousValue && newValue == that.newValue
    }

    override fun hashCode(): Int {
        return Objects.hash(previousValue, newValue)
    }

    class Builder {

        private var previousValue: String? = null
        private var newValue: String? = null

        fun setPreviousValue(previousValue: String): Builder {
            this.previousValue = previousValue
            return this
        }

        fun setNewValue(newValue: String): Builder {
            this.newValue = newValue
            return this
        }

        fun build(): PreviousNewValuePair {
            return PreviousNewValuePair(previousValue, newValue)
        }
    }

    companion object {

        fun builder(): Builder {
            return Builder()
        }
    }
}
