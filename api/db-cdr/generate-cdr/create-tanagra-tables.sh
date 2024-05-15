#!/usr/bin/env bash

# This script removes/creates all CDR indices specific tables.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

echo "Deleting $table_name"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_concept"
wait
  
echo "Creating $table_name"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_concept.json" "$BQ_DATASET.prep_concept"

echo "Deleting $table_name"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_concept_relationship"
wait
  
echo "Creating $table_name"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_concept_relationship.json" "$BQ_DATASET.prep_concept_relationship"

echo "Deleting $table_name"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_cpt"
wait
  
echo "Creating $table_name"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_cpt.json" "$BQ_DATASET.prep_cpt"

echo "Deleting $table_name"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"
wait
  
echo "Creating $table_name"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_survey.json" "$BQ_DATASET.prep_survey"

wait
echo "Done creating tables"

