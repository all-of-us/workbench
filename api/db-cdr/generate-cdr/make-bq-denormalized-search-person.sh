#!/bin/bash

# This generates big query denormalized tables for search.

set -ex

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export WGV_PROJECT=$3       # whole genome variant project
export WGV_DATASET=$4       # whole genome variant dataset
export CDR_DATE=$5          # cdr date
export DRY_RUN=$6           # dry run

if [ "$DRY_RUN" == true ]
then
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.prep_concept_ancestor")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.person")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.concept")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.cb_criteria")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.condition_occurrence")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.procedure_occurrence")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.device_exposure")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.drug_exposure")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.observation")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.measurement")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.visit_occurrence")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.condition_occurrence_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.procedure_occurrence_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.device_exposure_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.drug_exposure_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.observation_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.measurement_ext")
  test=$(bq show "$BQ_PROJECT:$BQ_DATASET.visit_occurrence_ext")
  test=$(bq show "$WGV_PROJECT:$WGV_DATASET.metadata")
  exit 0
fi

# Test that datset exists
test=$(bq show "$BQ_PROJECT:$BQ_DATASET")
test=$(bq show "$WGV_PROJECT:$WGV_DATASET")
echo "$test"

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas

bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.cb_search_person
bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/cb_search_person.json --time_partitioning_type=DAY --clustering_fields person_id $BQ_DATASET.cb_search_person

################################################
# insert person data into cb_search_person
################################################
echo "Inserting person data into cb_search_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
    (
          person_id
        , gender
        , sex_at_birth
        , race
        , ethnicity
        , dob
        , is_deceased
        , has_fitbit
        , has_whole_genome_variant
    )
SELECT
      p.person_id
    , CASE
        WHEN p.gender_concept_id = 0 THEN 'Unknown'
        ELSE g.concept_name
      END as gender
    , CASE
        WHEN p.sex_at_birth_concept_id = 0 THEN 'Unknown'
        ELSE s.concept_name
      END as sex_at_birth
    , CASE
        WHEN p.race_concept_id = 0 THEN 'Unknown'
        ELSE regexp_replace(r.concept_name, r'^.+:\s', '')
      END as race
    , CASE
        WHEN e.concept_name is null THEN 'Unknown'
        ELSE regexp_replace(e.concept_name, r'^.+:\s', '')
      END as ethnicity
    , date(birth_datetime) as dob
    , CASE
        WHEN d.death_date is null THEN 0
        ELSE 1
      END is_deceased
    , CASE
        WHEN f.person_id is null THEN 0
        ELSE 1
      END has_fitbit
    , CASE
        WHEN w.sample_name is null THEN 0
        ELSE 1
      END has_whole_genome_variant
FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` s on (p.sex_at_birth_concept_id = s.concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)
LEFT JOIN
    (
        SELECT DISTINCT person_id, death_date
        FROM \`$BQ_PROJECT.$BQ_DATASET.death\`
    ) d on (p.person_id = d.person_id)
LEFT JOIN
    (
        SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`
        union distinct
        SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`
        union distinct
        SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`
        union distinct
        SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`
    ) f on (p.person_id = f.person_id)
LEFT JOIN \`$WGV_PROJECT.$WGV_DATASET.metadata\` w on (p.person_id = CAST(w.sample_name as int64))"

################################################
# calculate age_at_consent and age_at_cdr
################################################
# To get date_of_consent, we first try to find a consent date, if none, we fall back to Street Address: PII State
# If that does not exist, we fall back to the MINIMUM date of The Basics Survey
echo "adding age data to cb_search_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.age_at_consent = y.age_at_consent,
    x.age_at_cdr = y.age_at_cdr
FROM
    (
        SELECT person_id
            ,date_of_birth
            ,DATE_DIFF(date_of_consent,date_of_birth, YEAR) - IF(EXTRACT(MONTH FROM date_of_birth)*100 + EXTRACT(DAY FROM date_of_birth) > EXTRACT(MONTH FROM date_of_consent)*100 + EXTRACT(DAY FROM date_of_consent),1,0) as age_at_consent
            ,DATE_DIFF(date_of_cdr,date_of_birth, YEAR) - IF(EXTRACT(MONTH FROM date_of_birth)*100 + EXTRACT(DAY FROM date_of_birth) > EXTRACT(MONTH FROM date_of_cdr)*100 + EXTRACT(DAY FROM date_of_cdr),1,0) as age_at_cdr
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
WHERE x.person_id = y.person_id"

################################################
# set has physical measurement data flag
################################################
echo "set has physical measurement data flag in cb_search_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.has_physical_measurement_data = y.has_data
FROM
    (
        SELECT a.person_id, CASE WHEN b.person_id is not null THEN 1 ELSE 0 END AS has_data
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
        LEFT JOIN
            (
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
                WHERE measurement_source_concept_id in
                    (
                        SELECT concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.concept\`
                        WHERE vocabulary_id = 'PPI'
                            and concept_class_id = 'Clinical Observation'
                            and domain_id = 'Measurement'
                    )
            ) b on a.person_id = b.person_id
    ) y
WHERE x.person_id = y.person_id"

################################################
# set has ppi survey data flag
################################################
echo "set has ppi survey data flag in cb_search_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.has_ppi_survey_data = y.has_data
FROM
    (
        SELECT a.person_id, CASE WHEN b.person_id is not null THEN 1 ELSE 0 END AS has_data
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
        LEFT JOIN
            (
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\`
                WHERE observation_source_concept_id in
                    (
                        SELECT descendant_concept_id
                        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_concept_ancestor\`
                        WHERE ancestor_concept_id in
                            (
                                SELECT concept_id
                                FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
                                WHERE domain_id = 'SURVEY'
                                    and parent_id = 0
                            )
                    )
            ) b on a.person_id = b.person_id
    ) y
WHERE x.person_id = y.person_id"

################################################
# set has EHR data flag
################################################
echo "set has EHR data flag in cb_search_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.has_ehr_data = y.has_data
FROM
    (
        SELECT a.person_id, CASE WHEN b.person_id is not null THEN 1 ELSE 0 END AS has_data
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\` a
        LEFT JOIN
            (
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.measurement_ext\` as b on a.measurement_id = b.measurement_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` as b on a.condition_occurrence_id = b.condition_occurrence_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.device_exposure_ext\` as b on a.device_exposure_id = b.device_exposure_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` as b on a.drug_exposure_id = b.drug_exposure_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` as b on a.observation_id = b.observation_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` as b on a.procedure_occurrence_id = b.procedure_occurrence_id
                WHERE lower(b.src_id) like 'ehr site%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence_ext\` as b on a.visit_occurrence_id = b.visit_occurrence_id
                WHERE lower(b.src_id) like 'ehr site%'
            ) b on a.person_id = b.person_id
    ) y
WHERE x.person_id = y.person_id"