#!/bin/bash

# This generates new mysql database for tanagra underlays

set -xeuo pipefail
IFS=$'\n\t'

USAGE="./init-new-tanagra-db.sh [--drop-if-exists]>"
DROP_IF_EXISTS="N"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --drop-if-exists) DROP_IF_EXISTS="Y"; shift 1;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ -z "${TANAGRA_DB_NAME}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

CREATE_DB_FILE=/tmp/create_tanagra_db.sql

function finish {
  rm -f "${CREATE_DB_FILE}"
}
trap finish EXIT

envsubst < "$(dirname "${BASH_SOURCE}")/create_tanagra_db.sql" > $CREATE_DB_FILE

function run_mysql() {
  if [ -f /.dockerenv ]; then
    mysql $@
  else
    echo "Outside docker: invoking mysql via docker for portability"
    docker run -i --rm --network host --entrypoint '' \
      mariadb:10.2 \
      mysql $@
  fi
}
cat "${CREATE_DB_FILE}"

# Drop and create new cdr database
if [ "${DROP_IF_EXISTS}" == "Y" ]
then
  echo "Dropping database $TANAGRA_DB_NAME"
  run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}" -e "drop database if exists ${TANAGRA_DB_NAME}"
fi
echo "Creating database ..."
run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}" < "${CREATE_DB_FILE}"

# If not set you get exception when running both dev-up and dev-up-tanagra
# Caused by: java.sql.SQLNonTransientConnectionException: (conn=208) Too many connections
echo "Setting global max_connections variable to 300"
run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}" -e "SET GLOBAL max_connections=300;"

exit 0
