#!/bin/bash

# This generates big query denormalized tables for search.

set -ex

export BQ_PROJECT=$1        # project
export BQ_DATASET=$2        # dataset
export DATA_BROWSER=$3      # data browser flag

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas

bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.cb_search_all_events
bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/cb_search_all_events.json --time_partitioning_type=DAY --clustering_fields concept_id $BQ_DATASET.cb_search_all_events


############################################################
# insert source condition data into cb_search_all_events
############################################################
echo "Inserting source conditions data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    co.condition_start_date as entry_date,
    co.condition_start_datetime as entry_datetime,
    0 as is_standard,
    co.condition_source_concept_id as concept_id,
    'Condition' as domain,
    DATE_DIFF(co.condition_start_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM co.condition_start_date)*100 + EXTRACT(DAY FROM co.condition_start_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = co.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = co.condition_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = co.visit_occurrence_id)
where co.condition_source_concept_id is not null
    and co.condition_source_concept_id != 0"

##############################################################
# insert standard condition data into cb_search_all_events
##############################################################
echo "Inserting standard conditions data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    co.condition_start_date as entry_date,
    co.condition_start_datetime as entry_datetime,
    1 as is_standard,
    co.condition_concept_id as concept_id,
    'Condition' as domain,
    DATE_DIFF(co.condition_start_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM co.condition_start_date)*100 + EXTRACT(DAY FROM co.condition_start_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = co.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = co.condition_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = co.visit_occurrence_id)
WHERE co.condition_concept_id is not null
    and co.condition_concept_id != 0"

############################################################
#   insert source procedure data into cb_search_all_events
############################################################
echo "Inserting source procedures data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    po.procedure_date as entry_date,
    po.procedure_datetime as entry_datetime,
    0 as is_standard,
    po.procedure_source_concept_id as concept_id,
    'Procedure' as domain,
    DATE_DIFF(po.procedure_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM po.procedure_date)*100 + EXTRACT(DAY FROM po.procedure_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = po.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = po.procedure_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = po.visit_occurrence_id)
WHERE po.procedure_source_concept_id is not null
    and po.procedure_source_concept_id != 0"

##############################################################
#   insert standard procedure data into cb_search_all_events
##############################################################
echo "Inserting standard procedures data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    po.procedure_date as entry_date,
    po.procedure_datetime as entry_datetime,
    1 as is_standard,
    po.procedure_concept_id as concept_id,
    'Procedure' as domain,
    DATE_DIFF(po.procedure_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM po.procedure_date)*100 + EXTRACT(DAY FROM po.procedure_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = po.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = po.procedure_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = po.visit_occurrence_id)
WHERE po.procedure_concept_id is not null
    and po.procedure_concept_id != 0"

##############################################################
# insert source measurement data into cb_search_all_events
##############################################################
echo "Inserting source measurement data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, systolic, diastolic)
SELECT p.person_id,
    m.measurement_date as entry_date,
    m.measurement_datetime as entry_datetime,
    0 as is_standard,
    m.measurement_source_concept_id as concept_id,
    'Measurement' as domain,
    DATE_DIFF(m.measurement_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM m.measurement_date)*100 + EXTRACT(DAY FROM m.measurement_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id,
    m.value_as_number,
    m.value_as_concept_id,
    case when measurement_source_concept_id = 903118 then m.value_as_number end as systolic,
    case when measurement_source_concept_id = 903115 then m.value_as_number end as diastolic
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = m.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = m.measurement_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = m.visit_occurrence_id)
WHERE m.measurement_source_concept_id is not null
    and m.measurement_source_concept_id != 0"

#####################################################################
# update source diastolic pressure data into cb_search_all_events
#####################################################################
echo "Updating diastolic pressure data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.diastolic = meas.diastolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as diastolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_source_concept_id = 903115
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 0
    -- this is intentional as we want to update diastolic on the systolic row
    and sad.concept_id = 903118"

#####################################################################
#   update source systolic pressure data into cb_search_all_events
#####################################################################
echo "Updating systolic pressure data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.systolic = meas.systolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as systolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_source_concept_id = 903118
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 0
    -- this is intentional as we want to update systolic on the diastolic row
    and sad.concept_id = 903115"

################################################################
#   insert standard measurement data into cb_search_all_events
################################################################
echo "Inserting measurement data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, systolic, diastolic)
SELECT p.person_id,
    m.measurement_date as entry_date,
    m.measurement_datetime as entry_datetime,
    1 as is_standard,
    m.measurement_concept_id as concept_id,
    'Measurement' as domain,
    DATE_DIFF(m.measurement_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM m.measurement_date)*100 + EXTRACT(DAY FROM m.measurement_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id,
    m.value_as_number,
    m.value_as_concept_id,
    case when measurement_concept_id = 903118 then m.value_as_number end as systolic,
    case when measurement_concept_id = 903115 then m.value_as_number end as diastolic
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = m.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = m.measurement_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = m.visit_occurrence_id)
WHERE m.measurement_concept_id is not null
    and m.measurement_concept_id != 0"

#####################################################################
#   update standard diastolic pressure data into cb_search_all_events
#######################################################################
echo "Updating diastolic pressure data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.diastolic = meas.diastolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as diastolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_concept_id = 903115
        GROUP BY m.person_id, m.measurement_datetime, diastolic
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 1
    -- this is intentional as we want to update diastolic on the systolic row
    and sad.concept_id = 903118"

#######################################################################
#   update standard systolic pressure data into cb_search_all_events
#######################################################################
echo "Updating systolic pressure data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.systolic = meas.systolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as systolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_concept_id = 903118
        GROUP BY m.person_id, m.measurement_datetime, systolic
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 1
    -- this is intentional as we want to update systolic on the diastolic row
    and sad.concept_id = 903115"

if [ "$DATA_BROWSER" == false ]
then
  ##############################################################
  # Observation - Source Data
  ##############################################################
  echo "cb_search_all_events - inserting observation source data"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
      (
            person_id
          , entry_date
          , entry_datetime
          , is_standard
          , concept_id
          , domain
          , age_at_event
          , visit_concept_id
          , visit_occurrence_id
          , value_as_number
          , value_as_concept_id
          , value_source_concept_id
          , survey_version_concept_id
      )
  SELECT
        b.person_id
      , a.observation_date as entry_date
      , a.observation_datetime as entry_datetime
      , 0 as is_standard
      , a.observation_source_concept_id as concept_id
      , 'Observation' as domain
      , DATE_DIFF(a.observation_date, DATE(b.birth_datetime), YEAR) -
          IF(EXTRACT(MONTH FROM DATE(b.birth_datetime))*100 + EXTRACT(DAY FROM DATE(b.birth_datetime))
          > EXTRACT(MONTH FROM a.observation_date)*100 + EXTRACT(DAY FROM a.observation_date), 1, 0) as age_at_event
      , d.visit_concept_id
      , d.visit_occurrence_id
      , a.value_as_number
      , a.value_as_concept_id
      , a.value_source_concept_id
      , c.survey_version_concept_id
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` b on a.person_id = b.person_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` c on a.observation_id = c.observation_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` d on a.visit_occurrence_id = d.visit_occurrence_id
  WHERE a.observation_source_concept_id is not null
      and a.observation_source_concept_id != 0"
else
  ##############################################################
  # Observation - Source Data
  ##############################################################
  echo "cb_search_all_events - inserting observation source data"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
      (
            person_id
          , entry_date
          , entry_datetime
          , is_standard
          , concept_id
          , domain
          , age_at_event
          , visit_concept_id
          , visit_occurrence_id
          , value_as_number
          , value_as_concept_id
          , value_source_concept_id
      )
  SELECT
        b.person_id
      , a.observation_date as entry_date
      , a.observation_datetime as entry_datetime
      , 0 as is_standard
      , a.observation_source_concept_id as concept_id
      , 'Observation' as domain
      , DATE_DIFF(a.observation_date, DATE(b.birth_datetime), YEAR) -
          IF(EXTRACT(MONTH FROM DATE(b.birth_datetime))*100 + EXTRACT(DAY FROM DATE(b.birth_datetime))
          > EXTRACT(MONTH FROM a.observation_date)*100 + EXTRACT(DAY FROM a.observation_date), 1, 0) as age_at_event
      , d.visit_concept_id
      , d.visit_occurrence_id
      , a.value_as_number
      , a.value_as_concept_id
      , a.value_source_concept_id
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` b on a.person_id = b.person_id
  LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` d on a.visit_occurrence_id = d.visit_occurrence_id
  WHERE a.observation_source_concept_id is not null
      and a.observation_source_concept_id != 0"
fi

################################################################
#   insert standard observation data into cb_search_all_events
################################################################
echo "Inserting observation data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, value_source_concept_id)
SELECT p.person_id,
    o.observation_date as entry_date,
    o.observation_datetime as entry_datetime,
    1 as is_standard,
    o.observation_concept_id as concept_id,
    'Observation' as domain,
    DATE_DIFF(o.observation_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM o.observation_date)*100 + EXTRACT(DAY FROM o.observation_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id,
    o.value_as_number,
    o.value_as_concept_id,
    o.value_source_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` o
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = o.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = o.observation_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = o.visit_occurrence_id)
WHERE o.observation_concept_id is not null
    and o.observation_concept_id != 0"

#######################################################
#   insert source drug data into cb_search_all_events
#######################################################
echo "Inserting drug data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    d.drug_exposure_start_date as entry_date,
    d.drug_exposure_start_datetime as entry_datetime,
    0 as is_standard,
    d.drug_source_concept_id as concept_id,
    'Drug' as domain,
    DATE_DIFF(d.drug_exposure_start_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM d.drug_exposure_start_date)*100 + EXTRACT(DAY FROM d.drug_exposure_start_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` d
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = d.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = d.drug_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = d.visit_occurrence_id)
WHERE d.drug_source_concept_id is not null
    and d.drug_source_concept_id != 0"

#########################################################
#   insert standard drug data into cb_search_all_events
#########################################################
echo "Inserting drug data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    d.drug_exposure_start_date as entry_date,
    d.drug_exposure_start_datetime as entry_datetime,
    1 as is_standard,
    d.drug_concept_id as concept_id,
    'Drug' as domain,
    DATE_DIFF(d.drug_exposure_start_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM d.drug_exposure_start_date)*100 + EXTRACT(DAY FROM d.drug_exposure_start_date),1,0) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` d
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = d.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = d.drug_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = d.visit_occurrence_id)
WHERE d.drug_concept_id is not null
    and d.drug_concept_id != 0"

##########################################################
#   insert standard visit data into cb_search_all_events
##########################################################
echo "Inserting visit data into cb_search_all_events"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    v.visit_start_date as entry_date,
    v.visit_start_datetime as entry_datetime,
    1 as is_standard,
    v.visit_concept_id as concept_id,
    'Visit' as domain,
    DATE_DIFF(v.visit_start_date,date(p.BIRTH_DATETIME), YEAR) - IF(EXTRACT(MONTH FROM date(p.BIRTH_DATETIME))*100 + EXTRACT(DAY FROM date(p.BIRTH_DATETIME)) > EXTRACT(MONTH FROM v.visit_start_date)*100 + EXTRACT(DAY FROM v.visit_start_date),1,0) as age_at_event,
    v.visit_concept_id,
    v.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = v.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = v.visit_concept_id)
WHERE v.visit_concept_id is not null
    and v.visit_concept_id != 0"