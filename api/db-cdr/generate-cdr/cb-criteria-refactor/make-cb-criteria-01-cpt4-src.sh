#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='CPT4'
SQL_SCRIPT_ORDER=1
TBL_CBC='cb_criteria'
TBL_ANC='prep_cpt_ancestor'
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
    TBL_ANC=$(createTmpTable $TBL_ANC)
fi
####### end common block ###########
# make-cb-criteria-01-cpt4-src.sh
#262 - #424 : make-bq-criteria-tables.sh
# cb_criteria: Uses : prep_cpt, concept, cb_search_all_events
# prep_cpt_ancestor: Uses : cb_criteria
# cb_criteria update counts: Uses : prep_cpt_ancestor, cb_search_all_events, cb_criteria
# cb_criteria delete zero count parents: Uses :  cb_criteria after update counts
# NOTE: From here on the cb_criteria.id depends on max(id) column from cb_criteria
################################################
# CPT4 - SOURCE  -- STEP-01
################################################
echo "CPT4 - SOURCE - insert data (do not insert zero count children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
SELECT
     (a.id + $CB_CRITERIA_START_ID) id
    , a.parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , a.code
    , CASE WHEN b.concept_id is not null THEN b.concept_name ELSE a.name END AS name
    , CASE WHEN a.parent_id != 0 THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.parent_id != 0 THEN
            CASE
                WHEN c.cnt is null THEN 0
                ELSE c.cnt
            END
        ELSE null
      END AS item_count
    , CASE WHEN a.is_group = 0 and a.is_selectable = 1 THEN c.cnt ELSE null END AS est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , a.path
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\` a
LEFT JOIN
    (
        SELECT concept_id, concept_name
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id = 'CPT4'
    ) b on a.concept_id = b.concept_id
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts that are selectable
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_cpt\`
                    WHERE type = 'CPT4'
                        and is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE a.type = 'CPT4'
    AND
        (
            -- get all groups and get all children that have a count
            is_group = 1
            OR
            (
                is_group = 0
                AND is_selectable = 1
                AND
                    (
                        c.cnt != 0
                        OR c.cnt is not null
                    )
            )
      )
ORDER BY 1"
############ prep_cpt_ancestor ############
echo "CPT4 - SOURCE - add ancestor data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_ANC\`
    (
          ancestor_id
        , descendant_id
    )
SELECT
      DISTINCT a.id ancestor_id
    , coalesce(h.id, g.id, f.id, e.id, d.id, c.id, b.id) descendant_id
FROM (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
           WHERE domain_id = 'PROCEDURE' and type = 'CPT4' and is_standard = 0 and is_group = 1 and parent_id !=0
                 and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) a
JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) b on a.id  = b.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) c on b.id  = c.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) d on c.id  = d.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) e on d.id  = e.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) f on e.id  = f.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) g on f.id  = g.parent_id + $CB_CRITERIA_START_ID
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) h on g.id  = h.parent_id + $CB_CRITERIA_START_ID"

############ cb_criteria - update counts ############
echo "CPT4 - SOURCE - generate parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each group, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all groups except the top level
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                        WHERE type = 'CPT4'
                            and parent_id != 0
                            and is_group = 1
                            and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.$TBL_ANC\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type = 'CPT4'
                    and c.is_selectable = 1
                    and d.is_standard = 0
                    and c.id > $CB_CRITERIA_START_ID and c.id < $CB_CRITERIA_END_ID
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "CPT4 - SOURCE - delete zero count parents"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
WHERE type = 'CPT4'
    and is_group = 1
    and
        (
            (parent_id !=0 and rollup_count = 0)
            or id not in
                (
                    SELECT parent_id + $CB_CRITERIA_START_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                    WHERE type = 'CPT4'
                      and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
                )
        )"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
if [[ "$RUN_PARALLEL" == "mult" ]]; then
  cpToMain "$TBL_CBC" &
  cpToMain "$TBL_ANC" &
fi
wait
