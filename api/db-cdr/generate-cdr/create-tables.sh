#!/bin/bash

# This script removes/creates all CDR indices specific tables.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4", "R2020Q4R3")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against $BQ_DATASET!"
  exit 1
fi

schema_path=generate-cdr/bq-schemas
for filename in bq-schemas/*.json;
do
    json_name=${filename##*/}
    table_name=${json_name%.json}
    echo "Deleting $table_name"
    bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$table_name"
    if [[ "$table_name" == 'cb_search_all_events' ]]
    then
      echo "Creating $table_name"
      bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields concept_id "$BQ_DATASET.$table_name"
    elif [[ "$table_name" == 'cb_review_survey' || "$table_name" == 'cb_search_person' ]]
    then
      echo "Creating $table_name"
      bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields person_id "$BQ_DATASET.$table_name"
    elif [[ "$table_name" == 'cb_review_all_events' ]]
    then
      echo "Creating $table_name"
      bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields person_id,domain "$BQ_DATASET.$table_name"
    elif [[ "$table_name" == 'ds_zip_code_socioeconomic' ]]
    then
      if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]
      then
        echo "Creating $table_name"
        bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
      fi
    else
      echo "Creating $table_name"
      bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
    fi
done