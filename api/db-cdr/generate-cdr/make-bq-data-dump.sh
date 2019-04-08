#!/bin/bash

# This makes the bq data dump in csv format for a dataset and puts it in google cloud storage
# ACCOUNT must be authorized with gcloud auth login previously

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/make-bq-data-dump.sh --project <PROJECT> --dataset <DATASET>  --bucket=<BUCKET>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --dataset) DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

echo "Dumping tables to csv from $BUCKET"

##echo "Echoing dataset name"
##echo ${DATASET}

# Get tables in project, stripping out tableId.
# Note tables larger than 1 G need to be dumped into more than one file.
# Namin scheme is table_name.*.csv.gz

tables=(concept concept_relationship criteria_relationship cb_criteria_relationship criteria cb_criteria criteria_attribute cb_criteria_attribute domain_info survey_module domain vocabulary criteria_ancestor cb_criteria_ancestor concept_synonym domain_vocabulary_info)

for table in ${tables[@]}; do
  echo "Dumping table : $table"
  # It would be nice to use .* for everything but bq extract does a bad job and you end up with a hundred small files
  # for tables that should just be one file without the .*
  # TODO:Remove criteria
  if [[ $table =~ ^(concept|concept_relationship|concept_synonym|criteria|cb_criteria)$ ]]
  then
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.*.csv.gz
  else
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.csv.gz
  fi
done

exit 0
