#!/bin/bash

# Explanation here

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cb_prep_tables/redcap"
CSV_OUTPUT_DIR="output"
ALL_CSV="_updated_all.csv"
REGISTERED_CSV="_updated_registered.csv"
CONTROLLED_CSV="_updated_controlled.csv"

function main() {
  rm -rf $TEMP_FILE_DIR
  mkdir $TEMP_FILE_DIR

  if gsutil -m cp gs://$BUCKET/$CSV_HOME_DIR/*.csv $TEMP_FILE_DIR
  then

    FILE_PREFIX=()
    # Collect each type of redcap survey csv file
    for file in "$TEMP_FILE_DIR"/*; do
      substring=$(echo $file | cut -d'_' -f 1)
      substring=$(echo $substring | cut -d'/' -f 2)
      if [[ " ${FILE_PREFIX[@]} " =~ " ${substring} " ]];
      then
        continue
      else
        FILE_PREFIX+=($substring)
      fi
    done

    MOST_RECENT_REDCAP_CSVS=()
    # Collect the most recent version of each redcap survey csv type.
    # Files that use the YYYY-MM-DD format are sorted* in alphabetical order.
    # They are also sorted* in chronological order, so the last file will be the most recent
    for prefix in "${FILE_PREFIX[@]}"; do
      for file in "$TEMP_FILE_DIR"/"$prefix"*; do
        most_recent_file=$file
      done
      MOST_RECENT_REDCAP_CSVS+=($most_recent_file)
    done

    # Iterate the most recent redcap survey csv files and generate a csv file for
    # 1) all 2) registered 3) controlled
    for file in "${MOST_RECENT_REDCAP_CSVS[@]}"; do

      echo "Processing survey: "$file

      initalizeHeaderRows $file

      while IFS= read -r line
      do

        # All of your work goes here. Below some code to query the BQ
        # database and write some csv files for All, Controlled and Registered

        # Query proper BQ dataset
        sqlParam="Respiratory Conditions"
        #sql="SELECT concept_code FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` WHERE concept_name = '$sqlParam' and vocabulary_id = 'PPI' and concept_class_id = 'Topic'"
        #concept_code=$(echo $(echo -e $sql | bq query --quiet --nouse_legacy_sql --format=json) | jq '.[0]' | jq '.concept_code')
        #echo $concept_code

        # Write rows in new csv files
        writeAllRow $file
        writeRegisteredRow $file
        writeControlledRow $file

      done < $file
    done

    # Copy the generated csv files back to proper bucket
    for file in "${MOST_RECENT_REDCAP_CSVS[@]}"; do
      filePrefix $file
      echo "Copying "$prefix$REGISTERED_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$REGISTERED_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
      echo "Copying "$prefix$CONTROLLED_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$CONTROLLED_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
      echo "Copying "$prefix$ALL_CSV" to bucket"
      gsutil cp $TEMP_FILE_DIR/$prefix$ALL_CSV gs://$BUCKET/$CSV_HOME_DIR/$CSV_OUTPUT_DIR/
    done

  fi

  rm -rf $TEMP_FILE_DIR
}

function initalizeHeaderRows() {
  # Initialize output files with header rows
  filePrefix $1
  echo "id,parent_id,code,name,type,min,max,answers_bucketed" >> "$TEMP_FILE_DIR/$prefix$REGISTERED_CSV"
  echo "id,parent_id,code name,type,min,max,answers_bucketed" >> "$TEMP_FILE_DIR/$prefix$CONTROLLED_CSV"
  echo "id,parent_id,code,name,type,min,max,registered_topic_suppressed,registered_question_suppressed,registered_answer_bucketed,
  controlled_topic_suppressed,controlled_question_suppressed,controlled_answer_bucketed" >> "$TEMP_FILE_DIR/$prefix$ALL_CSV"
}

function writeAllRow() {
  # Write a row to All file
  filePrefix $1
  echo "0,0,001,name,type,0,0,0,0,0,0,0,0" >> "$TEMP_FILE_DIR/$prefix$ALL_CSV"
}

function writeControlledRow() {
  # Write a row to Controlled file
  filePrefix $1
  echo "0,0,001,name,type,0,0,0" >> "$TEMP_FILE_DIR/$prefix$CONTROLLED_CSV"
}

function writeRegisteredRow() {
  # Write a row to Registered file
  filePrefix $1
  echo "0,0,001,name,type,0,0,0" >> "$TEMP_FILE_DIR/$prefix$REGISTERED_CSV"
}

function filePrefix() {
  local file=$1
  prefix=$(echo $file | cut -d'_' -f 1)
  prefix=$(echo $prefix | cut -d'/' -f 2)
  prefix=${prefix:7}
}

main