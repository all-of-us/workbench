#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='PHYSICAL MEASUREMENTS - CONCEPT SET'
TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$ID_PREFIX
  res=$(bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMain(){
  local tbl_to=`echo "$1" | sed -e 's/prep_temp_\(.*\)_[0-9]*/\1/'`
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     $BQ_DATASET.$1 $BQ_DATASET.$tbl_to
}
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########
# make-cb-criteria-pm-concept-set.sh
#892 - #890 : make-bq-criteria-tables.sh
# ---------ORDER - 4 - PHYSICAL MEASUREMENTS - CONCEPT SET---------
# ORDER 4: #891 - #947: PHYSICAL MEASUREMENTS - CONCEPT SET
# cb_criteria: Uses : cb_criteria, concept, measurement
################################################
# PHYSICAL MEASUREMENTS - CONCEPT SET
################################################
echo "PHYSICAL MEASUREMENTS - CONCEPT SET"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
      ROW_NUMBER() OVER (ORDER BY concept_name)
       + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) AS id
    , -1
    , 'PHYSICAL_MEASUREMENT_CSS'
    , 0
    , vocabulary_id as type
    , concept_id
    , concept_code as code
    , concept_name as name
    , 0 as rollup_count
    , b.cnt as item_count
    , b.cnt as est_count
    , 0
    , 1
    , 0
    , 0
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_name)
        + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) as STRING)
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id = 'PPI'
        and concept_class_id = 'Clinical Observation'
        and domain_id = 'Measurement'
    ) a
JOIN
    (   --- get the count of distinct patients coded with each concept
        SELECT measurement_source_concept_id , COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        GROUP BY 1
    ) b on a.concept_id = b.measurement_source_concept_id"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
wait
