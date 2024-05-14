#!/bin/bash

set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

################################################
# CREATE STATIC PREP TABLES FOR TANAGRA
################################################
BUCKET="all-of-us-workbench-private-cloudsql"
FOLDER="static_prep_tables"
SCHEMA_PATH="generate-cdr/bq-schemas"

echo "Loading data into table - prep_concept"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_concept" \
"$BUCKET_FILE" "$SCHEMA_PATH/prep_concept.json"

echo "Loading data into table - prep_concept_relationship"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_concept_relationship" \
"$BUCKET_FILE" "$SCHEMA_PATH/prep_concept_relationship.json"

echo "Loading data into table - prep_cpt"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_cpt" \
"$BUCKET_FILE" "$SCHEMA_PATH/prep_cpt.json"