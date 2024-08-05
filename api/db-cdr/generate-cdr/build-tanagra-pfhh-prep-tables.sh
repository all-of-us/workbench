#!/bin/bash

set -e

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset

################################################
# CREATE PFHH PREP TABLE FOR TANAGRA
################################################
BUCKET="all-of-us-workbench-private-cloudsql"
FOLDER="static_prep_tables"
SCHEMA_PATH="generate-cdr/bq-schemas"

echo "Loading data into table - prep_pfhh_mapping"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_pfhh_mapping" \
"gs://$BUCKET/$FOLDER/prep_pfhh_mapping.csv" "$SCHEMA_PATH/prep_pfhh_mapping.json"

echo "Loading data into table - prep_pfhh_remove_from_index"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_pfhh_remove_from_index" \
"gs://$BUCKET/$FOLDER/prep_pfhh_remove_from_index.csv" "$SCHEMA_PATH/prep_pfhh_remove_from_index.json"

echo "Loading data into table - prep_pfhh_non_answer_insert"
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_pfhh_non_answer_insert" \
"gs://$BUCKET/$FOLDER/prep_pfhh_non_answer_insert.csv" "$SCHEMA_PATH/prep_pfhh_non_answer_insert.json"

##############################################################
# PFHH Source Data
##############################################################
echo "prep_pfhh_observation - inserting PFHH source data"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\`
SELECT DISTINCT o.person_id, o.observation_date, o.observation_datetime, o.observation_source_concept_id, p.birth_datetime, o.visit_detail_id, o.visit_occurrence_id, o.value_as_concept_id, o.value_as_number, o.value_source_concept_id, oe.survey_version_concept_id, sc.survey_concept_id, sc.collection_method_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` o
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = o.person_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` oe on o.observation_id = oe.observation_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.survey_conduct\` sc ON o.questionnaire_response_id = sc.survey_conduct_id
WHERE sc.survey_concept_id IN (43529712, 43528698, 1740639)"

##############################################################
# Unify the PFHH survey
##############################################################
echo "prep_pfhh_observation - updating all questions to new pfhh survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\` x
SET x.observation_source_concept_id = y.new_question,
x.survey_concept_id = 1740639
FROM (
  SELECT DISTINCT pfhh.person_id, pfhh.observation_date, pfhh.observation_source_concept_id AS historic_question, pfhh.value_source_concept_id AS answer, pfhh_mapping.pfhh_question_concept_id AS new_question
  FROM \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\` pfhh
  JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_mapping\` pfhh_mapping ON pfhh.observation_source_concept_id = pfhh_mapping.historic_question_concept_id AND pfhh.value_source_concept_id = pfhh_mapping.pfhh_answer_concept_id
  AND survey_concept_id IN (43528698, 43529712)
) y
WHERE x.concept_id = y.historic_question
AND x.value_source_concept_id = y.answer
AND x.person_id = y.person_id
AND x.observation_date = y.observation_date"

echo "Removing deprecated questions"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"DELETE FROM \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\` y
WHERE EXISTS (
  SELECT 'x'
  FROM \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_remove_from_index\` x
  WHERE x.concept_id = y.observation_source_concept_id
  AND x.value = y.value_source_concept_id
)"

echo "Insert skip rows into new pfhh survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\`
SELECT DISTINCT
      a.person_id
    , a.entry_date
    , a.entry_datetime
    , a.concept_id
    , a.domain
    , a.age_at_event
    , a.visit_concept_id
    , a.visit_occurrence_id
    , a.value_as_number
    , a.value_as_concept_id
    , a.value_source_concept_id
    , a.survey_version_concept_id
    , a.survey_concept_id
    , a.cati_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_non_answer_insert\` a
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` b on a.person_id = b.person_id"

echo "Updating all survey_concept_ids for Med History/Personal History to new PFHH survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.prep_pfhh_observation\`
 SET survey_concept_id = 1740639
 WHERE survey_concept_id IN (43528698, 43529712)"