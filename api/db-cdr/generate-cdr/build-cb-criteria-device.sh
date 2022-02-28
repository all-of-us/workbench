#!/bin/bash

set -e
SQL_FOR='DEVICE'
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

bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
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
    ROW_NUMBER() OVER(order by vocabulary_id, concept_name)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as id
    , -1
    , 'DEVICE'
    , 1
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 0
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name)
        + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING) as path
FROM
    (
        SELECT
              b.concept_name
            , b.vocabulary_id
            , b.concept_id
            , b.concept_code
            , count(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.device_concept_id = b.concept_id
        WHERE b.standard_concept = 'S'
            and b.domain_id = 'Device'
            and a.device_concept_id != 0
        GROUP BY 1,2,3,4
    ) x"

# copy temp table back to main table then delete temp table
cpToMainThenRmTmpTable "$TBL_CBC"
