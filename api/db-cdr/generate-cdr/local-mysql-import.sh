#!/bin/bash

# This imports a mysqldump from a bucket to your local mysql.

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-clousql-cdr/local-mysql-import.sh --sql-dump-file <cdrYYYYMMDD.sql> --bucket <BUCKET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --sql-dump-file) SQL_DUMP_FILE=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

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

echo "Creating local mysql DB from dump file $SQL_DUMP_FILE"


# Make a dir for the csvs
local_fpath=/tmp/$SQL_DUMP_FILE
rm -rf $local_fpath

# Download data
gsutil -m cp gs://$BUCKET/$SQL_DUMP_FILE $local_fpath

# Import dump
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < $local_fpath

echo "Import complete"

