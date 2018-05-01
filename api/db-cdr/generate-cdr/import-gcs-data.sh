#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'
# Imports data from gcs bucket into local mysql cdr<VERSION> tables

USAGE="./generate-cdr/import-gcs-data.sh --bucket <BUCKET>  --cdr-db-name <cdrYYYYMMDD>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bucket) BUCKET=$2; shift 2;;
    --cdr-db-name) CDR_DB_NAME=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done
# Todo this requires args in right order and doesn't print usage. Prints "Unbound variable ...."
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
TABLES=(achilles_analysis achilles_results achilles_results_concept db_domain domain vocabulary criteria concept concept_relationship concept_ancestor)

# Make a dir for the csvs
local_fpath=/tmp/$CDR_DB_NAME
rm -rf $local_fpath
mkdir -p $local_fpath

# Download data
gsutil -m cp gs://$BUCKET/$CDR_DB_NAME/*.csv $local_fpath

# Drop indexes before importing
echo "Dropping indexes on big tables to speed up inserts"
../gradlew update $drop_index_activity

# Function for mysqlimport call
function mysqlimport_table () {
   db=$1
   file=$2

   echo "Mysql importing $file into $db.$table..."
   mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
      --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
      -u root -p${MYSQL_ROOT_PASSWORD} $db $file
}

# Import data
for table in "${TABLES[@]}"
do
  # Truncate the table in case this is ever run separate
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "truncate table $CDR_DB_NAME.$table"

  #  some files come in multiple csv because big. If we have just $table.csv , it is one file
  #  $table0*.csv will be multiple files.
  if [ -f $local_fpath/${table}.csv ]
  then
    mysqlimport_table $CDR_DB_NAME $local_fpath/${table}.csv
  else
    shopt -s nullglob # This makes files be an empty array if there aren't any matching files
    files=($local_fpath/${table}0*)
    files_count=${#files[@]}

    if (( $files_count == 0 ))
    then
      echo "No files for $table"
    else
      for f in "${files[@]}"
      do
        # Move file to $table.csv for mysqlimport. It needs csv named exactly $table.csv
        mv $f $local_fpath/${table}.csv
        mysqlimport_table $CDR_DB_NAME $local_fpath/${table}.csv
      done
    fi

  fi
done

rm -r $local_fpath

# Add the indexes back and any other queries with data
echo "Adding indexes back to $CDR_DB_NAME ..."
../gradlew update $add_index_activity

exit 0