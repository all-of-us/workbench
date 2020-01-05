package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CriteriaMenuOption;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CriteriaMenuOptionsListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class CriteriaMenuOptionsListResponse   {
  @JsonProperty("items")
  private List<CriteriaMenuOption> items = new ArrayList<CriteriaMenuOption>();

  public CriteriaMenuOptionsListResponse items(List<CriteriaMenuOption> items) {
    this.items = items;
    return this;
  }

  public CriteriaMenuOptionsListResponse addItemsItem(CriteriaMenuOption itemsItem) {
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

  public List<CriteriaMenuOption> getItems() {
    return items;
  }

  public void setItems(List<CriteriaMenuOption> items) {
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
    CriteriaMenuOptionsListResponse criteriaMenuOptionsListResponse = (CriteriaMenuOptionsListResponse) o;
    return Objects.equals(this.items, criteriaMenuOptionsListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CriteriaMenuOptionsListResponse {\n");
    
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

