#!/bin/bash

# This generates the cb survey version table for cohort builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas

bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.cb_survey_version"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/cb_survey_version.json" "$BQ_DATASET.cb_survey_version"

###############################
# CREATE cb_survey_version TABLE
###############################
echo "Insert cb_survey_version"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`
    (survey_version_concept_id,survey_concept_id,display_name,display_order)
VALUES
(2100000002, 1333342, 'May 2020', 1),
(2100000003, 1333342, 'June 2020', 2),
(2100000004, 1333342, 'July 2020', 3)"

INSERT INTO `cb_survey_version` (`survey_version_concept_id`, `survey_concept_id`, `display_name`, `display_order`) VALUES
(2100000002, 1333342, 'May 2020', 1),
(2100000003, 1333342, 'June 2020', 2),
(2100000004, 1333342, 'July 2020', 3);