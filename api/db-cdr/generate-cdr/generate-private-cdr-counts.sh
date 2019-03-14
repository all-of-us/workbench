#!/bin/bash

# This generates new BigQuery dataset for use in cloudsql by the workbench
# and dumps csvs of that dataset to import to cloudsql

# End product is:
# 0) Big query dataset for cdr version cdrYYYYMMDD
# 1) .csv of all the tables in a bucket

# Example usage, you need to provide a bunch of args
# ./project.rb generate-private-cdr-counts --bq-project all-of-us-ehr-dev --bq-dataset test_merge_dec26 \
# --workbench-project all-of-us-workbench-test --cdr-version 20180130 \
# --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'


USAGE="./generate-cdr/generate-private-cdr-counts --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --bucket <BUCKET> --cdr-version=YYYYMMDD"
USAGE="$USAGE \n Data is generated from bq-project.bq-dataset and dumped to workbench-project.cdr<cdr-version>."

BQ_PROJECT=""
BQ_DATASET=""
WORKBENCH_PROJECT=""
CDR_VERSION=""
BUCKET=""

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    -- ) shift; echo -e "Usage: $USAGE"; break ;;
    * ) break ;;
  esac
done

if [ -z "${BQ_PROJECT}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing bq project name"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing bq_dataset name"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing workbench_project name"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing bucket name"
  exit 1
fi

#Check cdr_version is not empty
if [ -z "${CDR_VERSION}" ]
then
  echo -e "Usage: $USAGE"
  echo -e "Missing cdr version"
  exit 1
fi

WORKBENCH_DATASET=$CDR_VERSION

startDate=$(date)
echo $(date) " Starting generate-private-cdr-counts $startDate"

## Make workbench cdr count data
echo "Making BigQuery cdr dataset"
if ./generate-cdr/make-bq-data.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --output-project $WORKBENCH_PROJECT \
 --output-dataset $WORKBENCH_DATASET --cdr-version "$CDR_VERSION"
then
    echo "BigQuery cdr data generated"
else
    echo "FAILED To generate BigQuery data for cdr $CDR_VERSION"
    exit 1
fi

## Dump workbench cdr count data
echo "Dumping BigQuery cdr dataset to .csv"
if ./generate-cdr/make-bq-data-dump.sh --dataset $WORKBENCH_DATASET --project $WORKBENCH_PROJECT --bucket $BUCKET
then
    echo "Workbench cdr count data dumped"
else
    echo "FAILED to dump Workbench cdr count data"
    exit 1
fi

stopDate=$(date)
echo "Start $startDate Stop: $stopDate"
echo $(date) " Finished generate-private-cdr-counts "

