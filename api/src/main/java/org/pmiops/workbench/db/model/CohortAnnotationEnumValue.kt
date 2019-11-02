package org.pmiops.workbench.db.model

import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(name = "cohort_annotation_enum_value")
class CohortAnnotationEnumValue : Comparable<*> {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "cohort_annotation_enum_value_id")
    var cohortAnnotationEnumValueId: Long = 0
    @get:Column(name = "name")
    var name: String? = null
    @get:Column(name = "enum_order")
    var order: Int = 0
    @get:ManyToOne
    @get:JoinColumn(name = "cohort_annotation_definition_id")
    var cohortAnnotationDefinition: CohortAnnotationDefinition? = null
    @get:OneToOne(mappedBy = "cohortAnnotationEnumValue")
    var participantCohortAnnotation: ParticipantCohortAnnotation? = null

    fun cohortAnnotationEnumValueId(cohortAnnotationEnumValueId: Long): CohortAnnotationEnumValue {
        this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId
        return this
    }

    fun name(name: String): CohortAnnotationEnumValue {
        this.name = name
        return this
    }

    fun order(order: Int): CohortAnnotationEnumValue {
        this.order = order
        return this
    }

    fun cohortAnnotationDefinition(
            cohortAnnotationDefinition: CohortAnnotationDefinition): CohortAnnotationEnumValue {
        this.cohortAnnotationDefinition = cohortAnnotationDefinition
        return this
    }

    fun participantCohortAnnotation(
            participantCohortAnnotation: ParticipantCohortAnnotation): CohortAnnotationEnumValue {
        this.participantCohortAnnotation = participantCohortAnnotation
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val enumValue = o as CohortAnnotationEnumValue?
        return name == enumValue!!.name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("cohortAnnotationEnumValueId", cohortAnnotationEnumValueId)
                .append("name", name)
                .toString()
    }

    override operator fun compareTo(o: Any): Int {
        val thisOrder = this.order
        val otherOrder = (o as CohortAnnotationEnumValue).order
        return thisOrder.compareTo(otherOrder)
    }
}
