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
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
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

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

echo "Creating cloudsql DB from dump file $SQL_DUMP_FILE \n"
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

# Import asynch
gcloud sql instances import --project $PROJECT --account $SERVICE_ACCOUNT $INSTANCE gs://$BUCKET/$SQL_DUMP_FILE --async

echo "Import started, waiting for it to complete."
echo "You can also kill this script and check status at at https://console.cloud.google.com/sql/instances/${INSTANCE}/operations?project=${PROJECT}."
minutes_waited=0
while true; do
  sleep 1m
  minutes_waited=$((minutes_waited + 1))
  import_status=`gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT"`
  if [[ $import_status =~ .*RUNNING* ]]
  then
     echo "Import is still running after ${minutes_waited} minutes."
  else
    echo "Import finished after ${minutes_waited} minutes."
    break
  fi
done
