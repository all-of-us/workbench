#!/bin/bash

# This imports a csv file in gcs into a cloudsql table
# The csv can be gzipped or not. Provide the full bucket/path/csv-file path

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/cloudsql-import-csv.sh --project <PROJECT> --instance <INSTANCE>"
USAGE="$USAGE --gs-csv <bucket/path/concept.csv.gz> --database <cdrXXX> --table <concept>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --project) PROJECT=$2; shift 2;;
    --instance) INSTANCE=$2; shift 2;;
    --gs-csv) GS_CSV=$2; shift 2;;
    --database) DATABASE=$2; shift 2;;
    --table) TABLE=$2; shift 2;;
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

if [ -z "${GS_CSV}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${DATABASE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${TABLE}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# Grant Access to file for service account
SERVICE_ACCOUNT="${PROJECT}@appspot.gserviceaccount.com"

gsutil acl ch -u $SERVICE_ACCOUNT:O gs://$GS_CSV
gcloud auth activate-service-account $SERVICE_ACCOUNT --key-file=$GOOGLE_APPLICATION_CREDENTIALS

# Grant access to buckets for service account for cloudsql
SQL_SERVICE_ACCOUNT=$(gcloud sql instances describe --project $PROJECT \
    --account $SERVICE_ACCOUNT $INSTANCE | grep serviceAccountEmailAddress \
    | cut -d: -f2)
# Trim leading whitespace from sql service account
SQL_SERVICE_ACCOUNT=${SQL_SERVICE_ACCOUNT//[[:blank:]]/}

echo "Granting GCS access to ${SQL_SERVICE_ACCOUNT} to bucket file $GS_CSV"
# Note, this only applies to files already existing in the bucket.
gsutil acl ch -u ${SQL_SERVICE_ACCOUNT}:R gs://$GS_CSV

# Import asynch

startDate=$(date)
echo $(date) " Starting Import of $GS_CSV into $INSTANCE $DATABASE.$TABLE at $startDate"

# Need recent cloud sdk 216.0.0
gcloud sql import csv $INSTANCE gs://$GS_CSV --project $PROJECT --database=$DATABASE --table=$TABLE

stopDate=$(date)


#echo "Import started, waiting for it to complete."
#echo "You can also kill this script and check status at at https://console.cloud.google.com/sql/instances/${INSTANCE}/operations?project=${PROJECT}."
#minutes_waited=0
#while true; do
# sleep 1m
#  minutes_waited=$((minutes_waited + 1))
#  import_status=$(gcloud sql operations list --instance $INSTANCE --project $PROJECT | grep "IMPORT")
#  if [[ $import_status =~ .*RUNNING* ]]
#  then
#     echo "Import is still running after ${minutes_waited} minutes."
#  else
#    echo "Import finished after ${minutes_waited} minutes."
#    break
#  fi
#done
echo "Start $startDate Stop: $stopDate"