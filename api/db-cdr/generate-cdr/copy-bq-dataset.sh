#!/bin/bash

# This command may be used to copy an entire BigQuery dataset from one location to
# another. This is meant ONLY to be used when copying test / synthetic data between
# cloud projects.
#
# Example usage (see RW-3112 for context):
#
# ./db-cdr/generate-cdr/copy-bq-dataset.sh all-of-us-ehr-dev:synthetic_cdr20180606 fc-aou-cdr-synthetic-test:synthetic_cdr20180606
#

export SOURCE_DATASET=$1  # project1:dataset1
export DEST_DATASET=$2  # project2:dataset2

for f in `bq ls -n 1000 $SOURCE_DATASET | grep TABLE | awk '{print $1}'`
do
  export CP_COMMAND="bq cp -f $SOURCE_DATASET.$f $DEST_DATASET.$f"
  echo $CP_COMMAND
  echo `$CP_COMMAND`
done