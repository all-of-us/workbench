#!/bin/bash
# do not output cmd-line for now
set -e
SQL_FOR='DEMOGRAPHICS'
TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-parallel/cb-criteria-utils.sh
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
      + (SELECT COALESCE(MAX(id),$CB_CRITERIA_START_ID) FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` where id > $CB_CRITERIA_START_ID AND id < $CB_CRITERIA_END_ID) AS id
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
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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

#wait for process to end before copying
wait
## copy temp tables back to main tables, and delete temp?
cpToMain "$TBL_CBC" &
wait

