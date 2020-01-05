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
 * A specification for retrieving annotation values or review statuses entered for participants. 
 */
@ApiModel(description = "A specification for retrieving annotation values or review statuses entered for participants. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class AnnotationQuery   {
  @JsonProperty("columns")
  private List<String> columns = null;

  @JsonProperty("orderBy")
  private List<String> orderBy = null;

  public AnnotationQuery columns(List<String> columns) {
    this.columns = columns;
    return this;
  }

  public AnnotationQuery addColumnsItem(String columnsItem) {
    if (this.columns == null) {
      this.columns = new ArrayList<String>();
    }
    this.columns.add(columnsItem);
    return this;
  }

   /**
   * An array of names of annotations to retrieve about participants, or \"review_status\" for the cohort review status of the participant or \"person_id\" for the ID of the participant. Defaults to \"person_id\", \"review_status\", and the names of all defined annotations in the cohort review. This is only valid in combination with the use of cohortName above. Only data for participants in the cohort review will be returned; if no cohort review has been created, no results will be returned. 
   * @return columns
  **/
  @ApiModelProperty(value = "An array of names of annotations to retrieve about participants, or \"review_status\" for the cohort review status of the participant or \"person_id\" for the ID of the participant. Defaults to \"person_id\", \"review_status\", and the names of all defined annotations in the cohort review. This is only valid in combination with the use of cohortName above. Only data for participants in the cohort review will be returned; if no cohort review has been created, no results will be returned. ")


  public List<String> getColumns() {
    return columns;
  }

  public void setColumns(List<String> columns) {
    this.columns = columns;
  }

  public AnnotationQuery orderBy(List<String> orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public AnnotationQuery addOrderByItem(String orderByItem) {
    if (this.orderBy == null) {
      this.orderBy = new ArrayList<String>();
    }
    this.orderBy.add(orderByItem);
    return this;
  }

   /**
   * An array of names of annotations, or \"review status\" or \"person_id\", each one optionally enclosed in \"DESCENDING()\" for descending sort order. Specifies the order that results should be returned. Defaults to \"person_id\" (in ascending order). Annotations referenced in orderBy must also be present in columns. 
   * @return orderBy
  **/
  @ApiModelProperty(value = "An array of names of annotations, or \"review status\" or \"person_id\", each one optionally enclosed in \"DESCENDING()\" for descending sort order. Specifies the order that results should be returned. Defaults to \"person_id\" (in ascending order). Annotations referenced in orderBy must also be present in columns. ")


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
    AnnotationQuery annotationQuery = (AnnotationQuery) o;
    return Objects.equals(this.columns, annotationQuery.columns) &&
        Objects.equals(this.orderBy, annotationQuery.orderBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columns, orderBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AnnotationQuery {\n");
    
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
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

