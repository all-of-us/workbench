#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

echo "Getting self_reported_category_concept_id column count"
selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "select count(column_name) as count 
from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\` where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\"" | tr -dc '0-9')

echo "$selfReportedCategoryDataCount"