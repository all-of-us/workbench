#!/bin/bash

# Creates cloudsql count db from data in bucket

# Example usage, you need to provide a bunch of args
# ./project.rb generate-cloudsql-dbs --cdr-version 20180130 --bucket all-of-us-workbench-private

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-cloudsql-db.sh --project <PROJECT> --instance <INSTANCE> --database <cdrYYYYMMDD> --bucket <BUCKET> "
USAGE="$USAGE \n Creates cloudsql count db from data in bucket"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --database) DATABASE=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
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

if [ -z "${DATABASE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# TODO -- maybe check database doesn't already exist

startDate=$(date)
echo " Starting generate-cloudsql-db $DATABASE from bucket $BUCKET $startDate"


# Init the local database
echo "Initializing new  $DATABASE"
if ./generate-cdr/init-new-cdr-db.sh --drop-if-exists Y --cdr-db-name $DATABASE
then
  echo "Local MYSQL CDR Initialized"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

# Make empty sql dump
if ./generate-clousql-cdr/make-mysqldump.sh --db-name $DATABASE --bucket $BUCKET
then
  echo "Success"
else
  echo "Error"
  exit 1
fi

# Import Sql dump and data in bucket to cloudsql


# Init the db to fresh state ready for new cdr data keeping schema and certain tables
echo "Doing private count data"
if ./generate-cdr/cloudsql-import-bucket.sh --project $PROJECT --instance $INSTANCE --bucket $BUCKET --database $DATABASE --create-dump-file $DATABASE.sql
then
  echo "Success"
else
  echo "Failed"
  exit 1
fi

# Init the db to fresh state ready for new cdr data keeping schema and certain tables
echo "Doing public count data"
if ./generate-cdr/generate-local-cdr-db.sh --cdr-version "$CDR_VERSION" --cdr-db-prefix public --bucket $BUCKET
then
  echo "Success"
else
  echo "Failed"
  exit 1
fi

stopDate=$(date)
echo "Start $startDate Stop: $stopDate"
echo $(date) " Finished generate-local-count-dbs "
