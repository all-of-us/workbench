#!/bin/bash
# run test_survey with args for testing

echo "Task started at "`date`
echo "PID "$$

set -e

# hard coded vars:
export BQ_PROJECT=all-of-us-workbench-test   #$1        # CDR project
export BQ_DATASET=chenchals_survey_refactor             #$2        # CDR dataset
# export FILE_NAME=personalmedicalhistory_staged.csv #$3  # Filename to process
# export ID=13000   #$4 Starting id position

function timeIt(){
  local e=$((SECONDS - $1))
  echo "$e"
}

function runScript(){
  echo "$1 $2 $3 $4 $5"
  source "$1" "$2" "$3" "$4" "$5"
  echo "Running script $1 done in - $(timeIt st_time) secs - total time - $(timeIt main_start) secs"
  echo ""
}

starting_ID=10000

staged_surveys=(
basics_staged.csv
cope_staged.csv
familyhealthhistory_staged.csv
healthcareaccessutiliza_staged.csv
lifestyle_staged.csv
overallhealth_staged.csv
personalmedicalhistory_staged.csv
socialdeterminantsofhea_staged.csv
winterminutesurveyoncov_staged.csv
)
i=1
for f in "${staged_surveys[@]}"; do
  st_time=$SECONDS
    runScript "test_survey.sh" "$BQ_PROJECT" "$BQ_DATASET" "$f" $((starting_ID*i)) &
    sleep 1
    i=$((i+1))
done
wait
echo "All surveys done in $(timeIt main_start) secs"
echo ""
echo "Task ended at "`date`
echo "-----------------------------------------------------------"
