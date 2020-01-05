package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SurveyModule
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class SurveyModule   {
  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("questionCount")
  private Long questionCount = null;

  @JsonProperty("participantCount")
  private Long participantCount = null;

  @JsonProperty("orderNumber")
  private Integer orderNumber = null;

  public SurveyModule conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * the concept ID for the survey module
   * @return conceptId
  **/
  @ApiModelProperty(required = true, value = "the concept ID for the survey module")
  @NotNull


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public SurveyModule name(String name) {
    this.name = name;
    return this;
  }

   /**
   * display name of the module
   * @return name
  **/
  @ApiModelProperty(required = true, value = "display name of the module")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SurveyModule description(String description) {
    this.description = description;
    return this;
  }

   /**
   * description of the module
   * @return description
  **/
  @ApiModelProperty(required = true, value = "description of the module")
  @NotNull


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public SurveyModule questionCount(Long questionCount) {
    this.questionCount = questionCount;
    return this;
  }

   /**
   * number of questions in the module
   * @return questionCount
  **/
  @ApiModelProperty(required = true, value = "number of questions in the module")
  @NotNull


  public Long getQuestionCount() {
    return questionCount;
  }

  public void setQuestionCount(Long questionCount) {
    this.questionCount = questionCount;
  }

  public SurveyModule participantCount(Long participantCount) {
    this.participantCount = participantCount;
    return this;
  }

   /**
   * number of participants with data in the CDR for questions in this module
   * @return participantCount
  **/
  @ApiModelProperty(required = true, value = "number of participants with data in the CDR for questions in this module")
  @NotNull


  public Long getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(Long participantCount) {
    this.participantCount = participantCount;
  }

  public SurveyModule orderNumber(Integer orderNumber) {
    this.orderNumber = orderNumber;
    return this;
  }

   /**
   * survey release order number
   * @return orderNumber
  **/
  @ApiModelProperty(required = true, value = "survey release order number")
  @NotNull


  public Integer getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(Integer orderNumber) {
    this.orderNumber = orderNumber;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SurveyModule surveyModule = (SurveyModule) o;
    return Objects.equals(this.conceptId, surveyModule.conceptId) &&
        Objects.equals(this.name, surveyModule.name) &&
        Objects.equals(this.description, surveyModule.description) &&
        Objects.equals(this.questionCount, surveyModule.questionCount) &&
        Objects.equals(this.participantCount, surveyModule.participantCount) &&
        Objects.equals(this.orderNumber, surveyModule.orderNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, name, description, questionCount, participantCount, orderNumber);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SurveyModule {\n");
    
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    questionCount: ").append(toIndentedString(questionCount)).append("\n");
    sb.append("    participantCount: ").append(toIndentedString(participantCount)).append("\n");
    sb.append("    orderNumber: ").append(toIndentedString(orderNumber)).append("\n");
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

