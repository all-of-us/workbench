#!/bin/bash

# This imports a mysqldump from a bucket to your local mysql.

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-clousql-cdr/cloudsql-import.sh --project <PROJECT> --sql-dump-file <cdrYYYYMMDD.sql> --bucket <BUCKET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --sql-dump-file) SQL_DUMP_FILE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${SQL_DUMP_FILE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

echo "Creating local mysql DB from dump file $SQL_DUMP_FILE \n"
SERVICE_ACCOUNT="${PROJECT}@appspot.gserviceaccount.com"

gsutil acl ch -u $SERVICE_ACCOUNT:O gs://$BUCKET/$SQL_DUMP_FILE
gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $PROJECT \
    --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
    | cut -d: -f2`
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $BUCKET/$SQL_DUMP_FILE"
# Note, this only applies to files already existing in the bucket.
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R gs://$BUCKET/$SQL_DUMP_FILE

# Make a dir for the csvs
local_fpath=/tmp/$SQL_DUMP_FILE
rm -rf $local_fpath

# Download data
gsutil -m cp gs://$BUCKET/$SQL_DUMP_FILE $local_fpath

# Import dump
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < $local_fpath

echo "Import complete"