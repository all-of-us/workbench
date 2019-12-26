package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantChartData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class ParticipantChartData   {
  @JsonProperty("standardName")
  private String standardName = null;

  @JsonProperty("standardVocabulary")
  private String standardVocabulary = null;

  @JsonProperty("startDate")
  private String startDate = null;

  @JsonProperty("ageAtEvent")
  private Integer ageAtEvent = null;

  @JsonProperty("rank")
  private Integer rank = null;

  public ParticipantChartData standardName(String standardName) {
    this.standardName = standardName;
    return this;
  }

   /**
   * the standard name
   * @return standardName
  **/
  @ApiModelProperty(required = true, value = "the standard name")
  @NotNull


  public String getStandardName() {
    return standardName;
  }

  public void setStandardName(String standardName) {
    this.standardName = standardName;
  }

  public ParticipantChartData standardVocabulary(String standardVocabulary) {
    this.standardVocabulary = standardVocabulary;
    return this;
  }

   /**
   * the standard vocabulary
   * @return standardVocabulary
  **/
  @ApiModelProperty(required = true, value = "the standard vocabulary")
  @NotNull


  public String getStandardVocabulary() {
    return standardVocabulary;
  }

  public void setStandardVocabulary(String standardVocabulary) {
    this.standardVocabulary = standardVocabulary;
  }

  public ParticipantChartData startDate(String startDate) {
    this.startDate = startDate;
    return this;
  }

   /**
   * the date of the event
   * @return startDate
  **/
  @ApiModelProperty(required = true, value = "the date of the event")
  @NotNull


  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public ParticipantChartData ageAtEvent(Integer ageAtEvent) {
    this.ageAtEvent = ageAtEvent;
    return this;
  }

   /**
   * The age at event for the participant
   * @return ageAtEvent
  **/
  @ApiModelProperty(required = true, value = "The age at event for the participant")
  @NotNull


  public Integer getAgeAtEvent() {
    return ageAtEvent;
  }

  public void setAgeAtEvent(Integer ageAtEvent) {
    this.ageAtEvent = ageAtEvent;
  }

  public ParticipantChartData rank(Integer rank) {
    this.rank = rank;
    return this;
  }

   /**
   * the rank of the data
   * @return rank
  **/
  @ApiModelProperty(required = true, value = "the rank of the data")
  @NotNull


  public Integer getRank() {
    return rank;
  }

  public void setRank(Integer rank) {
    this.rank = rank;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantChartData participantChartData = (ParticipantChartData) o;
    return Objects.equals(this.standardName, participantChartData.standardName) &&
        Objects.equals(this.standardVocabulary, participantChartData.standardVocabulary) &&
        Objects.equals(this.startDate, participantChartData.startDate) &&
        Objects.equals(this.ageAtEvent, participantChartData.ageAtEvent) &&
        Objects.equals(this.rank, participantChartData.rank);
  }

  @Override
  public int hashCode() {
    return Objects.hash(standardName, standardVocabulary, startDate, ageAtEvent, rank);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantChartData {\n");
    
    sb.append("    standardName: ").append(toIndentedString(standardName)).append("\n");
    sb.append("    standardVocabulary: ").append(toIndentedString(standardVocabulary)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    ageAtEvent: ").append(toIndentedString(ageAtEvent)).append("\n");
    sb.append("    rank: ").append(toIndentedString(rank)).append("\n");
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

