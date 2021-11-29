#!/bin/bash

# This will load local data from dataset into bucket

set -e

export BQ_PROJECT=$1         # CDR project
export BQ_DATASET=$2         # CDR dataset

BUCKET="all-of-us-cb-test-csv"

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
TABLES_WITH_SCHEMA=("cb_criteria_ancestor_temp" "cb_criteria_attribute_temp" "cb_criteria_relationship_temp" "cb_criteria_menu_temp" "cb_survey_attribute_temp" \
                    "cb_survey_version_temp" "ds_data_dictionary_temp" "ds_linking_temp")

#remove temp once confirmed that its correct
TABLES_WITHOUT_SCHEMA=("cb_criteria_drug_temp" "cb_criteria_measurement_temp" "cb_criteria_observation_temp" \
                "cb_criteria_person_temp" "cb_criteria_physical_measurement_temp" "cb_criteria_procedure_standard_temp" "cb_criteria_procedure_source_temp" \
                 "cb_criteria_survey_temp" "cb_criteria_visit_temp" "cb_criteria_fitbit_temp" "cb_criteria_condition_source_temp" "cb_criteria_condition_standard_temp" \
                 "cb_criteria_wgv_temp")

for t in "${TABLES_WITHOUT_SCHEMA[@]}"
do
    bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project_id=$BQ_PROJECT mk --schema="$schema_path/cb_criteria.json" $BQ_DATASET.$t
done

for t in "${TABLES_WITH_SCHEMA[@]}"
do
    temp=${t%_temp}
    bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/$temp.json $BQ_DATASET.$t
done



echo "Insert cb_criteria_ancestor_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`"

echo "Insert cb_criteria_attribute_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`"

echo "Insert cb_criteria_drug_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_drug_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id = 'DRUG'
ORDER by id"

echo "Insert cb_criteria_measurement_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_measurement_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id = 'MEASUREMENT'
ORDER by id"

echo "Insert cb_criteria_observation_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_observation_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id = 'OBSERVATION'
ORDER by id"

echo "Insert cb_criteria_person_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_person_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id = 'PERSON'
ORDER by id"

echo "Insert cb_criteria_physical_measurement_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_physical_measurement_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE domain_id in ('PHYSICAL_MEASUREMENT', 'PHYSICAL_MEASUREMENT_CSS')
ORDER by id"

echo "Insert cb_criteria_procedure_standard_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_procedure_standard_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'PROCEDURE'
and is_standard = 1
order by id
limit 20000"

echo "Insert cb_criteria_procedure_source_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_procedure_source_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'PROCEDURE'
and is_standard = 0
order by id
limit 20000"

echo "Insert cb_criteria_survey_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_survey_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'SURVEY'
order by id"

echo "Insert cb_criteria_visit_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_visit_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'VISIT'
order by id"

echo "Insert cb_criteria_fitbit_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_fitbit_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'FITBIT'
order by id"

echo "Insert cb_criteria_condition_source_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_condition_source_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'CONDITION'
and is_standard = 0
order by id
limit 20000"

echo "Insert cb_criteria_condition_standard_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_condition_standard_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'CONDITION'
and is_standard = 1
order by id
limit 20000"

echo "Insert cb_criteria_wgv_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_wgv_temp\`
(id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms)
   SELECT id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,has_ancestor_data,path,synonyms,rollup_count,item_count,full_text,display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'WHOLE_GENOME_VARIANT'
order by id"

echo "Insert cb_criteria_relationship_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`"

echo "Insert cb_criteria_menu_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`"

echo "Insert cb_survey_attribute_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`"

echo "Insert cb_survey_version_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`"

echo "Insert ds_data_dictionary_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_data_dictionary_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_data_dictionary\`"

echo "Insert ds_linking_temp"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking_temp\`
   SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`"




for i in "${TABLES_WITHOUT_SCHEMA[@]}"
do
    echo "Inserting into bucket"
    bq extract --project_id $BQ_PROJECT --destination_format=CSV --field_delimiter=',' $BQ_PROJECT:$BQ_DATASET.$i \
    "gs://$BUCKET/${i%_temp}.csv"
    echo "Deleting tables"
    bq rm -f -t $BQ_PROJECT:$BQ_DATASET.$i
done

for i in "${TABLES_WITH_SCHEMA[@]}"
do
    echo "Inserting into bucket"
    bq extract --project_id $BQ_PROJECT --destination_format=CSV --field_delimiter=',' $BQ_PROJECT:$BQ_DATASET.$i \
    "gs://$BUCKET/${i%_temp}.csv"
    echo "Deleting tables"
    bq rm -f -t $BQ_PROJECT:$BQ_DATASET.$i
done

#for i in "${TABLES_WITHOUT_SCHEMA[@]}"
#do
#    echo "Deleting tables"
#    bq rm -f -t $BQ_PROJECT:$BQ_DATASET.$i
#done
#
#for i in "${TABLES_WITH_SCHEMA[@]}"
#do
#    echo "Deleting tables"
#    bq rm -f -t $BQ_PROJECT:$BQ_DATASET.$i
#done