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
 * CohortAnnotationsResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class CohortAnnotationsResponse   {
  @JsonProperty("columns")
  private List<String> columns = null;

  @JsonProperty("results")
  private List<Object> results = new ArrayList<Object>();

  public CohortAnnotationsResponse columns(List<String> columns) {
    this.columns = columns;
    return this;
  }

  public CohortAnnotationsResponse addColumnsItem(String columnsItem) {
    if (this.columns == null) {
      this.columns = new ArrayList<String>();
    }
    this.columns.add(columnsItem);
    return this;
  }

   /**
   * An array of columns for the annotations being returned. 
   * @return columns
  **/
  @ApiModelProperty(value = "An array of columns for the annotations being returned. ")


  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public CohortAnnotationsResponse results(List<Object> results) {
    this.results = results;
    return this;
  }

  public CohortAnnotationsResponse addResultsItem(Object resultsItem) {
    this.results.add(resultsItem);
    return this;
  }

   /**
   * An array of JSON dictionaries, with each dictionary representing the requested annotations and/or review status for a single person. (In Java, this is represented as Map<String, Object>[]. In Python clients, this is a list[object] where each object is a dictionary. In Typescript clients, this is an Array<any> where each object is a dictionary.) Keys in the dictionaries will be \"person_id\", \"review_status\", or the name of an annotation. 
   * @return results
  **/
  @ApiModelProperty(required = true, value = "An array of JSON dictionaries, with each dictionary representing the requested annotations and/or review status for a single person. (In Java, this is represented as Map<String, Object>[]. In Python clients, this is a list[object] where each object is a dictionary. In Typescript clients, this is an Array<any> where each object is a dictionary.) Keys in the dictionaries will be \"person_id\", \"review_status\", or the name of an annotation. ")
  @NotNull


  public List<Object> getResults() {
    return results;
  }

  public void setResults(List<Object> results) {
    this.results = results;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortAnnotationsResponse cohortAnnotationsResponse = (CohortAnnotationsResponse) o;
    return Objects.equals(this.columns, cohortAnnotationsResponse.columns) &&
        Objects.equals(this.results, cohortAnnotationsResponse.results);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns, results);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortAnnotationsResponse {\n");
    
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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

