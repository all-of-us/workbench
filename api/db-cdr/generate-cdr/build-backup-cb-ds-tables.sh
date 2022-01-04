#!/bin/bash

# This creates a backup of all cb_ and ds_ tables

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export OUTPUT_PROJECT=$3 # output project
export OUTPUT_DATASET=$4 # output dataset

BACKUP_DATASET=${OUTPUT_DATASET}_backup

# Make dataset for backup
datasets=$(bq --project_id="$OUTPUT_PROJECT" ls --max_results=1000)
if [[ $datasets =~ $BACKUP_DATASET ]]; then
  echo "$BACKUP_DATASET exists"
else
  echo "Creating $BACKUP_DATASET"
  bq --project_id="$OUTPUT_PROJECT" mk "$BACKUP_DATASET"
fi

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
backup_tables=(cb_criteria
cb_criteria_ancestor
cb_criteria_attribute
cb_criteria_menu
cb_criteria_relationship
cb_review_all_events
cb_review_survey
cb_search_all_events
cb_search_person
cb_survey_attribute
cb_survey_version
ds_activity_summary
ds_condition_occurrence
ds_data_dictionary
ds_drug_exposure
ds_heart_rate_minute_level
ds_heart_rate_summary
ds_linking
ds_measurement
ds_observation
ds_person
ds_procedure_occurrence
ds_steps_intraday
ds_survey
ds_visit_occurrence
ds_zip_code_socioeconomic
)

for t in "${backup_tables[@]}"
do
    bq --project_id="$OUTPUT_PROJECT" rm -f "$BACKUP_DATASET.$t"
    bq --quiet --project_id="$OUTPUT_PROJECT" mk --schema="$schema_path/$t.json" "$BACKUP_DATASET.$t"
done

###############
# cb_criteria #
###############
echo "Inserting cb_criteria"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_criteria\`
(id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms, rollup_count, item_count, full_text, display_synonyms)
SELECT id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms, rollup_count, item_count, full_text, display_synonyms
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`"

############################
#   cb_criteria_ancestor   #
############################
echo "Inserting cb_criteria_ancestor"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_criteria_ancestor\`
(ancestor_id, descendant_id)
SELECT ancestor_id, descendant_id
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`"

#########################
# cb_criteria_attribute #
#########################
echo "Inserting cb_criteria_attribute"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_criteria_attribute\`
(id, concept_id, value_as_concept_id, concept_name, type, est_count)
SELECT id, concept_id, value_as_concept_id, concept_name, type, est_count
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`"

#######################
#  cb_criteria_menu   #
#######################
echo "Inserting cb_criteria_menu"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_criteria_menu\`
(id,parent_id,category,domain_id,type,name,is_group,sort_order)
SELECT id,parent_id,category,domain_id,type,name,is_group,sort_order
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`"

############################
# cb_criteria_relationship #
############################
echo "Inserting cb_criteria_relationship"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_criteria_relationship\`
(concept_id_1, concept_id_2)
SELECT concept_id_1, concept_id_2
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`"

########################
# cb_review_all_events #
########################
echo "Inserting cb_review_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_review_all_events\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_review_all_events\`"

####################
# cb_review_survey #
####################
echo "Inserting cb_review_survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_review_survey\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_review_survey\`"

########################
# cb_search_all_events #
########################
echo "Inserting cb_search_all_events"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_search_all_events\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\`"

####################
# cb_search_person #
####################
echo "Inserting cb_search_person"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_search_person\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`"

#######################
# cb_survey_attribute #
#######################
echo "Inserting cb_survey_attribute"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_survey_attribute\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`"

#####################
# cb_survey_version #
#####################
echo "Inserting cb_survey_version"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.cb_survey_version\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`"

#######################
# ds_activity_summary #
#######################
echo "Inserting ds_activity_summary"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_activity_summary\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_activity_summary\`"

###########################
# ds_condition_occurrence #
###########################
echo "Inserting ds_condition_occurrence"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_condition_occurrence\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_condition_occurrence\`"

#######################
# ds_data_dictionary #
#######################
echo "Inserting ds_data_dictionary"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_data_dictionary\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_data_dictionary\`"

####################
# ds_drug_exposure #
####################
echo "Inserting ds_drug_exposure"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_drug_exposure\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_drug_exposure\`"

##############################
# ds_heart_rate_minute_level #
##############################
echo "Inserting ds_heart_rate_minute_level"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_heart_rate_minute_level\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_minute_level\`"

#########################
# ds_heart_rate_summary #
#########################
echo "Inserting ds_heart_rate_summary"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_heart_rate_summary\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_heart_rate_summary\`"

##############
# ds_linking #
##############
echo "Inserting ds_linking"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_linking\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`"

##################
# ds_measurement #
##################
echo "Inserting ds_measurement"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_measurement\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_measurement\`"

##################
# ds_observation #
##################
echo "Inserting ds_observation"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_observation\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_observation\`"

#############
# ds_person #
#############
echo "Inserting ds_person"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_person\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_person\`"

###########################
# ds_procedure_occurrence #
###########################
echo "Inserting ds_procedure_occurrence"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_procedure_occurrence\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_procedure_occurrence\`"

#####################
# ds_steps_intraday #
#####################
echo "Inserting ds_steps_intraday"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_steps_intraday\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_steps_intraday\`"

#############
# ds_survey #
#############
echo "Inserting ds_survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_survey\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_survey\`"

#######################
# ds_visit_occurrence #
#######################
echo "Inserting ds_visit_occurrence"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_visit_occurrence\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_visit_occurrence\`"

#############################
# ds_zip_code_socioeconomic #
#############################
echo "Inserting ds_zip_code_socioeconomic"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$BACKUP_DATASET.ds_zip_code_socioeconomic\`
SELECT *
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_zip_code_socioeconomic\`"