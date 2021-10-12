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
# make-cb-criteria-23-seq-03-clean-up-text-synonym.sh
#6472 - #6509: make-bq-criteria-tables.sh
# ---------ORDER - 23 - CLEAN UP AND FULL_TEXT AND SYNONYMS---------
# ORDER - 23.1: #6472 - #6504: - CB_CRITERIA DATA CLEAN UP ---------
	#cb_criteria: #6472 - #6503: Clean up for null (replace with -1) in different count columns
# ORDER - 23.2: #6509 - #6609: - CB_CRITERIA FULL_TEXT and SYNONYMS ---------
	#cb_criteria: #6509: Uses : cb_criteria, concept_synonym, concept
################################################
# DATA CLEAN UP
################################################
echo "CLEAN UP - set rollup_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET rollup_count = -1
WHERE rollup_count is null"

echo "CLEAN UP - set item_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET item_count = -1
WHERE item_count is null"

echo "CLEAN UP - set est_count = -1 WHERE the count is NULL"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET est_count = -1
WHERE est_count is null"

echo "CLEAN UP - set has_ancestor_data = 0 for all items WHERE it is currently NULL"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET has_ancestor_data = 0
WHERE has_ancestor_data is null"

echo "CLEAN UP - remove all double quotes FROM criteria names"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
SET name = REGEXP_REPLACE(name, r'[\"]', '')
WHERE REGEXP_CONTAINS(name, r'[\"]')"

###############################################
# FULL_TEXT and SYNONYMS
###############################################
echo "FULL_TEXT and SYNONYMS - adding data"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
            ) b on a.value = CAST(b.concept_id as STRING)
            where a.domain_id = 'SURVEY'
            and a.subtype = 'ANSWER'
        GROUP BY a.id, a.name, a.concept_id, a.domain_id
    ) y
WHERE x.id = y.id"

# add [rank1] for all items. this is to deal with the poly-hierarchical issue in many trees
echo "FULL_TEXT - add [rank1]"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
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
