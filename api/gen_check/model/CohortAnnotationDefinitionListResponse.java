package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortAnnotationDefinitionListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public class CohortAnnotationDefinitionListResponse   {
  @JsonProperty("items")
  private List<CohortAnnotationDefinition> items = new ArrayList<CohortAnnotationDefinition>();

  public CohortAnnotationDefinitionListResponse items(List<CohortAnnotationDefinition> items) {
    this.items = items;
    return this;
  }

  public CohortAnnotationDefinitionListResponse addItemsItem(CohortAnnotationDefinition itemsItem) {
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

  public List<CohortAnnotationDefinition> getItems() {
    return items;
  }

  public void setItems(List<CohortAnnotationDefinition> items) {
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
    CohortAnnotationDefinitionListResponse cohortAnnotationDefinitionListResponse = (CohortAnnotationDefinitionListResponse) o;
    return Objects.equals(this.items, cohortAnnotationDefinitionListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortAnnotationDefinitionListResponse {\n");
    
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

