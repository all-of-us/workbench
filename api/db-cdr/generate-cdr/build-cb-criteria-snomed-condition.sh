#!/bin/bash

set -e

TBL_CBC='cb_criteria'
TBL_PAS='prep_ancestor_staging'
TBL_PCA='prep_concept_ancestor'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "Creating SNOMED condition hierarchy"

CB_CRITERIA_START_ID=10000000000
CB_CRITERIA_END_ID=11000000000

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
echo "Creating temp table for $TBL_PAS"
TBL_PAS=$(createTmpTable $TBL_PAS)
echo "Creating temp table for $TBL_PCA"
TBL_PCA=$(createTmpTable $TBL_PCA)
####### end common block ###########

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding root"
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
    (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 AS id
    , 0
    , 'CONDITION'
    , 1
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 441840"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding level 0"
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
    , 'CONDITION'
    , 1
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
         + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'CONDITION'
    and p.type = 'SNOMED'
    and p.is_standard = 1
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 17 levels, but we run it 18 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..18};
do
    echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding level $i"
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
        , 'CONDITION'
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
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'CONDITION'
        and p.type = 'SNOMED'
        and p.is_standard = 1
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
            )"
done

# Join Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add items into staging table for use in next query"
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
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
        , concept_id_13
        , concept_id_14
        , concept_id_15
        , concept_id_16
        , concept_id_17
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
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id as c12
    , o.concept_id as c13
    , p.concept_id as c14
    , q.concept_id as c15
    , r.concept_id as c16
    , s.concept_id as c17
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
          WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1 and parent_id != 0 and is_group = 1
                 and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) n on m.id = n.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) o on n.id = o.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) p on o.id = p.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) q on p.id = q.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) r on q.id = r.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) s on r.id = s.parent_id"

# Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add items into ancestor table"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_17 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_17 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_16 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_16 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_15 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_15 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_14 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_14 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_13 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_13 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_12 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_11 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_10 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_9 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_8 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_7 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_6 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_5 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_4 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_3 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_2 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_1 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - item counts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT condition_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - parent counts"
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
                        WHERE domain_id = 'CONDITION'
                            and type = 'SNOMED'
                            and is_standard = 1
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` b on a.descendant_concept_id = b.condition_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_group = 1"

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC" "$TBL_PAS" "$TBL_PCA"
