#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cdr_csv_files"
CRITERIA_MENU="cb_criteria_menu.csv"
DS_DATA_DICTIONARY="ds_data_dictionary.csv"
CB_SURVEY_VERSION="cb_survey_version.csv"
PREP_CPT="prep_cpt.csv"
PREP_CLINICAL_TERMS="prep_clinical_terms_nc.csv"
PREP_CONCEPT="prep_concept.csv"
PREP_CONCEPT_RELATIONSHIP="prep_concept_relationship.csv"
PREP_SURVEY="prep_survey.csv"
PREP_PHYSICAL_MEASUREMENT="prep_physical_measurement.csv"
NON_PREP_FILES=($CRITERIA_MENU
           $DS_DATA_DICTIONARY
           $CB_SURVEY_VERSION)
PREP_FILES=($PREP_CPT
           $PREP_CLINICAL_TERMS
           $PREP_CONCEPT
           $PREP_CONCEPT_RELATIONSHIP
           $PREP_SURVEY
           $PREP_PHYSICAL_MEASUREMENT)
ALL_FILES=("${NON_PREP_FILES[@]}" "${PREP_FILES[@]}")
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
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4")

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

for table in ${DEPENDENT_TABLES[@]}; do
  echo "Validating that $table exists!"
  tableInfo=$(bq show "$BQ_PROJECT:$BQ_DATASET.$table")
  echo $tableInfo
done

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

# Process all tables with full validation
if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
then
  for file in ${ALL_FILES[@]}; do
    case $file in
      $CRITERIA_MENU|$DS_DATA_DICTIONARY)
        echo "Processing $file"
        gzip $TEMP_FILE_DIR/$file
        # Copy it back to bucket
        gsutil cp $TEMP_FILE_DIR/$file.gz gs://$BUCKET/$BQ_DATASET/$CDR_VERSION/
      ;;
    $CB_SURVEY_VERSION | \
    $PREP_CPT | \
    $PREP_CLINICAL_TERMS | \
    $PREP_CONCEPT | \
    $PREP_CONCEPT_RELATIONSHIP | \
    $PREP_SURVEY | \
    $PREP_PHYSICAL_MEASUREMENT)
      # Check to see if table exists
      tableName=${file%.*}
      loadCSVFile $file $tableName
    ;;
    esac
  done
fi

rm -rf $TEMP_FILE_DIR

# Validate that all survey version exist
echo "Validating that all survey versions exist..."
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`
where survey_version_concept_id not in
( select distinct survey_version_concept_id from \`$BQ_PROJECT.$BQ_DATASET.observation_ext\`)"
surveyVersionCount=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql "$query" | tr -dc '0-9')
if [[ $surveyVersionCount != 0 ]];
then
  echo "Missing survey version in $BQ_PROJECT.$BQ_DATASET.cb_survey_version!"
  exit 1
fi
echo "Survey versions are valid!"