package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ResearchPurpose
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ResearchPurpose   {
  @JsonProperty("additionalNotes")
  private String additionalNotes = null;

  @JsonProperty("approved")
  private Boolean approved = null;

  @JsonProperty("ancestry")
  private Boolean ancestry = false;

  @JsonProperty("anticipatedFindings")
  private String anticipatedFindings = null;

  @JsonProperty("commercialPurpose")
  private Boolean commercialPurpose = false;

  @JsonProperty("controlSet")
  private Boolean controlSet = false;

  @JsonProperty("diseaseFocusedResearch")
  private Boolean diseaseFocusedResearch = false;

  @JsonProperty("diseaseOfFocus")
  private String diseaseOfFocus = null;

  @JsonProperty("drugDevelopment")
  private Boolean drugDevelopment = false;

  @JsonProperty("educational")
  private Boolean educational = false;

  @JsonProperty("intendedStudy")
  private String intendedStudy = null;

  @JsonProperty("methodsDevelopment")
  private Boolean methodsDevelopment = false;

  @JsonProperty("otherPopulationDetails")
  private String otherPopulationDetails = null;

  @JsonProperty("otherPurpose")
  private Boolean otherPurpose = false;

  @JsonProperty("otherPurposeDetails")
  private String otherPurposeDetails = null;

  @JsonProperty("population")
  private Boolean population = false;

  @JsonProperty("populationDetails")
  private List<SpecificPopulationEnum> populationDetails = null;

  @JsonProperty("populationHealth")
  private Boolean populationHealth = false;

  @JsonProperty("reasonForAllOfUs")
  private String reasonForAllOfUs = null;

  @JsonProperty("reviewRequested")
  private Boolean reviewRequested = false;

  @JsonProperty("socialBehavioral")
  private Boolean socialBehavioral = false;

  @JsonProperty("timeRequested")
  private Long timeRequested = null;

  @JsonProperty("timeReviewed")
  private Long timeReviewed = null;

  public ResearchPurpose additionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
    return this;
  }

   /**
   * Get additionalNotes
   * @return additionalNotes
  **/
  @ApiModelProperty(value = "")


  public String getAdditionalNotes() {
    return additionalNotes;
  }

  public void setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
  }

  public ResearchPurpose approved(Boolean approved) {
    this.approved = approved;
    return this;
  }

   /**
   * Get approved
   * @return approved
  **/
  @ApiModelProperty(value = "")


  public Boolean getApproved() {
    return approved;
  }

  public void setApproved(Boolean approved) {
    this.approved = approved;
  }

  public ResearchPurpose ancestry(Boolean ancestry) {
    this.ancestry = ancestry;
    return this;
  }

   /**
   * Get ancestry
   * @return ancestry
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getAncestry() {
    return ancestry;
  }

  public void setAncestry(Boolean ancestry) {
    this.ancestry = ancestry;
  }

  public ResearchPurpose anticipatedFindings(String anticipatedFindings) {
    this.anticipatedFindings = anticipatedFindings;
    return this;
  }

   /**
   * Get anticipatedFindings
   * @return anticipatedFindings
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getAnticipatedFindings() {
    return anticipatedFindings;
  }

  public void setAnticipatedFindings(String anticipatedFindings) {
    this.anticipatedFindings = anticipatedFindings;
  }

  public ResearchPurpose commercialPurpose(Boolean commercialPurpose) {
    this.commercialPurpose = commercialPurpose;
    return this;
  }

   /**
   * Get commercialPurpose
   * @return commercialPurpose
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getCommercialPurpose() {
    return commercialPurpose;
  }

  public void setCommercialPurpose(Boolean commercialPurpose) {
    this.commercialPurpose = commercialPurpose;
  }

  public ResearchPurpose controlSet(Boolean controlSet) {
    this.controlSet = controlSet;
    return this;
  }

   /**
   * Get controlSet
   * @return controlSet
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getControlSet() {
    return controlSet;
  }

  public void setControlSet(Boolean controlSet) {
    this.controlSet = controlSet;
  }

  public ResearchPurpose diseaseFocusedResearch(Boolean diseaseFocusedResearch) {
    this.diseaseFocusedResearch = diseaseFocusedResearch;
    return this;
  }

   /**
   * Get diseaseFocusedResearch
   * @return diseaseFocusedResearch
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getDiseaseFocusedResearch() {
    return diseaseFocusedResearch;
  }

  public void setDiseaseFocusedResearch(Boolean diseaseFocusedResearch) {
    this.diseaseFocusedResearch = diseaseFocusedResearch;
  }

  public ResearchPurpose diseaseOfFocus(String diseaseOfFocus) {
    this.diseaseOfFocus = diseaseOfFocus;
    return this;
  }

   /**
   * Get diseaseOfFocus
   * @return diseaseOfFocus
  **/
  @ApiModelProperty(value = "")


  public String getDiseaseOfFocus() {
    return diseaseOfFocus;
  }

  public void setDiseaseOfFocus(String diseaseOfFocus) {
    this.diseaseOfFocus = diseaseOfFocus;
  }

  public ResearchPurpose drugDevelopment(Boolean drugDevelopment) {
    this.drugDevelopment = drugDevelopment;
    return this;
  }

   /**
   * Get drugDevelopment
   * @return drugDevelopment
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getDrugDevelopment() {
    return drugDevelopment;
  }

  public void setDrugDevelopment(Boolean drugDevelopment) {
    this.drugDevelopment = drugDevelopment;
  }

  public ResearchPurpose educational(Boolean educational) {
    this.educational = educational;
    return this;
  }

   /**
   * Get educational
   * @return educational
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getEducational() {
    return educational;
  }

  public void setEducational(Boolean educational) {
    this.educational = educational;
  }

  public ResearchPurpose intendedStudy(String intendedStudy) {
    this.intendedStudy = intendedStudy;
    return this;
  }

   /**
   * Get intendedStudy
   * @return intendedStudy
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getIntendedStudy() {
    return intendedStudy;
  }

  public void setIntendedStudy(String intendedStudy) {
    this.intendedStudy = intendedStudy;
  }

  public ResearchPurpose methodsDevelopment(Boolean methodsDevelopment) {
    this.methodsDevelopment = methodsDevelopment;
    return this;
  }

   /**
   * Get methodsDevelopment
   * @return methodsDevelopment
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getMethodsDevelopment() {
    return methodsDevelopment;
  }

  public void setMethodsDevelopment(Boolean methodsDevelopment) {
    this.methodsDevelopment = methodsDevelopment;
  }

  public ResearchPurpose otherPopulationDetails(String otherPopulationDetails) {
    this.otherPopulationDetails = otherPopulationDetails;
    return this;
  }

   /**
   * Get otherPopulationDetails
   * @return otherPopulationDetails
  **/
  @ApiModelProperty(value = "")


  public String getOtherPopulationDetails() {
    return otherPopulationDetails;
  }

  public void setOtherPopulationDetails(String otherPopulationDetails) {
    this.otherPopulationDetails = otherPopulationDetails;
  }

  public ResearchPurpose otherPurpose(Boolean otherPurpose) {
    this.otherPurpose = otherPurpose;
    return this;
  }

   /**
   * Get otherPurpose
   * @return otherPurpose
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getOtherPurpose() {
    return otherPurpose;
  }

  public void setOtherPurpose(Boolean otherPurpose) {
    this.otherPurpose = otherPurpose;
  }

  public ResearchPurpose otherPurposeDetails(String otherPurposeDetails) {
    this.otherPurposeDetails = otherPurposeDetails;
    return this;
  }

   /**
   * Get otherPurposeDetails
   * @return otherPurposeDetails
  **/
  @ApiModelProperty(value = "")


  public String getOtherPurposeDetails() {
    return otherPurposeDetails;
  }

  public void setOtherPurposeDetails(String otherPurposeDetails) {
    this.otherPurposeDetails = otherPurposeDetails;
  }

  public ResearchPurpose population(Boolean population) {
    this.population = population;
    return this;
  }

   /**
   * Get population
   * @return population
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getPopulation() {
    return population;
  }

  public void setPopulation(Boolean population) {
    this.population = population;
  }

  public ResearchPurpose populationDetails(List<SpecificPopulationEnum> populationDetails) {
    this.populationDetails = populationDetails;
    return this;
  }

  public ResearchPurpose addPopulationDetailsItem(SpecificPopulationEnum populationDetailsItem) {
    if (this.populationDetails == null) {
      this.populationDetails = new ArrayList<SpecificPopulationEnum>();
    }
    this.populationDetails.add(populationDetailsItem);
    return this;
  }

   /**
   * Get populationDetails
   * @return populationDetails
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<SpecificPopulationEnum> getPopulationDetails() {
    return populationDetails;
  }

  public void setPopulationDetails(List<SpecificPopulationEnum> populationDetails) {
    this.populationDetails = populationDetails;
  }

  public ResearchPurpose populationHealth(Boolean populationHealth) {
    this.populationHealth = populationHealth;
    return this;
  }

   /**
   * Get populationHealth
   * @return populationHealth
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getPopulationHealth() {
    return populationHealth;
  }

  public void setPopulationHealth(Boolean populationHealth) {
    this.populationHealth = populationHealth;
  }

  public ResearchPurpose reasonForAllOfUs(String reasonForAllOfUs) {
    this.reasonForAllOfUs = reasonForAllOfUs;
    return this;
  }

   /**
   * Get reasonForAllOfUs
   * @return reasonForAllOfUs
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getReasonForAllOfUs() {
    return reasonForAllOfUs;
  }

  public void setReasonForAllOfUs(String reasonForAllOfUs) {
    this.reasonForAllOfUs = reasonForAllOfUs;
  }

  public ResearchPurpose reviewRequested(Boolean reviewRequested) {
    this.reviewRequested = reviewRequested;
    return this;
  }

   /**
   * Get reviewRequested
   * @return reviewRequested
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getReviewRequested() {
    return reviewRequested;
  }

  public void setReviewRequested(Boolean reviewRequested) {
    this.reviewRequested = reviewRequested;
  }

  public ResearchPurpose socialBehavioral(Boolean socialBehavioral) {
    this.socialBehavioral = socialBehavioral;
    return this;
  }

   /**
   * Get socialBehavioral
   * @return socialBehavioral
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getSocialBehavioral() {
    return socialBehavioral;
  }

  public void setSocialBehavioral(Boolean socialBehavioral) {
    this.socialBehavioral = socialBehavioral;
  }

  public ResearchPurpose timeRequested(Long timeRequested) {
    this.timeRequested = timeRequested;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return timeRequested
  **/
  @ApiModelProperty(value = "Milliseconds since the UNIX epoch.")


  public Long getTimeRequested() {
    return timeRequested;
  }

  public void setTimeRequested(Long timeRequested) {
    this.timeRequested = timeRequested;
  }

  public ResearchPurpose timeReviewed(Long timeReviewed) {
    this.timeReviewed = timeReviewed;
    return this;
  }

   /**
   * Milliseconds since the UNIX epoch.
   * @return timeReviewed
  **/
  @ApiModelProperty(value = "Milliseconds since the UNIX epoch.")


  public Long getTimeReviewed() {
    return timeReviewed;
  }

  public void setTimeReviewed(Long timeReviewed) {
    this.timeReviewed = timeReviewed;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResearchPurpose researchPurpose = (ResearchPurpose) o;
    return Objects.equals(this.additionalNotes, researchPurpose.additionalNotes) &&
        Objects.equals(this.approved, researchPurpose.approved) &&
        Objects.equals(this.ancestry, researchPurpose.ancestry) &&
        Objects.equals(this.anticipatedFindings, researchPurpose.anticipatedFindings) &&
        Objects.equals(this.commercialPurpose, researchPurpose.commercialPurpose) &&
        Objects.equals(this.controlSet, researchPurpose.controlSet) &&
        Objects.equals(this.diseaseFocusedResearch, researchPurpose.diseaseFocusedResearch) &&
        Objects.equals(this.diseaseOfFocus, researchPurpose.diseaseOfFocus) &&
        Objects.equals(this.drugDevelopment, researchPurpose.drugDevelopment) &&
        Objects.equals(this.educational, researchPurpose.educational) &&
        Objects.equals(this.intendedStudy, researchPurpose.intendedStudy) &&
        Objects.equals(this.methodsDevelopment, researchPurpose.methodsDevelopment) &&
        Objects.equals(this.otherPopulationDetails, researchPurpose.otherPopulationDetails) &&
        Objects.equals(this.otherPurpose, researchPurpose.otherPurpose) &&
        Objects.equals(this.otherPurposeDetails, researchPurpose.otherPurposeDetails) &&
        Objects.equals(this.population, researchPurpose.population) &&
        Objects.equals(this.populationDetails, researchPurpose.populationDetails) &&
        Objects.equals(this.populationHealth, researchPurpose.populationHealth) &&
        Objects.equals(this.reasonForAllOfUs, researchPurpose.reasonForAllOfUs) &&
        Objects.equals(this.reviewRequested, researchPurpose.reviewRequested) &&
        Objects.equals(this.socialBehavioral, researchPurpose.socialBehavioral) &&
        Objects.equals(this.timeRequested, researchPurpose.timeRequested) &&
        Objects.equals(this.timeReviewed, researchPurpose.timeReviewed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(additionalNotes, approved, ancestry, anticipatedFindings, commercialPurpose, controlSet, diseaseFocusedResearch, diseaseOfFocus, drugDevelopment, educational, intendedStudy, methodsDevelopment, otherPopulationDetails, otherPurpose, otherPurposeDetails, population, populationDetails, populationHealth, reasonForAllOfUs, reviewRequested, socialBehavioral, timeRequested, timeReviewed);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResearchPurpose {\n");
    
    sb.append("    additionalNotes: ").append(toIndentedString(additionalNotes)).append("\n");
    sb.append("    approved: ").append(toIndentedString(approved)).append("\n");
    sb.append("    ancestry: ").append(toIndentedString(ancestry)).append("\n");
    sb.append("    anticipatedFindings: ").append(toIndentedString(anticipatedFindings)).append("\n");
    sb.append("    commercialPurpose: ").append(toIndentedString(commercialPurpose)).append("\n");
    sb.append("    controlSet: ").append(toIndentedString(controlSet)).append("\n");
    sb.append("    diseaseFocusedResearch: ").append(toIndentedString(diseaseFocusedResearch)).append("\n");
    sb.append("    diseaseOfFocus: ").append(toIndentedString(diseaseOfFocus)).append("\n");
    sb.append("    drugDevelopment: ").append(toIndentedString(drugDevelopment)).append("\n");
    sb.append("    educational: ").append(toIndentedString(educational)).append("\n");
    sb.append("    intendedStudy: ").append(toIndentedString(intendedStudy)).append("\n");
    sb.append("    methodsDevelopment: ").append(toIndentedString(methodsDevelopment)).append("\n");
    sb.append("    otherPopulationDetails: ").append(toIndentedString(otherPopulationDetails)).append("\n");
    sb.append("    otherPurpose: ").append(toIndentedString(otherPurpose)).append("\n");
    sb.append("    otherPurposeDetails: ").append(toIndentedString(otherPurposeDetails)).append("\n");
    sb.append("    population: ").append(toIndentedString(population)).append("\n");
    sb.append("    populationDetails: ").append(toIndentedString(populationDetails)).append("\n");
    sb.append("    populationHealth: ").append(toIndentedString(populationHealth)).append("\n");
    sb.append("    reasonForAllOfUs: ").append(toIndentedString(reasonForAllOfUs)).append("\n");
    sb.append("    reviewRequested: ").append(toIndentedString(reviewRequested)).append("\n");
    sb.append("    socialBehavioral: ").append(toIndentedString(socialBehavioral)).append("\n");
    sb.append("    timeRequested: ").append(toIndentedString(timeRequested)).append("\n");
    sb.append("    timeReviewed: ").append(toIndentedString(timeReviewed)).append("\n");
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

