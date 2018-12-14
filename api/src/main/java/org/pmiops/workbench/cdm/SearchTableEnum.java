package org.pmiops.workbench.cdm;

public enum SearchTableEnum {
  SEARCH_ICD9("ICD9",
    "search_icd9",
    "source_concept_id"),
  SEARCH_ICD10("ICD10",
    "search_icd10",
    "source_concept_id"),
  SEARCH_CPT("CPT",
    "search_cpt",
    "source_concept_id"),
  SEARCH_DRUG("DRUG",
    "search_drug",
    "concept_id"),
  SEARCH_MEASUREMENT("MEAS",
    "search_measurement",
    "concept_id"),
  SEARCH_PM("PM",
    "search_pm",
    "source_concept_id"),
  SEARCH_PPI("PPI",
    "search_ppi",
    "source_concept_id"),
  SEARCH_SNOMED("SNOMED",
    "search_snomed",
    "concept_id");

  private String type;
  private String tableName;
  private String conceptIdOrSourceConceptId;

  private SearchTableEnum(String type,
                          String tableName,
                          String conceptIdOrSourceConceptId) {
    this.type = type;
    this.tableName = tableName;
    this.conceptIdOrSourceConceptId = conceptIdOrSourceConceptId;
  }

  public static String getTableName(String type) {
    for (SearchTableEnum item: values()) {
      if (item.type.equalsIgnoreCase(type)) {
        return item.tableName;
      }
    }
    return null;
  }

  public static String getConceptIdOrSourceConceptId(String type) {
    for (SearchTableEnum item: values()) {
      if (item.type.equalsIgnoreCase(type)) {
        return item.conceptIdOrSourceConceptId;
      }
    }
    return null;
  }
}
