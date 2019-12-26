package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.UserRole;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * WorkspaceUserRolesResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class WorkspaceUserRolesResponse   {
  @JsonProperty("workspaceEtag")
  private String workspaceEtag = null;

  @JsonProperty("items")
  private List<UserRole> items = null;

  public WorkspaceUserRolesResponse workspaceEtag(String workspaceEtag) {
    this.workspaceEtag = workspaceEtag;
    return this;
  }

   /**
   * Updated workspace etag after the share request has been applied. 
   * @return workspaceEtag
  **/
  @ApiModelProperty(value = "Updated workspace etag after the share request has been applied. ")


  public String getWorkspaceEtag() {
    return workspaceEtag;
  }

  public void setWorkspaceEtag(String workspaceEtag) {
    this.workspaceEtag = workspaceEtag;
  }

  public WorkspaceUserRolesResponse items(List<UserRole> items) {
    this.items = items;
    return this;
  }

  public WorkspaceUserRolesResponse addItemsItem(UserRole itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<UserRole>();
    }
    this.items.add(itemsItem);
    return this;
  }

   /**
   * Get items
   * @return items
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<UserRole> getItems() {
    return items;
  }

  public void setItems(List<UserRole> items) {
    this.items = items;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkspaceUserRolesResponse workspaceUserRolesResponse = (WorkspaceUserRolesResponse) o;
    return Objects.equals(this.workspaceEtag, workspaceUserRolesResponse.workspaceEtag) &&
        Objects.equals(this.items, workspaceUserRolesResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceEtag, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceUserRolesResponse {\n");
    
    sb.append("    workspaceEtag: ").append(toIndentedString(workspaceEtag)).append("\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

