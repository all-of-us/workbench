# Building prep_survey table

## How the build works
Overview of how this script builds the prep_ppi_xxx tables.

## Setup

`pip install google-cloud-bigquery`

`pip install google-cloud-storage`

## Run Python command to build prep_ppi_xxx tables

`cd workbench/api/db-cdr/prep-ppi-tables`

`python make-prep-ppi-tables.py --project all-of-us-workbench-test --dataset SR2019q4r3 --date 2021-04-21`