package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.AnnotationType;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortAnnotationDefinition
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class CohortAnnotationDefinition   {
  @JsonProperty("cohortAnnotationDefinitionId")
  private Long cohortAnnotationDefinitionId = null;

  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("cohortId")
  private Long cohortId = null;

  @JsonProperty("columnName")
  private String columnName = null;

  @JsonProperty("annotationType")
  private AnnotationType annotationType = null;

  @JsonProperty("enumValues")
  private List<String> enumValues = null;

  public CohortAnnotationDefinition cohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
    return this;
  }

   /**
   * the annotation definition id.
   * @return cohortAnnotationDefinitionId
  **/
  @ApiModelProperty(value = "the annotation definition id.")


  public Long getCohortAnnotationDefinitionId() {
    return cohortAnnotationDefinitionId;
  }

  public void setCohortAnnotationDefinitionId(Long cohortAnnotationDefinitionId) {
    this.cohortAnnotationDefinitionId = cohortAnnotationDefinitionId;
  }

  public CohortAnnotationDefinition etag(String etag) {
    this.etag = etag;
    return this;
  }

   /**
   * Entity tag for optimistic concurrency control. To be set during a read-modify-write to ensure that the client has not attempted to modify a changed resource. 
   * @return etag
  **/
  @ApiModelProperty(value = "Entity tag for optimistic concurrency control. To be set during a read-modify-write to ensure that the client has not attempted to modify a changed resource. ")


  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public CohortAnnotationDefinition cohortId(Long cohortId) {
    this.cohortId = cohortId;
    return this;
  }

   /**
   * the cohort id.
   * @return cohortId
  **/
  @ApiModelProperty(required = true, value = "the cohort id.")
  @NotNull


  public Long getCohortId() {
    return cohortId;
  }

  public void setCohortId(Long cohortId) {
    this.cohortId = cohortId;
  }

  public CohortAnnotationDefinition columnName(String columnName) {
    this.columnName = columnName;
    return this;
  }

   /**
   * the name of this annotation.
   * @return columnName
  **/
  @ApiModelProperty(required = true, value = "the name of this annotation.")
  @NotNull


  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
  }

  public CohortAnnotationDefinition annotationType(AnnotationType annotationType) {
    this.annotationType = annotationType;
    return this;
  }

   /**
   * Get annotationType
   * @return annotationType
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public AnnotationType getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationType(AnnotationType annotationType) {
    this.annotationType = annotationType;
  }

  public CohortAnnotationDefinition enumValues(List<String> enumValues) {
    this.enumValues = enumValues;
    return this;
  }

  public CohortAnnotationDefinition addEnumValuesItem(String enumValuesItem) {
    if (this.enumValues == null) {
      this.enumValues = new ArrayList<String>();
    }
    this.enumValues.add(enumValuesItem);
    return this;
  }

   /**
   * Get enumValues
   * @return enumValues
  **/
  @ApiModelProperty(value = "")


  public List<String> getEnumValues() {
    return enumValues;
  }

  public void setEnumValues(List<String> enumValues) {
    this.enumValues = enumValues;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortAnnotationDefinition cohortAnnotationDefinition = (CohortAnnotationDefinition) o;
    return Objects.equals(this.cohortAnnotationDefinitionId, cohortAnnotationDefinition.cohortAnnotationDefinitionId) &&
        Objects.equals(this.etag, cohortAnnotationDefinition.etag) &&
        Objects.equals(this.cohortId, cohortAnnotationDefinition.cohortId) &&
        Objects.equals(this.columnName, cohortAnnotationDefinition.columnName) &&
        Objects.equals(this.annotationType, cohortAnnotationDefinition.annotationType) &&
        Objects.equals(this.enumValues, cohortAnnotationDefinition.enumValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortAnnotationDefinitionId, etag, cohortId, columnName, annotationType, enumValues);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortAnnotationDefinition {\n");
    
    sb.append("    cohortAnnotationDefinitionId: ").append(toIndentedString(cohortAnnotationDefinitionId)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    columnName: ").append(toIndentedString(columnName)).append("\n");
    sb.append("    annotationType: ").append(toIndentedString(annotationType)).append("\n");
    sb.append("    enumValues: ").append(toIndentedString(enumValues)).append("\n");
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

