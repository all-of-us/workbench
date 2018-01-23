#!/bin/bash

# This generates cloudsql  database from cdr dump
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


# Make data
# Make the vocabulary table from cdr with no changes
#bq --project=all-of-us-ehr-dev cp test_merge_dec26.vocabulary test_vocabulary_ppi.vocabulary


# Todo  maybe not hardcode service account name ?
#SERVICE_ACCOUNT=all-of-us-workbench-test@appspot.gserviceaccount.com
#gcloud auth activate-service-account $CREDS_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $project \
    --account $SERVICE_ACCOUNT $cloudsql_instance | grep serviceAccountEmailAddress \
    | cut -d: -f2`
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $gcs_bucket..."
# Note, this only applies to files already existing in the bucket . So must rerun after adding files
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:W $gcs_bucket
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R $gcs_bucket/*.sql

# Import the schema into the cloud sql instance
# Todo install gcloud beta for importing csv and sql . It is intended to replace sql instances import
# gcloud beta sql import sql $cloudsql_instance $gcs_bucket/$cdr_schema_file

#gcloud sql instances import --quiet --project $project  $cloudsql_instance $gcs_bucket/$cdr_schema_file

# Import hard data
#gcloud sql instances import --quiet --project $project  --database cdr $cloudsql_instance $gcs_bucket/$hard_data_file

