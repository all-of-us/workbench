#!/bin/bash
#set -ex
set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
#export DATA_BROWSER=$3      # data browser flag

# make-bq-prep-loinc-rel-tables.sh
#3547 - #3570 : prep_loinc_rel : make-bq-criteria-tables.sh
#       Uses tables: concept_relationship, concept, relationship
echo "MEASUREMENT - Labs - STANDARD LOINC - create prep_loinc_rel"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
###############################################
#3571 - #3622 : prep_loinc_rel_in_data : make-bq-criteria-tables.sh
#       Uses tables: measurement, concept, prep_loinc_rel
echo "MEASUREMENT - Labs - STANDARD LOINC - temp table adding level 0"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
    bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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

