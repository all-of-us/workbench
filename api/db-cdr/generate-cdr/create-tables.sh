#!/usr/bin/env bash

# using #!/usr/bin/env bash else need to change for local development
# #!/usr/bin/env bash -> we can use associative arrays
# #!/bin/bash -> we can use associative arrays

# This script removes/creates all CDR indices specific tables.
set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

export table_names_table="prep_create_tables_list"

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
  updateRowCounts $table_name
}

function createTableForRowCounts(){
  echo "Creating $table_names_table"
  ddl="CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.$table_names_table\` AS SELECT '' table_name, null row_count"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$ddl"
  wait
}

function updateRowCounts(){
  local table_name=$1
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$table_names_table\`
      (table_name, row_count)
      (SELECT '$table_name' as table_name, count(*) as row_count
      FROM \`$BQ_PROJECT.$BQ_DATASET.$table_name\`)
    "
  wait
}

INCOMPATIBLE_DATASETS=("R2019Q4R3" "R2019Q4R4" "R2020Q4R3", "R2021Q3R5", "C2021Q2R1", "C2021Q3R6", "R2022Q2R2", "C2022Q2R2", "R2022Q2R6")

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
    elif [[ "$table_name" == 'prep_survey' ]]; then
      if [[ "$TABLE_LIST" != *"prep_survey"* ]]; then
        deleteAndCreateTable "$table_name"
      else
        echo "Keeping existing prep_survey table"
      fi
    else
      deleteAndCreateTable "$table_name"
    fi
done
wait
echo "Done creating tables"

