package org.pmiops.workbench.cohortbuilder.querybuilder;

/**
 * This enum maps different domain types to table name and concept id.
 */
public enum DomainTableEnum {
    CONDITION("Condition", "condition_occurrence", "condition_source_concept_id", "condition_start_date"),
    PROCEDURE("Procedure", "procedure_occurrence", "procedure_source_concept_id", "procedure_date"),
    OBSERVATION("Observation", "observation", "observation_source_concept_id", "observation_date"),
    MEASUREMENT("Measurement", "measurement", "measurement_source_concept_id", "measurement_date"),
    DRUG("Drug", "drug_exposure", "drug_source_concept_id", "drug_exposure_start_date");

    private String domainId;
    private String tableName;
    private String sourceConceptId;
    private String entryDate;

    private DomainTableEnum(String domainId, String tableName, String sourceConceptId, String entryDate) {
        this.domainId = domainId;
        this.tableName = tableName;
        this.sourceConceptId = sourceConceptId;
        this.entryDate = entryDate;
    }

    public static String getTableName(String domainId) {
        for (DomainTableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.tableName;
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
}
