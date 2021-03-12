#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export CDR_VERSION=$3       # CDR version

FILE_DIR="csv"
All_FILES=("cb_criteria_menu.csv")

rm -rf "$FILE_DIR"
mkdir "$FILE_DIR"
if gsutil cp gs://all-of-us-workbench-private-cloudsql/"${BQ_DATASET}"/cdr_csv_files/*.csv "$FILE_DIR"
 then
  for file in ${All_FILES[@]}; do
    read -r firstline < "$FILE_DIR"/"$file"
    if [[ "$firstline" == id,* ]];
    then
      # remove the first line of file
      sed 1d "$FILE_DIR"/"$file" > "$FILE_DIR"/temp_"$file"
      # zip the file
      gzip -cvf "$FILE_DIR"/temp_"$file" > "$FILE_DIR"/"$file".gz
      # copy it back to bucket
      gsutil cp "$FILE_DIR"/"$file".gz gs://all-of-us-workbench-private-cloudsql/"${BQ_DATASET}"/"${CDR_VERSION}"/
    fi
  done
else
  echo "csv files don't exist, exiting CDR indices build!"
  exit 1
fi
rm -rf "$FILE_DIR"
# More validation to come in future stories