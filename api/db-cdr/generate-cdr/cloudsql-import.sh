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
USAGE="./generate-clousql-cdr/cloudsql-import.sh --project <PROJECT> --instance <INSTANCE> --database <database> \
--bucket <BUCKET> [--create-db-sql-file <filename.sql>] [--file <just_import_me_filename>]"
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
  local gs_file=$1
  local wait_interval=$2
  # Sleep an initial 5 seconds before checking for small files to import
  sleep 5
  seconds_waited=5
  while true; do
    if [[ $(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep ".*RUNNING") ]]
    then
        sleep $wait_interval
        seconds_waited=$((seconds_waited + wait_interval))
    else
        echo "Operation $gs_file finished after ${seconds_waited} seconds."
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

# Function to check database exists
# if [ $(db_exists($DATABASE)) == 1 ]; then echo "database exists"; fi
function db_exists () {
  local db_name=$1
  local db=$(gcloud sql databases list -i $INSTANCE --project $PROJECT --filter="NAME = $db_name" \
--format="csv [no-heading] (NAME)")
  if [ "$db" == "$db_name" ]
  then
    echo 1
  else
    echo 0;
  fi
}


# Create db from sql file if specified
create_gs_file=gs://
if [ "${CREATE_DB_SQL_FILE}" ]
then
    create_gs_file=$create_gs_file$BUCKET/$CREATE_DB_SQL_FILE
    grant_access_to_files $create_gs_file
    echo "Creating cloudsql DB from sql file ${CREATE_DB_SQL_FILE}"

    # Wait on any running job to finish before starting.
    import_wait "any job" 10

    gcloud sql import sql $INSTANCE $create_gs_file --project $PROJECT  \
        --account $SERVICE_ACCOUNT --quiet --async
    import_wait $create_gs_file 10

    if [ $(db_exists $DATABASE) == 1 ]
    then
      echo "$DATABASE created successfully"
      gsutil mv $create_gs_file gs://$BUCKET/imported_to_cloudsql/
    else
      echo "Error importing create sql. "
      exit 1
    fi
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
    sqls=( $(gsutil ls gs://$BUCKET/*.sql* 2> /dev/null || true) )
    csvs=( $(gsutil ls gs://$BUCKET/*.csv* 2> /dev/null || true) )
fi

for gs_file in "${sqls[@]}"
do
    if [ "$gs_file" = "$create_gs_file" ]
    then
        echo "Skipping create db sql file ";
    else
        # Wait on any running job to finish before starting. Sometimes a backup or other operation starts
        # between iterations of this loop
        import_wait "any job" 10

        # Don't pass database to sql import . Use use <db> inside sql.
        gcloud sql import sql $INSTANCE $gs_file --project $PROJECT --account $SERVICE_ACCOUNT --quiet --async

        # Wait for this file to import
        import_wait $gs_file 10

        # Move file to imported dir
        gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
    fi
done

# Check database exists before importing csvs as they need it.
# It may have been created in sql imports above.
# Even if we don't have any csv's we check it exists to ensure sql above ran
if [ $(db_exists ${DATABASE}) == 1 ]
then
      echo "$DATABASE exists. Carrying on."
else
      echo "Error. $DATABASE database does not exist."
      exit 1
fi

for gs_file in "${csvs[@]}"
do
   # Get table name from file
   filename="${gs_file##*/}"  # gets everything after last /
   table=${filename%%.*}      # truncates everything starting with first .
   if [[ $table ]]
   then
        # Wait on any running job to finish before starting.
        import_wait "any job" 10

        echo "Importing file into $table. If table does not exist in the database, this file is moved to imported."
        gcloud sql import csv $INSTANCE $gs_file --project $PROJECT --quiet --account $SERVICE_ACCOUNT \
        --database=$DATABASE --table=$table --async

        # Wait for this file to import
        import_wait $gs_file 10

        # Move file to imported dir
        gsutil mv $gs_file gs://$BUCKET/imported_to_cloudsql/
   else
        echo "Unable to parse table from $filename. Skipping it."
   fi
done




