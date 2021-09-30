#!/bin/bash

# This script removes/creates all CDR indices specific tables.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

schema_path=generate-cdr/bq-schemas
create_tables=(prep_survey cb_criteria)

for t in "${create_tables[@]}"
do
    bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/$t.json $BQ_DATASET.$t
done