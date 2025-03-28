#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

# run this query to initializing our .bigqueryrc configuration file
# otherwise this will corrupt the output of the first call to find_info()
echo "Running a simple select to avoid problem with initializing our .bigqueryrc configuration file"
query="select count(*) from \`$BQ_PROJECT.$BQ_DATASET.concept\`"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query"

echo "Getting fitbit count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_fitbit = 1"
fitbitCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting wgv count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_whole_genome_variant = 1"
wgvCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting long read wgs count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_lr_whole_genome_variant = 1"
longReadWGSCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting array_data count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_array_data = 1"
arrayCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting Structural Variant data count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_structural_variant_data = 1"
structuralVariantDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting Wear Consent data count"
query="select count(*) as count from \`$BQ_PROJECT.$BQ_DATASET.cb_search_person\`
where has_wear_consent = 1"
wearConsentDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Getting self_reported_category_concept_id column count"
query="select count(column_name) as count from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\`
where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\""
selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')
echo "self reported count: $selfReportedCategoryDataCount"

echo "Checking if device tables exists in this dataset"
query="select count(table_id) as count from \`$BQ_PROJECT.$BQ_DATASET.__TABLES__\`
where table_id=\"device\""
deviceTableExist=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

echo "Checking if EtM tables exists in this dataset"
query="select count(table_id) as count from \`$BQ_PROJECT.$BQ_DATASET.__TABLES__\`
where table_id=\"delaydiscounting\""
etmTablesExist=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

###############################
# CREATE cb_criteria_menu TABLE
###############################
function insertCriteriaMenu(){
  bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
  "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
      (id,parent_id,category,domain_id,type,name,is_group,sort_order)
  VALUES $1"
}
ID=0

echo "Insert into cb_criteria_menu"
insertCriteriaMenu "
($((++ID)),0,'Program Data','PERSON','','Demographics',1,$ID),
($((++ID)),0,'Program Data','SURVEY','PPI','Surveys',1,$ID),
($((++ID)),0,'Program Data','PHYSICAL_MEASUREMENT','PPI','Physical Measurements',0,$ID)"

if [[ $fitbitCount > 0 ]];
then
  echo "Insert fitbit into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),0,'Program Data','WEARABLES','','Wearables',1,$ID)"
fi

if [[ $wgvCount > 0 || $longReadWGSCount > 0 || $arrayCount > 0 || $structuralVariantDataCount > 0 || "$TABLE_LIST" == *"prep_vat"* ]];
then
  echo "Insert Genomics parent menu item into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),0,'Program Data','GENOMICS','','Genomics',1,$ID)"
fi

echo "Insert cb_criteria_menu"
insertCriteriaMenu "
($((++ID)),0,'Domains','CONDITION','','Conditions',0,$ID),
($((++ID)),0,'Domains','PROCEDURE','','Procedures',0,$ID),
($((++ID)),0,'Domains','DRUG','','Drugs',0,$ID),
($((++ID)),0,'Domains','MEASUREMENT','','Labs and Measurements',0,$ID),
($((++ID)),0,'Domains','VISIT','VISIT','Visits',0,$ID),
($((++ID)),0,'Domains','OBSERVATION','','Observations',0,$ID),
($((++ID)),0,'Domains','DEVICE','','Devices',0,$ID),
($((++ID)),0,'Domains','PERSON','HAS_EHR_DATA','Has EHR data',0,$ID),
($((++ID)),0,'Concepts','CONCEPT_SET','','Concept Set',0,$ID),
($((++ID)),0,'Concepts','CONCEPT_QUICK_ADD','','Concept Quick Add',0,$ID),
($((++ID)),1,'Program Data','PERSON','AGE','Age',0,1),
($((++ID)),1,'Program Data','PERSON','DECEASED','Deceased',0,2),
($((++ID)),1,'Program Data','PERSON','ETHNICITY','Ethnicity',0,3),
($((++ID)),1,'Program Data','PERSON','GENDER','Gender Identity',0,4),
($((++ID)),1,'Program Data','PERSON','RACE','Race',0,5),
($((++ID)),1,'Program Data','PERSON','SEX','Sex Assigned at Birth',0,6)"

if [[ $selfReportedCategoryDataCount > 0 ]];
then
  insertCriteriaMenu "($((++ID)),1,'Program Data','PERSON','SELF_REPORTED_CATEGORY','Self Reported Category',0,7)"
fi
insertCriteriaMenu "($((++ID)),2,'Program Data','SURVEY','PPI','All Surveys',0,1)"

echo "Adding surveys"
query="select name from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria\`
where domain_id = 'SURVEY'
and parent_id = 0
order by id"
surveyNames=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql --format csv "$query")

echo "Getting parent id"
query="select id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\` where domain_id = 'SURVEY' and is_group = 1"
PARENT_ID=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

SORT_ORDER=1

while IFS= read -r line
do
  if [[ "$line" != "name" ]]; then
    bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
      "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\`
          (id,parent_id,category,domain_id,type,name,is_group,sort_order)
      VALUES
      ($((++ID)),$PARENT_ID,'Program Data','SURVEY','PPI','$line',0,$((++SORT_ORDER)))"
  fi
done <<< "$surveyNames"

if [[ $etmTablesExist > 0 ]];
then
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','ETM_DELAYDISCOUNTING','ETM_DELAYDISCOUNTING','Has Any EtM Now or Later test data',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','ETM_EMORECOG','ETM_EMORECOG','Has Any EtM Guess The Emotion test data',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','ETM_FLANKER','ETM_FLANKER','Has Any EtM Left or Right test data',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','ETM_GRADCPT','ETM_GRADCPT','Has Any EtM City or Mountain test data',0,$((++SORT_ORDER)))"
fi

if [[ $fitbitCount > 0 ]];
then
  echo "Insert fitbit into cb_criteria_menu"

  echo "Getting parent id"
  query="select id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\` where domain_id = 'WEARABLES'"
  PARENT_ID=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

  SORT_ORDER=0
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT','FITBIT','Has Any Fitbit Data',0,$((++SORT_ORDER)))"
  if [[ $deviceTableExist > 0 ]];
  then
    insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_PLUS_DEVICE','FITBIT_PLUS_DEVICE','Has Any Fitbit Data Plus Device',0,$((++SORT_ORDER)))"
  fi
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_ACTIVITY','FITBIT_ACTIVITY','Fitbit Activity Summary',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_HEART_RATE_SUMMARY','FITBIT_HEART_RATE_SUMMARY','Fitbit Heart Rate Summary',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_HEART_RATE_LEVEL','FITBIT_HEART_RATE_LEVEL','Fitbit Heart Rate Minute Level',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_INTRADAY_STEPS','FITBIT_INTRADAY_STEPS','Fitbit Steps Intraday',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_SLEEP_DAILY_SUMMARY','FITBIT_SLEEP_DAILY_SUMMARY','Fitbit Sleep Daily Summary',0,$((++SORT_ORDER)))"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_SLEEP_LEVEL','FITBIT_SLEEP_LEVEL','Fitbit Sleep Level',0,$((++SORT_ORDER)))"
  if [[ $deviceTableExist > 0 ]];
  then
    insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','FITBIT_DEVICE','FITBIT_DEVICE','Fitbit Device',0,$((++SORT_ORDER)))"
  fi
    
  if [[ $wearConsentDataCount > 0 ]];
  then
    echo "Insert wear consent into cb_criteria_menu"
    insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','WEAR_CONSENT','WEAR_CONSENT','Wear Consent',0,$((++SORT_ORDER)))"
  fi
fi

echo "Getting parent id for GENOMICS"
query="select id from \`$BQ_PROJECT.$BQ_DATASET.cb_criteria_menu\` where domain_id = 'GENOMICS'"
PARENT_ID=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')
SORT_ORDER=0

if [[ $wgvCount > 0 && $PARENT_ID > 0 ]];
then
  echo "Insert wgv into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','WHOLE_GENOME_VARIANT','','has Short Read WGS data',0,$((++SORT_ORDER)))"
fi

if [[ $longReadWGSCount > 0 && $PARENT_ID > 0 ]];
then
  echo "Insert long read wgs into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','LR_WHOLE_GENOME_VARIANT','','has Long Read WGS data',0,$((++SORT_ORDER)))"
fi

if [[ $arrayCount > 0 && $PARENT_ID > 0 ]];
then
  echo "Insert array data into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','ARRAY_DATA','','has Global Diversity Array data',0,$((++SORT_ORDER)))"
fi

if [[ $structuralVariantDataCount > 0 && $PARENT_ID > 0 ]];
then
  echo "Insert Structural Variant data into cb_criteria_menu"
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','STRUCTURAL_VARIANT_DATA','','has Structural Variant data',0,$((++SORT_ORDER)))"
fi

if [[ "$TABLE_LIST" == *"prep_vat"* && $PARENT_ID > 0 ]]
then
  echo "Insert SNP/Indel Variants data into cb_criteria_menu."
  insertCriteriaMenu "($((++ID)),$PARENT_ID,'Program Data','SNP_INDEL_VARIANT','','SNP/Indel Variants',0,$((++SORT_ORDER)))"
fi
