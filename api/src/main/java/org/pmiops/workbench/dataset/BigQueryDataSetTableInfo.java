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
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 1)\n "
          + "or condition_source_concept_id in(select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 0))",
      " (condition_concept_id in unnest(@conceptIds) or condition_source_concept_id in unnest(@conceptIds))"),
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
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 1)\n "
          + "or procedure_source_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@conceptIds)\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 0))",
      " (procedure_concept_id in unnest(@conceptIds) or procedure_source_concept_id in unnest(@conceptIds))"),
  DRUG(
      Domain.DRUG,
      "ds_drug_exposure",
      " drug_concept_id in (select distinct ca.descendant_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
          + "join (select distinct c.concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(cr.id as string) as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "where domain_id = 'DRUG'\n"
          + "and concept_id in unnest(@conceptIds)\n"
          + ") a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'DRUG'\n"
          + ") b on (ca.ancestor_id = b.concept_id))",
      " (drug_concept_id in unnest(@conceptIds) or drug_source_concept_id in unnest(@conceptIds))"),
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
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 1)",
      " (measurement_concept_id in unnest(@conceptIds) or measurement_source_concept_id in unnest(@conceptIds))"),
  SURVEY(
      Domain.SURVEY,
      "ds_survey",
      " question_concept_id in unnest(@conceptIds)",
      " question_concept_id in unnest(@conceptIds)"),
  PERSON(Domain.PERSON, "ds_person", null, null),
  OBSERVATION(
      Domain.OBSERVATION,
      "ds_observation",
      " observation_concept_id in unnest(@conceptIds)",
      " (observation_concept_id in unnest(@conceptIds) or observation_source_concept_id in unnest(@conceptIds))"),
  FITBIT_HEART_RATE_SUMMARY(Domain.FITBIT_HEART_RATE_SUMMARY, "ds_heart_rate_summary", null, null),
  FITBIT_HEART_RATE_LEVEL(Domain.FITBIT_HEART_RATE_LEVEL, "ds_heart_rate_minute_level", null, null),
  FITBIT_ACTIVITY(Domain.FITBIT_ACTIVITY, "ds_activity_summary", null, null),
  FITBIT_INTRADAY_STEPS(Domain.FITBIT_INTRADAY_STEPS, "ds_steps_intraday", null, null);

  private final Domain domain;
  private final String tableName;
  private final String conceptIdIn;
  private final String conceptIdInOld;

  BigQueryDataSetTableInfo(
      Domain domain, String tableName, String conceptIdIn, String conceptIdInOld) {
    this.domain = domain;
    this.tableName = tableName;
    this.conceptIdIn = conceptIdIn;
    this.conceptIdInOld = conceptIdInOld;
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

  public static String getConceptIdInOld(Domain domain) {
    for (BigQueryDataSetTableInfo info : values()) {
      if (info.domain.equals(domain)) {
        return info.conceptIdInOld;
      }
    }
    return null;
  }
}
