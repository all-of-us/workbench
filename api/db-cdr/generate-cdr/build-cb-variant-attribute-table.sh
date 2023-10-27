#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

