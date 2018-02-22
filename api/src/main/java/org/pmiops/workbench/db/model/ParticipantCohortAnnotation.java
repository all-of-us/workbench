package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Date;
import java.util.Objects;

@Entity
@Table(name = "participant_cohort_annotations")
public class ParticipantCohortAnnotation {
    private Long annotationId;
    private Long cohortAnnotationDefinitionId;
    private Long cohortReviewId;
    private Long participantId;
    private String annotationValueString;
    private Long cohortAnnotationEnumValueId;
    private String annotationValueEnum;
    private Date annotationValueDate;
    private String annotationValueDateString;
    private Boolean annotationValueBoolean;
    private Integer annotationValueInteger;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "annotation_id")
    public Long getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(Long annotationId) {
        this.annotationId = annotationId;
    }

    public ParticipantCohortAnnotation annotationId(Long annotationId) {
        this.annotationId = annotationId;
        return this;
    }

    @Column(name = "cohort_annotation_definition_id")
    public Long getCohortAnnotationDefinitionId() {
        return cohortAnnotationDefinitionId;
    }

    public void setCohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
    }

    public ParticipantCohortAnnotation cohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
        this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
        return this;
    }

    @Column(name = "cohort_review_id")
    public Long getCohortReviewId() {
        return cohortReviewId;
    }

    public void setCohortReviewId(Long cohortReviewId) {
        this.cohortReviewId = cohortReviewId;
    }

    public ParticipantCohortAnnotation cohortReviewId(Long cohortReviewId) {
        this.cohortReviewId = cohortReviewId;
        return this;
    }

    @Column(name = "participant_id")
    public Long getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
    }

    public ParticipantCohortAnnotation participantId(Long participantId) {
        this.participantId = participantId;
        return this;
    }

    @Column(name = "annotation_value_string")
    public String getAnnotationValueString() {
        return annotationValueString;
    }

    public void setAnnotationValueString(String annotationValueString) {
        this.annotationValueString = annotationValueString;
    }

    public ParticipantCohortAnnotation annotationValueString(String annotationValueString) {
        this.annotationValueString = annotationValueString;
        return this;
    }

    @Column(name = "cohort_annotation_enum_value_id")
    public Long getCohortAnnotationEnumValueId() {
        return cohortAnnotationEnumValueId;
    }

    public void setCohortAnnotationEnumValueId(Long cohortAnnotationEnumValueId) {
        this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
    }

    public ParticipantCohortAnnotation cohortAnnotationEnumValueId(Long cohortAnnotationEnumValueId) {
        this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
        return this;
    }

    @Transient
    public String getAnnotationValueEnum() {
        return annotationValueEnum;
    }

    public void setAnnotationValueEnum(String annotationValueEnum) {
        this.annotationValueEnum = annotationValueEnum;
    }

    public ParticipantCohortAnnotation annotationValueEnum(String annotationValueEnum) {
        this.annotationValueEnum = annotationValueEnum;
        return this;
    }

    @Column(name = "annotation_value_date")
    public Date getAnnotationValueDate() {
        return annotationValueDate;
    }

    public void setAnnotationValueDate(Date annotationValueDate) {
        this.annotationValueDate = annotationValueDate;
    }

    public ParticipantCohortAnnotation annotationValueDate(Date annotationValueDate) {
        this.annotationValueDate = annotationValueDate;
        return this;
    }

    @Transient
    public String getAnnotationValueDateString() {
        return annotationValueDateString;
    }

    public void setAnnotationValueDateString(String annotationValueDateString) {
        this.annotationValueDateString = annotationValueDateString;
    }

    public ParticipantCohortAnnotation annotationValueDateString(String annotationValueDateString) {
        this.annotationValueDateString = annotationValueDateString;
        return this;
    }

    @Column(name = "annotation_value_boolean")
    public Boolean getAnnotationValueBoolean() {
        return annotationValueBoolean;
    }

    public void setAnnotationValueBoolean(Boolean annotationValueBoolean) {
        this.annotationValueBoolean = annotationValueBoolean;
    }

    public ParticipantCohortAnnotation annotationValueBoolean(Boolean annotationValueBoolean) {
        this.annotationValueBoolean = annotationValueBoolean;
        return this;
    }

    @Column(name = "annotation_value_integer")
    public Integer getAnnotationValueInteger() {
        return annotationValueInteger;
    }

    public void setAnnotationValueInteger(Integer annotationValueInteger) {
        this.annotationValueInteger = annotationValueInteger;
    }

    public ParticipantCohortAnnotation annotationValueInteger(Integer annotationValueInteger) {
        this.annotationValueInteger = annotationValueInteger;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParticipantCohortAnnotation that = (ParticipantCohortAnnotation) o;
        return Objects.equals(cohortAnnotationDefinitionId, that.cohortAnnotationDefinitionId) &&
                Objects.equals(cohortReviewId, that.cohortReviewId) &&
                Objects.equals(participantId, that.participantId) &&
                Objects.equals(annotationValueString, that.annotationValueString) &&
                Objects.equals(cohortAnnotationEnumValueId, that.cohortAnnotationEnumValueId) &&
                Objects.equals(annotationValueDate, that.annotationValueDate) &&
                Objects.equals(annotationValueBoolean, that.annotationValueBoolean) &&
                Objects.equals(annotationValueInteger, that.annotationValueInteger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cohortAnnotationDefinitionId,
                cohortReviewId,
                participantId,
                annotationValueString,
                cohortAnnotationEnumValueId,
                annotationValueDate,
                annotationValueBoolean,
                annotationValueInteger);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("annotationId", annotationId)
                .append("cohortAnnotationDefinitionId", cohortAnnotationDefinitionId)
                .append("cohortReviewId", cohortReviewId)
                .append("participantId", participantId)
                .append("annotationValueString", annotationValueString)
                .append("cohortAnnotationEnumValueId", cohortAnnotationEnumValueId)
                .append("annotationValueDate", annotationValueDate)
                .append("annotationValueBoolean", annotationValueBoolean)
                .append("annotationValueInteger", annotationValueInteger)
                .toString();
    }
}
