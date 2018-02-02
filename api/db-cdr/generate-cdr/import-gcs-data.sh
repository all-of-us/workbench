#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'
# Imports data from gcs bucket into local mysql cdr<VERSION> tables
# Example usage with ./project.rb :
# ./project.rb generate-cloudsql-cdr --account peter.speltz@pmi-ops.org --cdr-version 20180201 --bucket all-of-us-workbench-cloudsql-create

USAGE="./generate-cdr/import-gcs-data.sh --account <ACCOUNT> --bucket <BUCKET>  --cdr-db-name <cdrYYYYMMDD>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --cdr-db-name) CDR_DB_NAME=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
if [ -z "${ACCOUNT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BUCKET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${CDR_DB_NAME}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

drop_index_activity="-PrunList=drop_bigdata_indexes"
add_index_activity="-PrunList=add_bigdata_indexes"
export CDR_DB_NAME  # export this so other scripts and gradle knows this
echo "Importing data from $BUCKET"
# Ruby is not installed in our dev container and this script is short, so bash is fine.

# Download data and import
REMOTE_DATA_LOC=https://storage.googleapis.com/$BUCKET
echo "Importing data files from $REMOTE_DATA_LOC"

# Add tables names of files to import here
TABLES=(achilles_analysis achilles_results achilles_results_concept domain vocabulary criteria concept concept_relationship)

# Make a dir for the csvs
local_fpath=./generate-cdr/tmp/$CDR_DB_NAME
rm -rf $local_fpath
mkdir -p $local_fpath

# Download data and truncate tables before dropping indexes
gsutil -m cp gs://$BUCKET/$CDR_DB_NAME/*.csv $local_fpath

# Drop indexes before importing
echo "Dropping indexes on big tables to speed up inserts"
../gradlew update $drop_index_activity

# Import data
for table in "${TABLES[@]}"
do
  # Truncate the table in case this is ever run separate
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "truncate table $CDR_DB_NAME.$table"

  #  some files come in multiple csv because big. So we have to check for that
  shopt -s nullglob # This makes files be an empty array if there aren't any matching files

  files=($local_fpath/${table}0*)
  files_count=${#files[@]}
  echo "Files length $files_count"
  if [ "$files_count" -eq "0" ]
  then
    if [ -f $local_fpath/${table}.csv ]
    then
      echo "Importing one file for $table $local_fpath/${table}.csv"
      mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
      --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
      -u root -p${MYSQL_ROOT_PASSWORD} $CDR_DB_NAME $local_fpath/${table}.csv
    else
      echo "No file $local_fpath/${table}.csv to import."
    fi
  else
    echo "More than one file for $table"
    for f in "${files[@]}"
    do
      # Move file to table.csv for mysql import
      mv $f $local_fpath/${table}.csv
      mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
      --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
      -u root -p${MYSQL_ROOT_PASSWORD} $CDR_DB_NAME $local_fpath/${table}.csv
    done
  fi
done

#rm -r $local_fpath

# Add the indexes back and any other queries with data
echo "Adding indexes back to $CDR_DB_NAME ..."
../gradlew update $add_index_activity

exit 0