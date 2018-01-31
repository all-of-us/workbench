#!/bin/bash

# Local or remote  mysql databases named cdr<cdr-version> and public<cdr-version> are created and populated
# with data from specified bucket

# Example usage, you need to provide a bunch of args
# Provide:  your authorized gcloud account
#  bq project and dataset where the omop CDR release is
#  the workbench-project you want the new dataset  to be generated in
#  the cdr release number -- YYYYMMDD format . This is used to name generated datasets
#  the gcs bucket you want to put the generated data in
#
# ./project.rb generate-bigquery-cloudsql-cdr --account peter.speltz@pmi-ops.org --bq-project all-of-us-ehr-dev \
# --bq-dataset test_merge_dec26 --workbench-project all-of-us-workbench-test --cdr-version 20180130 --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-cloudsql-cdr --account <ACCOUNT> --cdr-version YYYYMMDD --bucket <BUCKET>"
USAGE="$USAGE \n Local mysql or remote cloudsql database named cdr<cdr-version> and public<cdr-version> are created and populated."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

# Todo test if this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
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
if [[ $CDR_VERSION =~ ^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

# Init the local cdr database
# Init the db to fresh state ready for new cdr data keeping schema and certain tables
echo "Initializing new cdr db"
if ./generate-cdr/init-new-cdr-db.sh --cdr-version $CDR_VERSION
then
  echo "Local MYSQL CDR Initialized"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi

# Import the gcs data
echo "Initializing new cdr db"
if ./generate-cdr/import-gcs-data.sh --account $ACCOUNT --bucket $BUCKET --cdr-version $CDR_VERSION
then
  echo "Imported data to local database"
else
  echo "Local MYSQL CDR failed to initialize"
  exit 1
fi
