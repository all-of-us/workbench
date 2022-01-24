#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

echo "Getting fitbit count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_fitbit = 1"
fitbitCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting wgv count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_whole_genome_variant = 1"
wgvCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting array_data count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_array_data = 1"
arrayCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

###############################
# CREATE cb_criteria_menu TABLE
###############################
echo "Insert cb_criteria_menu"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,sort_order)
VALUES
(1,0,'Program Data','PERSON','','Demographics',1,1),
(2,0,'Program Data','SURVEY','PPI','Surveys',0,2),
(3,0,'Program Data','PHYSICAL_MEASUREMENT','PPI','Physical Measurements',0,3)"

if [[ $fitbitCount > 0 ]];
then
  echo "Insert fitbit into cb_criteria_menu"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES
  (4,0,'Program Data','FITBIT','FITBIT','Fitbit',0,4)"
fi

if [[ $wgvCount > 0 ]];
then
  echo "Insert wgv into cb_criteria_menu"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES
  (5,0,'Program Data','WHOLE_GENOME_VARIANT','','Whole Genome Sequence',0,5)"
fi

if [[ $arrayCount > 0 ]];
then
  echo "Insert array data into cb_criteria_menu"
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES
  (6,0,'Program Data','ARRAY_DATA','','Global Diversity Array',0,6)"
fi

echo "Insert cb_criteria_menu"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,sort_order)
VALUES
(7,0,'Domains','CONDITION','','Conditions',0,7),
(8,0,'Domains','PROCEDURE','','Procedures',0,8),
(9,0,'Domains','DRUG','','Drugs',0,9),
(10,0,'Domains','MEASUREMENT','','Labs and Measurements',0,10),
(11,0,'Domains','VISIT','VISIT','Visits',0,11),
(12,0,'Domains','OBSERVATION','','Observations',0,12),
(13,0,'Domains','DEVICE','','Devices',0,13),
(14,1,'Program Data','PERSON','AGE','Age',0,1),
(15,1,'Program Data','PERSON','DECEASED','Deceased',0,2),
(16,1,'Program Data','PERSON','ETHNICITY','Ethnicity',0,3),
(17,1,'Program Data','PERSON','GENDER','Gender Identity',0,4),
(18,1,'Program Data','PERSON','RACE','Race',0,5),
(19,1,'Program Data','PERSON','SEX','Sex Assigned at Birth',0,6)"