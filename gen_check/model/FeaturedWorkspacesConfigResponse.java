package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.FeaturedWorkspace;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * FeaturedWorkspacesConfigResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class FeaturedWorkspacesConfigResponse   {
  @JsonProperty("featuredWorkspacesList")
  private List<FeaturedWorkspace> featuredWorkspacesList = null;

  public FeaturedWorkspacesConfigResponse featuredWorkspacesList(List<FeaturedWorkspace> featuredWorkspacesList) {
    this.featuredWorkspacesList = featuredWorkspacesList;
    return this;
  }

  public FeaturedWorkspacesConfigResponse addFeaturedWorkspacesListItem(FeaturedWorkspace featuredWorkspacesListItem) {
    if (this.featuredWorkspacesList == null) {
      this.featuredWorkspacesList = new ArrayList<FeaturedWorkspace>();
    }
    this.featuredWorkspacesList.add(featuredWorkspacesListItem);
    return this;
  }

   /**
   * Get featuredWorkspacesList
   * @return featuredWorkspacesList
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<FeaturedWorkspace> getFeaturedWorkspacesList() {
    return featuredWorkspacesList;
  }

  public void setFeaturedWorkspacesList(List<FeaturedWorkspace> featuredWorkspacesList) {
    this.featuredWorkspacesList = featuredWorkspacesList;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeaturedWorkspacesConfigResponse featuredWorkspacesConfigResponse = (FeaturedWorkspacesConfigResponse) o;
    return Objects.equals(this.featuredWorkspacesList, featuredWorkspacesConfigResponse.featuredWorkspacesList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featuredWorkspacesList);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeaturedWorkspacesConfigResponse {\n");
    
    sb.append("    featuredWorkspacesList: ").append(toIndentedString(featuredWorkspacesList)).append("\n");
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

