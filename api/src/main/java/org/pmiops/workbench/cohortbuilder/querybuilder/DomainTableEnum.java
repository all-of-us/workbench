package org.pmiops.workbench.cohortbuilder.querybuilder;

/**
 * This enum maps different domain types to table name and concept id.
 */
public enum DomainTableEnum {
    CONDITION("Condition", "condition_occurrence", "condition_source_concept_id"),
    PROCEDURE("Procedure", "procedure_occurrence", "procedure_source_concept_id"),
    OBSERVATION("Observation", "observation", "observation_source_concept_id"),
    MEASUREMENT("Measurement", "measurement", "measurement_source_concept_id"),
    DRUG("Drug", "drug_exposure", "drug_source_concept_id");

    private String domainId;
    private String tableName;
    private String sourceConceptId;

    private DomainTableEnum(String domainId, String tableName, String sourceConceptId) {
        this.domainId = domainId;
        this.tableName = tableName;
        this.sourceConceptId = sourceConceptId;
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
}
