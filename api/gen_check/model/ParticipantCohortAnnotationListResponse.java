package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ParticipantCohortAnnotationListResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public class ParticipantCohortAnnotationListResponse   {
  @JsonProperty("items")
  private List<ParticipantCohortAnnotation> items = new ArrayList<ParticipantCohortAnnotation>();

  public ParticipantCohortAnnotationListResponse items(List<ParticipantCohortAnnotation> items) {
    this.items = items;
    return this;
  }

  public ParticipantCohortAnnotationListResponse addItemsItem(ParticipantCohortAnnotation itemsItem) {
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

  public List<ParticipantCohortAnnotation> getItems() {
    return items;
  }

  public void setItems(List<ParticipantCohortAnnotation> items) {
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
    ParticipantCohortAnnotationListResponse participantCohortAnnotationListResponse = (ParticipantCohortAnnotationListResponse) o;
    return Objects.equals(this.items, participantCohortAnnotationListResponse.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ParticipantCohortAnnotationListResponse {\n");
    
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

