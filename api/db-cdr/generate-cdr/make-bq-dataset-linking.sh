#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -xeuo pipefail
IFS=$'\n\t'


# get options

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-dataset.sh --bq-project <PROJECT> --bq-dataset <DATASET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done


if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$BQ_DATASET.* ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi


################################################
# CREATE LINKING TABLE
################################################
echo "CREATE TABLE - ds_linking"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`
(
    DENORMALIZED_NAME               STRING,
    OMOP_SQL                        STRING,
    JOIN_VALUE                      STRING,
    DOMAIN                          STRING,
)"

################################################
# INSERT DATA
################################################
echo "ds_linking - inserting conditions data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`
VALUES
    ('PERSON_ID', 'a.PERSON_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_CONCEPT_ID', 'a.CONDITION_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('STANDARD_CONCEPT_NAME', 'c1.concept_name as STANDARD_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID', 'Condition'),
    ('STANDARD_CONCEPT_CODE', 'c1.concept_code as STANDARD_CONCEPT_CODE', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID', 'Condition'),
    ('STANDARD_VOCABULARY', 'c1.vocabulary_id as STANDARD_VOCABULARY', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID', 'Condition'),
    ('CONDITION_START_DATETIME', 'CONDITION_START_DATETIME', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_END_DATETIME', 'CONDITION_END_DATETIME', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_TYPE_CONCEPT_ID', 'CONDITION_TYPE_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_TYPE_CONCEPT_NAME', 'c2.concept_name as CONDITION_TYPE_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONDITION_TYPE_CONCEPT_ID = c2.CONCEPT_ID', 'Condition'),
    ('STOP_REASON', 'STOP_REASON', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('VISIT_OCCURRENCE_ID', 'a.VISIT_OCCURRENCE_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'c3.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id', 'Condition'),
    ('CONDITION_SOURCE_VALUE', 'CONDITION_SOURCE_VALUE', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_SOURCE_CONCEPT_ID', 'CONDITION_SOURCE_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('SOURCE_CONCEPT_NAME', 'c4.concept_name as SOURCE_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.CONDITION_SOURCE_CONCEPT_ID = c4.CONCEPT_ID', 'Condition'),
    ('SOURCE_CONCEPT_CODE', 'c4.concept_code as SOURCE_CONCEPT_CODE', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.CONDITION_SOURCE_CONCEPT_ID = c4.CONCEPT_ID', 'Condition'),
    ('SOURCE_VOCABULARY', 'c4.vocabulary_id as SOURCE_VOCABULARY', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on a.CONDITION_SOURCE_CONCEPT_ID = c4.CONCEPT_ID', 'Condition'),
    ('CONDITION_STATUS_SOURCE_VALUE', 'CONDITION_STATUS_SOURCE_VALUE', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_STATUS_CONCEPT_ID', 'CONDITION_STATUS_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a', 'Condition'),
    ('CONDITION_STATUS_CONCEPT_NAME', 'c5.concept_name as CONDITION_STATUS_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.CONDITION_STATUS_CONCEPT_ID = c5.CONCEPT_ID', 'Condition')"

echo "ds_linking - inserting drug exposure data"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`
VALUES
    ('PERSON_ID', 'a.PERSON_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DRUG_CONCEPT_ID', 'DRUG_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('STANDARD_CONCEPT_NAME', 'c1.concept_name as STANDARD_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.DRUG_CONCEPT_ID = c1.CONCEPT_ID', 'Drug'),
    ('STANDARD_CONCEPT_CODE', 'c1.concept_code as STANDARD_CONCEPT_CODE', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.DRUG_CONCEPT_ID = c1.CONCEPT_ID', 'Drug'),
    ('STANDARD_VOCABULARY', 'c1.vocabulary_id as STANDARD_VOCABULARY', 'left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.DRUG_CONCEPT_ID = c1.CONCEPT_ID', 'Drug'),
    ('DRUG_EXPOSURE_START_DATETIME', 'DRUG_EXPOSURE_START_DATETIME', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DRUG_EXPOSURE_END_DATETIME', 'DRUG_EXPOSURE_END_DATETIME', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('VERBATIM_END_DATE', 'VERBATIM_END_DATE', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DRUG_TYPE_CONCEPT_ID', 'DRUG_TYPE_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DRUG_TYPE_CONCEPT_NAME', 'c2.concept_name as DRUG_TYPE_CONCEPT_NAME', 'LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.drug_type_concept_id = c2.CONCEPT_ID', 'Drug'),
    ('STOP_REASON', 'STOP_REASON', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('REFILLS', 'REFILLS', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('QUANTITY', 'QUANTITY', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DAYS_SUPPLY', 'DAYS_SUPPLY', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('SIG', 'SIG', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('ROUTE_CONCEPT_ID', 'ROUTE_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('ROUTE_CONCEPT_NAME', 'c3.concept_name as ROUTE_CONCEPT_NAME', 'LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on a.ROUTE_CONCEPT_ID = c3.CONCEPT_ID', 'Drug'),
    ('LOT_NUMBER', 'LOT_NUMBER', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('VISIT_OCCURRENCE_ID', 'a.VISIT_OCCURRENCE_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('VISIT_OCCURRENCE_CONCEPT_NAME', 'c4.concept_name as VISIT_OCCURRENCE_CONCEPT_NAME', 'left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.VISIT_CONCEPT_ID = c4.CONCEPT_ID', 'Drug'),
    ('DRUG_SOURCE_VALUE', 'DRUG_SOURCE_VALUE', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DRUG_SOURCE_CONCEPT_ID', 'DRUG_SOURCE_CONCEPT_ID', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('SOURCE_CONCEPT_NAME', 'c5.concept_name as SOURCE_CONCEPT_NAME', 'LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.DRUG_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"', 'Drug'),
    ('SOURCE_CONCEPT_CODE', 'c5.concept_code as SOURCE_CONCEPT_CODE', 'LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.DRUG_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"', 'Drug'),
    ('SOURCE_VOCABULARY', 'c5.vocabulary_id as SOURCE_VOCABULARY', 'LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c5 on a.DRUG_SOURCE_CONCEPT_ID = c5.CONCEPT_ID"', 'Drug'),
    ('ROUTE_SOURCE_VALUE', 'ROUTE_SOURCE_VALUE', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug'),
    ('DOSE_UNIT_SOURCE_VALUE', 'DOSE_UNIT_SOURCE_VALUE', 'from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a', 'Drug')"

