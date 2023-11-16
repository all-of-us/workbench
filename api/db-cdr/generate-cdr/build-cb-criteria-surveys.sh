#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_PCA='prep_concept_ancestor'

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating survey hierarchy"

CB_CRITERIA_START_ID=3000000000
CB_CRITERIA_END_ID=4000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########

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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
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

echo "PPI SURVEYS - generate answer counts for all questions"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
 SET x.item_count = y.cnt
     , x.est_count = y.cnt
 FROM
     (
         SELECT a.id, se.concept_id, CAST(value_source_concept_id as STRING) as value, COUNT(DISTINCT person_id) cnt
         FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
         JOIN (
             SELECT LEFT(path, STRPOS(path, '.')-1) AS id, concept_id
             FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
             WHERE domain_id = 'SURVEY'
             and type = 'PPI'
             and subtype = 'ANSWER'
         ) a ON se.concept_id = a.concept_id
         WHERE is_standard = 0
         GROUP BY 1,2,3
         ORDER BY 1,2,3
     ) y
 WHERE x.domain_id = 'SURVEY'
     and x.type = 'PPI'
     and x.subtype = 'ANSWER'
     and x.concept_id = y.concept_id
     and x.value = y.value
     and x.path LIKE CONCAT('%', y.id, '.%')"

echo "PPI SURVEYS - generate question counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
 SET x.rollup_count = y.cnt
     , x.item_count = y.cnt
     , x.est_count = y.cnt
 FROM (
   SELECT id, se.concept_id, COUNT(DISTINCT person_id) cnt
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
   JOIN (
     SELECT DISTINCT a.id, concept_id
     FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` c
     JOIN (
       SELECT CAST(id AS STRING) AS id
         FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
       WHERE domain_id = 'SURVEY'
         AND parent_id = 0
     ) a ON (c.path LIKE CONCAT('%', a.id, '.%'))
   ) b ON (se.concept_id = b.concept_id)
   WHERE is_standard = 0
   GROUP BY 1, 2
 ) y
 WHERE x.domain_id = 'SURVEY'
   AND x.type = 'PPI'
   AND x.is_group = 1
   AND x.concept_id = y.concept_id
   AND x.path LIKE CONCAT('%', y.id, '.%')"

echo "PPI SURVEYS - generate survey counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
 SET x.rollup_count = y.cnt
     , x.est_count = y.cnt
 FROM
     (
         SELECT c.id, c.ancestor_concept_id, count(DISTINCT a.person_id) as cnt
         FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
         JOIN
             (
                 SELECT b.id, pca.ancestor_concept_id, pca.descendant_concept_id
                 FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\` pca
                 JOIN (

                     SELECT CAST(id AS STRING) AS id, concept_id
                     FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                     WHERE domain_id = 'SURVEY'
                     AND parent_id = 0

                 ) b ON pca.ancestor_concept_id = b.concept_id
             ) c on a.concept_id = c.descendant_concept_id
         WHERE a.is_standard = 0
         GROUP BY 1, 2
     ) y
 WHERE x.domain_id = 'SURVEY'
 and x.concept_id = y.ancestor_concept_id
 AND x.path = y.id"

echo "PPI SURVEYS - update Minute Survey Name"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.name = 'COVID-19 Vaccine Survey'
WHERE code = 'cope_vaccine4'
AND domain_id = 'SURVEY'"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_PCA"
