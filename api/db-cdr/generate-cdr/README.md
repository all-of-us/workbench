# Creating CDR indices

## Before building a brand new CDR indices we have to build pre-requisite prep tables. 

## Prep Tables Playbook
Please refer to the  [Prep Tables Playbook](https://docs.google.com/document/d/17B31LeN7fBLi84OJfpY6zjqS7NR3kIbztw3axl-5Tu4/edit#)
for a step-by-step guide to creating all pre-requisite tables. All relevant commands listed below, but the playbook should be referenced for usage.

## Prep Table commands

#### Copy over all static prep tables into a new CDR bucket
`./project.rb make-prep-tables-bucket --new-cdr-version C2021Q2R2 --previous-cdr-version C2021Q2R1`

#### Build prep ppi tables from surveys in redcap
`./project.rb make-prep-ppi-csv-files --project all-of-us-workbench-test --dataset DummySR --date 2021-04-21`

#### Build prep_survey table from prep ppi tables in previous command
`./project.rb make-bq-prep-survey --project all-of-us-workbench-test --dataset DummySR --date 2021-04-21 --tier controlled`

## CDR Indices Playbook
NOTE: All pre-requistie prep tables must exist, then CDR indices builds can be run as many times as required against the prep tables.
Please refer to the  [CDR Indices Playbook](https://docs.google.com/document/d/1St6pG_EUFB9oRQUQaOSO7a9UPxPkQ5n4qAVyKF9j9tk/edit#)
for a step-by-step guide.

## CDR indices commands

#### Full CDR indices build run in CircleCi
`./project.rb circle-build-cdr-indices --project all-of-us-rw-preprod --bq-dataset C2021Q2R1 --cdr-version c_2021q2_4 --wgv-project aou-res-curation-output-prod --wgv-dataset C2021Q2R1 --wgv-table prep_wgs_metadata`

