#!/bin/bash

BQ_PROJECT='all-of-us-ehr-dev'       # project
BQ_DATASET='BillDummyMult'        # dataset
BQ_ORIG_DATASET='Chenchal_R2021Q3R2_v2'

crit_table=cb_criteria_sorted
#orig_crit_table=cb_criteria_original
orig_crit_table=cb_criteria_sorted
bucket=bill-comp-bucket

function extractTable() {
  tableName=$1

  echo "Extracting $tableName to Bucket";
  bq extract --quiet --destination_format CSV \
     --project_id $BQ_PROJECT \
      $BQ_PROJECT:$BQ_DATASET.$tableName \
      gs://${bucket}/${tableName}_*.csv
}

function createAggregateTables() {
  echo "Creating arrays table for full_text and synonyms fields"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.text_arrays\` as(
    select (select split(full_text, '|') as text_array from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b where b.id=a.id) as text_array,
           (select split(synonyms, '|') as synonym_array from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` b where b.id=a.id) as synonym_array,
           a.id
             from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` a
  )"

  echo "Creating table for sorted full_text string"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.sorted_text\` as (
    select string_agg(x, '|' order by x) as full_text, t.id
      from \`$BQ_PROJECT.$BQ_DATASET.text_arrays\` t,
           unnest((select t2.text_array from \`$BQ_PROJECT.$BQ_DATASET.text_arrays\` t2 where t2.id = t.id)) as x
    group by t.id
  )"

  echo "Creating table for sorted synonyms string"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.sorted_synonyms\` as (
    select string_agg(x, '|' order by x) as synonyms, t.id
      from \`$BQ_PROJECT.$BQ_DATASET.text_arrays\` t,
           unnest((select t2.synonym_array from \`$BQ_PROJECT.$BQ_DATASET.text_arrays\` t2 where t2.id = t.id)) as x
    group by t.id
  )"

  echo "Creating cb_criteria_sorted"
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_sorted\` as (
    SELECT c.id, parent_id, domain_id, is_standard, type, subtype, concept_id,
           code, name, value, est_count, is_group, is_selectable, has_attribute,
           has_hierarchy, has_ancestor_data, path, s.synonyms, rollup_count, item_count,
           t.full_text, display_synonyms
      FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\` c,
           \`$BQ_PROJECT.$BQ_DATASET.sorted_synonyms\` s,
           \`$BQ_PROJECT.$BQ_DATASET.sorted_text\` t
     where c.id = s.id
       and s.id = t.id)"
}

function createCompTables() {
  domainId=$1
  type=$2
  formatted_type=${type// /_}

  tableName="COMP_${domainId}_${formatted_type}";

  echo "Creating table $tableName";
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$tableName\` as
      select domain_id, is_standard, type, subtype, concept_id, code, name, value,
             est_count, is_group, is_selectable, has_attribute, has_hierarchy,
             has_ancestor_data, synonyms, rollup_count, item_count, full_text,
             display_synonyms from \`$BQ_PROJECT.$BQ_DATASET.$crit_table\`
      where domain_id='$domainId' and type='$type'
      order by concept_id"

  extractTable $tableName

  tableName="${tableName}_ORIG";

  echo "Creating table $tableName";
  bq --quiet --project_id=$BQ_PROJECT query --batch --nouse_legacy_sql \
  "create or replace table \`$BQ_PROJECT.$BQ_DATASET.$tableName\` as
      select domain_id, is_standard, type, subtype, concept_id, code, name, value,
             est_count, is_group, is_selectable, has_attribute, has_hierarchy,
             has_ancestor_data, synonyms, rollup_count, item_count, full_text,
             display_synonyms from \`$BQ_PROJECT.$BQ_ORIG_DATASET.$orig_crit_table\`
      where domain_id='$domainId' and type='$type'
      order by concept_id"

  extractTable $tableName
}

createCompTables "CONDITION" "CIEL";
createCompTables "CONDITION" "ICD10CM";
createCompTables "CONDITION" "ICD9CM";
createCompTables "CONDITION" "SNOMED";
createCompTables "CONDITION" "Nebraska Lexicon";
createCompTables "CONDITION" "OMOP Extension";
createCompTables "DRUG" "ATC";
createCompTables "DRUG" "BRAND";
createCompTables "DRUG" "CPT4";
createCompTables "DRUG" "CVX";
createCompTables "DRUG" "HCPCS";
createCompTables "DRUG" "RXNORM";
createCompTables "DRUG" "RxNorm Extension";
createCompTables "FITBIT" "FITBIT";
createCompTables "MEASUREMENT" "CPT4";
createCompTables "MEASUREMENT" "Cancer Modifier";
createCompTables "MEASUREMENT" "HCPCS";
createCompTables "MEASUREMENT" "LOINC";
createCompTables "MEASUREMENT" "OMOP Extension";
createCompTables "MEASUREMENT" "SNOMED";
createCompTables "OBSERVATION" "CPT4";
createCompTables "OBSERVATION" "DRG";
createCompTables "OBSERVATION" "HCPCS";
createCompTables "OBSERVATION" "LOINC";
createCompTables "OBSERVATION" "NAACCR";
createCompTables "OBSERVATION" "OMOP Extension";
createCompTables "OBSERVATION" "PPI";
createCompTables "OBSERVATION" "SNOMED";
createCompTables "PERSON" "AGE";
createCompTables "PERSON" "DECEASED";
createCompTables "PERSON" "ETHNICITY";
createCompTables "PERSON" "GENDER";
createCompTables "PERSON" "RACE";
createCompTables "PERSON" "SEX";
createCompTables "PHYSICAL_MEASUREMENT" "PPI";
createCompTables "PHYSICAL_MEASUREMENT_CSS" "PPI";
createCompTables "PROCEDURE" "CIEL";
createCompTables "PROCEDURE" "CPT4";
createCompTables "PROCEDURE" "HCPCS";
createCompTables "PROCEDURE" "ICD10PCS";
createCompTables "PROCEDURE" "ICD9Proc";
createCompTables "PROCEDURE" "SNOMED";
createCompTables "PROCEDURE" "UCUM";
createCompTables "SURVEY" "PPI";
createCompTables "VISIT" "VISIT";
createCompTables "WHOLE_GENOME_VARIANT" "WHOLE_GENOME_VARIANT";
