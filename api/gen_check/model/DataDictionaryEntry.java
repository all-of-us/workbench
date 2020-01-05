package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * DataDictionaryEntry
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:48:19.506-05:00")

public class DataDictionaryEntry   {
  @JsonProperty("cdrVersionId")
  private Long cdrVersionId = null;

  @JsonProperty("definedTime")
  private Long definedTime = null;

  @JsonProperty("relevantOmopTable")
  private String relevantOmopTable = null;

  @JsonProperty("fieldName")
  private String fieldName = null;

  @JsonProperty("omopCdmStandardOrCustomField")
  private String omopCdmStandardOrCustomField = null;

  @JsonProperty("description")
  private String description = null;

  @JsonProperty("fieldType")
  private String fieldType = null;

  @JsonProperty("dataProvenance")
  private String dataProvenance = null;

  @JsonProperty("sourcePpiModule")
  private String sourcePpiModule = null;

  @JsonProperty("transformedByRegisteredTierPrivacyMethods")
  private Boolean transformedByRegisteredTierPrivacyMethods = false;

  public DataDictionaryEntry cdrVersionId(Long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
    return this;
  }

   /**
   * Get cdrVersionId
   * @return cdrVersionId
  **/
  @ApiModelProperty(value = "")


  public Long getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(Long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  public DataDictionaryEntry definedTime(Long definedTime) {
    this.definedTime = definedTime;
    return this;
  }

   /**
   * Get definedTime
   * @return definedTime
  **/
  @ApiModelProperty(value = "")


  public Long getDefinedTime() {
    return definedTime;
  }

  public void setDefinedTime(Long definedTime) {
    this.definedTime = definedTime;
  }

  public DataDictionaryEntry relevantOmopTable(String relevantOmopTable) {
    this.relevantOmopTable = relevantOmopTable;
    return this;
  }

   /**
   * Get relevantOmopTable
   * @return relevantOmopTable
  **/
  @ApiModelProperty(value = "")


  public String getRelevantOmopTable() {
    return relevantOmopTable;
  }

  public void setRelevantOmopTable(String relevantOmopTable) {
    this.relevantOmopTable = relevantOmopTable;
  }

  public DataDictionaryEntry fieldName(String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

   /**
   * Get fieldName
   * @return fieldName
  **/
  @ApiModelProperty(value = "")


  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public DataDictionaryEntry omopCdmStandardOrCustomField(String omopCdmStandardOrCustomField) {
    this.omopCdmStandardOrCustomField = omopCdmStandardOrCustomField;
    return this;
  }

   /**
   * Get omopCdmStandardOrCustomField
   * @return omopCdmStandardOrCustomField
  **/
  @ApiModelProperty(value = "")


  public String getOmopCdmStandardOrCustomField() {
    return omopCdmStandardOrCustomField;
  }

  public void setOmopCdmStandardOrCustomField(String omopCdmStandardOrCustomField) {
    this.omopCdmStandardOrCustomField = omopCdmStandardOrCustomField;
  }

  public DataDictionaryEntry description(String description) {
    this.description = description;
    return this;
  }

   /**
   * Get description
   * @return description
  **/
  @ApiModelProperty(value = "")


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DataDictionaryEntry fieldType(String fieldType) {
    this.fieldType = fieldType;
    return this;
  }

   /**
   * Get fieldType
   * @return fieldType
  **/
  @ApiModelProperty(value = "")


  public String getFieldType() {
    return fieldType;
  }

  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  public DataDictionaryEntry dataProvenance(String dataProvenance) {
    this.dataProvenance = dataProvenance;
    return this;
  }

   /**
   * Get dataProvenance
   * @return dataProvenance
  **/
  @ApiModelProperty(value = "")


  public String getDataProvenance() {
    return dataProvenance;
  }

  public void setDataProvenance(String dataProvenance) {
    this.dataProvenance = dataProvenance;
  }

  public DataDictionaryEntry sourcePpiModule(String sourcePpiModule) {
    this.sourcePpiModule = sourcePpiModule;
    return this;
  }

   /**
   * Get sourcePpiModule
   * @return sourcePpiModule
  **/
  @ApiModelProperty(value = "")


  public String getSourcePpiModule() {
    return sourcePpiModule;
  }

  public void setSourcePpiModule(String sourcePpiModule) {
    this.sourcePpiModule = sourcePpiModule;
  }

  public DataDictionaryEntry transformedByRegisteredTierPrivacyMethods(Boolean transformedByRegisteredTierPrivacyMethods) {
    this.transformedByRegisteredTierPrivacyMethods = transformedByRegisteredTierPrivacyMethods;
    return this;
  }

   /**
   * Get transformedByRegisteredTierPrivacyMethods
   * @return transformedByRegisteredTierPrivacyMethods
  **/
  @ApiModelProperty(value = "")


  public Boolean getTransformedByRegisteredTierPrivacyMethods() {
    return transformedByRegisteredTierPrivacyMethods;
  }

  public void setTransformedByRegisteredTierPrivacyMethods(Boolean transformedByRegisteredTierPrivacyMethods) {
    this.transformedByRegisteredTierPrivacyMethods = transformedByRegisteredTierPrivacyMethods;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataDictionaryEntry dataDictionaryEntry = (DataDictionaryEntry) o;
    return Objects.equals(this.cdrVersionId, dataDictionaryEntry.cdrVersionId) &&
        Objects.equals(this.definedTime, dataDictionaryEntry.definedTime) &&
        Objects.equals(this.relevantOmopTable, dataDictionaryEntry.relevantOmopTable) &&
        Objects.equals(this.fieldName, dataDictionaryEntry.fieldName) &&
        Objects.equals(this.omopCdmStandardOrCustomField, dataDictionaryEntry.omopCdmStandardOrCustomField) &&
        Objects.equals(this.description, dataDictionaryEntry.description) &&
        Objects.equals(this.fieldType, dataDictionaryEntry.fieldType) &&
        Objects.equals(this.dataProvenance, dataDictionaryEntry.dataProvenance) &&
        Objects.equals(this.sourcePpiModule, dataDictionaryEntry.sourcePpiModule) &&
        Objects.equals(this.transformedByRegisteredTierPrivacyMethods, dataDictionaryEntry.transformedByRegisteredTierPrivacyMethods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cdrVersionId, definedTime, relevantOmopTable, fieldName, omopCdmStandardOrCustomField, description, fieldType, dataProvenance, sourcePpiModule, transformedByRegisteredTierPrivacyMethods);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataDictionaryEntry {\n");
    
    sb.append("    cdrVersionId: ").append(toIndentedString(cdrVersionId)).append("\n");
    sb.append("    definedTime: ").append(toIndentedString(definedTime)).append("\n");
    sb.append("    relevantOmopTable: ").append(toIndentedString(relevantOmopTable)).append("\n");
    sb.append("    fieldName: ").append(toIndentedString(fieldName)).append("\n");
    sb.append("    omopCdmStandardOrCustomField: ").append(toIndentedString(omopCdmStandardOrCustomField)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    fieldType: ").append(toIndentedString(fieldType)).append("\n");
    sb.append("    dataProvenance: ").append(toIndentedString(dataProvenance)).append("\n");
    sb.append("    sourcePpiModule: ").append(toIndentedString(sourcePpiModule)).append("\n");
    sb.append("    transformedByRegisteredTierPrivacyMethods: ").append(toIndentedString(transformedByRegisteredTierPrivacyMethods)).append("\n");
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

