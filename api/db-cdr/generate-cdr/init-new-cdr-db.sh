#!/bin/bash

# This generates new mysql database for a cdr version

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./init-new-cdr-db.sh [--drop-if-exists] --cdr-db-name cdrYYYYMMDD|publicYYYYMMDD> --version-flag cdr|public"
DROP_IF_EXISTS="N"
VERSION_FLAG="cdr"
RUN_LIST="schema"
CONTEXT=

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --cdr-db-name) CDR_DB_NAME=$2; shift 2;;
    --drop-if-exists) DROP_IF_EXISTS="Y"; shift 1;;
    --version-flag) VERSION_FLAG=$2; shift 2;;
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
#export CDR_DB_NAME

# If version flag contains public, we want to the public_db_user env var substituted in the create_db.sql
if [[ $VERSION_FLAG =~ .*public.* ]]
then
  echo "Working the public db init cdr"
  export WORKBENCH_DB_USER=$PUBLIC_DB_USER
  export WORKBENCH_DB_PASSWORD=$PUBLIC_DB_PASSWORD
fi

CREATE_DB_FILE=/tmp/create_db.sql

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

envsubst < "$(dirname "${BASH_SOURCE}")/create_db.sql" > $CREATE_DB_FILE

# Drop and create new cdr database
if [ "${DROP_IF_EXISTS}" == "Y" ]
then
    echo "Dropping database $CDR_DB_NAME"
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "drop database if exists $CDR_DB_NAME"
fi
echo "Creating database ..."
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${CREATE_DB_FILE}

if [ "${RUN_LIST}" == "data" ]
then
    echo "Copying csv files from gs://all-of-us-cb-test-cs"
    # make sure csv folder is empty
    rm -rf "$(dirname "${BASH_SOURCE}")/../csv"
    mkdir "$(dirname "${BASH_SOURCE}")/../csv"
    # copy down csv files from bucket
    gsutil -m cp gs://all-of-us-cb-test-csv/*.csv "$(dirname "${BASH_SOURCE}")/../csv"
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
