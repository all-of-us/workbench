#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'

activity="-PrunList=$1"
if [ -z ${2+x} ]
then
    context=""
else
    context="-Pcontexts=$2"
fi

# Ruby is not installed in our dev container and this script is short, so bash is fine.

CREATE_DB_FILE=/tmp/create_db.sql
HEX_DB_PASSWORD=$(hexdump -e '"%X"' <<< ${MYSQL_ROOT_PASSWORD})

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

envsubst < "$(dirname "${BASH_SOURCE}")/create_db.sql" > $CREATE_DB_FILE

function run_mysql() {
#   if [ -f /.dockerenv ]; then
    mysql $@
#  else
#    echo "Outside docker: invoking mysql via docker for portability"
#    docker run -d --name mysql mysql:8.0.26
#    sleep 10
#    docker exec -i mysql mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}"
#
#    docker run --rm --entrypoint '' -i \
#      mysql:8.0.26 \
#      mysql $@
#  fi
}

echo "Creating database if it does not exist..."

echo "${MYSQL_ROOT_PASSWORD}"
echo "${HEX_DB_PASSWORD}"
cat "${CREATE_DB_FILE}" | run_mysql -h "${DB_HOST}" --port "${DB_PORT}" -u root -p"${MYSQL_ROOT_PASSWORD}"

echo "Upgrading database..."
(cd "$(dirname "${BASH_SOURCE}")" && ../gradlew update $activity $context)
