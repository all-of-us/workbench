#!/bin/bash

# Create tables from all the csv files in gs://all-of-us-workbench-private-cloudsql/static_prep_tables
set -ex

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

################################################
# CREATE STATIC PREP TABLES
################################################
BUCKET="all-of-us-workbench-private-cloudsql"
FOLDER="static_prep_tables"
SCHEMA_PATH="generate-cdr/bq-schemas"

BUCKET_FILES=( $(gsutil ls gs://$BUCKET/$FOLDER/*.csv* 2> /dev/null || true) )

for BUCKET_FILE in "${BUCKET_FILES[@]}"
do
   # Get table name from file
   FILENAME="${BUCKET_FILE##*/}"  # gets everything after last /
   TABLE_NAME=${FILENAME%%.*}     # truncates everything starting with first .
   echo "Creating table - $TABLE_NAME"
   bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$TABLE_NAME"
   bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.$TABLE_NAME" \
   "$BUCKET_FILE" "$SCHEMA_PATH/$TABLE_NAME.json"
done