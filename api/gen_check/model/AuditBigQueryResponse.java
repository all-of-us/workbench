package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * AuditBigQueryResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class AuditBigQueryResponse   {
  @JsonProperty("numQueryIssues")
  private Integer numQueryIssues = null;

  public AuditBigQueryResponse numQueryIssues(Integer numQueryIssues) {
    this.numQueryIssues = numQueryIssues;
    return this;
  }

   /**
   * Number of queries issues against the Curated data repository which are flagged as possible audit issues. See logs/alerts for details. 
   * @return numQueryIssues
  **/
  @ApiModelProperty(value = "Number of queries issues against the Curated data repository which are flagged as possible audit issues. See logs/alerts for details. ")


  public Integer getNumQueryIssues() {
    return numQueryIssues;
  }

  public void setNumQueryIssues(Integer numQueryIssues) {
    this.numQueryIssues = numQueryIssues;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuditBigQueryResponse auditBigQueryResponse = (AuditBigQueryResponse) o;
    return Objects.equals(this.numQueryIssues, auditBigQueryResponse.numQueryIssues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(numQueryIssues);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuditBigQueryResponse {\n");
    
    sb.append("    numQueryIssues: ").append(toIndentedString(numQueryIssues)).append("\n");
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

