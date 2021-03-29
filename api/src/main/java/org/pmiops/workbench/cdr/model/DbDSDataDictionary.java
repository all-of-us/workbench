package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ds_data_dictionary")
public class DbDSDataDictionary {
  private long id;
  private String fieldName;
  private String relevantOmopTable;
  private String description;
  private String fieldType;
  private String omopCdmStandardOrCustomField;
  private String dataProvenance;
  private String sourcePpiModule;
  private String domain;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "field_name")
  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  @Column(name = "relevant_omop_table")
  public String getRelevantOmopTable() {
    return relevantOmopTable;
  }

  public void setRelevantOmopTable(String relevantOmopTable) {
    this.relevantOmopTable = relevantOmopTable;
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

  @Column(name = "omop_cdm_standard_or_custom_field")
  public String getOmopCdmStandardOrCustomField() {
    return omopCdmStandardOrCustomField;
  }

  public void setOmopCdmStandardOrCustomField(String omopCdmStandardOrCustomField) {
    this.omopCdmStandardOrCustomField = omopCdmStandardOrCustomField;
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

  @Column(name = "domain")
  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbDSDataDictionary that = (DbDSDataDictionary) o;
    return id == that.id
        && fieldName.equals(that.fieldName)
        && relevantOmopTable.equals(that.relevantOmopTable)
        && description.equals(that.description)
        && fieldType.equals(that.fieldType)
        && omopCdmStandardOrCustomField.equals(that.omopCdmStandardOrCustomField)
        && dataProvenance.equals(that.dataProvenance)
        && sourcePpiModule.equals(that.sourcePpiModule)
        && domain.equals(that.domain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        fieldName,
        relevantOmopTable,
        description,
        fieldType,
        omopCdmStandardOrCustomField,
        dataProvenance,
        sourcePpiModule,
        domain);
  }

  public static DbDSDataDictionary.Builder builder() {
    return new DbDSDataDictionary.Builder();
  }

  public static class Builder {

    private long id;
    private String fieldName;
    private String relevantOmopTable;
    private String description;
    private String fieldType;
    private String omopCdmStandardOrCustomField;
    private String dataProvenance;
    private String sourcePpiModule;
    private String domain;

    private Builder() {}

    public DbDSDataDictionary.Builder addDataDictionaryEntryId(long id) {
      this.id = id;
      return this;
    }

    public DbDSDataDictionary.Builder addDomain(String domain) {
      this.domain = domain;
      return this;
    }

    public DbDSDataDictionary.Builder addRelevantOmopTable(String relevantOmopTable) {
      this.relevantOmopTable = relevantOmopTable;
      return this;
    }

    public DbDSDataDictionary.Builder addFieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public DbDSDataDictionary.Builder addOmopCdmStandardOrCustomField(
        String omopCdmStandardOrCustomField) {
      this.omopCdmStandardOrCustomField = omopCdmStandardOrCustomField;
      return this;
    }

    public DbDSDataDictionary.Builder addDescription(String description) {
      this.description = description;
      return this;
    }

    public DbDSDataDictionary.Builder addFieldType(String fieldType) {
      this.fieldType = fieldType;
      return this;
    }

    public DbDSDataDictionary.Builder addDataProvenance(String dataProvenance) {
      this.dataProvenance = dataProvenance;
      return this;
    }

    public DbDSDataDictionary.Builder addSourcePpiModule(String sourcePpiModule) {
      this.sourcePpiModule = sourcePpiModule;
      return this;
    }

    public DbDSDataDictionary build() {
      DbDSDataDictionary dbDataDictionary = new DbDSDataDictionary();
      dbDataDictionary.setId(this.id);
      dbDataDictionary.setRelevantOmopTable(this.relevantOmopTable);
      dbDataDictionary.setFieldName(this.fieldName);
      dbDataDictionary.setOmopCdmStandardOrCustomField(this.omopCdmStandardOrCustomField);
      dbDataDictionary.setDescription(this.description);
      dbDataDictionary.setFieldType(this.fieldType);
      dbDataDictionary.setDataProvenance(this.dataProvenance);
      dbDataDictionary.setSourcePpiModule(this.sourcePpiModule);
      dbDataDictionary.setDomain(this.domain);
      return dbDataDictionary;
    }
  }
}
