package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.DomainCount;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ConceptListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class ConceptListResponse   {
  @JsonProperty("items")
  private List<Concept> items = new ArrayList<Concept>();

  @JsonProperty("domainCounts")
  private List<DomainCount> domainCounts = null;

  public ConceptListResponse items(List<Concept> items) {
    this.items = items;
    return this;
  }

  public ConceptListResponse addItemsItem(Concept itemsItem) {
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

  public List<Concept> getItems() {
    return items;
  }

  public void setItems(List<Concept> items) {
    this.items = items;
  }

  public ConceptListResponse domainCounts(List<DomainCount> domainCounts) {
    this.domainCounts = domainCounts;
    return this;
  }

  public ConceptListResponse addDomainCountsItem(DomainCount domainCountsItem) {
    if (this.domainCounts == null) {
      this.domainCounts = new ArrayList<DomainCount>();
    }
    this.domainCounts.add(domainCountsItem);
    return this;
  }

   /**
   * Get domainCounts
   * @return domainCounts
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<DomainCount> getDomainCounts() {
    return domainCounts;
  }

  public void setDomainCounts(List<DomainCount> domainCounts) {
    this.domainCounts = domainCounts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConceptListResponse conceptListResponse = (ConceptListResponse) o;
    return Objects.equals(this.items, conceptListResponse.items) &&
        Objects.equals(this.domainCounts, conceptListResponse.domainCounts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, domainCounts);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConceptListResponse {\n");
    
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    domainCounts: ").append(toIndentedString(domainCounts)).append("\n");
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

