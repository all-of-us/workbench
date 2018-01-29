#!/bin/bash

# This makes the bq data dump in csv format for a dataset and puts it in google cloud storage
# ACCOUNT must be authorized with gcloud auth login previously

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/make-bq-data-dump.sh --account <ACCOUNT> --project <PROJECT> --dataset <DATASET>  --bucket=<BUCKET>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --dataset) DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

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


CREDS_ACCOUNT=${ACCOUNT}

echo "Dumping tables to csv from $BUCKET\n"

# Get tables in project, stripping out tableId.
# Note tables larger than 1 G need to be dumped into more than one file.
# concept_relationship and concept are only big ones now.

tables=`bq ls $PROJECT:$DATASET | tr -d "-" |  tr -s " " |  cut -f 2 -d' ' | sed "s/tableId//"`

for table in $tables; do
  echo "Dumping table : $table"
  if [[ $table =~ ^(concept|concept_relationship)$ ]]
  then
    bq extract $PROJECT:$DATASET.$table gs://$BUCKET/$DATASET/$table*.csv
  else
    bq extract $PROJECT:$DATASET.$table gs://$BUCKET/$DATASET/$table.csv
  fi
done

exit 0
