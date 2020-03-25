SELECT p.person_id                                             _id,
       DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH) birth_datetime,
       cbp.age_at_consent,
       cbp.age_at_cdr,
       p.gender_concept_id,
       case
           when gc.concept_name is null then 'Unknown'
           else gc.concept_name end                    as      gender_concept_name,
       p.race_concept_id,
       case
           when rc.concept_name is null then 'Unknown'
           else rc.concept_name end                    as      race_concept_name,
       p.ethnicity_concept_id,
       case
           when ec.concept_name is null then 'Unknown'
           else ec.concept_name end                    as      ethnicity_concept_name,
       p.sex_at_birth_concept_id,
       case
           when sc.concept_name is null then 'Unknown'
           else sc.concept_name end                    as      sex_at_birth_concept_name,
       condition_concept_ids,
       condition_source_concept_ids,
       observation_concept_ids,
       observation_source_concept_ids,
       drug_concept_ids,
       drug_source_concept_ids,
       procedure_concept_ids,
       procedure_source_concept_ids,
       measurement_concept_ids,
       measurement_source_concept_ids,
       visit_concept_ids,
       death.person_id is not null                     as      is_deceased,
       CAST(cbp.has_ehr_data as BOOL)                  as      has_ehr_data,
       CAST(cbp.has_physical_measurement_data as BOOL) as      has_physical_measurement_data,
       ARRAY_CONCAT(observations, conditions, drugs, procedures,
                    measurements)                      AS      events
FROM `{BQ_DATASET}.person` p
         LEFT JOIN (
    SELECT ob.person_id                                                             person_id,
           ARRAY_AGG(DISTINCT observation_concept_id)                               observation_concept_ids,
           ARRAY_AGG(DISTINCT observation_source_concept_id)                        observation_source_concept_ids,
           ARRAY_AGG(STRUCT(observation_concept_id AS concept_id,
                            observation_source_concept_id AS source_concept_id,
                            observation_date AS start_date,
                            DATE_DIFF(observation_date,
                                      DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH),
                                      YEAR) AS age_at_start,
                            v.visit_concept_id,
                            value_as_number,
                            value_as_concept_id,
                            value_source_concept_id as value_as_source_concept_id)) observations
    FROM `{BQ_DATASET}.observation` ob
             LEFT JOIN
         `{BQ_DATASET}.person` p
         ON
             ob.person_id = p.person_id
             LEFT JOIN
         `{BQ_DATASET}.visit_occurrence` v
         ON
             ob.visit_occurrence_id = v.visit_occurrence_id
    WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) ob
                   ON
                       ob.person_id = p.person_id
         LEFT JOIN (
    SELECT co.person_id                                                         person_id,
           ARRAY_AGG(DISTINCT condition_concept_id)                             condition_concept_ids,
           ARRAY_AGG(DISTINCT condition_source_concept_id)                      condition_source_concept_ids,
           ARRAY_AGG(STRUCT(condition_concept_id AS concept_id,
                            condition_source_concept_id AS source_concept_id,
                            condition_start_date AS start_date,
                            DATE_DIFF(condition_start_date,
                                      DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH),
                                      YEAR) AS age_at_start,
                            v.visit_concept_id,
                            CAST(NULL as FLOAT64) as value_as_number,
                            CAST(NULL as INT64) as value_as_concept_id,
                            CAST(NULL as INT64) as value_as_source_concept_id)) conditions
    FROM `{BQ_DATASET}.condition_occurrence` co
             LEFT JOIN
         `{BQ_DATASET}.person` p
         ON
             co.person_id = p.person_id
             LEFT JOIN
         `{BQ_DATASET}.visit_occurrence` v
         ON
             co.visit_occurrence_id = v.visit_occurrence_id
    WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) co
                   ON
                       co.person_id = p.person_id
         LEFT JOIN (
    SELECT d.person_id                                                          person_id,
           ARRAY_AGG(DISTINCT drug_concept_id)                                  drug_concept_ids,
           ARRAY_AGG(DISTINCT drug_source_concept_id)                           drug_source_concept_ids,
           ARRAY_AGG(STRUCT(drug_concept_id AS concept_id,
                            drug_source_concept_id AS source_concept_id,
                            drug_exposure_start_date AS start_date,
                            DATE_DIFF(drug_exposure_start_date,
                                      DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH),
                                      YEAR) AS age_at_start,
                            v.visit_concept_id,
                            CAST(NULL as FLOAT64) as value_as_number,
                            CAST(NULL as INT64) as value_as_concept_id,
                            CAST(NULL as INT64) as value_as_source_concept_id)) drugs
    FROM `{BQ_DATASET}.drug_exposure` d
             LEFT JOIN
         `{BQ_DATASET}.person` p
         ON
             d.person_id = p.person_id
             LEFT JOIN
         `{BQ_DATASET}.visit_occurrence` v
         ON
             d.visit_occurrence_id = v.visit_occurrence_id
    WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) d
                   ON
                       d.person_id = p.person_id
         LEFT JOIN (
    SELECT pr.person_id                                                         person_id,
           ARRAY_AGG(DISTINCT procedure_concept_id)                             procedure_concept_ids,
           ARRAY_AGG(DISTINCT procedure_source_concept_id)                      procedure_source_concept_ids,
           ARRAY_AGG(STRUCT(procedure_concept_id AS concept_id,
                            procedure_source_concept_id AS source_concept_id,
                            procedure_date AS start_date,
                            DATE_DIFF(procedure_date,
                                      DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH),
                                      YEAR) AS age_at_start,
                            v.visit_concept_id,
                            CAST(NULL as FLOAT64) as value_as_number,
                            CAST(NULL as INT64) as value_as_concept_id,
                            CAST(NULL as INT64) as value_as_source_concept_id)) procedures
    FROM `{BQ_DATASET}.procedure_occurrence` pr
             LEFT JOIN
         `{BQ_DATASET}.person` p
         ON
             pr.person_id = p.person_id
             LEFT JOIN
         `{BQ_DATASET}.visit_occurrence` v
         ON
             pr.visit_occurrence_id = v.visit_occurrence_id
    WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) pr
                   ON
                       pr.person_id = p.person_id
         LEFT JOIN (
    SELECT m.person_id                                                          person_id,
           ARRAY_AGG(DISTINCT measurement_concept_id)                           measurement_concept_ids,
           ARRAY_AGG(DISTINCT measurement_source_concept_id)                    measurement_source_concept_ids,
           ARRAY_AGG(STRUCT(measurement_concept_id AS concept_id,
                            measurement_source_concept_id AS source_concept_id,
                            measurement_date AS start_date,
                            DATE_DIFF(measurement_date,
                                      DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH),
                                      YEAR) AS age_at_start,
                            v.visit_concept_id,
                            value_as_number,
                            value_as_concept_id,
                            CAST(NULL as INT64) as value_as_source_concept_id)) measurements
    FROM `{BQ_DATASET}.measurement` m
             LEFT JOIN
         `{BQ_DATASET}.person` p
         ON
             m.person_id = p.person_id
             LEFT JOIN
         `{BQ_DATASET}.visit_occurrence` v
         ON
             m.visit_occurrence_id = v.visit_occurrence_id
    WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) m
                   ON
                       m.person_id = p.person_id
         LEFT JOIN (
    SELECT v.person_id,
           ARRAY_AGG(DISTINCT visit_concept_id) visit_concept_ids
    FROM `{BQ_DATASET}.visit_occurrence` v
    WHERE MOD(v.person_id, {PERSON_ID_MOD}) = 0
    GROUP BY 1) v
                   ON
                       v.person_id = p.person_id
         LEFT JOIN
     `{BQ_DATASET}.concept` gc
     ON
         p.gender_concept_id = gc.concept_id
         LEFT JOIN
     `{BQ_DATASET}.concept` rc
     ON
         p.race_concept_id = rc.concept_id
         LEFT JOIN
     `{BQ_DATASET}.concept` ec
     ON
         p.ethnicity_concept_id = ec.concept_id
         LEFT JOIN
     `{BQ_DATASET}.concept` sc
     ON
         p.sex_at_birth_concept_id = sc.concept_id
         LEFT JOIN
     `{BQ_DATASET}.death` death
     ON
         p.person_id = death.person_id
         LEFT JOIN
     `{BQ_DATASET}.cb_search_person` cbp
     ON
         p.person_id = cbp.person_id
WHERE MOD(p.person_id, {PERSON_ID_MOD}) = 0
