#!/bin/bash

# This generates new mysql database for a cdr version
# note dev-up must be run to generate the schema
# note run-local-data-migrations must be run to generate hard coded data from liquibase

set -xeuo pipefail
IFS=$'\n\t'

# INIT defaults FOR OPTIONAL PARAMS
COPY_CDR_VERSION="";
USAGE="./init-new-cdr-db.sh --cdr-version <VERSION> [--copy-cdr-version <VERSION>]"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --cdr-version) CDR_VERSION=$2; shift 2;;
    --copy-cdr-version) COPY_CDR_VERSION=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo check versions are of form YYYYMMDD
if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# variables
tmp_dir="/tmp"
cdr_schema_file="cdr_schema.sql"
public_schema_file="public_schema.sql"
hard_data_file="hard_data.sql"
new_cdr_db_name="cdr$CDR_VERSION"
copy_cdr_db_name="cdr$COPY_CDR_VERSION"

echo "Making a copy of $copy_cdr_db_name to $new_cdr_db_name"

# create new cdr database
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "drop database if exists $new_cdr_db_name"
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "create database $new_cdr_db_name"

# Dump copy cdr schema and import to new database
echo "Dumping cdr schema "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
    --no-data --add-drop-table --ignore-table=$copy_cdr_db_name.DATABASECHANGELOG \
    --ignore-table=$copy_cdr_db_name.DATABASECHANGELOGLOCK $copy_cdr_db_name  \
    > $tmp_dir/$cdr_schema_file

mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} $new_cdr_db_name < $tmp_dir/$cdr_schema_file
rm $tmp_dir/$cdr_schema_file

# Dump the couy cdr data from the hardcoded data tables -- criteria, db_domain .. and import
echo "Dumping Hard coded data "
mysqldump -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} \
     --add-drop-table --disable-keys $copy_cdr_db_name db_domain criteria > $tmp_dir/$hard_data_file
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} $new_cdr_db_name < $tmp_dir/$hard_data_file

rm $tmp_dir/$hard_data_file

# Success
exit 0