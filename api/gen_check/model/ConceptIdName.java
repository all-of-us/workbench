package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ConceptIdName
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ConceptIdName   {
  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("conceptName")
  private String conceptName = null;

  public ConceptIdName conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * Get conceptId
   * @return conceptId
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public ConceptIdName conceptName(String conceptName) {
    this.conceptName = conceptName;
    return this;
  }

   /**
   * Get conceptName
   * @return conceptName
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getConceptName() {
    return conceptName;
  }

  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptIdName conceptIdName = (ConceptIdName) o;
    return Objects.equals(this.conceptId, conceptIdName.conceptId) &&
        Objects.equals(this.conceptName, conceptIdName.conceptName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptId, conceptName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConceptIdName {\n");
    
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    conceptName: ").append(toIndentedString(conceptName)).append("\n");
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

