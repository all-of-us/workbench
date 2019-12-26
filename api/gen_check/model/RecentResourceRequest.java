package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * RecentResourceRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class RecentResourceRequest   {
  @JsonProperty("notebookName")
  private String notebookName = null;

  public RecentResourceRequest notebookName(String notebookName) {
    this.notebookName = notebookName;
    return this;
  }

   /**
   * Get notebookName
   * @return notebookName
  **/
  @ApiModelProperty(value = "")


  public String getNotebookName() {
    return notebookName;
  }

  public void setNotebookName(String notebookName) {
    this.notebookName = notebookName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecentResourceRequest recentResourceRequest = (RecentResourceRequest) o;
    return Objects.equals(this.notebookName, recentResourceRequest.notebookName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notebookName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecentResourceRequest {\n");
    
    sb.append("    notebookName: ").append(toIndentedString(notebookName)).append("\n");
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

