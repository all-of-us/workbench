#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='WHOLE GENOME VARIANT DATA'
SQL_SCRIPT_ORDER=6
TBL_CBC='cb_criteria'
####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$SQL_SCRIPT_ORDER
  res=$(bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$tmpTbl\` AS
      SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.$1\` LIMIT 0")
  echo $res >&2
  echo "$tmpTbl"
}
function cpToMain(){
  local tbl_to=`echo "$1" | perl -pe 's/(prep_temp_)|(_\d+)//g'`
  bq cp --append_table=true --quiet --project_id=$BQ_PROJECT \
     $BQ_DATASET.$1 $BQ_DATASET.$tbl_to
}
echo "Running in parallel and Multitable mode - " "$SQL_SCRIPT_ORDER - $SQL_FOR"
STEP=$SQL_SCRIPT_ORDER
CB_CRITERIA_START_ID=$[$STEP*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[STEP+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########
# make-cb-criteria-06-whole-genome-variant.sh
#979 - #1088 : make-bq-criteria-tables.sh
# ---------ORDER - 6 - WHOLE GENOME VARIANT---------
# ORDER 6: #979 - #1088: WHOLE GENOME VARIANT DATA
# cb_criteria: Uses : cb_criteria
################################################
# WHOLE GENOME VARIANT DATA
################################################
echo "WHOLE GENOME VARIANT DATA"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , name
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) + 1  AS id
    , -1
    , 'WHOLE_GENOME_VARIANT'
    , 1
    , 'WHOLE_GENOME_VARIANT'
    , 'Whole Genome Variant'
    , 1
    , 0
    , 0
    , 0"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
wait
