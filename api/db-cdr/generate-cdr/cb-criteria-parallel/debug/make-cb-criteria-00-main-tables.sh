#!/bin/bash

# This script does the following
#   populate all cb_criteria table *in order*
#   populate cb_criteria_* abd cb_survey_* tables
echo "-----------------------------------------------------------"
echo "Started at "`date`
echo "PID "$$

set -e
# vars are purposely hard-coded for iterative testing
export BQ_PROJECT=$1       # project - 'all-of-us-ehr-dev'
export BQ_DATASET=$2       # dataset - 'BillDummyMult'

run_in_parallel=$1

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
../make-cb-criteria-proc-cpt4.sh
../make-cb-criteria-ppi-phys-meas.sh
../make-cb-criteria-ppi-surveys.sh
../make-cb-criteria-pm-concept-set.sh
../make-cb-criteria-fitbit.sh
../make-cb-criteria-whole-genome-variant.sh
../make-cb-criteria-demographics.sh
../make-cb-criteria-visit.sh
../make-cb-criteria-icd9-src.sh
../make-cb-criteria-icd10-cm-src.sh
../make-cb-criteria-icd10-pcs-src.sh
../make-cb-criteria-cond-occur-snomed-std.sh
../make-cb-criteria-meas-clin-loinc-std.sh
../make-cb-criteria-meas-labs-loinc-std.sh
../make-cb-criteria-meas-snomed-std.sh
../make-cb-criteria-drug-rxnorm.sh
../make-cb-criteria-proc-occur-snomed-std.sh
../make-cb-criteria-observation.sh
)

echo "multi run"
i=1
for f in "${main_tables[@]}" ; do
  runScript "$f" "$BQ_PROJECT" "$BQ_DATASET" i &
  i=$i+1
done
# wait for all processes to finish
wait
run_in_order=(
../make-cb-criteria-seq-01-add-in-missing-codes.sh
../make-cb-criteria-seq-02-attrib-other-tables.sh
../make-cb-criteria-seq-03-clean-up-text-synonym.sh
)
for f in "${run_in_order[@]}" ; do
  runScript "$f" "$BQ_PROJECT" "$BQ_DATASET" seq
done
# wait to finish
wait
echo "Running scripts *all from make-cb-criteria-00-main-tables.sh* done in $(timeIt main_start) secs"
echo ""
echo "Ended main tables at "`date`
echo "-----------------------------------------------------------"

