#!/bin/bash

set -e

TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating Visits criteria"

CB_CRITERIA_START_ID=6000000000
CB_CRITERIA_END_ID=7000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########

echo "VISIT_OCCURRENCE - add items with counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_name)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , -1
    , 'VISIT'
    , 1
    , 'VISIT'
    , concept_id
    , concept_name
    , 0
    , a.cnt
    , a.cnt
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT b.concept_id, b.concept_name, COUNT(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b ON a.visit_concept_id = b.concept_id
        WHERE b.domain_id = 'Visit'
            and b.standard_concept = 'S'
        GROUP BY 1, 2
    ) a"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC"
