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
 * ClusterLocalizeRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class ClusterLocalizeRequest   {
  @JsonProperty("workspaceNamespace")
  private String workspaceNamespace = null;

  @JsonProperty("workspaceId")
  private String workspaceId = null;

  @JsonProperty("notebookNames")
  private List<String> notebookNames = new ArrayList<String>();

  @JsonProperty("playgroundMode")
  private Boolean playgroundMode = false;

  public ClusterLocalizeRequest workspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
    return this;
  }

   /**
   * Workspace namespace from which to source notebooks
   * @return workspaceNamespace
  **/
  @ApiModelProperty(required = true, value = "Workspace namespace from which to source notebooks")
  @NotNull


  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public void setWorkspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
  }

  public ClusterLocalizeRequest workspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

   /**
   * Workspace from which to source notebooks
   * @return workspaceId
  **/
  @ApiModelProperty(required = true, value = "Workspace from which to source notebooks")
  @NotNull


  public String getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public ClusterLocalizeRequest notebookNames(List<String> notebookNames) {
    this.notebookNames = notebookNames;
    return this;
  }

  public ClusterLocalizeRequest addNotebookNamesItem(String notebookNamesItem) {
    this.notebookNames.add(notebookNamesItem);
    return this;
  }

   /**
   * Names of notebooks to localize. This is just the basename (no gs:// needed). This is interpreted as relative to the /notebooks directory within the provided workspace's Google Cloud Storage bucket. 
   * @return notebookNames
  **/
  @ApiModelProperty(required = true, value = "Names of notebooks to localize. This is just the basename (no gs:// needed). This is interpreted as relative to the /notebooks directory within the provided workspace's Google Cloud Storage bucket. ")
  @NotNull


  public List<String> getNotebookNames() {
    return notebookNames;
  }

  public void setNotebookNames(List<String> notebookNames) {
    this.notebookNames = notebookNames;
  }

  public ClusterLocalizeRequest playgroundMode(Boolean playgroundMode) {
    this.playgroundMode = playgroundMode;
    return this;
  }

   /**
   * Set to true if playgroundMode needed
   * @return playgroundMode
  **/
  @ApiModelProperty(required = true, value = "Set to true if playgroundMode needed")
  @NotNull


  public Boolean getPlaygroundMode() {
    return playgroundMode;
  }

  public void setPlaygroundMode(Boolean playgroundMode) {
    this.playgroundMode = playgroundMode;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterLocalizeRequest clusterLocalizeRequest = (ClusterLocalizeRequest) o;
    return Objects.equals(this.workspaceNamespace, clusterLocalizeRequest.workspaceNamespace) &&
        Objects.equals(this.workspaceId, clusterLocalizeRequest.workspaceId) &&
        Objects.equals(this.notebookNames, clusterLocalizeRequest.notebookNames) &&
        Objects.equals(this.playgroundMode, clusterLocalizeRequest.playgroundMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceNamespace, workspaceId, notebookNames, playgroundMode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterLocalizeRequest {\n");
    
    sb.append("    workspaceNamespace: ").append(toIndentedString(workspaceNamespace)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    notebookNames: ").append(toIndentedString(notebookNames)).append("\n");
    sb.append("    playgroundMode: ").append(toIndentedString(playgroundMode)).append("\n");
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

