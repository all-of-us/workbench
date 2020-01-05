package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantCohortAnnotation
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class ParticipantCohortAnnotation   {
  @JsonProperty("annotationId")
  private Long annotationId = null;

  @JsonProperty("cohortAnnotationDefinitionId")
  private Long cohortAnnotationDefinitionId = null;

  @JsonProperty("cohortReviewId")
  private Long cohortReviewId = null;

  @JsonProperty("participantId")
  private Long participantId = null;

  @JsonProperty("annotationValueString")
  private String annotationValueString = null;

  @JsonProperty("annotationValueEnum")
  private String annotationValueEnum = null;

  @JsonProperty("annotationValueDate")
  private String annotationValueDate = null;

  @JsonProperty("annotationValueBoolean")
  private Boolean annotationValueBoolean = null;

  @JsonProperty("annotationValueInteger")
  private Integer annotationValueInteger = null;

  public ParticipantCohortAnnotation annotationId(Long annotationId) {
    this.annotationId = annotationId;
    return this;
  }

   /**
   * participant annotation id.
   * @return annotationId
  **/
  @ApiModelProperty(value = "participant annotation id.")


  public Long getAnnotationId() {
    return annotationId;
  }

  public void setAnnotationId(Long annotationId) {
    this.annotationId = annotationId;
  }

  public ParticipantCohortAnnotation cohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
    return this;
  }

   /**
   * annotation definition id.
   * @return cohortAnnotationDefinitionId
  **/
  @ApiModelProperty(required = true, value = "annotation definition id.")
  @NotNull


  public Long getCohortAnnotationDefinitionId() {
    return cohortAnnotationDefinitionId;
  }

  public void setCohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
  }

  public ParticipantCohortAnnotation cohortReviewId(Long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
    return this;
  }

   /**
   * the cohort reivew id.
   * @return cohortReviewId
  **/
  @ApiModelProperty(required = true, value = "the cohort reivew id.")
  @NotNull


  public Long getCohortReviewId() {
    return cohortReviewId;
  }

  public void setCohortReviewId(Long cohortReviewId) {
    this.cohortReviewId = cohortReviewId;
  }

  public ParticipantCohortAnnotation participantId(Long participantId) {
    this.participantId = participantId;
    return this;
  }

   /**
   * the participant id.
   * @return participantId
  **/
  @ApiModelProperty(required = true, value = "the participant id.")
  @NotNull


  public Long getParticipantId() {
    return participantId;
  }

  public void setParticipantId(Long participantId) {
    this.participantId = participantId;
  }

  public ParticipantCohortAnnotation annotationValueString(String annotationValueString) {
    this.annotationValueString = annotationValueString;
    return this;
  }

   /**
   * The Value of the annotation if the AnnotationType is STRING
   * @return annotationValueString
  **/
  @ApiModelProperty(value = "The Value of the annotation if the AnnotationType is STRING")


  public String getAnnotationValueString() {
    return annotationValueString;
  }

  public void setAnnotationValueString(String annotationValueString) {
    this.annotationValueString = annotationValueString;
  }

  public ParticipantCohortAnnotation annotationValueEnum(String annotationValueEnum) {
    this.annotationValueEnum = annotationValueEnum;
    return this;
  }

   /**
   * The option chosen for the annotation if the AnnotationType is ENUM
   * @return annotationValueEnum
  **/
  @ApiModelProperty(value = "The option chosen for the annotation if the AnnotationType is ENUM")


  public String getAnnotationValueEnum() {
    return annotationValueEnum;
  }

  public void setAnnotationValueEnum(String annotationValueEnum) {
    this.annotationValueEnum = annotationValueEnum;
  }

  public ParticipantCohortAnnotation annotationValueDate(String annotationValueDate) {
    this.annotationValueDate = annotationValueDate;
    return this;
  }

   /**
   * The Value of the annotation if the AnnotationType is DATE
   * @return annotationValueDate
  **/
  @ApiModelProperty(value = "The Value of the annotation if the AnnotationType is DATE")


  public String getAnnotationValueDate() {
    return annotationValueDate;
  }

  public void setAnnotationValueDate(String annotationValueDate) {
    this.annotationValueDate = annotationValueDate;
  }

  public ParticipantCohortAnnotation annotationValueBoolean(Boolean annotationValueBoolean) {
    this.annotationValueBoolean = annotationValueBoolean;
    return this;
  }

   /**
   * The Value of the annotation if the AnnotationType is BOOLEAN
   * @return annotationValueBoolean
  **/
  @ApiModelProperty(value = "The Value of the annotation if the AnnotationType is BOOLEAN")


  public Boolean getAnnotationValueBoolean() {
    return annotationValueBoolean;
  }

  public void setAnnotationValueBoolean(Boolean annotationValueBoolean) {
    this.annotationValueBoolean = annotationValueBoolean;
  }

  public ParticipantCohortAnnotation annotationValueInteger(Integer annotationValueInteger) {
    this.annotationValueInteger = annotationValueInteger;
    return this;
  }

   /**
   * The Value of the annotation if the AnnotationType is INTEGER
   * @return annotationValueInteger
  **/
  @ApiModelProperty(value = "The Value of the annotation if the AnnotationType is INTEGER")


  public Integer getAnnotationValueInteger() {
    return annotationValueInteger;
  }

  public void setAnnotationValueInteger(Integer annotationValueInteger) {
    this.annotationValueInteger = annotationValueInteger;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantCohortAnnotation participantCohortAnnotation = (ParticipantCohortAnnotation) o;
    return Objects.equals(this.annotationId, participantCohortAnnotation.annotationId) &&
        Objects.equals(this.cohortAnnotationDefinitionId, participantCohortAnnotation.cohortAnnotationDefinitionId) &&
        Objects.equals(this.cohortReviewId, participantCohortAnnotation.cohortReviewId) &&
        Objects.equals(this.participantId, participantCohortAnnotation.participantId) &&
        Objects.equals(this.annotationValueString, participantCohortAnnotation.annotationValueString) &&
        Objects.equals(this.annotationValueEnum, participantCohortAnnotation.annotationValueEnum) &&
        Objects.equals(this.annotationValueDate, participantCohortAnnotation.annotationValueDate) &&
        Objects.equals(this.annotationValueBoolean, participantCohortAnnotation.annotationValueBoolean) &&
        Objects.equals(this.annotationValueInteger, participantCohortAnnotation.annotationValueInteger);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotationId, cohortAnnotationDefinitionId, cohortReviewId, participantId, annotationValueString, annotationValueEnum, annotationValueDate, annotationValueBoolean, annotationValueInteger);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantCohortAnnotation {\n");
    
    sb.append("    annotationId: ").append(toIndentedString(annotationId)).append("\n");
    sb.append("    cohortAnnotationDefinitionId: ").append(toIndentedString(cohortAnnotationDefinitionId)).append("\n");
    sb.append("    cohortReviewId: ").append(toIndentedString(cohortReviewId)).append("\n");
    sb.append("    participantId: ").append(toIndentedString(participantId)).append("\n");
    sb.append("    annotationValueString: ").append(toIndentedString(annotationValueString)).append("\n");
    sb.append("    annotationValueEnum: ").append(toIndentedString(annotationValueEnum)).append("\n");
    sb.append("    annotationValueDate: ").append(toIndentedString(annotationValueDate)).append("\n");
    sb.append("    annotationValueBoolean: ").append(toIndentedString(annotationValueBoolean)).append("\n");
    sb.append("    annotationValueInteger: ").append(toIndentedString(annotationValueInteger)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

