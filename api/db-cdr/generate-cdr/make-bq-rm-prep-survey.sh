#!/bin/bash

# This removes prep_survey

set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset

bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"