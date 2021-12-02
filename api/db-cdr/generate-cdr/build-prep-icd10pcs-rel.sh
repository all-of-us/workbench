#!/bin/bash
set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "ICD10PCS - SOURCE - create prep_icd10pcs_rel_src"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src\`
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_icd10pcs_rel_src_in_data\`
        (
            p_concept_id    INT64,
            p_concept_code  STRING,
            p_concept_name  STRING,
            concept_id      INT64,
            concept_code    STRING,
            concept_name    STRING
        )
SELECT *
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
    bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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

