package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.ConceptSet;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * CreateConceptSetRequest
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public class CreateConceptSetRequest   {
  @JsonProperty("conceptSet")
  private ConceptSet conceptSet = null;

  @JsonProperty("addedIds")
  private List<Long> addedIds = null;

  public CreateConceptSetRequest conceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
    return this;
  }

   /**
   * Concept set to be created; concepts is ignored
   * @return conceptSet
  **/
  @ApiModelProperty(required = true, value = "Concept set to be created; concepts is ignored")
  @NotNull

  @Valid

  public ConceptSet getConceptSet() {
    return conceptSet;
  }

  public void setConceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
  }

  public CreateConceptSetRequest addedIds(List<Long> addedIds) {
    this.addedIds = addedIds;
    return this;
  }

  public CreateConceptSetRequest addAddedIdsItem(Long addedIdsItem) {
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
  @ApiModelProperty(value = "The IDs of concepts to be added to the concept set.")


  public List<Long> getAddedIds() {
    return addedIds;
  }

  public void setAddedIds(List<Long> addedIds) {
    this.addedIds = addedIds;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateConceptSetRequest createConceptSetRequest = (CreateConceptSetRequest) o;
    return Objects.equals(this.conceptSet, createConceptSetRequest.conceptSet) &&
        Objects.equals(this.addedIds, createConceptSetRequest.addedIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conceptSet, addedIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateConceptSetRequest {\n");
    
    sb.append("    conceptSet: ").append(toIndentedString(conceptSet)).append("\n");
    sb.append("    addedIds: ").append(toIndentedString(addedIds)).append("\n");
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

