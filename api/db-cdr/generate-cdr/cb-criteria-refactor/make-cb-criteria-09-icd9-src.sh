#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='ICD9 - SOURCE'
SQL_SCRIPT_ORDER=9
####### common block for all make-cb-criteria-dd-*.sh scripts ###########
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
# export DATA_BROWSER=$3      # data browser flag
IS_PARALLEL=$3
if [[ "$IS_PARALLEL" -eq 1 ]]; then
  echo "Running in parallel - " "$SQL_SCRIPT_ORDER - $SQL_FOR"
  STEP=$SQL_SCRIPT_ORDER
  CB_CRITERIA_START_ID=$[$STEP*10**9] # 3  billion
  CB_CRITERIA_END_ID=$[$[STEP+1]*10**9] # 4  billion
else
    echo "Running in Order - "  "$SQL_SCRIPT_ORDER - $SQL_FOR"
    # set start_to to 0 (if running in ORDER from main script)
    CB_CRITERIA_START_ID=0
    # max limit 50 billion (max limit for cb_criteria)
    # not used in this script
    CB_CRITERIA_END_ID=$[50*10**9]
fi
####### end common block ###########
# make-cb-criteria-09-icd9-src.sh
#1009 - #1254 : make-bq-criteria-tables.sh
# ---------ORDER - 9 - ICD9 - SOURCE---------
# Order 9: #1304 - #1713: ICD9 - SOURCE
# 9.0: #1307 : ICD9 - SOURCE - inserting roots
  #cb_criteria: Uses : cb_criteria, prep_concept_merged
# 9.1: #1345 : ICD9 - SOURCE - inserting level 2 (only groups at this level)
  #cb_criteria: Uses : cb_criteria, prep_concept_relationship_merged, prep_concept_merged
# 9.2: #1395 : ICD9 - SOURCE - inserting level 3 (only groups at this level)
  #cb_criteria: Uses : cb_criteria, prep_concept_relationship_merged, prep_concept_merged, cb_search_all_events
# 9.3: #1464 : ICD9 - SOURCE - inserting level 4 (parents and children)
  #cb_criteria: Uses : cb_criteria, concept, cb_search_all_events, prep_concept_merged
# 9.4: #1550 : ICD9 - SOURCE - inserting level 5 (children)
  #cb_criteria: Uses : cb_criteria, concept, cb_search_all_events, prep_concept_merged
# prep_ancestor_staging: #1621 : Uses : cb_criteria
# prep_concept_ancestor: #1644 : Uses : prep_ancestor_staging
# cb_criteria update rollup counts: #1670 : Uses : cb_criteria, prep_concept_ancestor, cb_search_all_events
# cb_criteria delete zero counts: #1704 : Uses : cb_criteria
# TODO comment : #1713 : TODO there are still some parents that don't actually have any children and never will.  WHAT TO DO?
################################################
# ICD9 - SOURCE
################################################
echo "ICD9 - SOURCE - inserting roots"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
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
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_id) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
-- these are the four root nodes
WHERE concept_id in (2500000024, 2500000023,2500000025,2500000080)"

echo "ICD9 - SOURCE - inserting level 2 (only groups at this level)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
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
    ,CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
-- in order to get level 2, we will link it from its level 1 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type in ('ICD9CM', 'ICD9Proc')
            and parent_id = 0
    ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on x.concept_id_2 = c.concept_id"

echo "ICD9 - SOURCE - inserting level 3 (only groups at this level)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , code
        , name
        , item_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
-- in order to get level 3, we will link it from its level 2 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type in ('ICD9CM', 'ICD9Proc')
            and parent_id != 0
            and is_group = 1
            and is_selectable = 0
    ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on  x.concept_id_2 = c.concept_id
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                )
        GROUP BY 1
    ) d on c.concept_id = d.concept_id"

echo "ICD9 - SOURCE - inserting level 4 (parents and children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , b.id AS parent_id
    , b.domain_id
    , b.is_standard
    , a.vocabulary_id AS type
    , a.concept_id
    , a.concept_code AS code
    , a.concept_name AS name
    , CASE WHEN c.code is null THEN 0 ELSE null END AS rollup_count     -- c.code is null = child
    , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
    , CASE WHEN c.code is null THEN d.cnt ELSE null END AS est_count
    , CASE WHEN c.code is null THEN 0 ELSE 1 END as is_group
    , 1
    , 0
    , 1
    ,CONCAT(b.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
-- in order to get level 4, we will link it to its level 3 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
            -- level 4 codes have a decimal with 1 digit after (ex: 98.0)
            and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d$')
    ) a
-- in order to find its parent, which is just its whole number (ex: 98.0's parent is 98), we will use regex to extract the whole number
JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = b.code and a.vocabulary_id = b.type)
LEFT JOIN
    (
        -- determine if this item is a parent or child by seeing if it has any child items
        -- ex: V09.8 > V09.80 so is_group = 1
        -- ex: E879.5 > nothing so is_group = 0
        SELECT distinct REGEXP_EXTRACT(concept_code, r'^\w{1,}\.\d') code
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
            and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) c on a.concept_code = c.code
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE
    (
        -- get all parents OR get all children that have a count
        c.code is not null
        OR
        (
            c.code is null
            AND d.cnt is not null
        )
    )"

echo "ICD9 - SOURCE - inserting level 5 (children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , CASE WHEN b.id is not null THEN b.id ELSE c.id END AS parent_id
    , CASE WHEN b.domain_id is not null THEN b.domain_id ELSE c.domain_id END as domain_id
    , 0
    , a.vocabulary_id AS type
    , a.concept_id,a.concept_code AS code
    , a.concept_name AS name
    , 0 as rollup_count
    , d.cnt AS item_count
    , d.cnt AS est_count
    , 0
    , 1
    , 0
    , 1
    , CASE
        WHEN b.id is not null THEN
            b.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
        ELSE
            c.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
        END as path
-- in order to get level 5, we will link it to its level 4 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
        -- codes such as 98.01, V09.71, etc.
        and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) a
-- get any level 4 parents that link to this item
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}\.\d') = b.code and a.vocabulary_id = b.type)
-- get any level 3 parents that link to this item (this is because some level 5 items only link to a level 3 item)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = c.code and a.vocabulary_id = c.type)
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE d.cnt is not null"

echo "ICD9 - SOURCE - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
    )
SELECT DISTINCT
      a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc') and is_group = 1 and is_selectable = 1 and is_standard = 0) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc')) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc')) c on b.id = c.parent_id"

echo "ICD9 - SOURCE - inserting into prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and type in ('ICD9CM','ICD9Proc')
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and type in ('ICD9CM','ICD9Proc')
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE type in ('ICD9CM','ICD9Proc')
and is_standard = 0"

echo "ICD9 - SOURCE - generate rollup counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type in ('ICD9CM', 'ICD9Proc')
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
    and x.type in ('ICD9CM', 'ICD9Proc')
    and x.is_standard = 0
    and x.is_group = 1"

echo "ICD9 - SOURCE - delete parents that have no count"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM\`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type in ('ICD9CM', 'ICD9Proc')
    and is_group = 1
    and is_selectable = 1
    and rollup_count is null"

# TODO there are still some parents that don't actually have any children and never will. WHAT TO DO?

