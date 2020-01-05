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
 * ShareWorkspaceRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ShareWorkspaceRequest   {
  @JsonProperty("workspaceEtag")
  private String workspaceEtag = null;

  @JsonProperty("items")
  private List<UserRole> items = new ArrayList<UserRole>();

  public ShareWorkspaceRequest workspaceEtag(String workspaceEtag) {
    this.workspaceEtag = workspaceEtag;
    return this;
  }

   /**
   * Associated workspace etag retrieved via reading the workspaces API. If provided, validates that the workspace (and its user roles) has not been modified since this etag was retrieved. 
   * @return workspaceEtag
  **/
  @ApiModelProperty(value = "Associated workspace etag retrieved via reading the workspaces API. If provided, validates that the workspace (and its user roles) has not been modified since this etag was retrieved. ")


  public String getWorkspaceEtag() {
    return workspaceEtag;
  }

  public void setWorkspaceEtag(String workspaceEtag) {
    this.workspaceEtag = workspaceEtag;
  }

  public ShareWorkspaceRequest items(List<UserRole> items) {
    this.items = items;
    return this;
  }

  public ShareWorkspaceRequest addItemsItem(UserRole itemsItem) {
    this.items.add(itemsItem);
    return this;
  }

   /**
   * Get items
   * @return items
  **/
  @ApiModelProperty(required = true, value = "")
  @NotNull

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
    ShareWorkspaceRequest shareWorkspaceRequest = (ShareWorkspaceRequest) o;
    return Objects.equals(this.workspaceEtag, shareWorkspaceRequest.workspaceEtag) &&
        Objects.equals(this.items, shareWorkspaceRequest.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceEtag, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ShareWorkspaceRequest {\n");
    
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

