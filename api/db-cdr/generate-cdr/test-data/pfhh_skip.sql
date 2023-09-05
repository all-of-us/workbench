--this query helps build the prep_pfhh_non_answer_insert table
--it contains all skip combos for each question
INSERT INTO `all-of-us-ehr-dev.brian_test.prep_pfhh_non_answer_insert`
(
person_id
, entry_date
, entry_datetime
, is_standard
, concept_id
, domain
, age_at_event
, visit_concept_id
, visit_occurrence_id
, value_as_number
, value_as_concept_id
, value_source_concept_id
, survey_version_concept_id
, survey_concept_id
, cati_concept_id
)
SELECT DISTINCT
    b.person_id
              , a.observation_date as entry_date
              , a.observation_datetime as entry_datetime
              , 0 as is_standard
              , 1740668 as concept_id
              , 'Observation' as domain
           , DATE_DIFF(a.observation_date, DATE(b.birth_datetime), YEAR) -
               IF(EXTRACT(MONTH FROM DATE(b.birth_datetime))*100 + EXTRACT(DAY FROM DATE(b.birth_datetime))
               > EXTRACT(MONTH FROM a.observation_date)*100 + EXTRACT(DAY FROM a.observation_date), 1, 0) as age_at_event
           , d.visit_concept_id
           , d.visit_occurrence_id
           , a.value_as_number
           , a.value_as_concept_id
           , a.value_source_concept_id
           , c.survey_version_concept_id
           , 1740639 as survey_concept_id
           , e.collection_method_concept_id
FROM `aou-res-curation-output-prod.R2022Q4R9.observation` a
    JOIN `aou-res-curation-output-prod.R2022Q4R9.person` b on a.person_id = b.person_id
    LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.observation_ext` c on a.observation_id = c.observation_id
    LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.visit_occurrence` d on a.visit_occurrence_id = d.visit_occurrence_id
    LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.survey_conduct` e on a.questionnaire_response_id = e.survey_conduct_id
    JOIN (
    SELECT * FROM (
    SELECT person_id, observation_source_concept_id, 903096 AS value_source_concept_id,
    RANK() OVER ( PARTITION BY person_id ORDER BY observation_source_concept_id ) AS rank
    FROM (
    SELECT person_id, observation_source_concept_id
    FROM `all-of-us-ehr-dev.brian_test.observation`
    WHERE (observation_source_concept_id = 836840 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 1384430 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529632 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529633 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529634 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529635 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529636 AND value_source_concept_id = 903096)
    OR (observation_source_concept_id = 43529637 AND value_source_concept_id = 903096)
    ) y
    )
    WHERE rank = 1
    AND person_id NOT IN (
    SELECT distinct person_id
    FROM `all-of-us-ehr-dev.brian_test.cb_search_all_events`
    WHERE concept_id = 1740668
    AND CAST(value_source_concept_id AS STRING) NOT LIKE '90%'
    )
    ) x
    ON x.observation_source_concept_id = a.observation_source_concept_id
    AND x.value_source_concept_id = a.value_source_concept_id
    AND x.person_id = a.person_id

--find questions for new pfhh survey
SELECT distinct concept_id
FROM `aou-res-curation-output-prod.R2022Q4R9.cb_criteria` c
         JOIN (
    SELECT CAST(id AS STRING) AS id
    FROM `aou-res-curation-output-prod.R2022Q4R9.cb_criteria`
    WHERE concept_id IN (1740639)
      AND domain_id = 'SURVEY'
) a ON (c.path LIKE CONCAT('%', a.id, '.%'))
WHERE domain_id = 'SURVEY'


--find questions for old surveys
SELECT distinct concept_id
FROM `aou-res-curation-output-prod.R2022Q2R6.cb_criteria` c
         JOIN (
    SELECT CAST(id AS STRING) AS id
    FROM `aou-res-curation-output-prod.R2022Q2R6.cb_criteria`
    WHERE concept_id IN (43528698, 43529712)
      AND domain_id = 'SURVEY'
) a ON (c.path LIKE CONCAT('%', a.id, '.%'))
WHERE domain_id = 'SURVEY'