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
create_tables=(cb_survey_attribute cb_survey_version cb_criteria cb_criteria_attribute cb_criteria_relationship cb_criteria_ancestor ds_linking ds_data_dictionary domain_info survey_module cb_person cb_data_filter cb_criteria_menu cb_menu)

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

# Populate cb_menu
#######################
#  cb_menu   #
#######################
echo "Inserting cb_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_menu\`
(id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order)
SELECT id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_menu\`"

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

# Populate domain_info
###############
# domain_info #
###############
echo "Inserting domain_info"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\`
(concept_id,domain,domain_id,domain_enum,name,description,all_concept_count,standard_concept_count,participant_count)
VALUES
(19,0,'Condition','CONDITION','Conditions','Conditions are records of a Person suggesting the presence of a disease or medical condition stated as a diagnosis, a sign or a symptom, which is either observed by a Provider or reported by the patient.',0,0,0),
(13,3,'Drug','DRUG','Drug Exposures','Drugs biochemical substance formulated in such a way that when administered to a Person it will exert a certain physiological or biochemical effect. The drug exposure domain concepts capture records about the utilization of a Drug when ingested or otherwise introduced into the body.',0,0,0),
(21,4,'Measurement','MEASUREMENT','Labs and Measurements','Labs and Measurements',0,0,0),
(10,6,'Procedure','PROCEDURE','Procedures','Procedure',0,0,0),
(27,5,'Observation','OBSERVATION','Observations','Observation',0,0,0),
(0,19,'Physical Measurements','PHYSICAL_MEASUREMENT_CSS','Physical Measurements','Participants have the option to provide a standard set of physical measurements as part of the enrollment process',0,0,0)"

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
SELECT ROW_NUMBER() OVER() ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN
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
# domain info updates                    #
##########################################

# Set all_concept_count and standard_concept_count on domain_info
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.all_concept_count = c.all_concept_count
from (select domain_id, sum(all_concept_count) as all_concept_count
      from (select c.domain_id as domain_id, c.is_standard, COUNT(DISTINCT c.concept_id) as all_concept_count
              from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c
              join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d2
              on d2.domain_enum = c.domain_id and c.is_selectable = 1 and d2.domain_enum != 'PHYSICAL_MEASUREMENT_CSS'
              group by c.domain_id, c.is_standard ) a
      group by domain_id) c
where d.domain_enum = c.domain_id"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.standard_concept_count = c.standard_concept_count
from (select c.domain_id as domain_id, COUNT(DISTINCT c.concept_id) as standard_concept_count
from \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\` c
join \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d2
on d2.domain_enum = c.domain_id and c.is_standard = 1 and c.is_selectable = 1  and d2.domain_enum != 'PHYSICAL_MEASUREMENT_CSS'
group by c.domain_id) c
where d.domain_enum = c.domain_id"

# Set all_concept_count on domain_info for Physical Measurements
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.all_concept_count = c.all_concept_count, d.standard_concept_count = c.standard_concept_count from
(SELECT count(distinct concept_id) as all_concept_count, 0 as standard_concept_count
FROM \`$OUTPUT_PROJECT.$OUTPUT_DATASET.cb_criteria\`
WHERE type = 'PPI'
AND domain_id = 'PHYSICAL_MEASUREMENT_CSS') c
where d.domain = 19"

# Set participant counts for Condition domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(select count(distinct person_id) as count
from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` co) as r
where d.concept_id = 19"

# Set participant counts for Measurement domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(select count(distinct person_id) as count
from \`$BQ_PROJECT.$BQ_DATASET.measurement\` m) as r
where d.concept_id = 21"

# Set participant counts for Procedure domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(select count(distinct person_id) as count
from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` po) as r
where d.concept_id = 10"

# Set participant counts for Drug domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(select count(distinct person_id) as count
from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` de) as r
where d.concept_id = 13"

# Set participant counts for Observation domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(select count(distinct person_id) as count
from \`$BQ_PROJECT.$BQ_DATASET.observation\` o) as r
where d.concept_id = 27"

# Set participant counts for Physical Measurements domain
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$OUTPUT_PROJECT.$OUTPUT_DATASET.domain_info\` d
set d.participant_count = r.count from
(SELECT COUNT(DISTINCT person_id) as count
FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\`
WHERE measurement_source_concept_id IN (
SELECT concept_id
FROM \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
WHERE type = 'PPI'
AND domain_id = 'PHYSICAL_MEASUREMENT_CSS')) as r
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
