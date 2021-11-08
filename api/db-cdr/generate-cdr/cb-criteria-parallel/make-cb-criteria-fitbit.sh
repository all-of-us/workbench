#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='FITBIT DATA'
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
# make-cb-criteria-fitbit.sh
#949 - #978 : make-bq-criteria-tables.sh
# ---------ORDER - 5 - FITBIT DATA---------
# ORDER 5: #949 - #978: FITBIT DATA
# cb_criteria: Uses : cb_criteria
################################################
# FITBIT DATA
################################################
echo "FITBIT DATA"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
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
    (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) + 1  AS id
    , -1
    , 'FITBIT'
    , 1
    , 'FITBIT'
    , 'Fitbit'
    , 1
    , 0
    , 0
    , 0"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
wait

