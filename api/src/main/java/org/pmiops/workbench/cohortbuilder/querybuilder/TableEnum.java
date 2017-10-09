package org.pmiops.workbench.cohortbuilder.querybuilder;

/**
 * This enum maps different domain types to table name and concept id.
 */
public enum TableEnum {
    CONDITION("Condition", "condition_occurrence", "condition_source_concept_id"),
    PROCEDURE("Procedure", "procedure_occurrence", "procedure_source_concept_id"),
    OBSERVATION("Observation", "observation", "observation_source_concept_id"),
    MEASUREMENT("Measurement", "measurement", "measurement_source_concept_id");

    private String domainId;
    private String tableName;
    private String sourceConceptId;

    private TableEnum(String domainId, String tableName, String sourceConceptId) {
        this.domainId = domainId;
        this.tableName = tableName;
        this.sourceConceptId = sourceConceptId;
    }

    public static String getTableName(String domainId) {
        for (TableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.tableName;
            }
        }
        return null;
    }

    public static String getSourceConceptId(String domainId) {
        for (TableEnum item: values()) {
            if (item.domainId.equals(domainId)) {
                return item.sourceConceptId;
            }
        }
        return null;
    }
}
