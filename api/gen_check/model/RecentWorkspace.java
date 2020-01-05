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
 * RecentWorkspace
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class RecentWorkspace   {
  @JsonProperty("workspace")
  private Workspace workspace = null;

  @JsonProperty("accessLevel")
  private WorkspaceAccessLevel accessLevel = null;

  @JsonProperty("accessedTime")
  private String accessedTime = null;

  public RecentWorkspace workspace(Workspace workspace) {
    this.workspace = workspace;
    return this;
  }

   /**
   * Get workspace
   * @return workspace
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Workspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  public RecentWorkspace accessLevel(WorkspaceAccessLevel accessLevel) {
    this.accessLevel = accessLevel;
    return this;
  }

   /**
   * Get accessLevel
   * @return accessLevel
  **/
  @ApiModelProperty(value = "")

  @Valid

  public WorkspaceAccessLevel getAccessLevel() {
    return accessLevel;
  }

  public void setAccessLevel(WorkspaceAccessLevel accessLevel) {
    this.accessLevel = accessLevel;
  }

  public RecentWorkspace accessedTime(String accessedTime) {
    this.accessedTime = accessedTime;
    return this;
  }

   /**
   * The date and time that the workspace was last accessed, in ISO-8601 format
   * @return accessedTime
  **/
  @ApiModelProperty(value = "The date and time that the workspace was last accessed, in ISO-8601 format")


  public String getAccessedTime() {
    return accessedTime;
  }

  public void setAccessedTime(String accessedTime) {
    this.accessedTime = accessedTime;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecentWorkspace recentWorkspace = (RecentWorkspace) o;
    return Objects.equals(this.workspace, recentWorkspace.workspace) &&
        Objects.equals(this.accessLevel, recentWorkspace.accessLevel) &&
        Objects.equals(this.accessedTime, recentWorkspace.accessedTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspace, accessLevel, accessedTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecentWorkspace {\n");
    
    sb.append("    workspace: ").append(toIndentedString(workspace)).append("\n");
    sb.append("    accessLevel: ").append(toIndentedString(accessLevel)).append("\n");
    sb.append("    accessedTime: ").append(toIndentedString(accessedTime)).append("\n");
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

