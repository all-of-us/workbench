#!/bin/bash

set -e
SQL_FOR='ADD IN OTHER CODES NOT ALREADY CAPTURED'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

################################################
## ADD IN OTHER CODES NOT ALREADY CAPTURED
#################################################
echo "CONDITION_OCCURRENCE - add other source concepts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER (order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , -1
    , 'CONDITION'
    , 0
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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
            and b.vocabulary_id not in ('PPI', 'SNOMED')
            and (b.domain_id LIKE 'Condition%' OR c.domain_id = 'Condition')
        GROUP BY 1,2,3,4
    ) x"

echo "CONDITION_OCCURRENCE - add other standard concepts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID
    , -1
    , 'CONDITION'
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER (order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as id
    , -1
    , 'PROCEDURE'
    , 0
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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
            and b.vocabulary_id not in ('PPI', 'SNOMED')
            and (b.domain_id = 'Procedure' OR c.domain_id = 'Procedure')
        GROUP BY 1,2,3,4
    ) x"

echo "PROCEDURE_OCCURRENCE - add other standard concepts"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID
    , -1
    , 'PROCEDURE',1
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT
    ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID
    , -1
    , 'MEASUREMENT'
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
    (
      id
      ,parent_id
      ,domain_id
      ,is_standard
      ,type
      ,concept_id
      ,code
      ,name
      ,rollup_count
      ,item_count
      ,est_count
      ,is_group
      ,is_selectable
      ,has_attribute
      ,has_hierarchy
      ,path
    )
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as ID
    , -1
    , 'DRUG'
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
    , CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`) as STRING) as path
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

echo "PROCEDURE - Delete all source concepts listed as standard"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE
 FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
 WHERE domain_id = 'PROCEDURE'
 AND type IN ('CPT4', 'ICD9Proc', 'ICD10PCS')
 AND is_standard = 1
 AND has_hierarchy = 0"

echo "Measurement - Delete all source concepts listed as standard"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE
 FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
 WHERE domain_id = 'MEASUREMENT'
 AND type IN ('CPT4')
 AND is_standard = 1
 AND has_hierarchy = 0"

echo "Observation - Delete all source concepts listed as standard"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"DELETE
 FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
 WHERE domain_id = 'OBSERVATION'
 AND type IN ('CPT4')
 AND is_standard = 1
 AND has_hierarchy = 0"