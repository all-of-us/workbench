package org.pmiops.workbench.dataset;

import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.CHILD_LOOKUP_SQL;
import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.DRUG_CHILD_LOOKUP_SQL;

import org.pmiops.workbench.model.Domain;

public enum BigQueryDataSetTableInfo {
  CONDITION(
      Domain.CONDITION,
      "ds_condition_occurrence",
      " condition_concept_id IN " + String.format(CHILD_LOOKUP_SQL, "@standardConceptIds", 1),
      " condition_source_concept_id IN " + String.format(CHILD_LOOKUP_SQL, "@sourceConceptIds", 0)),
  PROCEDURE(
      Domain.PROCEDURE,
      "ds_procedure_occurrence",
      " procedure_concept_id IN " + String.format(CHILD_LOOKUP_SQL, "@standardConceptIds", 1),
      " procedure_source_concept_id IN " + String.format(CHILD_LOOKUP_SQL, "@sourceConceptIds", 0)),
  DRUG(
      Domain.DRUG,
      "ds_drug_exposure",
      " drug_concept_id IN " + String.format(DRUG_CHILD_LOOKUP_SQL, "@standardConceptIds", 1),
      " drug_source_concept_id in " + String.format(DRUG_CHILD_LOOKUP_SQL, "@sourceConceptIds", 0)),
  MEASUREMENT(
      Domain.MEASUREMENT,
      "ds_measurement",
      " measurement_concept_id IN " + String.format(CHILD_LOOKUP_SQL, "@standardConceptIds", 1),
      " measurement_source_concept_id IN "
          + String.format(CHILD_LOOKUP_SQL, "@sourceConceptIds", 0)),
  PHYSICAL_MEASUREMENT_CSS(
      Domain.PHYSICAL_MEASUREMENT_CSS,
      "ds_measurement",
      " measurement_concept_id IN unnest(@standardConceptIds)",
      " measurement_source_concept_id IN unnest(@sourceConceptIds)"),
  DEVICE(
      Domain.DEVICE,
      "ds_device",
      " device_concept_id IN unnest(@standardConceptIds)",
      " device_source_concept_id IN unnest(@sourceConceptIds)"),
  SURVEY(Domain.SURVEY, "ds_survey", null, " question_concept_id IN unnest(@sourceConceptIds)"),
  PERSON(Domain.PERSON, "ds_person", null, null),
  OBSERVATION(
      Domain.OBSERVATION,
      "ds_observation",
      " observation_concept_id IN unnest(@standardConceptIds)",
      " observation_source_concept_id IN unnest(@sourceConceptIds)"),
  FITBIT_HEART_RATE_SUMMARY(Domain.FITBIT_HEART_RATE_SUMMARY, "ds_heart_rate_summary", null, null),
  FITBIT_HEART_RATE_LEVEL(Domain.FITBIT_HEART_RATE_LEVEL, "ds_heart_rate_minute_level", null, null),
  FITBIT_ACTIVITY(Domain.FITBIT_ACTIVITY, "ds_activity_summary", null, null),
  FITBIT_INTRADAY_STEPS(Domain.FITBIT_INTRADAY_STEPS, "ds_steps_intraday", null, null),
  FITBIT_SLEEP_DAILY_SUMMARY(
      Domain.FITBIT_SLEEP_DAILY_SUMMARY, "ds_sleep_daily_summary", null, null),
  FITBIT_SLEEP_LEVEL(Domain.FITBIT_SLEEP_LEVEL, "ds_sleep_level", null, null),
  FITBIT_DEVICE(Domain.FITBIT_DEVICE, "ds_fitbit_device", null, null),
  ETM_DELAYDISCOUNTING_METADATA(
      Domain.ETM_DELAYDISCOUNTING_METADATA, "ds_delaydiscounting_metadata", null, null),
  ETM_DELAYDISCOUNTING_OUTCOMES(
      Domain.ETM_DELAYDISCOUNTING_OUTCOMES, "ds_delaydiscounting_outcomes", null, null),
  ETM_DELAYDISCOUNTING_TRIAL_DATA(
      Domain.ETM_DELAYDISCOUNTING_TRIAL_DATA, "ds_delaydiscounting_trial_data", null, null),
  ETM_EMORECOG_METADATA(Domain.ETM_EMORECOG_METADATA, "ds_emorecog_metadata", null, null),
  ETM_EMORECOG_OUTCOMES(Domain.ETM_EMORECOG_OUTCOMES, "ds_emorecog_outcomes", null, null),
  ETM_EMORECOG_TRIAL_DATA(Domain.ETM_EMORECOG_TRIAL_DATA, "ds_emorecog_trial_data", null, null),
  ETM_FLANKER_METADATA(Domain.ETM_FLANKER_METADATA, "ds_flanker_metadata", null, null),
  ETM_FLANKER_OUTCOMES(Domain.ETM_FLANKER_OUTCOMES, "ds_flanker_outcomes", null, null),
  ETM_FLANKER_TRIAL_DATA(Domain.ETM_FLANKER_TRIAL_DATA, "ds_flanker_trial_data", null, null),
  ETM_GRADCPT_METADATA(Domain.ETM_GRADCPT_METADATA, "ds_gradcpt_metadata", null, null),
  ETM_GRADCPT_OUTCOMES(Domain.ETM_GRADCPT_OUTCOMES, "ds_gradcpt_outcomes", null, null),
  ETM_GRADCPT_TRIAL_DATA(Domain.ETM_GRADCPT_TRIAL_DATA, "ds_gradcpt_trial_data", null, null),
  ZIP_CODE_SOCIOECONOMIC(Domain.ZIP_CODE_SOCIOECONOMIC, "ds_zip_code_socioeconomic", null, null),
  VISIT(
      Domain.VISIT,
      "ds_visit_occurrence",
      " visit_concept_id IN unnest(@standardConceptIds)",
      " visit_source_concept_id IN unnest(@sourceConceptIds)");

  private final Domain domain;
  private final String tableName;
  private final String standardConceptIdIn;
  private final String sourceConceptIdIn;

  BigQueryDataSetTableInfo(
      Domain domain, String tableName, String standardConceptIdIn, String sourceConceptIdIn) {
    this.domain = domain;
    this.tableName = tableName;
    this.standardConceptIdIn = standardConceptIdIn;
    this.sourceConceptIdIn = sourceConceptIdIn;
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
}
