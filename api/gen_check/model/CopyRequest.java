package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CopyRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class CopyRequest   {
  @JsonProperty("toWorkspaceName")
  private String toWorkspaceName = null;

  @JsonProperty("toWorkspaceNamespace")
  private String toWorkspaceNamespace = null;

  @JsonProperty("newName")
  private String newName = null;

  public CopyRequest toWorkspaceName(String toWorkspaceName) {
    this.toWorkspaceName = toWorkspaceName;
    return this;
  }

   /**
   * Get toWorkspaceName
   * @return toWorkspaceName
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getToWorkspaceName() {
    return toWorkspaceName;
  }

  public void setToWorkspaceName(String toWorkspaceName) {
    this.toWorkspaceName = toWorkspaceName;
  }

  public CopyRequest toWorkspaceNamespace(String toWorkspaceNamespace) {
    this.toWorkspaceNamespace = toWorkspaceNamespace;
    return this;
  }

   /**
   * Get toWorkspaceNamespace
   * @return toWorkspaceNamespace
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getToWorkspaceNamespace() {
    return toWorkspaceNamespace;
  }

  public void setToWorkspaceNamespace(String toWorkspaceNamespace) {
    this.toWorkspaceNamespace = toWorkspaceNamespace;
  }

  public CopyRequest newName(String newName) {
    this.newName = newName;
    return this;
  }

   /**
   * Get newName
   * @return newName
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull


  public String getNewName() {
    return newName;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CopyRequest copyRequest = (CopyRequest) o;
    return Objects.equals(this.toWorkspaceName, copyRequest.toWorkspaceName) &&
        Objects.equals(this.toWorkspaceNamespace, copyRequest.toWorkspaceNamespace) &&
        Objects.equals(this.newName, copyRequest.newName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(toWorkspaceName, toWorkspaceNamespace, newName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CopyRequest {\n");
    
    sb.append("    toWorkspaceName: ").append(toIndentedString(toWorkspaceName)).append("\n");
    sb.append("    toWorkspaceNamespace: ").append(toIndentedString(toWorkspaceNamespace)).append("\n");
    sb.append("    newName: ").append(toIndentedString(newName)).append("\n");
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

