#!/bin/bash

# Imports sql and csv files in a bucket to a cloudsql database.
# IF --create-db-sql-file is specified, it is ran first to create the database
# IF --file is specified, it is ran next after create-db-sql-file and then it quits.
# *Note IF you want to import a single file only, always use the --file option. Even if it is an entire db dump file.
# Otherwise it will try to import the rest of the files in the bucket.
# IF --file is NOT specified, it carries on running sql files first, then csv files
# Table names for csvs are determined from the filename.
# The files can be gzipped or not. gcloud sql import does not care either way.

set -xeuo pipefail
IFS=$'\n\t'

CREATE_DB_SQL_FILE=
FILE=

# get options
USAGE="./generate-clousql-cdr/cloudsql-import.sh --project <PROJECT> --instance <INSTANCE> --bucket <BUCKET> \
--database <database> [--create-db-sql-file <filename.sql>] [--file <just_import_me_filename>]"
# example account for test : all-of-us-workbench-test@appspot.gserviceaccount.com
while [ $# -gt 0 ]; do
  echo "$1 in 1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --database) DATABASE=$2; shift 2;;
    --create-db-sql-file) CREATE_DB_SQL_FILE=$2; shift 2;;
    --file) FILE=$2; shift 2;;
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

if [ -z "${DATABASE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Grant Access to files for service account
# TODO -- if they pass just one file do we just grant access it it ? Not sure it is worth it to do that
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

# Create db from sql file if specified
create_gs_file=gs://
if [ "${CREATE_DB_SQL_FILE}" ]
then
    create_gs_file=$create_gs_file$BUCKET/$CREATE_DB_SQL_FILE
    echo "Creating cloudsql DB from sql file ${CREATE_DB_SQL_FILE}"
    gcloud sql import sql $INSTANCE $create_gs_file --project $PROJECT --database=$DATABASE \
        --account $SERVICE_ACCOUNT --quiet
    # If above fails let it die as user obviously intended to create the db
    gsutil mv $create_gs_file gs://$BUCKET/imported_to_cloudsql/
fi

#Import sql and csv files, do sql first as they are intended for schema changes and such
# If file argument passed we just import that one
sqls=()
csvs=()

if [ "${FILE}" ]
then
    if [[ $FILE =~ *.sql ]]
    then
        sqls[0]=$FILE
    elif [[ $FILE =~ *.csv ]]
    then
        csvs[0]=$FILE
    else
        echo "Error. Cannot import file, $FILE,  that is not a .sql or .csv"
        exit 1;
    fi
else
    # gsutil returns error if no files match thus the "2> /dev/null || true" part
    sqls=( $(gsutil ls gs://$BUCKET/*.sql* 2> /dev/null || true))
    csvs=( $(gsutil ls gs://$BUCKET/*.csv* 2> /dev/null || true))
fi

for gs_file in "${sqls[@]}"
do
    if [ "$gs_file" = "$create_gs_file" ]
    then
        echo "Skipping create dump file ";
    else
        gcloud sql import sql $INSTANCE $gs_file --project $PROJECT --account $SERVICE_ACCOUNT --quiet --async
        echo "Import started, waiting for it to complete."
        seconds_waited=0
        wait_interval=15
        while true; do
            sleep $wait_interval
            seconds_waited=$((seconds_waited + wait_interval))
            import_status=
            if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT.*RUNNING") ]]
            then
                echo "Import is still running after ${seconds_waited} seconds."
            else
                echo "Import finished after ${seconds_waited} seconds."
                # Move file to imported dir
                gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
                break
            fi
        done
    fi
done

for gs_file in "${csvs[@]}"
do
   # Get table name from file, It is everything before the first '.'
   filename="${gs_file##*/}"
   table=${filename%%.*}
   if [[ $table ]]
   then
        echo "Importing file into $table"
        gcloud sql import csv $INSTANCE $gs_file --project $PROJECT --quiet --account $SERVICE_ACCOUNT \
        --database=$DATABASE --table=$table --async
        echo "Import started, waiting for it to complete."
        seconds_waited=0
        wait_interval=15
        while true; do
            sleep $wait_interval
            seconds_waited=$((seconds_waited + wait_interval))
            import_status=
            if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT.*RUNNING") ]]
            then
                echo "Import is still running after ${seconds_waited} seconds."
            else
                echo "Import finished after ${seconds_waited} seconds."
                # Move file to imported dir
                gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
                break
            fi
        done
   else
        echo "Unable to parse table from $filename. Skipping it."
   fi
done




