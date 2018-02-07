#!/bin/bash

# This makes a mysqldump of the cdr database specified and copies it to a gcs bucket
# ACCOUNT must be authorized with gcloud auth login previously

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/make-cloudsql-db.sh --instance <INSTANCE> --sql-dump-file <cdrYYYYMMDD.sql> --db-name <DBNAME> --bucket <BUCKET>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --instance) INSTANCE=$2; shift 2;;
    --sql-dump-file) SQL_DUMP_FILE=$2; shift 2;;
    --db-name) DB_NAME=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${INSTANCE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${SQL_DUMP_FILE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${DB_NAME}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


startDate=`date`
echo "Creating cloudsql $DB_NAME from dump file $SQL_DUMP_FILE \n"


SERVICE_ACCOUNT=all-of-us-workbench-test@appspot.gserviceaccount.com
PROJECT=$WORKBENCH_PROJECT

gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=`gcloud sql instances describe --project $PROJECT \
    --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
    | cut -d: -f2`
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket $BUCKET/*.sql..."
# Note, this only applies to files already existing in the bucket.
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R gs://$BUCKET/*.sql


gcloud sql instances import --project $PROJECT --account $SERVICE_ACCOUNT $INSTANCE gs://$BUCKET/$SQL_DUMP_FILE



stopDate=`date`

echo "Finished creating $DB_NAME on $INSTANCE \n"
echo "Start $startDate Stop: $stopDate"