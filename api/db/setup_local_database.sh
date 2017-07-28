#!/bin/bash -ae

# Sets up a MySQL database named "workbench" locally (dropping the database if it
# already exists), and sets the database config information.
# You must have MySQL installed before using this.
#
# If you have an environment variable named "MYSQL_ROOT_PASSWORD" it will be
# used as the password to connect to the database; by default, the password
# "root" will be used.

if [ ! -f setup_local_vars.sh ]
then
  echo Script must be run from local db/ directory.
  exit 1
fi

CREATE_DB_FILE=/tmp/create_db.sql

ROOT_USERNAME=
ROOT_PASSWORD=root
while true; do
  case "$1" in
    --nopassword) ROOT_PASSWORD=; shift 1;;
    --db_user) ROOT_DB_USER=$2; shift 2;;
    --db_name) DB_NAME=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

if [ "${MYSQL_ROOT_PASSWORD}" ]
then
  ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}"
else
  echo "Using a default root mysql password. Set MYSQL_ROOT_PASSWORD to override."
fi
source setup_local_vars.sh

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

cat drop_db.sql create_db.sql | envsubst > $CREATE_DB_FILE

echo "Creating empty database..."
mysql -u "$ROOT_DB_USER" -p${ROOT_PASSWORD} < ${CREATE_DB_FILE}
if [ $? != '0' ]
then
  echo "Error creating database. Exiting."
  exit 1
fi

echo "Upgrading database..."
../gradlew update
