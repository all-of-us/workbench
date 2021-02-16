#!/bin/bash

# This generates the big query data dictionary tables by importing data from
# csv at gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export DRY_RUN=$3     # dry run

if [ "$DRY_RUN" == true ]
then
  echo "Check if data dictionary file exists"
  archiveList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/archive/*.csv) > /dev/null || true;
  dataDictionaryList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/*.csv) > /dev/null || true;
  if [ -z "$dataDictionaryList" ] && [ -z "$archiveList" ]
  then
    echo "Error: Data Dictionary file is missing!"
    exit 1;
  fi
  exit 0
fi

# Throw error if there is no data_dictionary file for CDR release
echo "Check if data dictionary file exists"
archiveList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/archive/*.csv) > /dev/null || true;
dataDictionaryList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/*.csv) > /dev/null || true;
if [ -z "$dataDictionaryList" ] && [ -z "$archiveList" ]
then
  echo "Error: Data Dictionary file is missing!"
  exit 1;
fi

tableCreated=false;

for filename in $dataDictionaryList; do
  if (! $tableCreated)
  then
    echo "CREATE TABLE - ds_data_dictionary"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.ds_data_dictionary\`
    (
      FIELD_NAME               STRING,
      RELEVANT_OMOP_SQL        STRING,
      DESCRIPTION              STRING,
      FIELD_TYPE               STRING,
      OMOP_CDM_STANDARD_OR_CUSTOM_FIELD  STRING,
      DATA_PROVENANCE          STRING,
      SOURCE_PPI_MODULE        STRING,
      DOMAIN                   STRING
    )"
    tableCreated=true;
  fi
  echo "Load ${filename} into ds_data_dictionary"

  bq load --skip_leading_rows=1  --project_id=$BQ_PROJECT --source_format=CSV \
  $BQ_DATASET.ds_data_dictionary $filename

  echo "move ${filename} to archive folder"
  gsutil mv $filename \
  "gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/archive"
done
echo "The end"