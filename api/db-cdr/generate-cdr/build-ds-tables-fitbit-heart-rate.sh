#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

################################################
# INSERT DATA FOR - FITBIT and HEART RATE
################################################

echo "ds_activity_summary - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_activity_summary\`
    (date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
    lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id)
SELECT date, activity_calories, calories_bmr, calories_out, elevation, fairly_active_minutes, floors,
    lightly_active_minutes, marginal_calories, sedentary_minutes, steps, very_active_minutes, person_id
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`)
where rank = 1"

echo "ds_heart_rate_minute_level - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_minute_level\`
    (datetime, person_id, heart_rate_value)
SELECT datetime, person_id, heart_rate_value
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`)
where rank = 1"

echo "ds_heart_rate_summary - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_summary\`
    (person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count)
SELECT person_id, date, zone_name, min_heart_rate, max_heart_rate, minute_in_zone, calorie_count
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`)
where rank = 1"

echo "ds_steps_intraday - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_steps_intraday\`
    (datetime, steps, person_id)
SELECT datetime, steps, person_id
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`)
where rank = 1"

echo "ds_sleep_daily_summary - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_sleep_daily_summary\`
    (person_id, sleep_date, is_main_sleep, minute_in_bed, minute_asleep, minute_after_wakeup, minute_awake, minute_restless, minute_deep, minute_light, minute_rem, minute_wake)
SELECT person_id, sleep_date, is_main_sleep, minute_in_bed, minute_asleep, minute_after_wakeup, minute_awake, minute_restless, minute_deep, minute_light, minute_rem, minute_wake
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`)
where rank = 1"

echo "ds_sleep_level - inserting data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_sleep_level\`
    (person_id, sleep_date, is_main_sleep, level, start_datetime, duration_in_min)
SELECT person_id, sleep_date, is_main_sleep, level, start_datetime, duration_in_min
FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY person_id ) AS rank
FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`)
where rank = 1"
