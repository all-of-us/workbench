package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SurveyAnswerResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class SurveyAnswerResponse   {
  @JsonProperty("answer")
  private String answer = null;

  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("participationCount")
  private Long participationCount = null;

  @JsonProperty("percentAnswered")
  private Double percentAnswered = null;

  public SurveyAnswerResponse answer(String answer) {
    this.answer = answer;
    return this;
  }

   /**
   * Get answer
   * @return answer
  **/
  @ApiModelProperty(value = "")


  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public SurveyAnswerResponse conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * Get conceptId
   * @return conceptId
  **/
  @ApiModelProperty(value = "")


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public SurveyAnswerResponse participationCount(Long participationCount) {
    this.participationCount = participationCount;
    return this;
  }

   /**
   * Get participationCount
   * @return participationCount
  **/
  @ApiModelProperty(value = "")


  public Long getParticipationCount() {
    return participationCount;
  }

  public void setParticipationCount(Long participationCount) {
    this.participationCount = participationCount;
  }

  public SurveyAnswerResponse percentAnswered(Double percentAnswered) {
    this.percentAnswered = percentAnswered;
    return this;
  }

   /**
   * Get percentAnswered
   * @return percentAnswered
  **/
  @ApiModelProperty(value = "")


  public Double getPercentAnswered() {
    return percentAnswered;
  }

  public void setPercentAnswered(Double percentAnswered) {
    this.percentAnswered = percentAnswered;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SurveyAnswerResponse surveyAnswerResponse = (SurveyAnswerResponse) o;
    return Objects.equals(this.answer, surveyAnswerResponse.answer) &&
        Objects.equals(this.conceptId, surveyAnswerResponse.conceptId) &&
        Objects.equals(this.participationCount, surveyAnswerResponse.participationCount) &&
        Objects.equals(this.percentAnswered, surveyAnswerResponse.percentAnswered);
  }

  @Override
  public int hashCode() {
    return Objects.hash(answer, conceptId, participationCount, percentAnswered);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SurveyAnswerResponse {\n");
    
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    participationCount: ").append(toIndentedString(participationCount)).append("\n");
    sb.append("    percentAnswered: ").append(toIndentedString(percentAnswered)).append("\n");
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

