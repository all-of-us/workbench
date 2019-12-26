package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.CohortStatus;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortAnnotationsRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class CohortAnnotationsRequest   {
  @JsonProperty("cohortName")
  private String cohortName = null;

  @JsonProperty("statusFilter")
  private List<CohortStatus> statusFilter = null;

  @JsonProperty("cdrVersionName")
  private String cdrVersionName = null;

  @JsonProperty("annotationQuery")
  private AnnotationQuery annotationQuery = null;

  public CohortAnnotationsRequest cohortName(String cohortName) {
    this.cohortName = cohortName;
    return this;
  }

   /**
   * The name of a cohort that annotations should be retrieved for. 
   * @return cohortName
  **/
  @ApiModelProperty(required = true, value = "The name of a cohort that annotations should be retrieved for. ")
  @NotNull


  public String getCohortName() {
    return cohortName;
  }

  public void setCohortName(String cohortName) {
    this.cohortName = cohortName;
  }

  public CohortAnnotationsRequest statusFilter(List<CohortStatus> statusFilter) {
    this.statusFilter = statusFilter;
    return this;
  }

  public CohortAnnotationsRequest addStatusFilterItem(CohortStatus statusFilterItem) {
    if (this.statusFilter == null) {
      this.statusFilter = new ArrayList<CohortStatus>();
    }
    this.statusFilter.add(statusFilterItem);
    return this;
  }

   /**
   * An array of status values; participants with these statuses will have their annotations retrieved. Defaults to [NOT_REVIEWED, INCLUDED, NEEDS_FURTHER_REVIEW] -- everything but EXCLUDED. 
   * @return statusFilter
  **/
  @ApiModelProperty(value = "An array of status values; participants with these statuses will have their annotations retrieved. Defaults to [NOT_REVIEWED, INCLUDED, NEEDS_FURTHER_REVIEW] -- everything but EXCLUDED. ")

  @Valid

  public List<CohortStatus> getStatusFilter() {
    return statusFilter;
  }

  public void setStatusFilter(List<CohortStatus> statusFilter) {
    this.statusFilter = statusFilter;
  }

  public CohortAnnotationsRequest cdrVersionName(String cdrVersionName) {
    this.cdrVersionName = cdrVersionName;
    return this;
  }

   /**
   * The name of a CDR version to use when retrieving annotations; if none is specified, the CDR version currently associated with the workspace will be used 
   * @return cdrVersionName
  **/
  @ApiModelProperty(value = "The name of a CDR version to use when retrieving annotations; if none is specified, the CDR version currently associated with the workspace will be used ")


  public String getCdrVersionName() {
    return cdrVersionName;
  }

  public void setCdrVersionName(String cdrVersionName) {
    this.cdrVersionName = cdrVersionName;
  }

  public CohortAnnotationsRequest annotationQuery(AnnotationQuery annotationQuery) {
    this.annotationQuery = annotationQuery;
    return this;
  }

   /**
   * Specification defining what annotations to retrieve. 
   * @return annotationQuery
  **/
  @ApiModelProperty(value = "Specification defining what annotations to retrieve. ")

  @Valid

  public AnnotationQuery getAnnotationQuery() {
    return annotationQuery;
  }

  public void setAnnotationQuery(AnnotationQuery annotationQuery) {
    this.annotationQuery = annotationQuery;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortAnnotationsRequest cohortAnnotationsRequest = (CohortAnnotationsRequest) o;
    return Objects.equals(this.cohortName, cohortAnnotationsRequest.cohortName) &&
        Objects.equals(this.statusFilter, cohortAnnotationsRequest.statusFilter) &&
        Objects.equals(this.cdrVersionName, cohortAnnotationsRequest.cdrVersionName) &&
        Objects.equals(this.annotationQuery, cohortAnnotationsRequest.annotationQuery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortName, statusFilter, cdrVersionName, annotationQuery);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortAnnotationsRequest {\n");
    
    sb.append("    cohortName: ").append(toIndentedString(cohortName)).append("\n");
    sb.append("    statusFilter: ").append(toIndentedString(statusFilter)).append("\n");
    sb.append("    cdrVersionName: ").append(toIndentedString(cdrVersionName)).append("\n");
    sb.append("    annotationQuery: ").append(toIndentedString(annotationQuery)).append("\n");
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

