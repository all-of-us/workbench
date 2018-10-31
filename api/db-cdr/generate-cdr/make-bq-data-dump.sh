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

if [[ $DATASET == *public* ]] || [[ $DATASET == *PUBLIC* ]];
then
    tables=(achilles_analysis achilles_results achilles_results_dist concept concept_relationship criteria domain_info survey_module domain vocabulary concept_synonym domain_vocabulary_info)
else
    tables=(achilles_analysis achilles_results achilles_results_dist concept concept_relationship criteria criteria_attribute domain_info survey_module domain vocabulary concept_ancestor concept_synonym domain_vocabulary_info)
fi

for table in ${tables[@]}; do
  echo "Dumping table : $table"
  # It would be nice to use .* for everything but bq extract does a bad job and you end up with a hundred small files
  # for tables that should just be one file without the .*
  if [[ $table =~ ^(concept|concept_relationship|concept_ancestor|concept_synonym|criteria)$ ]]
  then
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.*.csv.gz
  else
    bq extract --project_id $PROJECT --compression=GZIP --print_header=false $PROJECT:$DATASET.$table \
    gs://$BUCKET/$DATASET/$table.csv.gz
  fi
done

exit 0
