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

# Download data and import
REMOTE_DATA_LOC=https://storage.googleapis.com/all-of-us-workbench-cdr-init
echo "Importing data files from $REMOTE_DATA_LOC"

# Add data files to import here
DATA_FILES=(concept.csv concept_relationship.csv)

# Download data and truncate tables before dropping indexes
for f in "${DATA_FILES[@]}"
do
  local_fpath=/tmp/$f
  curl -o $local_fpath "$REMOTE_DATA_LOC/$f"
  db_name=cdr
  table_name="${f%\.csv*}"
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "truncate table $db_name.$table_name"
done

echo "Dropping indexes on big tables to speed up insert (dramatically) "
../gradlew update $drop_index_activity $context

# Import the data
for f in "${DATA_FILES[@]}"
do
  local_fpath=/tmp/$f
  db_name=cdr
  table_name="${f%\.csv*}"
  mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
    --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
    -u root -p${MYSQL_ROOT_PASSWORD} cdr $local_fpath
  rm $local_fpath
done

# Add the indexes back and any other queries with data
echo "Adding indexes back, finalizing data ..."
../gradlew update $add_index_activity $context
