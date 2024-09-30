#!/usr/bin/env bash

# This script creates all fitbit tables with id column.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

if [[ "$TABLE_LIST" == *"prep_activity_summary"* ]]
then
  echo "CREATE TABLE - activity_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.activity_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_activity_summary\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_activity_summary\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.activity_summary\`"
  
  echo "CREATE TABLE - activity_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.activity_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_activity_summary\`"
fi

if [[ "$TABLE_LIST" == *"prep_heart_rate_minute_level"* ]]
then
  echo "CREATE TABLE - heart_rate_minute_level"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_minute_level\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_minute_level\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\`"
  
  echo "CREATE TABLE - heart_rate_minute_level"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.heart_rate_minute_level\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_minute_level\`"
fi

if [[ "$TABLE_LIST" == *"prep_heart_rate_summary"* ]]
then
  echo "CREATE TABLE - heart_rate_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_summary\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_summary\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\`"
  
  echo "CREATE TABLE - heart_rate_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.heart_rate_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_heart_rate_summary\`"
fi

if [[ "$TABLE_LIST" == *"prep_sleep_daily_summary"* ]]
then
  echo "CREATE TABLE - sleep_daily_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_daily_summary\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_daily_summary\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\`"
  
  echo "CREATE TABLE - sleep_daily_summary"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.sleep_daily_summary\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_daily_summary\`"
fi

if [[ "$TABLE_LIST" == *"prep_sleep_level"* ]]
then
  echo "CREATE TABLE - sleep_level"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.sleep_level\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_level\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_level\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.sleep_level\`"
  
  echo "CREATE TABLE - sleep_level"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.sleep_level\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_sleep_level\`"
fi

if [[ "$TABLE_LIST" == *"prep_steps_intraday"* ]]
then
  echo "CREATE TABLE - steps_intraday"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_steps_intraday\`"
else
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.prep_steps_intraday\` AS
  SELECT ROW_NUMBER() OVER() AS row_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\`"
  
  echo "CREATE TABLE - steps_intraday"
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.steps_intraday\` AS
  SELECT * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_steps_intraday\`"
fi

echo "Done creating tables"