#!/bin/bash

# This script removes/creates all CDR indices specific tables.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

function createBasicTable(){
  local table_name=$1
  echo "Creating $table_name"
  bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
  wait
}

function createClusteredTable(){
  local table_name=$1
  local cluster_fields=$2
  echo "Creating $table_name clustering_fields $cluster_fields"
  bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields "$cluster_fields" "$BQ_DATASET.$table_name"
  wait
}
SKIP_TABLES=("cb_data_filter" "cb_person" "survey_module" "domain_card")
CLUSTERED_TABLES=("cb_search_all_events" "cb_review_survey" "cb_search_person" "cb_review_all_events")

TABLE_LIST=($(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET" | tail -n +3 | cut -d " " -f 3 ))

INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4" "R2020Q4R3", "R2021Q3R5", "C2021Q2R1", "C2021Q3R6", "R2022Q2R2", "C2022Q2R2", "R2022Q2R6")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against $BQ_DATASET!"
  exit 1
fi

schema_path=generate-cdr/bq-schemas
for filename in generate-cdr/bq-schemas/*.json;
do
    json_name=${filename##*/}
    table_name=${json_name%.json}
    if [[ ${CLUSTERED_TABLES[@]} =~ "$table_name" ]]; then
      if [[ "$table_name" =~ cb_search_all_events ]]; then
        createClusteredTable "$table_name" "concept_id"
      elif [[ "$table_name" =~ cb_review_survey|cb_search_person ]]; then
        createClusteredTable "$table_name" "person_id"
      elif [[ "$table_name" =~ cb_review_all_events ]]; then
        createClusteredTable "$table_name" "person_id,domain"
      fi
    elif [[ ${SKIP_TABLES[@]} =~ "$table_name"  ]]; then
      echo "Skipping table $table_name"
    elif [[ "$table_name" == 'ds_zip_code_socioeconomic' ]]; then
      if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
        createBasicTable "$table_name"
      fi
    else
      createBasicTable "$table_name"
    fi
done
