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


################################################
# CREATE TABLES
################################################
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

# staging table to make it easier to add data into prep_concept_ancestor
echo "CREATE TABLES - prep_ancestor_staging"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    , CASE WHEN a.is_selectable = 1 THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.is_selectable = 1 THEN
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
        -- there are two ICD9 codes = 92, this gets the one that is valid
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
WHERE a.type in ('ICD9CM', 'ICD9Proc')
    AND
        (
            -- get all parents and get all children that have a count
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

echo "ICD9 - SOURCE - generate parent rollup counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    ,x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each parent, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all parents that are selectable
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
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type in ('ICD9CM', 'ICD9Proc')
                    and c.is_selectable = 1
                    and d.is_standard = 0
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "ICD9 - SOURCE - delete parents that have no count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type in ('ICD9CM', 'ICD9Proc')
    and is_group = 1
    and is_selectable = 1
    and rollup_count = 0"


################################################
# ICD10CM - SOURCE
################################################
echo "ICD10CM - SOURCE - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    , CASE WHEN a.is_selectable = 1 THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.is_selectable = 1 THEN
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
        WHERE vocabulary_id = 'ICD10CM'
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
                    WHERE type = 'ICD10CM'
                        and is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE a.type = 'ICD10CM'
    AND
        (
            -- get all parents and get all children that have a count
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

echo "ICD10CM - SOURCE - generate parent rollup counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    ,x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each parent, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all parents that are selectable
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'ICD10CM'
                            and is_group = 1
                            and is_selectable = 1
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type = 'ICD10CM'
                    and c.is_selectable = 1
                    and d.is_standard = 0
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "ICD10CM - SOURCE - delete zero count parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type = 'ICD10CM'
    and is_group = 1
    and is_selectable = 1
    and rollup_count = 0"


################################################
# ICD10PCS - SOURCE
################################################
echo "ICD10PCS - SOURCE - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    , CASE WHEN a.is_selectable = 1 THEN 0 ELSE null END AS rollup_count
    , CASE
        WHEN a.is_selectable = 1 THEN
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
        WHERE vocabulary_id = 'ICD10PCS'
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
                    WHERE type = 'ICD10PCS'
                        and is_selectable = 1
                )
        GROUP BY 1
    ) c on b.concept_id = c.concept_id
WHERE a.type = 'ICD10PCS'
    AND
        (
            -- get all parents and all children that have a count
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

echo "ICD10PCS - SOURCE  - generate parent rollup counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        SELECT e.id, COUNT(DISTINCT f.person_id) cnt
        FROM
            (
                -- for each parent, get it and all items under it
                SELECT a.id, b.descendant_id
                FROM
                    (
                        -- get all parents that are selectable
                        SELECT id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                        WHERE type = 'ICD10PCS'
                            and is_group = 1
                            and is_selectable = 1
                    ) a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
            ) e
        LEFT JOIN
            (
                SELECT c.id, d.person_id, d.concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
                JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` d on c.concept_id = d.concept_id
                WHERE c.type = 'ICD10PCS'
                    and c.is_selectable = 1
                    and d.is_standard = 0
            ) f on e.descendant_id = f.id
        GROUP BY 1
    ) y
WHERE x.id = y.id"

echo "ICD10PCS - SOURCE - delete zero count parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DELETE
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type = 'ICD10PCS'
    and is_group = 1
    and is_selectable = 1
    and rollup_count = 0"


################################################
# CPT4 - SOURCE
################################################
echo "CPT4 - SOURCE - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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

echo "CPT4 - SOURCE - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
      id
    , parent_id
    , domain_id
    , is_standard
    , type
    , subtype
    , concept_id
    , name
    , value
    , CASE WHEN is_selectable = 1 THEN 0 ELSE null END as item_count
    , CASE WHEN is_selectable = 1 THEN 0 ELSE null END as est_count
    , is_group
    , is_selectable
    , has_attribute
    , has_hierarchy
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
WHERE domain_id = 'PHYSICAL_MEASUREMENT'
ORDER BY 1"

echo "PM - counts for Heart Rate, Height, Weight, BMI, Waist Circumference, Hip Circumference"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , path
    )
SELECT
      id
    , parent_id
    , domain_id
    , is_standard
    , type
    , subtype
    , concept_id
    , name
    , value
    , CASE
        WHEN (is_selectable = 1 and name != 'Select a value') THEN 0
        ELSE null
      END as rollup_count
      , CASE
          WHEN (is_selectable = 1 and name != 'Select a value') THEN 0
          ELSE null
        END as item_count
    , CASE
        WHEN (is_selectable = 1 and name != 'Select a value') THEN 0
        ELSE null
      END as est_count
    , is_group
    , is_selectable
    , has_attribute
    , has_hierarchy
    , path
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
WHERE domain_id = 'SURVEY'
    and type = 'PPI'
ORDER BY 1"

echo "PPI SURVEYS - insert extra answers (Skip, Prefer Not To Answer, Dont Know)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_source_concept_id = b.concept_id
        LEFT JOIN
            (
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
                WHERE domain_id = 'SURVEY'
            ) c on (a.observation_source_concept_id = c.concept_id and CAST(a.value_source_concept_id as STRING) = c.value)
        WHERE a.value_source_concept_id in (903096, 903079, 903087)
            and a.observation_source_concept_id in
                (
                    SELECT concept_id
                    FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
                    WHERE domain_id = 'SURVEY'
                        and concept_id is not null
                )
            and c.id is null
    ) d
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` e on
    (d.observation_source_concept_id = e.concept_id and e.domain_id = 'SURVEY' and e.is_group = 1)"

echo "PPI SURVEYS - add items to ancestor table"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.item_count = y.cnt
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
# DEMOGRAPHICS
################################################
echo "DEMOGRAPHICS - Deceased"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`)+1 AS id
    , 0
    , 'PERSON'
    , 1
    , 'DECEASED'
    , 'Deceased'
    , COUNT(DISTINCT person_id)
    , COUNT(DISTINCT person_id)
    , 0
    , 1
    , 0
    , 0
FROM \`$BQ_PROJECT.$BQ_DATASET.death\`"

echo "DEMOGRAPHICS - Gender Identity"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , 'PERSON'
    , 1
    , 'GENDER'
    , concept_id
    , CASE WHEN b.concept_id = 0 THEN 'Unknown' ELSE b.concept_name END as name
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

echo "DEMOGRAPHICS - Sex at Birth"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , 'PERSON'
    , 1
    , 'SEX'
    , concept_id
    , CASE WHEN b.concept_id = 0 THEN 'Unknown' ELSE b.concept_name END as name
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

echo "DEMOGRAPHICS - Race"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , 'PERSON'
    , 1
    , 'RACE'
    , concept_id
    , CASE
        WHEN a.race_concept_id = 0 THEN 'Unknown'
        ELSE regexp_replace(b.concept_name, r'^.+:\s', '')
      END as name
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) AS id
    , 0
    , 'PERSON'
    , 1
    , 'ETHNICITY'
    , concept_id
    , regexp_replace(b.concept_name, r'^.+:\s', '')
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , concept_id
        , name
        , item_count
        , est_count
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
    )
SELECT
    ROW_NUMBER() OVER(ORDER BY concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , 0
    , 'VISIT'
    , 1
    , 'VISIT'
    , concept_id
    , concept_name
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
# CONDITION_OCCURRENCE - SNOMED - SOURCE
################################################
echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - temp table level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
FROM \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\`
WHERE concept_id in
    (
        SELECT DISTINCT a.condition_source_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
        JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_source_concept_id = b.concept_id
        WHERE a.condition_source_concept_id != 0
            and b.domain_id = 'Condition'
            and b.vocabulary_id = 'SNOMED'
    )"

# currently, there are only 6 levels, but we run it 7 times to be safe
for i in {1..7};
do
    echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - temp table level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    FROM \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\`
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_selectable
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_selectable
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

# currently, there are only 17 levels, but we run it 18 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..18};
do
    echo "CONDITION_OCCURRENCE - SNOMED - SOURCE - adding level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_selectable
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - temp table level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
FROM \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
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

# currently, there are only 5 levels, but we run it 6 times to be safe
for i in {1..6};
do
    echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - temp table level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    FROM \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_selectable
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
        , is_selectable
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

# currently, there are only 17 levels, but we run it 18 times to be safe
# NOTE: if loop number changes, change number of joins in next two queries
for i in {1..18};
do
    echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
            , is_selectable
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
                SELECT PARENT_ID
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
            )"
done

# Join Count: 18 - If loop count above is changed, the number of JOINS below must be updated
echo "CONDITION_OCCURRENCE - SNOMED - STANDARD - add items into staging table for use in next query"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.rollup_count = y.cnt
    , x.est_count = y.cnt
FROM
    (
        select ancestor_concept_id as concept_id
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