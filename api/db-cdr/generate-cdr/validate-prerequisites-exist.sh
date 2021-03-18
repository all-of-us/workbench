#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version
export BUCKET=$4            # Bucket

TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cdr_csv_files"
CRITERIA_MENU="cb_criteria_menu.csv"
PREP_CRITERIA="prep_criteria.csv"
PREP_CRITERIA_ANCESTOR="prep_criteria_ancestor.csv"
PREP_CLINICAL_TERMS="prep_clinical_terms_nc.csv"
All_FILES=($CRITERIA_MENU $PREP_CRITERIA $PREP_CRITERIA_ANCESTOR $PREP_CLINICAL_TERMS)
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against "$BQ_DATASET"!"
  exit 1
fi

# Purge all backup csv files except for the last 10 versions
fileCount=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/DummySR/cdr_csv_files/backup | wc -l)
numberToDelete=$(($fileCount - ${#All_FILES[@]} * 10))
if [[ $numberToDelete > 0 ]];
then
  echo "Purging backup files"
  while IFS= read -r line; do
    ts=$(echo $line | cut -d_ -f1)
    if [[ ${#ts} > 0 ]];
    then
      echo "Removing $line"
      gsutil rm gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/$line
    fi
  # This lists all the files in the backup bucket sorted by timestamp and gets only the number to delete
  done < <(gsutil ls gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup | rev | cut -d/ -f1 | rev | sort | head -$numberToDelete)
fi

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

if gsutil -m cp gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
 then
  timestamp=$(date +%s)
  for file in ${All_FILES[@]}; do
    read -r header < $TEMP_FILE_DIR/$file
    IFS=',' read -r -a columns <<< $header
    case $file in
      $CRITERIA_MENU)
        echo "Processing $file"
        if [[ $columns =~ id ]];
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
    $PREP_CRITERIA|$PREP_CRITERIA_ANCESTOR|$PREP_CLINICAL_TERMS)
      tableName=${file%.*}
      if [[ $columns =~ id || $columns =~ ancestor_id || $columns =~ parent ]];
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

      # Backup the csv file
      echo "Backing up $file"
      bq extract --project_id=$BQ_PROJECT --compression GZIP --print_header=false $BQ_DATASET.$tableName gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/backup/"$timestamp"_"$file".gz

      # Load the csv file into table
      echo "Starting load of $file"
      schema_path=generate-cdr/bq-schemas
      bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$tableName
      bq load --project_id=$BQ_PROJECT --source_format=CSV $BQ_DATASET.$tableName gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/$file $schema_path/$tableName.json
      echo "Finished loading $file"
    ;;

    esac

  done
fi

rm -rf $TEMP_FILE_DIR