#!/bin/bash

export CDR_DB_NAME=$1 

CREATE_GRANTS_FILE=/tmp/create_grants.sql

envsubst < "$(dirname "${BASH_SOURCE}")/create_grants.sql" > $CREATE_GRANTS_FILE

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

echo "Setting grants ..."
cat "${CREATE_GRANTS_FILE}" | run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}"