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
 * CdrQuery
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public class CdrQuery   {
  @JsonProperty("sql")
  private String sql = null;

  @JsonProperty("columns")
  private List<String> columns = new ArrayList<String>();

  @JsonProperty("configuration")
  private Object _configuration = null;

  @JsonProperty("bigqueryProject")
  private String bigqueryProject = null;

  @JsonProperty("bigqueryDataset")
  private String bigqueryDataset = null;

  public CdrQuery sql(String sql) {
    this.sql = sql;
    return this;
  }

   /**
   * Google SQL to use when querying the CDR. If empty, it means no participants can possibly match the data table specification, and an empty data table should be returned. 
   * @return sql
  **/
  @ApiModelProperty(value = "Google SQL to use when querying the CDR. If empty, it means no participants can possibly match the data table specification, and an empty data table should be returned. ")


  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public CdrQuery columns(List<String> columns) {
    this.columns = columns;
    return this;
  }

  public CdrQuery addColumnsItem(String columnsItem) {
    this.columns.add(columnsItem);
    return this;
  }

   /**
   * An array of names to be used for the columns being returned by the query. (Note that related table aliases will be returned with '.' as a separator, whereas '__' is used in the SQL.) This will be populated even if sql is empty (i.e. there are no results.) 
   * @return columns
  **/
  @ApiModelProperty(required = true, value = "An array of names to be used for the columns being returned by the query. (Note that related table aliases will be returned with '.' as a separator, whereas '__' is used in the SQL.) This will be populated even if sql is empty (i.e. there are no results.) ")
  @NotNull


  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public CdrQuery _configuration(Object _configuration) {
    this._configuration = _configuration;
    return this;
  }

   /**
   * configuration for the BigQuery job (includes named parameters); you can pass this JSON dictionary in for the configuration when calling methods like pandas.read_gbq(). 
   * @return _configuration
  **/
  @ApiModelProperty(value = "configuration for the BigQuery job (includes named parameters); you can pass this JSON dictionary in for the configuration when calling methods like pandas.read_gbq(). ")


  public Object getConfiguration() {
    return _configuration;
  }

  public void setConfiguration(Object _configuration) {
    this._configuration = _configuration;
  }

  public CdrQuery bigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
    return this;
  }

   /**
   * name of the Google Cloud project containing the CDR dataset
   * @return bigqueryProject
  **/
  @ApiModelProperty(required = true, value = "name of the Google Cloud project containing the CDR dataset")
  @NotNull


  public String getBigqueryProject() {
    return bigqueryProject;
  }

  public void setBigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
  }

  public CdrQuery bigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
    return this;
  }

   /**
   * name of the CDR BigQuery dataset
   * @return bigqueryDataset
  **/
  @ApiModelProperty(required = true, value = "name of the CDR BigQuery dataset")
  @NotNull


  public String getBigqueryDataset() {
    return bigqueryDataset;
  }

  public void setBigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CdrQuery cdrQuery = (CdrQuery) o;
    return Objects.equals(this.sql, cdrQuery.sql) &&
        Objects.equals(this.columns, cdrQuery.columns) &&
        Objects.equals(this._configuration, cdrQuery._configuration) &&
        Objects.equals(this.bigqueryProject, cdrQuery.bigqueryProject) &&
        Objects.equals(this.bigqueryDataset, cdrQuery.bigqueryDataset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sql, columns, _configuration, bigqueryProject, bigqueryDataset);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CdrQuery {\n");
    
    sb.append("    sql: ").append(toIndentedString(sql)).append("\n");
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    _configuration: ").append(toIndentedString(_configuration)).append("\n");
    sb.append("    bigqueryProject: ").append(toIndentedString(bigqueryProject)).append("\n");
    sb.append("    bigqueryDataset: ").append(toIndentedString(bigqueryDataset)).append("\n");
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

