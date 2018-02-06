#!/bin/bash

# Local  mysql databases named cdr<cdr-version> and public<cdr-version> are created and populated
# with data from specified bucket.

# Example usage, you need to provide a bunch of args
# ./project.rb generate-local-count-dbs --cdr-version 20180130 --bucket all-of-us-workbench-cloudsql-create

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-cdr/generate-local-count-dbs.sh --cdr-version <''|YYYYMMDD> --bucket <BUCKET>"
USAGE="$USAGE \n Creates local mysql  database named cdr<cdr-version> and public<cdr-version> populated with data from bucket."

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --bucket) BUCKET=$2; shift;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

startDate=`date`
echo " Starting generate-local-count-dbs $startDate"

#Check cdr_version is of form YYYYMMDD
if [[ $CDR_VERSION =~ ^$|^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[1-2][0-9]|3[0-1])$ ]]; then
    echo "New CDR VERSION will be $CDR_VERSION"
  else
    echo "CDR Version doesn't match required format YYYYMMDD"
    echo "Usage: $USAGE"
    exit 1
fi

# Init the db to fresh state ready for new cdr data keeping schema and certain tables
echo "Doing private count data"
if ./generate-cdr/generate-local-cdr-db.sh  --cdr-version "$CDR_VERSION" --cdr-db-prefix cdr --bucket $BUCKET
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

stopDate=`date`
echo "Start $startDate Stop: $stopDate"
echo `date` " Finished generate-local-count-dbs "
