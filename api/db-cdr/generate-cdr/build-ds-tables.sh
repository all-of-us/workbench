#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset
export DOMAIN=$3       # specific domain table to build


function do_ds_condition_occurrence(){
  echo "ds_condition_occurrence - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_condition_occurrence\`
      (PERSON_ID, CONDITION_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
      CONDITION_START_DATETIME, CONDITION_END_DATETIME, CONDITION_TYPE_CONCEPT_ID, CONDITION_TYPE_CONCEPT_NAME,
      STOP_REASON, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, CONDITION_SOURCE_VALUE, CONDITION_SOURCE_CONCEPT_ID,
      SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, CONDITION_STATUS_SOURCE_VALUE,
      CONDITION_STATUS_CONCEPT_ID, CONDITION_STATUS_CONCEPT_NAME)
  select a.PERSON_ID, CONDITION_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, CONDITION_START_DATETIME, CONDITION_END_DATETIME, CONDITION_TYPE_CONCEPT_ID,
      c2.concept_name as CONDITION_TYPE_CONCEPT_NAME, STOP_REASON, a.VISIT_OCCURRENCE_ID, c3.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME,
      CONDITION_SOURCE_VALUE, CONDITION_SOURCE_CONCEPT_ID, c4.concept_name as SOURCE_CONCEPT_NAME, c4.concept_code as SOURCE_CONCEPT_CODE,
      c4.vocabulary_id as SOURCE_VOCABULARY, CONDITION_STATUS_SOURCE_VALUE, CONDITION_STATUS_CONCEPT_ID,
      c5.concept_name as CONDITION_STATUS_CONCEPT_NAME
  from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONDITION_TYPE_CONCEPT_ID = c2.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.CONDITION_SOURCE_CONCEPT_ID = c4.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.CONDITION_STATUS_CONCEPT_ID = c5.CONCEPT_ID"
}

function do_ds_device(){
  echo "ds_device - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_device\`
       (PERSON_ID, DEVICE_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
       DEVICE_EXPOSURE_START_DATETIME, DEVICE_EXPOSURE_END_DATETIME, DEVICE_TYPE_CONCEPT_ID, DEVICE_TYPE_CONCEPT_NAME,
       VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, DEVICE_SOURCE_VALUE, DEVICE_SOURCE_CONCEPT_ID,
       SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY)
   select a.PERSON_ID, DEVICE_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
       c1.vocabulary_id as STANDARD_VOCABULARY, DEVICE_EXPOSURE_START_DATETIME, DEVICE_EXPOSURE_END_DATETIME, DEVICE_TYPE_CONCEPT_ID,
       c2.concept_name as DEVICE_TYPE_CONCEPT_NAME, a.VISIT_OCCURRENCE_ID, c3.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME,
       DEVICE_SOURCE_VALUE, DEVICE_SOURCE_CONCEPT_ID, c4.concept_name as SOURCE_CONCEPT_NAME, c4.concept_code as SOURCE_CONCEPT_CODE,
       c4.vocabulary_id as SOURCE_VOCABULARY
   from \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` a
   left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.DEVICE_CONCEPT_ID = c1.CONCEPT_ID
   left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.DEVICE_TYPE_CONCEPT_ID = c2.CONCEPT_ID
   left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
   left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
   left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.DEVICE_SOURCE_CONCEPT_ID = c4.CONCEPT_ID"
}

function do_ds_drug_exposure(){
  echo "ds_drug_exposure - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_drug_exposure\`
      (PERSON_ID, DRUG_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY, DRUG_EXPOSURE_START_DATETIME,
      DRUG_EXPOSURE_END_DATETIME, VERBATIM_END_DATE, DRUG_TYPE_CONCEPT_ID, DRUG_TYPE_CONCEPT_NAME, STOP_REASON, REFILLS, QUANTITY,
      DAYS_SUPPLY, SIG, ROUTE_CONCEPT_ID, ROUTE_CONCEPT_NAME, LOT_NUMBER, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, DRUG_SOURCE_VALUE,
      DRUG_SOURCE_CONCEPT_ID, SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, ROUTE_SOURCE_VALUE, DOSE_UNIT_SOURCE_VALUE)
  SELECT
      a.PERSON_ID, DRUG_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, DRUG_EXPOSURE_START_DATETIME, DRUG_EXPOSURE_END_DATETIME, VERBATIM_END_DATE,
      DRUG_TYPE_CONCEPT_ID, c2.concept_name as DRUG_TYPE_CONCEPT_NAME, STOP_REASON, REFILLS, QUANTITY, DAYS_SUPPLY, SIG,
      ROUTE_CONCEPT_ID, c3.concept_name as ROUTE_CONCEPT_NAME, LOT_NUMBER, a.VISIT_OCCURRENCE_ID,
      c4.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME, DRUG_SOURCE_VALUE, DRUG_SOURCE_CONCEPT_ID,
      c5.concept_name as SOURCE_CONCEPT_NAME, c5.concept_code as SOURCE_CONCEPT_CODE, c5.vocabulary_id as SOURCE_VOCABULARY,
      ROUTE_SOURCE_VALUE, DOSE_UNIT_SOURCE_VALUE
  FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.DRUG_CONCEPT_ID = c1.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.drug_type_concept_id = c2.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.ROUTE_CONCEPT_ID = c3.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.VISIT_CONCEPT_ID = c4.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.DRUG_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"
}

function do_ds_measurement(){
  echo "ds_measurement - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_measurement\`
      (PERSON_ID, MEASUREMENT_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
      MEASUREMENT_DATETIME, MEASUREMENT_TYPE_CONCEPT_ID, MEASUREMENT_TYPE_CONCEPT_NAME, OPERATOR_CONCEPT_ID,
      OPERATOR_CONCEPT_NAME, VALUE_AS_NUMBER, VALUE_AS_CONCEPT_ID, VALUE_AS_CONCEPT_NAME, UNIT_CONCEPT_ID, UNIT_CONCEPT_NAME,
      RANGE_LOW, RANGE_HIGH, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, MEASUREMENT_SOURCE_VALUE,
      MEASUREMENT_SOURCE_CONCEPT_ID, SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, UNIT_SOURCE_VALUE,
      VALUE_SOURCE_VALUE)
  select
      a.PERSON_ID, MEASUREMENT_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, MEASUREMENT_DATETIME, MEASUREMENT_TYPE_CONCEPT_ID,
      c2.concept_name as MEASUREMENT_TYPE_CONCEPT_NAME, OPERATOR_CONCEPT_ID, c3.concept_name as OPERATOR_CONCEPT_NAME,
      VALUE_AS_NUMBER, VALUE_AS_CONCEPT_ID, c4.concept_name as VALUE_AS_CONCEPT_NAME, UNIT_CONCEPT_ID,
      c5.concept_name as UNIT_CONCEPT_NAME, RANGE_LOW, RANGE_HIGH, a.VISIT_OCCURRENCE_ID,
      c6.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME, MEASUREMENT_SOURCE_VALUE, MEASUREMENT_SOURCE_CONCEPT_ID,
      c7.concept_name as SOURCE_CONCEPT_NAME, c7.concept_code as SOURCE_CONCEPT_CODE, c7.vocabulary_id as SOURCE_VOCABULARY,
      UNIT_SOURCE_VALUE, VALUE_SOURCE_VALUE
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.measurement_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.measurement_type_concept_id = c2.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.operator_concept_id = c3.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.value_as_concept_id = c4.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.unit_concept_id = c5.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.visit_occurrence_id = v.visit_occurrence_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c6 on v.visit_concept_id = c6.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c7 on a.measurement_source_concept_id = c7.concept_id"
}

function do_ds_observation(){
  echo "ds_observation - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_observation\`
      (PERSON_ID, OBSERVATION_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
      OBSERVATION_DATETIME, OBSERVATION_TYPE_CONCEPT_ID, OBSERVATION_TYPE_CONCEPT_NAME, VALUE_AS_NUMBER, VALUE_AS_STRING,
      VALUE_AS_CONCEPT_ID, VALUE_AS_CONCEPT_NAME, QUALIFIER_CONCEPT_ID, QUALIFIER_CONCEPT_NAME, UNIT_CONCEPT_ID,
      UNIT_CONCEPT_NAME, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, OBSERVATION_SOURCE_VALUE,
      OBSERVATION_SOURCE_CONCEPT_ID, SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, UNIT_SOURCE_VALUE,
      QUALIFIER_SOURCE_VALUE, value_source_concept_id, value_source_value, questionnaire_response_id)
  SELECT
      a.PERSON_ID, OBSERVATION_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, OBSERVATION_DATETIME, OBSERVATION_TYPE_CONCEPT_ID,
      c2.concept_name as OBSERVATION_TYPE_CONCEPT_NAME, VALUE_AS_NUMBER, VALUE_AS_STRING, VALUE_AS_CONCEPT_ID,
      c3.concept_name as VALUE_AS_CONCEPT_NAME, QUALIFIER_CONCEPT_ID, c4.concept_name as QUALIFIER_CONCEPT_NAME,
      UNIT_CONCEPT_ID, c5.concept_name as UNIT_CONCEPT_NAME, a.VISIT_OCCURRENCE_ID, c6.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME,
      OBSERVATION_SOURCE_VALUE, OBSERVATION_SOURCE_CONCEPT_ID, c7.concept_name as SOURCE_CONCEPT_NAME,
      c7.concept_code as SOURCE_CONCEPT_CODE, c7.vocabulary_id as SOURCE_VOCABULARY, UNIT_SOURCE_VALUE,
      QUALIFIER_SOURCE_VALUE, value_source_concept_id, value_source_value, questionnaire_response_id
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.OBSERVATION_CONCEPT_ID = c1.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.OBSERVATION_TYPE_CONCEPT_ID = c2.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.value_as_concept_id = c3.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.qualifier_concept_id = c4.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.unit_concept_id = c5.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c6 on v.visit_concept_id = c6.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c7 on a.OBSERVATION_SOURCE_CONCEPT_ID = c7.CONCEPT_ID"
}

function do_ds_person(){
  echo "ds_person - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_person\`
      (PERSON_ID, GENDER_CONCEPT_ID, GENDER, DATE_OF_BIRTH, RACE_CONCEPT_ID, RACE, ETHNICITY_CONCEPT_ID, ETHNICITY, SEX_AT_BIRTH_CONCEPT_ID, SEX_AT_BIRTH)
  SELECT
      PERSON_ID, GENDER_CONCEPT_ID, c1.concept_name as GENDER, BIRTH_DATETIME as DATE_OF_BIRTH, RACE_CONCEPT_ID,
      c2.concept_name as RACE, ETHNICITY_CONCEPT_ID, c3.concept_name as ETHNICITY, SEX_AT_BIRTH_CONCEPT_ID, c4.concept_name as SEX_AT_BIRTH
  FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.gender_concept_id = c1.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.race_concept_id = c2.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.ethnicity_concept_id = c3.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.sex_at_birth_concept_id = c4.CONCEPT_ID"
}

function do_ds_procedure_occurrence(){
  echo "ds_procedure_occurrence (OMOP 5.3.1) - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_procedure_occurrence\`
      (PERSON_ID, PROCEDURE_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
      PROCEDURE_DATETIME, PROCEDURE_TYPE_CONCEPT_ID, PROCEDURE_TYPE_CONCEPT_NAME, MODIFIER_CONCEPT_ID, MODIFIER_CONCEPT_NAME,
      QUANTITY, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, PROCEDURE_SOURCE_VALUE, PROCEDURE_SOURCE_CONCEPT_ID,
      SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, MODIFIER_SOURCE_VALUE)
  select
      a.PERSON_ID, PROCEDURE_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, PROCEDURE_DATETIME, PROCEDURE_TYPE_CONCEPT_ID,
      c2.concept_name as PROCEDURE_TYPE_CONCEPT_NAME, MODIFIER_CONCEPT_ID, c3.concept_name as MODIFIER_CONCEPT_NAME,
      QUANTITY, a.VISIT_OCCURRENCE_ID, c4.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME, PROCEDURE_SOURCE_VALUE,
      PROCEDURE_SOURCE_CONCEPT_ID, c5.concept_name as SOURCE_CONCEPT_NAME, c5.concept_code as SOURCE_CONCEPT_CODE,
      c5.vocabulary_id as SOURCE_VOCABULARY, MODIFIER_SOURCE_VALUE
  from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.PROCEDURE_TYPE_CONCEPT_ID = c2.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.MODIFIER_CONCEPT_ID = c3.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.visit_concept_id = c4.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.PROCEDURE_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"
}

function do_ds_visit_occurrence(){
  echo "ds_visit_occurrence - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_visit_occurrence\`
      (PERSON_ID, VISIT_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY, VISIT_START_DATETIME,
      VISIT_END_DATETIME, VISIT_TYPE_CONCEPT_ID, VISIT_TYPE_CONCEPT_NAME, VISIT_SOURCE_VALUE, VISIT_SOURCE_CONCEPT_ID,
      SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, ADMITTING_SOURCE_CONCEPT_ID, ADMITTING_SOURCE_CONCEPT_NAME,
      ADMITTING_SOURCE_VALUE, DISCHARGE_TO_CONCEPT_ID, DISCHARGE_TO_CONCEPT_NAME, DISCHARGE_TO_SOURCE_VALUE)
  select
      a.PERSON_ID, VISIT_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
      c1.vocabulary_id as STANDARD_VOCABULARY, VISIT_START_DATETIME, VISIT_END_DATETIME, VISIT_TYPE_CONCEPT_ID,
      c2.concept_name as VISIT_TYPE_CONCEPT_NAME, VISIT_SOURCE_VALUE, VISIT_SOURCE_CONCEPT_ID, c3.concept_name as SOURCE_CONCEPT_NAME,
      c3.concept_code as SOURCE_CONCEPT_CODE, c3.vocabulary_id as SOURCE_VOCABULARY, ADMITTING_SOURCE_CONCEPT_ID,
      c4.concept_name as ADMITTING_SOURCE_CONCEPT_NAME, ADMITTING_SOURCE_VALUE, DISCHARGE_TO_CONCEPT_ID,
      c5.concept_name as DISCHARGE_TO_CONCEPT_NAME, DISCHARGE_TO_SOURCE_VALUE
  from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.VISIT_CONCEPT_ID = c1.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.VISIT_TYPE_CONCEPT_ID = c2.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.VISIT_SOURCE_CONCEPT_ID = c3.CONCEPT_ID
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.ADMITTING_SOURCE_CONCEPT_ID = c4.CONCEPT_ID
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.DISCHARGE_TO_CONCEPT_ID = c5.CONCEPT_ID"
}

function do_ds_zip_code_socioeconomic(){
  if [[ "$(bq ls -n 1000 "$BQ_PROJECT":"$BQ_DATASET")" == *"zip3_ses_map"* ]]; then
    echo "ds_zip_code_socioeconomic - inserting data - Controlled Tier"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_zip_code_socioeconomic\`
    (PERSON_ID, OBSERVATION_DATETIME, ZIP3_AS_STRING, FRACTION_ASSISTED_INCOME, FRACTION_HIGH_SCHOOL_EDU, MEDIAN_INCOME, FRACTION_NO_HEALTH_INS, FRACTION_POVERTY, FRACTION_VACANT_HOUSING, DEPRIVATION_INDEX, ACS)
    select o.person_id,
    o.observation_datetime,
    zip.zip3_as_string,
    zip.fraction_assisted_income,
    zip.fraction_high_school_edu,
    zip.median_income,
    zip.fraction_no_health_ins,
    zip.fraction_poverty,
    zip.fraction_vacant_housing,
    zip.deprivation_index,
    zip.acs
    from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    join \`$BQ_PROJECT.$BQ_DATASET.zip3_ses_map\` zip on CAST(SUBSTR(o.value_as_string, 0, STRPOS(o.value_as_string, '*') - 1) as INT64) = zip.zip3
    where observation_source_concept_id = 1585250
    and o.value_as_string not like 'Res%'"
  else
    echo "ds_zip_code_socioeconomic - not used - Registered Tier"
  fi
}

function do_fitbit() {
  echo "ds_activity_summary - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_activity_summary\`
      (date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
      lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id)
  SELECT date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
      lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`)
  where rank = 1"

  echo "ds_heart_rate_minute_level - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_minute_level\`
      (datetime, person_id, heart_rate_value)
  SELECT datetime, person_id, heart_rate_value
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`)
  where rank = 1"

  echo "ds_heart_rate_summary - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_summary\`
      (person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count)
  SELECT person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`)
  where rank = 1"

  echo "ds_steps_intraday - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_steps_intraday\`
      (datetime, steps, person_id)
  SELECT datetime, steps, person_id
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`)
  where rank = 1"

  echo "ds_sleep_daily_summary - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_sleep_daily_summary\`
      (person_id, sleep_date, is_main_sleep, minute_in_bed, minute_asleep, minute_after_wakeup, minute_awake, minute_restless, minute_deep, minute_light, minute_rem, minute_wake)
  SELECT person_id, sleep_date, is_main_sleep, minute_in_bed, minute_asleep, minute_after_wakeup, minute_awake, minute_restless, minute_deep, minute_light, minute_rem, minute_wake
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`)
  where rank = 1"

  echo "ds_sleep_level - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_sleep_level\`
      (person_id, sleep_date, is_main_sleep, level, start_datetime, duration_in_min)
  SELECT person_id, sleep_date, is_main_sleep, level, start_datetime, duration_in_min
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`)
  where rank = 1"
}

function do_COPE_and_PFHH(){
  echo "ds_survey - inserting data for all surveys except COPE and PFHH"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer)
  SELECT DISTINCT a.person_id,
          a.entry_datetime as survey_datetime,
          c.name as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
  JOIN
      (
          SELECT DISTINCT *
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
          WHERE ancestor_concept_id in
              (
                  SELECT concept_id
                  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                  WHERE domain_id = 'SURVEY'
                  AND parent_id = 0
                  AND concept_id NOT IN (1741006, 1333342, 1740639)
              )
      ) b on a.concept_id = b.descendant_concept_id
  JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id"
}

function do_PFHH(){
  echo "ds_survey - inserting data for PFHH survey"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer)
  SELECT DISTINCT a.person_id,
          a.entry_datetime as survey_datetime,
          'Personal and Family Health History' as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
  JOIN
      (
          SELECT DISTINCT CAST(value AS INT64) as answer_concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
          JOIN (
                SELECT CAST(id AS STRING) AS id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE concept_id IN (1740639)
                AND domain_id = 'SURVEY'
              ) a ON (c.path LIKE CONCAT('%', a.id, '.%'))
          WHERE domain_id = 'SURVEY'
          AND type = 'PPI'
          AND subtype = 'ANSWER'
      ) b on a.value_source_concept_id = b.answer_concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
  WHERE a.is_standard = 0"
}

function do_COPE_vaccine(){
  echo "ds_survey - inserting data for cope vaccine survey"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer, survey_version_concept_id, survey_version_name)
  SELECT DISTINCT a.person_id,
          a.observation_datetime as survey_datetime,
          'COVID-19 Vaccine Survey' as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer,
          g.survey_version_concept_id as survey_version_concept_id,
          g.display_name as survey_version_name
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  JOIN
      (
          SELECT *
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
          WHERE ancestor_concept_id in
              (
                  SELECT concept_id
                  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                  WHERE domain_id = 'SURVEY'
                  AND parent_id = 0
                  AND concept_id IN (1741006)
              )
      ) b on a.observation_source_concept_id = b.descendant_concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.survey_conduct\` f on a.questionnaire_response_id = f.survey_conduct_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\` g on f.survey_concept_id = g.survey_version_concept_id"
}

function do_COPE(){
echo "ds_survey - inserting data for COVID-19 Participant Experience (COPE) Survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
   (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer, survey_version_concept_id, survey_version_name)
SELECT DISTINCT a.person_id,
        a.observation_datetime as survey_datetime,
        'COVID-19 Participant Experience (COPE) Survey' as survey,
        d.concept_id as question_concept_id,
        d.concept_name as question,
        e.concept_id as answer_concept_id,
        case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer,
        g.survey_version_concept_id as survey_version_concept_id,
        g.display_name as survey_version_name
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
JOIN
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
        WHERE ancestor_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'SURVEY'
                AND parent_id = 0
                AND concept_id IN (1333342)
            )
        AND descendant_concept_id NOT IN (1310132, 1310137)
    ) b on a.observation_source_concept_id = b.descendant_concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.survey_conduct\` f on a.questionnaire_response_id = f.survey_conduct_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\` g on f.survey_concept_id = g.survey_version_concept_id"
}

if [[ "$DOMAIN" = "observation" ]]; then
  do_ds_observation
elif [[ "$DOMAIN" = "condition" ]]; then
  do_ds_condition_occurrence
elif [[ "$DOMAIN" = "drug" ]]; then
  do_ds_drug_exposure
elif [[ "$DOMAIN" = "visit" ]]; then
  do_ds_visit_occurrence
elif [[ "$DOMAIN" = "measurement" ]]; then
  do_ds_measurement
elif [[ "$DOMAIN" = "procedure" ]]; then
  do_ds_procedure_occurrence
elif [[ "$DOMAIN" = "device" ]]; then
  do_ds_device
elif [[ "$DOMAIN" = "person" ]]; then
  do_ds_person
elif [[ "$DOMAIN" = "zip_code_socioeconomic" ]]; then
  do_ds_zip_code_socioeconomic
elif [[ "$DOMAIN" = "all_except_cope_and_pfhh" ]]; then
  do_COPE_and_PFHH
elif [[ "$DOMAIN" = "pfhh" ]]; then
  do_PFHH
elif [[ "$DOMAIN" = "cope" ]]; then
  do_COPE
elif [[ "$DOMAIN" = "cope_minute" ]]; then
  do_COPE_vaccine
elif [[ "$DOMAIN" = "fitbit" ]]; then
  do_fitbit
else
  echo "Failed to build ds_ tables. Unknown table $DOMAIN"
  exit 1
fi
