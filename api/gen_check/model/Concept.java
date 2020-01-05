package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A concept describing a type of entity (e.g. measurement, observation, procedure.)
 */
@ApiModel(description = "A concept describing a type of entity (e.g. measurement, observation, procedure.)")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class Concept   {
  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("conceptName")
  private String conceptName = null;

  @JsonProperty("domainId")
  private String domainId = null;

  @JsonProperty("vocabularyId")
  private String vocabularyId = null;

  @JsonProperty("conceptCode")
  private String conceptCode = null;

  @JsonProperty("conceptClassId")
  private String conceptClassId = null;

  @JsonProperty("standardConcept")
  private Boolean standardConcept = false;

  @JsonProperty("countValue")
  private Long countValue = null;

  @JsonProperty("prevalence")
  private Float prevalence = null;

  @JsonProperty("conceptSynonyms")
  private List<String> conceptSynonyms = null;

  public Concept conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * ID of the concept
   * @return conceptId
  **/
  @ApiModelProperty(required = true, value = "ID of the concept")
  @NotNull


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public Concept conceptName(String conceptName) {
    this.conceptName = conceptName;
    return this;
  }

   /**
   * Name of the concept
   * @return conceptName
  **/
  @ApiModelProperty(required = true, value = "Name of the concept")
  @NotNull


  public String getConceptName() {
    return conceptName;
  }

  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }

  public Concept domainId(String domainId) {
    this.domainId = domainId;
    return this;
  }

   /**
   * Domain ID of the concept (e.g. Observation)
   * @return domainId
  **/
  @ApiModelProperty(required = true, value = "Domain ID of the concept (e.g. Observation)")
  @NotNull


  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public Concept vocabularyId(String vocabularyId) {
    this.vocabularyId = vocabularyId;
    return this;
  }

   /**
   * Vocabulary ID of the concept (e.g. SNOMED)
   * @return vocabularyId
  **/
  @ApiModelProperty(required = true, value = "Vocabulary ID of the concept (e.g. SNOMED)")
  @NotNull


  public String getVocabularyId() {
    return vocabularyId;
  }

  public void setVocabularyId(String vocabularyId) {
    this.vocabularyId = vocabularyId;
  }

  public Concept conceptCode(String conceptCode) {
    this.conceptCode = conceptCode;
    return this;
  }

   /**
   * Code for the concept in its vocabulary (e.g. G8107)
   * @return conceptCode
  **/
  @ApiModelProperty(required = true, value = "Code for the concept in its vocabulary (e.g. G8107)")
  @NotNull


  public String getConceptCode() {
    return conceptCode;
  }

  public void setConceptCode(String conceptCode) {
    this.conceptCode = conceptCode;
  }

  public Concept conceptClassId(String conceptClassId) {
    this.conceptClassId = conceptClassId;
    return this;
  }

   /**
   * Class of the concept (e.g. Ingredient)
   * @return conceptClassId
  **/
  @ApiModelProperty(required = true, value = "Class of the concept (e.g. Ingredient)")
  @NotNull


  public String getConceptClassId() {
    return conceptClassId;
  }

  public void setConceptClassId(String conceptClassId) {
    this.conceptClassId = conceptClassId;
  }

  public Concept standardConcept(Boolean standardConcept) {
    this.standardConcept = standardConcept;
    return this;
  }

   /**
   * True if this is a standard concept, false otherwise
   * @return standardConcept
  **/
  @ApiModelProperty(required = true, value = "True if this is a standard concept, false otherwise")
  @NotNull


  public Boolean getStandardConcept() {
    return standardConcept;
  }

  public void setStandardConcept(Boolean standardConcept) {
    this.standardConcept = standardConcept;
  }

  public Concept countValue(Long countValue) {
    this.countValue = countValue;
    return this;
  }

   /**
   * Count of participants matching this concept in the CDR
   * @return countValue
  **/
  @ApiModelProperty(required = true, value = "Count of participants matching this concept in the CDR")
  @NotNull


  public Long getCountValue() {
    return countValue;
  }

  public void setCountValue(Long countValue) {
    this.countValue = countValue;
  }

  public Concept prevalence(Float prevalence) {
    this.prevalence = prevalence;
    return this;
  }

   /**
   * Prevalence among participants in the CDR (a percentage of the total)
   * @return prevalence
  **/
  @ApiModelProperty(required = true, value = "Prevalence among participants in the CDR (a percentage of the total)")
  @NotNull


  public Float getPrevalence() {
    return prevalence;
  }

  public void setPrevalence(Float prevalence) {
    this.prevalence = prevalence;
  }

  public Concept conceptSynonyms(List<String> conceptSynonyms) {
    this.conceptSynonyms = conceptSynonyms;
    return this;
  }

  public Concept addConceptSynonymsItem(String conceptSynonymsItem) {
    if (this.conceptSynonyms == null) {
      this.conceptSynonyms = new ArrayList<String>();
    }
    this.conceptSynonyms.add(conceptSynonymsItem);
    return this;
  }

   /**
   * concept synonym names
   * @return conceptSynonyms
  **/
  @ApiModelProperty(value = "concept synonym names")


  public List<String> getConceptSynonyms() {
    return conceptSynonyms;
  }

  public void setConceptSynonyms(List<String> conceptSynonyms) {
    this.conceptSynonyms = conceptSynonyms;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Concept concept = (Concept) o;
    return Objects.equals(this.conceptId, concept.conceptId) &&
        Objects.equals(this.conceptName, concept.conceptName) &&
        Objects.equals(this.domainId, concept.domainId) &&
        Objects.equals(this.vocabularyId, concept.vocabularyId) &&
        Objects.equals(this.conceptCode, concept.conceptCode) &&
        Objects.equals(this.conceptClassId, concept.conceptClassId) &&
        Objects.equals(this.standardConcept, concept.standardConcept) &&
        Objects.equals(this.countValue, concept.countValue) &&
        Objects.equals(this.prevalence, concept.prevalence) &&
        Objects.equals(this.conceptSynonyms, concept.conceptSynonyms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, conceptName, domainId, vocabularyId, conceptCode, conceptClassId, standardConcept, countValue, prevalence, conceptSynonyms);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Concept {\n");
    
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    conceptName: ").append(toIndentedString(conceptName)).append("\n");
    sb.append("    domainId: ").append(toIndentedString(domainId)).append("\n");
    sb.append("    vocabularyId: ").append(toIndentedString(vocabularyId)).append("\n");
    sb.append("    conceptCode: ").append(toIndentedString(conceptCode)).append("\n");
    sb.append("    conceptClassId: ").append(toIndentedString(conceptClassId)).append("\n");
    sb.append("    standardConcept: ").append(toIndentedString(standardConcept)).append("\n");
    sb.append("    countValue: ").append(toIndentedString(countValue)).append("\n");
    sb.append("    prevalence: ").append(toIndentedString(prevalence)).append("\n");
    sb.append("    conceptSynonyms: ").append(toIndentedString(conceptSynonyms)).append("\n");
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

