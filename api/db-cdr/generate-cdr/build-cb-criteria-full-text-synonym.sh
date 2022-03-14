#!/bin/bash

set -e
SQL_FOR='POPULATE OTHER CB_* TABLES'
export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

###############################################
# FULL_TEXT and SYNONYMS
###############################################
echo "FULL_TEXT and SYNONYMS - adding data"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET   x.full_text = y.full_text
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
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
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

echo "FULL_TEXT and SYNONYMS - adding update for survey answers"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET   x.full_text = y.full_text
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
            ) b on a.value = CAST(b.concept_id as STRING)
            where a.domain_id = 'SURVEY'
            and a.subtype = 'ANSWER'
        GROUP BY a.id, a.name, a.concept_id, a.domain_id
    ) y
WHERE x.id = y.id"

# add [rank1] for all items except source hierarchies. this is to deal with the poly-hierarchical issue in many trees
echo "FULL_TEXT - add [rank1]"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.full_text = CONCAT(x.full_text, '|', y.rnk)
FROM
    (
        SELECT MIN(id) as id, CONCAT('[', LOWER(domain_id), '_rank1]') as rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE full_text is not null
            and ( (is_selectable = 1 and est_count != -1) OR type = 'BRAND')
            and id not in (
              SELECT id
              FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
              WHERE type IN ('ICD10CM', 'ICD9CM', 'ICD10PCS', 'ICD9Proc', 'CPT4')
              AND is_standard = 0
              AND has_hierarchy = 1
              AND is_selectable = 1
              AND est_count != -1
            )
        GROUP BY domain_id, is_standard, type, subtype, concept_id, name
    ) y
WHERE x.id = y.id"

# add [rank1] for all condition source hierarchies.
echo "FULL_TEXT - add [rank1]"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.full_text = CONCAT(x.full_text, '|', y.rnk)
FROM
    (
        SELECT MIN(id) as id, '[condition_rank1]' as rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type IN ('ICD10CM', 'ICD9CM')
        AND is_standard = 0
        AND has_hierarchy = 1
        AND is_selectable = 1
        AND est_count != -1
        GROUP BY domain_id, is_standard, type, subtype, concept_id, name
    ) y
WHERE x.id = y.id"

# add [rank1] for all procedure source hierarchies.
echo "FULL_TEXT - add [rank1]"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` x
SET x.full_text = CONCAT(x.full_text, '|', y.rnk)
FROM
    (
        SELECT MIN(id) as id, '[procedure_rank1]' as rnk
        FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
        WHERE type IN ('ICD10PCS', 'ICD9Proc', 'CPT4')
        AND is_standard = 0
        AND has_hierarchy = 1
        AND is_selectable = 1
        AND est_count != -1
        GROUP BY domain_id, is_standard, type, subtype, concept_id, name
    ) y
WHERE x.id = y.id"

# Update full_text column to contain only rank1 synonyms for polyhierarchical trees
# Polyhierarchical trees - (Drug, Measurement, Procedure and Condition)
echo "FULL_TEXT - updating full_text to null for non rank1"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET full_text = null
WHERE domain_id in ('DRUG', 'MEASUREMENT', 'PROCEDURE', 'CONDITION')
AND type in ('LOINC', 'SNOMED', 'ATC', 'RXNORM')
AND is_standard = 1
AND concept_id is not null
AND full_text not like '%rank1%'"
