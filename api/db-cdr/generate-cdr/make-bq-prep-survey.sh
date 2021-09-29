#!/bin/bash

# This generates big query denormalized tables for search, review and datasets.

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset
export FILE_NAME=$3          # Filename to process
export ID=$4                 # Starting id position
export REMOVE_PREP_SURVEY=$5 # Should remove prep_survey

BUCKET="all-of-us-workbench-private-cloudsql"
SCHEMA_PATH="generate-cdr/bq-schemas"
TEMP_FILE_DIR="csv"
DATASET_DIR="$BQ_DATASET/cdr_csv_files"
TOPIC_PARENT_ID=0
QUESTION_PARENT_ID=0
ANSWER_PARENT_ID=0
OUTPUT_FILE_NAME=$(echo "$FILE_NAME" | cut -d'_' -f 1 | xargs -I {} bash -c 'echo {}.csv')

function simple_select() {
  # run this query to initializing our .bigqueryrc configuration file
  # otherwise this will corrupt the output of the first call to find_info()
  query="select count(*) from \`$BQ_PROJECT.$BQ_DATASET.concept\`"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql --format=csv "$query"
}

function find_info() {
  local concept_code=$1
  local survey_name=$2
  local order_by=$3
  query="select domain_id, is_standard, type, subtype, concept_id, code, name, value, is_group, is_selectable, has_attribute, has_hierarchy, survey
  from (
  select 1 as id,
  'SURVEY' as domain_id,
  0 as is_standard,
  'PPI' as type,
  'SURVEY' as subtype,
  concept_id,
  concept_code as code,
  concept_name as name,
  null as value,
  1 as is_group,
  1 as is_selectable,
  0 as has_attribute,
  1 as has_hierarchy,
  '$survey_name' as survey
  from \`$BQ_PROJECT.$BQ_DATASET.concept\`
  where lower(concept_code) = lower('$concept_code')
  and concept_class_id in ('Module')
  union distinct
  select 1 as id,
  'SURVEY' as domain_id,
  0 as is_standard,
  'PPI' as type,
  'QUESTION' as subtype,
  c.concept_id,
  concept_code as code,
  CASE WHEN cs.concept_synonym_name is null THEN c.concept_name ELSE REPLACE(cs.concept_synonym_name, '|', ',') END AS name,
  null as value,
  1 as is_group,
  1 as is_selectable,
  0 as has_attribute,
  1 as has_hierarchy,
  '$survey_name' as survey
  from \`$BQ_PROJECT.$BQ_DATASET.concept\` c
  join \`$BQ_PROJECT.$BQ_DATASET.observation\` o on o.observation_source_concept_id = c.concept_id
  left join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs on (c.concept_id = cs.concept_id and lower(cs.concept_synonym_name) not like '%this condition?' and NOT STARTS_WITH(cs.concept_synonym_name, c.concept_code))
  where lower(concept_code) = lower('$concept_code')
  and concept_class_id in ('Question')
  group by id, domain_id, is_standard, type, subtype, concept_id, concept_code, concept_synonym_name, concept_name, value, is_group, is_selectable, has_attribute, has_hierarchy, survey
  union distinct
  select 2 as id,
  'SURVEY' as domain_id,
  0 as is_standard,
  'PPI' as type,
  'ANSWER' as subtype,
  c.concept_id,
  '' as code,
  'Select a value' as name,
  null as value,
  0 as is_group,
  1 as is_selectable,
  1 as has_attribute,
  1 as has_hierarchy,
  '$survey_name' as survey
  from \`$BQ_PROJECT.$BQ_DATASET.concept\` c
  join \`$BQ_PROJECT.$BQ_DATASET.observation\` o on o.observation_source_concept_id = c.concept_id
  where lower(observation_source_value) = lower('$concept_code')
  and value_source_concept_id is null
  group by id, domain_id, is_standard, type, subtype, concept_id, code, name, value, is_group, is_selectable, has_attribute, has_hierarchy, survey
  union distinct
  select id,
  'SURVEY' as domain_id,
  0 as is_standard,
  'PPI' as type,
  'ANSWER' as subtype,
  concept_id,
  concept_code as code,
  CASE WHEN STRPOS(concept_name, ':') > 1 THEN SUBSTR(concept_name, (STRPOS(concept_name, ':') + 2), (LENGTH(concept_name) - STRPOS(concept_name, ':'))) ELSE concept_name END as name,
  value_source_concept_id as value,
  0 as is_group,
  1 as is_selectable,
  0 as has_attribute,
  1 as has_hierarchy,
  '$survey_name' as survey
  from (
  select ROW_NUMBER() OVER (ORDER BY ($order_by)) + (SELECT 2) AS id,
  concept_code,
  observation_source_concept_id as concept_id,
  concept_name,
  value_source_concept_id,
  value_source_value
  from \`$BQ_PROJECT.$BQ_DATASET.observation\` o
  join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on c.concept_id = o.value_source_concept_id
  where lower(observation_source_value) = lower('$concept_code')
  and value_source_concept_id != 0
  and value_source_concept_id is not null
  group by concept_code, observation_source_concept_id, concept_name, value_source_concept_id, value_source_value
  order by ($order_by))
  order by id) order by id"
  echo $(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql --format=csv "$query" | sed "1 d" | tr '\n' '|')
}

function increment_ids() {
  increment_topic_parent_id "$ID"
  increment_question_parent_id "$ID"
  increment_id
}

function increment_id() {
  ID=$((ID + 1))
}

function increment_topic_parent_id() {
  TOPIC_PARENT_ID=$(($1))
}

function increment_question_parent_id() {
  QUESTION_PARENT_ID=$(($1))
}

function increment_answer_parent_id() {
  ANSWER_PARENT_ID=$(($1))
}

rm -rf "$TEMP_FILE_DIR"
mkdir "$TEMP_FILE_DIR"

gsutil -m cp gs://"$BUCKET/$DATASET_DIR/$FILE_NAME" "$TEMP_FILE_DIR"

# run this query to initializing our .bigqueryrc configuration file
# otherwise this will corrupt the output of the first call to find_info()
simple_select

while IFS=$'|' read -r concept_code survey_name topic answers;
do
  # Build custom order by clause
  if [[ -z "$answers" ]]
  then
    order_by="CASE WHEN lower(value_source_value)=lower('PMI_Skip') THEN 1 ELSE 2 END"
  else
    order_by="CASE "
    IFS=' ' read -r -a array <<< "$answers"
    for i in "${!array[@]}"
    do
      order_by+="WHEN lower(value_source_value)=lower('${array[i]}') THEN $((i + 1)) "
    done
    last_index=$((i + 2))
    order_by+="ELSE $last_index END"
  fi

  result=$(find_info "$concept_code" "$survey_name" "$order_by")
  IFS=$'|' read -a result_array <<< "$result"
  if [[ ! -z "$topic" && "$topic" != "topic" && "${#result_array[@]}" -ge 2 ]]
  then
    formatted_topic=$(echo "$topic" | sed "s/'/\'/")
    echo "writing topic: $formatted_topic"
    echo "$ID,$TOPIC_PARENT_ID,SURVEY,0,PPI,TOPIC,,,\"${formatted_topic}\",,1,0,0,1,$survey_name" >> "$TEMP_FILE_DIR/$OUTPUT_FILE_NAME"
    increment_question_parent_id "$ID"
    increment_id
  fi
  for res in "${result_array[@]}"
  do
    type=$(echo "${res}" | cut -d "," -f 4)
    if [[ "$type" == "SURVEY" ]]
    then
      echo "writing survey: $survey_name"
      echo "$ID,0,${res}" >> "$TEMP_FILE_DIR/$OUTPUT_FILE_NAME"
      increment_ids
    elif [[ "$type" == "QUESTION" ]]
    then
      echo "writing question for concept_code: $concept_code"
      echo "$ID,$QUESTION_PARENT_ID,${res}" >> "$TEMP_FILE_DIR/$OUTPUT_FILE_NAME"
      increment_answer_parent_id "$ID"
      increment_id
    elif [[ "$type" == "ANSWER" ]]
    then
      echo "writing answers for concept_code: $concept_code"
      echo "$ID,$ANSWER_PARENT_ID,${res}" >> "$TEMP_FILE_DIR/$OUTPUT_FILE_NAME"
      increment_id
    fi
  done

done < csv/"$FILE_NAME"

gsutil cp "$TEMP_FILE_DIR/$OUTPUT_FILE_NAME" "gs://$BUCKET/$BQ_DATASET/cdr_csv_files/$OUTPUT_FILE_NAME"

echo "Loading data into prep_survey"
if [[ "$REMOVE_PREP_SURVEY" = true ]]
then
  bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"
fi
bq load --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.prep_survey" \
"gs://$BUCKET/$BQ_DATASET/cdr_csv_files/$OUTPUT_FILE_NAME" "$SCHEMA_PATH/prep_survey.json"

rm -rf $TEMP_FILE_DIR