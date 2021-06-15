#!/bin/bash

# This generates the criteria tables for the CDR

# PREP: upload all prep tables

# ./project.rb generate-cb-criteria-tables --bq-project aou-res-curation-output-prod --bq-dataset SR2019q4r3

set -ex

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
export DATA_BROWSER=$3      # data browser flag

################################################
# CREATE TABLES
################################################
echo "CREATE TABLES - cb_criteria"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
(
    id                  INT64,
    parent_id           INT64,
    domain_id           STRING,
    is_standard         INT64,
    type                STRING,
    subtype             STRING,
    concept_id          INT64,
    code                STRING,
    name                STRING,
    value               STRING,
    rollup_count        INT64,
    item_count          INT64,
    est_count           INT64,
    is_group            INT64,
    is_selectable       INT64,
    has_attribute       INT64,
    has_hierarchy       INT64,
    has_ancestor_data   INT64,
    path                STRING,
    synonyms            STRING,
    full_text           STRING,
    display_synonyms    STRING
)"

# table that holds the ingredient --> coded drugs mapping
echo "CREATE TABLES - cb_criteria_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`
(
    ancestor_id INT64,
    descendant_id INT64
)"

# table that holds categorical results and min/max information about individual labs
echo "CREATE TABLES - cb_criteria_attribute"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
(
    id                    INT64,
    concept_id            INT64,
    value_as_concept_id	  INT64,
    concept_name          STRING,
    type                  STRING,
    est_count             STRING
)"

# table that holds which survey versions each question and answer are apart of
echo "CREATE TABLES - cb_survey_attribute"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`
(
      id                    INT64
    , question_concept_id   INT64
    , answer_concept_id     INT64
    , survey_version_concept_id INT64
    , item_count            INT64
)"

# table that holds the drug brands to ingredients relationship mapping
# also holds source concept --> standard concept mapping information
echo "CREATE TABLES - cb_criteria_relationship"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`
(
    concept_id_1 INT64,
    concept_id_2 INT64
)"

# staging table to make it easier to add data into prep_concept_ancestor
echo "CREATE TABLES - prep_ancestor_staging"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
(
    ancestor_concept_id     INT64,
    domain_id               STRING,
    type                    STRING,
    is_standard             INT64,
    concept_id_1            INT64,
    concept_id_2            INT64,
    concept_id_3            INT64,
    concept_id_4            INT64,
    concept_id_5            INT64,
    concept_id_6            INT64,
    concept_id_7            INT64,
    concept_id_8            INT64,
    concept_id_9            INT64,
    concept_id_10           INT64,
    concept_id_11           INT64,
    concept_id_12           INT64,
    concept_id_13           INT64,
    concept_id_14           INT64,
    concept_id_15           INT64,
    concept_id_16           INT64,
    concept_id_17           INT64,
    concept_id_18           INT64,
    concept_id_19           INT64,
    concept_id_20           INT64
)"

# table that holds ancestor information for parent counts
echo "CREATE TABLES - prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
(
    ancestor_concept_id     INT64,
    descendant_concept_id   INT64,
    is_standard             INT64
)"

# table that holds CPT4 ancestor information for parent counts
echo "CREATE TABLES - prep_cpt_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_cpt_ancestor\`
(
    ancestor_id     INT64,
    descendant_id   INT64
)"

# holds atc and rxnorm concept relationships for drugs
echo "CREATE TABLES - prep_atc_rel_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds LOINC concept relationship data for measurements
echo "CREATE TABLES - prep_loinc_rel_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds standard snomed concept relationship data for conditions
echo "CREATE TABLES - prep_snomed_rel_cm_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds source snomed concept relationship data for conditions
echo "CREATE TABLES - prep_snomed_rel_cm_src_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds standard snomed concept relationship data for measurements
echo "CREATE TABLES - prep_snomed_rel_meas_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds standard snomed concept relationship data for procedures
echo "CREATE TABLES - prep_snomed_rel_pcs_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# holds source snomed concept relationship data for procedures
echo "CREATE TABLES - prep_snomed_rel_pcs_src_in_data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
(
    p_concept_id    INT64,
    p_concept_code  STRING,
    p_concept_name  STRING,
    p_domain_id     STRING,
    concept_id      INT64,
    concept_code    STRING,
    concept_name    STRING,
    domain_id       STRING
)"

# create merged table of concept and prep_concept
echo "CREATE TABLES - prep_concept_merged"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` AS
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
UNION ALL
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept\`"

# create merged table of concept_relationship and prep_concept_relationship
echo "CREATE TABLES - prep_concept_relationship_merged"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\` AS
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\`
UNION ALL
SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship\`"


################################################
# CPT4 - SOURCE
################################################
echo "CPT4 - SOURCE - insert data (do not insert zero count children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
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
      a.id
    , a.parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , a.code
    , CASE WHEN b.concept_id is not null THEN b.concept_name ELSE a.name END AS name
    , CASE WHEN a.parent_id != 0 THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.parent_id != 0 THEN
            CASE
                WHEN c.cnt is null THEN 0
                ELSE c.cnt
            END
        ELSE null
      END AS item_count
    , CASE WHEN a.is_group = 0 and a.is_selectable = 1 THEN c.cnt ELSE null END AS est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , a.path
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
LEFT JOIN
    (
        SELECT concept_id, concept_name
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id = 'CPT4'
    ) b on a.concept_id = b.concept_id
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts that are selectable
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
                    WHERE type = 'CPT4'
                        and is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE a.type = 'CPT4'
    AND
        (
            -- get all groups and get all children that have a count
            is_group = 1
            OR
            (
                is_group = 0
                AND is_selectable = 1
                AND
                    (
                        c.cnt != 0
                        OR c.cnt is not null
                    )
            )
      )
ORDER BY 1"

echo "CPT4 - SOURCE - add ancestor data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_cpt_ancestor\`
    (
          ancestor_id
        , descendant_id
    )
SELECT
      DISTINCT a.id ancestor_id
    , coalesce(h.id, g.id, f.id, e.id, d.id, c.id, b.id) descendant_id
FROM (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'CPT4' and is_standard = 0 and is_group = 1 and parent_id != 0) a
JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) b on a.id = b.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) c on b.id = c.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) d on c.id = d.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) e on d.id = e.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) f on e.id = f.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) g on f.id = g.parent_id
LEFT JOIN (SELECT id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) h on g.id = h.parent_id"

echo "CPT4 - SOURCE - generate parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each group, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all groups except the top level
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'CPT4'
                            and parent_id != 0
                            and is_group = 1
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_cpt_ancestor\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type = 'CPT4'
                    and c.is_selectable = 1
                    and d.is_standard = 0
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "CPT4 - SOURCE - delete zero count parents"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type = 'CPT4'
    and is_group = 1
    and
        (
            (parent_id != 0 and rollup_count = 0)
            or id not in
                (
                    SELECT parent_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE type = 'CPT4'
                )
        )"


################################################
# PPI PHYSICAL MEASUREMENTS (PM)
################################################
echo "PM - insert data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
FROM (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_physical_measurement\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_physical_measurement\`) b on a.parent_id = b.id
ORDER BY 1"


echo "PM - counts for Heart Rate, Height, Weight, BMI, Waist Circumference, Hip Circumference"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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


################################################
# PPI SURVEYS
################################################
echo "PPI SURVEYS - insert data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , value
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
      a.new_id as id
    , CASE WHEN a.parent_id = 0 THEN 0 ELSE b.new_id END as parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , a.code
    , a.name
    , a.value
    , CASE
        WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
        ELSE null
      END as rollup_count
      , CASE
          WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
          ELSE null
        END as item_count
    , CASE
        WHEN (a.is_selectable = 1 and a.name != 'Select a value') THEN 0
        ELSE null
      END as est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , REGEXP_REPLACE( IFNULL(e.new_id,-1) ||'.'|| IFNULL(d.new_id,-1) ||'.'|| IFNULL(c.new_id,-1) ||'.'|| IFNULL(b.new_id,-1) ||'.'|| IFNULL(a.new_id,-1), '(-1.)*' ,'' ) as path
FROM
    (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) a
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) b on a.parent_id = b.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) c on b.parent_id = c.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) d on c.parent_id = d.id
    LEFT JOIN (SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as new_id, id, parent_id FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) e on d.parent_id = e.id
ORDER BY 1"

echo "PPI SURVEYS - insert extra answers (Skip, Prefer Not To Answer, Dont Know)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY e.id, d.answer) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , e.id as parent_id
    , e.domain_id
    , e.is_standard
    , e.type
    , 'ANSWER'
    , e.concept_id
    , d.answer as name
    , CAST(d.value_source_concept_id as STRING)
    , 0
    , 0
    , 1
    , 0
    , 1
    , CONCAT(e.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY e.id, d.answer) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING))
FROM
    (
        SELECT DISTINCT a.observation_source_concept_id
            , a.value_source_concept_id
            , regexp_replace(b.concept_name, r'^.+:\s', '') as answer  --remove 'PMI: ' from concept name (ex: PMI: Skip)
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_source_concept_id = b.concept_id
        LEFT JOIN  --filter out items already in the table
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'SURVEY'
            ) c on (a.observation_source_concept_id = c.concept_id and CAST(a.value_source_concept_id as STRING) = c.value)
        WHERE a.value_source_concept_id in (903096, 903079, 903087)
            and a.observation_source_concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'SURVEY'
                        and concept_id is not null
                )
            and c.id is null
    ) d
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` e on
    (d.observation_source_concept_id = e.concept_id and e.domain_id = 'SURVEY' and e.is_group = 1)"

# the concept_id of the answer is the concept_id for the question
# we do this because there are a few answers that are attached to a topic and we want to get those as well
echo "PPI SURVEYS - add items to ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT b.concept_id as ancestor_concept_id
    , a.concept_id as descendant_concept_id
    , a.is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on CAST(regexp_extract(a.path, r'^\d+') AS INT64) = b.id
WHERE a.domain_id = 'SURVEY'
    and a.subtype = 'ANSWER'"

echo "PPI SURVEYS - generate answer counts for all questions EXCEPT where question concept_id = 1585747"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, CAST(value_source_concept_id as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'SURVEY'
                        and type = 'PPI'
                        and subtype = 'ANSWER'
                        and concept_id != 1585747
                )
        GROUP BY 1,2
        ORDER BY 1,2
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.subtype = 'ANSWER'
    and x.concept_id = y.concept_id
    and x.value = y.value"

echo "PPI SURVEYS - generate answer counts for question concept_id = 1585747"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, CAST(value_as_number as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id = 1585747
            and value_as_number is not null
        GROUP BY 1,2

        UNION ALL

        SELECT concept_id, CAST(value_source_concept_id as STRING) as value, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id = 1585747
            and value_source_concept_id != 0
        GROUP BY 1,2
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.subtype = 'ANSWER'
    and x.concept_id = y.concept_id
    and x.value = y.value"

echo "PPI SURVEYS - generate question counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'SURVEY'
                        and type = 'PPI'
                        and is_group = 1
                        and is_selectable = 1
                        and parent_id != 0
                )
        GROUP BY 1
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.type = 'PPI'
    and x.is_group = 1
    and x.concept_id = y.concept_id"

echo "PPI SURVEYS - generate survey counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT b.ancestor_concept_id, count(DISTINCT a.person_id) as cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'SURVEY'
                            and parent_id = 0
                    )
            ) b on a.concept_id = b.descendant_concept_id
        WHERE a.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.domain_id = 'SURVEY'
    and x.concept_id = y.ancestor_concept_id"


################################################
# PHYSICAL MEASUREMENTS - CONCEPT SET
################################################
echo "PHYSICAL MEASUREMENTS - CONCEPT SET"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
      ROW_NUMBER() OVER (ORDER BY concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
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
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_name) +
      (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
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


################################################
# FITBIT DATA
################################################
echo "FITBIT DATA"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , -1
    , 'FITBIT'
    , 1
    , 'FITBIT'
    , 'Fitbit'
    , 1
    , 0
    , 0
    , 0"

################################################
# WHOLE GENOME VARIANT DATA
################################################
echo "WHOLE GENOME VARIANT DATA"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , -1
    , 'WHOLE_GENOME_VARIANT'
    , 1
    , 'WHOLE_GENOME_VARIANT'
    , 'Whole Genome Variant'
    , 1
    , 0
    , 0
    , 0"

################################################
# DEMOGRAPHICS
################################################
echo "DEMOGRAPHICS - Age"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
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
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
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
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      ROW_NUMBER() OVER(ORDER BY a.cnt DESC) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
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

if [ "$DATA_BROWSER" == false ]
then
  echo "DEMOGRAPHICS - Sex at Birth"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
      ROW_NUMBER() OVER(ORDER BY a.cnt DESC) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
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
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    ROW_NUMBER() OVER(ORDER BY a.cnt DESC) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
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
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    ROW_NUMBER() OVER(ORDER BY a.cnt DESC) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
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


################################################
# VISIT_OCCURRENCE (VISITS/ENCOUNTERS)
################################################
echo "VISIT_OCCURRENCE - add items with counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
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
    ROW_NUMBER() OVER(ORDER BY concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , -1
    , 'VISIT'
    , 1
    , 'VISIT'
    , concept_id
    , concept_name
    , 0
    , a.cnt
    , a.cnt
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT b.concept_id, b.concept_name, COUNT(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b ON a.visit_concept_id = b.concept_id
        WHERE b.domain_id = 'Visit'
            and b.standard_concept = 'S'
        GROUP BY 1, 2
    ) a"


################################################
# ICD9 - SOURCE
################################################
echo "ICD9 - SOURCE - inserting roots"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , domain_id
    , 0
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_id) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
-- these are the four root nodes
WHERE concept_id in (2500000024, 2500000023,2500000025,2500000080)"

echo "ICD9 - SOURCE - inserting level 2 (only groups at this level)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , 1
    , 0
    , 0
    , 1
    ,CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
-- in order to get level 2, we will link it from its level 1 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type in ('ICD9CM', 'ICD9Proc')
            and parent_id = 0
    ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on x.concept_id_2 = c.concept_id"

echo "ICD9 - SOURCE - inserting level 3 (only groups at this level)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , item_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
-- in order to get level 3, we will link it from its level 2 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type in ('ICD9CM', 'ICD9Proc')
            and parent_id != 0
            and is_group = 1
            and is_selectable = 0
    ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on  x.concept_id_2 = c.concept_id
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                )
        GROUP BY 1
    ) d on c.concept_id = d.concept_id"

echo "ICD9 - SOURCE - inserting level 4 (parents and children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
      ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , b.id AS parent_id
    , b.domain_id
    , b.is_standard
    , a.vocabulary_id AS type
    , a.concept_id
    , a.concept_code AS code
    , a.concept_name AS name
    , CASE WHEN c.code is null THEN 0 ELSE null END AS rollup_count     -- c.code is null = child
    , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
    , CASE WHEN c.code is null THEN d.cnt ELSE null END AS est_count
    , CASE WHEN c.code is null THEN 0 ELSE 1 END as is_group
    , 1
    , 0
    , 1
    ,CONCAT(b.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
-- in order to get level 4, we will link it to its level 3 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
            -- level 4 codes have a decimal with 1 digit after (ex: 98.0)
            and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d$')
    ) a
-- in order to find its parent, which is just its whole number (ex: 98.0's parent is 98), we will use regex to extract the whole number
JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = b.code and a.vocabulary_id = b.type)
LEFT JOIN
    (
        -- determine if this item is a parent or child by seeing if it has any child items
        -- ex: V09.8 > V09.80 so is_group = 1
        -- ex: E879.5 > nothing so is_group = 0
        SELECT distinct REGEXP_EXTRACT(concept_code, r'^\w{1,}\.\d') code
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
            and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) c on a.concept_code = c.code
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE
    (
        -- get all parents OR get all children that have a count
        c.code is not null
        OR
        (
            c.code is null
            AND d.cnt is not null
        )
    )"

echo "ICD9 - SOURCE - inserting level 5 (children)"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
      ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , CASE WHEN b.id is not null THEN b.id ELSE c.id END AS parent_id
    , CASE WHEN b.domain_id is not null THEN b.domain_id ELSE c.domain_id END as domain_id
    , 0
    , a.vocabulary_id AS type
    , a.concept_id,a.concept_code AS code
    , a.concept_name AS name
    , 0 as rollup_count
    , d.cnt AS item_count
    , d.cnt AS est_count
    , 0
    , 1
    , 0
    , 1
    , CASE
        WHEN b.id is not null THEN
            b.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
        ELSE
            c.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
        END as path
-- in order to get level 5, we will link it to its level 4 parent
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
        -- codes such as 98.01, V09.71, etc.
        and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) a
-- get any level 4 parents that link to this item
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}\.\d') = b.code and a.vocabulary_id = b.type)
-- get any level 3 parents that link to this item (this is because some level 5 items only link to a level 3 item)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = c.code and a.vocabulary_id = c.type)
LEFT JOIN
    (
        -- get the count of distinct patients coded with each concept
        SELECT concept_id, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
            and concept_id in
                (
                    -- get all concepts
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
                    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
                ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE d.cnt is not null"

echo "ICD9 - SOURCE - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
    )
SELECT DISTINCT
      a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc') and is_group = 1 and is_selectable = 1 and is_standard = 0) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc')) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type in ('ICD9CM','ICD9Proc')) c on b.id = c.parent_id"

echo "ICD9 - SOURCE - inserting into prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and type in ('ICD9CM','ICD9Proc')
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and type in ('ICD9CM','ICD9Proc')
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE type in ('ICD9CM','ICD9Proc')
and is_standard = 0"

echo "ICD9 - SOURCE - generate rollup counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type in ('ICD9CM', 'ICD9Proc')
                            and is_standard = 0
                            and is_selectable = 1
                            and is_group = 1
                    )
                    and is_standard = 0
                ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` b on a.descendant_concept_id = b.concept_id
        WHERE b.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type in ('ICD9CM', 'ICD9Proc')
    and x.is_standard = 0
    and x.is_group = 1"

echo "ICD9 - SOURCE - delete parents that have no count"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM\`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type in ('ICD9CM', 'ICD9Proc')
    and is_group = 1
    and is_selectable = 1
    and rollup_count is null"

# TODO there are still some parents that don't actually have any children and never will. WHAT TO DO?


################################################
# ICD10CM - SOURCE
################################################
# some items have multiple parent relationsips which should not happen
# used rank to assign concept to its one-up parent
# ex: Z83.438 has three parents Z83.43, Z83.4, and Z83
echo "ICD10CM - SOURCE - create prep_icd10_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\` AS
SELECT * EXCEPT(rnk)
FROM
    (
        SELECT DISTINCT
              c1.concept_id AS p_concept_id
            , c1.concept_code AS p_concept_code
            , c1.concept_name AS p_concept_name
            , c2.concept_id
            , c2.concept_code
            , c2.concept_name
            , RANK() OVER (PARTITION BY c2.concept_code ORDER BY LENGTH(c1.concept_code) DESC) rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\` cr
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c1 ON cr.concept_id_1 = c1.concept_id
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c2 ON cr.concept_id_2 = c2.concept_id
        WHERE c1.vocabulary_id ='ICD10CM'
            AND c2.vocabulary_id ='ICD10CM'
            AND cr.relationship_id = 'Subsumes'
    )
WHERE rnk =1"

# adding in child items that fell out due to not having a relationship to a parent in concept_relationship
# from the joins below we are going through parents, grandparents, great grandparents to find the first parent that exists
echo "ICD10CM - SOURCE - adding extra child items to prep_icd10_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , concept_id
        , concept_code
        , concept_name
    )
SELECT
      COALESCE(d.concept_id, e.concept_id, f.concept_id) as p_concept_id
    , COALESCE(d.concept_code, e.concept_code, f.concept_code) as p_concept_code
    , COALESCE(d.concept_name, e.concept_name, f.concept_name) as p_concept_name
    , c.concept_id
    , c.concept_code
    , c.concept_name
FROM
    (
        SELECT DISTINCT
              a.concept_id
            , b.concept_code
            , b.vocabulary_id
            , b.concept_name
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            and b.vocabulary_id = 'ICD10CM'
            and a.concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
            )
    ) c
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-1), '.') = d.concept_code and c.vocabulary_id = d.vocabulary_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-2), '.') = e.concept_code and c.vocabulary_id = e.vocabulary_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` f on ( TRIM(LEFT(c.concept_code, LENGTH(c.concept_code)-3), '.') = f.concept_code and c.vocabulary_id = f.vocabulary_id)"

# adding in parent items that fell out due to not having a relationship in concept_relationship
echo "ICD10CM - SOURCE - adding extra parent items to prep_icd10_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , concept_id
        , concept_code
        , concept_name
    )
SELECT
      b.concept_id as p_concept_id
    , b.concept_code as p_concept_name
    , b.concept_name as p_concept_name
    , a.p_concept_id as concept_id
    , a.p_concept_code as concept_code
    , a.p_concept_name as concept_name
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\` a
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on ( TRIM(LEFT(a.p_concept_code, LENGTH(a.p_concept_code)-1), '.') = b.concept_code and b.vocabulary_id = 'ICD10CM' )
WHERE a.p_concept_id NOT IN
    (
        SELECT DISTINCT concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    )
    and a.p_concept_code is not null"

echo "ICD10CM - SOURCE - temp table inserting level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
    (
        p_concept_id    INT64,
        p_concept_code  STRING,
        p_concept_name  STRING,
        concept_id      INT64,
        concept_code    STRING,
        concept_name    STRING
    )
AS SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
WHERE concept_id in
    (
        SELECT DISTINCT a.concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            and b.vocabulary_id = 'ICD10CM'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# we loop one more time that is actually needed
for i in {1..5};
do
    echo "ICD10CM - SOURCE - temp table inserting level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , concept_id
            , concept_code
            , concept_name
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_cm_src\`
    WHERE
        concept_id in
            (
                SELECT p_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
            )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\`
            )"
done

echo "ICD10CM - SOURCE - inserting root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        ,is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , domain_id
    , 0
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_id) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
--- this is the root for ICD10CM
WHERE concept_id = 2500000000"

echo "ICD10CM - SOURCE - inserting second level"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , 1
    , 0
    , 0
    , 1
    ,CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE parent_id = 0
            and type = 'ICD10CM'
    ) p
JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) b on p.concept_id = b.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on  b.concept_id_2 = c.concept_id"

# for each loop, add all items (children/parents) related to the items that were previously added
# only need to loop 5 times, but do 6 to be safe
for i in {1..6};
do
    echo "ICD10CM - SOURCE - inserting level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
          ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , p.domain_id
        , p.is_standard
        , p.type
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10_rel_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.type = 'ICD10CM'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

echo "ICD10CM - SOURCE - add items into ancestor staging to use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4

FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10CM' and is_group = 1 and is_selectable = 1 and is_standard = 0) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10CM') b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10CM') c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10CM') d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10CM') e on d.id = e.parent_id"

echo "ICD10CM - SOURCE - insert into prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and type = 'ICD10CM'
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE type = 'ICD10CM'
and is_standard = 0"

echo "ICD10CM - SOURCE - update item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT a.concept_id, COUNT(distinct a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            AND b.vocabulary_id = 'ICD10CM'
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10CM'
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "ICD10CM - SOURCE - generate rollup counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
                , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                        , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'ICD10CM'
                            and is_standard = 0
                            and is_selectable = 1
                            and is_group = 1
                    )
                    and is_standard = 0
                ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` b on a.descendant_concept_id = b.concept_id
        WHERE b.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10CM'
    and x.is_standard = 0
    and x.is_group = 1"


################################################
# ICD10PCS - SOURCE
################################################
echo "ICD10PCS - SOURCE - create prep_icd10pcs_rel_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src\` AS
SELECT DISTINCT
      c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\` cr
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c1 ON cr.concept_id_1 = c1.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`c2 ON cr.concept_id_2 = c2.concept_id
WHERE c1.vocabulary_id = 'ICD10PCS'
    AND c2.vocabulary_id = 'ICD10PCS'
    AND cr.relationship_id = 'Subsumes'"

echo "ICD10PCS - SOURCE - temp table insert level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\`
        (
            p_concept_id    INT64,
            p_concept_code  STRING,
            p_concept_name  STRING,
            concept_id      INT64,
            concept_code    STRING,
            concept_name    STRING
        )
AS SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src\`
WHERE concept_id in
    (
        SELECT DISTINCT a.concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` b on a.concept_id = b.concept_id
        WHERE a.is_standard = 0
            and b.vocabulary_id = 'ICD10PCS'
    )"


# for each loop, add all items (children/parents) related to the items that were previously added
# we do this one more time than is necessary
for i in {1..7};
do
    echo "ICD10PCS - SOURCE - temp table insert level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\`
        (
             p_concept_id
            , p_concept_code
            , p_concept_name
            , concept_id
            , concept_code
            , concept_name
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src\`
    WHERE
        concept_id in
            (
                SELECT p_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\`
            )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\`
            )"
done

echo "ICD10PCS - SOURCE - adding root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        ,is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , domain_id
    , 0
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER (ORDER BY concept_id) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING)
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\`
-- this is the root concept
WHERE concept_id = 2500000022"

echo "ICD10PCS - SOURCE - adding second level"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        ,is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , p.id AS parent_id
    , p.domain_id
    , p.is_standard
    , p.type
    , c.concept_id AS concept_id
    , c.concept_code AS code
    , c.concept_name AS name
    , 1
    , 0
    , 0
    , 1
    ,CONCAT(p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id,c.concept_code) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE parent_id = 0
            and type = 'ICD10PCS'
    ) p
JOIN
    (
        SELECT concept_id_1,concept_id_2
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_relationship_merged\`
        WHERE relationship_id = 'Subsumes'
    ) b on p.concept_id = b.concept_id_1
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_concept_merged\` c on  b.concept_id_2 = c.concept_id"

# for each loop, add all items (children/parents) related to the items that were previously added
# only need to loop 6 times, but do 7 to be safe
for i in {1..7};
do
    echo "ICD10PCS - SOURCE - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            ,is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , p.domain_id
        , p.is_standard
        , p.type
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.type = 'ICD10PCS'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

echo "ICD10PCS - SOURCE - add items into ancestor staging to use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5

FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS' and is_group = 1 and is_selectable = 1 and is_standard = 0) a
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS') b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS') c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS') d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS') e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'ICD10PCS') f on e.id = f.parent_id"

echo "ICD10PCS - SOURCE - insert into prep_concept_ancestor"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and type = 'ICD10PCS'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and type = 'ICD10PCS'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and type = 'ICD10PCS'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and type = 'ICD10PCS'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and type = 'ICD10PCS'
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE type = 'ICD10PCS'
and is_standard = 0"

echo "ICD10PCS - SOURCE - generate item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT concept_id, COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
        WHERE is_standard = 0
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10PCS'
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "ICD10PCS - SOURCE - generate rollup counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
                , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                        , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'ICD10PCS'
                            and is_standard = 0
                            and is_selectable = 1
                            and is_group = 1
                    )
                    and is_standard = 0
                ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` b on a.descendant_concept_id = b.concept_id
        WHERE b.is_standard = 0
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'ICD10PCS'
    and x.is_standard = 0
    and x.is_group = 1"


################################################
# CONDITION_OCCURRENCE - SNOMED - SOURCE
################################################
echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - create prep_snomed_rel_cm_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c1.domain_id AS p_domain_id
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
    , c2.domain_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND c1.vocabulary_id = 'SNOMED'
    AND c2.vocabulary_id = 'SNOMED'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.domain_id = 'Condition'
    AND c2.domain_id = 'Condition'
    AND cr.relationship_id = 'Subsumes'"

echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , p_domain_id
        , concept_id
        , concept_code
        , concept_name
        , domain_id
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src\`
WHERE concept_id in
    (
        SELECT DISTINCT a.condition_source_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_source_concept_id = b.concept_id
        WHERE a.condition_source_concept_id != 0
            and b.domain_id = 'Condition'
            and b.vocabulary_id = 'SNOMED'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# currently, there are only 6 levels, but we run it 7 times to be safe
for i in {1..7};
do
    echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - temp table adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , p_domain_id
            , concept_id
            , concept_code
            , concept_name
            , domain_id
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src\`
    WHERE
        concept_id in
            (
                SELECT p_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
            )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
            )"
done

echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - adding root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , 0
    , 'CONDITION'
    , 0
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 441840"

echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'CONDITION'
    , 0
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'CONDITION'
    and p.type = 'SNOMED'
    and p.is_standard = 0
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 17 levels, but we run it 18 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..18};
do
    echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'CONDITION'
        , 0
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'CONDITION'
        and p.type = 'SNOMED'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
        , concept_id_13
        , concept_id_14
        , concept_id_15
        , concept_id_16
        , concept_id_17
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id as c12
    , o.concept_id as c13
    , p.concept_id as c14
    , q.concept_id as c15
    , r.concept_id as c16
    , s.concept_id as c17
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0 and parent_id != 0 and is_group = 1) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) n on m.id = n.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) o on n.id = o.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) p on o.id = p.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) q on p.id = q.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) r on q.id = r.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 0) s on r.id = s.parent_id"

# Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - add items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_17 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_17 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_16 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_16 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_15 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_15 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_14 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_14 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_13 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_13 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_12 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_11 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_10 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_9 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 0"

echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT condition_source_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'CONDITION'
                            and type = 'SNOMED'
                            and is_standard = 0
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 0
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` b on a.descendant_concept_id = b.condition_source_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 0
    and x.is_group = 1"


###############################################
# CONDITION_OCCURRENCE - SNOMED - STANDARD
###############################################
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - create prep_snomed_rel_cm"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c1.domain_id AS p_domain_id
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
    , c2.domain_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` r
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND c1.vocabulary_id = 'SNOMED'
    AND c2.vocabulary_id = 'SNOMED'
    AND c1.standard_concept = 'S'
    AND c2.standard_concept = 'S'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.domain_id = 'Condition'
    AND c2.domain_id = 'Condition'
    AND cr.relationship_id = 'Subsumes'"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , p_domain_id
        , concept_id
        , concept_code
        , concept_name
        , domain_id
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm\` a
WHERE concept_id in
    (
        SELECT DISTINCT condition_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_concept_id = b.concept_id
        WHERE a.condition_concept_id != 0
            and b.domain_id = 'Condition'
            and b.standard_concept = 'S'
            and b.vocabulary_id = 'SNOMED'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# currently, there are only 5 levels, but we run it 6 times to be safe
for i in {1..6};
do
    echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - temp table adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , p_domain_id
            , concept_id
            , concept_code
            , concept_name
            , domain_id
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm\` a
    WHERE concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
        )
      and concept_id not in
        (
            SELECT concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
        )"
done

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , 0
    , 'CONDITION'
    , 1
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 441840"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'CONDITION'
    , 1
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'CONDITION'
    and p.type = 'SNOMED'
    and p.is_standard = 1
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 17 levels, but we run it 18 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..18};
do
    echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'CONDITION'
        , 1
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_cm_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'CONDITION'
        and p.type = 'SNOMED'
        and p.is_standard = 1
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Join Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
        , concept_id_13
        , concept_id_14
        , concept_id_15
        , concept_id_16
        , concept_id_17
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id as c12
    , o.concept_id as c13
    , p.concept_id as c14
    , q.concept_id as c15
    , r.concept_id as c16
    , s.concept_id as c17
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1 and parent_id != 0 and is_group = 1) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) n on m.id = n.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) o on n.id = o.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) p on o.id = p.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) q on p.id = q.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) r on q.id = r.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'CONDITION' and type = 'SNOMED' and is_standard = 1) s on r.id = s.parent_id"

# Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_17 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_17 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_16 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_16 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_15 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_15 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_14 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_14 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_13 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_13 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_12 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_11 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_10 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_9 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE domain_id = 'CONDITION'
    and type = 'SNOMED'
    and is_standard = 1"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT condition_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'CONDITION'
                            and type = 'SNOMED'
                            and is_standard = 1
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` b on a.descendant_concept_id = b.condition_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'CONDITION'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_group = 1"


################################################
# MEASUREMENT - Clinical - STANDARD LOINC
################################################
echo "MEASUREMENT - Clinical - STANDARD LOINC - add root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as id
    , 0
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'CLIN'
    , 36207527
    , 'LP248771-0'
    , 'Clinical'
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS STRING)"

echo "MEASUREMENT - Clinical - STANDARD LOINC - add parents"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , name
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT ROW_NUMBER() OVER(ORDER BY name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , (SELECT id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'CLIN') as parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'CLIN'
    , name
    , 1
    , 0
    , 0
    , 1
    , CONCAT( (SELECT id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'CLIN'), '.',
        CAST(ROW_NUMBER() OVER(ORDER BY name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING) )
FROM
    (
        SELECT DISTINCT parent as name
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_clinical_terms_nc\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b using (concept_id)
        WHERE b.concept_id in
            (
                SELECT DISTINCT measurement_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
            )
    ) c"

# this will add all clinical items that have been categorized and added into prep_clinical_terms_nc
echo "MEASUREMENT - Clinical - STANDARD LOINC - add children"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
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
    ROW_NUMBER() OVER(ORDER BY parent_id, concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'CLIN'
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 1
    , CONCAT(parent_path, '.',
        CAST(ROW_NUMBER() OVER(ORDER BY parent_id, concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING))
FROM
    (
        SELECT
              b.concept_name
            , b.concept_id
            , b.concept_code
            , d.id as parent_id
            , d.path as parent_path
            , COUNT(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_clinical_terms_nc\` c on b.concept_id = c.concept_id
        JOIN
            (
                SELECT id, name, path
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE type = 'LOINC'
                    and subtype = 'CLIN'
                    and is_group = 1
            ) d on c.parent = d.name
        WHERE standard_concept = 'S'
            and domain_id = 'Measurement'
            and vocabulary_id = 'LOINC'
        GROUP BY 1,2,3, 4, 5
    ) e"


################################################
# MEASUREMENT - Labs - STANDARD LOINC
################################################
echo "MEASUREMENT - Labs - STANDARD LOINC - create prep_loinc_rel"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` r
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND cr.relationship_id = 'Subsumes'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.vocabulary_id = 'LOINC'
    AND c2.vocabulary_id = 'LOINC'
    AND c1.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')
    AND c2.concept_class_id IN ('LOINC Hierarchy', 'LOINC Component', 'Lab Test')"

echo "MEASUREMENT - Labs - STANDARD LOINC - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , concept_id
        , concept_code
        , concept_name
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel\` a
WHERE concept_id in
    (
        SELECT DISTINCT measurement_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        WHERE measurement_concept_id != 0
            and b.vocabulary_id = 'LOINC'
            and b.standard_concept = 'S'
            and b.domain_id = 'Measurement'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# currently, there are only 4 levels, but we run it 5 times to be safe
for i in {1..5};
do
    echo "MEASUREMENT - Labs - STANDARD LOINC - load temp table adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , concept_id
            , concept_code
            , concept_name
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel\` a
    WHERE concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
        )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
            )"
done

echo "MEASUREMENT - Labs - STANDARD LOINC - add root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as id
    , 0
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , 36206173
    , 'LP29693-6'
    , 'Lab'
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS STRING)"

# add items directly under the root item in the above query
echo "MEASUREMENT - Labs - STANDARD LOINC - add level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , rollup_count
        , item_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT( p.path, '.',
        CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING) )
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` c on p.code = c.p_concept_code
WHERE p.type = 'LOINC'
    and p.subtype = 'LAB'
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 12 levels, but we run it 13 times to be safe
# if this number is changed, you will need to change the number of JOINS in the query below
for i in {1..13};
do
    echo "MEASUREMENT - Labs - STANDARD LOINC - add level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        (
              id
            , parent_id
            , domain_id
            , is_standard
            , type
            , subtype
            , concept_id
            , code
            , name
            , rollup_count
            , item_count
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
          ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'MEASUREMENT'
        , 1
        , 'LOINC'
        , 'LAB'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT( p.path, '.',
            CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING) )
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_loinc_rel_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.type = 'LOINC'
        and p.subtype = 'LAB'
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

echo "MEASUREMENT - Labs - STANDARD LOINC - add parent for un-categorized labs"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , name
        , rollup_count
        , item_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as id
    , a.id as parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , 'Uncategorized'
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT(a.path, '.', CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
WHERE type = 'LOINC'
    and subtype = 'LAB'
    and parent_id = 0"

echo "MEASUREMENT - Labs - STANDARD LOINC - add uncategorized labs"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
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
SELECT ROW_NUMBER() OVER (ORDER BY concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as parent_id
    , 'MEASUREMENT'
    , 1
    , 'LOINC'
    , 'LAB'
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 1
    , CONCAT(
        (SELECT path FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type = 'LOINC' and subtype = 'LAB' and name = 'Uncategorized'), '.',
        CAST(ROW_NUMBER() OVER (ORDER BY concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING) )
FROM
    (
        SELECT concept_id, concept_code, concept_name, COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Measurement'
            and vocabulary_id = 'LOINC'
            and measurement_concept_id not in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE type = 'LOINC'
                        and concept_id is not null
                )
        GROUP BY 1,2,3
    ) x"

# Join Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - Labs - STANDARD LOINC - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id c12
FROM
    (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB' and is_group = 1 and parent_id != 0 and concept_id is not null) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE type = 'LOINC' and subtype = 'LAB') n on m.id = n.parent_id"

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - Labs - STANDARD LOINC - add items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_12 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_11 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_10 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_9 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and type = 'LOINC'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE type = 'LOINC'
    and is_standard = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT measurement_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'LOINC'
    and x.subtype = 'LAB'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - generate parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'LOINC'
                            and subtype = 'LAB'
                            and is_standard = 1
                            and is_group = 1
                            and parent_id != 0
                            and concept_id is not null
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.type = 'LOINC'
    and subtype = 'LAB'
    and x.is_standard = 1
    and x.is_group = 1"

echo "MEASUREMENT - Labs - STANDARD LOINC - generate count for Uncategorized parent"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        WHERE measurement_concept_id IN
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE parent_id IN
                    (
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'LOINC'
                            and subtype = 'LAB'
                            and name = 'Uncategorized'
                    )
            )
    ) y
WHERE x.type = 'LOINC'
    and x.subtype = 'LAB'
    and x.name = 'Uncategorized'"


################################################
# MEASUREMENT - SNOMED - STANDARD
################################################
echo "MEASUREMENT - SNOMED - STANDARD - create table prep_snomed_rel_meas"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c1.domain_id AS p_domain_id
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
    , c2.domain_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND c1.vocabulary_id = 'SNOMED'
    AND c2.vocabulary_id = 'SNOMED'
    AND c1.standard_concept = 'S'
    AND c2.standard_concept = 'S'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.domain_id = 'Measurement'
    AND c2.domain_id = 'Measurement'
    AND cr.relationship_id = 'Subsumes'"

echo "MEASUREMENT - SNOMED - STANDARD - temp table level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , p_domain_id
        , concept_id
        , concept_code
        , concept_name
        , domain_id
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas\` a
WHERE concept_id in
    (
        SELECT DISTINCT measurement_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        WHERE measurement_concept_id != 0
            and b.domain_id = 'Measurement'
            and b.vocabulary_id = 'SNOMED'
            and b.standard_concept = 'S'
    )"

# currently, there are only 4 levels, but we run it 5 times to be safe
for i in {1..4};
do
    echo "MEASUREMENT - SNOMED - STANDARD - temp table level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , p_domain_id
            , concept_id
            , concept_code
            , concept_name
            , domain_id
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas\` a
    WHERE concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
        )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
            )"
done

echo "MEASUREMENT - SNOMED - STANDARD - add roots"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
      ROW_NUMBER() OVER(ORDER BY concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , 0
    , 'MEASUREMENT'
    , 1
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST(ROW_NUMBER() OVER(ORDER BY concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT DISTINCT concept_id, concept_name, concept_code
        FROM
            (
                SELECT d.concept_id, d.concept_name, d.concept_code, RANK() OVER (PARTITION BY c.descendant_concept_id ORDER BY c.max_levels_of_separation DESC) rnk
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on c.ancestor_concept_id = d.concept_id
                WHERE d.domain_id = 'Measurement'
                    and d.vocabulary_id = 'SNOMED'
                    and c.descendant_concept_id in
                        (
                            SELECT DISTINCT concept_id
                            FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
                            JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
                            WHERE standard_concept = 'S'
                                and domain_id = 'Measurement'
                                and vocabulary_id = 'SNOMED'
                        )
            ) a
        WHERE rnk = 1
    ) x"

echo "MEASUREMENT - SNOMED - STANDARD - adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'MEASUREMENT'
    , 1
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 0
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'MEASUREMENT'
    and p.type = 'SNOMED'
    and p.is_standard = 1
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 8 levels, but we run it 9 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..9};
do
    echo "MEASUREMENT - SNOMED - STANDARD - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
                , is_group
                , is_selectable
                , has_attribute
                , has_hierarchy
                , path
            )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'MEASUREMENT'
        , 1
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'MEASUREMENT'
        and p.type = 'SNOMED'
        and p.is_standard = 1
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Join Count: 9 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - SNOMED - STANDARD - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1 and parent_id != 0 and is_group = 1) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'MEASUREMENT' and type = 'SNOMED' and is_standard = 1) i on h.id = i.parent_id"

# Count: 9 - If loop count above is changed, the number of JOINS below must be updated
echo "MEASUREMENT - SNOMED - STANDARD - add items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
-- this statement is to add the ancestor item to itself
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE domain_id = 'MEASUREMENT'
    and type = 'SNOMED'
    and is_standard = 1"

echo "MEASUREMENT - SNOMED - STANDARD - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT measurement_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'MEASUREMENT'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "MEASUREMENT - SNOMED - STANDARD - parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'MEASUREMENT'
                            and type = 'SNOMED'
                            and is_standard = 1
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'MEASUREMENT'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_group = 1"

################################################
# DRUG_EXPOSURE - ATC/RXNORM
################################################
#----- RXNORM / RXNORM EXTENSION -----
# ATC4 - ATC5 --> RXNORM/RXNORM Extension ingredient
# ATC4 - ATC5 --> RXNORM/RXNORM Extension precise ingedient --> RXNORM ingredient
echo "DRUG_EXPOSURE - RXNORM - temp table - ATC4 to ATC5 to RXNORM"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
SELECT distinct e.p_concept_id, e.p_concept_code, e.p_concept_name, e.p_DOMAIN_ID,
    d.CONCEPT_ID, d.CONCEPT_CODE, d.CONCEPT_NAME, d.DOMAIN_ID
from
    (
        SELECT c1.CONCEPT_ID, c1.CONCEPT_CODE, c1.CONCEPT_NAME, c1.DOMAIN_ID, c2.CONCEPT_ID atc_5
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, rxnorm, ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, atc, atc_5th
        WHERE a.RELATIONSHIP_ID IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
            and c1.VOCABULARY_ID = 'RxNorm' and c1.CONCEPT_CLASS_ID = 'Ingredient' and c1.STANDARD_CONCEPT = 'S'
            and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 5th' and c2.STANDARD_CONCEPT = 'C'
            and c1.concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
        UNION ALL
        SELECT c1.CONCEPT_ID, c1.CONCEPT_CODE, c1.CONCEPT_NAME, c1.DOMAIN_ID, c3.CONCEPT_ID atc_5
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, rxnorm, ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, rxnorm, precise ingredient
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` b on a.CONCEPT_ID_2 = b.CONCEPT_ID_1
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on b.CONCEPT_ID_2 = c3.CONCEPT_ID--child, atc, atc_5th
        WHERE a.RELATIONSHIP_ID = 'Has form' and b.RELATIONSHIP_ID = 'RxNorm - ATC'
            and c1.VOCABULARY_ID = 'RxNorm' and c1.CONCEPT_CLASS_ID = 'Ingredient' and c1.STANDARD_CONCEPT = 'S'
            and c2.VOCABULARY_ID = 'RxNorm' and c2.CONCEPT_CLASS_ID = 'Precise Ingredient'
            and c3.VOCABULARY_ID = 'ATC' and c3.CONCEPT_CLASS_ID = 'ATC 5th' and c3.STANDARD_CONCEPT = 'C'
            and c1.concept_id in
                (
                    SELECT ANCESTOR_CONCEPT_ID
                    FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    WHERE DESCENDANT_CONCEPT_ID in
                        (
                            SELECT distinct DRUG_CONCEPT_ID
                            FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
    ) d
left join
    (
        select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
            c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID as atc_5, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
        from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID --parent, atc, atc_4
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID --child, atc, atc_5
        where RELATIONSHIP_ID = 'Subsumes'
            and c1.VOCABULARY_ID = 'ATC'
            and c1.CONCEPT_CLASS_ID = 'ATC 4th'
            and c1.STANDARD_CONCEPT = 'C'
            and c2.VOCABULARY_ID = 'ATC'
            and c2.CONCEPT_CLASS_ID = 'ATC 5th'
            and c2.STANDARD_CONCEPT = 'C'
    ) e on d.atc_5 = e.atc_5"

echo "DRUGS - temp table - ATC3 to ATC4"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 3rd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 4th' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

echo "DRUGS - temp table - ATC2 TO ATC3"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 2nd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 3rd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

echo "DRUGS - temp table - ATC1 TO ATC2"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID as p_concept_id, c1.CONCEPT_CODE as p_concept_code, c1.CONCEPT_NAME as p_concept_name,
    c1.DOMAIN_ID as p_DOMAIN_ID, c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 1st' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 2nd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in
        (
            select P_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )
    and c2.concept_id not in
        (
            select CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\`
        )"

echo "DRUGS - add roots"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by concept_code) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS ID,
    0, 'DRUG', 1, 'ATC', concept_id, concept_code, CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) ),
    1,0,0,1,1,
    CAST(ROW_NUMBER() OVER(order by concept_code) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
from \`$BQ_PROJECT.$BQ_DATASET.concept\`
where VOCABULARY_ID = 'ATC'
    and CONCEPT_CLASS_ID = 'ATC 1st'
    and STANDARD_CONCEPT = 'C'"

echo "DRUGS - add root for unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS ID,
    0,'DRUG',1,'ATC','Unmapped ingredients',1,0,0,1,1,
    CAST( (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as STRING ) as path"

echo "DRUGS - level 2"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`),
    p.id,'DRUG',1,'ATC',c.concept_id,c.concept_code,
    CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) ),1,1,0,1,1,
    CONCAT(p.path, '.',
        CAST(row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
join \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
where p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            select parent_id
            from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            where domain_id = 'DRUG'
                and type = 'ATC'
        )"

echo "DRUGS - level 3"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`),
    p.id,'DRUG',1,'ATC',c.concept_id,c.concept_code,
    CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) ),1,1,0,1,1,
    CONCAT(p.path, '.',
        CAST(row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
join \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
where p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            select parent_id
            from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            where domain_id = 'DRUG'
                and type = 'ATC'
        )"

echo "DRUGS - level 4"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`),
    p.id,'DRUG',1,'ATC',c.concept_id,c.concept_code,
    CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) ),1,1,0,1,1,
    CONCAT(p.path, '.',
        CAST(row_number() over (order by p.id, c.concept_code)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
join \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
where p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            select parent_id
            from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            where domain_id = 'DRUG'
                and type = 'ATC'
        )"

echo "DRUGS - level 5 - ingredients"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by p.id, UPPER(c.concept_name))+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`),
    p.id,'DRUG',1,'RXNORM',c.concept_id,c.concept_code,
    CONCAT( UPPER(SUBSTR(c.concept_name, 1, 1)), LOWER(SUBSTR(c.concept_name, 2)) ),0,1,0,1,1,
    CONCAT(p.path, '.',
        CAST(row_number() over (order by p.id, UPPER(c.concept_name))+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
join \`$BQ_PROJECT.$BQ_DATASET.prep_atc_rel_in_data\` c on p.code = c.p_concept_code
where p.domain_id = 'DRUG'
    and p.type = 'ATC'
    and p.id not in
        (
            select parent_id
            from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            where domain_id = 'DRUG'
                and type = 'ATC'
        )"

echo "DRUGS - add parents for unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select ROW_NUMBER() OVER(ORDER BY upper(name)) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS ID,
    (select id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`  where name = 'Unmapped ingredients') as parent_id,
    'DRUG',1,'ATC',name as code, name,1,0,0,1,1,
    CONCAT( (select CAST(id as STRING) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`  where name = 'Unmapped ingredients'),
        '.', CAST(ROW_NUMBER() OVER(ORDER BY upper(name)) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING) )
from
    (
        select distinct UPPER(SUBSTR(concept_name, 1, 1)) name
        from \`$BQ_PROJECT.$BQ_DATASET.concept\`
        where VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
            and CONCEPT_CLASS_ID = 'Ingredient'
            and STANDARD_CONCEPT = 'S'
            and concept_id in
                (
                    select ANCESTOR_CONCEPT_ID
                    from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    where DESCENDANT_CONCEPT_ID in
                        (
                            select distinct DRUG_CONCEPT_ID
                            from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
            and concept_id not in
                (
                    select concept_id
                    from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    where domain_id = 'DRUG'
                        and concept_id is not null
                )

    ) x"

echo "DRUGS - add unmapped ingredients"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path)
select row_number() over (order by z.id, upper(x.concept_name))+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`),
    z.id,'DRUG',1,'RXNORM',x.concept_id,x.concept_code,CONCAT( UPPER(SUBSTR(x.concept_name, 1, 1)), LOWER(SUBSTR(x.concept_name, 2)) ),
    0,1,0,1,1,
    CONCAT(z.path, '.',
        CAST(ROW_NUMBER() OVER(order by z.id, upper(x.concept_name)) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS STRING))
from
    (
        select *
        from \`$BQ_PROJECT.$BQ_DATASET.concept\`
        where VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
            and CONCEPT_CLASS_ID = 'Ingredient'
            and STANDARD_CONCEPT = 'S'
            and concept_id in
                (
                    select ANCESTOR_CONCEPT_ID
                    from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
                    where DESCENDANT_CONCEPT_ID in
                        (
                            select distinct DRUG_CONCEPT_ID
                            from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
                        )
                )
            and concept_id not in
                (
                    select concept_id
                    from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    where domain_id = 'DRUG'
                        and concept_id is not null
                )
    ) x
join
    (
        select *
        from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        where domain_id = 'DRUG'
            and type = 'ATC'
            and length(name) = 1
    ) z on UPPER(SUBSTR(x.concept_name, 1, 1)) = z.name"

echo "DRUGS - generate child counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
set x.rollup_count = 0
    , x.item_count = y.cnt
    , x.est_count = y.cnt
from
    (
        select b.ANCESTOR_CONCEPT_ID as concept_id, count(distinct a.person_id) cnt
        from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
        join
            (
                select *
                from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` x
                left join \`$BQ_PROJECT.$BQ_DATASET.concept\` y on x.ANCESTOR_CONCEPT_ID = y.CONCEPT_ID
                where VOCABULARY_ID in  ('RxNorm', 'RxNorm Extension')
                    and CONCEPT_CLASS_ID = 'Ingredient'
            ) b on a.DRUG_CONCEPT_ID = b.DESCENDANT_CONCEPT_ID
        group by 1
    ) y
where x.concept_id = y.concept_id
    and x.domain_id = 'DRUG'
    and x.type = 'RXNORM'"

echo "DRUG_EXPOSURE - ATC/RXNORM - add brand names"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      ROW_NUMBER() OVER(ORDER BY upper(concept_name)) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , -1
    , 'DRUG'
    , 1
    , 'BRAND'
    , concept_id
    , concept_code
    , concept_name
    , 0
    , 1
    , 0
    , 0
FROM
    (
        SELECT DISTINCT b.concept_id, b.concept_name, b.concept_code
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.concept_id_1 = b.concept_id --brands
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.concept_id_2 = c.concept_id --ingredients
        WHERE b.vocabulary_id in ('RxNorm','RxNorm Extension')
            and b.concept_class_id = 'Brand Name'
            and b.invalid_reason is null
            and c.concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'DRUG'
                        and type = 'RXNORM'
                        and is_group = 0
                        and is_selectable = 1
                )
    ) x"

echo "DRUG_EXPOSURE - ATC/RXNORM - add data into prep_concept_ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , COALESCE(e.concept_id, d.concept_id, c.concept_id, b.concept_id) as descendant_concept_id
    , a.is_standard
FROM (SELECT id, parent_id, concept_id, is_standard FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM') and is_group = 1 and is_selectable = 1) a
JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) b on a.id = b.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) c on b.id = c.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) d on c.id = d.parent_id
LEFT JOIN (SELECT id, parent_id, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'DRUG' and type in ('ATC','RXNORM')) e on d.id = e.parent_id"

echo "DRUG_EXPOSURE - ATC/RXNORM - generate parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.item_count = 0
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'DRUG'
                            and type = 'ATC'
                            and is_group = 1
                            and is_selectable = 1
                    )
                    and is_standard = 1
            ) a
        JOIN
            (
                SELECT d.ancestor_concept_id as concept_id, c.person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` c
                JOIN
                    (
                        SELECT ancestor_concept_id, descendant_concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` a
                        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.ancestor_concept_id = b.concept_id
                        WHERE vocabulary_id IN  ('RxNorm', 'RxNorm Extension')
                            and concept_class_id = 'Ingredient'
                    ) d on c.drug_concept_id = d.descendant_concept_id
            ) b on a.descendant_concept_id = b.concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'DRUG'
    and type = 'ATC'
    and is_group = 1"


################################################
# PROCEDURE_OCCURRENCE - SNOMED - SOURCE
################################################
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - create prep_snomed_rel_pcs_src"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c1.domain_id AS p_domain_id
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
    , c2.domain_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND c1.vocabulary_id = 'SNOMED'
    AND c2.vocabulary_id = 'SNOMED'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.domain_id = 'Procedure'
    AND c2.domain_id = 'Procedure'
    AND cr.relationship_id = 'Subsumes'"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , p_domain_id
        , concept_id
        , concept_code
        , concept_name
        , domain_id
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src\` a
WHERE concept_id in
    (
        SELECT DISTINCT a.procedure_source_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_source_concept_id = b.concept_id
        WHERE a.procedure_source_concept_id != 0
            and b.domain_id = 'Procedure'
            and b.vocabulary_id = 'SNOMED'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# currently, there are only 8 levels, but we run it 9 times to be safe
for i in {1..9};
do
    echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - temp table adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , p_domain_id
            , concept_id
            , concept_code
            , concept_name
            , domain_id
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src\` a
    WHERE concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
        )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
            )"
done

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , 0
    , 'PROCEDURE'
    , 0
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 4322976"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'PROCEDURE'
    , 0
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'PROCEDURE'
    and p.type = 'SNOMED'
    and p.is_standard = 0
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 12 levels, but we run it 13 times to be safe (if changed, change number of joins in next query)
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..13};
do
    echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'PROCEDURE'
        , 0
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_src_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'PROCEDURE'
        and p.type = 'SNOMED'
        and p.is_standard = 0
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id as c12
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0 and parent_id != 0 and is_group = 1) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 0) n on m.id = n.parent_id"

# Count: 13 - If loop count above is changed, the number of JOINS below must be updated
# the last UNION statement is to add the item to itself
echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - adding items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_12 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_11 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_10 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_9 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 0"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT procedure_source_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.is_standard = 0
    and x.is_selectable = 1"

echo "PROCEDURE_OCCURRENCE - SNOMED - SOURCE - parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'PROCEDURE'
                            and type = 'SNOMED'
                            and is_standard = 0
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 0
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` b on a.descendant_concept_id = b.procedure_source_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.is_standard = 0
    and x.is_group = 1"


###############################################
# PROCEDURE_OCCURRENCE - SNOMED - STANDARD
###############################################
echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - create prep_snomed_rel_pcs"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs\` AS
SELECT DISTINCT c1.concept_id AS p_concept_id
    , c1.concept_code AS p_concept_code
    , c1.concept_name AS p_concept_name
    , c1.domain_id AS p_domain_id
    , c2.concept_id
    , c2.concept_code
    , c2.concept_name
    , c2.domain_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` c2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE cr.concept_id_1 = c1.concept_id
    AND cr.concept_id_2 = c2.concept_id
    AND cr.relationship_id = r.relationship_id
    AND c1.vocabulary_id = 'SNOMED'
    AND c2.vocabulary_id = 'SNOMED'
    AND c1.standard_concept = 'S'
    AND c2.standard_concept = 'S'
    AND r.is_hierarchical = '1'
    AND r.defines_ancestry = '1'
    AND c1.domain_id = 'Procedure'
    AND c2.domain_id = 'Procedure'
    AND cr.relationship_id = 'Subsumes'"

echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
    (
          p_concept_id
        , p_concept_code
        , p_concept_name
        , p_domain_id
        , concept_id
        , concept_code
        , concept_name
        , domain_id
    )
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs\` a
WHERE concept_id in
    (
        SELECT DISTINCT procedure_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_concept_id = b.concept_id
        WHERE procedure_concept_id != 0
            and b.domain_id = 'Procedure'
            and b.vocabulary_id = 'SNOMED'
            and b.standard_concept = 'S'
    )"

# for each loop, add all items (children/parents) related to the items that were previously added
# currently, there are only 5 levels, but we run it 6 times to be safe
for i in {1..6};
do
    echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - temp table adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
        (
              p_concept_id
            , p_concept_code
            , p_concept_name
            , p_domain_id
            , concept_id
            , concept_code
            , concept_name
            , domain_id
        )
    SELECT *
    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs\` a
    WHERE
        concept_id in
            (
                SELECT p_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
            )
        and concept_id not in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
            )"
done

echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - adding root"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , 0
    , 'PROCEDURE'
    , 1
    , 'SNOMED'
    , concept_id
    , concept_code
    , concept_name
    , 1
    , 0
    , 0
    , 1
    , CAST((SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 as STRING) as path
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
WHERE concept_id = 4322976"

echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , path
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
    , p.id
    , 'PROCEDURE'
    , 1
    , 'SNOMED'
    , c.concept_id
    , c.concept_code
    , c.concept_name
    , 0
    , 0
    , 1
    , 1
    , 0
    , 1
    , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\` c on p.code = c.p_concept_code
WHERE p.domain_id = 'PROCEDURE'
    and p.type = 'SNOMED'
    and p.is_standard = 1
    and p.id not in
        (
            SELECT parent_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        )
    and c.concept_id in
        (
            SELECT p_concept_id
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\`
        )"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 15 levels, but we run it 16 times to be safe, If this count changes, change the query below
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..16};
do
    echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - adding level $i"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_group
            , is_selectable
            , has_attribute
            , has_hierarchy
            , path
        )
    SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)
        , p.id
        , 'PROCEDURE'
        , 1
        , 'SNOMED'
        , c.concept_id
        , c.concept_code
        , c.concept_name
        , 0
        , 0
        , CASE WHEN l.concept_code is null THEN 1 ELSE 0 END as is_group
        , 1
        , 0
        , 1
        , CONCAT(p.path, '.', CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_name) +
            (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING))
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` p
    JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\` c on p.code = c.p_concept_code
    LEFT JOIN
        (
            SELECT DISTINCT a.concept_code
            FROM \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\` a
            LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_pcs_in_data\` b on a.concept_id = b.p_concept_id
            WHERE b.concept_id is null
        ) l on c.concept_code = l.concept_code
    WHERE p.domain_id = 'PROCEDURE'
        and p.type = 'SNOMED'
        and p.is_standard = 1
        and p.id not in
            (
                SELECT parent_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Join Count: 16 - If loop count above is changed, the number of JOINS below must be updated
echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - add items into staging table for use in next query"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
    (
          ancestor_concept_id
        , domain_id
        , type
        , is_standard
        , concept_id_1
        , concept_id_2
        , concept_id_3
        , concept_id_4
        , concept_id_5
        , concept_id_6
        , concept_id_7
        , concept_id_8
        , concept_id_9
        , concept_id_10
        , concept_id_11
        , concept_id_12
        , concept_id_13
        , concept_id_14
        , concept_id_15
    )
SELECT DISTINCT a.concept_id as ancestor_concept_id
    , a.domain_id
    , a.type
    , a.is_standard
    , b.concept_id c1
    , c.concept_id c2
    , d.concept_id c3
    , e.concept_id c4
    , f.concept_id c5
    , g.concept_id c6
    , h.concept_id c7
    , i.concept_id c8
    , j.concept_id c9
    , k.concept_id c10
    , m.concept_id c11
    , n.concept_id as c12
    , o.concept_id as c13
    , p.concept_id as c14
    , q.concept_id as c15
FROM (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1 and parent_id != 0 and is_group = 1) a
    JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) b on a.id = b.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) c on b.id = c.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) d on c.id = d.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) e on d.id = e.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) f on e.id = f.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) g on f.id = g.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) h on g.id = h.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) i on h.id = i.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) j on i.id = j.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) k on j.id = k.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) m on k.id = m.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) n on m.id = n.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) o on n.id = o.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) p on o.id = p.parent_id
    LEFT JOIN (SELECT id, parent_id, domain_id, type, is_standard, concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` WHERE domain_id = 'PROCEDURE' and type = 'SNOMED' and is_standard = 1) q on p.id = q.parent_id"

# Join Count: 16 - If loop count above is changed, the number of JOINS below must be updated
# the last UNION statement is to add the item to itself
echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - add items into ancestor table"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
    (
          ancestor_concept_id
        , descendant_concept_id
        , is_standard
    )
SELECT DISTINCT ancestor_concept_id, concept_id_15 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_15 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_14 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_14 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_13 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_13 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_12 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_12 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_11 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_11 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_10 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_10 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_9 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_9 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_8 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_8 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_7 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_7 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_6 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_6 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_5 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_5 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_4 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_4 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_3 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_3 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_2 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_2 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, concept_id_1 as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE concept_id_1 is not null
    and domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1
UNION DISTINCT
SELECT DISTINCT ancestor_concept_id, ancestor_concept_id as descendant_concept_id, is_standard
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ancestor_staging\`
WHERE domain_id = 'PROCEDURE'
    and type = 'SNOMED'
    and is_standard = 1"

echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - generate item counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.item_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT procedure_concept_id as concept_id
            , COUNT(distinct person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_selectable = 1"

echo "PROCEDURE_OCCURRENCE - SNOMED - STANDARD - parent counts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT ancestor_concept_id as concept_id
            , COUNT(DISTINCT person_id) cnt
        FROM
            (
                SELECT ancestor_concept_id
                    , descendant_concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                WHERE ancestor_concept_id in
                    (
                        SELECT DISTINCT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE domain_id = 'PROCEDURE'
                            and type = 'SNOMED'
                            and is_standard = 1
                            and parent_id != 0
                            and is_group = 1
                    )
                    and is_standard = 1
            ) a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` b on a.descendant_concept_id = b.procedure_concept_id
        GROUP BY 1
    ) y
WHERE x.concept_id = y.concept_id
    and x.domain_id = 'PROCEDURE'
    and x.type = 'SNOMED'
    and x.is_standard = 1
    and x.is_group = 1"


################################################
# OBSERVATION
################################################
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
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
    ROW_NUMBER() OVER(order by vocabulary_id, concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , -1
    , 'OBSERVATION'
    , 1
    , vocabulary_id
    , concept_id
    , concept_code
    , concept_name
    , 0
    , cnt
    , cnt
    , 0
    , 1
    , 0
    , 0
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) +
        (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT
              b.concept_name
            , b.vocabulary_id
            , b.concept_id
            , b.concept_code
            , count(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.observation_concept_id = b.concept_id
        WHERE b.standard_concept = 'S'
            and b.domain_id = 'Observation'
            and a.observation_concept_id != 0
        GROUP BY 1,2,3,4
    ) x"


################################################
# CB_CRITERIA_ANCESTOR
################################################
echo "CB_CRITERIA_ANCESTOR - Drugs - add ingredients to drugs mapping"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`
    (
          ancestor_id
        , descendant_id
    )
SELECT
      ancestor_concept_id
    , descendant_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
WHERE ancestor_concept_id in
    (
        SELECT DISTINCT concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE domain_id = 'DRUG'
            and type = 'RXNORM'
            and is_group = 0
            and is_selectable = 1
    )
and descendant_concept_id in
    (
        SELECT DISTINCT drug_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
    )"

###############################################
# ADD IN OTHER CODES NOT ALREADY CAPTURED
################################################
echo "CONDITION_OCCURRENCE - add other source concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER (order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id,
    -1, 'CONDITION', 0, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT b.concept_name, b.vocabulary_id, b.concept_id, b.concept_code, count(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_source_concept_id = b.concept_id
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.condition_concept_id = c.concept_id
        WHERE a.condition_source_concept_id NOT IN
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE is_standard = 0
                    and concept_id is not null
            )
            and a.condition_source_concept_id != 0
            and a.condition_source_concept_id is not null
            and b.concept_id is not null
            and b.vocabulary_id != 'PPI'
            and (b.domain_id LIKE 'Condition%' OR c.domain_id = 'Condition')
        GROUP BY 1,2,3,4
    ) x"

echo "CONDITION_OCCURRENCE - add other standard concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID,
    -1, 'CONDITION',1, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT concept_name, vocabulary_id, concept_id, concept_code, count(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Condition'
            and condition_concept_id NOT IN
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'CONDITION'
                        and is_standard = 1
                        and concept_id is not null
                )
        GROUP BY 1,2,3,4
    ) x"

echo "PROCEDURE_OCCURRENCE - add other source concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER (order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id,
    -1, 'PROCEDURE', 0, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT b.concept_name, b.vocabulary_id, b.concept_id, b.concept_code, count(DISTINCT a.person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_source_concept_id = b.concept_id
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.procedure_concept_id = c.concept_id
        WHERE a.procedure_source_concept_id NOT IN
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE is_standard = 0
                    and concept_id is not null
            )
            and a.procedure_source_concept_id != 0
            and a.procedure_source_concept_id is not null
            and b.concept_id is not null
            and b.vocabulary_id != 'PPI'
            and (b.domain_id = 'Procedure' OR c.domain_id = 'Procedure')
        GROUP BY 1,2,3,4
    ) x"

echo "PROCEDURE_OCCURRENCE - add other standard concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID,
    -1, 'PROCEDURE',1, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT concept_name, vocabulary_id, concept_id, concept_code, count(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Procedure'
            and procedure_concept_id NOT IN
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'PROCEDURE'
                        and is_standard = 1
                        and concept_id is not null
                )
        GROUP BY 1,2,3,4
    ) x"

echo "MEASUREMENT - add other standard concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID,
    -1, 'MEASUREMENT',1, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT concept_name, vocabulary_id, concept_id, concept_code, count(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Measurement'
            and vocabulary_id not in ('PPI')
            and measurement_concept_id NOT IN
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                    WHERE domain_id = 'MEASUREMENT'
                        and is_standard = 1
                        and concept_id is not null
                )
        GROUP BY 1,2,3,4
    ) x"

echo "DRUG_EXPOSURE - add other standard concepts"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,rollup_count,item_count,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID,
    -1, 'DRUG',1, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
    CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
FROM
    (
        SELECT concept_name, vocabulary_id, concept_id, concept_code, count(DISTINCT person_id) cnt
        FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.drug_concept_id = b.concept_id
        WHERE standard_concept = 'S'
            and domain_id = 'Drug'
            and drug_concept_id NOT IN
                (
                    SELECT descendant_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`
                    WHERE ancestor_id in
                        (
                            SELECT concept_id
                            FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                            WHERE domain_id = 'DRUG'
                                and is_standard = 1
                        )
                )
        GROUP BY 1,2,3,4
    ) x"

################################################
# CB_CRITERIA_ATTRIBUTE
################################################
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

################################################
# DATA CLEAN UP
################################################
echo "CLEAN UP - set rollup_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET rollup_count = -1
WHERE rollup_count is null"

echo "CLEAN UP - set item_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET item_count = -1
WHERE item_count is null"

echo "CLEAN UP - set est_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET est_count = -1
WHERE est_count is null"

echo "CLEAN UP - set has_ancestor_data = 0 for all items WHERE it is currently NULL"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET has_ancestor_data = 0
WHERE has_ancestor_data is null"

echo "CLEAN UP - remove all double quotes FROM criteria names"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET name = REGEXP_REPLACE(name, r'[\"]', '')
WHERE REGEXP_CONTAINS(name, r'[\"]')"


###############################################
# FULL_TEXT and SYNONYMS
###############################################
echo "FULL_TEXT and SYNONYMS - adding data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET   x.full_text = y.full_text
    , x.synonyms = y.full_text
FROM
    (
        SELECT
              a.id
            , CASE
                WHEN (STRING_AGG(REPLACE(b.concept_name,'|','||'),'|') is null OR a.concept_id = 0) THEN a.name
                ELSE STRING_AGG(REPLACE(b.concept_name,'|','||'),'|')
              END as full_text
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
        LEFT JOIN
            (
                SELECT concept_id, concept_synonym_name as concept_name
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\`
                WHERE NOT REGEXP_CONTAINS(concept_synonym_name, r'\p{Han}') --remove items with Chinese characters
                UNION DISTINCT
                SELECT concept_id, concept_name
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
                WHERE concept_id is not null
            ) b on a.concept_id = b.concept_id
        GROUP BY a.id, a.name, a.concept_id, a.domain_id
    ) y
WHERE x.id = y.id"

echo "DISPLAY_SYNONYMS - adding data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET   x.display_synonyms = y.display_synonyms
FROM
    (
        SELECT
              a.id
            , CASE
                WHEN (a.domain_id != 'SURVEY' and a.concept_id != 0) THEN STRING_AGG(b.concept_name,'; ')
                ELSE null
              END as display_synonyms
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
        LEFT JOIN
            (
                SELECT concept_id, concept_synonym_name as concept_name
                FROM \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\`
                WHERE NOT REGEXP_CONTAINS(concept_synonym_name, r'\p{Han}') --remove items with Chinese characters
                EXCEPT DISTINCT
                SELECT concept_id, name
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE concept_id is not null
            ) b on a.concept_id = b.concept_id
        GROUP BY a.id, a.name, a.concept_id, a.domain_id
    ) y
WHERE x.id = y.id"

# add [rank1] for all items. this is to deal with the poly-hierarchical issue in many trees
echo "FULL_TEXT - add [rank1]"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.full_text = CONCAT(x.full_text, '|', y.rnk)
   ,x.synonyms = CONCAT(x.full_text, '|', y.rnk)
FROM
    (
        SELECT MIN(id) as id, CONCAT('[', LOWER(domain_id), '_rank1]') as rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE full_text is not null
            and ( (is_selectable = 1 and est_count != -1) OR type = 'BRAND')
        GROUP BY domain_id, is_standard, type, subtype, concept_id, name
    ) y
WHERE x.id = y.id"