package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.SearchGroup;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * The SearchRequest describes the state of the Cohort Builder at any given moment. It contains two keys, &#x60;include&#x60; and &#x60;exclude&#x60;, each of which specifies an array of SearchGroups which are &#x60;AND&#x60;ed together, and which collectively specify which subjects to include or exclude from the cohort. 
 */
@ApiModel(description = "The SearchRequest describes the state of the Cohort Builder at any given moment. It contains two keys, `include` and `exclude`, each of which specifies an array of SearchGroups which are `AND`ed together, and which collectively specify which subjects to include or exclude from the cohort. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class SearchRequest   {
  @JsonProperty("includes")
  private List<SearchGroup> includes = new ArrayList<SearchGroup>();

  @JsonProperty("excludes")
  private List<SearchGroup> excludes = new ArrayList<SearchGroup>();

  public SearchRequest includes(List<SearchGroup> includes) {
    this.includes = includes;
    return this;
  }

  public SearchRequest addIncludesItem(SearchGroup includesItem) {
    this.includes.add(includesItem);
    return this;
  }

   /**
   * Get includes
   * @return includes
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<SearchGroup> getIncludes() {
    return includes;
  }

  public void setIncludes(List<SearchGroup> includes) {
    this.includes = includes;
  }

  public SearchRequest excludes(List<SearchGroup> excludes) {
    this.excludes = excludes;
    return this;
  }

  public SearchRequest addExcludesItem(SearchGroup excludesItem) {
    this.excludes.add(excludesItem);
    return this;
  }

   /**
   * Get excludes
   * @return excludes
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public List<SearchGroup> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<SearchGroup> excludes) {
    this.excludes = excludes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchRequest searchRequest = (SearchRequest) o;
    return Objects.equals(this.includes, searchRequest.includes) &&
        Objects.equals(this.excludes, searchRequest.excludes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includes, excludes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchRequest {\n");
    
    sb.append("    includes: ").append(toIndentedString(includes)).append("\n");
    sb.append("    excludes: ").append(toIndentedString(excludes)).append("\n");
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

