#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_PAS='prep_ancestor_staging'
TBL_PCA='prep_concept_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating ICD10CM source hierarchy"

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PAS"
TBL_PAS=$(createTmpTable $TBL_PAS)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########

CB_CRITERIA_START_ID=8000000000
CB_CRITERIA_END_ID=9000000000

echo "ICD10CM - SOURCE - inserting root"
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
        , is_group
        ,is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY concept_id)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , 0
    , domain_id
    , 0
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_id)
         + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
--- this is the root for ICD10CM
WHERE concept_id = 2500000000"

echo "ICD10CM - SOURCE - inserting second level"
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , 1
    , 0
    , 0
    , 1
    ,CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code)
        + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
        WHERE parent_id = 0
            and type = 'ICD10CM'
            and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
      ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) b on p.concept_id = b.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on  b.concept_id_2 = c.concept_id"

# for each loop, add all items (children/parents) related to the items that were previously added
# only need to loop 5 times, but do 6 to be safe
for i in {1..6};
do
    echo "ICD10CM - SOURCE - inserting level $i"
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID)
        , p.id
        , p.domain_id
        , p.is_standard
        , p.type
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code)
             + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.type = 'ICD10CM'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
            )"
done

echo "ICD10CM - SOURCE - add items into ancestor staging to use in next query"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4

FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
         WHERE domain_id = 'CONDITION' and type = 'ICD10CM' and is_group = 1 and is_selectable = 1 and is_standard = 0
         and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') e on d.id = e.parent_id"

echo "ICD10CM - SOURCE - insert into prep_concept_ancestor"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_4 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_3 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_2 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_1 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE type = 'ICD10CM'
and is_standard = 0"

echo "ICD10CM - SOURCE - update item counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT a.concept_id, COUNT(distinct a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            AND b.vocabulary_id = 'ICD10CM'
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10CM'
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "ICD10CM - SOURCE - generate rollup counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
                , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                        , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE type = 'ICD10CM'
                            and is_standard = 0
                            and is_selectable = 1
                            and is_group = 1
                    )
                    and is_standard = 0
                ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` b on a.descendant_concept_id = b.concept_id
        WHERE b.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10CM'
    and x.is_standard = 0
    and x.is_group = 1"

echo "ICD10 - SOURCE - update ICD10CM domain"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.domain_id = y.domain_id
FROM (
  SELECT c.concept_id, UPPER(c.domain_id) as domain_id
  FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` cr
  JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (cr.concept_id = c.concept_id and cr.type = c.vocabulary_id)
  AND cr.type = 'ICD10CM'
  AND cr.is_standard = 0
  AND c.domain_id in ('Observation', 'Measurement', 'Procedure')
) y
WHERE x.concept_id = y.concept_id
AND x.type = 'ICD10CM'
AND x.is_standard = 0"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_PAS" "$TBL_PCA"
