#!/bin/bash

# This generates big query denormalized tables for review.

set -xeuo pipefail
IFS=$'\n\t'


# get options

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-review.sh --bq-project <PROJECT> --bq-dataset <DATASET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done


if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi


# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls --max_results=150)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$BQ_DATASET.* ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas

bq --project=$BQ_PROJECT rm -f $BQ_DATASET.cb_review_survey
bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/cb_review_survey.json --time_partitioning_type=DAY --clustering_fields person_id $BQ_DATASET.cb_review_survey

#########################################
# insert survey data into cb_review_survey #
#########################################
echo "Inserting survey data into cb_review_survey"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_survey\`
   (person_id, data_id, start_datetime, survey, question, answer)
SELECT y.person_id,
    y.data_id,
    y.survey_datetime,
    x.survey,
    x.question,
    y.answer
FROM
    (
        SELECT s.name as survey,
            d.concept_name as question,
            q.concept_id as question_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` s on a.ancestor_id = s.id
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` q on a.descendant_id = q.id
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on q.concept_id = d.concept_id
        WHERE ancestor_id in
            (
                SELECT id
                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                WHERE domain_id = 'SURVEY'
                    and parent_id = 0
            )
    ) x
JOIN
    (
        SELECT a.person_id,
            a.observation_id as data_id,
            a.observation_datetime as survey_datetime,
            a.observation_source_concept_id as question_concept_id,
            case when a.value_as_number is not null then CAST(a.value_as_number as STRING) else b.concept_name END as answer,
            a.value_source_concept_id as answer_concept_id
        FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
        LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_source_concept_id = b.concept_id
    ) y on x.question_concept_id = y.question_concept_id"

# Create bq tables we have json schema for
bq --project=$BQ_PROJECT rm -f $BQ_DATASET.cb_review_all_events
bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/cb_review_all_events.json --time_partitioning_type=DAY --clustering_fields person_id,domain $BQ_DATASET.cb_review_all_events

###########################################
# insert drug data into cb_review_all_events #
###########################################
echo "Inserting drug data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
 (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, standard_concept_id, source_name, source_code, source_vocabulary,
 source_concept_id, visit_type, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, dose, strength, route, domain)
SELECT P.PERSON_ID,
    t.DRUG_EXPOSURE_ID AS DATA_ID,
    t.DRUG_EXPOSURE_START_DATETIME as START_DATETIME,
    case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
    case when c1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
    case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
    case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
    case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else C2.VOCABULARY_ID end AS SOURCE_VOCABULARY,
    case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
    case when c4.CONCEPT_NAME is null then '' else c4.CONCEPT_NAME end as VISIT_TYPE,
    CAST(FLOOR(DATE_DIFF(t.DRUG_EXPOSURE_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
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

################################################
# insert condition data into cb_review_all_events #
################################################
echo "Inserting conditions data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
 (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code,
 source_vocabulary, source_name, source_concept_id, age_at_event, visit_type, domain)
SELECT P.PERSON_ID,
	a.CONDITION_OCCURRENCE_ID AS DATA_ID,
	a.CONDITION_START_DATETIME as START_DATETIME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
    case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
	case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
    case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
	case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
	case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
	case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
	CAST(FLOOR(DATE_DIFF(a.CONDITION_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
	'CONDITION' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONDITION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"

##########################################
# insert lab data into cb_review_all_events #
##########################################
echo "Inserting lab data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
SELECT m.person_id,
    m.measurement_id as data_id,
    m.measurement_datetime as start_datetime,
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
    CAST(FLOOR(DATE_DIFF(m.measurement_date, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as age_at_event,
    case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
    'LAB' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on m.measurement_concept_id = c1.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on m.measurement_source_concept_id = c2.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on m.visit_occurrence_id = v.visit_occurrence_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on m.person_id = p.person_id
where c1.concept_class_id = 'Lab Test'"

############################################
# insert vital data into cb_review_all_events #
############################################
echo "Inserting vital data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
    source_concept_id, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
SELECT m.person_id,
    m.measurement_id as data_id,
    m.measurement_datetime as start_datetime,
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
    CAST(FLOOR(DATE_DIFF(m.measurement_date, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as age_at_event,
    case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
    'VITAL' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on m.measurement_concept_id = c1.concept_id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on m.measurement_source_concept_id = c2.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on m.visit_occurrence_id = v.visit_occurrence_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on m.person_id = p.person_id
where c1.concept_class_id != 'Lab Test'"

###################################################
# insert observation data into cb_review_all_events #
###################################################
echo "Inserting observation data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name,
 source_code, source_concept_id, source_vocabulary, visit_type, age_at_event, domain)
SELECT P.PERSON_ID,
	 t.OBSERVATION_ID AS DATA_ID,
     t.OBSERVATION_DATETIME as START_DATETIME,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
	 case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
     case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
     case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
     CAST(FLOOR(DATE_DIFF(t.OBSERVATION_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
     'OBSERVATION' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.OBSERVATION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.OBSERVATION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

##########################################################
# insert physicalMeasurement data into cb_review_all_events #
##########################################################
echo "Inserting pm data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, value_as_number, unit, age_at_event, domain, visit_type)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
     t.MEASUREMENT_DATETIME as START_DATETIME,
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
     CAST(FLOOR(DATE_DIFF(t.MEASUREMENT_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
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

################################################
# insert procedure data into cb_review_all_events #
################################################
echo "Inserting procedure data into cb_review_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, standard_concept_id, source_code, source_vocabulary, source_name,
   source_concept_id, age_at_event, visit_type, domain)
SELECT P.PERSON_ID,
	 a.PROCEDURE_OCCURRENCE_ID AS DATA_ID,
     a.PROCEDURE_DATETIME as START_DATETIME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
     CAST(FLOOR(DATE_DIFF(a.PROCEDURE_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
     case when c3.concept_name is null then '' else c3.concept_name end as visit_type,
     'PROCEDURE' as domain
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.PROCEDURE_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"