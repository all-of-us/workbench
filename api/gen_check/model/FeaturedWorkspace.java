package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * FeaturedWorkspace
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class FeaturedWorkspace   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("namespace")
  private String namespace = null;

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("category")
  private FeaturedWorkspaceCategory category = null;

  public FeaturedWorkspace name(String name) {
    this.name = name;
    return this;
  }

   /**
   * the name of the workspace
   * @return name
  **/
  @ApiModelProperty(value = "the name of the workspace")


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FeaturedWorkspace namespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

   /**
   * the namespace of the workspace
   * @return namespace
  **/
  @ApiModelProperty(value = "the namespace of the workspace")


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public FeaturedWorkspace id(String id) {
    this.id = id;
    return this;
  }

   /**
   * the Firecloud id of the workspace
   * @return id
  **/
  @ApiModelProperty(value = "the Firecloud id of the workspace")


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public FeaturedWorkspace category(FeaturedWorkspaceCategory category) {
    this.category = category;
    return this;
  }

   /**
   * The category that this workspace belongs to
   * @return category
  **/
  @ApiModelProperty(value = "The category that this workspace belongs to")

  @Valid

  public FeaturedWorkspaceCategory getCategory() {
    return category;
  }

  public void setCategory(FeaturedWorkspaceCategory category) {
    this.category = category;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeaturedWorkspace featuredWorkspace = (FeaturedWorkspace) o;
    return Objects.equals(this.name, featuredWorkspace.name) &&
        Objects.equals(this.namespace, featuredWorkspace.namespace) &&
        Objects.equals(this.id, featuredWorkspace.id) &&
        Objects.equals(this.category, featuredWorkspace.category);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, namespace, id, category);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeaturedWorkspace {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
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

