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
    --account) ACCOUNT=$2; shift 2;;
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --sql-dump-file) SQL_DUMP_FILE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
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
# SERVICE_ACCOUNT=all-of-us-workbench-test@appspot.gserviceaccount.com
SERVICE_ACCOUNT=$ACCOUNT

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
echo "Importing asynch. A 3 GB dump takes about 1 hr. When this is done you can run this command again to import another database."
echo "Check status at https://console.cloud.google.com/sql/instances/"$INSTANCE"/operations?project="$PROJECT

