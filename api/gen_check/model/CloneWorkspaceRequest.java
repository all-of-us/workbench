package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.Workspace;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CloneWorkspaceRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class CloneWorkspaceRequest   {
  @JsonProperty("includeUserRoles")
  private Boolean includeUserRoles = false;

  @JsonProperty("workspace")
  private Workspace workspace = null;

  public CloneWorkspaceRequest includeUserRoles(Boolean includeUserRoles) {
    this.includeUserRoles = includeUserRoles;
    return this;
  }

   /**
   * Whether to duplicate the user roles (sharing settings) on the workspace. By default, duplicate follows the behavior of CreateWorkspace - it is shared solely with the creator as an OWNER. If true, all user roles are also copied onto the new workspace in addition to the caller becoming an OWNER. 
   * @return includeUserRoles
  **/
  @ApiModelProperty(value = "Whether to duplicate the user roles (sharing settings) on the workspace. By default, duplicate follows the behavior of CreateWorkspace - it is shared solely with the creator as an OWNER. If true, all user roles are also copied onto the new workspace in addition to the caller becoming an OWNER. ")


  public Boolean getIncludeUserRoles() {
    return includeUserRoles;
  }

  public void setIncludeUserRoles(Boolean includeUserRoles) {
    this.includeUserRoles = includeUserRoles;
  }

  public CloneWorkspaceRequest workspace(Workspace workspace) {
    this.workspace = workspace;
    return this;
  }

   /**
   * Workspace metadata to be applied to the cloned workspace upon creation. The following workspace fields are accepted:   - name (required)   - namespace (required)   - researchPurpose (required)   - description: defaults to the cloned workspace description   - cdrVersionId defaults to the cloned workspace CDR version  All other fields will be ignored and are generated server-side or are copied from the cloned workspace. 
   * @return workspace
  **/
  @ApiModelProperty(required = true, value = "Workspace metadata to be applied to the cloned workspace upon creation. The following workspace fields are accepted:   - name (required)   - namespace (required)   - researchPurpose (required)   - description: defaults to the cloned workspace description   - cdrVersionId defaults to the cloned workspace CDR version  All other fields will be ignored and are generated server-side or are copied from the cloned workspace. ")
  @NotNull

  @Valid

  public Workspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloneWorkspaceRequest cloneWorkspaceRequest = (CloneWorkspaceRequest) o;
    return Objects.equals(this.includeUserRoles, cloneWorkspaceRequest.includeUserRoles) &&
        Objects.equals(this.workspace, cloneWorkspaceRequest.workspace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includeUserRoles, workspace);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CloneWorkspaceRequest {\n");
    
    sb.append("    includeUserRoles: ").append(toIndentedString(includeUserRoles)).append("\n");
    sb.append("    workspace: ").append(toIndentedString(workspace)).append("\n");
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

