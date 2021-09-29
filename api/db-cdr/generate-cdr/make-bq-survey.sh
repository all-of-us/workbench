#!/bin/bash

# This inserts all rows from prep_survey into cb_criteria with counts.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

schema_path=generate-cdr/bq-schemas

################################################
# PPI SURVEYS
################################################
echo "PPI SURVEYS - drop/create cb_criteria"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.cb_criteria"
bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/cb_criteria.json $BQ_DATASET.cb_criteria

echo "PPI SURVEYS - insert data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
    (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(15000 + id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(15000 + id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) b on a.parent_id = b.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(15000 + id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) c on b.parent_id = c.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(15000 + id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) d on c.parent_id = d.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(15000 + id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) e on d.parent_id = e.id
ORDER BY 1"