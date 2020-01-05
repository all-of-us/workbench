package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * WorkspaceResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class WorkspaceResponse   {
  @JsonProperty("workspace")
  private Workspace workspace = null;

  @JsonProperty("accessLevel")
  private WorkspaceAccessLevel accessLevel = null;

  public WorkspaceResponse workspace(Workspace workspace) {
    this.workspace = workspace;
    return this;
  }

   /**
   * Get workspace
   * @return workspace
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public Workspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public WorkspaceResponse accessLevel(WorkspaceAccessLevel accessLevel) {
    this.accessLevel = accessLevel;
    return this;
  }

   /**
   * Get accessLevel
   * @return accessLevel
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

  @Valid

  public WorkspaceAccessLevel getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(WorkspaceAccessLevel accessLevel) {
    this.accessLevel = accessLevel;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkspaceResponse workspaceResponse = (WorkspaceResponse) o;
    return Objects.equals(this.workspace, workspaceResponse.workspace) &&
        Objects.equals(this.accessLevel, workspaceResponse.accessLevel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspace, accessLevel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceResponse {\n");
    
    sb.append("    workspace: ").append(toIndentedString(workspace)).append("\n");
    sb.append("    accessLevel: ").append(toIndentedString(accessLevel)).append("\n");
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

