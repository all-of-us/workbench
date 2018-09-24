#!/bin/bash

# This generates big query denormalized tables.

set -xeuo pipefail
IFS=$'\n\t'


# get options

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-tables.sh --bq-project <PROJECT> --bq-dataset <DATASET>"

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

# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$BQ_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(p_all_events p_condition p_drug p_measurement p_observation p_procedure p_physical_measure)
for t in "${create_tables[@]}"
do
    bq --project=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/$t.json $BQ_DATASET.$t
done

# Populate some tables from cdr data

################################################
# insert condition data into person_condition #
################################################
echo "Inserting conditions data into person_condition"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_condition\`
 (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, source_name,
 source_code, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION)
SELECT P.PERSON_ID,
	t.CONDITION_OCCURRENCE_ID AS DATA_ID,
	t.CONDITION_START_DATETIME as START_DATETIME,
	c1.CONCEPT_NAME as STANDARD_NAME,
	c1.CONCEPT_CODE as STANDARD_CODE,
	C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
	c2.CONCEPT_NAME as SOURCE_NAME,
	c2.CONCEPT_CODE as SOURCE_CODE,
	c2.VOCABULARY_ID as SOURCE_VOCABULARY,
	CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
	CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
	CAST(FLOOR(DATE_DIFF(t.CONDITION_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	T.NUM_MENTIONS,
	T.FIRST_MENTION,
	T.LAST_MENTION
FROM
(SELECT CONDITION_OCCURRENCE_ID, a.PERSON_ID, a.CONDITION_CONCEPT_ID, CONDITION_START_DATE, CONDITION_START_DATETIME, VISIT_OCCURRENCE_ID,
a.CONDITION_SOURCE_CONCEPT_ID, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` A,
(SELECT PERSON_ID, CONDITION_CONCEPT_ID, CONDITION_SOURCE_CONCEPT_ID, COUNT(*) AS NUM_MENTIONS,
min(CONDITION_START_DATETIME) as FIRST_MENTION, max(CONDITION_START_DATETIME) as LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
GROUP BY PERSON_ID, CONDITION_CONCEPT_ID, CONDITION_SOURCE_CONCEPT_ID) B
WHERE a.PERSON_ID = b.PERSON_ID and a.CONDITION_CONCEPT_ID = b.CONDITION_CONCEPT_ID and a.CONDITION_SOURCE_CONCEPT_ID = b.CONDITION_SOURCE_CONCEPT_ID) t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.CONDITION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.CONDITION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

#####################################
# insert drug data into person_drug #
#####################################
echo "Inserting drug data into person_drug"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_drug\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, source_vocabulary,
   VISIT_ID, visit_concept_id, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, quantity, refills, strength, route)
SELECT P.PERSON_ID,
	 t.DRUG_EXPOSURE_ID AS DATA_ID,
     t.DRUG_EXPOSURE_START_DATETIME as START_DATETIME,
     c1.CONCEPT_NAME as STANDARD_NAME,
     c1.CONCEPT_CODE as STANDARD_CODE,
	 C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
     c2.CONCEPT_NAME as SOURCE_NAME,
     c2.CONCEPT_CODE as SOURCE_CODE,
     c2.VOCABULARY_ID as SOURCE_VOCABULARY,
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.DRUG_EXPOSURE_START_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	 T.NUM_MENTIONS,
	 T.FIRST_MENTION,
	 T.LAST_MENTION,
	 T.QUANTITY,
	 T.REFILLS,
	 '' as strength,
	 C3.CONCEPT_NAME AS ROUTE
FROM
(SELECT DRUG_EXPOSURE_ID, a.PERSON_ID, a.DRUG_CONCEPT_ID, DRUG_EXPOSURE_START_DATE, DRUG_EXPOSURE_START_DATETIME, VISIT_OCCURRENCE_ID,
a.DRUG_SOURCE_CONCEPT_ID, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION, REFILLS, QUANTITY, ROUTE_CONCEPT_ID
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
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

###################################################
# insert measurement data into person_measurement #
###################################################
echo "Inserting measurement data into person_measurement"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_measurement\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, 
 source_code, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event, value_concept, value_as_number, value_source_value, units, ref_range)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
     t.MEASUREMENT_DATETIME as START_DATETIME,
     c1.CONCEPT_NAME as STANDARD_NAME,
     c1.CONCEPT_CODE as STANDARD_CODE,
	 C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
     c2.CONCEPT_NAME as SOURCE_NAME,
     c2.CONCEPT_CODE as SOURCE_CODE,
	 c2.VOCABULARY_ID as SOURCE_VOCABULARY,
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.MEASUREMENT_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	 C4.CONCEPT_NAME AS VALUE_CONCEPT,
	 VALUE_AS_NUMBER,
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
# insert observation data into person_observation #
###################################################
echo "Inserting observation data into person_observation"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_observation\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, 
 source_code, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event)
SELECT P.PERSON_ID,
	 t.OBSERVATION_ID AS DATA_ID,
     t.OBSERVATION_DATETIME as START_DATETIME,
     c1.CONCEPT_NAME as STANDARD_NAME,
     c1.CONCEPT_CODE as STANDARD_CODE,
	 C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
     c2.CONCEPT_NAME as SOURCE_NAME,
     c2.CONCEPT_CODE as SOURCE_CODE,
     c2.VOCABULARY_ID as SOURCE_VOCABULARY,
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.OBSERVATION_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.OBSERVATION_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.OBSERVATION_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"

#################################################
# insert drug data into person_physical_measure #
#################################################
echo "Inserting drug data into person_physical_measure"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_physical_measure\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, 
   age_at_event, value_concept, value_as_number, value_source_value, units)
SELECT P.PERSON_ID,
	 t.MEASUREMENT_ID AS DATA_ID,
     t.MEASUREMENT_DATETIME as START_DATETIME,
     c1.CONCEPT_NAME as STANDARD_NAME,
     c1.CONCEPT_CODE as STANDARD_CODE,
	 C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
     CAST(FLOOR(DATE_DIFF(t.MEASUREMENT_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	 C2.CONCEPT_NAME AS VALUE_CONCEPT,
	 VALUE_AS_NUMBER,
	 VALUE_SOURCE_VALUE,
	 C3.CONCEPT_NAME AS UNITS
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
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_procedure\`
   (person_id, data_id, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, 
 source_code, source_vocabulary, VISIT_ID, visit_concept_id, age_at_event, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION)
SELECT P.PERSON_ID,
	 t.PROCEDURE_OCCURRENCE_ID AS DATA_ID,
     t.PROCEDURE_DATETIME as START_DATETIME,
     c1.CONCEPT_NAME as STANDARD_NAME,
     c1.CONCEPT_CODE as STANDARD_CODE,
	 C1.VOCABULARY_ID AS STANDARD_VOCABULARY,
     c2.CONCEPT_NAME as SOURCE_NAME,
     c2.CONCEPT_CODE as SOURCE_CODE,
     c2.VOCABULARY_ID as SOURCE_VOCABULARY,
     CASE WHEN t.VISIT_OCCURRENCE_ID is null then 0 else t.VISIT_OCCURRENCE_ID end as VISIT_ID,
     CASE WHEN v.visit_concept_id is null then 0 else v.visit_concept_id end as visit_concept_id,
     CAST(FLOOR(DATE_DIFF(t.PROCEDURE_DATE, DATE(p.YEAR_OF_BIRTH, p.MONTH_OF_BIRTH, p.DAY_OF_BIRTH), MONTH)/12) as INT64) as AGE_AT_EVENT,
	 T.NUM_MENTIONS,
	 T.FIRST_MENTION,
	 T.LAST_MENTION
FROM 
(SELECT PROCEDURE_OCCURRENCE_ID, a.PERSON_ID, a.PROCEDURE_CONCEPT_ID, PROCEDURE_DATE, PROCEDURE_DATETIME, VISIT_OCCURRENCE_ID, 
a.PROCEDURE_SOURCE_CONCEPT_ID, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` A,
(SELECT PERSON_ID, PROCEDURE_CONCEPT_ID, PROCEDURE_SOURCE_CONCEPT_ID, COUNT(*) AS NUM_MENTIONS, 
min(PROCEDURE_DATETIME) as FIRST_MENTION, max(PROCEDURE_DATETIME) as LAST_MENTION
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`
GROUP BY PERSON_ID, PROCEDURE_CONCEPT_ID, PROCEDURE_SOURCE_CONCEPT_ID) B
WHERE a.PERSON_ID = b.PERSON_ID and a.PROCEDURE_CONCEPT_ID = b.PROCEDURE_CONCEPT_ID and a.PROCEDURE_SOURCE_CONCEPT_ID = b.PROCEDURE_SOURCE_CONCEPT_ID) t
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.PROCEDURE_CONCEPT_ID = c1.CONCEPT_ID
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.PROCEDURE_SOURCE_CONCEPT_ID = c2.CONCEPT_ID
left join \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v on t.VISIT_OCCURRENCE_ID = v.VISIT_OCCURRENCE_ID
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.PERSON_ID = p.PERSON_ID"




################################################
# insert condition data into person_all_events #
################################################
echo "Inserting conditions data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, visit_type, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION)
 select person_id, data_id, 'Condition' as domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, b.concept_name as visit_type, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION
 from \`$BQ_PROJECT.$BQ_DATASET.p_condition\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

###########################################
# insert drug data into person_all_events #
###########################################
echo "Inserting drug data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, visit_type)
 select person_id, data_id, 'Drug' as domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, b.concept_name as visit_type
 from \`$BQ_PROJECT.$BQ_DATASET.p_drug\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

##################################################
# insert measurement data into person_all_events #
##################################################
echo "Inserting measurement data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, visit_type, source_value)
 select person_id, data_id, 'Measurement' as domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, b.concept_name as visit_type, CAST(value_as_number AS STRING) as source_value
 from \`$BQ_PROJECT.$BQ_DATASET.p_measurement\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

##################################################
# insert observation data into person_all_events #
##################################################
echo "Inserting observation data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, visit_type)
 select person_id, data_id, 'Observation' as domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, age_at_event, b.concept_name as visit_type
 from \`$BQ_PROJECT.$BQ_DATASET.p_observation\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"

################################################
# insert procedure data into person_all_events #
################################################
echo "Inserting procedure data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.p_all_events\`
 (person_id, data_id, domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, source_value, age_at_event, visit_type, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION)
 select person_id, data_id, 'Procedure' as domain, start_datetime, standard_name, standard_code, standard_vocabulary, source_name, source_code, 
 source_vocabulary, null as source_value, age_at_event, b.concept_name as visit_type, NUM_MENTIONS, FIRST_MENTION, LAST_MENTION
 from \`$BQ_PROJECT.$BQ_DATASET.p_procedure\` a
 join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id"
 