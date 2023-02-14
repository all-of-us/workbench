#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_PAS='prep_ancestor_staging'
TBL_PCA='prep_concept_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating MEASUREMENT - SNOMED - STANDARD"

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PAS"
TBL_PAS=$(createTmpTable $TBL_PAS)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########

CB_CRITERIA_START_ID=13000000000
CB_CRITERIA_END_ID=14000000000

echo "MEASUREMENT - SNOMED - STANDARD - add roots"
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
    ROW_NUMBER() OVER(ORDER BY concept_name)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as id
    , 0
    , 'MEASUREMENT'
    , 1
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER(ORDER BY concept_name)
         + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING) as path
FROM
    (
        SELECT DISTINCT concept_id, concept_name, concept_code
        FROM
            (
                SELECT d.concept_id, d.concept_name, d.concept_code, RANK() OVER (PARTITION BY c.descendant_concept_id ORDER BY c.max_levels_of_separation DESC) rnk
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on c.ancestor_concept_id = d.concept_id
                WHERE d.domain_id = 'Measurement'
                    and d.vocabulary_id = 'SNOMED'
                    and c.descendant_concept_id in
                        (
                            SELECT DISTINCT concept_id
                            FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
                            JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
                            WHERE standard_concept = 'S'
                                and domain_id = 'Measurement'
                                and vocabulary_id = 'SNOMED'
                        )
            ) a
        WHERE rnk = 1
    ) x"

echo "MEASUREMENT - SNOMED - STANDARD - adding level 0"
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
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'MEASUREMENT'
    , 1
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'MEASUREMENT'
    and p.type = 'SNOMED'
    and p.is_standard = 1
    and p.id > $CB_CRITERIA_START_ID and p.id < $CB_CRITERIA_END_ID
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 8 levels, but we run it 9 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..9};
do
    echo "MEASUREMENT - SNOMED - STANDARD - adding level $i"
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
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
        , p.id
        , 'MEASUREMENT'
        , 1
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
             + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'MEASUREMENT'
        and p.type = 'SNOMED'
        and p.is_standard = 1
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
            )"
done

# Join Count: 9 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - SNOMED - STANDARD - add items into staging table for use in next query"
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
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
         WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1 and parent_id != 0 and is_group = 1
               and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) i on h.id = i.parent_id"

# Count: 9 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - SNOMED - STANDARD - add items into ancestor table"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_8 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_7 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_6 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_5 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_4 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_3 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_2 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_1 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1"

echo "MEASUREMENT - SNOMED - STANDARD - item counts"
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
    and x.domain_id = 'MEASUREMENT'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "MEASUREMENT - SNOMED - STANDARD - parent counts"
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
                        WHERE domain_id = 'MEASUREMENT'
                            and type = 'SNOMED'
                            and is_standard = 1
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'MEASUREMENT'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_group = 1"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_PAS" "$TBL_PCA"
