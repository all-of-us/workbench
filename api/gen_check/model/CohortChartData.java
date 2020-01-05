package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortChartData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class CohortChartData   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("count")
  private Long count = null;

  public CohortChartData name(String name) {
    this.name = name;
    return this;
  }

   /**
   * the name of this data
   * @return name
  **/
  @ApiModelProperty(required = true, value = "the name of this data")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public CohortChartData conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * the source concept id for this data
   * @return conceptId
  **/
  @ApiModelProperty(required = true, value = "the source concept id for this data")
  @NotNull


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public CohortChartData count(Long count) {
    this.count = count;
    return this;
  }

   /**
   * the count for this data
   * @return count
  **/
  @ApiModelProperty(required = true, value = "the count for this data")
  @NotNull


  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortChartData cohortChartData = (CohortChartData) o;
    return Objects.equals(this.name, cohortChartData.name) &&
        Objects.equals(this.conceptId, cohortChartData.conceptId) &&
        Objects.equals(this.count, cohortChartData.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, conceptId, count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortChartData {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

