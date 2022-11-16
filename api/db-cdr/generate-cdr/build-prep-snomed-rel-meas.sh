#!/bin/bash
set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

echo "MEASUREMENT - SNOMED - STANDARD - create table prep_snomed_rel_meas"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_snomed_rel_meas\`
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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
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

# currently, there are only 6 levels, but we run it 7 times to be safe
for i in {1..6};
do
    echo "MEASUREMENT - SNOMED - STANDARD - temp table level $i"
    bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
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

