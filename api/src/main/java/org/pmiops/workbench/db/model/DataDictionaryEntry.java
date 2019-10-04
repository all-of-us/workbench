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
  private boolean transformedByRegisteredTierPrivacyMethods;

  public DataDictionaryEntry() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_dictionary_entry_id")
  public long getDataDictionaryEntryId() {
    return dataDictionaryEntryId;
  }

  @Column(name = "relevant_omop_table")
  public String getRelevantOmopTable() {
    return relevantOmopTable;
  }

  @Column(name = "field_name")
  public String getFieldName() {
    return fieldName;
  }

  @Column(name = "omop_cdm_standard_or_custom_field")
  public String getOmopCdmStandardOrCustomField() {
    return omopCdmStandardOrCustomField;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  @Column(name = "field_type")
  public String getFieldType() {
    return fieldType;
  }

  @Column(name = "data_provenance")
  public String getDataProvenance() {
    return dataProvenance;
  }

  @Column(name = "source_ppi_module")
  public String getSourcePpiModule() {
    return sourcePpiModule;
  }

  @Column(name = "transformed_by_registered_tier_privacy_methods")
  public boolean getTransformedByRegisteredTierPrivacyMethods() {
    return transformedByRegisteredTierPrivacyMethods;
  }
}
