#!/bin/bash

# This generates new cloudsql database for a cdr
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional

USAGE="./generate-cdr/generate-clousql-cdr --project <PROJECT> --account <ACCOUNT> --cdr-version=YYYYMMDD"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_VERSION}" ]
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

CREDS_ACCOUNT=${ACCOUNT}


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

# Make big query dbs and Run big queries
echo "Making big query dataset for cloudsql cdr"
if ./generate-cdr/make-bq-data.sh --project $PROJECT --account $ACCOUNT --cdr-version $CDR_VERSION
then
    echo "BIG QUERY CDR Data Generated"
else
    echo "FAILED To Generate BIG QUERY Data For CDR $CDR_VERSION"
    exit 1
fi

