#!/bin/bash

# This generates new cloudsql database for a cdr with counts
# note account must be preauthorized with gcloud auth login

# End product is:
# 1) Local mysql database cdrYYYYMMDD
# 2) Local mysql database publicYYYYMMDD
# 3) sql dump of both of these in google cloud storage

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-clousql-cdr --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --account <ACCOUNT> --cdr-version=YYYYMMDD"
USAGE="$USAGE \n Data is generated from bq-project.bq-dataset and dumped to workbench-project.cdr<cdr-version>."
USAGE="$USAGE \n Local mysql databases named cdr<cdr-version> and public<cdr-version> are created and populated."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

# Make BigQuery dbs
echo "Making big query dataset for cloudsql cdr"
if ./generate-cdr/make-bq-data.sh --bq-project $BQ_PROJECT --bq-dataset $BQ_DATASET --workbench-project $WORKBENCH_PROJECT --account $ACCOUNT --cdr-version $CDR_VERSION
then
    echo "BIG QUERY CDR Data Generated"
else
    echo "FAILED To Generate BIG QUERY Data For CDR $CDR_VERSION"
    exit 1
fi

# Make BigQuery data dump
dataset=cdr$CDR_VERSION
echo "Making big query dataset for cloudsql cdr"
if ./generate-cdr/make-bq-data-dump.sh --dataset $dataset --project $WORKBENCH_PROJECT --account $ACCOUNT --bucket $BUCKET
then
    echo "BIG QUERY CDR Data Generated"
else
    echo "FAILED To Generate BIG QUERY Data For CDR $CDR_VERSION"
    exit 1
fi

# Init the local cdr database
# Init the db to fresh state ready for new cdr data keeping schema and certain tables
# Todo after bq data generation complete and csvs made
# echo "Initializing new cdr db"
#if ./generate-cdr/init-new-cdr-db.sh --cdr-version $CDR_VERSION
#then
#    echo "Local MYSQL CDR Initialized"
#else
#    echo "Local MYSQL CDR failed to initialize"
#    exit 1
#fi