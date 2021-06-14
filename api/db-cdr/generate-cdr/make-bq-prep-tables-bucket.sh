#!/bin/bash

# This validates that prep tables, cohort builder menu, data dictionary, cope survey versions and whole genome variant tables exist.
set -e

export NEW_CDR=$1       # New CDR version
export PREVIOUS_CDR=$2  # Previous CDR version

BUCKET="all-of-us-workbench-private-cloudsql"

gsutil cp gs://$BUCKET/$PREVIOUS_CDR/cdr_csv_files gs://$BUCKET/$NEW_CDR/cdr_csv_files