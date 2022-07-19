#!/bin/bash

# wb-personal-med-history-prep-survey-job:
#   <<: *java_defaults
#   steps:
#   - submodule_update
#   - activate_service_account_credential
#   - run:
#       name: create personal medical history and load into prep_survey table
#       no_output_timeout: 120m
#       working_directory: ~/cdr-indices/workbench/api
#       command: |
#         ./project.rb build-prep-survey \
#           --bq-project << pipeline.parameters.cdr_source_project >> \
#           --bq-dataset << pipeline.parameters.cdr_source_dataset >> \
#           --filename personalmedicalhistory_staged.csv \
#           --id-start-block 12000
#
# build-prep-survey.#!/bin/sh
# export BQ_PROJECT=$1         # CDR project
# export BQ_DATASET=$2         # CDR dataset
# export FILE_NAME=$3          # Filename to process
# export ID=$4                 # Starting id position
#
# BUCKET="all-of-us-workbench-private-cloudsql"
# SCHEMA_PATH="generate-cdr/bq-schemas"
# TEMP_FILE_DIR="csv"
# DATASET_DIR="$BQ_DATASET/cdr_csv_files"

####### Testing compare prep_survey table ####
# bq cp aou-res-curation-output-prod:R2022Q2R2.concept all-of-us-workbench-test:chenchals_survey_refactor.concept
# bq cp aou-res-curation-output-prod:R2022Q2R2.concept_synonym all-of-us-workbench-test:chenchals_survey_refactor.concept_synonym
# bq cp aou-res-curation-output-prod:R2022Q2R2.observation all-of-us-workbench-test:chenchals_survey_refactor.observation
# bq cp aou-res-curation-output-prod:R2022Q2R2.prep_survey all-of-us-workbench-test:chenchals_survey_refactor.prep_survey_orig
# bq cp aou-res-curation-output-prod:R2022Q2R2.prep_survey_concept_ancestor all-of-us-workbench-test:chenchals_survey_refactor.prep_survey_concept_ancestor_orig
# compare prep_survey to prep_survey_orig
################################################

set -ex

TBL_PS='prep_survey'

export BQ_PROJECT=all-of-us-workbench-test   #$1        # CDR project
export BQ_DATASET=chenchals_survey_refactor             #$2        # CDR dataset
# export FILE_NAME=personalmedicalhistory_staged.csv #$3  # Filename to process
export FILE_NAME=cope_staged.csv #$3  # Filename to process
export ID=13000   #$4 Starting id position

export ID_PREFIX=$ID

# TODO: fix me in relative path and for adding to project.rb
source ./cb-criteria-utils.sh
# TODO: fixme for running through project.rb
TBL_PS_STAGED_SCHEMA="./bq-schemas/prep_survey_staged.json"
TBL_PS_SCHEMA="./bq-schemas/prep_survey.json"

BUCKET="all-of-us-workbench-private-cloudsql"
SCHEMA_PATH="generate-cdr/bq-schemas"
TEMP_FILE_DIR="csv_new"
DATASET_DIR="$BQ_DATASET/cdr_csv_files"

# create temp table for prep_survey by copying schema
TBL_TEMP_PS="prep_temp_"$TBL_PS"_"$ID_PREFIX
TBL_TEMP_SURVEY="prep_temp_"$(basename $FILE_NAME .csv)

# create final table: prep_survey if not exist
echo "Checking if $TBL_PS exists"
if [[ -z $(bq ls --project_id=$BQ_PROJECT --dataset_id=$BQ_DATASET | grep -w $TBL_PS) ]];
then
  echo "Table $TBL_PS does not exist"
  echo "Creating empty $TBL_PS"
  bq mk -t "$BQ_PROJECT:$BQ_DATASET.$TBL_PS" $TBL_PS_SCHEMA
else
  echo "Table $TBL_PS exists"
fi

# these are temp tables for each survey
# remove all if they exist to start fresh
rmTmpTable $TBL_TEMP_PS
rmTmpTable $TBL_TEMP_SURVEY

# create temp prep [survey] tables for seeding the data
bq mk -t "$BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_PS" $TBL_PS_SCHEMA

# to local working_directory
rm -rf "$TEMP_FILE_DIR"
mkdir "$TEMP_FILE_DIR"

echo "downloading $FILE_NAME to local $TEMP_FILE_DIR"
gsutil cp gs://"$BUCKET/$DATASET_DIR/$FILE_NAME" "$TEMP_FILE_DIR"

#nl -v (start-offset) -i (increment-max-no-answers-per-question) -s (seperator)
echo "Adding line_no to lines in $TEMP_FILE_DIR/$FILE_NAME"
nl -v$((ID-50)) -i50 -s "|"  "$TEMP_FILE_DIR/$FILE_NAME" > "$TEMP_FILE_DIR/temp.csv"
mv "$TEMP_FILE_DIR/temp.csv" "$TEMP_FILE_DIR/$FILE_NAME"

echo "Loading $TEMP_FILE_DIR/$FILE_NAME to $BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_SURVEY"
bq load --skip_leading_rows=1 --allow_jagged_rows=false --allow_quoted_newlines=true \
        --field_delimiter='|' --source_format=CSV $BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_SURVEY \
        $TEMP_FILE_DIR/$FILE_NAME $TBL_PS_STAGED_SCHEMA

echo "Adding survey name to $TBL_TEMP_PS"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
       -- , value
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , survey
    )
SELECT
    $ID
    , 0 as parent_id
    , 'SURVEY' as domain_id
    , 0 as is_standard
    , 'PPI' as type
    , 'SURVEY' as subtype
    , c.concept_id
    , c.concept_code as code
    , c.concept_name as name
    -- , null as value
    , 1 as is_group
    , 1 as is_selectable
    , 0 as has_attribute
    , 1 as has_hierarchy
    , d.survey_name as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` c
JOIN \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` d ON lower(c.concept_code) = lower(d.concept_code)
WHERE c.concept_class_id in ('Module')"

echo "Adding topics to $TBL_TEMP_PS"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
      -- , concept_id
      -- , code
        , name
      -- , value
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , survey
    )
SELECT DISTINCT
      $ID + row_number() over (order by line_no) as id
    , $ID as parent_id
    , 'SURVEY' as domain_id
    , 0 as is_standard
    , 'PPI' as type
    , 'TOPIC' as subtype
 -- ,
 -- ,
    , pt.topic as name
 -- ,
    , 1 as is_group
    , 0 as is_selectable
    , 0 as has_attribute
    , 1 as has_hierarchy
    , pt.survey_name as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` pt
where pt.topic is not null"

echo "Updating value for topic column where missing for $TBL_TEMP_SURVEY"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
   "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` as
     select line_no
          , concept_code
          , survey_name
          , last_value(topic ignore nulls) over (order by line_no) topic
          , concat(answers, ' PMI_Skip') as answers
     from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\`"

echo "Adding Questions to $TBL_TEMP_PS"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
     -- , value
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , survey
    )
SELECT DISTINCT
      pt.line_no as id
    , pt2.id as parent_id
    , 'SURVEY' as domain_id
		, 0 as is_standard
		, 'PPI' as type
		, upper(c.concept_class_id) as subtype
		, c.concept_id as concept_id
		, c.concept_code as concept_code
		, CASE WHEN cs.concept_synonym_name is null THEN c.concept_name ELSE REPLACE(cs.concept_synonym_name, '|', ',') END AS name
 -- ,
		, 1 as is_group
		, 1 as is_selectable
		, 0 as has_attribute
		, 1 as has_hierarchy
		, pt.survey_name as survey
from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` pt
join \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\` pt2 on pt.topic = pt2.name
join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on lower(pt.concept_code) = lower(c.concept_code)
join \`$BQ_PROJECT.$BQ_DATASET.observation\` o on o.observation_source_concept_id = c.concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
     on (cs.concept_id = c.concept_id
     and lower(cs.concept_synonym_name) not like '%this condition?'
     and NOT STARTS_WITH(cs.concept_synonym_name, c.concept_code))"

# For answers unnet the answers column into rows
echo "Unnesting value for answers column into a row per answer for $TBL_TEMP_SURVEY"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
   "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` as
     select line_no + answer_index as line_no
          , concept_code
          , survey_name
          , topic
          , answer
     from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\`
     cross join unnest(split(answers, ' ')) as answer with offset as answer_index"


echo "Adding Answers to $TBL_TEMP_PS"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\`
    (
          id
        , parent_id
        , domain_id
        , is_standard
        , type
        , subtype
        , concept_id
        , code
        , name
        , value
        , is_group
        , is_selectable
        , has_attribute
        , has_hierarchy
        , survey
    )
SELECT DISTINCT
      id as id
    , ps.id as parent_id
    , 'SURVEY' as domain_id
		, 0 as is_standard
		, 'PPI' as type
		, upper(c.concept_class_id) as subtype
		, ps.concept_id as concept_id
		, c.concept_code as concept_code
		, CASE WHEN REGEXP_CONTAINS(c.concept_name,':') THEN REGEXP_EXTRACT(c.concept_name,': (.*)',1,1) ELSE c.concept_name END as name
		, concat('',c.concept_id)  as value
		, 0 as is_group
		, 1 as is_selectable
		, 0 as has_attribute
		, 0 as has_hierarchy
		, pt.survey_name as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\` ps
join \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` pt
      on ( lower(ps.code) = lower(pt.concept_code) and ps.subtype = 'QUESTION' ) \
join \`$BQ_PROJECT.$BQ_DATASET.concept\` c  on lower(pt.answer) = lower(c.concept_code)
WHERE c.concept_class_id = 'Answer'"



wait
cpToMainAndDeleteTmp "$TBL_TEMP_PS"
echo "done... $FILE_NAME"
