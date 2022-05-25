#!/bin/bash

set -e
SQL_FOR='DEMOGRAPHICS'
TBL_CBC='cb_criteria'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
ID_PREFIX=$3
export DATA_BROWSER=$4      # data browser flag

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
source ./generate-cdr/cb-criteria-utils.sh
echo "Running in parallel and Multitable mode - " "$ID_PREFIX - $SQL_FOR"
CB_CRITERIA_START_ID=$[$ID_PREFIX*10**9] # 3  billion
CB_CRITERIA_END_ID=$[$[ID_PREFIX+1]*10**9] # 4  billion
echo "Creating temp table for $TBL_CBC"
TBL_CBC=$(createTmpTable $TBL_CBC)
####### end common block ###########

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

# Need to use the concept name for the source concept id since the survey uses this name
# https://precisionmedicineinitiative.atlassian.net/browse/RW-8178
echo "CB_CRITERIA - update gender identity name for Woman"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` cr
 SET cr.name = y.name
 FROM (SELECT DISTINCT gender_concept_id as concept_id, REGEXP_REPLACE(c.concept_name, r'^.+:\s', '') as name
             FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
             JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on p.gender_source_concept_id = c.concept_id
             WHERE p.gender_concept_id = 45878463) y
 WHERE cr.concept_id = y.concept_id
 AND domain_id = 'PERSON'
 AND type = 'GENDER'"

 echo "CB_CRITERIA - update gender identity name for Man"
 bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
 "UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\` cr
  SET cr.name = y.name
  FROM (SELECT DISTINCT gender_concept_id as concept_id, REGEXP_REPLACE(c.concept_name, r'^.+:\s', '') as name
              FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
              JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on p.gender_source_concept_id = c.concept_id
              WHERE p.gender_concept_id = 45880669) y
  WHERE cr.concept_id = y.concept_id
  AND domain_id = 'PERSON'
  AND type = 'GENDER'"

if [[ "$DATA_BROWSER" == false ]]
then
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
fi

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

## wait for process to end before copying
wait
## copy tmp tables back to main tables and delete tmp
cpToMainAndDeleteTmp "$TBL_CBC"
