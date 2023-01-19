#!/bin/bash

# This script removes/creates all CDR indices specific tables.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

function testCiBashAssociativeEnabled(){
  declare -A C_TABLES
  C_TABLES["cb_search_all_events"]="concept_id"
  C_TABLES["cb_review_survey"]="person_id"
  C_TABLES["cb_search_person"]="person_id"
  C_TABLES["cb_review_all_events"]="person_id,domain"

  echo "Associative Array Keys: ${!C_TABLES[@]}"
  echo "Associative Array Values: ${C_TABLES[@]}"
  key="cb_review_survey"
  echo "Value for key: $key = ${C_TABLES[$key]} "
}
# remove this after running once on Circle CI
testCiBashAssociativeEnabled

function deleteAndCreateTable(){
  local table_name=$1
  # delete table - no error thrown if table does not exist
  echo "Deleting $table_name"
  bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$table_name"
  wait
  # if number of arguments = 2, create a clustered table
  if [[ $# -eq 2 ]]; then
      local cluster_fields=$2
      echo "Creating $table_name clustering_fields $cluster_fields"
      bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields "$cluster_fields" "$BQ_DATASET.$table_name"
  else
  # create a basic table
  echo "Creating $table_name"
  bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
  fi
  wait
}

INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4" "R2020Q4R3", "R2021Q3R5", "C2021Q2R1", "C2021Q3R6", "R2022Q2R2", "C2022Q2R2", "R2022Q2R6")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against $BQ_DATASET!"
  exit 1
fi

TABLE_LIST=($(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET" | tail -n +3 | cut -d " " -f 3 ))

SKIP_TABLES=("cb_data_filter" "cb_person" "survey_module" "domain_card")
CLUSTERED_TABLES=("cb_search_all_events" "cb_review_survey" "cb_search_person" "cb_review_all_events")

schema_path=generate-cdr/bq-schemas
for filename in generate-cdr/bq-schemas/*.json;
do
    json_name=${filename##*/}
    table_name=${json_name%.json}

    if [[ ${CLUSTERED_TABLES[@]} =~ "$table_name" ]]; then
      if [[ "$table_name" =~ cb_search_all_events ]]; then
        deleteAndCreateTable "$table_name" "concept_id"
      elif [[ "$table_name" =~ cb_review_survey|cb_search_person ]]; then
        deleteAndCreateTable "$table_name" "person_id"
      elif [[ "$table_name" =~ cb_review_all_events ]]; then
        deleteAndCreateTable "$table_name" "person_id,domain"
      fi
    elif [[ ${SKIP_TABLES[@]} =~ "$table_name"  ]]; then
      echo "Skipping table $table_name"
    elif [[ "$table_name" == 'ds_zip_code_socioeconomic' ]]; then
      if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
        deleteAndCreateTable "$table_name"
      fi
    elif [[ "$table_name" == 'prep_survey' && && "$TABLE_LIST" != *"prep_survey"* ]]; then
      if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
        deleteAndCreateTable "$table_name"
      fi
    else
      deleteAndCreateTable "$table_name"
    fi
done
