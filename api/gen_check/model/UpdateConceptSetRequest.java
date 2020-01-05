package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * UpdateConceptSetRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

public class UpdateConceptSetRequest   {
  @JsonProperty("etag")
  private String etag = null;

  @JsonProperty("addedIds")
  private List<Long> addedIds = null;

  @JsonProperty("removedIds")
  private List<Long> removedIds = null;

  public UpdateConceptSetRequest etag(String etag) {
    this.etag = etag;
    return this;
  }

   /**
   * Etag of the concept set being modified. Validates that the concept set has not been modified since this etag was retrieved. 
   * @return etag
  **/
  @ApiModelProperty(required = true, value = "Etag of the concept set being modified. Validates that the concept set has not been modified since this etag was retrieved. ")
  @NotNull


  public String getEtag() {
    return etag;
  }

  public void setEtag(String etag) {
    this.etag = etag;
  }

  public UpdateConceptSetRequest addedIds(List<Long> addedIds) {
    this.addedIds = addedIds;
    return this;
  }

  public UpdateConceptSetRequest addAddedIdsItem(Long addedIdsItem) {
    if (this.addedIds == null) {
      this.addedIds = new ArrayList<Long>();
    }
    this.addedIds.add(addedIdsItem);
    return this;
  }

   /**
   * The IDs of concepts to be added to the concept set. 
   * @return addedIds
  **/
  @ApiModelProperty(value = "The IDs of concepts to be added to the concept set. ")


  public List<Long> getAddedIds() {
    return addedIds;
  }

  public void setAddedIds(List<Long> addedIds) {
    this.addedIds = addedIds;
  }

  public UpdateConceptSetRequest removedIds(List<Long> removedIds) {
    this.removedIds = removedIds;
    return this;
  }

  public UpdateConceptSetRequest addRemovedIdsItem(Long removedIdsItem) {
    if (this.removedIds == null) {
      this.removedIds = new ArrayList<Long>();
    }
    this.removedIds.add(removedIdsItem);
    return this;
  }

   /**
   * The IDs of concepts to be removed from the concept set. 
   * @return removedIds
  **/
  @ApiModelProperty(value = "The IDs of concepts to be removed from the concept set. ")


  public List<Long> getRemovedIds() {
    return removedIds;
  }

  public void setRemovedIds(List<Long> removedIds) {
    this.removedIds = removedIds;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateConceptSetRequest updateConceptSetRequest = (UpdateConceptSetRequest) o;
    return Objects.equals(this.etag, updateConceptSetRequest.etag) &&
        Objects.equals(this.addedIds, updateConceptSetRequest.addedIds) &&
        Objects.equals(this.removedIds, updateConceptSetRequest.removedIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag, addedIds, removedIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateConceptSetRequest {\n");
    
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    addedIds: ").append(toIndentedString(addedIds)).append("\n");
    sb.append("    removedIds: ").append(toIndentedString(removedIds)).append("\n");
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

