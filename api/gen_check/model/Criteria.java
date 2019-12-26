package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Criteria
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

public class Criteria   {
  @JsonProperty("id")
  private Long id = null;

  @JsonProperty("parentId")
  private Long parentId = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("subtype")
  private String subtype = null;

  @JsonProperty("code")
  private String code = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("count")
  private Long count = null;

  @JsonProperty("group")
  private Boolean group = false;

  @JsonProperty("selectable")
  private Boolean selectable = false;

  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("domainId")
  private String domainId = null;

  @JsonProperty("hasAttributes")
  private Boolean hasAttributes = false;

  @JsonProperty("path")
  private String path = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("hasHierarchy")
  private Boolean hasHierarchy = false;

  @JsonProperty("hasAncestorData")
  private Boolean hasAncestorData = false;

  @JsonProperty("isStandard")
  private Boolean isStandard = false;

  public Criteria id(Long id) {
    this.id = id;
    return this;
  }

   /**
   * Primary identifier which is unique within a CDR version. Value may not be stable across different CDR versions. 
   * @return id
  **/
  @ApiModelProperty(required = true, value = "Primary identifier which is unique within a CDR version. Value may not be stable across different CDR versions. ")
  @NotNull


  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Criteria parentId(Long parentId) {
    this.parentId = parentId;
    return this;
  }

   /**
   * The parent id of the criteria. 0 if this is the root node of a criteria tree. 
   * @return parentId
  **/
  @ApiModelProperty(required = true, value = "The parent id of the criteria. 0 if this is the root node of a criteria tree. ")
  @NotNull


  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Criteria type(String type) {
    this.type = type;
    return this;
  }

   /**
   * The tree type of this criteria, see TreeType. This will need to change to CriteriaType with the new implementation(change type below to ref CriteriaType) 
   * @return type
  **/
  @ApiModelProperty(required = true, value = "The tree type of this criteria, see TreeType. This will need to change to CriteriaType with the new implementation(change type below to ref CriteriaType) ")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Criteria subtype(String subtype) {
    this.subtype = subtype;
    return this;
  }

   /**
   * The subtype of this criteria, see TreeSubType. This will need to change to CriteriaSubType with the new implementation(change type below to ref CriteriaSubType) 
   * @return subtype
  **/
  @ApiModelProperty(value = "The subtype of this criteria, see TreeSubType. This will need to change to CriteriaSubType with the new implementation(change type below to ref CriteriaSubType) ")


  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  public Criteria code(String code) {
    this.code = code;
    return this;
  }

   /**
   * Code that identifies this criteria. In some vocabularies such as ICD9 and ICD10, this code captures the tree hierarchy, e.g. '001.002.003'. Multiple criteria may exist for the same code within a CDR version if a given concept has multiple entries at different locations in the criteria tree (this is common in SNOMED). Criteria codes should generally be stable across CDR versions. 
   * @return code
  **/
  @ApiModelProperty(value = "Code that identifies this criteria. In some vocabularies such as ICD9 and ICD10, this code captures the tree hierarchy, e.g. '001.002.003'. Multiple criteria may exist for the same code within a CDR version if a given concept has multiple entries at different locations in the criteria tree (this is common in SNOMED). Criteria codes should generally be stable across CDR versions. ")


  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public Criteria name(String name) {
    this.name = name;
    return this;
  }

   /**
   * description of criteria
   * @return name
  **/
  @ApiModelProperty(required = true, value = "description of criteria")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Criteria count(Long count) {
    this.count = count;
    return this;
  }

   /**
   * Estimated number of participants in a particular CDR version which have a least one event matching this criteria. 
   * @return count
  **/
  @ApiModelProperty(value = "Estimated number of participants in a particular CDR version which have a least one event matching this criteria. ")


  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  public Criteria group(Boolean group) {
    this.group = group;
    return this;
  }

   /**
   * specifies if child or parent
   * @return group
  **/
  @ApiModelProperty(required = true, value = "specifies if child or parent")
  @NotNull


  public Boolean getGroup() {
    return group;
  }

  public void setGroup(Boolean group) {
    this.group = group;
  }

  public Criteria selectable(Boolean selectable) {
    this.selectable = selectable;
    return this;
  }

   /**
   * Whether or not a client can specify this criteria in a search request. Selecting a group implies selecting all criteria contained within that group. 
   * @return selectable
  **/
  @ApiModelProperty(required = true, value = "Whether or not a client can specify this criteria in a search request. Selecting a group implies selecting all criteria contained within that group. ")
  @NotNull


  public Boolean getSelectable() {
    return selectable;
  }

  public void setSelectable(Boolean selectable) {
    this.selectable = selectable;
  }

  public Criteria conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * The OMOP concept id associated with this criteria. May be null if this criteria does not match an OMOP concept, e.g. for intermediate nodes (groups) in the criteria tree. Concept ids are ids associated with a specific vocabulary item in the concept table and allow you to search for that code in its domain table. This may be a source or standard concept ID, depending on the tree for the criteria. Similar to a code, multiple criteria may reference the same concept ID due to multiple occurrences of a concept in the criteria tree. In other cases a criteria may share the concept ID of its parent, e.g. in the PPI tree a question and child answer share a concept id. Concept IDs should generally be stable across CDR versions. 
   * @return conceptId
  **/
  @ApiModelProperty(value = "The OMOP concept id associated with this criteria. May be null if this criteria does not match an OMOP concept, e.g. for intermediate nodes (groups) in the criteria tree. Concept ids are ids associated with a specific vocabulary item in the concept table and allow you to search for that code in its domain table. This may be a source or standard concept ID, depending on the tree for the criteria. Similar to a code, multiple criteria may reference the same concept ID due to multiple occurrences of a concept in the criteria tree. In other cases a criteria may share the concept ID of its parent, e.g. in the PPI tree a question and child answer share a concept id. Concept IDs should generally be stable across CDR versions. ")


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public Criteria domainId(String domainId) {
    this.domainId = domainId;
    return this;
  }

   /**
   * # TODO: Remove this field. Deprecated. Clue to determine which OMOP tables to search, but these only exist for leaves in the tree. Parents don't have domain ids and concept id will be used in the case that a parent is selectable. 
   * @return domainId
  **/
  @ApiModelProperty(value = "# TODO: Remove this field. Deprecated. Clue to determine which OMOP tables to search, but these only exist for leaves in the tree. Parents don't have domain ids and concept id will be used in the case that a parent is selectable. ")


  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public Criteria hasAttributes(Boolean hasAttributes) {
    this.hasAttributes = hasAttributes;
    return this;
  }

   /**
   * Whether this criteria has associated attributes which can be filtered by value during a search, for example a blood pressure measurement criteria might have an associated measurement value attribute. 
   * @return hasAttributes
  **/
  @ApiModelProperty(required = true, value = "Whether this criteria has associated attributes which can be filtered by value during a search, for example a blood pressure measurement criteria might have an associated measurement value attribute. ")
  @NotNull


  public Boolean getHasAttributes() {
    return hasAttributes;
  }

  public void setHasAttributes(Boolean hasAttributes) {
    this.hasAttributes = hasAttributes;
  }

  public Criteria path(String path) {
    this.path = path;
    return this;
  }

   /**
   * A \".\" delimited path of all parent criteria IDs. Does not include the id for this criteria; root criteria nodes have a null path. 
   * @return path
  **/
  @ApiModelProperty(value = "A \".\" delimited path of all parent criteria IDs. Does not include the id for this criteria; root criteria nodes have a null path. ")


  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Criteria value(String value) {
    this.value = value;
    return this;
  }

   /**
   * A helper property to fully describe PPI/AGE data. Age holds the value of the age and for PPI it holds value as number or value as concept id. 
   * @return value
  **/
  @ApiModelProperty(value = "A helper property to fully describe PPI/AGE data. Age holds the value of the age and for PPI it holds value as number or value as concept id. ")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Criteria hasHierarchy(Boolean hasHierarchy) {
    this.hasHierarchy = hasHierarchy;
    return this;
  }

   /**
   * Whether criteria is linked to a tree.
   * @return hasHierarchy
  **/
  @ApiModelProperty(value = "Whether criteria is linked to a tree.")


  public Boolean getHasHierarchy() {
    return hasHierarchy;
  }

  public void setHasHierarchy(Boolean hasHierarchy) {
    this.hasHierarchy = hasHierarchy;
  }

  public Criteria hasAncestorData(Boolean hasAncestorData) {
    this.hasAncestorData = hasAncestorData;
    return this;
  }

   /**
   * Whether criteria needs lookup in the criteria_ancestor table.
   * @return hasAncestorData
  **/
  @ApiModelProperty(value = "Whether criteria needs lookup in the criteria_ancestor table.")


  public Boolean getHasAncestorData() {
    return hasAncestorData;
  }

  public void setHasAncestorData(Boolean hasAncestorData) {
    this.hasAncestorData = hasAncestorData;
  }

  public Criteria isStandard(Boolean isStandard) {
    this.isStandard = isStandard;
    return this;
  }

   /**
   * Reveals if this criteria is standard or source.
   * @return isStandard
  **/
  @ApiModelProperty(value = "Reveals if this criteria is standard or source.")


  public Boolean getIsStandard() {
    return isStandard;
  }

  public void setIsStandard(Boolean isStandard) {
    this.isStandard = isStandard;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Criteria criteria = (Criteria) o;
    return Objects.equals(this.id, criteria.id) &&
        Objects.equals(this.parentId, criteria.parentId) &&
        Objects.equals(this.type, criteria.type) &&
        Objects.equals(this.subtype, criteria.subtype) &&
        Objects.equals(this.code, criteria.code) &&
        Objects.equals(this.name, criteria.name) &&
        Objects.equals(this.count, criteria.count) &&
        Objects.equals(this.group, criteria.group) &&
        Objects.equals(this.selectable, criteria.selectable) &&
        Objects.equals(this.conceptId, criteria.conceptId) &&
        Objects.equals(this.domainId, criteria.domainId) &&
        Objects.equals(this.hasAttributes, criteria.hasAttributes) &&
        Objects.equals(this.path, criteria.path) &&
        Objects.equals(this.value, criteria.value) &&
        Objects.equals(this.hasHierarchy, criteria.hasHierarchy) &&
        Objects.equals(this.hasAncestorData, criteria.hasAncestorData) &&
        Objects.equals(this.isStandard, criteria.isStandard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, parentId, type, subtype, code, name, count, group, selectable, conceptId, domainId, hasAttributes, path, value, hasHierarchy, hasAncestorData, isStandard);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Criteria {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    parentId: ").append(toIndentedString(parentId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subtype: ").append(toIndentedString(subtype)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    selectable: ").append(toIndentedString(selectable)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    domainId: ").append(toIndentedString(domainId)).append("\n");
    sb.append("    hasAttributes: ").append(toIndentedString(hasAttributes)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    hasHierarchy: ").append(toIndentedString(hasHierarchy)).append("\n");
    sb.append("    hasAncestorData: ").append(toIndentedString(hasAncestorData)).append("\n");
    sb.append("    isStandard: ").append(toIndentedString(isStandard)).append("\n");
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

