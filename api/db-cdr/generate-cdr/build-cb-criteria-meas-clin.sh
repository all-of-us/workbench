#!/bin/bash

set -e

TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating Clinical - STANDARD LOINC"

CB_CRITERIA_START_ID=11000000000
CB_CRITERIA_END_ID=12000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########

echo "MEASUREMENT - Clinical - STANDARD LOINC"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
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
    select id,
    parent_id,
    domain_id,
    is_standard,
    type,
    subtype,
    concept_id,
    concept_code,
    concept_name,
    rollup_count,
    item_count,
    est_count,
    is_group,
    is_selectable,
    has_attribute,
    has_hierarchy,
    CAST(id as STRING) as path
    from
    (
    SELECT
    ROW_NUMBER() OVER(ORDER BY concept_name)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , -1 as parent_id
    , 'MEASUREMENT' as domain_id
    , 1 as is_standard
    , 'LOINC' as type
    , 'CLIN' as subtype
    , concept_id
    , concept_code
    , concept_name
    , 0 as rollup_count
    , cnt as item_count
    , cnt as est_count
    , 0 as is_group
    , 1 as is_selectable
    , 0 as has_attribute
    , 0 as has_hierarchy
    FROM
    (
    SELECT
    b.concept_name
    , b.concept_id
    , b.concept_code
    , COUNT(DISTINCT a.person_id) cnt
    FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
    JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
    WHERE standard_concept = 'S'
    and domain_id = 'Measurement'
    and vocabulary_id = 'LOINC'
    and concept_class_id = 'Clinical Observation'
    GROUP BY 1,2,3
    ) a) b"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC"
