#!/bin/bash

# This generates the cb survey version table for cohort builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

#####################################
# INSERT into cb_survey_version TABLE
#####################################
echo "Insert cb_survey_version"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`
    (survey_version_concept_id,survey_concept_id,display_name,display_order)
SELECT survey_version_concept_id, survey_concept_id, display_name, ROW_NUMBER() OVER (ORDER BY survey_version_concept_id) AS display_order
FROM (
  SELECT distinct o.survey_version_concept_id,
  (SELECT concept_id FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` WHERE concept_class_id = 'Module' AND concept_code = 'cope') as survey_concept_id,
  REPLACE (c.concept_name, 'COPE Survey', '') as display_name
FROM \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` o
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on c.concept_id = o.survey_version_concept_id
WHERE o.survey_version_concept_id is not null) x"