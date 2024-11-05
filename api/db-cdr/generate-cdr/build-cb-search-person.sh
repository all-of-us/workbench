#!/bin/bash

# This generates big query table cb_search_person.

set -e

export BQ_PROJECT=$1                # CDR project
export BQ_DATASET=$2                # CDR dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

# run this query to initializing our .bigqueryrc configuration file
# otherwise this will corrupt the output of the first call to find_info()
echo "Running a simple select to avoid problem with initializing our .bigqueryrc configuration file"
query="select count(*) from \`$BQ_PROJECT.$BQ_DATASET.concept\`"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query"

echo "Getting self_reported_category_concept_id column count"
query="select count(column_name) as count from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\`
where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\""
selfReportedCategoryCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

################################################
# insert person data into cb_search_person
################################################
if [[ ! "$BQ_DATASET" =~ (^C|^SC).* ]]
then
  
  if [[ $selfReportedCategoryCount > 0 ]];
  then
  
    echo "Inserting person data into cb_search_person for Registered Tier (RT)"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
        (
              person_id
            , gender
            , sex_at_birth
            , race
            , ethnicity
            , self_reported_category
            , dob
            , is_deceased
            , has_fitbit
            , state_of_residence
        )
    SELECT
          p.person_id
        , CASE
            WHEN p.gender_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(g.concept_name, r'^.+:\s', '')
          END as gender
        , CASE
            WHEN p.sex_at_birth_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(s.concept_name, r'^.+:\s', '')
          END as sex_at_birth
        , CASE
            WHEN p.race_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(r.concept_name, r'^.+:\s', '')
          END as race
        , CASE
            WHEN e.concept_name is null THEN 'Unknown'
            ELSE regexp_replace(e.concept_name, r'^.+:\s', '')
          END as ethnicity
        , CASE
            WHEN srp.concept_name is null THEN 'Unknown'
            ELSE regexp_replace(srp.concept_name, r'^.+:\s', '')
          END as self_reported_category
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
            WHEN p.state_of_residence_source_value like 'PII State%' THEN replace(p.state_of_residence_source_value, 'PII State: ','')
            ELSE null
          END state_of_residence
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` s on (p.sex_at_birth_concept_id = s.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` srp on (p.self_reported_category_concept_id = srp.concept_id)
    LEFT JOIN
        (
            SELECT person_id, max(death_date) as death_date
              FROM \`$BQ_PROJECT.$BQ_DATASET.death\`
            GROUP BY person_id
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
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`
        ) f on (p.person_id = f.person_id)"
        
  else
    
    echo "Inserting person data into cb_search_person for Registered Tier (RT)"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
        (
              person_id
            , gender
            , sex_at_birth
            , race
            , ethnicity
            , self_reported_category
            , dob
            , is_deceased
            , has_fitbit
            , state_of_residence
        )
    SELECT
          p.person_id
        , CASE
            WHEN p.gender_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(g.concept_name, r'^.+:\s', '')
          END as gender
        , CASE
            WHEN p.sex_at_birth_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(s.concept_name, r'^.+:\s', '')
          END as sex_at_birth
        , CASE
            WHEN p.race_concept_id = 0 THEN 'Unknown'
            ELSE regexp_replace(r.concept_name, r'^.+:\s', '')
          END as race
        , CASE
            WHEN e.concept_name is null THEN 'Unknown'
            ELSE regexp_replace(e.concept_name, r'^.+:\s', '')
          END as ethnicity
        , 'Unknown' as self_reported_category
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
            WHEN p.state_of_residence_source_value like 'PII State%' THEN replace(p.state_of_residence_source_value, 'PII State: ','')
            ELSE null
          END state_of_residence
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` s on (p.sex_at_birth_concept_id = s.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)
    LEFT JOIN
        (
            SELECT person_id, max(death_date) as death_date
              FROM \`$BQ_PROJECT.$BQ_DATASET.death\`
            GROUP BY person_id
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
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`
        ) f on (p.person_id = f.person_id)"
    
  fi
    
else
  
  if [[ $selfReportedCategoryCount > 0 ]];
    then
      
    echo "Inserting person data into cb_search_person for Controlled Tier (CT)"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
        (
              person_id
            , gender
            , sex_at_birth
            , race
            , ethnicity
            , self_reported_category
            , dob
            , is_deceased
            , has_fitbit
            , has_whole_genome_variant
            , has_array_data
            , has_lr_whole_genome_variant
            , has_structural_variant_data
            , state_of_residence
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
        , CASE
            WHEN srp.concept_name is null THEN 'Unknown'
            ELSE regexp_replace(srp.concept_name, r'^.+:\s', '')
          END as self_reported_category
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
        , CASE
            WHEN a.sample_name is null THEN 0
            ELSE 1
          END has_array_data
        , CASE
            WHEN lrw.sample_name is null THEN 0
            ELSE 1
          END has_lr_whole_genome_variant
        , CASE
            WHEN svt.sample_name is null THEN 0
            ELSE 1
          END has_structural_variant_data
        , CASE
            WHEN p.state_of_residence_source_value like 'PII State%' THEN replace(p.state_of_residence_source_value, 'PII State: ','')
            ELSE null
          END state_of_residence
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` s on (p.sex_at_birth_concept_id = s.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` srp on (p.self_reported_category_concept_id = srp.concept_id)
    LEFT JOIN
        (
            SELECT person_id, max(death_date) as death_date
              FROM \`$BQ_PROJECT.$BQ_DATASET.death\`
            GROUP BY person_id
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
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`
        ) f on (p.person_id = f.person_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_wgs_metadata\` w on (CAST(p.person_id as STRING) = w.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_longreads_metadata\` lrw on (CAST(p.person_id as STRING) = lrw.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_microarray_metadata\` a on (CAST(p.person_id as STRING) = a.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_structural_variants_metadata\` svt on (CAST(p.person_id as STRING) = svt.sample_name)"
    
  else
    
    echo "Inserting person data into cb_search_person for Controlled Tier (CT)"
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
        (
              person_id
            , gender
            , sex_at_birth
            , race
            , ethnicity
            , self_reported_category
            , dob
            , is_deceased
            , has_fitbit
            , has_whole_genome_variant
            , has_array_data
            , has_lr_whole_genome_variant
            , has_structural_variant_data
            , state_of_residence
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
        , 'Unknown' as self_reported_category
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
        , CASE
            WHEN a.sample_name is null THEN 0
            ELSE 1
          END has_array_data
        , CASE
            WHEN lrw.sample_name is null THEN 0
            ELSE 1
          END has_lr_whole_genome_variant
        , CASE
            WHEN svt.sample_name is null THEN 0
            ELSE 1
          END has_structural_variant_data
        , CASE
            WHEN p.state_of_residence_source_value like 'PII State%' THEN replace(p.state_of_residence_source_value, 'PII State: ','')
            ELSE null
          END state_of_residence
    FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` g on (p.gender_concept_id = g.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` s on (p.sex_at_birth_concept_id = s.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` r on (p.race_concept_id = r.concept_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` e on (p.ethnicity_concept_id = e.concept_id)
    LEFT JOIN
        (
            SELECT person_id, max(death_date) as death_date
              FROM \`$BQ_PROJECT.$BQ_DATASET.death\`
            GROUP BY person_id
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
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`
            union distinct
            SELECT person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`
        ) f on (p.person_id = f.person_id)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_wgs_metadata\` w on (CAST(p.person_id as STRING) = w.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_longreads_metadata\` lrw on (CAST(p.person_id as STRING) = lrw.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_microarray_metadata\` a on (CAST(p.person_id as STRING) = a.sample_name)
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_structural_variants_metadata\` svt on (CAST(p.person_id as STRING) = svt.sample_name)"
  
  fi
  
fi

################################################
# calculate age_at_consent and age_at_cdr
################################################
# To get date_of_consent, we first try to find a consent date, if none, we fall back to Street Address: PII State
# If that does not exist, we fall back to the MINIMUM date of The Basics Survey
echo "adding age data to cb_search_person"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
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
                , date((select max(entry_datetime) from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`)) as date_of_cdr
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
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
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
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
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
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
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
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence_ext\` as b on a.condition_occurrence_id = b.condition_occurrence_id
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.device_exposure\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.device_exposure_ext\` as b on a.device_exposure_id = b.device_exposure_id
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.drug_exposure_ext\` as b on a.drug_exposure_id = b.drug_exposure_id
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.observation_ext\` as b on a.observation_id = b.observation_id
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence_ext\` as b on a.procedure_occurrence_id = b.procedure_occurrence_id
                WHERE lower(b.src_id) like '%ehr%'
                UNION DISTINCT
                SELECT DISTINCT person_id
                FROM \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\` as a
                LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence_ext\` as b on a.visit_occurrence_id = b.visit_occurrence_id
                WHERE lower(b.src_id) like '%ehr%'
            ) b on a.person_id = b.person_id
    ) y
WHERE x.person_id = y.person_id"

################################################
# set fitbit tables flags
################################################
echo "set fitbit table flags in cb_search_person"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
SET x.has_fitbit_activity_summary = y.has_fitbit_activity_summary,
    x.has_fitbit_heart_rate_level = y.has_fitbit_heart_rate_level,
    x.has_fitbit_heart_rate_summary = y.has_fitbit_heart_rate_summary,
    x.has_fitbit_steps_intraday = y.has_fitbit_steps_intraday,
    x.has_fitbit_sleep_daily_summary = y.has_fitbit_sleep_daily_summary,
    x.has_fitbit_sleep_level = y.has_fitbit_sleep_level
FROM
    (
        SELECT
              p.person_id
            , CASE
                WHEN asum.person_id is null THEN 0
                ELSE 1
              END has_fitbit_activity_summary
            , CASE
                WHEN hrml.person_id is null THEN 0
                ELSE 1
              END has_fitbit_heart_rate_level
            , CASE
                WHEN hrs.person_id is null THEN 0
                ELSE 1
              END has_fitbit_heart_rate_summary
            , CASE
                WHEN si.person_id is null THEN 0
                ELSE 1
              END has_fitbit_steps_intraday
            , CASE
                WHEN sds.person_id is null THEN 0
                ELSE 1
              END has_fitbit_sleep_daily_summary
            , CASE
                WHEN sl.person_id is null THEN 0
                ELSE 1
              END has_fitbit_sleep_level
        FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`
            ) asum on (p.person_id = asum.person_id)
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`
            ) hrml on (p.person_id = hrml.person_id)
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`
            ) hrs on (p.person_id = hrs.person_id)
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`
            ) si on (p.person_id = si.person_id)
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`
            ) sds on (p.person_id = sds.person_id)
        LEFT JOIN
            (
                SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`
            ) sl on (p.person_id = sl.person_id)
    ) y
WHERE x.person_id = y.person_id"

if [[ "$TABLE_LIST" == *"wear_study"* ]];
then
  ################################################
  # set wear consent table flags
  ################################################
  echo "set wear consent table flags in cb_search_person"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\` x
  SET x.has_wear_consent = y.has_wear_consent
  FROM
      (
          SELECT
                p.person_id
              , CASE
                  WHEN ws.person_id is null THEN 0
                  ELSE 1
                END has_wear_consent
          FROM \`$BQ_PROJECT.$BQ_DATASET.person\` p
          LEFT JOIN
              (
                  SELECT distinct person_id FROM \`$BQ_PROJECT.$BQ_DATASET.wear_study\`
              ) ws on (p.person_id = ws.person_id)
      ) y
  WHERE x.person_id = y.person_id"
fi

