#!/bin/bash
# set -ex
# do not output cmd-line for now
set -e
SQL_FOR='POPULATE OTHER CB_* TABLES'

####### common block for all make-cb-criteria-dd-*.sh scripts ###########
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
# export DATA_BROWSER=$3      # data browser flag
# MUST BE RUN AFTER ALL PARALLEL COMPLETES AND
# make-cb-criteria-21-seq-01-add-in-missing-codes.sh COMPLETES
####### end common block ###########
# make-cb-criteria-22-seq-02-attrib-other-tables.sh
#6151 - #6471: make-bq-criteria-tables.sh
# ORDER - 22.1: #6151 - #6340: - CB_CRITERIA_ATTRIBUTE---------
# ORDER - 22.2: #6342 - #6423: - CB_SURVEY_ATTRIBUTE---------
# ORDER - 22.3: #6428 - #6471: - CB_CRITERIA_RELATIONSHIP---------
################################################
# CB_CRITERIA_ATTRIBUTE
################################################
# ORDER - 22.1: #6151 - #6340: - CB_CRITERIA_ATTRIBUTE---------
# 22.10: #6151: CB_CRITERIA_ATTRIBUTE - PPI SURVEY - add values for certain questions
  #cb_criteria_attribute: Uses : cb_criteria_attribute
# 22.11: #6225: CB_CRITERIA_ATTRIBUTE - Measurements - add numeric results
  #cb_criteria_attribute: Uses : cb_criteria_attribute, measurement, cb_criteria
# 22.12: #6286: CB_CRITERIA_ATTRIBUTE - Measurements - add categorical results
  #cb_criteria_attribute: Uses : cb_criteria_attribute, measurement, concept, cb_criteria
# 22.12: #6328 : CB_CRITERIA - update has_attribute
#cb_criteria: Uses : cb_criteria, cb_criteria_attribute
# we hard code the min/max for these PPI questions because we can't get the information programmatically
echo "CB_CRITERIA_ATTRIBUTE - PPI SURVEY - add values for certain questions"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
    (
          id
        , concept_id
        , value_as_concept_id
        , concept_name
        , type
        , est_count
    )
VALUES
    (1, 1585889, 0, 'MIN', 'NUM', '0'),
    (2, 1585889, 0, 'MAX', 'NUM', '20'),
    (3, 1585890, 0, 'MIN', 'NUM', '0'),
    (4, 1585890, 0, 'MAX', 'NUM', '20'),
    (5, 1585864, 0, 'MIN', 'NUM', '0'),
    (6, 1585864, 0, 'MAX', 'NUM', '99'),
    (7, 1585870, 0, 'MIN', 'NUM', '0'),
    (8, 1585870, 0, 'MAX', 'NUM', '99'),
    (9, 1585873, 0, 'MIN', 'NUM', '0'),
    (10, 1585873, 0, 'MAX', 'NUM', '99'),
    (11, 1586159, 0, 'MIN', 'NUM', '0'),
    (12, 1586159, 0, 'MAX', 'NUM', '99'),
    (13, 1586162, 0, 'MIN', 'NUM', '0'),
    (14, 1586162, 0, 'MAX', 'NUM', '99'),
    (15, 1585795, 0, 'MIN', 'NUM', '0'),
    (16, 1585795, 0, 'MAX', 'NUM', '99'),
    (17, 1585802, 0, 'MIN', 'NUM', '0'),
    (18, 1585802, 0, 'MAX', 'NUM', '99'),
    (19, 1585820, 0, 'MIN', 'NUM', '0'),
    (20, 1585820, 0, 'MAX', 'NUM', '255'),
    (21, 1333015, 0, 'MIN', 'NUM', '0'),
    (22, 1333015, 0, 'MAX', 'NUM', '20'),
    (23, 1333023, 0, 'MIN', 'NUM', '1'),
    (24, 1333023, 0, 'MAX', 'NUM', '20'),
    (25, 715717, 0, 'MIN', 'NUM', '0'),
    (26, 715717, 0, 'MAX', 'NUM', '24'),
    (27, 903642, 0, 'MIN', 'NUM', '0'),
    (28, 903642, 0, 'MAX', 'NUM', '1440'),
    (29, 715721, 0, 'MIN', 'NUM', '1'),
    (30, 715721, 0, 'MAX', 'NUM', '99'),
    (31, 715720, 0, 'MIN', 'NUM', '1'),
    (32, 715720, 0, 'MAX', 'NUM', '12'),
    (33, 715719, 0, 'MIN', 'NUM', '1'),
    (34, 715719, 0, 'MAX', 'NUM', '52'),
    (35, 1332870, 0, 'MIN', 'NUM', '1'),
    (36, 1332870, 0, 'MAX', 'NUM', '7'),
    (37, 903633, 0, 'MIN', 'NUM', '0'),
    (38, 903633, 0, 'MAX', 'NUM', '24'),
    (39, 715712, 0, 'MIN', 'NUM', '0'),
    (40, 715712, 0, 'MAX', 'NUM', '1440'),
    (41, 1332871, 0, 'MIN', 'NUM', '1'),
    (42, 1332871, 0, 'MAX', 'NUM', '7'),
    (43, 903634, 0, 'MIN', 'NUM', '0'),
    (44, 903634, 0, 'MAX', 'NUM', '24'),
    (45, 715715, 0, 'MIN', 'NUM', '0'),
    (46, 715715, 0, 'MAX', 'NUM', '1440'),
    (47, 1332872, 0, 'MIN', 'NUM', '1'),
    (48, 1332872, 0, 'MAX', 'NUM', '7'),
    (49, 903635, 0, 'MIN', 'NUM', '0'),
    (50, 903635, 0, 'MAX', 'NUM', '7'),
    (51, 715716, 0, 'MIN', 'NUM', '0'),
    (52, 715716, 0, 'MAX', 'NUM', '1440'),
    (53, 715718, 0, 'MIN', 'NUM', '0'),
    (54, 715718, 0, 'MAX', 'NUM', '1440'),
    (55, 715723, 0, 'MIN', 'NUM', '1'),
    (56, 715723, 0, 'MAX', 'NUM', '99'),
    (57, 715713, 0, 'MIN', 'NUM', '1'),
    (58, 715713, 0, 'MAX', 'NUM', '12'),
    (59, 715722, 0, 'MIN', 'NUM', '1'),
    (60, 715722, 0, 'MAX', 'NUM', '52')
"

# this will add the min/max values for all numeric measurement concepts
# this code will filter out any labs WHERE all results = 0
echo "CB_CRITERIA_ATTRIBUTE - Measurements - add numeric results"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            )
            and value_as_number is not null
        GROUP BY 1
        HAVING NOT (min(value_as_number) = 0 and max(value_as_number) = 0)
    ) a"

# this will add all categorical values for all measurement concepts where value_as_concept_id is valid
echo "CB_CRITERIA_ATTRIBUTE - Measurements - add categorical results"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            )
            and value_as_concept_id != 0
            and value_as_concept_id is not null
        GROUP BY 1,2,3
    ) a"


# set has_attribute=1 for any measurement criteria that has data in cb_criteria_attribute
echo "CB_CRITERIA - update has_attribute"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
# ORDER - 22.2: #6342 - #6423: - CB_SURVEY_ATTRIBUTE---------
# 22.20: #6342: CB_SURVEY_ATTRIBUTE - adding data for questions
  #cb_survey_attribute: Uses : cb_survey_attribute, cb_search_all_events, cb_criteria
# 22.21: #6380: CB_SURVEY_ATTRIBUTE - adding data for questions
  #cb_survey_attribute: Uses : cb_survey_attribute, cb_search_all_events, cb_criteria
# 22.23: #6411: CB_CRITERIA - update has_attribute
  #cb_criteria: Uses : cb_criteria, cb_survey_attribute
echo "CB_SURVEY_ATTRIBUTE - adding data for questions"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
# ORDER - 22.3: #6428 - #6471: - CB_CRITERIA_RELATIONSHIP---------
# 22.30: #6428: CB_CRITERIA_RELATIONSHIP - Drugs - add drug/ingredient relationship
  #cb_criteria_relationship: Uses : cb_criteria_relationship, concept_relationship, cb_criteria
# 22.31: #6449: CB_CRITERIA_RELATIONSHIP - Source Concept -> Standard Concept Mapping
  #cb_criteria_relationship: Uses : cb_criteria_relationship, concept_relationship, cb_criteria
echo "CB_CRITERIA_RELATIONSHIP - Drugs - add drug/ingredient relationships"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
    AND a.relationship_id = 'Maps to'
    AND a.concept_id_1 in
        (
            SELECT DISTINCT concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            WHERE concept_id is not null
                and is_standard = 0
                and domain_id in ('CONDITION', 'PROCEDURE')
        )"
