#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_PAS='prep_ancestor_staging'
TBL_PCA='prep_concept_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating MEASUREMENT - Labs - STANDARD LOINC"

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PAS"
TBL_PAS=$(createTmpTable $TBL_PAS)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########

CB_CRITERIA_START_ID=12000000000
CB_CRITERIA_END_ID=13000000000

echo "MEASUREMENT - Labs - STANDARD LOINC - add root"
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 as id
    , 0
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , 36206173
    , 'LP29693-6'
    , 'Lab'
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 AS STRING)"

# add items directly under the root item in the above query
echo "MEASUREMENT - Labs - STANDARD LOINC - add level 0"
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
        + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT( p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.type = 'LOINC'
    and p.subtype = 'LAB'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 12 levels, but we run it 13 times to be safe
# if this number is changed, you will need to change the number of JOINS in the query below
for i in {1..13};
do
    echo "MEASUREMENT - Labs - STANDARD LOINC - add level $i"
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
        , p.id
        , 'MEASUREMENT'
        , 1
        , 'LOINC'
        , 'LAB'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT( p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
             + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.type = 'LOINC'
        and p.subtype = 'LAB'
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
           )"
done

echo "MEASUREMENT - Labs - STANDARD LOINC - add parent for un-categorized labs"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
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
     (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 as id
    , a.id as parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , 'Uncategorized'
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT(a.path, '.', CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 AS STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` a
WHERE type = 'LOINC'
    and subtype = 'LAB'
    and parent_id = 0"

echo "MEASUREMENT - Labs - STANDARD LOINC - add uncategorized labs"
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
SELECT ROW_NUMBER() OVER (ORDER BY concept_name)
    + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as id
    , (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 1
    , CONCAT(
        (SELECT path FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
        WHERE type = 'LOINC' and subtype = 'LAB' and name = 'Uncategorized' and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID), '.',
        CAST(ROW_NUMBER() OVER (ORDER BY concept_name)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) AS STRING) )
FROM
    (
        SELECT concept_id, concept_code, concept_name, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Measurement'
            and vocabulary_id = 'LOINC'
            and concept_class_id = 'Lab Test'
            and measurement_concept_id not in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE type = 'LOINC'
                        and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                        and concept_id is not null
                )
        GROUP BY 1,2,3
    ) x"

# Join Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - Labs - STANDARD LOINC - add items into staging table for use in next query"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , subtype
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.subtype
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id c12
FROM
    (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
        WHERE type = 'LOINC' and subtype = 'LAB' and is_group = 1 and parent_id != 0 and concept_id is not null
              and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id from \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, subtype, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'LOINC' and subtype = 'LAB') n on m.id = n.parent_id"

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - Labs - STANDARD LOINC - add items into ancestor table"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_12 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_11 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_10 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_9 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_8 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_7 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_6 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_5 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_4 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_3 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_2 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_1 is not null
    and type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE type = 'LOINC'
    and subtype = 'LAB'
    and is_standard = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - item counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT measurement_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'LOINC'
    and x.subtype = 'LAB'
    and x.id > $CB_CRITERIA_START_ID and x.id < $CB_CRITERIA_END_ID
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - generate parent counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE type = 'LOINC'
                            and subtype = 'LAB'
                            and is_standard = 1
                            and is_group = 1
                            and parent_id != 0
                            and concept_id is not null
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'LOINC'
    and subtype = 'LAB'
    and x.is_standard = 1
    and x.is_group = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - generate count for Uncategorized parent"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        WHERE measurement_concept_id IN
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE parent_id IN
                    (
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE type = 'LOINC'
                            and subtype = 'LAB'
                            and name = 'Uncategorized'
                    )
            )
    ) y
WHERE x.type = 'LOINC'
    and x.subtype = 'LAB'
    and x.name = 'Uncategorized'"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_PAS" "$TBL_PCA"
