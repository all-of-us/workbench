#!/bin/bash

# Local or remote  mysql databases named cdr<cdr-version> and public<cdr-version> are created and populated
# with data from specified bucket

# Example usage, you need to provide a bunch of args
# Provide:
# your authorized gcloud account
# the cdr release number -- YYYYMMDD format
# cdr-db-prefix -- cdr or public usually
# bucket -- where the csvs are to import into the db
# Example
# ../project.rb generate-cloudsql-cdr --account peter.speltz@pmi-ops.org --cdr-version 20180130 --cdr-db-prefix cdr --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-cloudsql-cdr --account <ACCOUNT> --cdr-version YYYYMMDD --cdr-db-prefix <cdr|public> --bucket <BUCKET>"
USAGE="$USAGE \n Local mysql or remote cloudsql database named cdr<cdr-version> and public<cdr-version> are created and populated."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --cdr-db-prefix) CDR_DB_PREFIX=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

# Todo why does this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

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
export CDR_DB_NAME=${CDR_DB_PREFIX}${CDR_VERSION}

startDate=`date`
echo "Starting generate-local-cdr-db $startDate\n"

# Init the local cdr database
echo "Initializing new cdr db $CDR_DB_NAME"
if ./generate-cdr/init-new-cdr-db.sh --cdr-db-name $CDR_DB_NAME
then
  echo "Local MYSQL CDR Initialized"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

# Import the gcs data
echo "Initializing new cdr db"
if ./generate-cdr/import-gcs-data.sh --account $ACCOUNT --bucket $BUCKET --cdr-db-name $CDR_DB_NAME
then
  echo "Imported data to local database $CDR_DB_NAME"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

stopDate=`date`
echo "Start $startDate Stop: $stopDate"
echo "Finished generate-local-cdr-db \n"

