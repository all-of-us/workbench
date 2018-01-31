#!/bin/bash
set -xeuo pipefail
IFS=$'\n\t'
# Imports data from gcs bucket into local mysql cdr<VERSION> tables

USAGE="./generate-cdr/import-gcs-data.sh --account <ACCOUNT> --bucket <BUCKET>  --cdr-version <YYYYMMDD>"
while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --account) ACCOUNT=$2; shift 2;;
    --bucket) BUCKET=$2; shift 2;;
    --cdr-version) CDR_VERSION=$2; shift 2;;
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

if [ -z "${CDR_VERSION}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi




drop_index_activity="-PrunList=drop_bigdata_indexes"
add_index_activity="-PrunList=add_bigdata_indexes"
CDR_DB_NAME="cdr${CDR_VERSION}"
echo "Importing data from $BUCKET"
# Ruby is not installed in our dev container and this script is short, so bash is fine.

# Download data and import
REMOTE_DATA_LOC=https://storage.googleapis.com/$BUCKET
echo "Importing data files from $REMOTE_DATA_LOC"

# Add tables names of files to import here
TABLES=(achilles_analysis achilles_results achilles_results_concept domain vocabulary criteria concept concept_relationship)

# Make a dir for the csvs
local_fpath=./generate-cdr/tmp/$CDR_DB_NAME
#rm -rf $local_fpath
#mkdir -p $local_fpath

# Download data and truncate tables before dropping indexes
#gsutil -m cp gs://$BUCKET/$CDR_DB_NAME/*.csv $local_fpath
fpath=$local_fpath/concept.csv
mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
    --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
    -u root -p${MYSQL_ROOT_PASSWORD} $CDR_DB_NAME $fpath

# Concat concept files into  one concept.csv file for mysqlimport
shopt -s nullglob # This makes files be an empty array if there aren't any matching files
files=($local_fpath/concept0*)
i=0
for f in "${files[@]}"
do
  if [ "$i" -eq "0" ]
  then
    cat $f > $local_fpath/concept.csv
  else
    echo "stripping header line from file $f"
    cat $f | tail -n +2 >> $local_fpath/concept.csv
  fi
  i=$(($i + 1))


done

# Concat concept_relationship files into  one concept.csv file for mysqlimport
files=($local_fpath/concept_relationship0*)
i=0
for f in "${files[@]}"
do
  if [ "$i" -eq "0" ]
  then
    cat $f > $local_fpath/concept_relationship.csv
  else
    echo "stripping header line from file $f"
    cat $f | tail -n +2 >> $local_fpath/concept_relationship.csv
  fi
  i=$(($i + 1))

done

# Truncate tables first to make dropping indexes faster
for t in "${TABLES[@]}"
do
  mysql -h ${DB_HOST} --port ${DB_PORT} -u root -p${MYSQL_ROOT_PASSWORD} -e "truncate table $CDR_DB_NAME.$t"
done

echo "Dropping indexes on big tables to speed up insert (dramatically) "
../../gradlew update $drop_index_activity

# Import the data, some big tables come in multiple files. We cat them into one in pieces
for t in "${TABLES[@]}"
do
  fpath=$local_fpath/$t.csv
  mysqlimport --ignore-lines=1 --fields-terminated-by=, --fields-enclosed-by='"' \
    --verbose --local -h ${DB_HOST} --port ${DB_PORT} \
    -u root -p${MYSQL_ROOT_PASSWORD} $CDR_DB_NAME $fpath

done

#rm -r $local_fpath

# Add the indexes back and any other queries with data
echo "Adding indexes back, finalizing data ..."
../../gradlew update $add_index_activity
