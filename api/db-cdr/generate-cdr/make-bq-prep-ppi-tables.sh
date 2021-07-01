#!/bin/bash

# Create a new prep ppi tables from redcap file process in bucket: all-of-us-workbench-private-cloudsql/cb_prep_tables/redcap/$DATE.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export DATE=$3              # Redcap survey file date
export TIER=$4              # Ex: registered or controlled

BUCKET="all-of-us-workbench-private-cloudsql"
TEMP_FILE_DIR="csv"
CSV_HOME_DIR="cb_prep_tables/redcap/$DATE"

echo "Starting load of prep ppi tables into $BQ_PROJECT:$BQ_DATASET"

rm -rf $TEMP_FILE_DIR
mkdir $TEMP_FILE_DIR

gsutil -m cp gs://$BUCKET/$CSV_HOME_DIR/*_"$TIER".csv $TEMP_FILE_DIR
#schema_path=generate-cdr/bq-schemas
#bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$tableName
#bq load --project_id=$BQ_PROJECT --source_format=CSV $BQ_DATASET.$tableName \
#gs://$BUCKET/$BQ_DATASET/$CSV_HOME_DIR/$file $schema_path/$tableName.json

#rm -rf $TEMP_FILE_DIR
echo "Finished loading prep ppi tables into $BQ_PROJECT:$BQ_DATASETe"