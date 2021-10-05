#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='PROCEDURE_OCCURRENCE - SNOMED - SOURCE'
SQL_SCRIPT_ORDER=18
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
    CB_CRITERIA_END_ID=$[50*10**9] # MAX(id) FROM cb_criteria
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
# make-cb-criteria-18-proc-occur-snomed-other-src.sh
#5066- #5419: make-bq-criteria-tables.sh
# ---------ORDER - 18 - PROCEDURE_OCCURRENCE - SNOMED - SOURCE---------
#ORDER - 18: #5066- #5419: - PROCEDURE_OCCURRENCE - SNOMED - SOURCE ---------
# 18.0: #5066:  PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding root
  # cb_criteria: Uses : cb_criteria, concept
# 18.1: #5101:  PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding level 0
  #cb_criteria: Uses : cb_criteria, prep_snomed_rel_pcs_src_in_data
# 18.2: #5154:  PROCEDURE_OCCURRENCE - SNOMED - SOURCE - loop 13 times to add all items …
  #cb_criteria: Uses : cb_criteria, prep_snomed_rel_pcs_src_in_data
# prep_ancestor_staging: #5216: Uses : cb_criteria
# prep_concept_ancestor: #5267: Uses : prep_ancestor_staging
# cb_criteria update counts: #5367: Uses : cb_criteria, procedure_occurrence
# cb_criteria update parent counts: #5385: Uses : cb_criteria, prep_concept_ancestor, procedure_occurrence
# add to make-cb-criteria-18-proc-occur-snomed-other-src.sh
# ADD IN OTHER CODES NOT ALREADY CAPTURED
#6039: PROCEDURE_OCCURRENCE - add other source concepts
#cb_criteria: Uses : cb_criteria, procedure_occurrence, concept
################################################
# PROCEDURE_OCCURRENCE - SNOMED - SOURCE
################################################
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding root"
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
    (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 as id
    , 0
    , 'PROCEDURE'
    , 0
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID) + 1 AS STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 4322976"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding level 0"
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
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
        + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
    , p.id
    , 'PROCEDURE'
    , 0
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
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'PROCEDURE'
    and p.type = 'SNOMED'
    and p.is_standard = 0
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
            WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 12 levels, but we run it 13 times to be safe (if changed, change number of joins in next query)
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..13};
do
    echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding level $i"
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
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name)
          + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID)
        , p.id
        , 'PROCEDURE'
        , 0
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
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'PROCEDURE'
        and p.type = 'SNOMED'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
            )"
done

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - add items into staging table for use in next query"
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
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
         WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0 and parent_id != 0 and is_group = 1
               and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) n on m.id = n.parent_id"

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
# the last UNION statement is to add the item to itself
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_PCA\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_12 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_11 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_10 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_9 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_8 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_7 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_6 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_5 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_4 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_3 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_2 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE concept_id_1 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_PAS\`
WHERE domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT procedure_source_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.id > $CB_CRITERIA_START_ID and x.id < $CB_CRITERIA_END_ID
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - parent counts"
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
                        WHERE domain_id = 'PROCEDURE'
                            and type = 'SNOMED'
                            and is_standard = 0
                            and parent_id != 0
                            and is_group = 1
                            and id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID
                            and concept_id is not null
                    )
                    and is_standard = 0
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` b on a.descendant_concept_id = b.procedure_source_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.is_standard = 0
    and x.is_group = 1"

###############################################
# ADD IN OTHER CODES NOT ALREADY CAPTURED
################################################
echo "PROCEDURE_OCCURRENCE - add other source concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER (order by vocabulary_id,concept_name)
       + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) as id
    , -1
    , 'PROCEDURE'
    , 0
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 0
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name)
        + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`) as STRING) as path
FROM
    (
        SELECT b.concept_name, b.vocabulary_id, b.concept_id, b.concept_code, count(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_source_concept_id = b.concept_id
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.procedure_concept_id = c.concept_id
        WHERE a.procedure_source_concept_id NOT IN
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
                WHERE domain_id = 'PROCEDURE'
                    and is_standard = 0
                    and concept_id is not null
                    and id > $CB_CRITERIA_START_ID and id < $CB_CRITERIA_END_ID
            )
            and a.procedure_source_concept_id != 0
            and a.procedure_source_concept_id is not null
            and b.concept_id is not null
            and b.vocabulary_id != 'PPI'
            and (b.domain_id = 'Procedure' OR c.domain_id = 'Procedure')
        GROUP BY 1,2,3,4
    )"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
if [[ "$RUN_PARALLEL" == "mult" ]]; then
  cpToMain "$TBL_CBC" &
  cpToMain "$TBL_PAS" &
  cpToMain "$TBL_PCA" &
  wait
fi

