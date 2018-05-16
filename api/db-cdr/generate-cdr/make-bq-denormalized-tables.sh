#!/bin/bash

# This generates big query denormalized tables.

set -xeuo pipefail
IFS=$'\n\t'


# get options
# --project=all-of-us-workbench-test *required

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-tables.sh --bq-project <PROJECT> --bq-dataset <DATASET>"
USAGE="$USAGE --cdr-version=YYYYMMDD"

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
datasets=`bq --project=$BQ_PROJECT ls`
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
create_tables=(person_all_events person_condition person_device person_drug person_measurement person_observation person_procedure person_visit)
for t in "${create_tables[@]}"
do
    bq --project=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/$t.json $BQ_DATASET.$t
done

# Populate some tables from cdr data

################################################
# insert condition data into person_all_events #
################################################
echo "Inserting conditions data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
 (person_id, data_id, start_datetime, end_datetime, domain, standard_vocabulary,
 standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
 select p.person_id as person_id,
     t.condition_occurrence_id as data_id,
     t.condition_start_datetime as start_datetime,
     t.condition_end_datetime as end_datetime,
     'Condition' as domain,
     c1.vocabulary_id as standard_vocabulary,
     c1.concept_name as standard_name,
     c1.concept_code as standard_code,
     t.condition_source_value as source_value,
     c2.vocabulary_id as source_vocabulary,
     c2.concept_name as source_name,
     c2.concept_code as source_code,
     CAST(FLOOR(DATE_DIFF(t.condition_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` t
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.condition_concept_id = c1.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.condition_source_concept_id = c2.concept_id
join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

################################################
# insert procedure data into person_all_events #
################################################
echo "Inserting procedure data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
      t.procedure_occurrence_id as data_id,
      t.procedure_datetime as start_datetime,
      'Procedure' as domain,
      c1.vocabulary_id as standard_vocabulary,
      c1.concept_name as standard_name,
      c1.concept_code as standard_code,
      t.procedure_source_value as source_value,
      c2.vocabulary_id as source_vocabulary,
      c2.concept_name as source_name,
      c2.concept_code as source_code,
      CAST(FLOOR(DATE_DIFF(t.procedure_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
 from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` t
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.procedure_concept_id = c1.concept_id
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.procedure_source_concept_id = c2.concept_id
 join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

##################################################
# insert observation data into person_all_events #
##################################################
echo "Inserting observation data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
      t.observation_id as data_id,
      t.observation_datetime as start_datetime,
      'Observation' as domain,
      c1.vocabulary_id as standard_vocabulary,
      c1.concept_name as standard_name,
      c1.concept_code as standard_code,
      t.observation_source_value as source_value,
      c2.vocabulary_id as source_vocabulary,
      c2.concept_name as source_name,
      c2.concept_code as source_code,
      CAST(FLOOR(DATE_DIFF(t.observation_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
 from \`$BQ_PROJECT.$BQ_DATASET.observation\` t
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.observation_concept_id = c1.concept_id
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.observation_source_concept_id = c2.concept_id
 join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

##################################################
# insert measurement data into person_all_events #
##################################################
echo "Inserting measurement data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.measurement_id as data_id,
       t.measurement_datetime as start_datetime,
       'Measurement' as domain,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.measurement_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.measurement_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.measurement_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.measurement_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

###########################################
# insert drug data into person_all_events #
###########################################
echo "Inserting drug data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, end_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event, signature)
  select p.person_id as person_id,
       t.drug_exposure_id as data_id,
       t.drug_exposure_start_datetime as start_datetime,
       t.drug_exposure_end_datetime as end_datetime,
       'Drug' as domain,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.drug_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.drug_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event,
       sig as signature
  from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.drug_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.drug_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

#############################################
# insert device data into person_all_events #
#############################################
echo "Inserting device data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, end_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.device_exposure_id as data_id,
       t.device_exposure_start_datetime as start_datetime,
       t.device_exposure_end_datetime as end_datetime,
       'Device' as domain,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.device_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.device_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.device_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.device_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

############################################
# insert visit data into person_all_events #
############################################
echo "Inserting visit data into person_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_all_events\`
  (person_id, data_id, start_datetime, end_datetime, domain, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.visit_occurrence_id as data_id,
       t.visit_start_datetime as start_datetime,
       t.visit_end_datetime as end_datetime,
       'Visit' as domain,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.visit_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.visit_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.visit_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.visit_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

################################################
# insert condition data into person_condition #
################################################
echo "Inserting conditions data into person_condition"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_condition\`
 (person_id, data_id, start_datetime, standard_vocabulary,
 standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
 select p.person_id as person_id,
     t.condition_occurrence_id as data_id,
     t.condition_start_datetime as start_datetime,
     c1.vocabulary_id as standard_vocabulary,
     c1.concept_name as standard_name,
     c1.concept_code as standard_code,
     t.condition_source_value as source_value,
     c2.vocabulary_id as source_vocabulary,
     c2.concept_name as source_name,
     c2.concept_code as source_code,
     CAST(FLOOR(DATE_DIFF(t.condition_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` t
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.condition_concept_id = c1.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.condition_source_concept_id = c2.concept_id
join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

###############################################
# insert procedure data into person_procedure #
###############################################
echo "Inserting procedure data into person_procedure"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_procedure\`
  (person_id, data_id, start_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
      t.procedure_occurrence_id as data_id,
      t.procedure_datetime as start_datetime,
      c1.vocabulary_id as standard_vocabulary,
      c1.concept_name as standard_name,
      c1.concept_code as standard_code,
      t.procedure_source_value as source_value,
      c2.vocabulary_id as source_vocabulary,
      c2.concept_name as source_name,
      c2.concept_code as source_code,
      CAST(FLOOR(DATE_DIFF(t.procedure_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
 from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` t
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.procedure_concept_id = c1.concept_id
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.procedure_source_concept_id = c2.concept_id
 join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

###################################################
# insert observation data into person_observation #
###################################################
echo "Inserting observation data into person_observation"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_observation\`
  (person_id, data_id, start_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
      t.observation_id as data_id,
      t.observation_datetime as start_datetime,
      c1.vocabulary_id as standard_vocabulary,
      c1.concept_name as standard_name,
      c1.concept_code as standard_code,
      t.observation_source_value as source_value,
      c2.vocabulary_id as source_vocabulary,
      c2.concept_name as source_name,
      c2.concept_code as source_code,
      CAST(FLOOR(DATE_DIFF(t.observation_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
 from \`$BQ_PROJECT.$BQ_DATASET.observation\` t
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.observation_concept_id = c1.concept_id
 left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.observation_source_concept_id = c2.concept_id
 join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

###################################################
# insert measurement data into person_measurement #
###################################################
echo "Inserting measurement data into person_measurement"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_measurement\`
  (person_id, data_id, start_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.measurement_id as data_id,
       t.measurement_datetime as start_datetime,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.measurement_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.measurement_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.measurement_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.measurement_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

#####################################
# insert drug data into person_drug #
#####################################
echo "Inserting drug data into person_drug"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_drug\`
  (person_id, data_id, start_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event, signature)
  select p.person_id as person_id,
       t.drug_exposure_id as data_id,
       t.drug_exposure_start_datetime as start_datetime,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.drug_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.drug_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event,
       sig as signature
  from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.drug_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.drug_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

#########################################
# insert device data into person_device #
#########################################
echo "Inserting device data into person_device"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_device\`
  (person_id, data_id, start_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.device_exposure_id as data_id,
       t.device_exposure_start_datetime as start_datetime,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.device_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.device_exposure_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.device_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.device_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"

#######################################
# insert visit data into person_visit #
#######################################
echo "Inserting visit data into person_visit"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.person_visit\`
  (person_id, data_id, start_datetime, end_datetime, standard_vocabulary,
  standard_name, standard_code, source_value, source_vocabulary, source_name, source_code, age_at_event)
  select p.person_id as person_id,
       t.visit_occurrence_id as data_id,
       t.visit_start_datetime as start_datetime,
       t.visit_end_datetime as end_datetime,
       c1.vocabulary_id as standard_vocabulary,
       c1.concept_name as standard_name,
       c1.concept_code as standard_code,
       t.visit_source_value as source_value,
       c2.vocabulary_id as source_vocabulary,
       c2.concept_name as source_name,
       c2.concept_code as source_code,
       CAST(FLOOR(DATE_DIFF(t.visit_start_date, DATE(p.year_of_birth, p.month_of_birth, p.day_of_birth), MONTH)/12) as INT64) as age_at_event
  from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` t
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on t.visit_concept_id = c1.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on t.visit_source_concept_id = c2.concept_id
  join \`$BQ_PROJECT.$BQ_DATASET.person\` p on t.person_id = p.person_id"
