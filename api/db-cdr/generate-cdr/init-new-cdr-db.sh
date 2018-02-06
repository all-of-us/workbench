#!/bin/bash

# This generates new mysql database for a cdr version

set -xeuo pipefail
IFS=$'\n\t'

# Todo maybe have arg be cdr-db-name?
USAGE="./init-new-cdr-db.sh --cdr-db-name <cdrYYYYMMDD|publicYYYYMMDD>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --db-name) CDR_DB_NAME=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo check versions are of form YYYYMMDD
if [ -z "${CDR_DB_NAME}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# export for liquibase to use this
export CDR_DB_NAME

CREATE_DB_FILE=/tmp/create_db.sql

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

cat create_db.sql | envsubst > $CREATE_DB_FILE

# Drop and create new cdr database
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "drop database if exists $CDR_DB_NAME"
echo "Creating database ..."
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${CREATE_DB_FILE}

# Use liquibase to generate the schema and data
echo "Running liquibase "
../gradlew update -PrunList=schema

# Success
exit 0