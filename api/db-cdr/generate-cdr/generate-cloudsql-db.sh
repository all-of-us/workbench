#!/bin/bash

# Creates cloudsql count db from data in bucket

# Example usage, you need to provide a bunch of args
# ./project.rb generate-cloudsql-dbs --cdr-version 20180130 --bucket all-of-us-workbench-private

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-cloudsql-db.sh --project <PROJECT> --instance <INSTANCE> --database <cdrYYYYMMDD> \
--bucket <BUCKET>"

while [ $# -gt 0 ]; do
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
  echo $USAGE
  exit 1
fi

if [ -z "${INSTANCE}" ]
then
  echo $USAGE
  exit 1
fi

if [ -z "${DATABASE}" ]
then
  echo $USAGE
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo $USAGE
  exit 1
fi

if [[ ${BUCKET} =~ .*public.* ]]
then
  VERSION_FLAG="public"
else
  VERSION_FLAG="cdr"
fi

startDate=$(date)
echo " Starting generate-cloudsql-db $DATABASE from bucket $BUCKET $startDate"


# Init the local database
echo "Initializing new  $DATABASE"
if ./generate-cdr/init-new-cdr-db.sh --drop-if-exists --cdr-db-name ${DATABASE} --version-flag ${VERSION_FLAG}
then
  echo "Success"
else
  echo "Failed"
  exit 1
fi

# Make empty sql dump
if ./generate-cdr/make-mysqldump.sh --db-name $DATABASE --bucket $BUCKET
then
  echo "Success"
else
  echo "Failed"
  exit 1
fi

# Import Sql dump and data in bucket to cloudsql
if ./generate-cdr/cloudsql-import.sh --project $PROJECT --instance $INSTANCE --bucket $BUCKET \
    --database $DATABASE --create-db-sql-file $DATABASE.sql
then
  echo "Success"
else
  echo "Failed"
  exit 1
fi
stopDate=$(date)
echo "Start $startDate Stop: $stopDate"
echo $(date) " Finished generate-clousdsql-db "
