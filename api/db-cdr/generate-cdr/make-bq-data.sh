#!/bin/bash

# This generates big query data that gets put in cloudsql
# Counts and teh public and cdr data four cloudsql
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase
# note  the account must be authorized to perform gcloud and bq operations

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional

USAGE="./generate-clousql-cdr/make-bq-data.sh --project <PROJECT> --account <ACCOUNT> --cdr-version=YYYYMMDD"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

CREDS_ACCOUNT=${ACCOUNT}

# Variables

# TODO make this agument for cdr_vession
bq_cdr_dataset="test_merge_dec26"
NEW_BQ_CDR_DATASET=cloudsql_cdr$CDR_VERSION
schema_path=generate-cdr/bq-schemas
gcs_bucket=gs://all-of-us-workbench-cdr$CDR_VERSION

# Make dataset for cdr cloudsql tables
datasets=`bq --project=$PROJECT ls | grep $NEW_BQ_CDR_DATASET`
echo $datasets
if [[ $datasets =~ .*$NEW_BQ_CDR_DATASET.* ]]; then
  echo "$NEW_BQ_CDR_DATASET exists"
else
  echo "Creating $NEW_BQ_CDR_DATASET"
  catch_create=`eval("bq --project=$PROJECT mk $NEW_BQ_CDR_DATASET")`
fi

# Copy tables we can that we need from cdr to our cloudsql cdr dataset
copy_tables=( concept_relationship domain vocabulary criteria )
for t in "${copy_tables[@]}"
do
  bq --project=$PROJECT rm -f $NEW_BQ_CDR_DATASET.$t
  bq --quiet --project=$PROJECT cp $bq_cdr_dataset.$t $NEW_BQ_CDR_DATASET.$t
done

# Create bq tables we have json schema for
create_tables=( concept achilles_analysis achilles_results achilles_results_concept )
for t in "${create_tables[@]}"
do
  # Make the concept_counts table from cdr
  bq --project=$PROJECT rm -f $NEW_BQ_CDR_DATASET.$t
  bq --quiet --project=$PROJECT mk --schema=$schema_path/$t.json $NEW_BQ_CDR_DATASET.$t
done


###########################
# concept with count cols #
###########################
# We can't just copy concept because the schema has a couple extra columns
# Insert the data into it.
bq --quiet --project=$PROJECT query --allow_large_results --destination_table=$NEW_BQ_CDR_DATASET.concept \
 --use_legacy_sql "SELECT *, 0 as count_value, 0.00 as prevalence FROM [$PROJECT:$bq_cdr_dataset.concept]"

# Convert the date columns into mysql dates where they are not null
bq --quiet --project=$PROJECT query --nouse_legacy_sql \
  "UPDATE \`$NEW_BQ_CDR_DATASET.concept\` \
  SET valid_start_date = Concat(substr(valid_start_date, 1,4), '-',substr(valid_start_date,5,2),'-',substr(valid_start_date,7,2)) \
  WHERE valid_start_date is not null"

bq --quiet --project=$PROJECT query --nouse_legacy_sql \
  "UPDATE \`$NEW_BQ_CDR_DATASET.concept\` \
  SET valid_end_date = Concat(substr(valid_end_date, 1,4), '-',substr(valid_end_date,5,2),'-',substr(valid_end_date,7,2)) \
  WHERE valid_end_date is not null"

########################
# concept_relationship #
########################
# Convert the date columns into mysql dates where they are not null
bq --quiet --project=$PROJECT query --nouse_legacy_sql \
"UPDATE \`$NEW_BQ_CDR_DATASET.concept_relationship\` \
SET valid_start_date = Concat(substr(valid_start_date, 1,4), '-',substr(valid_start_date,5,2),'-',substr(valid_start_date,7,2)) \
WHERE valid_start_date is not null"

bq --quiet --project=$PROJECT query --nouse_legacy_sql \
"UPDATE \`$NEW_BQ_CDR_DATASET.concept_relationship\` \
SET valid_end_date = Concat(substr(valid_end_date, 1,4), '-',substr(valid_end_date,5,2),'-',substr(valid_end_date,7,2)) \
WHERE valid_end_date is not null"

# Run achilles count queries to fill achilles_results
if ./generate-cdr/run-achilles-queries.sh --project $PROJECT --account $ACCOUNT --cdr-version $CDR_VERSION
then
    echo "Achilles queries ran"
else
    echo "FAILED To run achilles queries for CDR $CDR_VERSION"
    exit 1
fi


