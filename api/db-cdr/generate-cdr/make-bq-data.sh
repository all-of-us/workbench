#!/bin/bash

# This generates big query data that gets put in cloudsql
# Counts and teh public and cdr data four cloudsql
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase
# note  the account must be authorized to perform gcloud and bq operations

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional

USAGE="./generate-clousql-cdr/make-bq-data.sh --project <PROJECT> --account <ACCOUNT> --cdr-version=YYYYMMDD"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
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

if [ -z "${CDR_VERSION}" ]
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

CREDS_ACCOUNT=${ACCOUNT}

# Variables

# TODO make this agument for cdr_vession
bqcdr="test_merge_dec26"

bq_new_cdr_dataset=cloudsql_cdr$CDR_VERSION
bq_new_public_dataset=cloudsql_public$CDR_VERSION

gcs_bucket=gs://all-of-us-workbench-cdr$CDR_VERSION

# Make dataset for cloudsql tables
bq --project=$PROJECT mk $bq_new_cdr_dataset
bq --project=$PROJECT mk $bq_new_public_dataset

# Make data
# Make the vocabulary table from cdr with no changes
#bq --project=all-of-us-ehr-dev cp test_merge_dec26.vocabulary test_vocabulary_ppi.vocabulary
