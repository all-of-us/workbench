#!/bin/bash

set -ex

# This command may be used to copy an entire BigQuery dataset from one location to
# another. This is meant ONLY to be used when copying test / synthetic data between
# cloud projects.
#
# Invoke via db-cdr/generate-cdr/project.rb publish-cdr
#

export SOURCE_DATASET=$1  # project1:dataset1
export DEST_DATASET=$2  # project2:dataset2
export JOB_PROJECT=$3 # project3

for f in $(bq ls -n 1000 $SOURCE_DATASET | grep TABLE | awk '{print $1}')
do
  bq --project_id="${JOB_PROJECT}" cp -f "${SOURCE_DATASET}.${f}" "${DEST_DATASET}.${f}"
done
