#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'

liquibaseCommand="${1-update}"
runList="-PrunList=${2-main}"
liquibaseArgs="${3--PliquibaseCommandValue=}"

CREATE_DB_FILE=/tmp/create_db.sql

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

envsubst < "$(dirname "${BASH_SOURCE}")/create_db.sql" > $CREATE_DB_FILE

echo "Creating database if it does not exist..."
# This command is run regardless, and we rely on the commands in CREATE_DB_FILE being idempotent.
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${CREATE_DB_FILE}

echo "Run Liquibase..."
# ../gradlew rollbackCount -PrunList=rollbackCount main
(cd "$(dirname "${BASH_SOURCE}")" && ../gradlew $liquibaseCommand $runList $liquibaseArgs)
