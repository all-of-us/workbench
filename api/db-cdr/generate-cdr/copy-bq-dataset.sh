#!/bin/bash

set -ex

# This command may be used to copy an entire BigQuery dataset from one location to
# another.
#
# Invoke via db-cdr/generate-cdr/project.rb publish-cdr
#

export SOURCE_DATASET=$1  # project1:dataset1
export DEST_DATASET=$2  # project2:dataset2
export JOB_PROJECT=$3 # project3
export MATCH_FILTER=$4 # table grep filter to include
export SKIP_FILTER=$5 # table grep filter to skip

for f in $(bq ls -n 1000 $SOURCE_DATASET |
             grep TABLE |
             awk '{print $1}' |
             grep "${MATCH_FILTER}" |
             grep -v "${SKIP_FILTER}")
do
  bq --project_id="${JOB_PROJECT}" cp -f "${SOURCE_DATASET}.${f}" "${DEST_DATASET}.${f}"
done