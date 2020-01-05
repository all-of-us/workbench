package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.ResultFilters;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A list of filters applied to the results of a query. Only results matching the filter criteria should be returned. Exactly one of \&quot;allOf\&quot;, \&quot;anyOf\&quot;, and \&quot;columnFilter\&quot; should be set. 
 */
@ApiModel(description = "A list of filters applied to the results of a query. Only results matching the filter criteria should be returned. Exactly one of \"allOf\", \"anyOf\", and \"columnFilter\" should be set. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class ResultFilters   {
  @JsonProperty("if_not")
  private Boolean ifNot = false;

  @JsonProperty("allOf")
  private List<ResultFilters> allOf = null;

  @JsonProperty("anyOf")
  private List<ResultFilters> anyOf = null;

  @JsonProperty("columnFilter")
  private ColumnFilter columnFilter = null;

  public ResultFilters ifNot(Boolean ifNot) {
    this.ifNot = ifNot;
    return this;
  }

   /**
   * Set to true if a result matching allOf or anyOf below should result in a result *not* being returned. 
   * @return ifNot
  **/
  @ApiModelProperty(value = "Set to true if a result matching allOf or anyOf below should result in a result *not* being returned. ")


  public Boolean getIfNot() {
    return ifNot;
  }

  public void setIfNot(Boolean ifNot) {
    this.ifNot = ifNot;
  }

  public ResultFilters allOf(List<ResultFilters> allOf) {
    this.allOf = allOf;
    return this;
  }

  public ResultFilters addAllOfItem(ResultFilters allOfItem) {
    if (this.allOf == null) {
      this.allOf = new ArrayList<ResultFilters>();
    }
    this.allOf.add(allOfItem);
    return this;
  }

   /**
   * A list of result filters. All filters matching means a result should be returned (or not returned if \"not\" is true.) 
   * @return allOf
  **/
  @ApiModelProperty(value = "A list of result filters. All filters matching means a result should be returned (or not returned if \"not\" is true.) ")

  @Valid

  public List<ResultFilters> getAllOf() {
    return allOf;
  }

  public void setAllOf(List<ResultFilters> allOf) {
    this.allOf = allOf;
  }

  public ResultFilters anyOf(List<ResultFilters> anyOf) {
    this.anyOf = anyOf;
    return this;
  }

  public ResultFilters addAnyOfItem(ResultFilters anyOfItem) {
    if (this.anyOf == null) {
      this.anyOf = new ArrayList<ResultFilters>();
    }
    this.anyOf.add(anyOfItem);
    return this;
  }

   /**
   * A list of column filters. Any filters matching means a result should be returned (or not returned if \"not\" is true.) 
   * @return anyOf
  **/
  @ApiModelProperty(value = "A list of column filters. Any filters matching means a result should be returned (or not returned if \"not\" is true.) ")

  @Valid

  public List<ResultFilters> getAnyOf() {
    return anyOf;
  }

  public void setAnyOf(List<ResultFilters> anyOf) {
    this.anyOf = anyOf;
  }

  public ResultFilters columnFilter(ColumnFilter columnFilter) {
    this.columnFilter = columnFilter;
    return this;
  }

   /**
   * A filter on a column in the table. Only a result matching this filter should be returned (or not returned if \"not\" is true.) 
   * @return columnFilter
  **/
  @ApiModelProperty(value = "A filter on a column in the table. Only a result matching this filter should be returned (or not returned if \"not\" is true.) ")

  @Valid

  public ColumnFilter getColumnFilter() {
    return columnFilter;
  }

  public void setColumnFilter(ColumnFilter columnFilter) {
    this.columnFilter = columnFilter;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResultFilters resultFilters = (ResultFilters) o;
    return Objects.equals(this.ifNot, resultFilters.ifNot) &&
        Objects.equals(this.allOf, resultFilters.allOf) &&
        Objects.equals(this.anyOf, resultFilters.anyOf) &&
        Objects.equals(this.columnFilter, resultFilters.columnFilter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ifNot, allOf, anyOf, columnFilter);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ResultFilters {\n");
    
    sb.append("    ifNot: ").append(toIndentedString(ifNot)).append("\n");
    sb.append("    allOf: ").append(toIndentedString(allOf)).append("\n");
    sb.append("    anyOf: ").append(toIndentedString(anyOf)).append("\n");
    sb.append("    columnFilter: ").append(toIndentedString(columnFilter)).append("\n");
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

