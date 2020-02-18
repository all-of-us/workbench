#!/bin/bash

# This generates big query denormalized tables for search.

set -xeuo pipefail
IFS=$'\n\t'


# get options

# --cdr=cdr_version ... *optional
USAGE="./generate-clousql-cdr/make-bq-denormalized-search.sh --bq-project <PROJECT> --bq-dataset <DATASET> --cdr-date <DATE>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --cdr-date) CDR_DATE=$2; shift 2;;
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

if [ -z "${CDR_DATE}" ]
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

bq --project=$BQ_PROJECT rm -f $BQ_DATASET.cb_search_person
bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/cb_search_person.json --time_partitioning_type=DAY --clustering_fields person_id $BQ_DATASET.cb_search_person

bq --project=$BQ_PROJECT rm -f $BQ_DATASET.cb_search_all_events
bq --quiet --project=$BQ_PROJECT mk --schema=$schema_path/cb_search_all_events.json --time_partitioning_type=DAY --clustering_fields concept_id $BQ_DATASET.cb_search_all_events


################################################
# insert person data into cb_search_person
################################################
echo "Inserting person data into cb_search_person"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
    (person_id, gender, race, ethnicity, dob)
SELECT p.person_id,
    case when p.gender_concept_id = 0 then 'Unknown' else g.concept_name end as gender,
    case when p.race_concept_id = 0 then 'Unknown' else regexp_replace(r.concept_name, r'^.+:\s', '') end as race,
    case when e.concept_name is null then 'Unknown' else regexp_replace(e.concept_name, r'^.+:\s', '') end as ethnicity,
    date(birth_datetime) as dob
FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)"

# add age_at_consent and age_at_cdr to each subject
# To get date_of_consent, we first try to find a consent date, if none, we fall back to Street Address: PII State
# If that does not exist, we fall back to the MINIMUM date of The Basics Survey
echo "adding age data to cb_search_person"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.age_at_consent = y.age_at_consent,
    x.age_at_cdr = y.age_at_cdr
FROM
    (
        SELECT person_id
            ,date_of_birth
            ,CAST(FLOOR(DATE_DIFF(date_of_consent, date_of_birth, month)/12) as INT64) as age_at_consent
            ,CAST(FLOOR(DATE_DIFF(date_of_cdr, date_of_birth, month)/12) as INT64) as age_at_cdr
        FROM
        (
            SELECT a.person_id
                , date(a.birth_datetime) as date_of_birth
                , coalesce(b.observation_date, c.observation_date, d.observation_date) as date_of_consent
                , date('$CDR_DATE') as date_of_cdr
            FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
            LEFT JOIN
            (
                -- Consent Date
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
                WHERE observation_source_concept_id = 1585482
            ) b on a.person_id = b.person_id
            LEFT JOIN
            (
                -- Street Address: PII State
                SELECT *
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
                WHERE observation_source_concept_id = 1585249
            ) c on a.person_id = c.person_id
            LEFT JOIN
            (
                -- The Basics
                SELECT person_id, MIN(observation_date) as observation_date
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
                WHERE observation_source_concept_id in
                    (
                        SELECT descendant_concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                        WHERE ancestor_concept_id = 1586134
                    )
                GROUP BY 1
            ) d on a.person_id = d.person_id
        )
    ) y
WHERE x.person_id = y.person_id
"

############################################################
# insert source condition data into cb_search_all_events
############################################################
echo "Inserting source conditions data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    co.condition_start_date as entry_date,
    co.condition_start_datetime as entry_datetime,
    0 as is_standard,
    co.condition_source_concept_id as concept_id,
    'Condition' as domain,
    cast(floor(date_diff(co.condition_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    co.condition_start_date as entry_date,
    co.condition_start_datetime as entry_datetime,
    1 as is_standard,
    co.condition_concept_id as concept_id,
    'Condition' as domain,
    cast(floor(date_diff(co.condition_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    po.procedure_date as entry_date,
    po.procedure_datetime as entry_datetime,
    0 as is_standard,
    po.procedure_source_concept_id as concept_id,
    'Procedure' as domain,
    cast(floor(date_diff(po.procedure_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    po.procedure_date as entry_date,
    po.procedure_datetime as entry_datetime,
    1 as is_standard,
    po.procedure_concept_id as concept_id,
    'Procedure' as domain,
    cast(floor(date_diff(po.procedure_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, systolic, diastolic)
SELECT p.person_id,
    m.measurement_date as entry_date,
    m.measurement_datetime as entry_datetime,
    0 as is_standard,
    m.measurement_source_concept_id as concept_id,
    'Measurement' as domain,
    cast(floor(date_diff(m.measurement_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.diastolic = meas.diastolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as diastolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_source_concept_id = 903115
        GROUP BY m.person_id, m.measurement_datetime, diastolic
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 0
    and sad.concept_id = 903118"

#####################################################################
#   update source systolic pressure data into cb_search_all_events
#####################################################################
echo "Updating diastolic pressure data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` sad
SET sad.systolic = meas.systolic
FROM
    (
        SELECT m.person_id,
            m.measurement_datetime,
            m.value_as_number as systolic
        FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` m
        WHERE m.measurement_source_concept_id = 903118
        GROUP BY m.person_id, m.measurement_datetime, systolic
    ) as meas
WHERE meas.person_id = sad.person_id
    and meas.measurement_datetime = sad.entry_datetime
    and sad.is_standard = 0
    and sad.concept_id = 903115"

################################################################
#   insert standard measurement data into cb_search_all_events
################################################################
echo "Inserting measurement data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, systolic, diastolic)
SELECT p.person_id,
    m.measurement_date as entry_date,
    m.measurement_datetime as entry_datetime,
    1 as is_standard,
    m.measurement_concept_id as concept_id,
    'Measurement' as domain,
    cast(floor(date_diff(m.measurement_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    and sad.concept_id = 903118"

#######################################################################
#   update standard diastolic pressure data into cb_search_all_events
#######################################################################
echo "Updating diastolic pressure data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
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
    and sad.concept_id = 903115"

##############################################################
#   insert source observation data into cb_search_all_events
##############################################################
echo "Inserting observation data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, value_source_concept_id)
SELECT p.person_id,
    o.observation_date as entry_date,
    o.observation_datetime as entry_datetime,
    0 as is_standard,
    o.observation_source_concept_id as concept_id,
    'Observation' as domain,
    cast(floor(date_diff(o.observation_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
    vo.visit_concept_id,
    vo.visit_occurrence_id,
    o.value_as_number,
    o.value_as_concept_id,
    o.value_source_concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` o
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = o.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = o.observation_source_concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` vo on (vo.visit_occurrence_id = o.visit_occurrence_id)
WHERE o.observation_source_concept_id is not null
    and o.observation_source_concept_id != 0"

################################################################
#   insert standard observation data into cb_search_all_events
################################################################
echo "Inserting observation data into cb_search_all_events"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id,
    visit_occurrence_id, value_as_number, value_as_concept_id, value_source_concept_id)
SELECT p.person_id,
    o.observation_date as entry_date,
    o.observation_datetime as entry_datetime,
    1 as is_standard,
    o.observation_concept_id as concept_id,
    'Observation' as domain,
    cast(floor(date_diff(o.observation_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    d.drug_exposure_start_date as entry_date,
    d.drug_exposure_start_datetime as entry_datetime,
    0 as is_standard,
    d.drug_source_concept_id as concept_id,
    'Drug' as domain,
    cast(floor(date_diff(d.drug_exposure_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    d.drug_exposure_start_date as entry_date,
    d.drug_exposure_start_datetime as entry_datetime,
    1 as is_standard,
    d.drug_concept_id as concept_id,
    'Drug' as domain,
    cast(floor(date_diff(d.drug_exposure_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
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
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`
    (person_id, entry_date, entry_datetime, is_standard, concept_id, domain, age_at_event, visit_concept_id, visit_occurrence_id)
SELECT p.person_id,
    v.visit_start_date as entry_date,
    v.visit_start_datetime as entry_datetime,
    1 as is_standard,
    v.visit_concept_id as concept_id,
    'Visit' as domain,
    cast(floor(date_diff(v.visit_start_date, date(p.year_of_birth, p.month_of_birth, p.day_of_birth), month)/12) as int64) as age_at_event,
    v.visit_concept_id,
    v.visit_occurrence_id
FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` v
JOIN \`$BQ_PROJECT.$BQ_DATASET.person\` p on p.person_id = v.person_id
JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (c.concept_id = v.visit_concept_id)
WHERE v.visit_concept_id is not null
    and v.visit_concept_id != 0"