#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version
export BUCKET=$4            # Bucket

FILE_DIR="csv"
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

rm -rf "$FILE_DIR"
mkdir "$FILE_DIR"

if gsutil -m cp gs://"$BUCKET"/"$BQ_DATASET"/cdr_csv_files/*.csv "$FILE_DIR"
 then
  for file in ${All_FILES[@]}; do
    read -r header < "$FILE_DIR"/"$file"
    IFS=',' read -r -a columns <<< "$header"
    case "$file" in
      "$CRITERIA_MENU")
        echo "Processing $CRITERIA_MENU"
        if [[ "$columns" =~ id ]];
        echo "Removing header"
        then
          # remove the first line of file
          sed 1d "$FILE_DIR"/"$file" > "$FILE_DIR"/temp_"$file"
          # zip the file
          gzip -cvf "$FILE_DIR"/temp_"$file" > "$FILE_DIR"/"$file".gz
        else
          # zip the file
          gzip "$FILE_DIR"/"$file"
        fi
        # copy it back to bucket
        gsutil cp "$FILE_DIR"/"$file".gz gs://"$BUCKET"/"$BQ_DATASET"/"$CDR_VERSION"/
      ;;
    "$PREP_CRITERIA")
      # TODO: RW-6441
      echo "Processing $PREP_CRITERIA"
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

rm -rf "$FILE_DIR"
# More validation to come in future stories