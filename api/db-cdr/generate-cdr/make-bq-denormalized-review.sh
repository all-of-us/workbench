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
datasets=$(bq --project=$BQ_PROJECT ls)
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
create_tables=(person_all_events person_condition person_drug person_measurement person_lab person_vital person_procedure person_physical_measure person_survey p_observation)
for t in "${create_tables[@]}"
do
    bq --project=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/$t.json --time_partitioning_type=DAY --clustering_fields person_id $BQ_DATASET.$t
done

# Populate some tables from cdr data


################################################
# insert condition data into person_condition #
################################################
echo "Inserting conditions data into person_condition"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_condition\`
 (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, source_code,
 source_vocabulary, source_name, age_at_event, visit_type)
SELECT P.PERSON_ID,
	a.CONDITION_OCCURRENCE_ID AS DATA_ID,
	a.CONDITION_START_DATETIME as START_DATETIME,
    case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
	case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
    case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
	case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
	case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
    case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
	CAST(FLOOR(DATE_DIFF(a.CONDITION_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	case when c3.concept_name is null then 'No matching concept' else c3.concept_name end as visit_type
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONDITION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONDITION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"

#####################################
# insert drug data into person_drug #
#####################################
echo "Inserting drug data into person_drug"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_drug\`
 (person_id, data_id, start_datetime, standard_name, route, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, visit_type)
SELECT P.PERSON_ID,
    t.DRUG_EXPOSURE_ID AS DATA_ID,
    t.DRUG_EXPOSURE_START_DATETIME as START_DATETIME,
    case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
    case when c4.concept_name is null then 'No matching concept' else c4.concept_name end as visit_type,
    CAST(FLOOR(DATE_DIFF(t.DRUG_EXPOSURE_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
    T.NUM_MENTIONS,
    T.FIRST_MENTION,
    T.LAST_MENTION,
    C3.CONCEPT_NAME AS ROUTE
FROM
(SELECT DRUG_EXPOSURE_ID, a.PERSON_ID, a.DRUG_CONCEPT_ID, DRUG_EXPOSURE_START_DATE, DRUG_EXPOSURE_START_DATETIME, VISIT_OCCURRENCE_ID,
a.DRUG_SOURCE_CONCEPT_ID, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, REFILLS, ROUTE_CONCEPT_ID
FROM `all-of-us-ehr-dev.synthetic_cdr20180606.drug_exposure` A,
(SELECT PERSON_ID, DRUG_CONCEPT_ID, DRUG_SOURCE_CONCEPT_ID, COUNT(*) AS NUM_MENTIONS,
min(DRUG_EXPOSURE_START_DATETIME) as FIRST_MENTION, max(DRUG_EXPOSURE_START_DATETIME) as LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`
GROUP BY PERSON_ID, DRUG_CONCEPT_ID, DRUG_SOURCE_CONCEPT_ID) B
WHERE a.PERSON_ID = b.PERSON_ID and a.DRUG_CONCEPT_ID = b.DRUG_CONCEPT_ID and a.DRUG_SOURCE_CONCEPT_ID = b.DRUG_SOURCE_CONCEPT_ID) t

LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.DRUG_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.DRUG_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on t.ROUTE_CONCEPT_ID = c3.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on v.visit_concept_id = c4.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID

###################################################
# insert measurement data into person_measurement #
###################################################
echo "Inserting measurement data into person_measurement"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_measurement\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name,
 source_code, source_concept_id, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event, value_concept, value_as_number, value_source_value, units, ref_range)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
     t.MEASUREMENT_DATETIME as START_DATETIME,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when c1.CONCEPT_ID is null then 0 else c1.CONCEPT_ID end as STANDARD_CONCEPT_ID,
     case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.CONCEPT_ID is null then 0 else c2.CONCEPT_ID end as SOURCE_CONCEPT_ID,
	 case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.MEASUREMENT_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	 case when C4.CONCEPT_NAME is null then 'No matching concept' else C4.CONCEPT_NAME end AS VALUE_CONCEPT,
	 case when VALUE_AS_NUMBER is null then 0.0 else VALUE_AS_NUMBER end as VALUE_AS_NUMBER,
	 VALUE_SOURCE_VALUE,
	 C3.CONCEPT_NAME AS UNITS,
	 CASE WHEN RANGE_LOW IS NULL AND RANGE_HIGH IS NULL THEN NULL
          WHEN RANGE_LOW IS NULL AND RANGE_HIGH iS NOT NULL THEN CAST(RANGE_HIGH AS STRING)
          WHEN RANGE_LOW IS NOT NULL AND RANGE_HIGH IS NULL THEN CAST(RANGE_LOW AS STRING)
          ELSE CONCAT(CAST(RANGE_LOW AS STRING) ,'-',CAST(RANGE_HIGH AS STRING) )
          END as REF_RANGE
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.MEASUREMENT_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.MEASUREMENT_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on t.UNIT_CONCEPT_ID = c3.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c4 on t.VALUE_AS_CONCEPT_ID = c4.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"


###################################################
# insert lab data into person_lab #
###################################################
echo "Inserting lab data into person_lab"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_lab\`
   (person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type)
select a.person_id as PERSON_ID,
    a.DATA_ID,
    a.date,
    a.time,
    a.unit,
    case when c.CONCEPT_NAME is null then 'No matching concept' else c.CONCEPT_NAME end as STANDARD_NAME,
    case when a.VALUE_AS_NUMBER is null then 0.0 else a.VALUE_AS_NUMBER end as VALUE_AS_NUMBER,
	CASE WHEN a.RANGE_LOW IS NULL AND a.RANGE_HIGH IS NULL THEN NULL
          WHEN a.RANGE_LOW IS NULL AND a.RANGE_HIGH iS NOT NULL THEN CAST(a.RANGE_HIGH AS STRING)
          WHEN a.RANGE_LOW IS NOT NULL AND a.RANGE_HIGH IS NULL THEN CAST(a.RANGE_LOW AS STRING)
          ELSE CONCAT(CAST(a.RANGE_LOW AS STRING) ,'-',CAST(a.RANGE_HIGH AS STRING) )
          END as REF_RANGE,
    CAST(FLOOR(DATE_DIFF(a.date, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	case when c.concept_name is null then 'No matching concept' else c.concept_name end as visit_type
FROM (SELECT a1.person_id as PERSON_ID,
    a1.measurement_id as DATA_ID,
    a1.measurement_date as date,
    a1.measurement_datetime as time,
    a1.unit_source_value	 as unit,
    a1.VALUE_AS_NUMBER as VALUE_AS_NUMBER,
    a1.RANGE_LOW as RANGE_LOW,
    a1.RANGE_HIGH as RANGE_HIGH,
    a1.VISIT_OCCURRENCE_ID as VISIT_OCCURRENCE_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a1
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b1 on a1.measurement_concept_id = b1.concept_id
where concept_class_id = 'Lab Test') a
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on v.visit_concept_id = c.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID

###################################################
# insert vital data into person_vital #
###################################################
echo "Inserting vital data into person_vital"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_vital\`
   (person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type)
select a.person_id as PERSON_ID,
    a.DATA_ID,
    a.date,
    a.time,
    a.unit,
    case when c.CONCEPT_NAME is null then 'No matching concept' else c.CONCEPT_NAME end as STANDARD_NAME,
    case when a.VALUE_AS_NUMBER is null then 0.0 else a.VALUE_AS_NUMBER end as VALUE_AS_NUMBER,
	CASE WHEN a.RANGE_LOW IS NULL AND a.RANGE_HIGH IS NULL THEN NULL
          WHEN a.RANGE_LOW IS NULL AND a.RANGE_HIGH iS NOT NULL THEN CAST(a.RANGE_HIGH AS STRING)
          WHEN a.RANGE_LOW IS NOT NULL AND a.RANGE_HIGH IS NULL THEN CAST(a.RANGE_LOW AS STRING)
          ELSE CONCAT(CAST(a.RANGE_LOW AS STRING) ,'-',CAST(a.RANGE_HIGH AS STRING) )
          END as REF_RANGE,
    CAST(FLOOR(DATE_DIFF(a.date, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	case when c.concept_name is null then 'No matching concept' else c.concept_name end as visit_type
FROM (SELECT a1.person_id as PERSON_ID,
    a1.measurement_id as DATA_ID,
    a1.measurement_date as date,
    a1.measurement_datetime as time,
    a1.unit_source_value	 as unit,
    a1.VALUE_AS_NUMBER as VALUE_AS_NUMBER,
    a1.RANGE_LOW as RANGE_LOW,
    a1.RANGE_HIGH as RANGE_HIGH,
    a1.VISIT_OCCURRENCE_ID as VISIT_OCCURRENCE_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a1
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b1 on a1.measurement_concept_id = b1.concept_id
where concept_class_id = 'Lab Test') a
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on v.visit_concept_id = c.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID



###################################################
# insert observation data into p_observation #
###################################################
echo "Inserting observation data into p_observation"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_observation\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name,
 source_code, source_concept_id, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event)
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
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.OBSERVATION_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.OBSERVATION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.OBSERVATION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

###################################################
# insert survey data into person_survey #
###################################################
echo "Inserting survey data into person_survey"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_survey\`
   (person_id, data_id, start_datetime, survey, question, answer)
SELECT PERSON_ID,
    DATA_ID,
    START_DATETIME,
    survey,
    question,
    answer
FROM
(SELECT person_id,
    observation_id as DATA_ID,
    observation_datetime as START_DATETIME,
    observation_source_concept_id as concept_id,
    case when observation_source_concept_id = 1585747 then CAST(value_as_number as STRING) else concept_name end as answer
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b1 on a.value_as_concept_id = b1.concept_id
where a.observation_source_concept_id in
(select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'PPI' and is_group = 0 and is_selectable = 1)) x
join
(select b2.name as survey,
    c1.name as question,
    c1.concept_id,
    b2.id as s_id,
    c1.id as q_id
FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor\` a1
left join \`$BQ_PROJECT.$BQ_DATASET.criteria\` b2 on a1.ancestor_id = b2.id
left join \`$BQ_PROJECT.$BQ_DATASET.criteria\` c1 on a1.descendant_id = c1.id
where ancestor_id in
(select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'PPI' and parent_id = 0)) y on x.concept_id = y.concept_id
order by person_id, s_id, q_id

#################################################
# insert physicalMeasurement data into person_physical_measure #
#################################################
echo "Inserting drug data into person_physical_measure"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_physical_measure\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, value_as_number,
   units, age_at_event)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
     t.MEASUREMENT_DATETIME as START_DATETIME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when VALUE_AS_NUMBER is null then VALUE_AS_CONCEPT_ID else VALUE_AS_NUMBER end as VALUE_AS_NUMBER,
     C2.CONCEPT_NAME AS UNITS
     CAST(FLOOR(DATE_DIFF(t.MEASUREMENT_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
FROM
(select *
from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
where measurement_source_concept_id in
(select concept_id from \`$BQ_PROJECT.$BQ_DATASET.concept\` where vocabulary_id = 'PPI' and domain_id = 'Measurement' and CONCEPT_CLASS_ID = 'Clinical Observation')) t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.MEASUREMENT_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.VALUE_AS_CONCEPT_ID = c2.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on t.UNIT_CONCEPT_ID = c3.CONCEPT_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

###############################################
# insert procedure data into person_procedure #
###############################################
echo "Inserting procedure data into person_procedure"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_procedure\`
   (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, source_code,
   source_vocabulary, source_name, age_at_event, visit_type)
SELECT P.PERSON_ID,
	 a.PROCEDURE_OCCURRENCE_ID AS DATA_ID,
     a.PROCEDURE_DATETIME as START_DATETIME,
     case when c1.CONCEPT_CODE is null then 'No matching concept' else c1.CONCEPT_CODE end as STANDARD_CODE,
     case when C1.VOCABULARY_ID is null then 'None' else C1.VOCABULARY_ID end AS STANDARD_VOCABULARY,
     case when c1.CONCEPT_NAME is null then 'No matching concept' else c1.CONCEPT_NAME end as STANDARD_NAME,
     case when c2.CONCEPT_CODE is null then 'No matching concept' else c2.CONCEPT_CODE end as SOURCE_CODE,
     case when c2.VOCABULARY_ID is null then 'None' else c2.VOCABULARY_ID end as SOURCE_VOCABULARY,
     case when c2.CONCEPT_NAME is null then 'No matching concept' else c2.CONCEPT_NAME end as SOURCE_NAME,
     CAST(FLOOR(DATE_DIFF(t.PROCEDURE_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
     case when c3.concept_name is null then 'No matching concept' else c3.concept_name end as visit_type

FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.PROCEDURE_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on a.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c3 on v.visit_concept_id = c3.concept_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on a.PERSON_ID = p.PERSON_ID"

################################################
# insert condition data into person_all_events #
################################################
echo "Inserting conditions data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
 (person_id, data_id, start_datetime, visit_type, standard_code, standard_vocabulary, standard_name, source_code, source_vocabulary,
 source_name, domain, age_at_event)
 select person_id, data_id, start_datetime, visit_type, standard_code, standard_vocabulary, standard_name, source_code,
 source_vocabulary, source_name, 'Condition' as domain, age_at_event
 from \`$BQ_PROJECT.$BQ_DATASET.person_condition\` a"

###########################################
# insert drug data into person_all_events #
###########################################
echo "Inserting drug data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
 (person_id, data_id, start_datetime, standard_name, route, strength, domain, age_at_event, num_mentions, first_mention, last_mention, visit_type)
 select person_id, data_id, start_datetime, standard_name, route, strength, 'Drug' as domain, age_at_event, num_mentions, first_mention,
 last_mention, visit_type
 from \`$BQ_PROJECT.$BQ_DATASET.person_drug\` a"

##################################################
# insert measurement data into person_all_events #
##################################################
echo "Inserting measurement data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name, source_code,
 source_concept_id, source_vocabulary, age_at_event, visit_type, source_value)
 select person_id, data_id, 'Measurement' as domain, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name, source_code,
 source_concept_id, source_vocabulary, age_at_event, b.concept_name as visit_type, CAST(value_as_number AS STRING) as source_value
 from \`$BQ_PROJECT.$BQ_DATASET.person_measurement\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

 ###########################################
 # insert lab data into person_all_events #
 ###########################################
 echo "Inserting lab data into person_all_events"
 bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
 "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
  select person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type,'LAB' as domain
  from \`$BQ_PROJECT.$BQ_DATASET.person_lab\` a"

 ###########################################
 # insert vital data into person_all_events #
 ###########################################
 echo "Inserting vital data into person_all_events"
 bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
 "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type, domain)
  select person_id, data_id, date, time, standard_name, value_as_number, unit, ref_range, age_at_event, visit_type,'LAB' as domain
  from \`$BQ_PROJECT.$BQ_DATASET.person_vital\` a"

##################################################
# insert observation data into person_all_events #
##################################################
echo "Inserting observation data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name, source_code,
 source_concept_id, source_vocabulary, age_at_event, visit_type)
 select person_id, data_id, 'Observation' as domain, start_datetime, standard_name, standard_code, standard_concept_id, standard_vocabulary, source_name, source_code,
 source_concept_id, source_vocabulary, age_at_event, b.concept_name as visit_type
 from \`$BQ_PROJECT.$BQ_DATASET.p_observation\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

################################################
# insert procedure data into person_all_events #
################################################
echo "Inserting procedure data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, source_code,
  source_vocabulary, source_name, age_at_event, visit_type)
  select person_id, data_id, start_datetime, standard_code, standard_vocabulary, standard_name, source_code,
  source_vocabulary, source_name, age_at_event, visit_type 'Procedure' as domain
  from \`$BQ_PROJECT.$BQ_DATASET.person_procedure\` a"
