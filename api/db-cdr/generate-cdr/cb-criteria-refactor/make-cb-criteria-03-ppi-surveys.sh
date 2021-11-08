#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='PPI SURVEYS'
TBL_CBC='cb_criteria'
TBL_PCA='prep_concept_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$ID_PREFIX
  res=$(bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMain(){
  local tbl_to=`echo "$1" | sed -e 's/prep_temp_\(.*\)_[0-9]*/\1/'`
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     $BQ_DATASET.$1 $BQ_DATASET.$tbl_to
}
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########
# make-cb-criteria-03-ppi-surveys.sh
#634 - #890 : make-bq-criteria-tables.sh
# ---------ORDER - 3 - PPI SURVEYS---------634
# ORDER 3: #634 - #890: PPI SURVEYS
# cb_criteria: Uses : cb_criteria, prep_survey
# cb_criteria additional inserts: Uses: cb_criteria, observation, concept
# prep_concept_ancestor: Uses: cb_criteria
# cb_criteria update counts: Uses: cb_criteria, cb_search_all_events, prep_concept_ancestor
################################################
# PPI SURVEYS
################################################
echo "PPI SURVEYS - insert data"
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
        , value
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
      a.new_id as id
    , CASE WHEN a.parent_id = 0 THEN 0 ELSE b.new_id END as parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , a.code
    , a.name
    , a.value
    , CASE
        WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
        ELSE null
      END as rollup_count
      , CASE
          WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
          ELSE null
        END as item_count
    , CASE
        WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
        ELSE null
      END as est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , REGEXP_REPLACE( IFNULL(e.new_id,-1) ||'.'|| IFNULL(d.new_id,-1) ||'.'|| IFNULL(c.new_id,-1) ||'.'|| IFNULL(b.new_id,-1) ||'.'|| IFNULL(a.new_id,-1), '(-1.)*' ,'' ) as path
FROM
    (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as new_id
      , * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as new_id
      , id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) b on a.parent_id = b.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as new_id
      , id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) c on b.parent_id = c.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as new_id
      , id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) d on c.parent_id = d.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as new_id
      , id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) e on d.parent_id = e.id
ORDER BY 1"

echo "PPI SURVEYS - insert extra answers (Skip, Prefer Not To Answer, Dont Know)"
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
        , name
        , value
        , rollup_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY e.id, d.answer)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) AS id
    , e.id as parent_id
    , e.domain_id
    , e.is_standard
    , e.type
    , 'ANSWER'
    , e.concept_id
    , d.answer as name
    , CAST(d.value_source_concept_id as STRING)
    , 0
    , 0
    , 1
    , 0
    , 1
    , CONCAT(e.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY e.id, d.answer)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS STRING))
FROM
    (
        SELECT DISTINCT a.observation_source_concept_id
            , a.value_source_concept_id
            , regexp_replace(b.concept_name, r'^.+:\s', '') as answer  --remove 'PMI: ' from concept name (ex: PMI: Skip)
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_source_concept_id = b.concept_id
        LEFT JOIN  --filter out items already in the table
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE domain_id = 'SURVEY'
            ) c on (a.observation_source_concept_id = c.concept_id and CAST(a.value_source_concept_id as STRING) = c.value)
        WHERE a.value_source_concept_id in (903096, 903079, 903087)
            and a.observation_source_concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'SURVEY'
                        and concept_id is not null
                )
            and c.id is null
    ) d
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` e on
    (d.observation_source_concept_id = e.concept_id and e.domain_id = 'SURVEY' and e.is_group = 1)"

# the concept_id of the answer is the concept_id for the question
# we do this because there are a few answers that are attached to a topic and we want to get those as well
echo "PPI SURVEYS - add items to ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT b.concept_id as ancestor_concept_id
    , a.concept_id as descendant_concept_id
    , a.is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` a
LEFT JOIN (SELECT id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID AND concept_id is not null) b on CAST(regexp_extract(a.path, r'^\d+') AS INT64) = b.id
WHERE a.domain_id = 'SURVEY'
    and a.subtype = 'ANSWER'"

echo "PPI SURVEYS - generate answer counts for all questions EXCEPT where question concept_id = 1585747"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, CAST(value_source_concept_id as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'SURVEY'
                        and type = 'PPI'
                        and subtype = 'ANSWER'
                        and concept_id != 1585747
                )
        GROUP BY 1,2
        ORDER BY 1,2
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.subtype = 'ANSWER'
    and x.concept_id = y.concept_id
    and x.value = y.value"

echo "PPI SURVEYS - generate answer counts for question concept_id = 1585747"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, CAST(value_as_number as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id = 1585747
            and value_as_number is not null
        GROUP BY 1,2

        UNION ALL

        SELECT concept_id, CAST(value_source_concept_id as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id = 1585747
            and value_source_concept_id != 0
        GROUP BY 1,2
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.subtype = 'ANSWER'
    and x.concept_id = y.concept_id
    and x.value = y.value"

echo "PPI SURVEYS - generate question counts"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE domain_id = 'SURVEY'
                        and type = 'PPI'
                        and is_group = 1
                        and is_selectable = 1
                        and parent_id != 0
                )
        GROUP BY 1
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.is_group = 1
    and x.concept_id = y.concept_id"

echo "PPI SURVEYS - generate survey counts"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT b.ancestor_concept_id, count(DISTINCT a.person_id) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
                WHERE ancestor_concept_id in
                    (
                        SELECT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE domain_id = 'SURVEY'
                            and parent_id = 0
                    )
            ) b on a.concept_id = b.descendant_concept_id
        WHERE a.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.concept_id = y.ancestor_concept_id"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
cpToMain "$TBL_PCA" &
wait

