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
All_FILES=("$CRITERIA_MENU" "$PREP_CRITERIA" "$PREP_CRITERIA_ANCESTOR" "$PREP_CLINICAL_TERMS")
INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4")

if [[ "${INCOMPATIBLE_DATASETS[@]}" =~ "$BQ_DATASET" ]];
  then
  echo "Can't run CDR build indices against "$BQ_DATASET"!"
  exit 1
fi

rm -rf "$TEMP_FILE_DIR"
mkdir "$TEMP_FILE_DIR"

if gsutil -m cp gs://"$BUCKET"/"$BQ_DATASET"/"$CSV_HOME_DIR"/*.csv "$TEMP_FILE_DIR"
 then
  for file in ${All_FILES[@]}; do
    read -r header < "$TEMP_FILE_DIR"/"$file"
    IFS=',' read -r -a columns <<< "$header"
    case "$file" in
      "$CRITERIA_MENU")
        echo "Processing $CRITERIA_MENU"
        if [[ "$columns" =~ id ]];
        then
          echo "Removing $CRITERIA_MENU header"
          # remove the first line of file
          sed 1d "$TEMP_FILE_DIR"/"$file" > "$TEMP_FILE_DIR"/temp_"$file"
          # zip the file
          gzip -cvf "$TEMP_FILE_DIR"/temp_"$file" > "$TEMP_FILE_DIR"/"$file".gz
        else
          # zip the file
          gzip "$TEMP_FILE_DIR"/"$file"
        fi
        # copy it back to bucket
        gsutil cp "$TEMP_FILE_DIR"/"$file".gz gs://"$BUCKET"/"$BQ_DATASET"/"$CDR_VERSION"/
      ;;
    "$PREP_CRITERIA")
      # TODO: RW-6441
      echo "Processing $PREP_CRITERIA"
      tableName=${file%.*}
      if [[ "$columns" =~ id ]];
      then
        echo "Removing $PREP_CRITERIA header"
        # remove the first line of file
        sed 1d "$TEMP_FILE_DIR"/"$file" > "$TEMP_FILE_DIR"/temp_"$file"
        # rename file
        mv "$TEMP_FILE_DIR"/temp_"$file" "$TEMP_FILE_DIR"/"$file"
      fi
      # copy it back to bucket
      gsutil cp "$TEMP_FILE_DIR"/"$file" gs://"$BUCKET"/"$BQ_DATASET"/"$CSV_HOME_DIR"/

      # Create bq tables we have json schema for
      schema_path=generate-cdr/bq-schemas

      bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET."$tableName"
      bq load --project_id=$BQ_PROJECT --source_format=CSV $BQ_DATASET."$tableName" gs://"$BUCKET"/"$BQ_DATASET"/"$CSV_HOME_DIR"/"$PREP_CRITERIA" $schema_path/"$tableName".json
      echo "Finished loading $PREP_CRITERIA"

    ;;
    "$PREP_CRITERIA_ANCESTOR")
      # TODO: RW-6441
      echo "Processing $PREP_CRITERIA_ANCESTOR"
    ;;
    "$PREP_CLINICAL_TERMS")
      # TODO: RW-6441
      echo "Processing $PREP_CLINICAL_TERMS"
    ;;

    esac

  done
fi

rm -rf "$TEMP_FILE_DIR"
# More validation to come in future stories