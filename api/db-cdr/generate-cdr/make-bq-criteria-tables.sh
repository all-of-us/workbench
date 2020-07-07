#!/bin/bash

# This generates the criteria tables for the CDR

# PREP: upload all prep tables

# ./project.rb generate-cb-criteria-tables --bq-project aou-res-curation-output-prod --bq-dataset SR2019q4r3

set -ex

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
export DATA_BROWSER=$3      # data browser flag
export DRY_RUN=$4           # dry run

if [ "$DRY_RUN" == true ]
then
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.cb_search_all_events")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.concept")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.concept_ancestor")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.concept_relationship")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.concept_synonym")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.condition_occurrence")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.death")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.drug_exposure")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.measurement")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.observation")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.person")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.prep_criteria")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.prep_criteria_ancestor")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.prep_clinical_terms_nc")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.procedure_occurrence")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.relationship")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.visit_occurrence")
  exit 0
fi

# Test that datset exists
test=$(bq show "$BQ_PROJECT:$BQ_DATASET")

echo "CREATE TABLES - cb_criteria"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    synonyms            STRING
)"

# table that holds the ingredient --> coded drugs mapping
echo "CREATE TABLES - cb_criteria_ancestor"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`
(
    ancestor_id INT64,
    descendant_id INT64
)"

# table that holds categorical results and min/max information about individual labs
echo "CREATE TABLES - cb_criteria_attribute"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`
(
    id                    INT64,
    concept_id            INT64,
    value_as_concept_id	  INT64,
    concept_name          STRING,
    type                  STRING,
    est_count             STRING
)"

# table that holds the drug brands to ingredients relationship mapping
# also holds source concept --> standard concept mapping information
echo "CREATE TABLES - cb_criteria_relationship"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`
(
    concept_id_1 INT64,
    concept_id_2 INT64
)"

# table that holds temp data to get ancestor information for parent counts
echo "CREATE TABLES - prep_concept_ancestor_temp"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor_temp\`
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
(
    ancestor_concept_id     INT64,
    descendant_concept_id   INT64,
    is_standard             INT64
)"

# holds atc and rxnorm concept relationships for drugs
echo "CREATE TABLES - prep_atc_rel_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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


################################################
# CREATE VIEWS
################################################
echo "CREATE VIEWS - v_loinc_rel"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` AS
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
    AND c1.standard_concept IN ('S','C')
    AND c2.standard_concept IN ('S','C')
    AND c1.concept_class_id IN ('LOINC Hierarchy', 'Lab Test')
    AND c2.concept_class_id IN ('LOINC Hierarchy', 'Lab Test')"

echo "CREATE VIEWS - v_snomed_rel_cm"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` AS
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

echo "CREATE VIEWS - v_snomed_rel_cm_src"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\` AS
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

echo "CREATE VIEWS - v_snomed_rel_meas"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` AS
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

echo "CREATE VIEWS - v_snomed_rel_pcs"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\` AS
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

echo "CREATE VIEWS - v_snomed_rel_pcs_src"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs_src\` AS
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


################################################
# ICD9 - SOURCE
################################################
echo "ICD9 - SOURCE - add data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,item_count
        ,est_count
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,path
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
    , CASE WHEN a.is_selectable = 1 THEN c.cnt ELSE null END AS item_count
    , CASE WHEN a.is_group = 0 and a.is_selectable = 1 THEN c.cnt ELSE null END AS est_count
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , a.path
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
LEFT JOIN
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
        -- for some reason there are two ICD9 codes = 92, this gets the one that is valid
        WHERE (vocabulary_id in ('ICD9CM', 'ICD9Proc') and concept_code != '92')
            OR (vocabulary_id = 'ICD9Proc' and concept_code = '92')
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
                    WHERE type in ('ICD9CM', 'ICD9Proc')
                        AND is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE type in ('ICD9CM', 'ICD9Proc')
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

echo "ICD9 - SOURCE - generate group rollup counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    ,x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each group, get it and all items under each group
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all groups that are selectable
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type in ('ICD9CM', 'ICD9Proc')
                            and is_group = 1
                            and is_selectable = 1
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
                JOIN
                    (
                        -- get all coded items for all selectable groups and children
                        SELECT person_id, concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
                        WHERE is_standard = 0
                            AND concept_id IN
                            (
                                -- get all selectable groups and children
                                SELECT concept_id
                                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                                WHERE type in ('ICD9CM', 'ICD9Proc')
                                    and is_selectable = 1
                            )
                    ) d on c.concept_id = d.concept_id
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "ICD9 - SOURCE - delete groups that have no count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type in ('ICD9CM', 'ICD9Proc')
    and is_group = 1
    and is_selectable = 1
    and (rollup_count is null or rollup_count = 0)"
