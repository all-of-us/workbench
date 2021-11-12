#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version
export DATA_BROWSER=$4      # data browser flag

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cdr_csv_files"
PREP_SURVEY="prep_survey.csv"
ALL_FILES=($PREP_SURVEY)
DEPENDENT_TABLES=("activity_summary"
            "concept"
            "concept_ancestor"
            "concept_relationship"
            "concept_synonym"
            "condition_occurrence"
            "condition_occurrence_ext"
            "death"
            "device_exposure"
            "device_exposure_ext"
            "domain"
            "drug_exposure"
            "drug_exposure_ext"
            "heart_rate_minute_level"
            "heart_rate_summary"
            "measurement"
            "measurement_ext"
            "observation"
            "observation_ext"
            "person"
            "procedure_occurrence"
            "procedure_occurrence_ext"
            "relationship"
            "steps_intraday"
            "visit_occurrence"
            "visit_occurrence_ext"
            "vocabulary")
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4", "R2020Q4R3")

function loadCSVFile() {
  local file=$1
  local tableName=$2
  # Load the csv file into table
  echo "Starting load of $file"
  schema_path=generate-cdr/bq-schemas
  bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$tableName
  bq load --project_id=$BQ_PROJECT --source_format=CSV $BQ_DATASET.$tableName \
  gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/$file $schema_path/$tableName.json
  echo "Finished loading $file"
}

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against "$BQ_DATASET"!"
  exit 1
fi

if [ "$DATA_BROWSER" == false ]
then
  for table in ${DEPENDENT_TABLES[@]}; do
    echo "Validating that $table exists!"
    tableInfo=$(bq show "$BQ_PROJECT:$BQ_DATASET.$table")
    echo $tableInfo
  done
fi

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

# Process all tables
if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
then
  for file in ${ALL_FILES[@]}; do
    tableName=${file%.*}
    loadCSVFile $file $tableName
  done
fi

rm -rf $TEMP_FILE_DIR