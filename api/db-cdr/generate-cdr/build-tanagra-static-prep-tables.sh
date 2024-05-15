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
"gs://$BUCKET/$FOLDER/prep_concept.csv" "$SCHEMA_PATH/prep_concept.json"

echo "Loading data into table - prep_concept_relationship"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_concept_relationship" \
"gs://$BUCKET/$FOLDER/prep_concept_relationship.csv" "$SCHEMA_PATH/prep_concept_relationship.json"

echo "Loading data into table - prep_cpt"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_cpt" \
"gs://$BUCKET/$FOLDER/prep_cpt.csv" "$SCHEMA_PATH/prep_cpt.json"