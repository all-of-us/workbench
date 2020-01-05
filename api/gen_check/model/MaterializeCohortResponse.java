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
 * MaterializeCohortResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class MaterializeCohortResponse   {
  @JsonProperty("results")
  private List<Object> results = new ArrayList<Object>();

  @JsonProperty("nextPageToken")
  private String nextPageToken = null;

  public MaterializeCohortResponse results(List<Object> results) {
    this.results = results;
    return this;
  }

  public MaterializeCohortResponse addResultsItem(Object resultsItem) {
    this.results.add(resultsItem);
    return this;
  }

   /**
   * An array of JSON dictionaries representing results to the cohort materialization. (In Java, this is represented as Map<String, Object>[]. In Python clients, this is a list[object] where each object is a dictionary. In Typescript clients, this is an Array<any> where each object is a dictionary.) Keys in the dictionaries will be the columns selected in the field set provided in the request, and the values will be the values of those columns. 
   * @return results
  **/
  @ApiModelProperty(required = true, value = "An array of JSON dictionaries representing results to the cohort materialization. (In Java, this is represented as Map<String, Object>[]. In Python clients, this is a list[object] where each object is a dictionary. In Typescript clients, this is an Array<any> where each object is a dictionary.) Keys in the dictionaries will be the columns selected in the field set provided in the request, and the values will be the values of those columns. ")
  @NotNull


  public List<Object> getResults() {
    return results;
  }

  public void setResults(List<Object> results) {
    this.results = results;
  }

  public MaterializeCohortResponse nextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
    return this;
  }

   /**
   * Pagination token that can be used in a subsequent call to MaterializeCohortRequest to retrieve more results. If not set, there are no more results to retrieve. 
   * @return nextPageToken
  **/
  @ApiModelProperty(value = "Pagination token that can be used in a subsequent call to MaterializeCohortRequest to retrieve more results. If not set, there are no more results to retrieve. ")


  public String getNextPageToken() {
    return nextPageToken;
  }

  public void setNextPageToken(String nextPageToken) {
    this.nextPageToken = nextPageToken;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaterializeCohortResponse materializeCohortResponse = (MaterializeCohortResponse) o;
    return Objects.equals(this.results, materializeCohortResponse.results) &&
        Objects.equals(this.nextPageToken, materializeCohortResponse.nextPageToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(results, nextPageToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MaterializeCohortResponse {\n");
    
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
    sb.append("    nextPageToken: ").append(toIndentedString(nextPageToken)).append("\n");
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

