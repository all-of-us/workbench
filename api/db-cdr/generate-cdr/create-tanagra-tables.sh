#!/usr/bin/env bash

# This script removes/creates all CDR indices specific tables.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

echo "Deleting $BQ_DATASET.prep_concept"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_concept"
wait
  
echo "Creating $BQ_DATASET.prep_concept"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_concept.json" "$BQ_DATASET.prep_concept"

echo "Deleting $BQ_DATASET.prep_concept_relationship"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_concept_relationship"
wait
  
echo "Creating $BQ_DATASET.prep_concept_relationship"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_concept_relationship.json" "$BQ_DATASET.prep_concept_relationship"

echo "Deleting $BQ_DATASET.prep_cpt"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_cpt"
wait
  
echo "Creating $BQ_DATASET.prep_cpt"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_cpt.json" "$BQ_DATASET.prep_cpt"

echo "Deleting $BQ_DATASET.prep_survey"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"
wait
  
echo "Creating $BQ_DATASET.prep_survey"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_survey.json" "$BQ_DATASET.prep_survey"

echo "Deleting $BQ_DATASET.prep_pfhh_observation"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_pfhh_observation"
wait
  
echo "Creating $BQ_DATASET.prep_pfhh_observation"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="generate-cdr/bq-schemas/prep_pfhh_observation.json" "$BQ_DATASET.prep_pfhh_observation"

wait
echo "Done creating tables"

