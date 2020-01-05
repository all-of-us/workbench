package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.TableQuery;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A specification for fields to retrieve about participants in a cohort. Exactly one of the properties below should be specified. 
 */
@ApiModel(description = "A specification for fields to retrieve about participants in a cohort. Exactly one of the properties below should be specified. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class FieldSet   {
  @JsonProperty("tableQuery")
  private TableQuery tableQuery = null;

  @JsonProperty("annotationQuery")
  private AnnotationQuery annotationQuery = null;

  public FieldSet tableQuery(TableQuery tableQuery) {
    this.tableQuery = tableQuery;
    return this;
  }

   /**
   * A query specifying how to pull data out of a single table. Either this or annotationQuery should be set (not both.) 
   * @return tableQuery
  **/
  @ApiModelProperty(value = "A query specifying how to pull data out of a single table. Either this or annotationQuery should be set (not both.) ")

  @Valid

  public TableQuery getTableQuery() {
    return tableQuery;
  }

  public void setTableQuery(TableQuery tableQuery) {
    this.tableQuery = tableQuery;
  }

  public FieldSet annotationQuery(AnnotationQuery annotationQuery) {
    this.annotationQuery = annotationQuery;
    return this;
  }

   /**
   * A query specifying how to retrieve annotation values created about participants in a cohort during cohort review. Either this or tableQuery should be set (not both.) 
   * @return annotationQuery
  **/
  @ApiModelProperty(value = "A query specifying how to retrieve annotation values created about participants in a cohort during cohort review. Either this or tableQuery should be set (not both.) ")

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
    FieldSet fieldSet = (FieldSet) o;
    return Objects.equals(this.tableQuery, fieldSet.tableQuery) &&
        Objects.equals(this.annotationQuery, fieldSet.annotationQuery);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableQuery, annotationQuery);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FieldSet {\n");
    
    sb.append("    tableQuery: ").append(toIndentedString(tableQuery)).append("\n");
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

