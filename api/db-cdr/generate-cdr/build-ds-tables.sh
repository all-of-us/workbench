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
  # run this query to initializing our .bigqueryrc configuration file
  # otherwise this will corrupt the output of the first call to find_info()
  echo "Running a simple select to avoid problem with initializing our .bigqueryrc configuration file"
  query="select count(*) from \`$BQ_PROJECT.$BQ_DATASET.concept\`"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query"
  
  echo "Getting self_reported_category_concept_id column count"
  query="select count(column_name) as count from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\`
  where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\""
  selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')
  
  if [[ $selfReportedCategoryDataCount > 0 ]];
  then
    echo "ds_person - inserting data"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_person\`
        (PERSON_ID, GENDER_CONCEPT_ID, GENDER, DATE_OF_BIRTH, RACE_CONCEPT_ID, RACE, ETHNICITY_CONCEPT_ID, ETHNICITY, SEX_AT_BIRTH_CONCEPT_ID, SEX_AT_BIRTH, SELF_REPORTED_CATEGORY_CONCEPT_ID, SELF_REPORTED_CATEGORY)
    SELECT
        PERSON_ID, GENDER_CONCEPT_ID, c1.concept_name as GENDER, BIRTH_DATETIME as DATE_OF_BIRTH, RACE_CONCEPT_ID,
        c2.concept_name as RACE, ETHNICITY_CONCEPT_ID, c3.concept_name as ETHNICITY, SEX_AT_BIRTH_CONCEPT_ID, c4.concept_name as SEX_AT_BIRTH,
        SELF_REPORTED_CATEGORY_CONCEPT_ID, c5.concept_name as SELF_REPORTED_CATEGORY
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.gender_concept_id = c1.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.race_concept_id = c2.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.ethnicity_concept_id = c3.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.sex_at_birth_concept_id = c4.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.self_reported_category_concept_id = c5.CONCEPT_ID"
  else
    echo "ds_person - inserting data"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_person\` AS
    SELECT
        PERSON_ID, GENDER_CONCEPT_ID, c1.concept_name as GENDER, BIRTH_DATETIME as DATE_OF_BIRTH, RACE_CONCEPT_ID,
        c2.concept_name as RACE, ETHNICITY_CONCEPT_ID, c3.concept_name as ETHNICITY, SEX_AT_BIRTH_CONCEPT_ID, c4.concept_name as SEX_AT_BIRTH
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.gender_concept_id = c1.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.race_concept_id = c2.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.ethnicity_concept_id = c3.CONCEPT_ID
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.sex_at_birth_concept_id = c4.CONCEPT_ID" 
  fi
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
  
  echo "ds_fitbit_device - inserting data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_fitbit_device\`
      (person_id, device_id, device_date, battery, battery_level, device_version, device_type, last_sync_time, src_id)
  SELECT person_id, device_id, device_date, battery, battery_level, device_version, device_type, last_sync_time, src_id
  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
  FROM \`$BQ_PROJECT.$BQ_DATASET.device\`)
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
  
  echo "ds_emorecog_metadata - inserting data for emorecog metadata"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_emorecog_metadata\`
     (sitting_id, person_id, src_id, response_device, screen_height, screen_width, touch, operating_system, test_duration, test_restarted, test_version, test_name, test_short_name, test_language, aou_version, test_params, user_utc_offset, test_start_date_time, test_end_date_time, user_agent)
  SELECT sitting_id, person_id, src_id, metadata.response_device, metadata.screen_height, metadata.screen_width, metadata.touch, metadata.operating_system, metadata.test_duration, metadata.test_restarted, metadata.test_version, metadata.test_name, metadata.test_short_name, metadata.test_language, metadata.aou_version, metadata.test_params, metadata.user_utc_offset, metadata.test_start_date_time, metadata.test_end_date_time, metadata.user_agent
  FROM \`$BQ_PROJECT.$BQ_DATASET.emorecog\`"
  
  echo "ds_emorecog_outcomes - inserting data for emorecog outcomes"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_emorecog_outcomes\`
    (sitting_id, person_id, src_id, score, accuracy, mean_rtc, median_rtc, sd_rtc, happy_accuracy, happy_mean_rtc, happy_median_rtc, happy_sd_rtc, angry_accuracy, angry_mean_rtc, angry_median_rtc, angry_sd_rtc, sad_accuracy, sad_mean_rtc, sad_median_rtc, sad_sd_rtc, fearful_accuracy, fearful_mean_rtc, fearful_median_rtc, fearful_sd_rtc, flag_median_rtc, flag_same_response, flag_trial_flags, any_timeouts)
  SELECT sitting_id, person_id, src_id, outcomes.score, outcomes.accuracy, outcomes.mean_rtc, outcomes.median_rtc, outcomes.sd_rtc, outcomes.happy_accuracy, outcomes.happy_mean_rtc, outcomes.happy_median_rtc, outcomes.happy_sd_rtc, outcomes.angry_accuracy, outcomes.angry_mean_rtc, outcomes.angry_median_rtc, outcomes.angry_sd_rtc, outcomes.sad_accuracy, outcomes.sad_mean_rtc, outcomes.sad_median_rtc, outcomes.sad_sd_rtc, outcomes.fearful_accuracy, outcomes.fearful_mean_rtc, outcomes.fearful_median_rtc, outcomes.fearful_sd_rtc, outcomes.flag_median_rtc, outcomes.flag_same_response, outcomes.flag_trial_flags, outcomes.any_timeouts
  FROM \`$BQ_PROJECT.$BQ_DATASET.emorecog\`"
    
  echo "ds_emorecog_trial_data - inserting data for emorecog trial data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_emorecog_trial_data\`
    (sitting_id, person_id, src_id, link_id, trial_id, emotion, response, correct, reaction_time, state, repeated, flagged, image, trial_timestamp)
  SELECT sitting_id, person_id, src_id, link_id, trial_id, emotion, response, correct, reaction_time, state, repeated, flagged, image, trial_timestamp
  FROM \`$BQ_PROJECT.$BQ_DATASET.emorecog\` CROSS JOIN UNNEST(trial_data)" 
  
  echo "ds_flanker_metadata - inserting data for flanker metadata"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_flanker_metadata\`
     (sitting_id, person_id, src_id, response_device, screen_height, screen_width, touch, operating_system, test_duration, test_restarted, test_version, test_name, test_short_name, test_language, aou_version, test_params, test_start_date_time, test_end_date_time, user_agent, user_utc_offset)
  SELECT sitting_id, person_id, src_id, metadata.response_device, metadata.screen_height, metadata.screen_width, metadata.touch, metadata.operating_system, metadata.test_duration, metadata.test_restarted, metadata.test_version, metadata.test_name, metadata.test_short_name, metadata.test_language, metadata.aou_version, metadata.test_params, metadata.test_start_date_time, metadata.test_end_date_time, metadata.user_agent, metadata.user_utc_offset
  FROM \`$BQ_PROJECT.$BQ_DATASET.flanker\`"
  
  echo "ds_flanker_outcomes - inserting data for flanker outcomes"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_flanker_outcomes\`
     (sitting_id, person_id, src_id, score, accuracy, mean_rtc, median_rtc, sd_rtc, median_rt_congruent, accuracy_congruent, rcs_congruent, median_rt_incongruent, accuracy_incongruent, rcs_incongruent, median_rt_interference, accuracy_interference, rcs_interference, flag_median_rtc, flag_accuracy, flag_trial_flags, any_timeouts)
  SELECT sitting_id, person_id, src_id, outcomes.score, outcomes.accuracy, outcomes.mean_rtc, outcomes.median_rtc, outcomes.sd_rtc, outcomes.median_rt_congruent, outcomes.accuracy_congruent, outcomes.rcs_congruent, outcomes.median_rt_incongruent, outcomes.accuracy_incongruent, outcomes.rcs_incongruent, outcomes.median_rt_interference, outcomes.accuracy_interference, outcomes.rcs_interference, outcomes.flag_median_rtc, outcomes.flag_accuracy, outcomes.flag_trial_flags, outcomes.any_timeouts
  FROM \`$BQ_PROJECT.$BQ_DATASET.flanker\`"
  
  echo "ds_flanker_trial_data - inserting data for flanker trial_data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_flanker_trial_data\`
     (sitting_id, person_id, src_id, link_id, trial_id, trial_type, trial_block, target, flankers, congruent, response, correct, reaction_time, state, repeated, flagged, trial_timestamp)
  SELECT sitting_id, person_id, src_id, link_id, trial_id, trial_type, trial_block, target, flankers, congruent, response, correct, reaction_time, state, repeated, flagged, trial_timestamp
  FROM \`$BQ_PROJECT.$BQ_DATASET.flanker\` CROSS JOIN UNNEST(trial_data)"
  
  echo "ds_gradcpt_metadata - inserting data for gradcpt metadata"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_gradcpt_metadata\`
     (sitting_id, person_id, src_id, response_device, screen_height, screen_width, touch, operating_system, test_duration, test_restarted, test_version, test_name, test_short_name, test_language, aou_version, test_params, test_start_date_time, test_end_date_time, user_agent, user_utc_offset)
  SELECT sitting_id, person_id, src_id, metadata.response_device, metadata.screen_height, metadata.screen_width, metadata.touch, metadata.operating_system, metadata.test_duration, metadata.test_restarted, metadata.test_version, metadata.test_name, metadata.test_short_name, metadata.test_language, metadata.aou_version, metadata.test_params, metadata.test_start_date_time, metadata.test_end_date_time, metadata.user_agent, metadata.user_utc_offset
  FROM \`$BQ_PROJECT.$BQ_DATASET.gradcpt\`"
  
  echo "ds_gradcpt_outcomes - inserting data for gradcpt outcomes"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_gradcpt_outcomes\`
     (sitting_id, person_id, src_id, cv_rtc, accuracy, mean_rtc, median_rtc, sd_rtc, go_accuracy, nogo_accuracy, score, dprime, crit, flag_trial_flags, flag_non_response, flag_omission_error_rate)
  SELECT sitting_id, person_id, src_id, outcomes.cv_rtc, outcomes.accuracy, outcomes.mean_rtc, outcomes.median_rtc, outcomes.sd_rtc, outcomes.go_accuracy, outcomes.nogo_accuracy, outcomes.score, outcomes.dprime, outcomes.crit, outcomes.flag_trial_flags, outcomes.flag_non_response, outcomes.flag_omission_error_rate
  FROM \`$BQ_PROJECT.$BQ_DATASET.gradcpt\`"
  
  echo "ds_gradcpt_trial_data - inserting data for gradcpt trial_data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_gradcpt_metadata_trial_data\`
     (sitting_id, person_id, src_id, link_id, trial_id, trial_type, go, trial_length, correct, reaction_time, image_opacity, response_type, response_code, state, flagged, image, trial_timestamp, response_timestamp)
  SELECT sitting_id, person_id, src_id, link_id, trial_id, trial_type, go, trial_length, correct, reaction_time, image_opacity, response_type, response_code, state, flagged, image, trial_timestamp, response_timestamp
  FROM \`$BQ_PROJECT.$BQ_DATASET.gradcpt\` CROSS JOIN UNNEST(trial_data)"
  
  echo "ds_delaydiscounting_metadata - inserting data for delaydiscounting metadata"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_delaydiscounting_metadata\`
     (sitting_id, person_id, src_id, response_device, screen_height, screen_width, touch, operating_system, test_duration, test_restarted, test_version, test_name, test_short_name, test_language, aou_version, test_params, user_utc_offset, test_start_date_time, test_end_date_time, user_agent)
  SELECT sitting_id, person_id, src_id, metadata.response_device, metadata.screen_height, metadata.screen_width, metadata.touch, metadata.operating_system, metadata.test_duration, metadata.test_restarted, metadata.test_version, metadata.test_name, metadata.test_short_name, metadata.test_language, metadata.aou_version, metadata.test_params, metadata.user_utc_offset, metadata.test_start_date_time, metadata.test_end_date_time, metadata.user_agent
  FROM \`$BQ_PROJECT.$BQ_DATASET.delaydiscounting\`"
  
  echo "ds_delaydiscounting_outcomes - inserting data for delaydiscounting outcomes"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_delaydiscounting_outcomes\`
     (sitting_id, person_id, src_id, score, catch_score, lnk, two_weeks_lnk, one_month_lnk, one_year_lnk, ten_years_lnk, flag_catch_trials, any_timeouts, mean_rt, median_rt, sd_rt, flag_median_rt)
  SELECT sitting_id, person_id, src_id, outcomes.score, outcomes.catch_score, outcomes.lnk, outcomes.two_weeks_lnk, outcomes.one_month_lnk, outcomes.one_year_lnk, outcomes.ten_years_lnk, outcomes.flag_catch_trials, outcomes.any_timeouts, outcomes.mean_rt, outcomes.median_rt, outcomes.sd_rt, outcomes.flag_median_rt
  FROM \`$BQ_PROJECT.$BQ_DATASET.delaydiscounting\`"
  
  echo "ds_delaydiscounting_trial_data - inserting data for delaydiscounting trial_data"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_delaydiscounting_trial_data\`
     (sitting_id, person_id, src_id, link_id, trial_id, trial_type, type_num, delay_time, delay_time_days, response, present_amount, future_amount, catch_correct, current_k, reaction_time, state, repeated, trial_timestamp)
  SELECT sitting_id, person_id, src_id, link_id, trial_id, trial_type, type_num, delay_time, delay_time_days, response, present_amount, future_amount, catch_correct, current_k, reaction_time, state, repeated, trial_timestamp
  FROM \`$BQ_PROJECT.$BQ_DATASET.delaydiscounting\` CROSS JOIN UNNEST(trial_data)"
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
          SELECT *
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
          WHERE ancestor_concept_id in
          (
              SELECT concept_id
              FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
              WHERE domain_id = 'SURVEY'
              AND parent_id = 0
              AND concept_id IN (1740639)
          )
      ) b on a.concept_id = b.descendant_concept_id
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
