#!/bin/bash
# 07-29-2022 - V6_HG_done
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
# compare prep_survey to prep_survey_orig
################################################

set -e

TBL_PS='prep_survey'

export BQ_PROJECT=$1 #all-of-us-workbench-test   #$1        # CDR project
export BQ_DATASET=$2 #chenchals_survey_refactor             #$2        # CDR dataset
# export FILE_NAME=personalmedicalhistory_staged.csv #$3  # Filename to process
export FILE_NAME=$3 # cope_staged.csv #$3  # Filename to process
export ID=$4 #13000   #$4 Starting id position

MAX_ANS_PER_Q=100

# TODO: fix me in relative path and for adding to project.rb
source ./cb-criteria-utils.sh
# TODO: fixme for running through project.rb
TBL_PS_STAGED_SCHEMA="./bq-schemas/prep_survey_staged.json"
TBL_PS_SCHEMA="./bq-schemas/prep_survey.json"

BUCKET="all-of-us-workbench-private-cloudsql"
SCHEMA_PATH="generate-cdr/bq-schemas"
TEMP_FILE_DIR="csv_new"
DATASET_DIR="$BQ_DATASET/cdr_csv_files"

# to local working_directory
if [ ! -d $TEMP_FILE_DIR ]; then
  mkdir "$TEMP_FILE_DIR"
fi

echo "downloading $FILE_NAME to local $TEMP_FILE_DIR"
gsutil cp gs://"$BUCKET/$DATASET_DIR/$FILE_NAME" "$TEMP_FILE_DIR"

#nl -v (start-offset) -i (increment-max-no-answers-per-question) -s (seperator)
echo "Adding line_no to lines in $TEMP_FILE_DIR/$FILE_NAME"
nl -v$((ID+MAX_ANS_PER_Q)) -i$((MAX_ANS_PER_Q)) -s "|"  "$TEMP_FILE_DIR/$FILE_NAME" > "$TEMP_FILE_DIR"/"$FILE_NAME"_temp.csv
mv "$TEMP_FILE_DIR"/"$FILE_NAME"_temp.csv "$TEMP_FILE_DIR/$FILE_NAME"

# create temp table for prep_survey by copying schema
TBL_TEMP_PS="prep_temp_"$TBL_PS"_"$ID
TBL_TEMP_SURVEY="prep_temp_"$(basename $FILE_NAME .csv)
TBL_TEMP_SURVEY_HG=$TBL_TEMP_SURVEY"_"HG

# these are temp tables for each survey
# remove all if they exist to start fresh
rmTmpTable $TBL_TEMP_PS
rmTmpTable $TBL_TEMP_SURVEY
rmTmpTable $TBL_TEMP_SURVEY_HG

# create temp prep [survey] tables for seeding the data
bq mk -t "$BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_PS" $TBL_PS_SCHEMA

echo "Loading $TEMP_FILE_DIR/$FILE_NAME to $BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_SURVEY"
bq load --skip_leading_rows=1 --allow_jagged_rows=false --allow_quoted_newlines=true \
        --field_delimiter='|' --source_format=CSV $BQ_PROJECT:$BQ_DATASET.$TBL_TEMP_SURVEY \
        $TEMP_FILE_DIR/$FILE_NAME $TBL_PS_STAGED_SCHEMA

##============ Create Holy Grail table form observation table ==================
# Holy Grail table Step 1 of 3: Add basic data from observation, concept, concept_synonym
echo "Holy Grail 1 of 3: Create basic $TBL_TEMP_SURVEY_HG from questions in $TBL_TEMP_SURVEY"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
   "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` as
    SELECT distinct o.observation_source_value as question_concept_code
      , CASE WHEN cs.concept_synonym_name is null
          THEN cq1.concept_name
          ELSE REPLACE(cs.concept_synonym_name, '|', ',') END AS question_name
      , o.observation_source_concept_id as question_concept_id
      , CASE WHEN o.value_as_concept_id is null THEN 'null' -- null string is before PMI_Skip when ordered
      ELSE ca.concept_code END as answer_concept_code
      , CASE WHEN NOT REGEXP_CONTAINS(ca.concept_name,r':') THEN ca.concept_name
            ELSE REGEXP_EXTRACT(ca.concept_name,r': (.*)',1,1) END as answer_name
      , o.value_source_concept_id as answer_concept_id_value
    FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` o
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c
        on (o.value_as_concept_id = c.concept_id
            and c.concept_id > 0 and c.concept_class_id = 'Answer')
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` ca
        on (o.value_source_concept_id = ca.concept_id
           and ca.concept_id > 0 and ca.concept_class_id = 'Answer')
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` cq1
        on (o.observation_source_concept_id = cq1.concept_id
            and cq1.concept_class_id = 'Question')
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs
        on (cq1.concept_id = cs.concept_id
            and lower(cs.concept_synonym_name) not like '%this condition?'
            and NOT STARTS_WITH(cs.concept_synonym_name, cq1.concept_code))
    WHERE (o.value_as_concept_id > 1 or o.value_as_concept_id is null)
        AND o.observation_source_concept_id in
        (
          SELECT concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.concept\` cq
          WHERE cq.concept_class_id = 'Question'
          AND lower(cq.concept_code) in (SELECT distinct lower(concept_code)
               FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\`)
          )"

# Holy Grail table Step 2 of 3: Add question details
echo "Holy Grail 2 of 3: Adding Question details to $TBL_TEMP_SURVEY_HG"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` as
    SELECT t.question_order as question_order
         , t.survey_name as survey_name
         , hg.question_concept_code as question_concept_code
         , hg.question_name as question_name
         , hg.question_concept_id as question_concept_id
         , hg.answer_concept_code as answer_concept_code
         , hg.answer_name as answer_name
         , hg.answer_concept_id_value as answer_concept_id_value
    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` hg
    LEFT JOIN (SELECT DISTINCT (min(line_no) over
                 (partition by concept_code order by line_no)) as question_order
                 , concept_code as q_code, survey_name
               FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\`
               WHERE concept_code is not null
               GROUP BY concept_code, survey_name, line_no
               ORDER BY question_order) t
    ON (lower(hg.question_concept_code) = lower(t.q_code))"

# Holy Grail table Step 3 of 3: Add answer details including answer_order
echo "Holy Grail 2 of 3: Adding Answer details to $TBL_TEMP_SURVEY_HG"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
"create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` as
    SELECT hg.question_order as question_order
          , hg.question_concept_code as question_concept_code
          , hg.survey_name as survey_name
          , hg.question_name as question_name
          , hg.question_concept_id as question_concept_id
          , hg.answer_concept_code as answer_concept_code
          , hg.answer_name as answer_name
          , hg.answer_concept_id_value as answer_concept_id_value
          , CASE WHEN answer_index is null THEN $((MAX_ANS_PER_Q-10)) ELSE answer_index + 1 END as answer_order
    FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` hg
    LEFT JOIN (\`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` t
        CROSS JOIN UNNEST(split(lower(t.answers),' ')) as answer_code with offset as answer_index)
        ON (hg.question_concept_code = lower(t.concept_code) and hg.answer_concept_code = answer_code)"

echo "Done creating Holy Grail table"

exit

echo "Updating value for topic column, unnest answers, add question_concept_id to $TBL_TEMP_SURVEY"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
   "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` as
     select CASE WHEN answer is null
                 THEN 1 + last_value(line_no) over(order by line_no)
                 ELSE line_no + answer_index + 1 END as line_no
          , c.concept_id as question_concept_id
          , s.concept_code
          , survey_name
          , last_value(topic ignore nulls) over (order by line_no) topic
          , answer as answer
          , answer_index + 1 as answer_order
     from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` s
     left join unnest(split(s.answers, ' ')) as answer with offset as answer_index
     left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on
          (lower(c.concept_code) = lower(s.concept_code) and c.concept_class_id = 'Question'
            and c.concept_id > 0)"


echo "exiting after creating the holy grail table...."
exit

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
        , name
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
    , pt.topic as name
    , 1 as is_group
    , 0 as is_selectable
    , 0 as has_attribute
    , 1 as has_hierarchy
    , pt.survey_name as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` pt
where pt.topic is not null"


echo "Updating value for topic column, unnest answers, add question_concept_id to $TBL_TEMP_SURVEY"
bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
   "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` as
     select CASE WHEN answer is null
                 THEN 1 + last_value(line_no) over(order by line_no)
                 ELSE line_no + answer_index + 1 END as line_no
          , c.concept_id as question_concept_id
          , s.concept_code
          , survey_name
          , last_value(topic ignore nulls) over (order by line_no) topic
          , CASE WHEN answer is null
                 THEN 'Select a value'
                 ELSE answer END as answer
          , CASE WHEN answer_index is null
                      THEN 1
                      ELSE answer_index + 1 END as answer_order
     from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` s
     left join unnest(split(s.answers, ' ')) as answer with offset as answer_index
     left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on
          (lower(c.concept_code) = lower(s.concept_code) and c.concept_class_id = 'Question'
            and c.concept_id > 0)"

  # drop all rows where there is no matching answer_concept_code in HG
  echo "Dropping rows where answer (for question) $TBL_TEMP_SURVEY is not in $TBL_TEMP_SURVEY_HG (answer_concept_code)"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
    "delete from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\`
        where answer not in (select distinct answer_concept_code from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\`)"

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
		, 'QUESTION' as subtype
		, hg.question_concept_id as concept_id
		, hg.question_concept_code as code
		, hg.question_name as name
		, 1 as is_group
		, 1 as is_selectable
		, 0 as has_attribute
		, 1 as has_hierarchy
		, pt2.survey as survey
from \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY_HG\` hg
left join ( select min(line_no)-min(answer_order) as line_no, question_concept_id, topic from
    \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` group by question_concept_id, topic) pt
    on  hg.question_concept_id = pt.question_concept_id
left join \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\` pt2 on pt.topic = pt2.name"

# exit
#
# echo "Adding Answers to $TBL_TEMP_PS"
# bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
# "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\`
#     (
#           id
#         , parent_id
#         , domain_id
#         , is_standard
#         , type
#         , subtype
#         , concept_id
#         , code
#         , name
#         , value
#         , is_group
#         , is_selectable
#         , has_attribute
#         , has_hierarchy
#         , survey
#     )
# SELECT DISTINCT
#       id as id
#     , ps.id as parent_id
#     , 'SURVEY' as domain_id
# 		, 0 as is_standard
# 		, 'PPI' as type
# 		, upper(c.concept_class_id) as subtype
# 		, ps.concept_id as concept_id
# 		, c.concept_code as concept_code
# 		, CASE WHEN REGEXP_CONTAINS(c.concept_name,':') THEN REGEXP_EXTRACT(c.concept_name,': (.*)',1,1) ELSE c.concept_name END as name
# 		, concat('',c.concept_id)  as value
# 		, 0 as is_group
# 		, 1 as is_selectable
# 		, 0 as has_attribute
# 		, 0 as has_hierarchy
# 		, pt.survey_name as survey
# FROM \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_PS\` ps
# join \`$BQ_PROJECT.$BQ_DATASET.$TBL_TEMP_SURVEY\` pt
#       on ( lower(ps.code) = lower(pt.concept_code) and ps.subtype = 'QUESTION' ) \
# join \`$BQ_PROJECT.$BQ_DATASET.concept\` c  on lower(pt.answer) = lower(c.concept_code)
# WHERE c.concept_class_id = 'Answer'"

wait

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

wait

cpToMainAndDeleteTmp "$TBL_TEMP_PS"
wait
# rmTmpTable "$TBL_TEMP_SURVEY"
echo "done... $FILE_NAME"
