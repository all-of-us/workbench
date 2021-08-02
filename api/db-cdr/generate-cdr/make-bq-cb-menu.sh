#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

# Create bq tables we have json schema for
schema_path=generate-cdr/bq-schemas
create_tables=(cb_criteria_menu cb_menu)

for t in "${create_tables[@]}"
do
    bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/$t.json $BQ_DATASET.$t
done

echo "Getting fitbit count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_fitbit = 1"
fitbitCount=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting wgv count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_whole_genome_variant = 1"
wgvCount=$(bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql "$query" | tr -dc '0-9')

###############################
# CREATE cb_criteria_menu TABLE
###############################
echo "Insert cb_criteria_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,sort_order)
VALUES
(1,0,'Program Data','PERSON','','Demographics',1,1),
(2,0,'Program Data','SURVEY','PPI','Surveys',0,2),
(3,0,'Program Data','PHYSICAL_MEASUREMENT','PPI','Physical Measurements',0,3)"

if [[ $fitbitCount > 0 ]];
then
  echo "Insert fitbit into cb_criteria_menu"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES
  (4,0,'Program Data','FITBIT','FITBIT','Fitbit',0,4)"
fi

if [[ $wgvCount > 0 ]];
then
  echo "Insert wgv into cb_criteria_menu"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES
  (5,0,'Program Data','WHOLE_GENOME_VARIANT','','Whole Genome Variant',0,5)"
fi

echo "Insert cb_criteria_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,sort_order)
VALUES
(6,0,'Domains','CONDITION','','Conditions',0,6),
(7,0,'Domains','PROCEDURE','','Procedures',0,7),
(8,0,'Domains','DRUG','','Drugs',0,8),
(9,0,'Domains','MEASUREMENT','','Labs and Measurements',0,9),
(10,0,'Domains','VISIT','VISIT','Visits',0,10),
(12,1,'Program Data','PERSON','AGE','Age',0,1),
(13,1,'Program Data','PERSON','DECEASED','Deceased',0,2),
(14,1,'Program Data','PERSON','ETHNICITY','Ethnicity',0,3),
(15,1,'Program Data','PERSON','GENDER','Gender Identity',0,4),
(16,1,'Program Data','PERSON','RACE','Race',0,5),
(17,1,'Program Data','PERSON','SEX','Sex Assigned at Birth',0,6)"

###############################
# CREATE cb_menu TABLE
###############################
echo "Insert cb_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order)
VALUES
(1,0,'Program Data','PERSON','','Demographics',1,0,1),
(2,0,'Program Data','SURVEY','PPI','Surveys',0,0,2),
(3,0,'Program Data','PHYSICAL_MEASUREMENT','PPI','Physical Measurements',0,0,3)"

if [[ $fitbitCount > 0 ]];
then
  echo "Insert fitbit into cb_menu"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order)
  VALUES
  (4,0,'Program Data','FITBIT','FITBIT','Fitbit',0,0,4)"
fi

if [[ $wgvCount > 0 ]];
then
  echo "Insert wgv into cb_menu"
  bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order)
  VALUES
  (5,0,'Program Data','WHOLE_GENOME_VARIANT','','Whole Genome Variant',0,0,5)"
fi

echo "Insert cb_menu"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_menu\`
    (id,parent_id,category,domain_id,type,name,is_group,is_standard,sort_order)
VALUES
(6,0,'Standard Domains','CONDITION','','Conditions',0,1,6),
(7,0,'Standard Domains','PROCEDURE','','Procedures',0,1,7),
(8,0,'Standard Domains','DRUG','','Drugs',0,1,8),
(9,0,'Standard Domains','MEASUREMENT','','Labs and Measurements',0,1,9),
(10,0,'Standard Domains','VISIT','VISIT','Visits',0,1,10),
(11,0,'Source Domains','CONDITION','','Conditions (ICD9, ICD10)',0,0,11),
(12,0,'Source Domains','PROCEDURE','','Procedures (ICD9, ICD10, CPT)',0,0,12),
(13,1,'Program Data','PERSON','AGE','Age',0,0,1),
(14,1,'Program Data','PERSON','DECEASED','Deceased',0,0,2),
(15,1,'Program Data','PERSON','ETHNICITY','Ethnicity',0,0,3),
(16,1,'Program Data','PERSON','GENDER','Gender Identity',0,0,4),
(17,1,'Program Data','PERSON','RACE','Race',0,0,5),
(18,1,'Program Data','PERSON','SEX','Sex Assigned at Birth',0,0,6)"