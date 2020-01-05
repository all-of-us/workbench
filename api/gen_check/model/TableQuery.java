package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ResultFilters;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A specification for retrieving data from a single table. 
 */
@ApiModel(description = "A specification for retrieving data from a single table. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class TableQuery   {
  @JsonProperty("tableName")
  private String tableName = null;

  @JsonProperty("columns")
  private List<String> columns = null;

  @JsonProperty("filters")
  private ResultFilters filters = null;

  @JsonProperty("conceptSetName")
  private String conceptSetName = null;

  @JsonProperty("orderBy")
  private List<String> orderBy = null;

  public TableQuery tableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

   /**
   * The name of a table containing a person_id column to retrieve data from (e.g. person, observation); should be in the OMOP CDM 5.2 schema. 
   * @return tableName
  **/
  @ApiModelProperty(required = true, value = "The name of a table containing a person_id column to retrieve data from (e.g. person, observation); should be in the OMOP CDM 5.2 schema. ")
  @NotNull


  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public TableQuery columns(List<String> columns) {
    this.columns = columns;
    return this;
  }

  public TableQuery addColumnsItem(String columnsItem) {
    if (this.columns == null) {
      this.columns = new ArrayList<String>();
    }
    this.columns.add(columnsItem);
    return this;
  }

   /**
   * An array of columns to retrieve from the table, taken from the table specified above. Defaults to all columns. 
   * @return columns
  **/
  @ApiModelProperty(value = "An array of columns to retrieve from the table, taken from the table specified above. Defaults to all columns. ")


  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public TableQuery filters(ResultFilters filters) {
    this.filters = filters;
    return this;
  }

   /**
   * Filters on the results. Only results matching the criteria specified in the filters will be returned. If both filters and conceptSetName are specified, results must match both. 
   * @return filters
  **/
  @ApiModelProperty(value = "Filters on the results. Only results matching the criteria specified in the filters will be returned. If both filters and conceptSetName are specified, results must match both. ")

  @Valid

  public ResultFilters getFilters() {
    return filters;
  }

  public void setFilters(ResultFilters filters) {
    this.filters = filters;
  }

  public TableQuery conceptSetName(String conceptSetName) {
    this.conceptSetName = conceptSetName;
    return this;
  }

   /**
   * A name of a concept set in the workspace used to filter results; results must match one of the concepts in the named concept set. If both filters and conceptSetName are specified, results must match both. 
   * @return conceptSetName
  **/
  @ApiModelProperty(value = "A name of a concept set in the workspace used to filter results; results must match one of the concepts in the named concept set. If both filters and conceptSetName are specified, results must match both. ")


  public String getConceptSetName() {
    return conceptSetName;
  }

  public void setConceptSetName(String conceptSetName) {
    this.conceptSetName = conceptSetName;
  }

  public TableQuery orderBy(List<String> orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public TableQuery addOrderByItem(String orderByItem) {
    if (this.orderBy == null) {
      this.orderBy = new ArrayList<String>();
    }
    this.orderBy.add(orderByItem);
    return this;
  }

   /**
   * An array of columns to sort the resulting data by, taken from the table specified above, each one optionally enclosed in \"DESCENDING()\" for descending sort order. Default sort order is \"person_id\" (in ascending order) followed by the ID of the specified table (in ascending order.) 
   * @return orderBy
  **/
  @ApiModelProperty(value = "An array of columns to sort the resulting data by, taken from the table specified above, each one optionally enclosed in \"DESCENDING()\" for descending sort order. Default sort order is \"person_id\" (in ascending order) followed by the ID of the specified table (in ascending order.) ")


  public List<String> getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(List<String> orderBy) {
    this.orderBy = orderBy;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableQuery tableQuery = (TableQuery) o;
    return Objects.equals(this.tableName, tableQuery.tableName) &&
        Objects.equals(this.columns, tableQuery.columns) &&
        Objects.equals(this.filters, tableQuery.filters) &&
        Objects.equals(this.conceptSetName, tableQuery.conceptSetName) &&
        Objects.equals(this.orderBy, tableQuery.orderBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, columns, filters, conceptSetName, orderBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TableQuery {\n");
    
    sb.append("    tableName: ").append(toIndentedString(tableName)).append("\n");
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    filters: ").append(toIndentedString(filters)).append("\n");
    sb.append("    conceptSetName: ").append(toIndentedString(conceptSetName)).append("\n");
    sb.append("    orderBy: ").append(toIndentedString(orderBy)).append("\n");
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

