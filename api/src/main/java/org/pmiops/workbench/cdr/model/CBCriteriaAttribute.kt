package org.pmiops.workbench.cdr.model

import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(name = "cb_criteria_attribute")
class CBCriteriaAttribute {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "id")
    var id: Long = 0
    @get:Column(name = "concept_id")
    var conceptId: Long = 0
    @get:Column(name = "value_as_concept_id")
    var valueAsConceptId: Long = 0
    @get:Column(name = "concept_name")
    var conceptName: String? = null
    @get:Column(name = "type")
    var type: String? = null
    @get:Column(name = "est_count")
    var estCount: String? = null

    fun id(id: Long): CBCriteriaAttribute {
        this.id = id
        return this
    }

    fun conceptId(conceptId: Long): CBCriteriaAttribute {
        this.conceptId = conceptId
        return this
    }

    fun valueAsConceptId(valueAsConceptId: Long): CBCriteriaAttribute {
        this.valueAsConceptId = valueAsConceptId
        return this
    }

    fun conceptName(conceptName: String): CBCriteriaAttribute {
        this.conceptName = conceptName
        return this
    }

    fun type(type: String): CBCriteriaAttribute {
        this.type = type
        return this
    }

    fun estCount(estCount: String): CBCriteriaAttribute {
        this.estCount = estCount
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as CBCriteriaAttribute?
        return (conceptId == that!!.conceptId
                && valueAsConceptId == that.valueAsConceptId
                && conceptName == that.conceptName
                && type == that.type
                && estCount == that.estCount)
    }

    override fun hashCode(): Int {
        return Objects.hash(conceptId, valueAsConceptId, conceptName, type, estCount)
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }
}
