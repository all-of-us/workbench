SELECT
  p.person_id _id,
  DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH) birth_datetime,
  p.gender_concept_id,
  gc.concept_name gender_concept_name,
  p.race_concept_id,
  rc.concept_name race_concept_name,
  p.ethnicity_concept_id,
  ec.concept_name ethnicity_concept_name,
  condition_concept_ids,
  condition_source_concept_ids,
  conditions
FROM
  `{BQ_DATASET}.person` p
LEFT JOIN (
  SELECT
    co.person_id person_id,
    ARRAY_AGG(DISTINCT condition_concept_id) condition_concept_ids,
    ARRAY_AGG(DISTINCT condition_concept_id) condition_source_concept_ids,
    ARRAY_AGG(STRUCT( condition_concept_id AS concept_id,
        condition_source_concept_id AS source_concept_id,
        condition_start_date AS start_date,
        DATE_DIFF(condition_start_date, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), YEAR) AS age_at_start_date,
        v.visit_type_concept_id,
        NULL AS value_as_number,
        NULL AS value_as_concept)) conditions
  FROM
    `{BQ_DATASET}.condition_occurrence` co
  LEFT JOIN
    `{BQ_DATASET}.person` p
  ON
    co.person_id = p.person_id
  LEFT JOIN
    `{BQ_DATASET}.visit_occurrence` v
  ON
    co.visit_occurrence_id = v.visit_occurrence_id
  WHERE
    MOD(p.person_id, {PERSON_ID_MOD}) = 0
  GROUP BY
    1) co
ON
  co.person_id = p.person_id
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
WHERE
  MOD(p.person_id, {PERSON_ID_MOD}) = 0
