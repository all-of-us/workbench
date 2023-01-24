#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
# values for DS_DOMAIN_TABLE:
# ds_observation, ds_condition_occurrence, ds_condition_occurrence, ds_drug_exposure
# ds_ds_measurement, ds_procedure_occurrence, ds_device, ds_person
# ds_zip_code_socioeconomic
export DS_DOMAIN_TABLE=$3 # ds_[domain] table to build

################################################
# INSERT DATA FUNCTIONS - DOMAIN
################################################
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
  if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
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
# listed in order of row-counts max->min
# ds_observation, ds_condition_occurrence, ds_condition_occurrence, ds_drug_exposure
# ds_ds_measurement, ds_procedure_occurrence, ds_device, ds_person
# ds_zip_code_socioeconomic
if [[ "$DS_DOMAIN_TABLE" eq "ds_observation" ]]; then
  do_ds_observation
elif [[ "$DS_DOMAIN_TABLE" eq "ds_condition_occurrence" ]]; then
  do_ds_condition_occurrence
elif [[ "$DS_DOMAIN_TABLE" eq "ds_drug_exposure" ]]; then
  do_ds_drug_exposure
elif [[ "$DS_DOMAIN_TABLE" eq "ds_visit_occurrence" ]]; then
  do_ds_visit_occurrence
elif [[ "$DS_DOMAIN_TABLE" eq "ds_measurement" ]]; then
  do_ds_measurement
elif [[ "$DS_DOMAIN_TABLE" eq "ds_procedure_occurrence" ]]; then
  do_ds_procedure_occurrence
elif [[ "$DS_DOMAIN_TABLE" eq "ds_device" ]]; then
  do_ds_device
elif [[ "$DS_DOMAIN_TABLE" eq "ds_person" ]]; then
  do_ds_person
elif [[ "$DS_DOMAIN_TABLE" eq "ds_zip_code_socioeconomic" ]]; then
  do_ds_zip_code_socioeconomic
else
  echo "Unknown table $DS_DOMAIN_TABLE"
  exit 0
fi
#wait for function to return
wait
echo " Done table $DS_DOMAIN_TABLE"


