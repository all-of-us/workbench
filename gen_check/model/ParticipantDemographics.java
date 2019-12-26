package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ConceptIdName;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantDemographics
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class ParticipantDemographics   {
  @JsonProperty("genderList")
  private List<ConceptIdName> genderList = new ArrayList<ConceptIdName>();

  @JsonProperty("raceList")
  private List<ConceptIdName> raceList = new ArrayList<ConceptIdName>();

  @JsonProperty("ethnicityList")
  private List<ConceptIdName> ethnicityList = new ArrayList<ConceptIdName>();

  public ParticipantDemographics genderList(List<ConceptIdName> genderList) {
    this.genderList = genderList;
    return this;
  }

  public ParticipantDemographics addGenderListItem(ConceptIdName genderListItem) {
    this.genderList.add(genderListItem);
    return this;
  }

   /**
   * Get genderList
   * @return genderList
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<ConceptIdName> getGenderList() {
    return genderList;
  }

  public void setGenderList(List<ConceptIdName> genderList) {
    this.genderList = genderList;
  }

  public ParticipantDemographics raceList(List<ConceptIdName> raceList) {
    this.raceList = raceList;
    return this;
  }

  public ParticipantDemographics addRaceListItem(ConceptIdName raceListItem) {
    this.raceList.add(raceListItem);
    return this;
  }

   /**
   * Get raceList
   * @return raceList
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<ConceptIdName> getRaceList() {
    return raceList;
  }

  public void setRaceList(List<ConceptIdName> raceList) {
    this.raceList = raceList;
  }

  public ParticipantDemographics ethnicityList(List<ConceptIdName> ethnicityList) {
    this.ethnicityList = ethnicityList;
    return this;
  }

  public ParticipantDemographics addEthnicityListItem(ConceptIdName ethnicityListItem) {
    this.ethnicityList.add(ethnicityListItem);
    return this;
  }

   /**
   * Get ethnicityList
   * @return ethnicityList
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<ConceptIdName> getEthnicityList() {
    return ethnicityList;
  }

  public void setEthnicityList(List<ConceptIdName> ethnicityList) {
    this.ethnicityList = ethnicityList;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantDemographics participantDemographics = (ParticipantDemographics) o;
    return Objects.equals(this.genderList, participantDemographics.genderList) &&
        Objects.equals(this.raceList, participantDemographics.raceList) &&
        Objects.equals(this.ethnicityList, participantDemographics.ethnicityList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(genderList, raceList, ethnicityList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantDemographics {\n");
    
    sb.append("    genderList: ").append(toIndentedString(genderList)).append("\n");
    sb.append("    raceList: ").append(toIndentedString(raceList)).append("\n");
    sb.append("    ethnicityList: ").append(toIndentedString(ethnicityList)).append("\n");
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

