#!/bin/bash

# Imports all sql and csv files in the bucket to the database.
# IF --create-dump-file is specified, it is ran first

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/cloudsql-import-bucket.sh --project <PROJECT> --instance <INSTANCE> --bucket <BUCKET>"
# example account for test : all-of-us-workbench-test@appspot.gserviceaccount.com
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --database) DATABASE=$2; shift 2;;
    --create-dump-file) CREATE_DUMP_FILE=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${INSTANCE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Grant Access to files for service account
SERVICE_ACCOUNT="${PROJECT}@appspot.gserviceaccount.com"

gsutil acl ch -u $SERVICE_ACCOUNT:O gs://$BUCKET/*
gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=$(gcloud sql instances describe --project $PROJECT \
    --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
    | cut -d: -f2)
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $BUCKET/*"
# Note, this only applies to files already existing in the bucket.
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R gs://$BUCKET/*


# Create db from dump file if we need to
if [ -z "${CREATE_DUMP_FILE}" ]
then
    echo "Creating cloudsql DB from dump file ${CREATE_DUMP_FILE}"
    gcloud sql instances import --project $PROJECT --account $SERVICE_ACCOUNT $INSTANCE gs://$BUCKET/$SQL_DUMP_FILE
    then
        echo "Success"
    else
        echo "Fail"
        exit 1
    fi
fi

#Import files
gsutil ls gs://$BUCKET



# Add tables names of files to import here
#TABLES=(achilles_analysis achilles_results achilles_results_dist db_domain domain vocabulary criteria criteria_attribute concept concept_relationship concept_ancestor concept_synonym)





