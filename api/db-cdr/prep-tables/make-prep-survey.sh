#!/bin/bash

# Explanation here

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cb_prep_tables/redcap"

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

if gsutil -m cp gs://$BUCKET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
then
  # Files that use the YYYY-MM-DD format are sorted* in alphabetical order.
  # They are also sorted* in chronological order, so the last file will be the most recent
  for file in "$TEMP_FILE_DIR"/*; do
    most_recent_file=$file
  done

  # Iterate
  while IFS= read -r line
  do
    echo "$line"
  done < "$most_recent_file"

fi

rm -rf $TEMP_FILE_DIR