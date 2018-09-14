#!/bin/bash

# Imports all sql and csv files in the bucket to the database.
# IF --create-dump-file is specified, it is ran first

set -xeuo pipefail
IFS=$'\n\t'

CREATE_DUMP_FILE=

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

create_gs_file=gs://
# Create db from dump file if we need to
if [ "${CREATE_DUMP_FILE}" ]
then
    create_gs_file=$create_gs_file$BUCKET/$CREATE_DUMP_FILE
    echo "Creating cloudsql DB from dump file ${CREATE_DUMP_FILE}"
    gcloud sql import sql --project $PROJECT --quiet --account $SERVICE_ACCOUNT $INSTANCE $create_gs_file
    # If above fails let it die as user obviously intended to create the db
fi

#Import files, do sql first as they may have some schema changes
# gsutil returns error if no files match thus the "2> /dev/null || true" part
sqls=( $(gsutil ls gs://$BUCKET/*.sql* 2> /dev/null || true))

for gs_file in "${sqls[@]}"
do


    if [ "$gs_file" = "$create_gs_file" ]
    then
        echo "Skipping create dump file ";
    else
        # Note big sql files might error out here. These are intended for schema changes and small things
        gcloud sql import sql --project $PROJECT --quiet --account $SERVICE_ACCOUNT $INSTANCE $gs_file
    fi
done

csvs=( $(gsutil ls gs://$BUCKET/*.csv* 2> /dev/null || true))
for gs_file in "${csvs[@]}"
do
   # Get table name from file, Table name can only contain letters, numbers, -, _
   filename="${gs_file##*/}"
   table=
   if [[ $filename =~ ([[:alnum:]_-]*) ]]
   then
        # Strip extension and the 00000* digits in case big tables were dumped into multiple files
        table=${BASH_REMATCH[1]}
        table=${table%%[0-9]*}
        echo "Importing file into $table"
        gcloud sql import csv $INSTANCE $gs_file --project $PROJECT --quiet --account $SERVICE_ACCOUNT \
        --database=$DATABASE --table=$table --async
        echo "Import started, waiting for it to complete."
        minutes_waited=0
        while true; do
            sleep 1m
            minutes_waited=$((minutes_waited + 1))
            import_status=$(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT")
            if [[ $import_status =~ .*RUNNING* ]]
            then
                echo "Import is still running after ${minutes_waited} minutes."
            else
                echo "Import finished after ${minutes_waited} minutes."
                break
            fi
        done
   else
        echo "Unable to parse table from $filename. Skipping it."
   fi


done




