package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CohortChartData;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CohortChartDataListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class CohortChartDataListResponse   {
  @JsonProperty("count")
  private Long count = null;

  @JsonProperty("items")
  private List<CohortChartData> items = new ArrayList<CohortChartData>();

  public CohortChartDataListResponse count(Long count) {
    this.count = count;
    return this;
  }

   /**
   * total count for cohort.
   * @return count
  **/
  @ApiModelProperty(required = true, value = "total count for cohort.")
  @NotNull


  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public CohortChartDataListResponse items(List<CohortChartData> items) {
    this.items = items;
    return this;
  }

  public CohortChartDataListResponse addItemsItem(CohortChartData itemsItem) {
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

  public List<CohortChartData> getItems() {
    return items;
  }

  public void setItems(List<CohortChartData> items) {
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
    CohortChartDataListResponse cohortChartDataListResponse = (CohortChartDataListResponse) o;
    return Objects.equals(this.count, cohortChartDataListResponse.count) &&
        Objects.equals(this.items, cohortChartDataListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(count, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortChartDataListResponse {\n");
    
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

