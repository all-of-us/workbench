#!/bin/bash

set -e

TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating PM for Concept Sets"

CB_CRITERIA_START_ID=4000000000
CB_CRITERIA_END_ID=5000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########

echo "PHYSICAL MEASUREMENTS - CONCEPT SET"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , code
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_name)
       + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) AS id
    , -1
    , 'PHYSICAL_MEASUREMENT_CSS'
    , 0
    , vocabulary_id as type
    , concept_id
    , concept_code as code
    , concept_name as name
    , 0 as rollup_count
    , b.cnt as item_count
    , b.cnt as est_count
    , 0
    , 1
    , 0
    , 0
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_name)
        + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id = 'PPI'
        and concept_class_id = 'Clinical Observation'
        and domain_id = 'Measurement'
    ) a
JOIN
    (   --- get the count of distinct patients coded with each concept
        SELECT measurement_source_concept_id , COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        GROUP BY 1
    ) b on a.concept_id = b.measurement_source_concept_id"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC"
