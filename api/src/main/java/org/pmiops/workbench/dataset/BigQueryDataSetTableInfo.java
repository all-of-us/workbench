package org.pmiops.workbench.dataset;

import org.pmiops.workbench.model.Domain;

public enum BigQueryDataSetTableInfo {
  CONDITION(
      Domain.CONDITION,
      "ds_condition_occurrence",
      " and (condition_concept_id in unnest(@conceptIds) or condition_source_concept_id in unnest(@conceptIds))"),
  PROCEDURE(
      Domain.PROCEDURE,
      "ds_procedure_occurrence",
      " and (procedure_concept_id in unnest(@conceptIds) or procedure_source_concept_id in unnest(@conceptIds))"),
  DRUG(
      Domain.DRUG,
      "ds_drug_exposure",
      " and (drug_concept_id in unnest(@conceptIds) or drug_source_concept_id in unnest(@conceptIds))"),
  MEASUREMENT(
      Domain.MEASUREMENT,
      "ds_measurement",
      " and (measurement_concept_id in unnest(@conceptIds) or measurement_source_concept_id in unnest(@conceptIds))"),
  SURVEY(Domain.SURVEY, "ds_survey", "and question_concept_id in unnest(@conceptIds)"),
  PERSON(Domain.PERSON, "ds_person", null),
  OBSERVATION(
      Domain.OBSERVATION,
      "ds_observation",
      " and (observation_concept_id in unnest(@conceptIds) or observation_source_concept_id in unnest(@conceptIds))");

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
