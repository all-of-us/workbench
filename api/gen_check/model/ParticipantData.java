package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class ParticipantData   {
  @JsonProperty("itemDate")
  private String itemDate = null;

  @JsonProperty("domain")
  private String domain = null;

  @JsonProperty("standardName")
  private String standardName = null;

  @JsonProperty("ageAtEvent")
  private Integer ageAtEvent = null;

  @JsonProperty("standardConceptId")
  private Long standardConceptId = null;

  @JsonProperty("sourceConceptId")
  private Long sourceConceptId = null;

  @JsonProperty("sourceVocabulary")
  private String sourceVocabulary = null;

  @JsonProperty("standardVocabulary")
  private String standardVocabulary = null;

  @JsonProperty("sourceName")
  private String sourceName = null;

  @JsonProperty("sourceCode")
  private String sourceCode = null;

  @JsonProperty("standardCode")
  private String standardCode = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("visitType")
  private String visitType = null;

  @JsonProperty("numMentions")
  private String numMentions = null;

  @JsonProperty("firstMention")
  private String firstMention = null;

  @JsonProperty("lastMention")
  private String lastMention = null;

  @JsonProperty("unit")
  private String unit = null;

  @JsonProperty("dose")
  private String dose = null;

  @JsonProperty("strength")
  private String strength = null;

  @JsonProperty("route")
  private String route = null;

  @JsonProperty("refRange")
  private String refRange = null;

  @JsonProperty("survey")
  private String survey = null;

  @JsonProperty("question")
  private String question = null;

  @JsonProperty("answer")
  private String answer = null;

  public ParticipantData itemDate(String itemDate) {
    this.itemDate = itemDate;
    return this;
  }

   /**
   * The date of the event
   * @return itemDate
  **/
  @ApiModelProperty(required = true, value = "The date of the event")
  @NotNull


  public String getItemDate() {
    return itemDate;
  }

  public void setItemDate(String itemDate) {
    this.itemDate = itemDate;
  }

  public ParticipantData domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * the domain of this data.
   * @return domain
  **/
  @ApiModelProperty(value = "the domain of this data.")


  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public ParticipantData standardName(String standardName) {
    this.standardName = standardName;
    return this;
  }

   /**
   * The standard name of this data
   * @return standardName
  **/
  @ApiModelProperty(value = "The standard name of this data")


  public String getStandardName() {
    return standardName;
  }

  public void setStandardName(String standardName) {
    this.standardName = standardName;
  }

  public ParticipantData ageAtEvent(Integer ageAtEvent) {
    this.ageAtEvent = ageAtEvent;
    return this;
  }

   /**
   * The age at event for the participant
   * @return ageAtEvent
  **/
  @ApiModelProperty(value = "The age at event for the participant")


  public Integer getAgeAtEvent() {
    return ageAtEvent;
  }

  public void setAgeAtEvent(Integer ageAtEvent) {
    this.ageAtEvent = ageAtEvent;
  }

  public ParticipantData standardConceptId(Long standardConceptId) {
    this.standardConceptId = standardConceptId;
    return this;
  }

   /**
   * The standard concept id
   * @return standardConceptId
  **/
  @ApiModelProperty(value = "The standard concept id")


  public Long getStandardConceptId() {
    return standardConceptId;
  }

  public void setStandardConceptId(Long standardConceptId) {
    this.standardConceptId = standardConceptId;
  }

  public ParticipantData sourceConceptId(Long sourceConceptId) {
    this.sourceConceptId = sourceConceptId;
    return this;
  }

   /**
   * The source concept id
   * @return sourceConceptId
  **/
  @ApiModelProperty(value = "The source concept id")


  public Long getSourceConceptId() {
    return sourceConceptId;
  }

  public void setSourceConceptId(Long sourceConceptId) {
    this.sourceConceptId = sourceConceptId;
  }

  public ParticipantData sourceVocabulary(String sourceVocabulary) {
    this.sourceVocabulary = sourceVocabulary;
    return this;
  }

   /**
   * The source vocabulary type of this data
   * @return sourceVocabulary
  **/
  @ApiModelProperty(value = "The source vocabulary type of this data")


  public String getSourceVocabulary() {
    return sourceVocabulary;
  }

  public void setSourceVocabulary(String sourceVocabulary) {
    this.sourceVocabulary = sourceVocabulary;
  }

  public ParticipantData standardVocabulary(String standardVocabulary) {
    this.standardVocabulary = standardVocabulary;
    return this;
  }

   /**
   * The standard vocabulary of this data
   * @return standardVocabulary
  **/
  @ApiModelProperty(value = "The standard vocabulary of this data")


  public String getStandardVocabulary() {
    return standardVocabulary;
  }

  public void setStandardVocabulary(String standardVocabulary) {
    this.standardVocabulary = standardVocabulary;
  }

  public ParticipantData sourceName(String sourceName) {
    this.sourceName = sourceName;
    return this;
  }

   /**
   * The source name of this data
   * @return sourceName
  **/
  @ApiModelProperty(value = "The source name of this data")


  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public ParticipantData sourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
    return this;
  }

   /**
   * The source code of this data
   * @return sourceCode
  **/
  @ApiModelProperty(value = "The source code of this data")


  public String getSourceCode() {
    return sourceCode;
  }

  public void setSourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
  }

  public ParticipantData standardCode(String standardCode) {
    this.standardCode = standardCode;
    return this;
  }

   /**
   * The standard code of this data
   * @return standardCode
  **/
  @ApiModelProperty(value = "The standard code of this data")


  public String getStandardCode() {
    return standardCode;
  }

  public void setStandardCode(String standardCode) {
    this.standardCode = standardCode;
  }

  public ParticipantData value(String value) {
    this.value = value;
    return this;
  }

   /**
   * The source value of this data
   * @return value
  **/
  @ApiModelProperty(value = "The source value of this data")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public ParticipantData visitType(String visitType) {
    this.visitType = visitType;
    return this;
  }

   /**
   * The visit type of this data
   * @return visitType
  **/
  @ApiModelProperty(value = "The visit type of this data")


  public String getVisitType() {
    return visitType;
  }

  public void setVisitType(String visitType) {
    this.visitType = visitType;
  }

  public ParticipantData numMentions(String numMentions) {
    this.numMentions = numMentions;
    return this;
  }

   /**
   * The number of mentions of this data
   * @return numMentions
  **/
  @ApiModelProperty(value = "The number of mentions of this data")


  public String getNumMentions() {
    return numMentions;
  }

  public void setNumMentions(String numMentions) {
    this.numMentions = numMentions;
  }

  public ParticipantData firstMention(String firstMention) {
    this.firstMention = firstMention;
    return this;
  }

   /**
   * The date of first mention of this data
   * @return firstMention
  **/
  @ApiModelProperty(value = "The date of first mention of this data")


  public String getFirstMention() {
    return firstMention;
  }

  public void setFirstMention(String firstMention) {
    this.firstMention = firstMention;
  }

  public ParticipantData lastMention(String lastMention) {
    this.lastMention = lastMention;
    return this;
  }

   /**
   * The date of last mention of this data
   * @return lastMention
  **/
  @ApiModelProperty(value = "The date of last mention of this data")


  public String getLastMention() {
    return lastMention;
  }

  public void setLastMention(String lastMention) {
    this.lastMention = lastMention;
  }

  public ParticipantData unit(String unit) {
    this.unit = unit;
    return this;
  }

   /**
   * The unit of this data
   * @return unit
  **/
  @ApiModelProperty(value = "The unit of this data")


  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public ParticipantData dose(String dose) {
    this.dose = dose;
    return this;
  }

   /**
   * The quantity of this data
   * @return dose
  **/
  @ApiModelProperty(value = "The quantity of this data")


  public String getDose() {
    return dose;
  }

  public void setDose(String dose) {
    this.dose = dose;
  }

  public ParticipantData strength(String strength) {
    this.strength = strength;
    return this;
  }

   /**
   * The strength of this data
   * @return strength
  **/
  @ApiModelProperty(value = "The strength of this data")


  public String getStrength() {
    return strength;
  }

  public void setStrength(String strength) {
    this.strength = strength;
  }

  public ParticipantData route(String route) {
    this.route = route;
    return this;
  }

   /**
   * The route of this data
   * @return route
  **/
  @ApiModelProperty(value = "The route of this data")


  public String getRoute() {
    return route;
  }

  public void setRoute(String route) {
    this.route = route;
  }

  public ParticipantData refRange(String refRange) {
    this.refRange = refRange;
    return this;
  }

   /**
   * The reference range of this data
   * @return refRange
  **/
  @ApiModelProperty(value = "The reference range of this data")


  public String getRefRange() {
    return refRange;
  }

  public void setRefRange(String refRange) {
    this.refRange = refRange;
  }

  public ParticipantData survey(String survey) {
    this.survey = survey;
    return this;
  }

   /**
   * The name of this survey
   * @return survey
  **/
  @ApiModelProperty(value = "The name of this survey")


  public String getSurvey() {
    return survey;
  }

  public void setSurvey(String survey) {
    this.survey = survey;
  }

  public ParticipantData question(String question) {
    this.question = question;
    return this;
  }

   /**
   * a question in this survey
   * @return question
  **/
  @ApiModelProperty(value = "a question in this survey")


  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public ParticipantData answer(String answer) {
    this.answer = answer;
    return this;
  }

   /**
   * an answer in this survey
   * @return answer
  **/
  @ApiModelProperty(value = "an answer in this survey")


  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantData participantData = (ParticipantData) o;
    return Objects.equals(this.itemDate, participantData.itemDate) &&
        Objects.equals(this.domain, participantData.domain) &&
        Objects.equals(this.standardName, participantData.standardName) &&
        Objects.equals(this.ageAtEvent, participantData.ageAtEvent) &&
        Objects.equals(this.standardConceptId, participantData.standardConceptId) &&
        Objects.equals(this.sourceConceptId, participantData.sourceConceptId) &&
        Objects.equals(this.sourceVocabulary, participantData.sourceVocabulary) &&
        Objects.equals(this.standardVocabulary, participantData.standardVocabulary) &&
        Objects.equals(this.sourceName, participantData.sourceName) &&
        Objects.equals(this.sourceCode, participantData.sourceCode) &&
        Objects.equals(this.standardCode, participantData.standardCode) &&
        Objects.equals(this.value, participantData.value) &&
        Objects.equals(this.visitType, participantData.visitType) &&
        Objects.equals(this.numMentions, participantData.numMentions) &&
        Objects.equals(this.firstMention, participantData.firstMention) &&
        Objects.equals(this.lastMention, participantData.lastMention) &&
        Objects.equals(this.unit, participantData.unit) &&
        Objects.equals(this.dose, participantData.dose) &&
        Objects.equals(this.strength, participantData.strength) &&
        Objects.equals(this.route, participantData.route) &&
        Objects.equals(this.refRange, participantData.refRange) &&
        Objects.equals(this.survey, participantData.survey) &&
        Objects.equals(this.question, participantData.question) &&
        Objects.equals(this.answer, participantData.answer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemDate, domain, standardName, ageAtEvent, standardConceptId, sourceConceptId, sourceVocabulary, standardVocabulary, sourceName, sourceCode, standardCode, value, visitType, numMentions, firstMention, lastMention, unit, dose, strength, route, refRange, survey, question, answer);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantData {\n");
    
    sb.append("    itemDate: ").append(toIndentedString(itemDate)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    standardName: ").append(toIndentedString(standardName)).append("\n");
    sb.append("    ageAtEvent: ").append(toIndentedString(ageAtEvent)).append("\n");
    sb.append("    standardConceptId: ").append(toIndentedString(standardConceptId)).append("\n");
    sb.append("    sourceConceptId: ").append(toIndentedString(sourceConceptId)).append("\n");
    sb.append("    sourceVocabulary: ").append(toIndentedString(sourceVocabulary)).append("\n");
    sb.append("    standardVocabulary: ").append(toIndentedString(standardVocabulary)).append("\n");
    sb.append("    sourceName: ").append(toIndentedString(sourceName)).append("\n");
    sb.append("    sourceCode: ").append(toIndentedString(sourceCode)).append("\n");
    sb.append("    standardCode: ").append(toIndentedString(standardCode)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    visitType: ").append(toIndentedString(visitType)).append("\n");
    sb.append("    numMentions: ").append(toIndentedString(numMentions)).append("\n");
    sb.append("    firstMention: ").append(toIndentedString(firstMention)).append("\n");
    sb.append("    lastMention: ").append(toIndentedString(lastMention)).append("\n");
    sb.append("    unit: ").append(toIndentedString(unit)).append("\n");
    sb.append("    dose: ").append(toIndentedString(dose)).append("\n");
    sb.append("    strength: ").append(toIndentedString(strength)).append("\n");
    sb.append("    route: ").append(toIndentedString(route)).append("\n");
    sb.append("    refRange: ").append(toIndentedString(refRange)).append("\n");
    sb.append("    survey: ").append(toIndentedString(survey)).append("\n");
    sb.append("    question: ").append(toIndentedString(question)).append("\n");
    sb.append("    answer: ").append(toIndentedString(answer)).append("\n");
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

