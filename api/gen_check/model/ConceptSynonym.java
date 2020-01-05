package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * concept synonym
 */
@ApiModel(description = "concept synonym")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class ConceptSynonym   {
  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("conceptSynonymName")
  private String conceptSynonymName = null;

  @JsonProperty("languageConceptId")
  private Long languageConceptId = null;

  public ConceptSynonym conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * concept id
   * @return conceptId
  **/
  @ApiModelProperty(required = true, value = "concept id")
  @NotNull


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public ConceptSynonym conceptSynonymName(String conceptSynonymName) {
    this.conceptSynonymName = conceptSynonymName;
    return this;
  }

   /**
   * concept synonym name
   * @return conceptSynonymName
  **/
  @ApiModelProperty(required = true, value = "concept synonym name")
  @NotNull


  public String getConceptSynonymName() {
    return conceptSynonymName;
  }

  public void setConceptSynonymName(String conceptSynonymName) {
    this.conceptSynonymName = conceptSynonymName;
  }

  public ConceptSynonym languageConceptId(Long languageConceptId) {
    this.languageConceptId = languageConceptId;
    return this;
  }

   /**
   * language concept id
   * @return languageConceptId
  **/
  @ApiModelProperty(required = true, value = "language concept id")
  @NotNull


  public Long getLanguageConceptId() {
    return languageConceptId;
  }

  public void setLanguageConceptId(Long languageConceptId) {
    this.languageConceptId = languageConceptId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptSynonym conceptSynonym = (ConceptSynonym) o;
    return Objects.equals(this.conceptId, conceptSynonym.conceptId) &&
        Objects.equals(this.conceptSynonymName, conceptSynonym.conceptSynonymName) &&
        Objects.equals(this.languageConceptId, conceptSynonym.languageConceptId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, conceptSynonymName, languageConceptId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConceptSynonym {\n");
    
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    conceptSynonymName: ").append(toIndentedString(conceptSynonymName)).append("\n");
    sb.append("    languageConceptId: ").append(toIndentedString(languageConceptId)).append("\n");
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

