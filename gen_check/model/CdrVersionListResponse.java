package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.CdrVersion;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CdrVersionListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class CdrVersionListResponse   {
  @JsonProperty("items")
  private List<CdrVersion> items = new ArrayList<CdrVersion>();

  @JsonProperty("defaultCdrVersionId")
  private String defaultCdrVersionId = null;

  public CdrVersionListResponse items(List<CdrVersion> items) {
    this.items = items;
    return this;
  }

  public CdrVersionListResponse addItemsItem(CdrVersion itemsItem) {
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

  public List<CdrVersion> getItems() {
    return items;
  }

  public void setItems(List<CdrVersion> items) {
    this.items = items;
  }

  public CdrVersionListResponse defaultCdrVersionId(String defaultCdrVersionId) {
    this.defaultCdrVersionId = defaultCdrVersionId;
    return this;
  }

   /**
   * ID of the CDR versions that should be used by the user by default
   * @return defaultCdrVersionId
  **/
  @ApiModelProperty(required = true, value = "ID of the CDR versions that should be used by the user by default")
  @NotNull


  public String getDefaultCdrVersionId() {
    return defaultCdrVersionId;
  }

  public void setDefaultCdrVersionId(String defaultCdrVersionId) {
    this.defaultCdrVersionId = defaultCdrVersionId;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CdrVersionListResponse cdrVersionListResponse = (CdrVersionListResponse) o;
    return Objects.equals(this.items, cdrVersionListResponse.items) &&
        Objects.equals(this.defaultCdrVersionId, cdrVersionListResponse.defaultCdrVersionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, defaultCdrVersionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CdrVersionListResponse {\n");
    
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    defaultCdrVersionId: ").append(toIndentedString(defaultCdrVersionId)).append("\n");
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

