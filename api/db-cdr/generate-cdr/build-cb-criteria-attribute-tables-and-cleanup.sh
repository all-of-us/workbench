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