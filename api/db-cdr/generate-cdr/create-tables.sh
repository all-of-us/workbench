#!/bin/bash

# This script removes/creates all CDR indices specific tables.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

schema_path=generate-cdr/bq-schemas

for filename in bq-schemas/*.json;
do
    json_name=${filename##*/}
    table_name=${json_name%.json}
    bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$table_name"
    bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
done