#!/bin/bash

# This generates the big query data dictionary tables by importing data from
# csv at gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary.

# This script is meant to be run as part of CDR process only. However if its being run individually,
# parameters required for this scripts are:
# BQ_PROJECT: workbench-project (eg all-of-us-rw-preprod) and required dataSet
# eg ./project.rb make_bq_data_dictionary --bq-project all-of-us-workbench-test --bq-dataset synth_r_2019q4_11
set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export DRY_RUN=$3     # dry run

archiveList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/archive/*.csv) > /dev/null || true;
dataDictionaryList=$(gsutil ls gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/*.csv) > /dev/null || true;
dataDictionaryLength=$(gsutil du gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/*.csv | wc -l)

if [ "$DRY_RUN" == true ]
then
  echo "Check if data dictionary file exists"
  if [ -z "$dataDictionaryList" ] && [ -z "$archiveList" ]
  then
    echo "Error: Data Dictionary file is missing!"
    exit 1;
  fi
  exit 0
fi

# Throw error if there is no data_dictionary file for CDR release
echo "Check if data dictionary file exists"
if [ -z "$dataDictionaryList" ] && [ -z "$archiveList" ]
then
  echo "Error: Data Dictionary file is missing!"
  exit 1;
fi
if [ "${dataDictionaryLength}" -gt 1 ]
then
  echo "Data Dictionary cannot have more than 1  CSV file"
  exit 1;
fi
filename="${dataDictionaryList[0]}"

echo "CREATE TABLE - ds_data_dictionary"
schema_path=generate-cdr/bq-schemas
create_tables=(ds_data_dictionary)

for table in "${create_tables[@]}"
do
bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$table
bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/$table.json $BQ_DATASET.$t
done

echo "Load ${filename} into ds_data_dictionary"

bq load --skip_leading_rows=1  --project_id=$BQ_PROJECT --source_format=CSV \
$BQ_DATASET.ds_data_dictionary $filename

echo "move ${filename} to archive folder"
gsutil mv $filename \
"gs://all-of-us-workbench-private-cloudsql/$BQ_DATASET/data_dictionary/archive"

echo "The end"