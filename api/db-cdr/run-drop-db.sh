#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'

# Ruby is not installed in our dev container and this script is short, so bash is fine.

DROP_DB_FILE=/tmp/drop_db.sql

function finish {
  rm -f ${DROP_DB_FILE}
}
trap finish EXIT

cat drop_db.sql | envsubst > $DROP_DB_FILE

echo "Dropping database..."
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${DROP_DB_FILE}
