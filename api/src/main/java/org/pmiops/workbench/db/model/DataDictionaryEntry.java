package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "data_dictionary_entry")
public class DataDictionaryEntry {

  private long dataDictionaryEntryId;
  private String relevantOmopTable;
  private String fieldName;
  private String omopCdmStandardOrCustomField;
  private String description;
  private String fieldType;
  private String dataProvenance;
  private String sourcePpiModule;
  private Boolean transformedByRegisteredTierPrivacyMethods;

  public DataDictionaryEntry() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_dictionary_entry_id")
  public long getDataDictionaryEntryId() {
    return dataDictionaryEntryId;
  }
  public void setDataDictionaryEntryId(long dataDictionaryEntryId) {
    this.dataDictionaryEntryId = dataDictionaryEntryId;
  }

  @Column(name = "relevant_omop_table")
  public String getRelevantOmopTable() {
    return relevantOmopTable;
  }
  public void setRelevantOmopTable(String relevantOmopTable) {
    this.relevantOmopTable = relevantOmopTable;
  }

  @Column(name = "field_name")
  public String getFieldName() {
    return fieldName;
  }
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  @Column(name = "omop_cdm_standard_or_custom_field")
  public String getOmopCdmStandardOrCustomField() {
    return omopCdmStandardOrCustomField;
  }
  public void setOmopCdmStandardOrCustomField(String omopCdmStandardOrCustomField) {
    this.omopCdmStandardOrCustomField = omopCdmStandardOrCustomField;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  @Column(name = "field_type")
  public String getFieldType() {
    return fieldType;
  }
  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  @Column(name = "data_provenance")
  public String getDataProvenance() {
    return dataProvenance;
  }
  public void setDataProvenance(String dataProvenance) {
    this.dataProvenance = dataProvenance;
  }

  @Column(name = "source_ppi_module")
  public String getSourcePpiModule() {
    return sourcePpiModule;
  }
  public void setSourcePpiModule(String sourcePpiModule) {
    this.sourcePpiModule = sourcePpiModule;
  }

  @Column(name = "transformed_by_registered_tier_privacy_methods")
  public Boolean getTransformedByRegisteredTierPrivacyMethods() {
    return transformedByRegisteredTierPrivacyMethods;
  }
  public void setTransformedByRegisteredTierPrivacyMethods(Boolean transformedByRegisteredTierPrivacyMethods) {
    this.transformedByRegisteredTierPrivacyMethods = transformedByRegisteredTierPrivacyMethods;
  }

}
