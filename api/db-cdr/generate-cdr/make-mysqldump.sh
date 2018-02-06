#!/bin/bash

# This makes a mysqldump of the cdr database specified and copies it to a gcs bucket
# ACCOUNT must be authorized with gcloud auth login previously

set -xeuo pipefail
IFS=$'\n\t'


# get options
USAGE="./generate-clousql-cdr/make-mysqldump.sh --db-name <DBNAME> --bucket <BUCKET>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --db-name) DB_NAME=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

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

dump_path=/tmp/$DB_NAME.sql
startDate=`date`
echo "Dumping $DB_NAME to $BUCKET \n"

mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
    --add-drop-table --disable-keys --ignore-table=$DB_NAME.DATABASECHANGELOG \
    --ignore-table=$DB_NAME.DATABASECHANGELOGLOCK --databases \
    $DB_NAME  > $dump_path


gsutil cp $dump_path gs://$BUCKET/$DB_NAME.sql

rm $dump_path
stopDate=`date`

echo "Finished dumping $DB_NAME to $BUCKET \n"
echo "Start $startDate Stop: $stopDate"