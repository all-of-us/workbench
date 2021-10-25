#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

################################################
# CREATE TABLES
################################################
echo "CREATE TABLE - ds_condition_occurrence"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_condition_occurrence\`
(
    PERSON_ID                       INT64,
    CONDITION_CONCEPT_ID            INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    CONDITION_START_DATETIME        TIMESTAMP,
    CONDITION_END_DATETIME          TIMESTAMP,
    CONDITION_TYPE_CONCEPT_ID       INT64,
    CONDITION_TYPE_CONCEPT_NAME     STRING,
    STOP_REASON                     STRING,
    VISIT_OCCURRENCE_ID             INT64,
    VISIT_OCCURRENCE_CONCEPT_NAME   STRING,
    CONDITION_SOURCE_VALUE          STRING,
    CONDITION_SOURCE_CONCEPT_ID     INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    CONDITION_STATUS_SOURCE_VALUE   STRING,
    CONDITION_STATUS_CONCEPT_ID     INT64,
    CONDITION_STATUS_CONCEPT_NAME   STRING
)"

echo "CREATE TABLE - ds_drug_exposure"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_drug_exposure\`
(
    PERSON_ID                       INT64,
    DRUG_CONCEPT_ID                 INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    DRUG_EXPOSURE_START_DATETIME    TIMESTAMP,
    DRUG_EXPOSURE_END_DATETIME      TIMESTAMP,
    VERBATIM_END_DATE               DATE,
    DRUG_TYPE_CONCEPT_ID            INT64,
    DRUG_TYPE_CONCEPT_NAME          STRING,
    STOP_REASON                     STRING,
    REFILLS                         INT64,
    QUANTITY                        FLOAT64,
    DAYS_SUPPLY                     INT64,
    SIG                             STRING,
    ROUTE_CONCEPT_ID                INT64,
    ROUTE_CONCEPT_NAME              STRING,
    LOT_NUMBER                      STRING,
    VISIT_OCCURRENCE_ID             INT64,
    VISIT_OCCURRENCE_CONCEPT_NAME   STRING,
    DRUG_SOURCE_VALUE               STRING,
    DRUG_SOURCE_CONCEPT_ID          INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    ROUTE_SOURCE_VALUE              STRING,
    DOSE_UNIT_SOURCE_VALUE          STRING
)"

echo "CREATE TABLE - ds_measurement"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_measurement\`
(
    PERSON_ID                       INT64,
    MEASUREMENT_CONCEPT_ID          INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    MEASUREMENT_DATETIME            TIMESTAMP,
    MEASUREMENT_TYPE_CONCEPT_ID     INT64,
    MEASUREMENT_TYPE_CONCEPT_NAME   STRING,
    OPERATOR_CONCEPT_ID             INT64,
    OPERATOR_CONCEPT_NAME           STRING,
    VALUE_AS_NUMBER                 FLOAT64,
    VALUE_AS_CONCEPT_ID             INT64,
    VALUE_AS_CONCEPT_NAME           STRING,
    UNIT_CONCEPT_ID                 INT64,
    UNIT_CONCEPT_NAME               STRING,
    RANGE_LOW                       FLOAT64,
    RANGE_HIGH                      FLOAT64,
    VISIT_OCCURRENCE_ID             INT64,
    VISIT_OCCURRENCE_CONCEPT_NAME   STRING,
    MEASUREMENT_SOURCE_VALUE        STRING,
    MEASUREMENT_SOURCE_CONCEPT_ID   INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    UNIT_SOURCE_VALUE               STRING,
    VALUE_SOURCE_VALUE              STRING
)"

echo "CREATE TABLE - ds_observation"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_observation\`
(
    PERSON_ID                       INT64,
    OBSERVATION_CONCEPT_ID          INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    OBSERVATION_DATETIME            TIMESTAMP,
    OBSERVATION_TYPE_CONCEPT_ID     INT64,
    OBSERVATION_TYPE_CONCEPT_NAME   STRING,
    VALUE_AS_NUMBER                 FLOAT64,
    VALUE_AS_STRING                 STRING,
    VALUE_AS_CONCEPT_ID             INT64,
    VALUE_AS_CONCEPT_NAME           STRING,
    QUALIFIER_CONCEPT_ID            INT64,
    QUALIFIER_CONCEPT_NAME          STRING,
    UNIT_CONCEPT_ID                 INT64,
    UNIT_CONCEPT_NAME               STRING,
    VISIT_OCCURRENCE_ID             INT64,
    VISIT_OCCURRENCE_CONCEPT_NAME   STRING,
    OBSERVATION_SOURCE_VALUE        STRING,
    OBSERVATION_SOURCE_CONCEPT_ID   INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    UNIT_SOURCE_VALUE               STRING,
    QUALIFIER_SOURCE_VALUE          STRING,
    value_source_concept_id         INT64,
    value_source_value              STRING,
    questionnaire_response_id       INT64
)"

echo "CREATE TABLE - ds_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_person\`
(
    PERSON_ID               INT64,
    GENDER_CONCEPT_ID       INT64,
    GENDER                  STRING,
    DATE_OF_BIRTH           TIMESTAMP,
    RACE_CONCEPT_ID         INT64,
    RACE                    STRING,
    ETHNICITY_CONCEPT_ID    INT64,
    ETHNICITY               STRING,
    SEX_AT_BIRTH_CONCEPT_ID INT64,
    SEX_AT_BIRTH            STRING
)"

echo "CREATE TABLE - ds_procedure_occurrence"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_procedure_occurrence\`
(
    PERSON_ID                       INT64,
    PROCEDURE_CONCEPT_ID            INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    PROCEDURE_DATETIME              TIMESTAMP,
    PROCEDURE_TYPE_CONCEPT_ID       INT64,
    PROCEDURE_TYPE_CONCEPT_NAME     STRING,
    MODIFIER_CONCEPT_ID             INT64,
    MODIFIER_CONCEPT_NAME           STRING,
    QUANTITY                        INT64,
    VISIT_OCCURRENCE_ID             INT64,
    VISIT_OCCURRENCE_CONCEPT_NAME   STRING,
    PROCEDURE_SOURCE_VALUE          STRING,
    PROCEDURE_SOURCE_CONCEPT_ID     INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    QUALIFIER_SOURCE_VALUE          STRING
)"

echo "CREATE TABLE - ds_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
(
    person_id           INT64,
    survey_datetime     TIMESTAMP,
    survey              STRING,
    question_concept_id INT64,
    question            STRING,
    answer_concept_id   INT64,
    answer              STRING,
    survey_version_concept_id   INT64,
    survey_version_name STRING
)"

echo "CREATE TABLE - ds_visit_occurrence"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_visit_occurrence\`
(
    PERSON_ID                       INT64,
    VISIT_CONCEPT_ID                INT64,
    STANDARD_CONCEPT_NAME           STRING,
    STANDARD_CONCEPT_CODE           STRING,
    STANDARD_VOCABULARY             STRING,
    VISIT_START_DATETIME            TIMESTAMP,
    VISIT_END_DATETIME              TIMESTAMP,
    VISIT_TYPE_CONCEPT_ID           INT64,
    VISIT_TYPE_CONCEPT_NAME         STRING,
    VISIT_SOURCE_VALUE              STRING,
    VISIT_SOURCE_CONCEPT_ID         INT64,
    SOURCE_CONCEPT_NAME             STRING,
    SOURCE_CONCEPT_CODE             STRING,
    SOURCE_VOCABULARY               STRING,
    ADMITTING_SOURCE_CONCEPT_ID     INT64,
    ADMITTING_SOURCE_CONCEPT_NAME   STRING,
    ADMITTING_SOURCE_VALUE          STRING,
    DISCHARGE_TO_CONCEPT_ID         INT64,
    DISCHARGE_TO_CONCEPT_NAME       STRING,
    DISCHARGE_TO_SOURCE_VALUE       STRING
)"

echo "CREATE TABLE - ds_activity_summary"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_activity_summary\`
(
    DATE                            DATE,
    ACTIVITY_CALORIES               FLOAT64,
    CALORIES_BMR                    FLOAT64,
    CALORIES_OUT                    FLOAT64,
    ELEVATION                       FLOAT64,
    FAIRLY_ACTIVE_MINUTES           FLOAT64,
    FLOORS                          INT64,
    LIGHTLY_ACTIVE_MINUTES          FLOAT64,
    MARGINAL_CALORIES               FLOAT64,
    SEDENTARY_MINUTES               FLOAT64,
    STEPS                           INT64,
    VERY_ACTIVE_MINUTES             FLOAT64,
    PERSON_ID                       INT64
)"

echo "CREATE TABLE - ds_heart_rate_minute_level"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_minute_level\`
(
    DATETIME                        DATETIME,
    PERSON_ID                       INT64,
    HEART_RATE_VALUE                INT64
)"

echo "CREATE TABLE - ds_heart_rate_summary"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_summary\`
(
    PERSON_ID                       INT64,
    DATE                            DATE,
    ZONE_NAME                       STRING,
    MIN_HEART_RATE                  INT64,
    MAX_HEART_RATE                  INT64,
    MINUTE_IN_ZONE                  INT64,
    CALORIE_COUNT                   FLOAT64
)"

echo "CREATE TABLE - ds_steps_intraday"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_steps_intraday\`
(
    DATETIME                        DATETIME,
    STEPS                           NUMERIC,
    PERSON_ID                       INT64
)"

if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
  echo "CREATE TABLE - ds_zip_code_socioeconomic"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_zip_code_socioeconomic\`
  (
      PERSON_ID                       INT64,
      OBSERVATION_DATETIME            TIMESTAMP,
      ZIP_CODE                        STRING,
      ASSISTED_INCOME                 FLOAT64,
      HIGH_SCHOOL_EDUCATION           FLOAT64,
      MEDIAN_INCOME                   FLOAT64,
      NO_HEALTH_INSURANCE             FLOAT64,
      POVERTY                         FLOAT64,
      VACANT_HOUSING                  FLOAT64,
      DEPRIVATION_INDEX               FLOAT64,
      AMERICAN_COMMUNITY_SURVEY_YEAR  INT64
  )"
fi

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
  (PERSON_ID, OBSERVATION_DATETIME, ZIP_CODE, ASSISTED_INCOME, HIGH_SCHOOL_EDUCATION, MEDIAN_INCOME, NO_HEALTH_INSURANCE, POVERTY, VACANT_HOUSING, DEPRIVATION_INDEX, AMERICAN_COMMUNITY_SURVEY_YEAR)
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
