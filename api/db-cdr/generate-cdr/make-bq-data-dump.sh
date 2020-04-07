#!/bin/bash

# This makes the bq data dump in csv format for a dataset and puts it in google cloud storage
# ACCOUNT must be authorized with gcloud auth login previously

set -ex

export PROJECT=$1  # project
export BUCKET=$2  # GCS bucket
export DATASET=$3 # dataset
echo "Dumping tables to csv from $BUCKET"

##echo "Echoing dataset name"
##echo ${DATASET}

# Get tables in project, stripping out tableId.
# Note tables larger than 1 G need to be dumped into more than one file.
# Namin scheme is table_name.*.csv.gz

tables=(concept concept_relationship cb_criteria_relationship cb_criteria cb_criteria_attribute domain_info survey_module domain vocabulary cb_criteria_ancestor concept_synonym cb_person cb_data_filter)

for table in ${tables[@]}; do
  echo "Dumping table : $table"
  # It would be nice to use .* for everything but bq extract does a bad job and you end up with a hundred small files
  # for tables that should just be one file without the .*
  if [[ $table =~ ^(concept|concept_relationship|concept_synonym|cb_criteria|cb_person)$ ]]
  then
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.*.csv.gz
  else
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.csv.gz
  fi
done

exit 0
