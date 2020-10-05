package org.pmiops.workbench.dataset;

import org.pmiops.workbench.model.Domain;

public enum BigQueryDataSetTableInfo {
  CONDITION(
      Domain.CONDITION,
      "ds_condition_occurrence",
      " (condition_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id))\n "
          + "or condition_source_concept_id in(select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id)))"),
  PROCEDURE(
      Domain.PROCEDURE,
      "ds_procedure_occurrence",
      " (procedure_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id))\n "
          + "or procedure_source_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id)))"),
  DRUG(
      Domain.DRUG,
      "ds_drug_exposure",
      " (drug_concept_id in (select distinct ca.descendant_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
          + "join (select distinct c.concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(cr.id as string) as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "where domain_id = 'DRUG'\n"
          + "and concept_id in unnest(@conceptIds)\n"
          + ") a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id)\n"
          + ") b on (ca.ancestor_id = b.concept_id)))"),
  MEASUREMENT(
      Domain.MEASUREMENT,
      "ds_measurement",
      " measurement_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id) or c.path like concat(a.id, '.%') or c.path = a.id))"),
  SURVEY(Domain.SURVEY, "ds_survey", " question_concept_id in unnest(@conceptIds)"),
  PERSON(Domain.PERSON, "ds_person", null),
  OBSERVATION(
      Domain.OBSERVATION,
      "ds_observation",
      " (observation_concept_id in unnest(@conceptIds) or observation_source_concept_id in unnest(@conceptIds))");

  private final Domain domain;
  private final String tableName;
  private final String conceptIdIn;

  BigQueryDataSetTableInfo(Domain domain, String tableName, String conceptIdIn) {
    this.domain = domain;
    this.tableName = tableName;
    this.conceptIdIn = conceptIdIn;
  }

  public static String getTableName(Domain domain) {
    for (BigQueryDataSetTableInfo info : values()) {
      if (info.domain.equals(domain)) {
        return info.tableName;
      }
    }
    return null;
  }

  public static String getConceptIdIn(Domain domain) {
    for (BigQueryDataSetTableInfo info : values()) {
      if (info.domain.equals(domain)) {
        return info.conceptIdIn;
      }
    }
    return null;
  }
}
