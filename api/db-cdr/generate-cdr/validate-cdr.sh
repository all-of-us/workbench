#!/bin/bash

# This will validate synthetic data and new CDRs

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

echo "Checking for selectable items with no count"
echo "Comment: Selectable items with no count" > ~/output.json
bq --quiet --format=prettyjson --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"SELECT domain_id, is_standard, type, count(*)
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where est_count = -1
and is_selectable = 1
and (type != 'BRAND'
and (domain_id != 'SURVEY'
and name != 'Select a value'))
group by 1,2,3
order by 1,2,3" >> ~/output.json

echo "Check for zero count items"
echo "Comment: Items with zero count" >> ~/output.json
bq --quiet --format=prettyjson --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where est_count = 0
and is_selectable = 1
order by 1" >> ~/output.json

echo "Check for null concept ids"
echo "Comment: Items with null concept ids" >> ~/output.json
bq --quiet --format=prettyjson --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where concept_id is null
order by 1" >> ~/output.json

