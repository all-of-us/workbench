#!/bin/bash
set -e
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "CREATE TABLES - prep_concept_merged"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` AS
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
UNION ALL
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept\`"

echo "CREATE TABLES - prep_concept_relationship_merged"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\` AS
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\`
UNION ALL
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship\`"

