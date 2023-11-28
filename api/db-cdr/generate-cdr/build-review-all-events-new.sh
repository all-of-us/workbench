SELECT DISTINCT a.person_id,
        o.observation_id AS data_id,
        CASE WHEN a.entry_datetime IS NULL THEN CAST(a.entry_date AS TIMESTAMP) ELSE a.entry_datetime END AS survey_datetime,
        f.name AS survey,
        d.concept_name AS question,
        CASE WHEN a.value_as_number IS NOT NULL THEN CAST(a.value_as_number AS STRING) ELSE e.concept_name END AS answer
FROM `aou-res-curation-output-prod.R2022Q4R9.cb_search_all_events_copy` a
JOIN
    (
        SELECT *
        FROM `aou-res-curation-output-prod.R2022Q4R9.prep_concept_ancestor`
        WHERE ancestor_concept_id IN
            (
                SELECT concept_id
                FROM `aou-res-curation-output-prod.R2022Q4R9.cb_criteria`
                WHERE domain_id = 'SURVEY'
                  AND parent_id = 0
            )
    ) b ON (a.concept_id = b.descendant_concept_id AND a.survey_concept_id = b.ancestor_concept_id)
JOIN `aou-res-curation-output-prod.R2022Q4R9.observation` o ON (a.entry_date = o.observation_date AND a.person_id = o.person_id AND a.concept_id = o.observation_source_concept_id AND a.value_source_concept_id = o.value_source_concept_id)
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.concept` d ON a.concept_id = d.concept_id
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.concept` e ON a.value_source_concept_id = e.concept_id
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.cb_criteria` f ON b.ancestor_concept_id = f.concept_id