package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.FieldSet;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * MaterializeCohortRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class MaterializeCohortRequest   {
  @JsonProperty("cohortName")
  private String cohortName = null;

  @JsonProperty("cohortSpec")
  private String cohortSpec = null;

  @JsonProperty("statusFilter")
  private List<CohortStatus> statusFilter = null;

  @JsonProperty("cdrVersionName")
  private String cdrVersionName = null;

  @JsonProperty("pageToken")
  private String pageToken = null;

  @JsonProperty("pageSize")
  private Integer pageSize = null;

  @JsonProperty("fieldSet")
  private FieldSet fieldSet = null;

  public MaterializeCohortRequest cohortName(String cohortName) {
    this.cohortName = cohortName;
    return this;
  }

   /**
   * The name of a cohort that is to be evaluated. Either this or cohortSpec should be specified 
   * @return cohortName
  **/
  @ApiModelProperty(value = "The name of a cohort that is to be evaluated. Either this or cohortSpec should be specified ")


  public String getCohortName() {
    return cohortName;
  }

  public void setCohortName(String cohortName) {
    this.cohortName = cohortName;
  }

  public MaterializeCohortRequest cohortSpec(String cohortSpec) {
    this.cohortSpec = cohortSpec;
    return this;
  }

   /**
   * JSON representation of a cohort to be evaluated (using the same format used for saved cohorts). Either this or cohortName should be specified 
   * @return cohortSpec
  **/
  @ApiModelProperty(value = "JSON representation of a cohort to be evaluated (using the same format used for saved cohorts). Either this or cohortName should be specified ")


  public String getCohortSpec() {
    return cohortSpec;
  }

  public void setCohortSpec(String cohortSpec) {
    this.cohortSpec = cohortSpec;
  }

  public MaterializeCohortRequest statusFilter(List<CohortStatus> statusFilter) {
    this.statusFilter = statusFilter;
    return this;
  }

  public MaterializeCohortRequest addStatusFilterItem(CohortStatus statusFilterItem) {
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

  public MaterializeCohortRequest cdrVersionName(String cdrVersionName) {
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

  public MaterializeCohortRequest pageToken(String pageToken) {
    this.pageToken = pageToken;
    return this;
  }

   /**
   * Pagination token retrieved from a previous call to materializeCohort; used for retrieving additional pages of results. If this is specified, all other fields on MaterializeCohortRequest apart from pageSize must match the values specified on the request that generated this token. 
   * @return pageToken
  **/
  @ApiModelProperty(value = "Pagination token retrieved from a previous call to materializeCohort; used for retrieving additional pages of results. If this is specified, all other fields on MaterializeCohortRequest apart from pageSize must match the values specified on the request that generated this token. ")


  public String getPageToken() {
    return pageToken;
  }

  public void setPageToken(String pageToken) {
    this.pageToken = pageToken;
  }

  public MaterializeCohortRequest pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

   /**
   * Maximum number of results to return in a response. Defaults to 1000. 
   * @return pageSize
  **/
  @ApiModelProperty(value = "Maximum number of results to return in a response. Defaults to 1000. ")


  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public MaterializeCohortRequest fieldSet(FieldSet fieldSet) {
    this.fieldSet = fieldSet;
    return this;
  }

   /**
   * Specification defining what data to return for participants in the cohort. Defaults to just participant IDs. 
   * @return fieldSet
  **/
  @ApiModelProperty(value = "Specification defining what data to return for participants in the cohort. Defaults to just participant IDs. ")

  @Valid

  public FieldSet getFieldSet() {
    return fieldSet;
  }

  public void setFieldSet(FieldSet fieldSet) {
    this.fieldSet = fieldSet;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaterializeCohortRequest materializeCohortRequest = (MaterializeCohortRequest) o;
    return Objects.equals(this.cohortName, materializeCohortRequest.cohortName) &&
        Objects.equals(this.cohortSpec, materializeCohortRequest.cohortSpec) &&
        Objects.equals(this.statusFilter, materializeCohortRequest.statusFilter) &&
        Objects.equals(this.cdrVersionName, materializeCohortRequest.cdrVersionName) &&
        Objects.equals(this.pageToken, materializeCohortRequest.pageToken) &&
        Objects.equals(this.pageSize, materializeCohortRequest.pageSize) &&
        Objects.equals(this.fieldSet, materializeCohortRequest.fieldSet);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortName, cohortSpec, statusFilter, cdrVersionName, pageToken, pageSize, fieldSet);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MaterializeCohortRequest {\n");
    
    sb.append("    cohortName: ").append(toIndentedString(cohortName)).append("\n");
    sb.append("    cohortSpec: ").append(toIndentedString(cohortSpec)).append("\n");
    sb.append("    statusFilter: ").append(toIndentedString(statusFilter)).append("\n");
    sb.append("    cdrVersionName: ").append(toIndentedString(cdrVersionName)).append("\n");
    sb.append("    pageToken: ").append(toIndentedString(pageToken)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    fieldSet: ").append(toIndentedString(fieldSet)).append("\n");
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

