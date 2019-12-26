package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Attribute;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * SearchParameter
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class SearchParameter   {
  @JsonProperty("parameterId")
  private String parameterId = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("value")
  private String value = null;

  @JsonProperty("domain")
  private String domain = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("subtype")
  private String subtype = null;

  @JsonProperty("group")
  private Boolean group = false;

  @JsonProperty("ancestorData")
  private Boolean ancestorData = false;

  @JsonProperty("standard")
  private Boolean standard = false;

  @JsonProperty("conceptId")
  private Long conceptId = null;

  @JsonProperty("attributes")
  private List<Attribute> attributes = new ArrayList<Attribute>();

  public SearchParameter parameterId(String parameterId) {
    this.parameterId = parameterId;
    return this;
  }

   /**
   * Unique within the cohort definition
   * @return parameterId
  **/
  @ApiModelProperty(required = true, value = "Unique within the cohort definition")
  @NotNull


  public String getParameterId() {
    return parameterId;
  }

  public void setParameterId(String parameterId) {
    this.parameterId = parameterId;
  }

  public SearchParameter name(String name) {
    this.name = name;
    return this;
  }

   /**
   * The name of the generating Criterion
   * @return name
  **/
  @ApiModelProperty(required = true, value = "The name of the generating Criterion")
  @NotNull


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SearchParameter value(String value) {
    this.value = value;
    return this;
  }

   /**
   * The Criteria code (or name if querying for Deceased)
   * @return value
  **/
  @ApiModelProperty(value = "The Criteria code (or name if querying for Deceased)")


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public SearchParameter domain(String domain) {
    this.domain = domain;
    return this;
  }

   /**
   * The omop domain that this criteria belongs to
   * @return domain
  **/
  @ApiModelProperty(required = true, value = "The omop domain that this criteria belongs to")
  @NotNull


  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public SearchParameter type(String type) {
    this.type = type;
    return this;
  }

   /**
   * The type of the generating Criterion
   * @return type
  **/
  @ApiModelProperty(required = true, value = "The type of the generating Criterion")
  @NotNull


  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public SearchParameter subtype(String subtype) {
    this.subtype = subtype;
    return this;
  }

   /**
   * The subtype of the generating Criterion
   * @return subtype
  **/
  @ApiModelProperty(value = "The subtype of the generating Criterion")


  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }

  public SearchParameter group(Boolean group) {
    this.group = group;
    return this;
  }

   /**
   * Specifies if the generating Criterion is a leaf or node in its Criteria tree
   * @return group
  **/
  @ApiModelProperty(required = true, value = "Specifies if the generating Criterion is a leaf or node in its Criteria tree")
  @NotNull


  public Boolean getGroup() {
    return group;
  }

  public void setGroup(Boolean group) {
    this.group = group;
  }

  public SearchParameter ancestorData(Boolean ancestorData) {
    this.ancestorData = ancestorData;
    return this;
  }

   /**
   * flag that determines if concept ids need to be looked up in the ancestor table
   * @return ancestorData
  **/
  @ApiModelProperty(required = true, value = "flag that determines if concept ids need to be looked up in the ancestor table")
  @NotNull


  public Boolean getAncestorData() {
    return ancestorData;
  }

  public void setAncestorData(Boolean ancestorData) {
    this.ancestorData = ancestorData;
  }

  public SearchParameter standard(Boolean standard) {
    this.standard = standard;
    return this;
  }

   /**
   * flag that determines standard or source
   * @return standard
  **/
  @ApiModelProperty(required = true, value = "flag that determines standard or source")
  @NotNull


  public Boolean getStandard() {
    return standard;
  }

  public void setStandard(Boolean standard) {
    this.standard = standard;
  }

  public SearchParameter conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

   /**
   * The concept id that maps to concept table, if any.
   * @return conceptId
  **/
  @ApiModelProperty(value = "The concept id that maps to concept table, if any.")


  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public SearchParameter attributes(List<Attribute> attributes) {
    this.attributes = attributes;
    return this;
  }

  public SearchParameter addAttributesItem(Attribute attributesItem) {
    this.attributes.add(attributesItem);
    return this;
  }

   /**
   * Attributes are extra inputs provided by users through the UX. Currently used by Demographics(Age), Measurements(Body Height, Weight.. etc) and Physical Measurements(Blood Pressure, Body Weight.. etc). Measurements and Physical Measurements have overlap in many areas such as body weight, but the difference is standard(measurements) and source(physical measurements).   Example demo age search parameter:  {    \"type\": \"DEMO\",    \"searchParameters\": [      {        \"type\": \"DEMO\",        \"subtype\": \"AGE\",        \"group\": false,        \"attributes\": [{\"name\": \"Age\", \"operator\": \"BETWEEN\", \"operands\": [23, 31]}]      }    ],    \"modifiers\": []  }   Example physical measurement blood pressure(Hypotensive (Systolic <= 90 / Diastolic <= 60))  {    \"type\": \"PM\",    \"searchParameters\": [      {        \"type\": \"PM\",        \"subtype\": \"BP\",        \"group\": false,        \"attributes\": [          {            \"conceptId\": 903118,            \"name\": \"Systolic\",            \"operands\": [\"90\"],            \"operator\":\"LESS_THAN_OR_EQUAL_TO\"          },          {            \"conceptId\": 903115,            \"name\": \"Diastolic\",            \"operands\": [\"60\"],             \"operator\": \"LESS_THAN_OR_EQUAL_TO\"           }         ]       }     ],     \"modifiers\": []   } 
   * @return attributes
  **/
  @ApiModelProperty(required = true, value = "Attributes are extra inputs provided by users through the UX. Currently used by Demographics(Age), Measurements(Body Height, Weight.. etc) and Physical Measurements(Blood Pressure, Body Weight.. etc). Measurements and Physical Measurements have overlap in many areas such as body weight, but the difference is standard(measurements) and source(physical measurements).   Example demo age search parameter:  {    \"type\": \"DEMO\",    \"searchParameters\": [      {        \"type\": \"DEMO\",        \"subtype\": \"AGE\",        \"group\": false,        \"attributes\": [{\"name\": \"Age\", \"operator\": \"BETWEEN\", \"operands\": [23, 31]}]      }    ],    \"modifiers\": []  }   Example physical measurement blood pressure(Hypotensive (Systolic <= 90 / Diastolic <= 60))  {    \"type\": \"PM\",    \"searchParameters\": [      {        \"type\": \"PM\",        \"subtype\": \"BP\",        \"group\": false,        \"attributes\": [          {            \"conceptId\": 903118,            \"name\": \"Systolic\",            \"operands\": [\"90\"],            \"operator\":\"LESS_THAN_OR_EQUAL_TO\"          },          {            \"conceptId\": 903115,            \"name\": \"Diastolic\",            \"operands\": [\"60\"],             \"operator\": \"LESS_THAN_OR_EQUAL_TO\"           }         ]       }     ],     \"modifiers\": []   } ")
  @NotNull

  @Valid

  public List<Attribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<Attribute> attributes) {
    this.attributes = attributes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchParameter searchParameter = (SearchParameter) o;
    return Objects.equals(this.parameterId, searchParameter.parameterId) &&
        Objects.equals(this.name, searchParameter.name) &&
        Objects.equals(this.value, searchParameter.value) &&
        Objects.equals(this.domain, searchParameter.domain) &&
        Objects.equals(this.type, searchParameter.type) &&
        Objects.equals(this.subtype, searchParameter.subtype) &&
        Objects.equals(this.group, searchParameter.group) &&
        Objects.equals(this.ancestorData, searchParameter.ancestorData) &&
        Objects.equals(this.standard, searchParameter.standard) &&
        Objects.equals(this.conceptId, searchParameter.conceptId) &&
        Objects.equals(this.attributes, searchParameter.attributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parameterId, name, value, domain, type, subtype, group, ancestorData, standard, conceptId, attributes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchParameter {\n");
    
    sb.append("    parameterId: ").append(toIndentedString(parameterId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subtype: ").append(toIndentedString(subtype)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
    sb.append("    ancestorData: ").append(toIndentedString(ancestorData)).append("\n");
    sb.append("    standard: ").append(toIndentedString(standard)).append("\n");
    sb.append("    conceptId: ").append(toIndentedString(conceptId)).append("\n");
    sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
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

