#!/bin/bash

# This creates a backup of all cb_ and ds_ tables

set -e

export BQ_PROJECT=$1     # project
export BQ_DATASET=$2     # dataset
export OUTPUT_PROJECT=$3 # output project
export OUTPUT_DATASET=$4 # output dataset

BACKUP_DATASET=${OUTPUT_DATASET}_backup

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

# Make dataset for backup
datasets=$(bq --project_id="$OUTPUT_PROJECT" ls --max_results=1000)
if [[ $datasets =~ $BACKUP_DATASET ]]; then
  echo "$OUTPUT_PROJECT:$BACKUP_DATASET exists"
else
  echo "Creating $OUTPUT_PROJECT:$BACKUP_DATASET"
  bq --project_id="$OUTPUT_PROJECT" mk "$BACKUP_DATASET"
fi

# tables that are copied to backup dataset
backup_tables=(cb_criteria
cb_criteria_ancestor
cb_criteria_attribute
cb_criteria_menu
cb_criteria_relationship
cb_review_all_events
cb_review_survey
cb_search_all_events
cb_search_person
cb_survey_attribute
cb_survey_version
ds_activity_summary
ds_condition_occurrence
ds_data_dictionary
ds_device
ds_drug_exposure
ds_heart_rate_minute_level
ds_heart_rate_summary
ds_linking
ds_measurement
ds_observation
ds_person
ds_procedure_occurrence
ds_steps_intraday
ds_survey
ds_visit_occurrence
)

function cpTableToBackupDataset(){
  echo "Copying $BQ_PROJECT:$BQ_DATASET.$1 to $OUTPUT_PROJECT:$BACKUP_DATASET.$1"
  # --force to overwrite if backup dataset exists
  bq --project_id="$OUTPUT_PROJECT" cp --force "$BQ_PROJECT:$BQ_DATASET.$1" "$OUTPUT_PROJECT:$BACKUP_DATASET.$1"
}

for t in "${backup_tables[@]}"
do
  cpTableToBackupDataset "$t" &
done

# this will be for controlled tier only
if [[ "$TABLE_LIST" == *"ds_zip_code_socioeconomic"* ]]
then
  cpTableToBackupDataset "ds_zip_code_socioeconomic" &
fi

# wait to finish all jobs
wait
echo "Backing up cb_* and ds_* tables from $BQ_PROJECT:$BQ_DATASET to $OUTPUT_PROJECT:$BACKUP_DATASET complete."
