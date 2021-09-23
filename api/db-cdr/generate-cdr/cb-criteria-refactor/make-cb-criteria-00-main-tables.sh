#!/bin/bash

# This script does the following
#   populate all cb_criteria table *in order*
#   populate cb_criteria_* abd cb_survey_* tables

set -e
# vars are purposely hard-coded for iterative testing
export BQ_PROJECT='all-of-us-ehr-dev'      # project
export DATASET_PAR='ChenchalDummyPar'        # dataset
export DATASET_SEQ='ChenchalDummySeq'        # dataset
export DATASET_MULT='ChenchalDummyMult'        # dataset
export DATASET_ORI='ChenchalDummyOri'        # dataset

run_in_parallel=$1
if [[ $run_in_parallel == "par" ]]; then
  BQ_DATASET=$DATASET_PAR
elif [[ $run_in_parallel == "seq" ]]; then
  BQ_DATASET=$DATASET_SEQ
elif [[ $run_in_parallel == "mult" ]]; then
  BQ_DATASET=$DATASET_MULT
elif [[ $run_in_parallel == "ori" ]]; then
  echo "for 'ori' run make-bq-criteria-tables.sh script directly! Comment out prep tables..."
  exit 1
fi

################################################
# CREATE EMPTY CB_CRITERIA RELATED TABLES
################################################
function timeIt(){
  local e=$((SECONDS - $1))
  echo "$e"
}

function runScript(){
  source "$1" "$2" "$3" "$4"
  echo "Running script $f done in - $(timeIt st_time) secs - total time - $(timeIt main_start) secs"
  echo ""
}

## TODO check EXIST required tables?
main_tables=(
make-cb-criteria-01-cpt4-src.sh
make-cb-criteria-02-ppi-phys-meas.sh
make-cb-criteria-03-ppi-surveys.sh
make-cb-criteria-04-pm-concept-set.sh
make-cb-criteria-05-fitbit.sh
make-cb-criteria-06-whole-genome-variant.sh
make-cb-criteria-07-demographics.sh
make-cb-criteria-08-visit.sh
make-cb-criteria-09-icd9-src.sh
#make-cb-criteria-10-icd10-cm-src.sh
# make-cb-criteria-11-icd10-pcs-src.sh <== need to fix
)
n_procs=${#main_tables[@]}
main_start=$SECONDS
parProc=(par\ mult)
for (( i=0; i < $n_procs; i++ )) ; do
  f="${main_tables[$i]}"
  st_time=$SECONDS
  if [[ ${parProc[*]} =~ $run_in_parallel ]]; then
    runScript "$f" "$BQ_PROJECT" "$BQ_DATASET" "$run_in_parallel" &
    main_pids[${i}]=$!
    sleep 1
  else
    runScript "$f" "$BQ_PROJECT" "$BQ_DATASET" "$run_in_parallel"
  fi
done
# wait for all processes to finish
wait
echo "Running scripts *all from make-cb-criteria-00-main-tables.sh* done on $(timeIt main_start) secs"
echo ""


