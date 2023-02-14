#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_CBAT='cb_criteria_attribute';
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating observation criteria"

CB_CRITERIA_START_ID=16000000000
CB_CRITERIA_END_ID=17000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_CBAT"
TBL_CBAT=$(createTmpTable $TBL_CBAT)
####### end common block ###########

bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
    , 'OBSERVATION'
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
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.observation_concept_id = b.concept_id
        WHERE b.standard_concept = 'S'
            and b.domain_id = 'Observation'
            and a.observation_concept_id != 0
            and b.vocabulary_id != 'PPI'
            and b.concept_class_id != 'Survey'
            and b.concept_id not in (
                select distinct observation_concept_id
                from \`$BQ_PROJECT.$BQ_DATASET.observation\`
                where observation_source_concept_id in (
                SELECT distinct concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
                )
            )
        GROUP BY 1,2,3,4
    )"

# this will add the min/max values for all numeric observation concepts
# this code will filter out any observations WHERE all results = 0
echo "CB_CRITERIA_ATTRIBUTE - Observations - add numeric results"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBAT\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT COALESCE(MAX(id),1) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBAT\`) as id
    , concept_id
    , value_as_concept_id
    , concept_name
    , type
    , cnt
FROM
    (
        SELECT
              observation_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MIN' as concept_name
            , 'NUM' as type
            , CAST(MIN(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
        WHERE observation_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE domain_id = 'OBSERVATION'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)

        UNION ALL

        SELECT
              observation_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MAX' as concept_name
            , 'NUM' as type
            , CAST(max(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
        WHERE observation_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE domain_id = 'OBSERVATION'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)
    ) a"

# this will add all categorical values for all observation concepts where value_as_concept_id is valid
echo "CB_CRITERIA_ATTRIBUTE - Observation - add categorical results"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBAT\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBAT\`) as id
    , concept_id
    , value_as_concept_id
    , concept_name
    , type
    , cnt
FROM
    (
        SELECT
              observation_concept_id as concept_id
            , value_as_concept_id
            , b.concept_name
            , 'CAT' as type
            , CAST(COUNT(DISTINCT person_id) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_as_concept_id = b.concept_id
        WHERE observation_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE domain_id = 'OBSERVATION'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_concept_id != 0
            and value_as_concept_id is not null
        GROUP BY 1,2,3
    ) a"

# set has_attribute=1 for any observation criteria that has data in cb_criteria_attribute
echo "CB_CRITERIA - update has_attribute"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET has_attribute = 1
WHERE domain_id = 'OBSERVATION'
    and is_selectable = 1
    and is_standard = 1
    and concept_id in
    (
        SELECT DISTINCT concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBAT\`
    )"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_CBAT"
