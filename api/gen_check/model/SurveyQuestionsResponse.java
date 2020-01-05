package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SurveyQuestionsResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class SurveyQuestionsResponse   {
  @JsonProperty("question")
  private String question = null;

  @JsonProperty("conceptId")
  private Long conceptId = null;

  public SurveyQuestionsResponse question(String question) {
    this.question = question;
    return this;
  }

   /**
   * Get question
   * @return question
  **/
  @ApiModelProperty(value = "")


  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public SurveyQuestionsResponse conceptId(Long conceptId) {
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


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SurveyQuestionsResponse surveyQuestionsResponse = (SurveyQuestionsResponse) o;
    return Objects.equals(this.question, surveyQuestionsResponse.question) &&
        Objects.equals(this.conceptId, surveyQuestionsResponse.conceptId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(question, conceptId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SurveyQuestionsResponse {\n");
    
    sb.append("    question: ").append(toIndentedString(question)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
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

