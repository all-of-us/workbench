SELECT DISTINCT a.person_id,
        o.observation_id as data_id,
        case when a.entry_datetime is null then CAST(a.entry_date AS TIMESTAMP) else a.entry_datetime end as survey_datetime,
        f.name as survey,
        d.concept_name as question,
        case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
FROM `aou-res-curation-output-prod.R2022Q4R9.cb_search_all_events_copy` a
JOIN
    (
        SELECT *
        FROM `aou-res-curation-output-prod.R2022Q4R9.prep_concept_ancestor`
        WHERE ancestor_concept_id in
            (
                SELECT concept_id
                FROM `aou-res-curation-output-prod.R2022Q4R9.cb_criteria`
                WHERE domain_id = 'SURVEY'
                    and parent_id = 0
            )
    ) b ON (a.concept_id = b.descendant_concept_id AND a.survey_concept_id = b.ancestor_concept_id)
JOIN `aou-res-curation-output-prod.R2022Q4R9.observation` o ON (a.entry_date = o.observation_date AND a.person_id = o.person_id AND a.concept_id = o.observation_source_concept_id AND a.value_source_concept_id = o.value_source_concept_id)
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.concept` d on a.concept_id = d.concept_id
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.concept` e on a.value_source_concept_id = e.concept_id
LEFT JOIN `aou-res-curation-output-prod.R2022Q4R9.cb_criteria` f on b.ancestor_concept_id = f.concept_id