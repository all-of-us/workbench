#!/bin/bash

# Imports sql and csv files in a bucket to a cloudsql database.
# IF --create-db-sql-file is specified, it is ran first to create the database
# IF --file is specified, it is ran next after create-db-sql-file and then it quits.
# IF the above are provided and do not exist it errors out.
# IF you want to import a single file only,  use the --file option or make sure it is the only file in the bucket.
# Table names for csvs are determined from the filename.
# The files can be gzipped or not.

set -xeuo pipefail
IFS=$'\n\t'

CREATE_DB_SQL_FILE=
FILE=

# get options
USAGE="./generate-clousql-cdr/cloudsql-import.sh --project <PROJECT> --instance <INSTANCE> --bucket <BUCKET> \
--database <database> [--create-db-sql-file <filename.sql>] [--file <just_import_me_filename>]"
# example account for test : all-of-us-workbench-test@appspot.gserviceaccount.com
while [ $# -gt 0 ]; do
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

# Function for waiting on import to finish.
# import_wait($file, $seconds_wait_interval)
function import_wait () {
  gs_file=$1
  wait_interval=$2
  seconds_waited=0
  while true; do
    sleep $wait_interval
    seconds_waited=$((seconds_waited + wait_interval))
    if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT.*RUNNING") ]]
    then
        echo "Import of $gs_file is still running after ${seconds_waited} seconds."
    else
        echo "Import of $gs_file finished after ${seconds_waited} seconds."
        break
    fi
  done
}

# Function to Grant Access to files for service account and db
function grant_access_to_files () {
  bucket_path=$1
  SERVICE_ACCOUNT="${PROJECT}@appspot.gserviceaccount.com"

  gsutil acl ch -u $SERVICE_ACCOUNT:O $bucket_path 2> /dev/null || true # Don't error if there are no files
  gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

  # Grant access to buckets for service account for cloudsql
  SQL_SERVICE_ACCOUNT=$(gcloud sql instances describe --project $PROJECT \
      --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
      | cut -d: -f2)
  # Trim leading whitespace from sql service account
  SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

  echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to $bucket_path"
  # Note, this only applies to files already existing in the bucket.
  gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R $bucket_path 2> /dev/null || true # Don't error if no files
}



# Create db from sql file if specified
create_gs_file=gs://
if [ "${CREATE_DB_SQL_FILE}" ]
then
    create_gs_file=$create_gs_file$BUCKET/$CREATE_DB_SQL_FILE
    grant_access_to_files $create_gs_file
    echo "Creating cloudsql DB from sql file ${CREATE_DB_SQL_FILE}"
    gcloud sql import sql $INSTANCE $create_gs_file --project $PROJECT  \
        --account $SERVICE_ACCOUNT --quiet --async
    import_wait $create_gs_file 15
    # If above fails let it die as user obviously intended to create the db
    gsutil mv $create_gs_file gs://$BUCKET/imported_to_cloudsql/
fi

#Import sql and csv files, do sql first as they are intended for schema changes and such
# If file argument passed we just import that one
sqls=()
csvs=()

if [ "${FILE}" ]
then
  # Just grant access to this file
  gs_file=gs://$BUCKET/$FILE
  grant_access_to_files $gs_file
  if [[ $FILE =~ \.sql.* ]]
  then
    sqls[0]=$gs_file
  elif [[ $FILE =~ \.csv.* ]]
  then
    csvs[0]=$gs_file
  else
    echo "Error. Cannot import file, $FILE,  that is not a .sql or .csv"
    exit 1;
  fi
else
    grant_access_to_files gs://$BUCKET/*
    # gsutil returns error if no files match thus the "2> /dev/null || true" part to ignore error
    sqls=( $(gsutil ls gs://$BUCKET/*.sql* 2> /dev/null || true))
    csvs=( $(gsutil ls gs://$BUCKET/*.csv* 2> /dev/null || true))
fi

for gs_file in "${sqls[@]}"
do
    if [ "$gs_file" = "$create_gs_file" ]
    then
        echo "Skipping create db sql file ";
    else
        # Don't pass database to sql import . Use use <db> inside sql.
        gcloud sql import sql $INSTANCE $gs_file --project $PROJECT --account $SERVICE_ACCOUNT --quiet --async
        import_wait $gs_file 15
        # Move file to imported dir
        gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
    fi
done

for gs_file in "${csvs[@]}"
do
   # Get table name from file
   filename="${gs_file##*/}"  # gets everything after last /
   table=${filename%%.*}      # truncates everything starting with first .
   if [[ $table ]]
   then
        echo "Importing file into $table. If table does not exist in the database, this file is moved to imported."
        gcloud sql import csv $INSTANCE $gs_file --project $PROJECT --quiet --account $SERVICE_ACCOUNT \
        --database=$DATABASE --table=$table --async
        import_wait $gs_file 15
        # Move file to imported dir
        gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
   else
        echo "Unable to parse table from $filename. Skipping it."
   fi
done




