package org.pmiops.workbench.cdm;

/**
 * This enum maps different domain types to table name and concept id.
 */
public enum DomainTableEnum {
    CONDITION("Condition",
      "condition_occurrence",
      "condition_occurrence_id",
      "condition_source_value",
      "condition_concept_id",
      "condition_source_concept_id",
      "condition_start_date",
      "condition_start_datetime"),
    PROCEDURE("Procedure",
      "procedure_occurrence",
      "procedure_occurrence_id",
      "procedure_source_value",
      "procedure_concept_id",
      "procedure_source_concept_id",
      "procedure_date",
      "procedure_datetime"),
    OBSERVATION("Observation",
      "observation",
      "observation_id",
      "observation_source_value",
      "observation_concept_id",
      "observation_source_concept_id",
      "observation_date",
      "observation_datetime"),
    MEASUREMENT("Measurement",
      "measurement",
      "measurement_id",
      "measurement_source_value",
      "measurement_concept_id",
      "measurement_source_concept_id",
      "measurement_date",
      "measurement_datetime"),
    DRUG("Drug",
      "drug_exposure",
      "drug_exposure_id",
      "drug_source_value",
      "drug_concept_id",
      "drug_source_concept_id",
      "drug_exposure_start_date",
      "drug_exposure_start_datetime"),
    DEVICE("Device",
      "device_exposure",
      "device_exposure_id",
      "device_source_value",
      "device_concept_id",
      "device_source_concept_id",
      "device_exposure_start_date",
      "device_exposure_start_datetime"),
    VISIT("Visit",
      "visit_occurrence",
      "visit_occurrence_id",
      "visit_source_value",
      "visit_concept_id",
      "visit_source_concept_id",
      "visit_start_date",
      "visit_start_datetime");

    private String domainId;
    private String tableName;
    private String primaryKey;
    private String sourceValue;
    private String conceptId;
    private String sourceConceptId;
    private String entryDate;
    private String entryDateTime;

    private DomainTableEnum(String domainId,
                            String tableName,
                            String primaryKey,
                            String sourceValue,
                            String conceptId,
                            String sourceConceptId,
                            String entryDate,
                            String entryDateTime) {
        this.domainId = domainId;
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        this.sourceValue = sourceValue;
        this.conceptId = conceptId;
        this.sourceConceptId = sourceConceptId;
        this.entryDate = entryDate;
        this.entryDateTime = entryDateTime;
    }

    public String getDomainId() {
        return this.domainId;
    }

    public static String getTableName(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.tableName;
            }
        }
        return null;
    }

    public static String getPrimaryKey(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.primaryKey;
            }
        }
        return null;
    }

    public static String getSourceValue(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.sourceValue;
            }
        }
        return null;
    }

    public static String getConceptId(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.conceptId;
            }
        }
        return null;
    }

    public static String getSourceConceptId(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.sourceConceptId;
            }
        }
        return null;
    }

    public static String getEntryDate(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.entryDate;
            }
        }
        return null;
    }

    public static String getEntryDateTime(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.entryDateTime;
            }
        }
        return null;
    }
}
