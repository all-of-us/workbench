#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

echo "Getting fitbit count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_fitbit = 1"
fitbitCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "fitbit count: $fitbitCount"

echo "Getting fitbit count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_fitbit = 1"
fitbitCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "fitbit count: $fitbitCount"

selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "select count(column_name) as count 
from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\` where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\"" | tr -dc '0-9')

echo "1st run self reported: $selfReportedCategoryDataCount"

echo "Getting self_reported_category_concept_id column count"
query="select count(column_name) as count from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\`
where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\""
selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "2nd run self reported: $selfReportedCategoryDataCount"