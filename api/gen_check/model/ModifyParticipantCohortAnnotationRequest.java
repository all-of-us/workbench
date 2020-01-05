package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ModifyParticipantCohortAnnotationRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ModifyParticipantCohortAnnotationRequest   {
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

  public ModifyParticipantCohortAnnotationRequest annotationValueString(String annotationValueString) {
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

  public ModifyParticipantCohortAnnotationRequest annotationValueEnum(String annotationValueEnum) {
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

  public ModifyParticipantCohortAnnotationRequest annotationValueDate(String annotationValueDate) {
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

  public ModifyParticipantCohortAnnotationRequest annotationValueBoolean(Boolean annotationValueBoolean) {
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

  public ModifyParticipantCohortAnnotationRequest annotationValueInteger(Integer annotationValueInteger) {
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
    ModifyParticipantCohortAnnotationRequest modifyParticipantCohortAnnotationRequest = (ModifyParticipantCohortAnnotationRequest) o;
    return Objects.equals(this.annotationValueString, modifyParticipantCohortAnnotationRequest.annotationValueString) &&
        Objects.equals(this.annotationValueEnum, modifyParticipantCohortAnnotationRequest.annotationValueEnum) &&
        Objects.equals(this.annotationValueDate, modifyParticipantCohortAnnotationRequest.annotationValueDate) &&
        Objects.equals(this.annotationValueBoolean, modifyParticipantCohortAnnotationRequest.annotationValueBoolean) &&
        Objects.equals(this.annotationValueInteger, modifyParticipantCohortAnnotationRequest.annotationValueInteger);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotationValueString, annotationValueEnum, annotationValueDate, annotationValueBoolean, annotationValueInteger);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModifyParticipantCohortAnnotationRequest {\n");
    
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

