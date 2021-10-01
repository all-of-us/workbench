#!/bin/bash

# This inserts all rows from prep_survey into cb_criteria with counts.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

BUCKET="all-of-us-workbench-private-cloudsql"
CDR_CSV_DIR="cdr_csv_files"

echo "Extracting prep_survey to the proper bucket"
bq extract --project_id="$BQ_PROJECT" --destination_format CSV --print_header=false \
"$BQ_DATASET.prep_survey" gs://"$BUCKET"/"$BQ_DATASET"/"$CDR_CSV_DIR"/prep_survey.csv
echo "Completed extract into bucket(gs://$BUCKET/$BQ_DATASET/$CDR_CSV_DIR/prep_survey.csv)"

echo "PPI SURVEYS - insert into cb_criteria"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),15000) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),15000) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) b on a.parent_id = b.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),15000) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) c on b.parent_id = c.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),15000) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) d on c.parent_id = d.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT COALESCE(MAX(id),15000) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) e on d.parent_id = e.id
ORDER BY 1"

# the concept_id of the answer is the concept_id for the question
# we do this because there are a few answers that are attached to a topic and we want to get those as well
echo "PPI SURVEYS - add items to ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT b.concept_id as ancestor_concept_id
    , a.concept_id as descendant_concept_id
    , a.is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on CAST(regexp_extract(a.path, r'^\d+') AS INT64) = b.id
WHERE a.domain_id = 'SURVEY'
    and a.subtype = 'ANSWER'"

echo "PPI SURVEYS - generate answer counts for all questions EXCEPT where question concept_id = 1585747"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT b.ancestor_concept_id, count(DISTINCT a.person_id) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'SURVEY'
                            and parent_id = 0
                    )
            ) b on a.concept_id = b.descendant_concept_id
        WHERE a.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.concept_id = y.ancestor_concept_id"