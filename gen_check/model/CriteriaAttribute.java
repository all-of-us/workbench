package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CriteriaAttribute
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class CriteriaAttribute   {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("valueAsConceptId")
  private Long valueAsConceptId = null;

  @JsonProperty("conceptName")
  private String conceptName = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("estCount")
  private String estCount = null;

  public CriteriaAttribute id(Long id) {
    this.id = id;
    return this;
  }

   /**
   * id of the criteria
   * @return id
  **/
  @ApiModelProperty(required = true, value = "id of the criteria")
  @NotNull


  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CriteriaAttribute valueAsConceptId(Long valueAsConceptId) {
    this.valueAsConceptId = valueAsConceptId;
    return this;
  }

   /**
   * categorical concept id
   * @return valueAsConceptId
  **/
  @ApiModelProperty(required = true, value = "categorical concept id")
  @NotNull


  public Long getValueAsConceptId() {
    return valueAsConceptId;
  }

  public void setValueAsConceptId(Long valueAsConceptId) {
    this.valueAsConceptId = valueAsConceptId;
  }

  public CriteriaAttribute conceptName(String conceptName) {
    this.conceptName = conceptName;
    return this;
  }

   /**
   * name of concept
   * @return conceptName
  **/
  @ApiModelProperty(required = true, value = "name of concept")
  @NotNull


  public String getConceptName() {
    return conceptName;
  }

  public void setConceptName(String conceptName) {
    this.conceptName = conceptName;
  }

  public CriteriaAttribute type(String type) {
    this.type = type;
    return this;
  }

   /**
   * numerical or categorical
   * @return type
  **/
  @ApiModelProperty(required = true, value = "numerical or categorical")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public CriteriaAttribute estCount(String estCount) {
    this.estCount = estCount;
    return this;
  }

   /**
   * possible count
   * @return estCount
  **/
  @ApiModelProperty(required = true, value = "possible count")
  @NotNull


  public String getEstCount() {
    return estCount;
  }

  public void setEstCount(String estCount) {
    this.estCount = estCount;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CriteriaAttribute criteriaAttribute = (CriteriaAttribute) o;
    return Objects.equals(this.id, criteriaAttribute.id) &&
        Objects.equals(this.valueAsConceptId, criteriaAttribute.valueAsConceptId) &&
        Objects.equals(this.conceptName, criteriaAttribute.conceptName) &&
        Objects.equals(this.type, criteriaAttribute.type) &&
        Objects.equals(this.estCount, criteriaAttribute.estCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, valueAsConceptId, conceptName, type, estCount);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CriteriaAttribute {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    valueAsConceptId: ").append(toIndentedString(valueAsConceptId)).append("\n");
    sb.append("    conceptName: ").append(toIndentedString(conceptName)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    estCount: ").append(toIndentedString(estCount)).append("\n");
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

