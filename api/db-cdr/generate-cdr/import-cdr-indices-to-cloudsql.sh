#!/bin/bash

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export WORKBENCH_PROJECT=$3 # workbench project
export CDR_VERSION=$4 # cdr version
export DATA_BROWSER=$3 # data browser flag

if [ "$DATA_BROWSER" == false ]
then

  WORKBENCH_DATASET=$CDR_VERSION
  BUCKET="all-of-us-workbench-private-cloudsql"

  startDate=$(date)
  echo $(date) " Starting import-cdr-indices-to-cloudsql $startDate"

  ## Make workbench cdr count data
  echo "Making BigQuery cdr dataset"
  if ./generate-cdr/make-bq-data.sh $BQ_PROJECT $BQ_DATASET $WORKBENCH_PROJECT $WORKBENCH_DATASET
  then
      echo "BigQuery cdr data generated"
  else
      echo "FAILED To generate BigQuery data for cdr $CDR_VERSION"
      exit 1
  fi

  ## Dump workbench cdr count data
  echo "Dumping BigQuery cdr dataset to .csv"
  if ./generate-cdr/make-bq-data-dump.sh $WORKBENCH_PROJECT "$BUCKET/$BQ_DATASET" $WORKBENCH_DATASET
  then
      echo "Workbench cdr count data dumped"
  else
      echo "FAILED to dump Workbench cdr count data"
      exit 1
  fi

  # Init the local database
  echo "Initializing new  $DATABASE"
  if ./generate-cdr/init-new-cdr-db.sh --drop-if-exists --cdr-db-name $WORKBENCH_DATASET
  then
    echo "Success"
  else
    echo "Failed"
    exit 1
  fi

  # Make empty sql dump
  if ./generate-cdr/make-mysqldump.sh --db-name $WORKBENCH_DATASET --bucket "$BUCKET/$BQ_DATASET/$CDR_VERSION"
  then
    echo "Success"
  else
    echo "Failed"
    exit 1
  fi

  # Import Sql dump and data in bucket to cloudsql
  if ./generate-cdr/cloudsql-import.sh --project $WORKBENCH_PROJECT --instance workbenchmaindb --bucket "$BUCKET/$BQ_DATASET/$CDR_VERSION" \
      --database $WORKBENCH_DATASET --create-db-sql-file $WORKBENCH_DATASET.sql
  then
    echo "Success"
  else
    echo "Failed"
    exit 1
  fi

  stopDate=$(date)
  echo "Start $startDate Stop: $stopDate"
  echo $(date) " Finished import-cdr-indices-to-cloudsql"

fi