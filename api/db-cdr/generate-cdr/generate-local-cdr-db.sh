#!/bin/bash

# This is called to make a local  mysql database  named  $cdr-db-prefix$cdr-version from a bucket of csvs
# It is called twice from generate-local-count-dbs.sh to make the cdr and public mysql dbs the workbench uses

# Example
# ../project.rb generate-cloudsql-cdr --cdr-version 20180130 --cdr-db-prefix cdr --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-cloudsql-cdr --cdr-version YYYYMMDD --cdr-db-prefix <cdr|public> --bucket <BUCKET>"
USAGE="$USAGE \n Local mysql or remote cloudsql database named cdr<cdr-version> and public<cdr-version> are created and populated."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --cdr-db-prefix) CDR_DB_PREFIX=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

# Todo why does this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${CDR_DB_PREFIX}" ]
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
if [[ $CDR_VERSION =~ ^$|^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

# Export CDR_DB_NAME for all scripts
CDR_DB_NAME=${CDR_DB_PREFIX}${CDR_VERSION}

startDate=`date`
echo "Starting generate-local-cdr-db $startDate\n"

# Init the local cdr database
echo "Initializing new cdr db $CDR_DB_NAME"
if ./generate-cdr/init-new-cdr-db.sh --drop-if-exists --cdr-db-name $CDR_DB_NAME
then
  echo "Local MYSQL CDR Initialized"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

# Import the gcs data
echo "Initializing new cdr db"
if ./generate-cdr/import-gcs-data.sh --bucket $BUCKET --cdr-db-name $CDR_DB_NAME
then
  echo "Imported data to local database $CDR_DB_NAME"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

stopDate=`date`
echo "Start $startDate Stop: $stopDate"
echo "Finished generate-local-cdr-db \n"

