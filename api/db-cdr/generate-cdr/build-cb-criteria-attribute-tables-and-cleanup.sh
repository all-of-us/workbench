#!/bin/bash

set -e
SQL_FOR='POPULATE OTHER CB_* TABLES'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "CB_CRITERIA_ATTRIBUTE - PPI SURVEY - add values for certain questions"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`) as id
    , concept_id
    , value_as_concept_id
    , concept_name
    , type
    , cnt
FROM
    (
        SELECT
              observation_source_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MIN' as concept_name
            , 'NUM' as type
            , CAST(MIN(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
        WHERE observation_source_concept_id in
            (
                SELECT concept_id
                from \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
                WHERE subtype = 'ANSWER'
                and name = 'Select a value'
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)

        UNION ALL
        SELECT
              observation_source_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MAX' as concept_name
            , 'NUM' as type
            , CAST(max(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
        WHERE observation_source_concept_id in
            (
                SELECT concept_id
                from \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
                WHERE subtype = 'ANSWER'
                and name = 'Select a value'
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)
    ) a"

# this will add the min/max values for all numeric measurement concepts
# this code will filter out any labs WHERE all results = 0
echo "CB_CRITERIA_ATTRIBUTE - Measurements - add numeric results"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`) as id
    , concept_id
    , value_as_concept_id
    , concept_name
    , type
    , cnt
FROM
    (
        SELECT
              measurement_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MIN' as concept_name
            , 'NUM' as type
            , CAST(MIN(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        WHERE measurement_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'MEASUREMENT'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)

        UNION ALL

        SELECT
              measurement_concept_id as concept_id
            , 0 as value_as_concept_id
            , 'MAX' as concept_name
            , 'NUM' as type
            , CAST(max(value_as_number) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        WHERE measurement_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'MEASUREMENT'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)
    ) a"

# this will add all categorical values for all measurement concepts where value_as_concept_id is valid
echo "CB_CRITERIA_ATTRIBUTE - Measurements - add categorical results"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`) as id
    , concept_id
    , value_as_concept_id
    , concept_name
    , type
    , cnt
FROM
    (
        SELECT
              measurement_concept_id as concept_id
            , value_as_concept_id
            , b.concept_name
            , 'CAT' as type
            , CAST(COUNT(DISTINCT person_id) as STRING) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_as_concept_id = b.concept_id
        WHERE measurement_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'MEASUREMENT'
                    and is_group = 0
                    and is_standard = 1
            )
            and value_as_concept_id != 0
            and value_as_concept_id is not null
        GROUP BY 1,2,3
    ) a"


# set has_attribute=1 for any measurement criteria that has data in cb_criteria_attribute
echo "CB_CRITERIA - update has_attribute"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET has_attribute = 1
WHERE domain_id = 'MEASUREMENT'
    and is_selectable = 1
    and is_standard = 1
    and concept_id in
    (
        SELECT DISTINCT concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
    )"

################################################
# CB_SURVEY_ATTRIBUTE
################################################
echo "CB_SURVEY_ATTRIBUTE - adding data for questions"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`
    (
          id
        , question_concept_id
        , answer_concept_id
        , survey_version_concept_id
        , item_count
    )
SELECT
        ROW_NUMBER() OVER (ORDER BY question_concept_id) as id
      , question_concept_id
      , 0
      , survey_version_concept_id
      , cnt
FROM
    (
        SELECT
              concept_id as question_concept_id
            , survey_version_concept_id
            , count(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE
            concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'SURVEY'
                )
            and survey_version_concept_id is not null
            and is_standard = 0
        GROUP BY 1,2
    )"

echo "CB_SURVEY_ATTRIBUTE - adding data for answers"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`
SELECT
        ROW_NUMBER() OVER (ORDER BY question_concept_id, answer_concept_id) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`) as id
      , question_concept_id
      , answer_concept_id
      , survey_version_concept_id
      , cnt
FROM
    (
        SELECT
              concept_id as question_concept_id
            , value_source_concept_id as answer_concept_id
            , survey_version_concept_id
            , count(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE
            concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'SURVEY'
                )
            and survey_version_concept_id is not null
            and is_standard = 0
            and value_source_concept_id != 0
        GROUP BY 1,2,3
    )"

# set has_attribute=1 for any criteria that has data in cb_survey_attribute
echo "CB_SURVEY - update has_attribute"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET has_attribute = 1
WHERE domain_id = 'SURVEY'
    and is_selectable = 1
    and concept_id in
    (
        SELECT DISTINCT question_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`
    )"

echo "cb_criteria - delete duplicated questions from COVID-19 Participant Experience (COPE) Survey"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
 WHERE id IN (
   SELECT id
       FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
       WHERE concept_id IN (
         SELECT concept_id FROM (
           SELECT concept_id, COUNT(*)
           FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
           WHERE domain_id = 'SURVEY'
           AND subtype = 'QUESTION'
           GROUP BY concept_id
           HAVING COUNT(*) > 1
         )
       )
       AND path LIKE (
         SELECT CONCAT(id, '.%')
         FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
         WHERE domain_id = 'SURVEY'
         AND LOWER(code) = 'cope'
       )
 )"

################################################
# CB_CRITERIA_RELATIONSHIP
################################################
echo "CB_CRITERIA_RELATIONSHIP - Drugs - add drug/ingredient relationships"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`
    (
          concept_id_1
        , concept_id_2
    )
SELECT
      a.concept_id_1
    , a.concept_id_2
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id_2 = b.concept_id
WHERE b.concept_class_id = 'Ingredient'
    and a.concept_id_1 in
        (
            SELECT concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            WHERE domain_id = 'DRUG'
                and type = 'BRAND'
        )"

echo "CB_CRITERIA_RELATIONSHIP - Source Concept -> Standard Concept Mapping"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`
    (
          concept_id_1
        , concept_id_2
    )
SELECT
      a.concept_id_1 as source_concept_id
    , a.concept_id_2 as standard_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id_2 = b.concept_id
WHERE b.standard_concept = 'S'
    AND b.domain_id in ('Condition', 'Procedure', 'Measurement', 'Observation', 'Drug')
    AND a.relationship_id in ('Maps to', 'Maps to value')
    AND a.concept_id_1 in
        (
            SELECT DISTINCT concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            WHERE concept_id is not null
                and is_standard = 0
                and domain_id in ('CONDITION', 'PROCEDURE')
                AND type in ('CPT4', 'ICD9CM', 'ICD10CM', 'ICD9Proc', 'ICD10PCS')
        )"

################################################
# DATA CLEAN UP
################################################
echo "CLEAN UP - set rollup_count = -1 WHERE the count is NULL"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET rollup_count = -1
WHERE rollup_count is null"

echo "CLEAN UP - set item_count = -1 WHERE the count is NULL"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET item_count = -1
WHERE item_count is null"

echo "CLEAN UP - set est_count = -1 WHERE the count is NULL"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET est_count = -1
WHERE est_count is null"

echo "CLEAN UP - set has_ancestor_data = 0 for all items WHERE it is currently NULL"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET has_ancestor_data = 0
WHERE has_ancestor_data is null"

echo "CLEAN UP - remove all double quotes FROM criteria names"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET name = REGEXP_REPLACE(name, r'[\"]', '')
WHERE REGEXP_CONTAINS(name, r'[\"]')"

echo "CLEAN UP - remove any survey with zero est_count"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id = 'SURVEY'
AND est_count = 0"

echo "CLEAN UP - remove any empty survey topics"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE
 FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
 WHERE id in (
  SELECT id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE domain_id = 'SURVEY'
          AND subtype = 'TOPIC'
 EXCEPT DISTINCT
 SELECT parent_id as id
    FROM (
      SELECT parent_id, COUNT(*) AS count
      FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
      WHERE parent_id IN (
        SELECT id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE domain_id = 'SURVEY'
          AND subtype = 'TOPIC')
      GROUP BY parent_id)
 )"

echo "CLEAN UP - remove any special characters ’ or … and replace with ' and ..."
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
    SELECT id, REPLACE (name, '’', '\'') AS name
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    WHERE name LIKE '%’%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, '…', '...') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%…%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'Ђ™', '\'') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%Ђ™%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'ö', 'o') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%ö%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, '“drink,”', '\"drink,\"') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%“drink,”%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'é', 'e') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%é%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'è', 'e') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%è%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'ä', 'a') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%ä%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'ü', 'u') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%ü%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, '－', '-') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%－%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'â', 'a') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%â%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'ž', 'z') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%ž%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, '·', '') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%·%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'Â', '') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%Â%') cr2
 WHERE cr1.id = cr2.id"

bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` cr1
 SET cr1.name = cr2.name
 FROM (
   SELECT id, REPLACE (name, 'É', 'E') AS name
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
   WHERE name LIKE '%É%') cr2
 WHERE cr1.id = cr2.id"