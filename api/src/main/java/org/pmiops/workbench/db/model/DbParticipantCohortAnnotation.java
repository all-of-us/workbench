package org.pmiops.workbench.db.model;

import java.sql.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "participant_cohort_annotations")
public class DbParticipantCohortAnnotation {
  private Long annotationId;
  private Long cohortAnnotationDefinitionId;
  private Long cohortReviewId;
  private Long participantId;
  private String annotationValueString;
  private DbCohortAnnotationEnumValue cohortAnnotationEnumValue;
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

  public DbParticipantCohortAnnotation annotationId(Long annotationId) {
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

  public DbParticipantCohortAnnotation cohortAnnotationDefinitionId(
      Long cohortAnnotationDefinitionId) {
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

  public DbParticipantCohortAnnotation cohortReviewId(Long cohortReviewId) {
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

  public DbParticipantCohortAnnotation participantId(Long participantId) {
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

  public DbParticipantCohortAnnotation annotationValueString(String annotationValueString) {
    this.annotationValueString = annotationValueString;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "cohort_annotation_enum_value_id")
  public DbCohortAnnotationEnumValue getCohortAnnotationEnumValue() {
    return cohortAnnotationEnumValue;
  }

  public void setCohortAnnotationEnumValue(DbCohortAnnotationEnumValue cohortAnnotationEnumValue) {
    this.cohortAnnotationEnumValue = cohortAnnotationEnumValue;
  }

  public DbParticipantCohortAnnotation cohortAnnotationEnumValue(
      DbCohortAnnotationEnumValue cohortAnnotationEnumValue) {
    this.cohortAnnotationEnumValue = cohortAnnotationEnumValue;
    return this;
  }

  @Transient
  public String getAnnotationValueEnum() {
    return annotationValueEnum;
  }

  public void setAnnotationValueEnum(String annotationValueEnum) {
    this.annotationValueEnum = annotationValueEnum;
  }

  public DbParticipantCohortAnnotation annotationValueEnum(String annotationValueEnum) {
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

  public DbParticipantCohortAnnotation annotationValueDate(Date annotationValueDate) {
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

  public DbParticipantCohortAnnotation annotationValueDateString(String annotationValueDateString) {
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

  public DbParticipantCohortAnnotation annotationValueBoolean(Boolean annotationValueBoolean) {
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

  public DbParticipantCohortAnnotation annotationValueInteger(Integer annotationValueInteger) {
    this.annotationValueInteger = annotationValueInteger;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbParticipantCohortAnnotation that = (DbParticipantCohortAnnotation) o;
    return Objects.equals(cohortAnnotationDefinitionId, that.cohortAnnotationDefinitionId)
        && Objects.equals(cohortReviewId, that.cohortReviewId)
        && Objects.equals(participantId, that.participantId)
        && Objects.equals(annotationValueString, that.annotationValueString)
        && Objects.equals(annotationValueDate, that.annotationValueDate)
        && Objects.equals(annotationValueBoolean, that.annotationValueBoolean)
        && Objects.equals(annotationValueInteger, that.annotationValueInteger);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cohortAnnotationDefinitionId,
        cohortReviewId,
        participantId,
        annotationValueString,
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
        .append("annotationValueDate", annotationValueDate)
        .append("annotationValueBoolean", annotationValueBoolean)
        .append("annotationValueInteger", annotationValueInteger)
        .toString();
  }
}
