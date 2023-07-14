CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` AS
SELECT vid AS vid, person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_to_person`
CROSS JOIN UNNEST(person_ids) AS person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping` AS
SELECT ROW_NUMBER() OVER(ORDER BY person_id) AS id,
person_id AS person_id,
NULL AS synth_person_id
FROM (
    SELECT DISTINCT person_id,
    FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample`
);

UPDATE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping` x
SET x.synth_person_id = y.person_id
FROM (
  SELECT ROW_NUMBER() OVER(ORDER BY person_id) AS id,
  person_id
  FROM `all-of-us-ehr-dev.SC2022Q2R6.person`
) y
WHERE x.id = y.id;

DELETE
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
WHERE synth_person_id IS NULL;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 0;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth` AS
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 25000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 50000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 75000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 100000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 125000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 150000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 175000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 200000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping`
ORDER BY person_id
LIMIT 25000 OFFSET 225000;

INSERT INTO `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
SELECT pvs.vid, pvsspimt.synth_person_id AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample` pvs
JOIN `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth_person_id_mapping_temp` pvsspimt ON pvs.person_id = pvsspimt.person_id;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_to_person_synth` AS
WITH converted_person_ids AS (
SELECT vid, SAFE_CAST(person_id AS INT64) AS person_id
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_sample_synth`
)
SELECT vid,
       ARRAY_AGG(DISTINCT person_id IGNORE NULLS) person_ids
FROM converted_person_ids
GROUP BY vid;