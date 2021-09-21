#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='DEMOGRAPHICS'
SQL_SCRIPT_ORDER=7
TBL_CBC='cb_criteria'
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
fi
####### end common block ###########
# make-cb-criteria-06-demographics.sh
#1009 - #1254 : make-bq-criteria-tables.sh
# ---------ORDER - 7 - DEMOGRAPHICS ---------
# Order 7: #1009 - #1254: DEMOGRAPHICS age, deceased, gender, sex at birth, race, ethnicity
# cb_criteria: Uses : cb_criteria, Death, person, concept
################################################
# DEMOGRAPHICS
################################################
echo "DEMOGRAPHICS - Age"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
    , 'PERSON'
    , 1
    , 'AGE'
    , 'Age'
    , 1
    , 0
    , 0
    , 0"

echo "DEMOGRAPHICS - Deceased"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) + 1 AS id
    , -1
    , 'PERSON'
    , 1
    , 'DECEASED'
    , 'Deceased'
    , 0
    , COUNT(DISTINCT person_id)
    , COUNT(DISTINCT person_id)
    , 0
    , 1
    , 0
    , 0
FROM \`$BQ_PROJECT.$BQ_DATASET.death\`"

echo "DEMOGRAPHICS - Gender Identity"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      ROW_NUMBER() OVER(ORDER BY a.cnt DESC) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , -1
    , 'PERSON'
    , 1
    , 'GENDER'
    , concept_id
    , CASE
          WHEN b.concept_id = 0 THEN 'Unknown'
          ELSE regexp_replace(b.concept_name, r'^.+:\s', '')
      END as name
    , 0
    , a.cnt
    , a.cnt
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT gender_concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\`
        GROUP BY 1
    ) a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.gender_concept_id = b.concept_id"

#if [[ "$DATA_BROWSER" == false ]]
#then
  echo "DEMOGRAPHICS - Sex at Birth"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
      (
            id
          , parent_id
          , domain_id
          , is_standard
          , type
          , concept_id
          , name
          , rollup_count
          , item_count
          , est_count
          , is_group
          , is_selectable
          , has_attribute
          , has_hierarchy
      )
  SELECT
      ROW_NUMBER() OVER(ORDER BY a.cnt DESC)
        + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
      , -1
      , 'PERSON'
      , 1
      , 'SEX'
      , concept_id
      , CASE
            WHEN b.concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(b.concept_name, r'^.+:\s', '')
        END as name
      , 0
      , a.cnt
      , a.cnt
      , 0
      , 1
      , 0
      , 0
  FROM
      (
          SELECT sex_at_birth_concept_id, COUNT(DISTINCT person_id) cnt
          FROM \`$BQ_PROJECT.$BQ_DATASET.person\`
          GROUP BY 1
      ) a
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.sex_at_birth_concept_id = b.concept_id"
#fi

echo "DEMOGRAPHICS - Race"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY a.cnt DESC)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , -1
    , 'PERSON'
    , 1
    , 'RACE'
    , concept_id
    , CASE
          WHEN a.race_concept_id = 0 THEN 'Unknown'
          ELSE regexp_replace(b.concept_name, r'^.+:\s', '')
      END as name
    , 0
    , a.cnt
    , a.cnt
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT race_concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\`
        GROUP BY 1
    ) a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.race_concept_id = b.concept_id
WHERE b.concept_id is not null"

echo "DEMOGRAPHICS - Ethnicity"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , rollup_count
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY a.cnt DESC)
      + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
    , 0
    , 'PERSON'
    , 1
    , 'ETHNICITY'
    , concept_id
    , regexp_replace(b.concept_name, r'^.+:\s', '')
    , 0
    , a.cnt
    , a.cnt
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT ethnicity_concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\`
        GROUP BY 1
    ) a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.ethnicity_concept_id = b.concept_id
WHERE b.concept_id is not null"

wait $!
## copy temp tables back to main tables, and delete temp?
if [[ "$RUN_PARALLEL" == "mult" ]]; then
  cpToMain "$TBL_CBC" &
fi
