package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.TemporalMention;
import org.pmiops.workbench.model.TemporalTime;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A SearchGroup is a container for groups of criteria which are &#x60;OR&#x60;ed together. 
 */
@ApiModel(description = "A SearchGroup is a container for groups of criteria which are `OR`ed together. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class SearchGroup   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("temporal")
  private Boolean temporal = false;

  @JsonProperty("mention")
  private TemporalMention mention = null;

  @JsonProperty("time")
  private TemporalTime time = null;

  @JsonProperty("timeValue")
  private Long timeValue = null;

  @JsonProperty("timeFrame")
  private String timeFrame = null;

  @JsonProperty("items")
  private List<SearchGroupItem> items = new ArrayList<SearchGroupItem>();

  public SearchGroup id(String id) {
    this.id = id;
    return this;
  }

   /**
   * Unique within the cohort definition
   * @return id
  **/
  @ApiModelProperty(value = "Unique within the cohort definition")


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SearchGroup temporal(Boolean temporal) {
    this.temporal = temporal;
    return this;
  }

   /**
   * indicates if this search group relates to time
   * @return temporal
  **/
  @ApiModelProperty(required = true, value = "indicates if this search group relates to time")
  @NotNull


  public Boolean getTemporal() {
    return temporal;
  }

  public void setTemporal(Boolean temporal) {
    this.temporal = temporal;
  }

  public SearchGroup mention(TemporalMention mention) {
    this.mention = mention;
    return this;
  }

   /**
   * first, last or any mention(used in temporal realtionships)
   * @return mention
  **/
  @ApiModelProperty(value = "first, last or any mention(used in temporal realtionships)")

  @Valid

  public TemporalMention getMention() {
    return mention;
  }

  public void setMention(TemporalMention mention) {
    this.mention = mention;
  }

  public SearchGroup time(TemporalTime time) {
    this.time = time;
    return this;
  }

   /**
   * time frame between temporal items
   * @return time
  **/
  @ApiModelProperty(value = "time frame between temporal items")

  @Valid

  public TemporalTime getTime() {
    return time;
  }

  public void setTime(TemporalTime time) {
    this.time = time;
  }

  public SearchGroup timeValue(Long timeValue) {
    this.timeValue = timeValue;
    return this;
  }

   /**
   * time value between temporal items
   * @return timeValue
  **/
  @ApiModelProperty(value = "time value between temporal items")


  public Long getTimeValue() {
    return timeValue;
  }

  public void setTimeValue(Long timeValue) {
    this.timeValue = timeValue;
  }

  public SearchGroup timeFrame(String timeFrame) {
    this.timeFrame = timeFrame;
    return this;
  }

   /**
   * time increments(Day, Month or Year)
   * @return timeFrame
  **/
  @ApiModelProperty(value = "time increments(Day, Month or Year)")


  public String getTimeFrame() {
    return timeFrame;
  }

  public void setTimeFrame(String timeFrame) {
    this.timeFrame = timeFrame;
  }

  public SearchGroup items(List<SearchGroupItem> items) {
    this.items = items;
    return this;
  }

  public SearchGroup addItemsItem(SearchGroupItem itemsItem) {
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

  public List<SearchGroupItem> getItems() {
    return items;
  }

  public void setItems(List<SearchGroupItem> items) {
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
    SearchGroup searchGroup = (SearchGroup) o;
    return Objects.equals(this.id, searchGroup.id) &&
        Objects.equals(this.temporal, searchGroup.temporal) &&
        Objects.equals(this.mention, searchGroup.mention) &&
        Objects.equals(this.time, searchGroup.time) &&
        Objects.equals(this.timeValue, searchGroup.timeValue) &&
        Objects.equals(this.timeFrame, searchGroup.timeFrame) &&
        Objects.equals(this.items, searchGroup.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, temporal, mention, time, timeValue, timeFrame, items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchGroup {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    temporal: ").append(toIndentedString(temporal)).append("\n");
    sb.append("    mention: ").append(toIndentedString(mention)).append("\n");
    sb.append("    time: ").append(toIndentedString(time)).append("\n");
    sb.append("    timeValue: ").append(toIndentedString(timeValue)).append("\n");
    sb.append("    timeFrame: ").append(toIndentedString(timeFrame)).append("\n");
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

