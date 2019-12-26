package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Modifier;
import org.pmiops.workbench.model.SearchParameter;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * A SearchGroupItem is the \&quot;line item\&quot; of the Cohort Builder.  It specifies a set of criteria of a given kind, possibly alongside a set of modifiers, the results of which are &#x60;OR&#x60;ed together with the other criteria in the group. 
 */
@ApiModel(description = "A SearchGroupItem is the \"line item\" of the Cohort Builder.  It specifies a set of criteria of a given kind, possibly alongside a set of modifiers, the results of which are `OR`ed together with the other criteria in the group. ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public class SearchGroupItem   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("temporalGroup")
  private Integer temporalGroup = null;

  @JsonProperty("searchParameters")
  private List<SearchParameter> searchParameters = new ArrayList<SearchParameter>();

  @JsonProperty("modifiers")
  private List<Modifier> modifiers = new ArrayList<Modifier>();

  public SearchGroupItem id(String id) {
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

  public SearchGroupItem type(String type) {
    this.type = type;
    return this;
  }

   /**
   * type of criteria
   * @return type
  **/
  @ApiModelProperty(required = true, value = "type of criteria")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public SearchGroupItem temporalGroup(Integer temporalGroup) {
    this.temporalGroup = temporalGroup;
    return this;
  }

   /**
   * temporal group that this item belongs to
   * @return temporalGroup
  **/
  @ApiModelProperty(value = "temporal group that this item belongs to")


  public Integer getTemporalGroup() {
    return temporalGroup;
  }

  public void setTemporalGroup(Integer temporalGroup) {
    this.temporalGroup = temporalGroup;
  }

  public SearchGroupItem searchParameters(List<SearchParameter> searchParameters) {
    this.searchParameters = searchParameters;
    return this;
  }

  public SearchGroupItem addSearchParametersItem(SearchParameter searchParametersItem) {
    this.searchParameters.add(searchParametersItem);
    return this;
  }

   /**
   * values that help search for subjects
   * @return searchParameters
  **/
  @ApiModelProperty(required = true, value = "values that help search for subjects")
  @NotNull

  @Valid

  public List<SearchParameter> getSearchParameters() {
    return searchParameters;
  }

  public void setSearchParameters(List<SearchParameter> searchParameters) {
    this.searchParameters = searchParameters;
  }

  public SearchGroupItem modifiers(List<Modifier> modifiers) {
    this.modifiers = modifiers;
    return this;
  }

  public SearchGroupItem addModifiersItem(Modifier modifiersItem) {
    this.modifiers.add(modifiersItem);
    return this;
  }

   /**
   * Predicates to apply to the search parameters. Aggregate modifiers (i.e. NUM_OF_OCCURRENCES) are applied independently to each SearchParameter and furthermore, are applied independently to any group elements within those SearchParameters. Consider the following example query scenario. Example criteria tree: - parent   - child1   - child2  Curated dataset contains 1 participant with 1 event each of concepts child1 and child2. Search request is made on \"parent\", with a modifier of {type: NUM_OF_OCCURRENCES, operands: [2]}. This does not match - the participant would need to have at least 2 events of type child1 and/or of child2 and/or of parent (in the event that \"parent\" corresponds to a real concept in the data). Additionally, aggregate modifiers are applied secondarily to other modifiers. For example, combining the AGE_AT_EVENT with NUM_OF_OCCURRENCES - first we filter down all events by the age predicate, then we count occurrences. 
   * @return modifiers
  **/
  @ApiModelProperty(required = true, value = "Predicates to apply to the search parameters. Aggregate modifiers (i.e. NUM_OF_OCCURRENCES) are applied independently to each SearchParameter and furthermore, are applied independently to any group elements within those SearchParameters. Consider the following example query scenario. Example criteria tree: - parent   - child1   - child2  Curated dataset contains 1 participant with 1 event each of concepts child1 and child2. Search request is made on \"parent\", with a modifier of {type: NUM_OF_OCCURRENCES, operands: [2]}. This does not match - the participant would need to have at least 2 events of type child1 and/or of child2 and/or of parent (in the event that \"parent\" corresponds to a real concept in the data). Additionally, aggregate modifiers are applied secondarily to other modifiers. For example, combining the AGE_AT_EVENT with NUM_OF_OCCURRENCES - first we filter down all events by the age predicate, then we count occurrences. ")
  @NotNull

  @Valid

  public List<Modifier> getModifiers() {
    return modifiers;
  }

  public void setModifiers(List<Modifier> modifiers) {
    this.modifiers = modifiers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchGroupItem searchGroupItem = (SearchGroupItem) o;
    return Objects.equals(this.id, searchGroupItem.id) &&
        Objects.equals(this.type, searchGroupItem.type) &&
        Objects.equals(this.temporalGroup, searchGroupItem.temporalGroup) &&
        Objects.equals(this.searchParameters, searchGroupItem.searchParameters) &&
        Objects.equals(this.modifiers, searchGroupItem.modifiers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, temporalGroup, searchParameters, modifiers);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchGroupItem {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    temporalGroup: ").append(toIndentedString(temporalGroup)).append("\n");
    sb.append("    searchParameters: ").append(toIndentedString(searchParameters)).append("\n");
    sb.append("    modifiers: ").append(toIndentedString(modifiers)).append("\n");
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

