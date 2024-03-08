#!/bin/bash

# This generates big query denormalized tables for review.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset
export DOMAIN=$3

function do_survey(){
#########################################
# insert survey data into cb_review_survey #
#########################################
# Insert all survey data except COPE and PFHH
echo "Inserting survey data into cb_review_survey - except COPE and PFHH"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_survey\`
   (person_id, data_id, start_datetime, survey, question, answer)
SELECT  a.person_id,
        a.observation_id as data_id,
        case when a.observation_datetime is null then CAST(a.observation_date AS TIMESTAMP) else a.observation_datetime end as survey_datetime,
        c.name as survey,
        d.concept_name as question,
        case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else e.concept_name END as answer
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
                    and parent_id = 0
                    and concept_id not in (1741006, 1333342, 1740639)
            )
    ) b on a.observation_source_concept_id = b.descendant_concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c ON b.ancestor_concept_id = c.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.observation_source_concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id"

# Insert COPE survey data
echo "Inserting COPE survey data into cb_review_survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_survey\`
   (person_id, data_id, start_datetime, survey, question, answer)
SELECT  DISTINCT a.person_id,
        o.observation_id as data_id,
        case when a.entry_datetime is null then CAST(a.entry_date AS TIMESTAMP) else a.entry_datetime end as survey_datetime,
        f.concept_name as survey,
        d.concept_name as question,
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
                    AND concept_id IN (1333342)
            )
    ) b ON (a.concept_id = b.descendant_concept_id AND a.survey_concept_id = b.ancestor_concept_id)
JOIN \`$BQ_PROJECT.$BQ_DATASET.observation\` o ON (a.entry_date = o.observation_date AND a.person_id = o.person_id AND a.concept_id = o.observation_source_concept_id AND a.value_source_concept_id = o.value_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` f on b.ancestor_concept_id = f.concept_id
WHERE a.is_standard = 0"

# Insert PFHH survey data
echo "Inserting PFHH survey data into cb_review_survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_survey\`
   (person_id, start_datetime, survey, question, answer)
SELECT  DISTINCT a.person_id,
        case when a.entry_datetime is null then CAST(a.entry_date AS TIMESTAMP) else a.entry_datetime end as survey_datetime,
        'Personal and Family Health History' as survey,
        d.concept_name as question,
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
                    AND concept_id IN (1740639)
            )
    ) b on a.concept_id = b.descendant_concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.concept_id = d.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on a.value_source_concept_id = e.concept_id"

}

function do_drug(){
###########################################
# insert drug data into cb_review_all_events #
###########################################
echo "Inserting drug data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
 (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, standard_concept_id, source_name, source_code, source_vocabulary,
 source_concept_id, visit_type, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, dose, strength, route, domain)
SELECT P.PERSON_ID,
    t.DRUG_EXPOSURE_ID AS DATA_ID,
    case when t.DRUG_EXPOSURE_START_DATETIME is null then CAST(t.DRUG_EXPOSURE_START_DATE AS TIMESTAMP) else t.DRUG_EXPOSURE_START_DATETIME end as START_DATETIME,
    case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
    case when c1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
    case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
    case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
    case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else C2.VOCABULARY_ID end AS SOURCE_VOCABULARY,
    case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
    case when c4.CONCEPT_NAME is null then '' else c4.CONCEPT_NAME end as VISIT_TYPE,
    DATE_DIFF(t.DRUG_EXPOSURE_START_DATE,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM t.DRUG_EXPOSURE_START_DATE)*100 + EXTRACT(DAY FROM t.DRUG_EXPOSURE_START_DATE),1,0)  as AGE_AT_EVENT,
    T.NUM_MENTIONS,
    T.FIRST_MENTION,
    T.LAST_MENTION,
    T.QUANTITY as dose,
    '' as strength,
    case when C3.CONCEPT_NAME is null then '' else C3.CONCEPT_NAME end as ROUTE,
    'DRUG' as domain
FROM
(SELECT DRUG_EXPOSURE_ID, a.PERSON_ID, a.DRUG_CONCEPT_ID, a.DRUG_SOURCE_CONCEPT_ID, DRUG_EXPOSURE_START_DATE, DRUG_EXPOSURE_START_DATETIME, VISIT_OCCURRENCE_ID,
NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, QUANTITY, ROUTE_CONCEPT_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` A,
(SELECT PERSON_ID, DRUG_CONCEPT_ID, DRUG_SOURCE_CONCEPT_ID, COUNT(*) AS NUM_MENTIONS,
min(DRUG_EXPOSURE_START_DATETIME) as FIRST_MENTION, max(DRUG_EXPOSURE_START_DATETIME) as LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
GROUP BY PERSON_ID, DRUG_CONCEPT_ID, DRUG_SOURCE_CONCEPT_ID) B
WHERE a.PERSON_ID = b.PERSON_ID and a.DRUG_CONCEPT_ID = b.DRUG_CONCEPT_ID and a.DRUG_SOURCE_CONCEPT_ID = b.DRUG_SOURCE_CONCEPT_ID) t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.DRUG_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.DRUG_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on t.ROUTE_CONCEPT_ID = c3.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.VISIT_CONCEPT_ID = c4.CONCEPT_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"
}

function do_condition(){
################################################
# insert condition data into cb_review_all_events #
################################################
echo "Inserting conditions data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
 (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code,
 source_vocabulary, source_name, source_concept_id, age_at_event, visit_type, domain)
SELECT P.PERSON_ID,
	a.CONDITION_OCCURRENCE_ID AS DATA_ID,
	case when a.CONDITION_START_DATETIME is null then CAST(a.CONDITION_START_DATE AS TIMESTAMP) else a.CONDITION_START_DATETIME end as START_DATETIME,
  case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
  case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
	case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
  case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
	case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
  case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
	case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
	case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
	DATE_DIFF(a.CONDITION_START_DATE,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM a.CONDITION_START_DATE)*100 + EXTRACT(DAY FROM a.CONDITION_START_DATE),1,0) as AGE_AT_EVENT,
	case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
	'CONDITION' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONDITION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"
}

function do_lab(){
##########################################
# insert lab data into cb_review_all_events #
##########################################
echo "Inserting lab data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
SELECT m.person_id,
    m.measurement_id as data_id,
    case when m.measurement_datetime is null then CAST(m.measurement_date AS TIMESTAMP) else m.measurement_datetime end as START_DATETIME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
    case when c1.VOCABULARY_ID is null then 'None' else c1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
    case when c1.concept_name is null then 'No matching concept' else c1.concept_name end as standard_name,
    case when c1.concept_id is null then 0 else c1.concept_id end as standard_concept_id,
    case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
    case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
    case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
    case when m.value_as_number is null then m.value_as_concept_id else value_as_number end as value_as_number,
    m.unit_source_value as unit,
    case when range_low IS NULL and range_high IS NULL then NULL
              when range_low IS NULL and range_high iS NOT NULL then cast(range_high AS STRING)
              when range_low IS NOT NULL and range_high IS NULL then cast(range_low AS STRING)
              else concat(cast(range_low AS STRING) ,'-',cast(range_high AS STRING) )
              end as ref_range,
    DATE_DIFF(m.measurement_date,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM m.measurement_date)*100 + EXTRACT(DAY FROM m.measurement_date),1,0) as age_at_event,
    case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
    'LAB' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on m.measurement_concept_id = c1.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on m.measurement_source_concept_id = c2.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on m.visit_occurrence_id = v.visit_occurrence_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on m.person_id = p.person_id
where c1.concept_class_id = 'Lab Test'"
}

function do_vital(){
############################################
# insert vital data into cb_review_all_events #
############################################
echo "Inserting vital data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
    source_concept_id, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
SELECT m.person_id,
    m.measurement_id as data_id,
    case when m.measurement_datetime is null then CAST(m.measurement_date AS TIMESTAMP) else m.measurement_datetime end as START_DATETIME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
    case when c1.VOCABULARY_ID is null then 'None' else c1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
    case when c1.concept_name is null then 'No matching concept' else c1.concept_name end as standard_name,
    case when c1.concept_id is null then 0 else c1.concept_id end as standard_concept_id,
    case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
    case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
    case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
    case when m.value_as_number is null then m.value_as_concept_id else value_as_number end as value_as_number,
    m.unit_source_value as unit,
    case when range_low IS NULL and range_high IS NULL then NULL
              when range_low IS NULL and range_high iS NOT NULL then cast(range_high AS STRING)
              when range_low IS NOT NULL and range_high IS NULL then cast(range_low AS STRING)
              else concat(cast(range_low AS STRING) ,'-',cast(range_high AS STRING) )
              end as ref_range,
    DATE_DIFF(m.measurement_date,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM m.measurement_date)*100 + EXTRACT(DAY FROM m.measurement_date),1,0) as age_at_event,
    case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
    'VITAL' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on m.measurement_concept_id = c1.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on m.measurement_source_concept_id = c2.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on m.visit_occurrence_id = v.visit_occurrence_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on m.person_id = p.person_id
where c1.concept_class_id != 'Lab Test'"
}

function do_observation(){
###################################################
# insert observation data into cb_review_all_events #
###################################################
echo "Inserting observation data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name,
    source_code, source_concept_id, source_vocabulary, visit_type, age_at_event, domain)
SELECT P.PERSON_ID,
	 t.OBSERVATION_ID AS DATA_ID,
	 case when t.OBSERVATION_DATETIME is null then CAST(t.OBSERVATION_DATE AS TIMESTAMP) else t.OBSERVATION_DATETIME end as START_DATETIME,
   case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
   case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
   case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
	 case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
   case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
   case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
   case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
   case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
   case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
   DATE_DIFF(t.OBSERVATION_DATE,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM t.OBSERVATION_DATE)*100 + EXTRACT(DAY FROM t.OBSERVATION_DATE),1,0) as AGE_AT_EVENT,
   'OBSERVATION' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.OBSERVATION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.OBSERVATION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"
}

function do_physical_measurement(){
##########################################################
# insert physicalMeasurement data into cb_review_all_events #
##########################################################
echo "Inserting pm data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, value_as_number, unit, age_at_event, domain, visit_type)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
	   case when t.MEASUREMENT_DATETIME is null then CAST(t.MEASUREMENT_DATE AS TIMESTAMP) else t.MEASUREMENT_DATETIME end as START_DATETIME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when c1.VOCABULARY_ID is null then 'None' else c1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end AS SOURCE_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
     case when VALUE_AS_NUMBER is null then VALUE_AS_CONCEPT_ID else VALUE_AS_NUMBER end as VALUE_AS_NUMBER,
     c3.CONCEPT_NAME AS UNIT,
     DATE_DIFF(t.MEASUREMENT_DATE,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM t.MEASUREMENT_DATE)*100 + EXTRACT(DAY FROM t.MEASUREMENT_DATE),1,0) as AGE_AT_EVENT,
     'PHYSICAL_MEASUREMENT' as domain,
     case when c4.concept_name is null then '' else c4.concept_name end as visit_type
FROM
(select *
from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
where measurement_source_concept_id in
(select concept_id from \`$BQ_PROJECT.$BQ_DATASET.concept\` where vocabulary_id = 'PPI' and domain_id = 'Measurement' and CONCEPT_CLASS_ID = 'Clinical Observation')) t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.MEASUREMENT_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.MEASUREMENT_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on t.UNIT_CONCEPT_ID = c3.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.visit_concept_id = c4.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"
}

function do_procedure(){
################################################
# insert procedure data into cb_review_all_events #
################################################
echo "Inserting procedure data into cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, age_at_event, visit_type, domain)
SELECT P.PERSON_ID,
	 a.PROCEDURE_OCCURRENCE_ID AS DATA_ID,
	   case when a.PROCEDURE_DATETIME is null then CAST(a.PROCEDURE_DATE AS TIMESTAMP) else a.PROCEDURE_DATETIME end as START_DATETIME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
     DATE_DIFF(a.PROCEDURE_DATE,DATE(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM DATE(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM DATE(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM a.PROCEDURE_DATE)*100 + EXTRACT(DAY FROM a.PROCEDURE_DATE),1,0) as AGE_AT_EVENT,
     case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
     'PROCEDURE' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.PROCEDURE_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"
}

if [[ "$DOMAIN" = "survey" ]]; then
  do_survey
elif [[ "$DOMAIN" = "drug" ]]; then
  do_drug
elif [[ "$DOMAIN" = "condition" ]]; then
  do_condition
elif [[ "$DOMAIN" = "lab" ]]; then
  do_lab
elif [[ "$DOMAIN" = "vital" ]]; then
  do_vital
elif [[ "$DOMAIN" = "observation" ]]; then
  do_observation
elif [[ "$DOMAIN" = "physical_measurement" ]]; then
  do_physical_measurement
elif [[ "$DOMAIN" = "procedure" ]]; then
  do_procedure
else
  echo "Failed to build ds_ tables. Unknown domain $DOMAIN"
  exit 1
fi
