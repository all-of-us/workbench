#!/usr/bin/env bash

# using #!/usr/bin/env bash else need to change for local development
# #!/usr/bin/env bash -> we can use associative arrays
# #!/bin/bash -> we can use associative arrays

# This script removes/creates all CDR indices specific tables.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset
export CREATE_PREP_TABLES=$3     # Create surveys flag

function deleteAndCreateTable(){
  local table_name=$1
  # delete table - no error thrown if table does not exist
  echo "Deleting $table_name"
  bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$table_name"
  wait
  # if number of arguments = 2, create a clustered table
  if [[ $# -eq 2 ]]; then
    local cluster_fields=$2
    echo "Creating $table_name clustering_fields: $cluster_fields"
    bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" --time_partitioning_type=DAY --clustering_fields "$cluster_fields" "$BQ_DATASET.$table_name"
  else
    # create a basic table
    echo "Creating $table_name"
    bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/$json_name" "$BQ_DATASET.$table_name"
  fi
  wait
}

INCOMPATIBLE_DATASETS=("SR2022Q2R6" "SC2022Q2R6" "R2019Q4R6" "R2020Q4R5" "C2021Q2R1" "R2021Q3R8" "C2021Q3R8" "R2022Q2R6" "C2022Q2R6")

if [[ ${INCOMPATIBLE_DATASETS[@]} =~ $BQ_DATASET ]];
  then
  echo "Can't run CDR build indices against $BQ_DATASET!"
  exit 1
fi

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET" | tail -n +3 | cut -d " " -f 3 )

SKIP_TABLES=("cb_data_filter" "cb_person" "survey_module" "domain_card")

declare -A CLUSTERED_TABLES
CLUSTERED_TABLES["cb_search_all_events"]="concept_id"
CLUSTERED_TABLES["cb_review_survey"]="person_id"
CLUSTERED_TABLES["cb_search_person"]="person_id"
CLUSTERED_TABLES["cb_review_all_events"]="person_id,domain"

schema_path=generate-cdr/bq-schemas
for filename in generate-cdr/bq-schemas/*.json;
do
    json_name=${filename##*/}
    table_name=${json_name%.json}

    if [[ -n "${CLUSTERED_TABLES[$table_name]}" ]]; then
      deleteAndCreateTable "$table_name" "${CLUSTERED_TABLES[$table_name]}"
    elif [[ ${SKIP_TABLES[@]} =~ "$table_name"  ]]; then
      echo "Skipping table $table_name"
    elif [[ "$table_name" == 'ds_zip_code_socioeconomic' ]]; then
      if [[ "$TABLE_LIST" == *"zip3_ses_map"* ]]; then
        deleteAndCreateTable "$table_name"
      fi
    # We need to check for any tables that start with prep_ in case we are not building them
    # We need to check for ds_data_dictionary cause it's the only non prep_ table that is built in the build-static-prep-tables.sh
    elif [[ "$table_name" == prep* ]] || [[ "$table_name" == 'ds_data_dictionary' ]]; then
      if [[ "$CREATE_PREP_TABLES" == true ]]; then
        deleteAndCreateTable "$table_name"
      else
        echo "Keeping existing table: $table_name"
      fi
    else
      deleteAndCreateTable "$table_name"
    fi
done
wait
echo "Done creating tables"

