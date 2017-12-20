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

drop_index_activity="-PrunList=drop_bigdata_indexes"
add_index_activity="-PrunList=add_bigdata_indexes"


echo "Running big data migrations"
# Ruby is not installed in our dev container and this script is short, so bash is fine.

CREATE_DB_FILE=/tmp/create_db.sql

function finish {
  rm -f ${CREATE_DB_FILE}
}
trap finish EXIT

cat create_db.sql | envsubst > $CREATE_DB_FILE

echo "Creating database if it does not exist..."
mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${CREATE_DB_FILE}

echo "Dropping indexes on big tables to speed up insert dramatically"
../gradlew update $drop_index_activity $context

# Download data and import
REMOTE_DATA_LOC=https://storage.googleapis.com/all-of-us-ehr-dev-peter-speltz
echo "Importing data files from REMOTE_DATA_LOC"

# Add data files to import here
DATA_FILES=( concept_relationship.csv concept.csv)
#DROP_INDEXES_FILE=/tmp/drop_indexes.sql

for f in "${DATA_FILES[@]}"
do
  local_fpath=/tmp/$f
  curl -o $local_fpath "$REMOTE_DATA_LOC/$f"
  db_name=cdr
  table_name="${f%\.csv*}"

  # Todo , possibly check download works before truncating
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "truncate table $db_name.$table_name"
  # Drop indexes on the table to speed up inserts. It is much faster dropping on big tables in liquibase
  #cat drop_indexes_$table_name.sql | envsubst > $DROP_INDEXES_FILE
  #mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} < ${DROP_INDEXES_FILE}
  mysqlimport --ignore-lines=1 --fields-terminated-by=, --verbose --local -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} cdr $local_fpath
  rm $local_fpath
done



# Add the indexes back
echo "Adding indexes back ..."
../gradlew update $add_index_activity $context