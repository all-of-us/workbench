#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version

PREP_TABLE_RUN="!_prep_tables_!"
BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cdr_csv_files"
CRITERIA_MENU="cb_criteria_menu.csv"
DS_DATA_DICTIONARY="ds_data_dictionary.csv"
CB_SURVEY_VERSION="cb_survey_version.csv"
PREP_CDR_DATE="prep_cdr_date.csv"
PREP_CRITERIA="prep_criteria.csv"
PREP_CRITERIA_ANCESTOR="prep_criteria_ancestor.csv"
PREP_CLINICAL_TERMS="prep_clinical_terms_nc.csv"
PREP_CONCEPT="prep_concept.csv"
PREP_CONCEPT_RELATIONSHIP="prep_concept_relationship.csv"
NON_PREP_FILES=($CRITERIA_MENU
           $DS_DATA_DICTIONARY
           $CB_SURVEY_VERSION)
PREP_FILES=($PREP_CDR_DATE
           $PREP_CRITERIA
           $PREP_CRITERIA_ANCESTOR
           $PREP_CLINICAL_TERMS
           $PREP_CONCEPT
           $PREP_CONCEPT_RELATIONSHIP)
ALL_FILES=("${NON_PREP_FILES[@]}" "${PREP_FILES[@]}")
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4")

function removeHeaderIfExist() {
  local file=$1
  local firstColumn=$2
  if [[ $firstColumn == id || \
        $firstColumn == ancestor_id || \
        $firstColumn == parent || \
        $firstColumn == concept_id || \
        $firstColumn == concept_id_1 || \
        $firstColumn == bq_dataset || \
        $firstColumn == survey_version_concept_id ]];
  then
    echo "Removing $file header"
    # Remove the first line of file
    sed 1d $TEMP_FILE_DIR/$file > $TEMP_FILE_DIR/temp_$file
    # Rename file
    mv $TEMP_FILE_DIR/temp_$file $TEMP_FILE_DIR/$file
    # Copy it back to bucket
    echo "Copying $file"
    gsutil cp $TEMP_FILE_DIR/$file gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/
  fi
}

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

function validateCutOffDate() {
  # Validate that a cdr cutoff date exists
  echo "Validating that a CDR cutoff date exists..."
  query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.prep_cdr_date\` where bq_dataset = '$BQ_DATASET'"
  cdrDate=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql "$query" | tr -dc '0-9')
  if [[ $cdrDate != 1 ]];
  then
    echo "CDR cutoff date doesn't exist in $BQ_PROJECT.$BQ_DATASET.prep_cdr_date!"
    exit 1
  fi
  echo "CDR cutoff date is valid!"
}

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against "$BQ_DATASET"!"
  exit 1
fi

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

# Process the prep tables only
if [[ $CDR_VERSION == $PREP_TABLE_RUN ]];
then

  if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
  then
    for file in ${PREP_FILES[@]}; do
      read -r header < $TEMP_FILE_DIR/$file
      IFS=',' read -r -a columns <<< $header
      removeHeaderIfExist file $(echo $columns | cut -d' ' -f 1)
      loadCSVFile $file ${file%.*}
    done
  fi

  rm -rf $TEMP_FILE_DIR

  validateCutOffDate

  echo "Validation is complete"

else

  # Process all tables with full validation
  if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
  then
    timestamp=$(date +%s)
    for file in ${ALL_FILES[@]}; do
      read -r header < $TEMP_FILE_DIR/$file
      IFS=',' read -r -a columns <<< $header
      firstColumn=$(echo $columns | cut -d' ' -f 1)
      case $file in
        $CRITERIA_MENU|$DS_DATA_DICTIONARY)
          echo "Processing $file"
          if [[ $firstColumn == id ]];
          then
            echo "Removing $file header"
            # Remove the first line of file
            sed 1d $TEMP_FILE_DIR/$file > $TEMP_FILE_DIR/temp_$file
            # Zip the file
            gzip -cvf $TEMP_FILE_DIR/temp_$file > $TEMP_FILE_DIR/$file.gz
          else
            # Zip the file
            gzip $TEMP_FILE_DIR/$file
          fi
          # Copy it back to bucket
          gsutil cp $TEMP_FILE_DIR/$file.gz gs://$BUCKET/$BQ_DATASET/$CDR_VERSION/
          # Backup the csv file
          echo "Backing up $file"
          gsutil cp $TEMP_FILE_DIR/$file.gz gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/"$timestamp"_"$file".gz
        ;;
      $CB_SURVEY_VERSION | \
      $PREP_CDR_DATE | \
      $PREP_CRITERIA | \
      $PREP_CRITERIA_ANCESTOR | \
      $PREP_CLINICAL_TERMS | \
      $PREP_CONCEPT | \
      $PREP_CONCEPT_RELATIONSHIP)
        removeHeaderIfExist file firstColumn

        # Check to see if table exists
        tableName=${file%.*}
        tables=$(bq ls --max_results 1000 "$BQ_PROJECT:$BQ_DATASET" | awk '{print $1}' | tail +3)
        for table in ${tables[@]};
        do
          if [[ $table == $tableName ]];
          then
            echo "Backing up $file"
            bq extract --project_id=$BQ_PROJECT --compression GZIP --print_header=false \
            $BQ_DATASET.$tableName gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/"$timestamp"_"$file".gz
          fi
        done

        loadCSVFile $file $tableName
      ;;
      esac
    done
  fi

  rm -rf $TEMP_FILE_DIR

  validateCutOffDate

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

  # Purge all backup csv files except for the last 10 versions
  fileCount=$(gsutil ls gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup | wc -l)
  allFilesCount=${#ALL_FILES[@]}
  numberToDelete=$(($((fileCount - 1)) - $((allFilesCount * 10))))
  if [[ $numberToDelete > 0 ]];
  then
    echo "Purging $numberToDelete backup files"
    while IFS= read -r line; do
      echo "Removing $line"
      gsutil rm gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/$line
    # This lists all the files in the backup bucket sorted by timestamp and gets only the number to delete
    done < <(gsutil ls gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup | rev | cut -d/ -f1 | rev | sort | awk 'NF' | head -$numberToDelete)
  fi

fi