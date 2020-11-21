package org.pmiops.workbench.dataset;

import org.pmiops.workbench.model.Domain;

public enum BigQueryDataSetTableInfo {
  CONDITION(
      Domain.CONDITION,
      "ds_condition_occurrence",
      " condition_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@standardConceptIds)\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 1)\n ",
      " condition_source_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@sourceConceptIds)\n"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))"
          + "and domain_id = 'CONDITION'\n"
          + "and is_standard = 0)",
      " (condition_concept_id in unnest(@conceptIds) or condition_source_concept_id in unnest(@conceptIds))"),
  PROCEDURE(
      Domain.PROCEDURE,
      "ds_procedure_occurrence",
      " procedure_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@standardConceptIds)\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 1)\n ",
      " procedure_source_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@sourceConceptIds)\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'PROCEDURE'\n"
          + "and is_standard = 0)",
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
          + "and concept_id in unnest(@standardConceptIds)\n"
          + ") a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'DRUG'\n"
          + ") b on (ca.ancestor_id = b.concept_id))",
      " drug_source_concept_id in (select distinct ca.descendant_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria_ancestor` ca\n"
          + "join (select distinct c.concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(cr.id as string) as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` cr\n"
          + "where domain_id = 'DRUG'\n"
          + "and concept_id in unnest(@sourceConceptIds)\n"
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
          + "where concept_id in unnest(@standardConceptIds)\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 1) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 1)",
      " measurement_source_concept_id in (select concept_id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria` c\n"
          + "join (select cast(id as string)  as id\n"
          + "from `${projectId}.${dataSetId}.cb_criteria`\n"
          + "where concept_id in unnest(@sourceConceptIds)\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 0) a\n"
          + "on (c.path like concat('%.', a.id, '.%') or c.path like concat('%.', a.id))\n"
          + "and domain_id = 'MEASUREMENT'\n"
          + "and is_standard = 0)",
      " (measurement_concept_id in unnest(@conceptIds) or measurement_source_concept_id in unnest(@conceptIds))"),
  SURVEY(
      Domain.SURVEY,
      "ds_survey",
      null,
      " question_concept_id in unnest(@sourceConceptIds)",
      " question_concept_id in unnest(@conceptIds)"),
  PERSON(Domain.PERSON, "ds_person", null, null, null),
  OBSERVATION(
      Domain.OBSERVATION,
      "ds_observation",
      " observation_concept_id in unnest(@standardConceptIds)",
      " observation_source_concept_id in unnest(@sourceConceptIds)",
      " (observation_concept_id in unnest(@conceptIds) or observation_source_concept_id in unnest(@conceptIds))"),
  FITBIT_HEART_RATE_SUMMARY(
      Domain.FITBIT_HEART_RATE_SUMMARY, "ds_heart_rate_summary", null, null, null),
  FITBIT_HEART_RATE_LEVEL(
      Domain.FITBIT_HEART_RATE_LEVEL, "ds_heart_rate_minute_level", null, null, null),
  FITBIT_ACTIVITY(Domain.FITBIT_ACTIVITY, "ds_activity_summary", null, null, null),
  FITBIT_INTRADAY_STEPS(Domain.FITBIT_INTRADAY_STEPS, "ds_steps_intraday", null, null, null);

  private final Domain domain;
  private final String tableName;
  private final String standardConceptIdIn;
  private final String sourceConceptIdIn;
  private final String conceptIdInOld;

  BigQueryDataSetTableInfo(
      Domain domain,
      String tableName,
      String standardConceptIdIn,
      String sourceConceptIdIn,
      String conceptIdInOld) {
    this.domain = domain;
    this.tableName = tableName;
    this.standardConceptIdIn = standardConceptIdIn;
    this.sourceConceptIdIn = sourceConceptIdIn;
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

  public static String getConceptIdIn(Domain domain, boolean standard) {
    for (BigQueryDataSetTableInfo info : values()) {
      if (info.domain.equals(domain)) {
        return standard ? info.standardConceptIdIn : info.sourceConceptIdIn;
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
