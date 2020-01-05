package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * StatusResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class StatusResponse   {
  @JsonProperty("firecloudStatus")
  private Boolean firecloudStatus = false;

  @JsonProperty("notebooksStatus")
  private Boolean notebooksStatus = false;

  public StatusResponse firecloudStatus(Boolean firecloudStatus) {
    this.firecloudStatus = firecloudStatus;
    return this;
  }

   /**
   * Get firecloudStatus
   * @return firecloudStatus
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getFirecloudStatus() {
    return firecloudStatus;
  }

  public void setFirecloudStatus(Boolean firecloudStatus) {
    this.firecloudStatus = firecloudStatus;
  }

  public StatusResponse notebooksStatus(Boolean notebooksStatus) {
    this.notebooksStatus = notebooksStatus;
    return this;
  }

   /**
   * Get notebooksStatus
   * @return notebooksStatus
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public Boolean getNotebooksStatus() {
    return notebooksStatus;
  }

  public void setNotebooksStatus(Boolean notebooksStatus) {
    this.notebooksStatus = notebooksStatus;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatusResponse statusResponse = (StatusResponse) o;
    return Objects.equals(this.firecloudStatus, statusResponse.firecloudStatus) &&
        Objects.equals(this.notebooksStatus, statusResponse.notebooksStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(firecloudStatus, notebooksStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StatusResponse {\n");
    
    sb.append("    firecloudStatus: ").append(toIndentedString(firecloudStatus)).append("\n");
    sb.append("    notebooksStatus: ").append(toIndentedString(notebooksStatus)).append("\n");
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

