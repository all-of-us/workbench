package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.TableQuery;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataTableSpecification
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class DataTableSpecification   {
  @JsonProperty("cohortName")
  private String cohortName = null;

  @JsonProperty("cohortSpec")
  private String cohortSpec = null;

  @JsonProperty("statusFilter")
  private List<CohortStatus> statusFilter = null;

  @JsonProperty("cdrVersionName")
  private String cdrVersionName = null;

  @JsonProperty("tableQuery")
  private TableQuery tableQuery = null;

  @JsonProperty("maxResults")
  private Long maxResults = null;

  public DataTableSpecification cohortName(String cohortName) {
    this.cohortName = cohortName;
    return this;
  }

   /**
   * The name of a cohort that data should be retrieved for. This and cohortSpec should not be used at the same time. If neither cohortName nor cohortSpec are specified, data will be extracted for everyone in the CDR. 
   * @return cohortName
  **/
  @ApiModelProperty(value = "The name of a cohort that data should be retrieved for. This and cohortSpec should not be used at the same time. If neither cohortName nor cohortSpec are specified, data will be extracted for everyone in the CDR. ")


  public String getCohortName() {
    return cohortName;
  }

  public void setCohortName(String cohortName) {
    this.cohortName = cohortName;
  }

  public DataTableSpecification cohortSpec(String cohortSpec) {
    this.cohortSpec = cohortSpec;
    return this;
  }

   /**
   * JSON representation of a cohort to be evaluated (using the same format used for saved cohorts). This and cohortName should not be used at the same time. If neither cohortName nor cohortSpec are specified, data will be extracted for everyone in the CDR. 
   * @return cohortSpec
  **/
  @ApiModelProperty(value = "JSON representation of a cohort to be evaluated (using the same format used for saved cohorts). This and cohortName should not be used at the same time. If neither cohortName nor cohortSpec are specified, data will be extracted for everyone in the CDR. ")


  public String getCohortSpec() {
    return cohortSpec;
  }

  public void setCohortSpec(String cohortSpec) {
    this.cohortSpec = cohortSpec;
  }

  public DataTableSpecification statusFilter(List<CohortStatus> statusFilter) {
    this.statusFilter = statusFilter;
    return this;
  }

  public DataTableSpecification addStatusFilterItem(CohortStatus statusFilterItem) {
    if (this.statusFilter == null) {
      this.statusFilter = new ArrayList<CohortStatus>();
    }
    this.statusFilter.add(statusFilterItem);
    return this;
  }

   /**
   * An array of status values; participants with these statuses will be included. Defaults to [NOT_REVIEWED, INCLUDED, NEEDS_FURTHER_REVIEW] -- everything but EXCLUDED. Only valid for use with cohortName (cohorts saved in the database.) 
   * @return statusFilter
  **/
  @ApiModelProperty(value = "An array of status values; participants with these statuses will be included. Defaults to [NOT_REVIEWED, INCLUDED, NEEDS_FURTHER_REVIEW] -- everything but EXCLUDED. Only valid for use with cohortName (cohorts saved in the database.) ")

  @Valid

  public List<CohortStatus> getStatusFilter() {
    return statusFilter;
  }

  public void setStatusFilter(List<CohortStatus> statusFilter) {
    this.statusFilter = statusFilter;
  }

  public DataTableSpecification cdrVersionName(String cdrVersionName) {
    this.cdrVersionName = cdrVersionName;
    return this;
  }

   /**
   * The name of a CDR version to use when evaluating the cohort; if none is specified, the CDR version currently associated with the workspace will be used 
   * @return cdrVersionName
  **/
  @ApiModelProperty(value = "The name of a CDR version to use when evaluating the cohort; if none is specified, the CDR version currently associated with the workspace will be used ")


  public String getCdrVersionName() {
    return cdrVersionName;
  }

  public void setCdrVersionName(String cdrVersionName) {
    this.cdrVersionName = cdrVersionName;
  }

  public DataTableSpecification tableQuery(TableQuery tableQuery) {
    this.tableQuery = tableQuery;
    return this;
  }

   /**
   * A query specifying how to pull data out of a single table. If tableQuery is not specified, just Person.person_id will be extracted. 
   * @return tableQuery
  **/
  @ApiModelProperty(value = "A query specifying how to pull data out of a single table. If tableQuery is not specified, just Person.person_id will be extracted. ")

  @Valid

  public TableQuery getTableQuery() {
    return tableQuery;
  }

  public void setTableQuery(TableQuery tableQuery) {
    this.tableQuery = tableQuery;
  }

  public DataTableSpecification maxResults(Long maxResults) {
    this.maxResults = maxResults;
    return this;
  }

   /**
   * The maximum number of results returned in the data table. Defaults to no limit (all matching results are returned.) 
   * @return maxResults
  **/
  @ApiModelProperty(value = "The maximum number of results returned in the data table. Defaults to no limit (all matching results are returned.) ")


  public Long getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Long maxResults) {
    this.maxResults = maxResults;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataTableSpecification dataTableSpecification = (DataTableSpecification) o;
    return Objects.equals(this.cohortName, dataTableSpecification.cohortName) &&
        Objects.equals(this.cohortSpec, dataTableSpecification.cohortSpec) &&
        Objects.equals(this.statusFilter, dataTableSpecification.statusFilter) &&
        Objects.equals(this.cdrVersionName, dataTableSpecification.cdrVersionName) &&
        Objects.equals(this.tableQuery, dataTableSpecification.tableQuery) &&
        Objects.equals(this.maxResults, dataTableSpecification.maxResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortName, cohortSpec, statusFilter, cdrVersionName, tableQuery, maxResults);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataTableSpecification {\n");
    
    sb.append("    cohortName: ").append(toIndentedString(cohortName)).append("\n");
    sb.append("    cohortSpec: ").append(toIndentedString(cohortSpec)).append("\n");
    sb.append("    statusFilter: ").append(toIndentedString(statusFilter)).append("\n");
    sb.append("    cdrVersionName: ").append(toIndentedString(cdrVersionName)).append("\n");
    sb.append("    tableQuery: ").append(toIndentedString(tableQuery)).append("\n");
    sb.append("    maxResults: ").append(toIndentedString(maxResults)).append("\n");
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

