#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='PPI - PHYSICAL MEASUREMENTS (PM)'
SQL_SCRIPT_ORDER=2
TBL_CBC='cb_criteria'
####### common block for all make-cb-criteria-dd-*.sh scripts ###########
function createTmpTable(){
  local tmpTbl="prep_temp_"$1"_"$SQL_SCRIPT_ORDER
  res=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
fi
####### end common block ###########
# make-cb-criteria-02-ppi-phys-meas-src.sh
#426 - #632 : make-bq-criteria-tables.sh
# ---------ORDER - 2 - PPI---------
# ORDER 2: #426 - #632: PPI PHYSICAL MEASUREMENTS (PM)
# cb_criteria: Uses : cb_criteria, prep_physical_measurement
# cb_criteria update counts for BP,HR etc: Uses :
# -- no delete counts :  since we already joined on cb_search_all_events(?)
################################################
# PPI PHYSICAL MEASUREMENTS (PM)
################################################
echo "PM - insert data"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , name
        , value
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      a.new_id as id
    , CASE WHEN a.parent_id = 0 THEN 0 ELSE b.new_id END as parent_id
    , domain_id
    , is_standard
    , type
    , subtype
    , concept_id
    , name
    , value
    , CASE WHEN is_selectable = 1 THEN 0 ELSE null END as rollup_count
    , CASE WHEN is_selectable = 1 THEN 0 ELSE null END as item_count
    , CASE WHEN is_selectable = 1 THEN 0 ELSE null END as est_count
    , is_group
    , is_selectable
    , has_attribute
    , has_hierarchy
FROM (SELECT ROW_NUMBER() OVER(ORDER BY id)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) as new_id
       , * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_physical_measurement\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id)
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` WHERE id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID ) as new_id
       , id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_physical_measurement\`) b on a.parent_id = b.id
ORDER BY 1"


echo "PM - counts for Heart Rate, Height, Weight, BMI, Waist Circumference, Hip Circumference"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT
              concept_id
            , COUNT(DISTINCT person_id) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE concept_id in (903126,903133,903121,903124,903135,903136)
            and is_standard = 0
        GROUP BY 1
    ) y
WHERE x.domain_id = 'PHYSICAL_MEASUREMENT'
    and x.concept_id = y.concept_id"

echo "PM - counts for heart rhythm, pregnancy, wheelchair use"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT
              concept_id
            , CAST(value_as_concept_id as STRING) as value
            , COUNT(DISTINCT person_id) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE concept_id IN (1586218, 903120, 903111)
            and is_standard = 0
        GROUP BY 1,2
    ) y
WHERE x.domain_id = 'PHYSICAL_MEASUREMENT'
    and x.concept_id = y.concept_id
    and x.value = y.value"

#----- BLOOD PRESSURE -----
echo "PM - blood pressure - hypotensive"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET item_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic <= 90
                and diastolic <= 60
        )
    , est_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic <= 90
                and diastolic <= 60
        )
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
    and subtype = 'BP'
    and name LIKE 'Hypotensive%'"

echo "PM - blood pressure - normal"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET item_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic <= 120
                and diastolic <= 80
        )
    , est_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic <= 120
                and diastolic <= 80
        )
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
    and subtype = 'BP'
    and name LIKE 'Normal%'"

echo "PM - blood pressure - pre-hypertensive"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET item_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic BETWEEN 120 AND 139
                and diastolic BETWEEN 81 AND 89
        )
    , est_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic BETWEEN 120 AND 139
                and diastolic BETWEEN 81 AND 89
        )
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
    and subtype = 'BP'
    and name LIKE 'Pre-Hypertensive%'"

echo "PM - blood pressure  - hypertensive"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET item_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic >= 140
                and diastolic >= 90
        )
    , est_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
                and systolic >= 140
                and diastolic >= 90
        )
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
    and subtype = 'BP'
    and name LIKE 'Hypertensive%'"

echo "PM - blood pressure  - detail"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
SET item_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
        )
    , est_count =
        (
            SELECT COUNT(DISTINCT person_id)
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
            WHERE concept_id in (903115, 903118)
                and is_standard = 0
        )
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
    and subtype = 'BP'
    and name = 'Blood Pressure'
    and is_selectable = 1"

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
if [[ "$RUN_PARALLEL" == "mult" ]]; then
  cpToMain "$TBL_CBC" &
  wait
fi

