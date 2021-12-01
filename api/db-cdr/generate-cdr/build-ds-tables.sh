#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

################################################
# INSERT DATA
################################################
echo "ds_condition_occurrence - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_drug_exposure - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_measurement - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_observation - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_person - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_procedure_occurrence - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_procedure_occurrence\`
    (PERSON_ID, PROCEDURE_CONCEPT_ID, STANDARD_CONCEPT_NAME, STANDARD_CONCEPT_CODE, STANDARD_VOCABULARY,
    PROCEDURE_DATETIME, PROCEDURE_TYPE_CONCEPT_ID, PROCEDURE_TYPE_CONCEPT_NAME, MODIFIER_CONCEPT_ID, MODIFIER_CONCEPT_NAME,
    QUANTITY, VISIT_OCCURRENCE_ID, VISIT_OCCURRENCE_CONCEPT_NAME, PROCEDURE_SOURCE_VALUE, PROCEDURE_SOURCE_CONCEPT_ID,
    SOURCE_CONCEPT_NAME, SOURCE_CONCEPT_CODE, SOURCE_VOCABULARY, QUALIFIER_SOURCE_VALUE)
select
    a.PERSON_ID, PROCEDURE_CONCEPT_ID, c1.concept_name as STANDARD_CONCEPT_NAME, c1.concept_code as STANDARD_CONCEPT_CODE,
    c1.vocabulary_id as STANDARD_VOCABULARY, PROCEDURE_DATETIME, PROCEDURE_TYPE_CONCEPT_ID,
    c2.concept_name as PROCEDURE_TYPE_CONCEPT_NAME, MODIFIER_CONCEPT_ID, c3.concept_name as MODIFIER_CONCEPT_NAME,
    QUANTITY, a.VISIT_OCCURRENCE_ID, c4.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME, PROCEDURE_SOURCE_VALUE,
    PROCEDURE_SOURCE_CONCEPT_ID, c5.concept_name as SOURCE_CONCEPT_NAME, c5.concept_code as SOURCE_CONCEPT_CODE,
    c5.vocabulary_id as SOURCE_VOCABULARY, QUALIFIER_SOURCE_VALUE
from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.PROCEDURE_TYPE_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.MODIFIER_CONCEPT_ID = c3.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.visit_concept_id = c4.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.PROCEDURE_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"

echo "ds_survey - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
   (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer, survey_version_concept_id, survey_version_name)
SELECT  a.person_id,
        a.observation_datetime as survey_datetime,
        c.name as survey,
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
                    and parent_id = 0
            )
    ) b on a.observation_source_concept_id = b.descendant_concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` f on a.observation_id = f.observation_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\` g on f.survey_version_concept_id = g.survey_version_concept_id"

echo "ds_visit_occurrence - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "ds_activity_summary - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_activity_summary\`
    (date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
    lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id)
SELECT date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
    lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`)
where rank = 1"

echo "ds_heart_rate_minute_level - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_minute_level\`
    (datetime, person_id, heart_rate_value)
SELECT datetime, person_id, heart_rate_value
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`)
where rank = 1"

echo "ds_heart_rate_summary - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_summary\`
    (person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count)
SELECT person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`)
where rank = 1"

echo "ds_steps_intraday - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_steps_intraday\`
    (datetime, steps, person_id)
SELECT datetime, steps, person_id
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`)
where rank = 1"

if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
  echo "ds_zip_code_socioeconomic - inserting data"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
fi
