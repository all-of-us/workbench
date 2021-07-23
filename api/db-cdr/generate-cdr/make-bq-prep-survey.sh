#!/bin/bash

# Create a new prep ppi tables from redcap file process in bucket: all-of-us-workbench-private-cloudsql/cb_prep_tables/redcap/$DATE.
set -e

export BQ_PROJECT=$1        # CDR project
export BQ_DATASET=$2        # CDR dataset
export DATE=$3              # Redcap survey file date
export TIER=$4              # Ex: registered or controlled

BUCKET="all-of-us-workbench-private-cloudsql"
CSV_HOME_DIR="cb_prep_tables/redcap/$DATE"
CDR_CSV_DIR="cdr_csv_files"
EXPECTED_TABLES=("prep_ppi_basics"
"prep_ppi_cope"
"prep_ppi_family_health"
"prep_ppi_health_care_access"
"prep_ppi_lifestyle"
"prep_ppi_overall_health"
"prep_ppi_personal_medical_history")

echo "Starting load of prep ppi tables into $BQ_PROJECT:$BQ_DATASET"

TABLES=$(gsutil ls gs://"$BUCKET"/"$CSV_HOME_DIR"/*_"$TIER".csv | cut -d'/' -f7 | cut -d'.' -f1 | awk -F"_$TIER" '{print $1}')

# Validate that all expected files are in bucket
DIFF_OUTPUT=( $(echo ${EXPECTED_TABLES[@]} ${TABLES[@]} | tr ' ' '\n' | sort | uniq -u) )
if [[ ${#DIFF_OUTPUT[@]} > 0 ]];
  then
  echo "Missing following files: ${DIFF_OUTPUT[@]}"
  exit 1
fi

schema_path=generate-cdr/bq-schemas

echo "Starting creation of prep ppi tables"
for tableName in $TABLES
do
  echo "Processing $tableName table"
  bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$tableName"
  bq load --skip_leading_rows=1 --project_id="$BQ_PROJECT" --source_format=CSV "$BQ_DATASET.$tableName" \
  gs://"$BUCKET"/"$CSV_HOME_DIR"/"${tableName}_$TIER.csv" "$schema_path/$tableName.json"
done
echo "Completed creation of prep ppi tables"

echo "Starting creation of the prep_survey table"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"
bq --quiet --project_id="$BQ_PROJECT" mk --schema="$schema_path/prep_survey.json" "$BQ_DATASET.prep_survey"

echo "Loading The Basics - Inserting prep_ppi_basics into prep_survey"
bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
     a.id
     , a.parent_id
     ,'SURVEY' as domain_id
     , 0 AS is_standard
     ,'PPI' AS type
     , a.type as subtype
     , CASE WHEN a.type='ANSWER' THEN c.concept_id ELSE d.concept_id END AS concept_id
     , a.code
     , a.name
     , CAST (if(a.type='ANSWER',d.concept_id,null) AS STRING) AS value
     , if(a.type='ANSWER',0,1) AS is_group
     , if (a.type = 'TOPIC', 0, 1) AS is_selectable
     , CASE WHEN a.name = 'Select a value' THEN 1 ELSE 0 END AS has_attribute
     , 1 AS has_hierarchy
     , 'basics' AS survey
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_basics\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_basics\` b on a.parent_id = b.id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on lower(b.code) = lower(c.concept_code)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on lower(a.code) = lower(d.concept_code)
order by a.id"

echo "Loading Lifestyle - Inserting prep_ppi_lifestyle into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
     ROW_NUMBER() OVER (ORDER BY a.id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS id
     , CASE WHEN a.type = 'SURVEY' THEN 0 ELSE a.parent_id +  (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) END AS parent_id
     ,'SURVEY' AS domain_id
     , 0 AS is_standard
     ,'PPI' AS type
     , a.type as subtype
     , CASE WHEN a.type='ANSWER' THEN c.concept_id ELSE d.concept_id END AS concept_id
     , a.code
     , a.name
     , CAST (if(a.type='ANSWER',d.concept_id,null) AS STRING) AS value
     , if(a.type='ANSWER',0,1) AS is_group
     , if (a.type = 'TOPIC', 0, 1) AS is_selectable
     , CASE WHEN a.name = 'Select a value' THEN 1 ELSE 0 END AS has_attribute
     , 1 AS has_hierarchy
     , 'lifestyle' AS survey
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_lifestyle\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_lifestyle\` b on a.parent_id = b.id
-- two concept codes associated with the code Lifestyle thus filtered out the incorrect one
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (lower(b.code) = lower(c.concept_code) and c.concept_id != 1333266)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on (lower(a.code) = lower(d.concept_code) and d.concept_id != 1333266)
order by a.id"

echo "Loading Overall Health - Inserting prep_ppi_overall_health into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
     a.new_id AS id
     , CASE WHEN b.id is null THEN 0 else b.new_id end as parent_id
     ,'SURVEY' as domain_id
     , 0 as is_standard
     ,'PPI' as type
     , a.type as subtype
     , CASE WHEN a.type='ANSWER' THEN c.concept_id ELSE d.concept_id END AS concept_id
     , a.code
     , a.name
     , CASE WHEN a.type ='ANSWER' and c.concept_id = 1585747 then a.name
            WHEN a.type = 'ANSWER' then CAST(d.concept_id as STRING)
            else null end as value
     , if(a.type='ANSWER',0,1) as is_group
     , if (a.type = 'TOPIC', 0, 1) as is_selectable
     , CASE WHEN a.name = 'Select a value' THEN 1 ELSE 0 end as has_attribute
     , 1 as has_hierarchy
     , 'overall health' as survey
FROM
(select ROW_NUMBER() OVER (ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS new_id, *
-- Filter out “Date of” items and additional item associated with Overall Health similar to Lifestyle
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_overall_health\` where name not like '%Date of%') a
LEFT JOIN(select ROW_NUMBER() OVER (ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS new_id, * FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_overall_health\` where name not like '%Date of%') b on a.parent_id = b.id
-- used aou-res-curation-output-prod project
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on (lower(b.code) = lower(c.concept_code) and c.concept_id != 1333057)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on (lower(a.code) = lower(d.concept_code) and d.concept_id != 1333057)
order by a.id"

echo "Loading Personal Medical History - Inserting prep_ppi_personal_medical_history into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
     ROW_NUMBER() OVER (ORDER BY a.id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS id
     , CASE WHEN a.type = 'SURVEY' THEN 0 ELSE a.parent_id +  (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) END AS parent_id
     ,'SURVEY' as domain_id
     , 0 as is_standard
     ,'PPI' as type
     , a.type as subtype
     , CASE WHEN a.type='ANSWER' THEN c.concept_id ELSE d.concept_id END AS concept_id
     , a.code
     , a.name
     , CAST (if(a.type='ANSWER',d.concept_id,null) AS STRING) AS value
     , if(a.type='ANSWER',0,1) as is_group
     , if (a.type = 'TOPIC', 0, 1) as is_selectable
     , CASE WHEN a.name = 'Select a value' THEN 1 ELSE 0 end as has_attribute
     , 1 as has_hierarchy
     , 'personal medical history' as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_personal_medical_history\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_personal_medical_history\` b on a.parent_id = b.id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on lower(b.code) = lower(c.concept_code)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on lower(a.code) = lower(d.concept_code)
order by a.id"

echo "Loading Family Health - Inserting prep_ppi_family_health into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT a.new_id as id
    , CASE WHEN a.parent_id = 0 THEN 0 ELSE b.new_id END as parent_id
    , a.domain_id
    , a.is_standard
    , a.type
    , a.subtype
    , a.concept_id
    , CASE WHEN a.subtype != 'ANSWER' then c.concept_code ELSE d.concept_code end as concept_id
    , a.name
    , cast(a.value as string)
    , a.is_group
    , a.is_selectable
    , a.has_attribute
    , a.has_hierarchy
    , 'family health history' as survey
    FROM
    (
        SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) as new_id, *
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_family_health\`
    ) a
    LEFT JOIN
    (   SELECT ROW_NUMBER() OVER(ORDER BY id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) as new_id,*
        FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_family_health\`
    ) b on a.parent_id = b.id
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.concept_id = c.concept_id
    LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on a.value = d.concept_id
order by a.id"

echo "Loading Health Care Access - Inserting prep_ppi_health_care_access into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
    ROW_NUMBER() OVER (ORDER BY a.id) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS id
    , CASE WHEN a.type = 'SURVEY' THEN 0 ELSE a.parent_id +  (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) END AS parent_id
    ,'SURVEY' as domain_id
    , 0 as is_standard
    ,'PPI' as type
    , a.type as subtype
    , CASE WHEN a.type='ANSWER' THEN c.concept_id ELSE d.concept_id END AS concept_id
    , a.code
    , a.name
    , CAST (if(a.type='ANSWER',d.concept_id,null) AS STRING) AS value
    , if(a.type='ANSWER',0,1) as is_group
    , if (a.type = 'TOPIC', 0, 1) as is_selectable
    , CASE WHEN a.name = 'Select a value' THEN 1 ELSE 0 end as has_attribute
    , 1 as has_hierarchy
    , 'health care access' as survey
FROM \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_health_care_access\` a
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_health_care_access\` b on a.parent_id = b.id
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on lower(b.code) = lower(c.concept_code)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on lower(a.code) = lower(d.concept_code)
order by a.id"

echo "Loading COPE - Inserting prep_ppi_cope into prep_survey"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`
    (
        id
        ,parent_id
        ,domain_id
        ,is_standard
        ,type
        ,subtype
        ,concept_id
        ,code
        ,name
        ,value
        ,is_group
        ,is_selectable
        ,has_attribute
        ,has_hierarchy
        ,survey
    )
SELECT
     a.new_id as id
     , CASE WHEN b.survey_seq is null THEN 0 else b.new_id end as parent_id
     ,'SURVEY' as domain_id
     , 0 as is_standard
     ,'PPI' as type
     , UPPER(a.type) as subtype
     , CASE WHEN a.type='Answer' THEN c.concept_id ELSE d.concept_id END AS concept_id
     , a.concept_code
     , a.display
     , CAST (if(a.type='Answer',d.concept_id,null) AS STRING) AS value
     , if(a.type='Answer',0,1) as is_group
     , if (a.type = 'Topic', 0, 1) as is_selectable
     , CASE WHEN a.display = 'Select a value' THEN 1 ELSE 0 end as has_attribute
     , 1 as has_hierarchy
     , 'COVID-19 Participant Experience (COPE) Survey' as survey
FROM
(select ROW_NUMBER() OVER (ORDER BY survey_seq) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS new_id, *
from \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_cope\`
-- Filter out ‘Please specify’ items from survey
where lower(display) not like '%please specify%') a
LEFT JOIN
(select ROW_NUMBER() OVER (ORDER BY survey_seq) + (SELECT MAX(id) FROM \`$BQ_PROJECT.$BQ_DATASET.prep_survey\`) AS new_id, *
from \`$BQ_PROJECT.$BQ_DATASET.prep_ppi_cope\`
where lower(display) not like '%please specify%') b on (a.parent_pmi_code = b.concept_code)
-- used aou-res-curation-output-prod project
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` c on lower(b.concept_code) = lower(c.concept_code)
LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` d on lower(a.concept_code) = lower(d.concept_code)
order by 1"

echo "Extracting prep_survey to the proper bucket"
bq extract --project_id="$BQ_PROJECT" --destination_format CSV --print_header=false \
"$BQ_DATASET.prep_survey" gs://"$BUCKET"/"$BQ_DATASET"/"$CDR_CSV_DIR"/prep_survey.csv
echo "Completed extract into bucket(gs://$BUCKET/$BQ_DATASET/$CDR_CSV_DIR/prep_survey.csv)"

echo "Cleaning up prep tables"
for tableName in $TABLES
do
  echo "Deleting $tableName table"
  bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.$tableName"
done
echo "Deleting prep_survey table"
bq --project_id="$BQ_PROJECT" rm -f "$BQ_DATASET.prep_survey"
echo "Completed clean up of prep tables"

echo "Completed creation of the prep_survey table"