#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='ICD10CM - SOURCE'
SQL_SCRIPT_ORDER=10
TBL_CBC='cb_criteria'
TBL_PAS='prep_ancestor_staging'
TBL_PCA='prep_concept_ancestor'
####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="temp_"$1"_"$SQL_SCRIPT_ORDER
  res=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMain(){
  local tbl_to=`echo "$1" | perl -pe 's/(temp_)|(_\d+)//g'`
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     $BQ_DATASET.$1 $BQ_DATASET.$tbl_to
}
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
# export DATA_BROWSER=$3      # data browser flag
RUN_PARALLEL=$3
if [[ "$RUN_PARALLEL" == "par" ]]; then
  echo "Running in parallel mode - " "$SQL_SCRIPT_ORDER - $SQL_FOR"
  STEP=$SQL_SCRIPT_ORDER
  CB_CRITERIA_START_ID=$[$STEP*10**9] # 3  billion
  CB_CRITERIA_END_ID=$[$[STEP+1]*10**9] # 4  billion
elif [[ "$RUN_PARALLEL" == "seq" ]]; then
    echo "Running in sequential mode - "  "$SQL_SCRIPT_ORDER - $SQL_FOR"
    CB_CRITERIA_START_ID=0
    CB_CRITERIA_END_ID=$[50*10**9] # max(id) from cb_criteria
elif [[ "$RUN_PARALLEL" == "mult" ]]; then
    echo "Running in parallel and Multitable mode - " "$SQL_SCRIPT_ORDER - $SQL_FOR"
    STEP=$SQL_SCRIPT_ORDER
    CB_CRITERIA_START_ID=$[$STEP*10**9] # 3  billion
    CB_CRITERIA_END_ID=$[$[STEP+1]*10**9] # 4  billion
    echo "Creating temp table for $TBL_CBC"
    TBL_CBC=$(createTmpTable $TBL_CBC)
    TBL_PAS=$(createTmpTable $TBL_PAS)
    TBL_PCA=$(createTmpTable $TBL_PCA)
fi
####### end common block ###########
# make-cb-criteria-10-icd10-cm-src.sh
#1868 - #2132 : make-bq-criteria-tables.sh
################################################
# ICD10CM - SOURCE
################################################
# prep tables already created before
# ---------ORDER - 10 - ICD10CM - SOURCE---------
# Order 10: #1868 - #2132: ICD9 - SOURCE
# 10.0: #1868:  ICD10CM - SOURCE - inserting roots
  # cb_criteria: Uses : cb_criteria, prep_concept_merged
# 10.1: #1906:  ICD10CM - SOURCE - inserting second level
  #cb_criteria: Uses : cb_criteria, prep_concept_relationship_merged, prep_concept_merged
# 10.2: #1955:  ICD10CM - SOURCE - loop 6 times to add all items …
  #cb_criteria: Uses : cb_criteria, prep_icd10_rel_src_in_data
# prep_ancestor_staging: #2014: Uses : cb_criteria
# prep_concept_ancestor: #2043: Uses : prep_ancestor_staging
# cb_criteria update counts: #2081: Uses : cb_criteria, cb_search_all_events, prep_concept_merged
# cb_criteria update rollup counts: #2100: Uses : cb_criteria, prep_concept_ancestor, cb_search_all_events
echo "ICD10CM - SOURCE - inserting root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
    ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
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
        (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
--- this is the root for ICD10CM
WHERE concept_id = 2500000000"

echo "ICD10CM - SOURCE - inserting second level"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
      ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , p.id - $CB_CRITERIA_START_ID AS parent_id
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
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
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
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
          ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID)
        , p.id - $CB_CRITERIA_START_ID
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
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING))
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
        and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        and p.id not in
            (
                SELECT parent_id + $CB_CRITERIA_START_ID
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            )"
done

echo "ICD10CM - SOURCE - add items into ancestor staging to use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
         WHERE type = 'ICD10CM' and is_group = 1 and is_selectable = 1 and is_standard = 0
         and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') b on a.id = b.parent_id + $CB_CRITERIA_START_ID
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') c on b.id = c.parent_id + $CB_CRITERIA_START_ID
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') d on c.id = d.parent_id + $CB_CRITERIA_START_ID
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE type = 'ICD10CM') e on d.id = e.parent_id + $CB_CRITERIA_START_ID"

echo "ICD10CM - SOURCE - insert into prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
if [[ "$RUN_PARALLEL" == "mult" ]]; then
  cpToMain "$TBL_CBC" &
  cpToMain "$TBL_PAS" &
  cpToMain "$TBL_PCA" &
fi
