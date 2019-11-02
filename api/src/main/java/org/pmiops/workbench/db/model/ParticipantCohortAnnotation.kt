package org.pmiops.workbench.db.model

import java.sql.Date
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Transient
import org.apache.commons.lang3.builder.ToStringBuilder

@Entity
@Table(name = "participant_cohort_annotations")
class ParticipantCohortAnnotation {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.IDENTITY)
    @get:Column(name = "annotation_id")
    var annotationId: Long? = null
    @get:Column(name = "cohort_annotation_definition_id")
    var cohortAnnotationDefinitionId: Long? = null
    @get:Column(name = "cohort_review_id")
    var cohortReviewId: Long? = null
    @get:Column(name = "participant_id")
    var participantId: Long? = null
    @get:Column(name = "annotation_value_string")
    var annotationValueString: String? = null
    @get:OneToOne
    @get:JoinColumn(name = "cohort_annotation_enum_value_id")
    var cohortAnnotationEnumValue: CohortAnnotationEnumValue? = null
    @get:Transient
    var annotationValueEnum: String? = null
    @get:Column(name = "annotation_value_date")
    var annotationValueDate: Date? = null
    @get:Transient
    var annotationValueDateString: String? = null
    @get:Column(name = "annotation_value_boolean")
    var annotationValueBoolean: Boolean? = null
    @get:Column(name = "annotation_value_integer")
    var annotationValueInteger: Int? = null

    fun annotationId(annotationId: Long?): ParticipantCohortAnnotation {
        this.annotationId = annotationId
        return this
    }

    fun cohortAnnotationDefinitionId(
            cohortAnnotationDefinitionId: Long?): ParticipantCohortAnnotation {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId
        return this
    }

    fun cohortReviewId(cohortReviewId: Long?): ParticipantCohortAnnotation {
        this.cohortReviewId = cohortReviewId
        return this
    }

    fun participantId(participantId: Long?): ParticipantCohortAnnotation {
        this.participantId = participantId
        return this
    }

    fun annotationValueString(annotationValueString: String): ParticipantCohortAnnotation {
        this.annotationValueString = annotationValueString
        return this
    }

    fun cohortAnnotationEnumValue(
            cohortAnnotationEnumValue: CohortAnnotationEnumValue): ParticipantCohortAnnotation {
        this.cohortAnnotationEnumValue = cohortAnnotationEnumValue
        return this
    }

    fun annotationValueEnum(annotationValueEnum: String): ParticipantCohortAnnotation {
        this.annotationValueEnum = annotationValueEnum
        return this
    }

    fun annotationValueDate(annotationValueDate: Date): ParticipantCohortAnnotation {
        this.annotationValueDate = annotationValueDate
        return this
    }

    fun annotationValueDateString(annotationValueDateString: String): ParticipantCohortAnnotation {
        this.annotationValueDateString = annotationValueDateString
        return this
    }

    fun annotationValueBoolean(annotationValueBoolean: Boolean?): ParticipantCohortAnnotation {
        this.annotationValueBoolean = annotationValueBoolean
        return this
    }

    fun annotationValueInteger(annotationValueInteger: Int?): ParticipantCohortAnnotation {
        this.annotationValueInteger = annotationValueInteger
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ParticipantCohortAnnotation?
        return (cohortAnnotationDefinitionId == that!!.cohortAnnotationDefinitionId
                && cohortReviewId == that.cohortReviewId
                && participantId == that.participantId
                && annotationValueString == that.annotationValueString
                && annotationValueDate == that.annotationValueDate
                && annotationValueBoolean == that.annotationValueBoolean
                && annotationValueInteger == that.annotationValueInteger)
    }

    override fun hashCode(): Int {
        return Objects.hash(
                cohortAnnotationDefinitionId,
                cohortReviewId,
                participantId,
                annotationValueString,
                annotationValueDate,
                annotationValueBoolean,
                annotationValueInteger)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("annotationId", annotationId)
                .append("cohortAnnotationDefinitionId", cohortAnnotationDefinitionId)
                .append("cohortReviewId", cohortReviewId)
                .append("participantId", participantId)
                .append("annotationValueString", annotationValueString)
                .append("annotationValueDate", annotationValueDate)
                .append("annotationValueBoolean", annotationValueBoolean)
                .append("annotationValueInteger", annotationValueInteger)
                .toString()
    }
}
