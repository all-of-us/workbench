#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
# values for DS_SURVEY_TOKEN:
# COPE_and_PFHH, PFHH, COPE, COPE_vaccine
export DS_SURVEY_TOKEN=$3 # specific names to build

################################################
# INSERT DATA FUNCTIONS - DOMAIN
################################################
function do_COPE_and_PFHH(){
  echo "ds_survey - inserting data for all surveys except COPE and PFHH"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer)
  SELECT  a.person_id,
          a.entry_datetime as survey_datetime,
          c.name as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
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
                  AND parent_id = 0
                  AND concept_id NOT IN (1741006, 1333342, 1740639)
              )
      ) b on a.concept_id = b.descendant_concept_id
  JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id"
}

function do_PFHH(){
  echo "ds_survey - inserting data for PFHH survey"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer)
  SELECT  a.person_id,
          a.entry_datetime as survey_datetime,
          'Personal and Family Health History' as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` a
  JOIN
      (
          SELECT DISTINCT CAST(value AS INT64) as answer_concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c
          JOIN (
                SELECT CAST(id AS STRING) AS id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE concept_id IN (1740639)
                AND domain_id = 'SURVEY'
              ) a ON (c.path LIKE CONCAT('%', a.id, '.%'))
          WHERE domain_id = 'SURVEY'
          AND type = 'PPI'
          AND subtype = 'ANSWER'
      ) b on a.value_source_concept_id = b.answer_concept_id
  JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.answer_concept_id = CAST(c.value AS INT64)
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id"
}

function do_COPE_vaccine(){
  echo "ds_survey - inserting data for cope vaccine survey"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
     (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer, survey_version_concept_id, survey_version_name)
  SELECT  a.person_id,
          a.observation_datetime as survey_datetime,
          case when g.display_name like '%COPE Survey' then 'COVID-19 Participant Experience (COPE) Survey' when g.display_name like '%Minute Survey' then 'COVID-19 Vaccine Survey' else c.name end as survey,
          d.concept_id as question_concept_id,
          d.concept_name as question,
          e.concept_id as answer_concept_id,
          case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer,
          g.survey_version_concept_id as survey_version_concept_id,
          g.display_name as survey_version_name
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  JOIN
      (
          SELECT *
          FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
          WHERE ancestor_concept_id in
              (
                  SELECT concept_id
                  FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                  WHERE domain_id = 'SURVEY'
                  AND parent_id = 0
                  AND concept_id IN (1741006)
              )
      ) b on a.observation_source_concept_id = b.descendant_concept_id
  JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.survey_conduct\` f on a.questionnaire_response_id = f.survey_conduct_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\` g on f.survey_concept_id = g.survey_version_concept_id
  GROUP BY a.person_id,
          survey_datetime,
          survey,
          question_concept_id,
          question,
          answer_concept_id,
          answer,
          survey_version_concept_id,
          survey_version_name"
}

function do_COPE(){
echo "ds_survey - inserting data for COVID-19 Participant Experience (COPE) Survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`
   (person_id, survey_datetime, survey, question_concept_id, question, answer_concept_id, answer, survey_version_concept_id, survey_version_name)
SELECT  a.person_id,
        a.observation_datetime as survey_datetime,
        case when g.display_name like '%COPE Survey' then 'COVID-19 Participant Experience (COPE) Survey' when g.display_name like '%Minute Survey' then 'COVID-19 Vaccine Survey' else c.name end as survey,
        d.concept_id as question_concept_id,
        d.concept_name as question,
        e.concept_id as answer_concept_id,
        case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer,
        g.survey_version_concept_id as survey_version_concept_id,
        g.display_name as survey_version_name
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
JOIN
    (
        SELECT *
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
        WHERE ancestor_concept_id in
            (
                SELECT concept_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'SURVEY'
                AND parent_id = 0
                AND concept_id IN (1333342)
            )
        AND descendant_concept_id NOT IN (1310132, 1310137)
    ) b on a.observation_source_concept_id = b.descendant_concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.survey_conduct\` f on a.questionnaire_response_id = f.survey_conduct_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\` g on f.survey_concept_id = g.survey_version_concept_id
GROUP BY a.person_id,
        survey_datetime,
        survey,
        question_concept_id,
        question,
        answer_concept_id,
        answer,
        survey_version_concept_id,
        survey_version_name"
}

# listed in order of row-counts max->min
# COPE_and_PFHH, PFHH, COPE, COPE_vaccine
if [[ "$DS_SURVEY_TOKEN" eq "COPE_and_PFHH" ]]; then
  do_COPE_and_PFHH
elif [[ "$DS_SURVEY_TOKEN" eq "PFHH" ]]; then
  do_PFHH
elif [[ "$DS_SURVEY_TOKEN" eq "COPE" ]]; then
  do_COPE
elif [[ "$DS_SURVEY_TOKEN" eq "COPE_vaccine" ]]; then
  do_COPE_vaccine
else
  echo "Unknown table $DS_SURVEY_TOKEN"
  exit 0
fi
#wait for function to return
wait
echo " Done table $DS_SURVEY_TOKEN"
