#!/bin/bash

# This generates new mysql database for a cdr version

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./init-new-cdr-db.sh [--drop-if-exists] --cdr-db-name cdrYYYYMMDD>"
DROP_IF_EXISTS="N"
RUN_LIST="schema"
CONTEXT=

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --cdr-db-name) CDR_DB_NAME=$2; shift 2;;
    --drop-if-exists) DROP_IF_EXISTS="Y"; shift 1;;
    --run-list) RUN_LIST=$2; shift 2;;
    --context) CONTEXT="-Pcontexts=$2"; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

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

envsubst < "$(dirname "${BASH_SOURCE}")/create_db.sql" > $CREATE_DB_FILE

function run_mysql() {
  if [ -f /.dockerenv ]; then
    mysql $@
  else
    echo "Outside docker: invoking mysql via docker for portability"
    docker run -i --rm --network host --entrypoint '' \
      mariadb:10.11.8 \
      mysql $@
  fi
}

# Drop and create new cdr database
if [ "${DROP_IF_EXISTS}" == "Y" ]
then
  echo "Dropping database $CDR_DB_NAME"
  run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}" -e "drop database if exists ${CDR_DB_NAME}"
fi
echo "Creating database ..."
cat "${CREATE_DB_FILE}" | run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}"

if [ "${RUN_LIST}" == "data" ]
then
    echo "Copying csv files from gs://all-of-us-cb-test-cs"
    # make sure csv folder is empty
    rm -rf "$(dirname "${BASH_SOURCE}")/../csv"
    mkdir "$(dirname "${BASH_SOURCE}")/../csv"
    # copy down csv files from bucket
    gsutil -m -o "GSUtil:parallel_process_count=1" cp gs://all-of-us-cb-test-csv/*.csv "$(dirname "${BASH_SOURCE}")/../csv"
fi

# Use liquibase to generate the schema and data
echo "Running liquibase "
(cd "$(dirname "${BASH_SOURCE}")/.." && ../gradlew update -PrunList=${RUN_LIST} ${CONTEXT})

if [ "${RUN_LIST}" == "data" ]
then
    rm -rf "$(dirname "${BASH_SOURCE}")/../csv"
fi

# Success
exit 0
