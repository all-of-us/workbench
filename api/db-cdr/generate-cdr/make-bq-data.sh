#!/bin/bash

# This generates big query count databases cdr that get put in cloudsql for workbench

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset
export OUTPUT_PROJECT=$3 # output project
export OUTPUT_DATASET=$4 # output dataset

# Check that bq_dataset exists and exit if not
datasets=$(bq --project_id=$BQ_PROJECT ls --max_results=1000)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
re=\\b$BQ_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Make dataset for cdr cloudsql tables
datasets=$(bq --project_id=$OUTPUT_PROJECT ls --max_results=1000)
re=\\b$OUTPUT_DATASET\\b
if [[ $datasets =~ $re ]]; then
  echo "$OUTPUT_DATASET exists"
else
  echo "Creating $OUTPUT_DATASET"
  bq --project_id=$OUTPUT_PROJECT mk $OUTPUT_DATASET
fi

#Check if tables to be copied over exists in bq project dataset
tables=$(bq --project_id=$BQ_PROJECT --dataset=$BQ_DATASET ls --max_results=1000)
cb_cri_table_check=\\bcb_criteria\\b
cb_cri_attr_table_check=\\bcb_criteria_attribute\\b
cb_cri_rel_table_check=\\bcb_criteria_relationship\\b
cb_cri_anc_table_check=\\bcb_criteria_ancestor\\b

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(cb_survey_attribute cb_survey_version cb_criteria cb_criteria_attribute cb_criteria_relationship cb_criteria_ancestor ds_linking ds_data_dictionary domain_card survey_module cb_person cb_data_filter cb_criteria_menu)

for t in "${create_tables[@]}"
do
    bq --project_id=$OUTPUT_PROJECT rm -f $OUTPUT_DATASET.$t
    bq --quiet --project_id=$OUTPUT_PROJECT mk --schema=$schema_path/$t.json $OUTPUT_DATASET.$t
done

# Populate ds_data_dictionary
#######################
# ds_data_dictionary #
#######################
echo "Inserting ds_data_dictionary"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.ds_data_dictionary\`
(id,field_name,relevant_omop_table,description,field_type,omop_cdm_standard_or_custom_field,data_provenance,source_ppi_module,domain)
SELECT id,field_name,relevant_omop_table,description,field_type,omop_cdm_standard_or_custom_field,data_provenance,source_ppi_module,domain
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_data_dictionary\`"

# Populate cb_criteria_menu
#######################
#  cb_criteria_menu   #
#######################
echo "Inserting cb_criteria_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_menu\`
(id,parent_id,category,domain_id,type,name,is_group,sort_order)
SELECT id,parent_id,category,domain_id,type,name,is_group,sort_order
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`"

# Populate cb_survey_attribute
#######################
# cb_survey_attribute #
#######################
echo "Inserting cb_survey_attribute"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_survey_attribute\`
(id,question_concept_id,answer_concept_id,survey_version_concept_id,item_count)
SELECT id,question_concept_id,answer_concept_id,survey_version_concept_id,item_count
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_attribute\`"

# Populate cb_survey_version
#####################
# cb_survey_version #
#####################
echo "Inserting cb_survey_version"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_survey_version\`
(survey_version_concept_id,survey_concept_id,display_name,display_order)
SELECT survey_version_concept_id,survey_concept_id,display_name,display_order
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_survey_version\`"

# Populate domain_card
###############
# domain_card #
###############
echo "Inserting domain_card"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\`
(id,category,domain,name,description,concept_count,participant_count,is_standard,sort_order)
VALUES
(1,'Domains',0,'Conditions','Conditions',0,0,1,1),
(2,'Domains',3,'Drug Exposures','Drug Exposures',0,0,1,2),
(3,'Domains',4,'Labs and Measurements','Labs and Measurements',0,0,1,3),
(4,'Domains',5,'Observations','Observations',0,0,1,4),
(5,'Domains',6,'Procedures','Procedures',0,0,1,5),
(6,'Program Physical Measurements',19,'Physical Measurements','Participants have the option to provide a standard set of physical measurements as part of the enrollment process(program physical measurements)',0,0,0,8)"

# Populate survey_module
#################
# survey_module #
#################
echo "Inserting survey_module"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.survey_module\`
(concept_id,name,description,question_count,participant_count,order_number)
VALUES
(1585855,'Lifestyle','Survey includes information on participant smoking, alcohol and recreational drug use.',0,0,3),
(1585710,'Overall Health','Survey provides information about how participants report levels of individual health.',0,0,2),
(1586134,'The Basics','Survey includes participant demographic information.',0,0,1),
(43529712,'Personal Medical History','This survey includes information about past medical history, including medical conditions and approximate age of diagnosis.',0,0,4),
(43528895,'Health Care Access & Utilization','Survey includes information about a participants access to and use of health care.',0,0,5),
(43528698,'Family History','Survey includes information about the medical history of a participants immediate biological family members.',0,0,6),
(1333342,'COVID-19 Participant Experience (COPE) Survey','COVID-19 Participant Experience (COPE) Survey.',0,0,7)"

# Populate cb_person table
echo "Inserting cb_person"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_person\`
(person_id, dob, age_at_consent, age_at_cdr, is_deceased)
SELECT person_id, dob, age_at_consent, age_at_cdr, is_deceased
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`"

# Populate cb_data_filter table
echo "Inserting cb_data_filter"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_data_filter\`
(data_filter_id, display_name, name)
VALUES
(1,'Only include participants with EHR data','has_ehr_data'),
(2,'Only include participants with Physical Measurements','has_physical_measurement_data')"

# Populate ds_linking table
echo "Inserting ds_linking"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.ds_linking\`
(ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
SELECT ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN
FROM \`$BQ_PROJECT.$BQ_DATASET.ds_linking\`"

# Populate some tables from cdr data
###############
# cb_criteria #
###############
if [[ $tables =~ $cb_cri_table_check ]]; then
    echo "Inserting cb_criteria"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\`
     (id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms, rollup_count, item_count, full_text, display_synonyms)
    SELECT id, parent_id, domain_id, type, subtype, is_standard, code, name, value, is_group, is_selectable, est_count, concept_id, has_attribute, has_hierarchy, has_ancestor_data, path, synonyms, rollup_count, item_count, full_text, display_synonyms
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`"
fi

#########################
# cb_criteria_attribute #
#########################
if [[ $tables =~ $cb_cri_attr_table_check ]]; then
    echo "Inserting cb_criteria_attribute"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_attribute\`
     (id, concept_id, value_as_concept_id, concept_name, type, est_count)
    SELECT id, concept_id, value_as_concept_id, concept_name, type, est_count
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_attribute\`"
fi

############################
# cb_criteria_relationship #
############################
if [[ $tables =~ $cb_cri_rel_table_check ]]; then
    echo "Inserting cb_criteria_relationship"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_relationship\`
     (concept_id_1, concept_id_2)
    SELECT concept_id_1, concept_id_2
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_relationship\`"
fi

############################
#   cb_criteria_ancestor   #
############################
if [[ $tables =~ $cb_cri_anc_table_check ]]; then
    echo "Inserting cb_criteria_ancestor"
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria_ancestor\`
     (ancestor_id, descendant_id)
    SELECT ancestor_id, descendant_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\`"
fi

##########################################
# domain card updates                    #
##########################################

# Set concept_count for standard Conditions
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct se.concept_id) as concept_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Condition'
      and c.is_standard = 1) c
where d.domain = 0
and d.is_standard = 1"

# Set participant_count for standard Conditions
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Condition'
      and c.is_standard = 1) c
where d.domain = 0
and d.is_standard = 1"

# Set concept_count for standard Drugs
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct se.concept_id) as concept_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      where concept_id in (select distinct ca.descendant_id
                           from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\` ca
                           where ca.ancestor_id in (select concept_id
                                                    from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\`
                                                    where domain_id = 'DRUG')
                          )
      and se.domain = 'Drug') c
where d.domain = 3
and d.is_standard = 1"

# Set participant_count for standard Drugs
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      where concept_id in (select distinct ca.descendant_id
                           from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_ancestor\` ca
                           where ca.ancestor_id in (select concept_id
                                                    from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\`
                                                    where domain_id = 'DRUG')
                          )
      and se.domain = 'Drug') c
where d.domain = 3
and d.is_standard = 1"

# Set concept_count for standard Measurements - exlcudes PM
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct se.concept_id) as concept_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Measurement'
      and c.domain_id not in ('PHYSICAL_MEASUREMENT_CSS', 'PHYSICAL_MEASUREMENT')) c
where d.domain = 4
and d.is_standard = 1"

# Set participant_count for standard Measurements - excludes PM
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Measurement'
      and c.domain_id not in ('PHYSICAL_MEASUREMENT_CSS', 'PHYSICAL_MEASUREMENT')) c
where d.domain = 4
and d.is_standard = 1"

# Set concept_count for standard Observations - exludes PM and Surveys
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct se.concept_id) as concept_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Observation'
      and c.domain_id not in ('PHYSICAL_MEASUREMENT', 'PHYSICAL_MEASUREMENT_CSS', 'SURVEY')) c
where d.domain = 5
and d.is_standard = 1"

# Set participant_count for standard Observations - exludes PM and Surveys
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Observation'
      and c.domain_id not in ('PHYSICAL_MEASUREMENT', 'PHYSICAL_MEASUREMENT_CSS', 'SURVEY')) c
where d.domain = 5
and d.is_standard = 1"

# Set concept_count for standard Procedures
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct se.concept_id) as concept_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Procedure'
      and c.is_standard = 1) c
where d.domain = 6
and d.is_standard = 1"

# Set participant_count for standard Procedures
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on se.concept_id = c.concept_id
      where se.domain = 'Procedure'
      and se.is_standard = 1) c
where d.domain = 6
and d.is_standard = 1"

# Set concept_count for source PM
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.concept_count = c.concept_count
from (select count(distinct concept_id) as concept_count
      from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c
      where domain_id = 'PHYSICAL_MEASUREMENT_CSS') c
where d.domain = 19"

# Set participant_count for source PM
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_card\` d
set d.participant_count = c.participant_count
from (select count(distinct se.person_id) as participant_count
      from \`$BQ_PROJECT.$BQ_DATASET.cb_search_all_events\` se
      join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c on c.concept_id = se.concept_id
      where c.domain_id = 'PHYSICAL_MEASUREMENT_CSS') c
where d.domain = 19"

##########################################
# survey count updates                   #
##########################################

# Set the participant count on the survey_module row
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` x
SET x.participant_count = y.est_count
FROM
    (
        SELECT concept_id, est_count
        FROM \`${BQ_PROJECT}.${BQ_DATASET}.cb_criteria\`
        WHERE domain_id = 'SURVEY'
            and parent_id = 0
    ) y
WHERE x.concept_id = y.concept_id"

# Set the question count on the survey_module row
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`${OUTPUT_PROJECT}.${OUTPUT_DATASET}.survey_module\` x
SET x.question_count = y.num_questions
FROM
    (
        SELECT ancestor_concept_id, count(*) as num_questions
        FROM \`${BQ_PROJECT}.${BQ_DATASET}.prep_concept_ancestor\`
        join \`${BQ_PROJECT}.${BQ_DATASET}.cb_criteria\` on concept_id = descendant_concept_id
        WHERE subtype = 'QUESTION'
        AND ancestor_concept_id in
            (
                SELECT concept_id
                FROM \`${BQ_PROJECT}.${BQ_DATASET}.cb_criteria\`
                WHERE domain_id = 'SURVEY'
                    and parent_id = 0
            )
        GROUP BY 1
    ) y
WHERE x.concept_id = y.ancestor_concept_id"
