package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "data_dictionary_entry")
public class DbDataDictionaryEntry {

  // Metadata fields
  private long dataDictionaryEntryId;
  private DbCdrVersion cdrVersion;
  private Timestamp definedTime;

  // Fields copied from the Data Dictionary export
  // See available_field_item in the following
  // https://github.com/all-of-us/cdrdatadictionary/blob/development/cdr_data_dictionary/schema.yaml
  private String relevantOmopTable;
  private String fieldName;
  private String omopCdmStandardOrCustomField;
  private String description;
  private String fieldType;
  private String dataProvenance;
  private String sourcePpiModule;
  private Boolean transformedByRegisteredTierPrivacyMethods;

  public DbDataDictionaryEntry() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_dictionary_entry_id")
  public long getDataDictionaryEntryId() {
    return dataDictionaryEntryId;
  }

  public void setDataDictionaryEntryId(long dataDictionaryEntryId) {
    this.dataDictionaryEntryId = dataDictionaryEntryId;
  }

  @ManyToOne
  @JoinColumn(name = "cdr_version_id")
  public DbCdrVersion getCdrVersion() {
    return cdrVersion;
  }

  public void setCdrVersion(DbCdrVersion cdrVersion) {
    this.cdrVersion = cdrVersion;
  }

  @Column(name = "defined_time")
  public Timestamp getDefinedTime() {
    return definedTime;
  }

  public void setDefinedTime(Timestamp definedTime) {
    this.definedTime = definedTime;
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

  public void setTransformedByRegisteredTierPrivacyMethods(
      Boolean transformedByRegisteredTierPrivacyMethods) {
    this.transformedByRegisteredTierPrivacyMethods = transformedByRegisteredTierPrivacyMethods;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbDataDictionaryEntry that = (DbDataDictionaryEntry) o;
    return dataDictionaryEntryId == that.dataDictionaryEntryId
        && Objects.equals(cdrVersion, that.cdrVersion)
        && Objects.equals(definedTime, that.definedTime)
        && Objects.equals(relevantOmopTable, that.relevantOmopTable)
        && Objects.equals(fieldName, that.fieldName)
        && Objects.equals(omopCdmStandardOrCustomField, that.omopCdmStandardOrCustomField)
        && Objects.equals(description, that.description)
        && Objects.equals(fieldType, that.fieldType)
        && Objects.equals(dataProvenance, that.dataProvenance)
        && Objects.equals(sourcePpiModule, that.sourcePpiModule)
        && Objects.equals(
            transformedByRegisteredTierPrivacyMethods,
            that.transformedByRegisteredTierPrivacyMethods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        dataDictionaryEntryId,
        cdrVersion,
        definedTime,
        relevantOmopTable,
        fieldName,
        omopCdmStandardOrCustomField,
        description,
        fieldType,
        dataProvenance,
        sourcePpiModule,
        transformedByRegisteredTierPrivacyMethods);
  }
}
