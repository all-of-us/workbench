package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ParticipantData;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantDataListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class ParticipantDataListResponse   {
  @JsonProperty("items")
  private List<ParticipantData> items = new ArrayList<ParticipantData>();

  @JsonProperty("count")
  private Long count = null;

  public ParticipantDataListResponse items(List<ParticipantData> items) {
    this.items = items;
    return this;
  }

  public ParticipantDataListResponse addItemsItem(ParticipantData itemsItem) {
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

  public List<ParticipantData> getItems() {
    return items;
  }

  public void setItems(List<ParticipantData> items) {
    this.items = items;
  }

  public ParticipantDataListResponse count(Long count) {
    this.count = count;
    return this;
  }

   /**
   * the total count of items.
   * @return count
  **/
  @ApiModelProperty(required = true, value = "the total count of items.")
  @NotNull


  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ParticipantDataListResponse participantDataListResponse = (ParticipantDataListResponse) o;
    return Objects.equals(this.items, participantDataListResponse.items) &&
        Objects.equals(this.count, participantDataListResponse.count);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, count);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantDataListResponse {\n");
    
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
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

