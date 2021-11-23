#!/bin/bash

set -e
SQL_FOR='VISIT_OCCURRENCE (VISITS/ENCOUNTERS)'
TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########

echo "VISIT_OCCURRENCE - add items with counts"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
wait

