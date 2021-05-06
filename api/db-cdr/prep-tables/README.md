# Building prep_survey table

## How the build works
Overview of how this script builds the prep_survey table.

## Setup

`pip install google-cloud-bigquery`

`pip install google-cloud-storage`

## Run Python command to build prep_survey table

`cd workbench/api/db-cdr/prep-tables`

`python make-prep-survey.py --project all-of-us-workbench-test --dataset SR2019q4r3 --date 2021-04-21`