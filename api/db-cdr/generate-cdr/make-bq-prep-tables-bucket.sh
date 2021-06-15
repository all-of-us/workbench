#!/bin/bash

# Create a new CDR bucket and copy over reusable csv files from previous CDR bucket.
set -e

export NEW_CDR_VERSION=$1       # New CDR version
export PREVIOUS_CDR_VERSION=$2  # Previous CDR version

BUCKET="all-of-us-workbench-private-cloudsql"
FOLDER="cdr_csv_files"
CSV_FILES=("cb_criteria_menu.csv"
"cb_survey_version.csv"
"ds_data_dictionary.csv"
"prep_cdr_date.csv"
"prep_clinical_terms_nc.csv"
"prep_concept.csv"
"prep_concept_relationship.csv"
"prep_criteria.csv"
"prep_physical_measurement.csv")

for file in ${CSV_FILES[@]}; do
  gsutil cp gs://$BUCKET/$PREVIOUS_CDR_VERSION/$FOLDER/$file gs://$BUCKET/$NEW_CDR_VERSION/$FOLDER/
done