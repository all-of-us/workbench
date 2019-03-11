#!/bin/bash

# This generates the criteria tables for the CDR

# PREP: upload all prep tables

# Example usage:
# ./project.rb generate-criteria-table --bq-project aou-res-curation-prod --bq-dataset deid_output_20181116
# ./project.rb generate-criteria-table --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606


set -xeuo pipefail
IFS=$'\n\t'

# --cdr=cdr_version ... *optional
USAGE="./generate-cdr/generate-criteria-table.sh --bq-project <PROJECT> --bq-dataset <DATASET>"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done


if [ -z "${BQ_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${BQ_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# Check that bq_project exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
if [ -z "$datasets" ]
then
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi
if [[ $datasets =~ .*$BQ_DATASET.* ]]; then
  echo "$BQ_PROJECT.$BQ_DATASET exists. Good. Carrying on."
else
  echo "$BQ_PROJECT.$BQ_DATASET does not exist. Please specify a valid project and dataset."
  exit 1
fi

# Check that bq_dataset exists and exit if not
datasets=$(bq --project=$BQ_PROJECT ls)
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


################################################
# CREATE TABLES
################################################
echo "CREATE TABLES - criteria"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria\`
(
  id            INT64,
  parent_id     INT64,
  type          STRING,
  subtype       STRING,
  code          STRING,
  name          STRING,
  is_group      INT64,
  is_selectable INT64,
  est_count     INT64,
  domain_id     STRING,
  concept_id    INT64,
  has_attribute INT64,
  path          STRING
)"

# table that holds the ingredient --> coded drugs mapping
echo "CREATE TABLES - criteria_ancestor"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor\`
(
  ancestor_id INT64,
  descendant_id INT64
)"

# table that holds categorical results and min/max information about individual labs
echo "CREATE TABLES - criteria_attribute"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute\`
(
  id                    INT64,
  concept_id            INT64,
  value_as_concept_id	INT64,
  concept_name          STRING,
  type                  STRING,
  est_count             STRING
)"

# table that holds the drug brands -> ingredients relationship mapping
echo "CREATE TABLES - criteria_relationship"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_relationship\`
(
  concept_id_1 INT64,
  concept_id_2 INT64
)"

echo "CREATE TABLES - atc_rel_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`
(
  p_concept_id INT64,
  p_concept_code STRING,
  p_concept_name STRING,
  p_domain_id STRING,
  concept_id INT64,
  concept_code STRING,
  concept_name STRING,
  domain_id STRING
)"

echo "CREATE TABLES - loinc_rel_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`
(
  p_concept_id INT64,
  p_concept_code STRING,
  p_concept_name STRING,
  p_domain_id STRING,
  concept_id INT64,
  concept_code STRING,
  concept_name STRING,
  domain_id STRING
)"

echo "CREATE TABLES - snomed_rel_cm_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`
(
  p_concept_id INT64,
  p_concept_code STRING,
  p_concept_name STRING,
  p_domain_id STRING,
  concept_id INT64,
  concept_code STRING,
  concept_name STRING,
  domain_id STRING
)"

echo "CREATE TABLES - snomed_rel_pcs_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`
(
  p_concept_id INT64,
  p_concept_code STRING,
  p_concept_name STRING,
  p_domain_id STRING,
  concept_id INT64,
  concept_code STRING,
  concept_name STRING,
  domain_id STRING
)"

echo "CREATE TABLES - snomed_rel_meas_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
(
  p_concept_id INT64,
  p_concept_code STRING,
  p_concept_name STRING,
  p_domain_id STRING,
  concept_id INT64,
  concept_code STRING,
  concept_name STRING,
  domain_id STRING
)"

################################################
# CREATE VIEWS
################################################
echo "CREATE VIEWS - v_loinc_rel"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` AS
SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE (((((((((((CR.CONCEPT_ID_1 = C1.CONCEPT_ID)
    AND (CR.CONCEPT_ID_2 = C2.CONCEPT_ID))
    AND (CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID))
    AND (C1.VOCABULARY_ID = 'LOINC'))
    AND (C2.VOCABULARY_ID = 'LOINC'))
    AND (R.IS_HIERARCHICAL = '1'))
    AND (R.DEFINES_ANCESTRY = '1'))
    AND (C1.CONCEPT_CLASS_ID
    IN (('LOINC Hierarchy'), ('Lab Test'))))
    AND (C2.CONCEPT_CLASS_ID
    IN (('LOINC Hierarchy'), ('Lab Test'))))
    AND (CR.RELATIONSHIP_ID = 'Subsumes'))
    AND (C2.CONCEPT_CODE NOT IN (
      SELECT DISTINCT C1.CONCEPT_CODE
      FROM (((
      \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR
      LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` C1 ON ((CR.CONCEPT_ID_1 = C1.CONCEPT_ID)))
      LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.concept\` C2 ON ((CR.CONCEPT_ID_2 = C2.CONCEPT_ID)))
      LEFT JOIN \`$BQ_PROJECT.$BQ_DATASET.relationship\` R ON ((CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID)))
      WHERE ((((((C1.VOCABULARY_ID = 'LOINC')
      AND (C2.VOCABULARY_ID = 'LOINC'))
      AND (R.IS_HIERARCHICAL = '1'))
      AND (R.DEFINES_ANCESTRY = '1'))
      AND REGEXP_CONTAINS(C2.CONCEPT_CODE, r'^\d?\d?\d?\d?\d\-\d$'))
      AND (R.RELATIONSHIP_NAME = 'Panel contains (LOINC)')))))"

echo "CREATE VIEWS - v_snomed_rel_cm"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` AS
SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
    AND C1.VOCABULARY_ID = 'SNOMED'
    AND C2.VOCABULARY_ID = 'SNOMED'
    AND C1.STANDARD_CONCEPT = 'S'
    AND C2.STANDARD_CONCEPT = 'S'
    AND R.IS_HIERARCHICAL = '1'
    AND R.DEFINES_ANCESTRY = '1'
    AND C1.DOMAIN_ID = 'Condition'
    AND C2.DOMAIN_ID = 'Condition'
    AND CR.RELATIONSHIP_ID = 'Subsumes'"

echo "CREATE VIEWS - v_snomed_rel_pcs"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\` AS
SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
    AND C1.VOCABULARY_ID = 'SNOMED'
    AND C2.VOCABULARY_ID = 'SNOMED'
    AND C1.STANDARD_CONCEPT = 'S'
    AND C2.STANDARD_CONCEPT = 'S'
    AND R.IS_HIERARCHICAL = '1'
    AND R.DEFINES_ANCESTRY = '1'
    AND C1.DOMAIN_ID = 'Procedure'
    AND C2.DOMAIN_ID = 'Procedure'
    AND CR.RELATIONSHIP_ID = 'Subsumes'"

echo "CREATE VIEWS - v_snomed_rel_meas"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` AS
SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
    AND C1.VOCABULARY_ID = 'SNOMED'
    AND C2.VOCABULARY_ID = 'SNOMED'
    AND C1.STANDARD_CONCEPT = 'S'
    AND C2.STANDARD_CONCEPT = 'S'
    AND R.IS_HIERARCHICAL = '1'
    AND R.DEFINES_ANCESTRY = '1'
    AND C1.DOMAIN_ID = 'Measurement'
    AND C2.DOMAIN_ID = 'Measurement'
    AND CR.RELATIONSHIP_ID = 'Subsumes'"

################################################
# ICD9
################################################
echo "ICD9 - inserting data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, is_group, is_selectable,
case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
case when is_group = 0 and IS_SELECTABLE = 1 then b.DOMAIN_ID else null end as DOMAIN_ID,
case when is_group = 0 and IS_SELECTABLE = 1 then b.CONCEPT_ID else null end as CONCEPT_ID, 0, PATH
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` a
left join
  (select * from \`$BQ_PROJECT.$BQ_DATASET.concept\`
  where (vocabulary_id in ('ICD9CM', 'ICD9Proc') and concept_code != '92')
  or (vocabulary_id = 'ICD9Proc' and concept_code = '92')) b on a.CODE = b.CONCEPT_CODE
left join
  (select concept_id, count(distinct person_id) cnt from
    (SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
    WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
    WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
    WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
    WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id) x
  GROUP BY 1) c on b.concept_id = c.concept_id
where type = 'ICD9'
and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
order by 1"

echo "ICD9 - generating parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (select c.id, count(distinct person_id) cnt
from (select * from (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD9' and is_group = 1 and is_selectable = 1) a
left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
left join
  (select a.id, b.*
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
    (
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
      (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
      WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
      (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
      WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
      (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
      WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
      (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD9' and is_group = 0 and is_selectable = 1) b
      WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id
    ) b
  where a.concept_id = b.concept_id) d on c.descendant_id = d.id
group by 1) y
where x.id = y.id"

echo "ICD9 - delete zero count parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"delete
from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
where type = 'ICD9'
and is_selectable = 1
and (est_count is null or est_count = 0)"

################################################
# ICD10
################################################
echo "ICD10 - CM - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select ID, PARENT_ID, "TYPE", SUBTYPE, CODE, NAME, is_group, is_selectable,
case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
case when is_group = 0 and IS_SELECTABLE = 1 then b.DOMAIN_ID else null end as DOMAIN_ID,
case when is_group = 0 and IS_SELECTABLE = 1 then b.CONCEPT_ID else null end as CONCEPT_ID, 0, PATH
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` a
left join (select * from \`$BQ_PROJECT.$BQ_DATASET.concept\` where vocabulary_id in ('ICD10CM')) b on a.CODE = b.CONCEPT_CODE
left join
  (select concept_id, count(distinct person_id) cnt from
    (SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
    WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
    WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
    WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
    WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id) x
  GROUP BY 1) c on b.concept_id = c.concept_id
where type = 'ICD10'
and subtype = 'CM'
and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
order by 1"

echo "ICD10 - PCS - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select ID, PARENT_ID, "TYPE", SUBTYPE, CODE, NAME, is_group, is_selectable,
case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
case when is_group = 0 and IS_SELECTABLE = 1 then b.DOMAIN_ID else null end as DOMAIN_ID,
case when is_group = 0 and IS_SELECTABLE = 1 then b.CONCEPT_ID else null end as CONCEPT_ID, 0, PATH
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` a
left join (select * from \`$BQ_PROJECT.$BQ_DATASET.concept\` where vocabulary_id in ('ICD10PCS')) b on a.CODE = b.CONCEPT_CODE
left join
  (select concept_id, count(distinct person_id) cnt from
    (SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
    WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
    WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
    WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
    UNION DISTINCT
    SELECT person_id, concept_id
    FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
    (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
    WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id) x
  GROUP BY 1) c on b.concept_id = c.concept_id
where type = 'ICD10'
and subtype = 'PCS'
and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
order by 1"

echo "ICD10 - CM - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (select c.id, count(distinct person_id) cnt
from (select * from (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'CM' and parent_id != 0 and is_group = 1) a
left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
left join
  (select a.id, b.*
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
    (
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
      WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
      WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
      WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'CM' and is_group = 0 and is_selectable = 1) b
      WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id
    ) b
  where a.concept_id = b.concept_id) d on c.descendant_id = d.id
group by 1) y
where x.id = y.id"

echo "ICD10 - PCS - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (select c.id, count(distinct person_id) cnt
from (select * from (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'PCS' and parent_id != 0 and is_group = 1) a
left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
left join
  (select a.id, b.*
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
    (
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
      WHERE a.CONDITION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
      WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
      WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'ICD10' and subtype = 'PCS' and is_group = 0 and is_selectable = 1) b
      WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id
    ) b
  where a.concept_id = b.concept_id) d on c.descendant_id = d.id
group by 1) y
where x.id = y.id"

echo "ICD10 - delete zero count parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"delete
from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
where type = 'ICD10'
and is_selectable = 1
and (est_count is null or est_count = 0)"

################################################
# CPT
################################################
echo "CPT - insert data (do not insert zero count children)"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select ID, PARENT_ID, "TYPE", SUBTYPE, CODE, NAME, is_group, is_selectable,
case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
case when is_group = 0 and IS_SELECTABLE = 1 then b.DOMAIN_ID else null end as DOMAIN_ID,
case when is_group = 0 and IS_SELECTABLE = 1 then b.CONCEPT_ID else null end as CONCEPT_ID, 0, PATH
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` a
left join (select * from \`$BQ_PROJECT.$BQ_DATASET.concept\` where vocabulary_id in ('CPT4')) b on a.CODE = b.CONCEPT_CODE
left join
  (select concept_id, count(distinct person_id) cnt from
    (SELECT person_id, concept_id
	  FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a,
	  (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
	  WHERE a.drug_SOURCE_CONCEPT_ID = b.concept_Id
	  UNION DISTINCT
	  SELECT person_id, concept_id
	  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
	  (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
	  WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
	  UNION DISTINCT
	  SELECT person_id, concept_id
	  FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
	  (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
	  WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
	  UNION DISTINCT
	  SELECT person_id, concept_id
	  FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
	  (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
	  WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id) x
  GROUP BY 1) c on b.concept_id = c.concept_id
where type = 'CPT'
and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
order by 1"

echo "CPT - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (select c.id, count(distinct person_id) cnt
from (select * from (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'CPT' and parent_id != 0 and is_group = 1) a
left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
left join
  (select a.id, b.*
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
    (
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a,
      (select concept_id, path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
      WHERE a.drug_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
      WHERE a.OBSERVATION_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
      WHERE a.MEASUREMENT_SOURCE_CONCEPT_ID = b.concept_id
      UNION DISTINCT
      SELECT person_id, concept_id
      FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a,
      (select concept_id, path, code from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'CPT' and is_group = 0 and is_selectable = 1) b
      WHERE a.PROCEDURE_SOURCE_CONCEPT_ID = b.concept_id
    ) b
  where a.concept_id = b.concept_id) d on c.descendant_id = d.id
group by 1) y
where x.id = y.id"

echo "CPT - delete zero count parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"delete
from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
where type = 'CPT'
and ( (parent_id != 0 and (est_count is null or est_count = 0))
or (is_group = 1 and id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
where type = 'CPT' and est_count is not null)) )"

################################################
# PPI
################################################
echo "PPI - insert data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select id, parent_id, type, subtype, code, name, is_group, is_selectable,
case when is_selectable = 1 then 0 else null end as est_count, domain_id, concept_id, has_attribute, path
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'PPI'
order by 1"

echo "PPI - generate child counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from (SELECT observation_source_concept_id as concept_id, value_as_concept_id as code, count(distinct person_id) cnt
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  where observation_source_concept_id in
  (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'PPI' and is_group = 0 and is_selectable = 1 and concept_id != 1585747)
  group by 1,2) y
where x.type = 'PPI'
and x.concept_id = y.concept_id
and CAST(x.code as INT64) = y.code"

echo "PPI - this record is different than the others"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from (select id, case when cnt is null then 0 else cnt end as cnt
from
(select * from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'PPI' and is_group = 0 and is_selectable = 1 and concept_id = 1585747) a
left join (SELECT observation_source_concept_id as concept_id, cast(value_as_number as INT64) as code, count(distinct person_id) cnt
FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
where observation_source_concept_id = 1585747
group by 1,2) b on a.concept_id = b.concept_id and CAST(a.name as INT64) = b.code) y
where x.type = 'PPI'
and x.id = y.id"

echo "PPI - generate question counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from (SELECT observation_source_concept_id as concept_id, count(distinct person_id) cnt
  FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a
  where observation_source_concept_id in
  (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  where type = 'PPI' and is_group = 1 and is_selectable = 1)
  group by 1) y
where x.type = 'PPI'
and x.is_group = 1
and x.concept_id = y.concept_id"

echo "PPI - generate survey counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (
select c.id, count(distinct person_id) cnt
  from
    (select * from
      (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
        where type = 'PPI' and parent_id = 0) a
    left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
    left join
      (select a.id, b.*
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
        (
          SELECT person_id, concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.observation\` a,
            (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
              where type = 'PPI' and is_group = 1 and is_selectable = 1) b
          WHERE a.observation_source_concept_id = b.concept_id
        ) b
      where a.concept_id = b.concept_id) d on c.descendant_id = d.id
  group by 1) y
where x.type = 'PPI'
and x.id = y.id"

################################################
# PHYSICAL MEASUREMENTS (PM)
################################################
echo "PM - insert data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute, path)
select id, parent_id, type, subtype, code, name, is_group, is_selectable,
case when is_selectable = 1 then 0 else null end as est_count, domain_id, concept_id, has_attribute, path
from \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\` where type = 'PM'
order by 1"

echo "PM - counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from
  (select measurement_source_concept_id as concept_id, count(distinct person_id) as cnt
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
  where measurement_source_concept_id in (903126,903133,903121,903124,903135,903136)
  group by 1) y
where x.type = 'PM'
and x.concept_id = y.concept_id"

echo "PM - counts for heart rhythm, pregnancy, and wheelchair use"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
  from (select measurement_source_concept_id as concept_id, value_as_concept_id as code, count(distinct person_id) as cnt
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
  where measurement_source_concept_id IN (1586218, 903120, 903111)
  group by 1,2) y
where x.type = 'PM'
and x.concept_id = y.concept_id
and CAST(x.code as INT64) = y.code"

#----- BLOOD PRESSURE -----
# !!!!!!! WILL WANT TO REWRITE TO USE RELATIONSHIP INFO WHEN WE HAVE IT!!!!!!---
echo "PM - blood pressure  - hypotensive"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\`
set est_count = (select count(distinct person_id) from (
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903118
and value_as_number <= 90
intersect distinct
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903115
and value_as_number <= 60
))
where type = 'PM' and subtype = 'BP' and name LIKE 'Hypotensive%'"

echo "PM - blood pressure  - normal"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\`
set est_count = (select count(distinct person_id) from (
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903118
and value_as_number <= 120
intersect distinct
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903115
and value_as_number <= 80
))
where type = 'PM' and subtype = 'BP' and name LIKE 'Normal%'"

echo "PM - blood pressure  - pre-hypertensive"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\`
set est_count = (select count(distinct person_id) from (
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903118
and value_as_number BETWEEN 120 AND 139
intersect distinct
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903115
   and value_as_number BETWEEN 81 AND 89
    ))
where type = 'PM' and subtype = 'BP' and name LIKE 'Pre-Hypertensive%'"

echo "PM - blood pressure  - hypertensive"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\`
set est_count = (select count(distinct person_id) from (
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903118
and value_as_number >= 140
intersect distinct
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903115
and value_as_number >= 90
    ))
where type = 'PM' and subtype = 'BP' and name LIKE 'Hypertensive%'"

echo "PM - blood pressure  - detail"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\`
set est_count = (select count(distinct person_id) from (
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903118
intersect distinct
select person_id, measurement_date from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
   where measurement_source_concept_id = 903115
    ))
where type = 'PM' and subtype = 'BP' and name LIKE 'Blood Pressure Detail'"

################################################
# DEMOGRAPHICS
################################################
echo "DEMO - Age parent"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'DEMO','AGE','Age',1,0,0"

echo "DEMO - Age children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,has_attribute)
select row_num + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'DEMO' and subtype = 'AGE' and parent_id = 0) as parent_id,'DEMO' as type,'AGE' as subtype,
CAST(row_num AS STRING) as code, CAST(row_num AS STRING) as name,0 as is_group,1 as is_selectable,
case when b.cnt is null then 0 else b.cnt end as est_count, 0 as has_attribute
FROM (select ROW_NUMBER() OVER(ORDER BY person_id) as row_num from \`$BQ_PROJECT.$BQ_DATASET.person\` order by person_id limit 120) A
left join (select CAST(FLOOR(DATE_DIFF(CURRENT_DATE(), DATE(birth_datetime), MONTH)/12) as INT64) as age, count(*) as cnt
from \`$BQ_PROJECT.$BQ_DATASET.person\`
where person_id not in (select person_id from \`$BQ_PROJECT.$BQ_DATASET.death\`)
group by 1) b on a.row_num = b.age
order by 1"

echo "DEMO - Deceased"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,est_count,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'DEMO','DEC','Deceased',0,1,
(select count(distinct person_id) from \`$BQ_PROJECT.$BQ_DATASET.death\`),0"

echo "DEMO - Gender parent"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'DEMO','GEN','Gender',1,0,0"

echo "DEMO - Gender children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,est_count,concept_id, has_attribute)
select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'DEMO' and subtype = 'GEN' and parent_id = 0), 'DEMO', 'GEN',
CONCAT(UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2))), 0, 1, b.cnt, concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
left join (select gender_concept_id, count(distinct person_id) cnt from \`$BQ_PROJECT.$BQ_DATASET.person\` group by 1) b on a.concept_id = b.gender_concept_id
where domain_id = 'Gender' and standard_concept = 'S'"

echo "DEMO - Race parent"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'DEMO','RACE','Race',1,0,0"

echo "DEMO - Race children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,est_count,concept_id,has_attribute)
select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'DEMO' and subtype = 'RACE' and parent_id = 0), 'DEMO', 'RACE',
CONCAT(UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2))), 0, 1, b.cnt, concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
left join (select race_concept_id, count(distinct person_id) cnt from \`$BQ_PROJECT.$BQ_DATASET.person\` group by 1) b on a.concept_id = b.race_concept_id
where domain_id = 'Race' and standard_concept = 'S' and (b.cnt != 0 or b.cnt is not null)"

echo "DEMO - Ethnicity parent"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'DEMO','ETH','Ethnicity',1,0,0"

echo "DEMO - Ethnicity children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,est_count,concept_id,has_attribute)
select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'DEMO' and subtype = 'ETH' and parent_id = 0), 'DEMO', 'ETH',
CONCAT(UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2))), 0, 1, b.cnt, concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
left join (select ethnicity_concept_id, count(distinct person_id) cnt from \`$BQ_PROJECT.$BQ_DATASET.person\` group by 1) b on a.concept_id = b.ethnicity_concept_id
where domain_id = 'Ethnicity' and standard_concept = 'S'"

################################################
# VISITS
################################################
echo "VISITS - add items with counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,name,is_group,is_selectable,est_count,concept_id,domain_id,has_attribute)
select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as ID, 0 as parent_id, 'VISIT' as type,
concept_name as name, 0 as is_group, 1 as is_selectable, a.cnt, concept_id, 'Visit' as domain_id, 0 as has_attribute
from (select visit_concept_id, count(distinct person_id) cnt
from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\`
group by 1) a
left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.visit_concept_id = b.concept_id
where b.vocabulary_id in ('Visit', 'Visit Type')"

################################################
# DRUGS
################################################
echo "DRUGS - temp table - ATC4 to RXNORM"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select distinct b.p_concept_id, b.p_concept_code, b.p_concept_name, b.p_DOMAIN_ID,
    a.CONCEPT_ID, a.CONCEPT_CODE, a.CONCEPT_NAME, a.DOMAIN_ID
from
    (select c1.CONCEPT_ID, c1.CONCEPT_CODE, c1.CONCEPT_NAME, c1.DOMAIN_ID, c2.CONCEPT_ID atc_5
    from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, rxnorm, ingredient
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, atc, atc_5th
    where RELATIONSHIP_ID IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
        and c1.VOCABULARY_ID = 'RxNorm' and c1.CONCEPT_CLASS_ID = 'Ingredient' and c1.STANDARD_CONCEPT = 'S'
        and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 5th' and c2.STANDARD_CONCEPT = 'C'
        and c1.concept_id in
            (select ANCESTOR_CONCEPT_ID
            from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
            where DESCENDANT_CONCEPT_ID in (select distinct DRUG_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`))
    ) a
left join
    (select c1.CONCEPT_ID p_concept_id, c1.CONCEPT_CODE p_concept_code, c1.CONCEPT_NAME p_concept_name, c1.DOMAIN_ID p_DOMAIN_ID,
        c2.CONCEPT_ID atc_5, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
    from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID--parent, atc, atc_4
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID--child, atc, atc_5
    where RELATIONSHIP_ID = 'Subsumes'
        and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 4th' and c1.STANDARD_CONCEPT = 'C'
        and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 5th' and c2.STANDARD_CONCEPT = 'C') b on a.atc_5 = b.atc_5"

echo "DRUGS - temp table - ATC3 to ATC4"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID p_concept_id, c1.CONCEPT_CODE p_concept_code, c1.CONCEPT_NAME p_concept_name, c1.DOMAIN_ID p_DOMAIN_ID,
    c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 3rd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 4th' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)
    and c2.concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)"

echo "DRUGS - temp table - ATC2 TO ATC3"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID p_concept_id, c1.CONCEPT_CODE p_concept_code, c1.CONCEPT_NAME p_concept_name, c1.DOMAIN_ID p_DOMAIN_ID,
    c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 2nd' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 3rd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)
    and c2.concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)"

echo "DRUGS - temp table - ATC1 TO ATC2"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select c1.CONCEPT_ID p_concept_id, c1.CONCEPT_CODE p_concept_code, c1.CONCEPT_NAME p_concept_name, c1.DOMAIN_ID p_DOMAIN_ID,
    c2.CONCEPT_ID, c2.CONCEPT_CODE, c2.CONCEPT_NAME, c2.DOMAIN_ID
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on a.CONCEPT_ID_1 = c1.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c2 on a.CONCEPT_ID_2 = c2.CONCEPT_ID
where RELATIONSHIP_ID = 'Subsumes'
    and c1.VOCABULARY_ID = 'ATC' and c1.CONCEPT_CLASS_ID = 'ATC 1st' and c1.STANDARD_CONCEPT = 'C'
    and c2.VOCABULARY_ID = 'ATC' and c2.CONCEPT_CLASS_ID = 'ATC 2nd' and c2.STANDARD_CONCEPT = 'C'
    and c2.concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)
    and c2.concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`)"

echo "DRUGS - add roots"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute)
select row_number() over (order by CONCEPT_CODE) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID, 0, 'DRUG', 'ATC', CONCEPT_CODE, CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) ), 1, 0, concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\`
where VOCABULARY_ID = 'ATC' and CONCEPT_CLASS_ID = 'ATC 1st' and STANDARD_CONCEPT = 'C'"

echo "DRUGS - add root for unmapped ingredients"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID, 0, 'DRUG','ATC', 'Unmapped ingredients', 1, 0, 0"

echo "DRUGS - level 2"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, b.CONCEPT_CODE)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'DRUG', 'ATC',
b.CONCEPT_CODE, CONCAT( UPPER(SUBSTR(b.concept_name, 1, 1)), LOWER(SUBSTR(b.concept_name, 2)) ), 1, 1, b.CONCEPT_ID, 0, CAST(t.ID as STRING)
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
join \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG')
and type = 'DRUG'"

echo "DRUGS - level 3"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, b.CONCEPT_CODE)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'DRUG', 'ATC',
b.CONCEPT_CODE, CONCAT( UPPER(SUBSTR(b.concept_name, 1, 1)), LOWER(SUBSTR(b.concept_name, 2)) ), 1, 1, b.CONCEPT_ID, 0, CONCAT(t.path, '.', CAST(t.ID as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
join \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG')
and type = 'DRUG'"

echo "DRUGS - level 4"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, b.CONCEPT_CODE)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'DRUG', 'ATC',
b.CONCEPT_CODE, CONCAT( UPPER(SUBSTR(b.concept_name, 1, 1)), LOWER(SUBSTR(b.concept_name, 2)) ), 1, 1, b.CONCEPT_ID, 0, CONCAT(t.path, '.', CAST(t.ID as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
join \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG')
and type = 'DRUG'"

echo "DRUGS - level 5"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, upper(b.CONCEPT_NAME))+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'DRUG', 'ATC',
  b.CONCEPT_CODE, CONCAT( UPPER(SUBSTR(b.concept_name, 1, 1)), LOWER(SUBSTR(b.concept_name, 2)) ), 0, 1, b.CONCEPT_ID, 0, CONCAT(t.path, '.', CAST(t.ID as STRING))
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
  join \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG')
and type = 'DRUG'"

echo "DRUGS - add parents for unmapped ingredients"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,has_attribute, path)
select ROW_NUMBER() OVER(ORDER BY upper(CONCEPT_NAME)) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
(select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`  where name = 'Unmapped ingredients') as parent_id, 'DRUG','ATC',
CONCEPT_NAME as code, CONCEPT_NAME, 1, 0, 0, (select CAST(id as STRING) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`  where name = 'Unmapped ingredients')
from
  (select distinct UPPER(SUBSTR(a.CONCEPT_NAME, 1, 1)) CONCEPT_NAME
  from (select *
    from \`$BQ_PROJECT.$BQ_DATASET.concept\`
    where VOCABULARY_ID = 'RxNorm' and CONCEPT_CLASS_ID = 'Ingredient' and STANDARD_CONCEPT = 'S'
      and concept_id in (select ANCESTOR_CONCEPT_ID
      from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
      where DESCENDANT_CONCEPT_ID in (select distinct DRUG_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`))
    ) a
  full join
    (select b.*
    from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
      left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.CONCEPT_ID_1 = b.CONCEPT_ID
      left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.CONCEPT_ID_2 = c.CONCEPT_ID
    where RELATIONSHIP_ID IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
      and b.VOCABULARY_ID = 'RxNorm' and b.CONCEPT_CLASS_ID = 'Ingredient' and b.STANDARD_CONCEPT = 'S'
      and c.VOCABULARY_ID = 'ATC' and c.CONCEPT_CLASS_ID = 'ATC 5th' and c.STANDARD_CONCEPT = 'C') b on a.concept_id = b.concept_id
  where b.CONCEPT_ID is null) X"

echo "DRUGS - add unmapped ingredients"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, has_attribute, path)
select row_number() over (order by c.ID, upper(a.CONCEPT_NAME))+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), c.ID, 'DRUG', 'ATC', a.CONCEPT_CODE,
  CONCAT( UPPER(SUBSTR(a.concept_name, 1, 1)), LOWER(SUBSTR(a.concept_name, 2)) ), 0, 1, a.CONCEPT_ID, 0, CONCAT(c.path, '.', CAST(c.ID as STRING))
from (select *
  from \`$BQ_PROJECT.$BQ_DATASET.concept\`
  where VOCABULARY_ID = 'RxNorm' and CONCEPT_CLASS_ID = 'Ingredient' and STANDARD_CONCEPT = 'S'
    and concept_id in (select ANCESTOR_CONCEPT_ID
    from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
    where DESCENDANT_CONCEPT_ID in (select distinct DRUG_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`))
  ) a
full join
  (select b.*
  from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.CONCEPT_ID_1 = b.CONCEPT_ID
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.CONCEPT_ID_2 = c.CONCEPT_ID
  where RELATIONSHIP_ID IN ('RxNorm - ATC name','Mapped from', 'RxNorm - ATC')
    and b.VOCABULARY_ID = 'RxNorm' and b.CONCEPT_CLASS_ID = 'Ingredient' and b.STANDARD_CONCEPT = 'S'
    and c.VOCABULARY_ID = 'ATC' and c.CONCEPT_CLASS_ID = 'ATC 5th' and c.STANDARD_CONCEPT = 'C') b on a.concept_id = b.concept_id
join (select * from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and length(name) = 1) c on UPPER(SUBSTR(a.CONCEPT_NAME, 1, 1)) = c.name
where b.CONCEPT_ID is null"

echo "DRUGS - generate child counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from
    (select b.ANCESTOR_CONCEPT_ID as concept_id, count(distinct a.person_id) cnt
    from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
        join (select *
            from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` x
            left join \`$BQ_PROJECT.$BQ_DATASET.concept\` y on x.ANCESTOR_CONCEPT_ID = y.CONCEPT_ID
            where VOCABULARY_ID = 'RxNorm' and CONCEPT_CLASS_ID = 'Ingredient') b on a.DRUG_CONCEPT_ID = b.DESCENDANT_CONCEPT_ID
    group by 1) y
where x.type = 'DRUG'
and x.concept_id = y.concept_id"

echo "DRUGS - add brand names"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
    (id, parent_id, type, subtype, code, name, is_group, is_selectable, concept_id, has_attribute)
select ROW_NUMBER() OVER(ORDER BY upper(CONCEPT_NAME)) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS ID,
    0 as parent_id,'DRUG', 'BRAND', CONCEPT_CODE, CONCEPT_NAME, 0, 1, CONCEPT_ID, 0
FROM
  (select distinct b.concept_id, b.concept_name, b.concept_code
  from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.CONCEPT_ID_1 = b.CONCEPT_ID --brands
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` c on a.CONCEPT_ID_2 = c.CONCEPT_ID --ingredients
  where b.vocabulary_id in ('RxNorm','RxNorm Extension')
    and b.concept_class_id = 'Brand Name'
    and b.invalid_reason is null
    and c.concept_id in
      (select concept_id
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
      where type = 'DRUG'
        and subtype = 'ATC'
        and is_group = 0
        and is_selectable = 1)
  ) X"

echo "DRUGS - add data into ancestor_count table"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\`
    (ANCESTOR_ID, DESCENDANT_ID)
select distinct a.ID as ANCESTOR_ID,
coalesce(e.ID, d.ID, c.ID, b.ID) as DESCENDANT_ID
from (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'ATC') a
    join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'ATC') b on a.ID = b.PARENT_ID
    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'ATC') c on b.ID = c.PARENT_ID
    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'ATC') d on c.ID = d.PARENT_ID
    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'ATC') e on d.ID = e.PARENT_ID
where a.IS_GROUP = 1"

echo "DRUGS - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (
select c.id, count(distinct person_id) cnt
  from
    (select * from
      (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
        where type = 'DRUG' and subtype = 'ATC' and is_group = 1 and is_selectable = 1) a
    left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
    left join
      (select a.id, b.*
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
        (
          select b.ANCESTOR_CONCEPT_ID as concept_id, a.person_id
          from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\` a
            join (select *
              from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` x
              left join \`$BQ_PROJECT.$BQ_DATASET.concept\` y on x.ANCESTOR_CONCEPT_ID = y.CONCEPT_ID
              where VOCABULARY_ID = 'RxNorm' and CONCEPT_CLASS_ID = 'Ingredient') b on a.DRUG_CONCEPT_ID = b.DESCENDANT_CONCEPT_ID
        ) b
      where a.concept_id = b.concept_id) d on c.descendant_id = d.id
  group by 1) y
where x.id = y.id"

################################################
# MEASUREMENTS
################################################
#----- roots -----
echo "MEASUREMENTS - add loinc root"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID,0,'MEAS','LOINC','LOINC',1,0,0"

echo "MEASUREMENTS - add clinical root"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute, path)
SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 as ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LOINC') as parent_id,'MEAS','CLIN','Clinical',1,0,0,
(SELECT CAST(ID AS STRING) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LOINC')"

echo "MEASUREMENTS - add lab root"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,has_attribute, path)
SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 as ID,
(SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LOINC') as parent_id,'MEAS','LAB','LP29693-6','Lab',1,0,0,
(SELECT CAST(ID AS STRING) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LOINC')"

#----- clinical -----
echo "MEASUREMENTS - clinical - add parents"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,name,is_group,is_selectable,has_attribute, path)
SELECT ROW_NUMBER() OVER(order by name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as ID,
    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'CLIN') as parent_id, 'MEAS', 'CLIN', name, 1, 0, 0,
    CONCAT((SELECT PATH FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'CLIN'), '.',
    (SELECT CAST(ID AS STRING) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'CLIN'))
from
    (select distinct parent as name
    FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_terms_nc\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.LOINC = b.concept_code
    WHERE b.concept_id in (select distinct measurement_concept_id from \`$BQ_PROJECT.$BQ_DATASET.measurement\`)
    ) a"

echo "MEASUREMENTS - clinical - add children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,concept_id,has_attribute, path)
select ROW_NUMBER() OVER(order by parent_name, concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as ID, parent_id,
    'MEAS', 'CLIN', concept_code, concept_name, 0, 1, est_count, concept_id, 1, CONCAT(parent_path, '.', CAST(parent_id AS STRING))
from (select z.*, y.id as parent_id, y.name as parent_name, y.path as parent_path
    from \`$BQ_PROJECT.$BQ_DATASET.criteria_terms_nc\` x
    join (SELECT id,name,path FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`
      WHERE type = 'MEAS' and subtype = 'CLIN' and is_group = 1) y on x.parent = y.name
    join (select concept_name, concept_id, concept_code, count(distinct person_id) est_count
          from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
            left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
          where standard_concept = 'S'
            and domain_id = 'Measurement'
            and vocabulary_id = 'LOINC'
          group by 1,2,3) z on x.loinc = z.concept_code) g"

#----- laboratory -----
echo "MEASUREMENTS - labs - temp table 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, concept_id, concept_code, concept_name)
select *
from \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` a
where concept_id in
  (select distinct MEASUREMENT_CONCEPT_ID
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.MEASUREMENT_CONCEPT_ID = b.concept_id
  where MEASUREMENT_CONCEPT_ID != 0
    and b.vocabulary_id = 'LOINC'
    and b.STANDARD_CONCEPT = 'S'
    and b.domain_id = 'Measurement')"

for i in {1..5};
do
    echo "MEASUREMENTS - labs - temp table $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` (p_concept_id, p_concept_code, p_concept_name, concept_id, concept_code, concept_name)
    select *
    from \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` a
    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)
    and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)"
done

echo "MEASUREMENTS - labs - add level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, CONCEPT_ID, HAS_ATTRIBUTE, PATH)
select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'MEAS', 'LAB',
    b.CONCEPT_CODE, b.CONCEPT_NAME, 1,0, b.CONCEPT_ID, 0,
    CONCAT( (SELECT PATH FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LAB'), '.',
    (SELECT CAST(ID as STRING) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` WHERE type = 'MEAS' and subtype = 'LAB') )
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
    join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)
    and b.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)
    and t.type = 'MEAS' and t.subtype = 'LAB'"

for i in {1..12};
do
    echo "MEASUREMENTS - labs - add level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
      (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, est_count, CONCEPT_ID, has_attribute, path)
    select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`),
        t.ID, 'MEAS', 'LAB', b.CONCEPT_CODE, b.CONCEPT_NAME,
        case when l.CONCEPT_CODE is null then 1 else 0 end as is_group,
        case when l.CONCEPT_CODE is null then 0 else 1 end as is_selectable,
        case when l.CONCEPT_CODE is null then null else m.cnt end as est_count,
        b.CONCEPT_ID,
        case when l.CONCEPT_CODE is null then 0 else 1 end as has_attribute,
        CONCAT(t.path, '.', CAST(t.ID as STRING))
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
           join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on t.code = b.p_concept_code
           left join (select distinct a.CONCEPT_CODE
                        from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` a
                             left join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on a.CONCEPT_ID = b.P_CONCEPT_ID
                       where b.CONCEPT_ID is null) l on b.CONCEPT_CODE = l.CONCEPT_CODE
         left join (select measurement_concept_id, count(distinct person_id) cnt
            from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
            group by 1) m on b.concept_id = m.measurement_concept_id
     where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB')
       and parent_id != 0"
done

echo "MEASUREMENTS - add parent for uncategorized labs"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (ID, PARENT_ID, TYPE, SUBTYPE, NAME, IS_GROUP, IS_SELECTABLE,domain_id, HAS_ATTRIBUTE, PATH)
SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 as ID,
  a.id,'MEAS','LAB','Uncategorized',1,0,'Measurement',0,
  CONCAT(a.path, '.', CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS STRING))
FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\` a
WHERE type = 'LOINC' and subtype = 'LAB' and parent_id = 0"

echo "MEASUREMENTS - add uncategorized labs"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, est_count, domain_id, CONCEPT_ID, has_attribute, path)
select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as ID,
  (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as parent_id,
  'MEAS','LAB',concept_code,concept_name,0,1,est_count,'Measurement',concept_id,1,
  CONCAT( (select path from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'LOINC' and subtype = 'LAB' and name = 'Uncategorized'), '.',
    CAST(ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS STRING) )
from
  (select concept_id, concept_code, concept_name, count(distinct person_id) est_count
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
  where standard_concept = 'S'
    and domain_id = 'Measurement'
    and vocabulary_id = 'LOINC'
    and measurement_concept_id not in
      (select concept_id
        from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
        where type = 'LOINC'
          and concept_id is not null)
  group by 1,2,3) x"

echo "MEASUREMENTS - labs - add data into ancestor table"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` (ANCESTOR_ID, DESCENDANT_ID)
select distinct a.ID ANCESTOR_ID,
       coalesce(n.ID, m.ID, k.ID, j.ID, i.ID, h.ID, g.ID, f.ID, e.ID, d.ID, c.ID, b.ID) DESCENDANT_ID
  from (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') a
     join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') b on a.ID = b.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') c on b.ID = c.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') d on c.ID = d.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') e on d.ID = e.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') f on e.ID = f.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') g on f.ID = g.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') h on g.ID = h.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') i on h.ID = i.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') j on i.ID = j.PARENT_ID
	   left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') k on j.ID = k.PARENT_ID
       left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') m on k.ID = m.PARENT_ID
       left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and subtype = 'LAB') n on m.ID = n.PARENT_ID
 where a.IS_SELECTABLE = 0"

echo "MEASUREMENTS - labs - generate parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from (
select c.id, count(distinct person_id) cnt
  from
    (select * from
      (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
        where type = 'MEAS' and subtype = 'LAB' and is_group = 1 and concept_id is not null) a
    left join \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` b on a.id = b.ancestor_id) c
    left join
      (select a.id, b.*
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a,
        (
          SELECT person_id, concept_id
          FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a,
            (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
              where type = 'MEAS' and subtype = 'LAB' and is_group = 0 and is_selectable = 1) b
          WHERE a.measurement_concept_id = b.concept_id
        ) b
      where a.concept_id = b.concept_id) d on c.descendant_id = d.id
  group by 1) y
where x.id = y.id"

# the first item in the measurement tree needs to have 'null' as a code so the logic works correctly
echo "MEASUREMENTS - labs - clean up"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` SET code = null where type = 'MEAS' and subtype = 'LAB' and code = 'LP29693-6'"

################################################
# SNOMED
################################################
echo "SNOMED - CM - temp table level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select *
from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
where concept_id in
  (select distinct CONDITION_CONCEPT_ID
  from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.CONDITION_CONCEPT_ID = b.concept_id
  where CONDITION_CONCEPT_ID != 0
    and b.vocabulary_id = 'SNOMED'
    and b.STANDARD_CONCEPT = 'S'
    and b.domain_id = 'Condition')"

for i in {1..6};
do
    echo "SNOMED - CM - temp table level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
    select *
    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)
      and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)"
done

echo "SNOMED - CM - add root"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute)
select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1 AS ID, 0, 'SNOMED', 'CM', CONCEPT_CODE, CONCEPT_NAME, 1, 0, 'Condition', concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\`
where concept_code = '404684003'"

echo "SNOMED - CM - add level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'SNOMED', 'CM',
b.CONCEPT_CODE, b.CONCEPT_NAME, 1,0, 'Condition', b.CONCEPT_ID, 0, CAST(t.id as STRING) as path
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
	   join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` b on t.code = b.p_concept_code
 where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)
   and b.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)"

for i in {1..18};
do
    echo "SNOMED - CM - add level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
    select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`),
      t.ID, 'SNOMED', 'CM', b.CONCEPT_CODE, b.CONCEPT_NAME,
      case when l.CONCEPT_CODE is null then 1 else 0 end,
      case when l.CONCEPT_CODE is null then 0 else 1 end,
      'Condition',b.CONCEPT_ID, 0, CONCAT(t.path, '.', CAST(t.ID as STRING))
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
       join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` b on t.code = b.p_concept_code
       left join (select distinct a.CONCEPT_CODE
              from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` a
                   left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` b on a.CONCEPT_ID = b.P_CONCEPT_ID
             where b.CONCEPT_ID is null) l on b.CONCEPT_CODE = l.CONCEPT_CODE
    where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)"
done

echo "SNOMED - CM - add parents as children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
select (row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)) as ID, *
from
  (select distinct a.ID as PARENT_ID, a.TYPE, a.SUBTYPE, a.CODE, a.NAME, 0, 1, 'Condition', a.CONCEPT_ID, 0, CONCAT(a.path, '.', CAST(a.ID as STRING))
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a
  where a.IS_GROUP = 1
    and CONCEPT_ID in
      (select distinct CONDITION_CONCEPT_ID
      from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
      left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.CONDITION_CONCEPT_ID = b.concept_id
      where CONDITION_CONCEPT_ID != 0
        and b.vocabulary_id = 'SNOMED'
        and b.STANDARD_CONCEPT = 'S'
        and b.domain_id = 'Condition') ) a"

echo "SNOMED - PCS - temp table level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select *
from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\` a
where concept_id in
  (select distinct procedure_CONCEPT_ID
  from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_CONCEPT_ID = b.concept_id
  where procedure_CONCEPT_ID != 0
    and b.vocabulary_id = 'SNOMED'
    and b.STANDARD_CONCEPT = 'S'
    and b.domain_id = 'Procedure')"

for i in {1..9};
do
    echo "SNOMED - PCS - temp table level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
    select *
    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\` a
    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`)
      and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`)"
done

echo "SNOMED - PCS - add root"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute)
select (select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)+1, 0, 'SNOMED', 'PCS', CONCEPT_CODE, CONCEPT_NAME, 1, 0, 'Procedure', concept_id, 0
from \`$BQ_PROJECT.$BQ_DATASET.concept\`
where concept_code = '71388002'"

echo "SNOMED - PCS - add level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'SNOMED', 'PCS',
b.CONCEPT_CODE, b.CONCEPT_NAME, 1,0, 'Procedure', b.CONCEPT_ID, 0, CAST(t.id as STRING) as path
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
	   join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` b on t.code = b.p_concept_code
 where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)
   and b.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`)
   and t.type = 'SNOMED' and t.subtype = 'PCS'"

for i in {1..12};
do
    echo "SNOMED - PCS - add level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
    select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`), t.ID, 'SNOMED', 'PCS',
      b.CONCEPT_CODE, b.CONCEPT_NAME,
      case when l.CONCEPT_CODE is null then 1 else 0 end,
      case when l.CONCEPT_CODE is null then 0 else 1 end,
      'Procedure', b.CONCEPT_ID, 0, CONCAT(t.path, '.', CAST(t.ID as STRING))
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
      join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` b on t.code = b.p_concept_code
      left join (select distinct a.CONCEPT_CODE
          from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` a
          left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\` b on a.CONCEPT_ID = b.P_CONCEPT_ID
          where b.CONCEPT_ID is null) l on b.CONCEPT_CODE = l.CONCEPT_CODE
    where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)"
done

echo "SNOMED - PCS - add parents as children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\` (ID, PARENT_ID, TYPE, SUBTYPE, CODE, NAME, IS_GROUP, IS_SELECTABLE, domain_id, CONCEPT_ID, has_attribute, path)
select (row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)) as ID, *
from
  (select distinct a.ID as PARENT_ID,a.TYPE, a.SUBTYPE, a.CODE, a.NAME, 0, 1, 'Procedure', a.CONCEPT_ID, 0, CONCAT(a.path, '.', CAST(a.ID as STRING))
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a
  where a.IS_GROUP = 1
  and CONCEPT_ID in
    (select distinct procedure_concept_id
    from \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` a
      left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.procedure_concept_id = b.concept_id
    where procedure_CONCEPT_ID != 0
      and b.vocabulary_id = 'SNOMED'
      and b.STANDARD_CONCEPT = 'S'
      and b.domain_id = 'Procedure') ) a"

echo "SNOMED - CM - child counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from
  (SELECT condition_concept_id as concept_id, count(distinct person_id) cnt
  FROM \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
  GROUP BY 1) y
where x.concept_id = y.concept_id
and x.type = 'SNOMED'
and x.subtype = 'CM'
and x.is_selectable = 1"

echo "SNOMED - CM - parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from
  (select ancestor_concept_id as concept_id, count(distinct person_id) cnt
  from
  (select *
  from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
  where ancestor_concept_id in
    (select distinct concept_id
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
    where type = 'SNOMED'
    and subtype = 'CM'
    and is_group = 1)) a
  join \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` b on a.descendant_concept_id = b.condition_concept_id
  group by 1) y
where x.type = 'SNOMED'
and x.subtype = 'CM'
and is_group = 1
and x.concept_id = y.concept_id"

echo "SNOMED - PCS - child counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
set x.est_count = y.cnt
from
  (SELECT procedure_concept_id as concept_id, count(distinct person_id) cnt
  FROM \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\`
  GROUP BY 1) y
where x.concept_id = y.concept_id
and x.type = 'SNOMED'
and x.subtype = 'PCS'
and x.is_selectable = 1"

echo "SNOMED - PCS - parent counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from
  (select ancestor_concept_id as concept_id, count(distinct person_id) cnt
  from
  (select *
  from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
  where ancestor_concept_id in
    (select distinct concept_id
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
    where type = 'SNOMED'
    and subtype = 'PCS'
    and is_group = 1)) a
  join \`$BQ_PROJECT.$BQ_DATASET.procedure_occurrence\` b on a.descendant_concept_id = b.procedure_concept_id
  group by 1) y
where x.type = 'SNOMED'
and x.subtype = 'PCS'
and is_group = 1
and x.concept_id = y.concept_id"


echo "MEASUREMENTS - SNOMED - temp table level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
  (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
select *
from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` a
where concept_id in
  (select distinct measurement_concept_id
  from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
  left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
  where measurement_concept_id != 0
    and b.vocabulary_id = 'SNOMED'
    and b.STANDARD_CONCEPT = 'S'
    and b.domain_id = 'Measurement')"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 3 levels, but we run it 4 times to be safe
for i in {1..4};
do
    echo "MEASUREMENTS - SNOMED - temp table level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
      (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
    select *
    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` a
    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)
      and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)"
done

echo "MEASUREMENTS - SNOMED - add roots"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (id,parent_id,type,subtype,code,name,is_group,is_selectable,domain_id,concept_id,has_attribute,path)
select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as ID,
  0,'SNOMED','MEAS',concept_code,concept_name,1,0,'Measurement',concept_id,1,
  CAST(ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as STRING) as path
from
  (select distinct concept_id, concept_name, concept_code
  from
    (select *, rank() over (partition by descendant_concept_id order by MAX_LEVELS_OF_SEPARATION desc) rnk
    from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` a
    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.ANCESTOR_CONCEPT_ID = b.concept_id
    where descendant_concept_id in
      	(select distinct concept_id
      	from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
      	left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
      	where standard_concept = 'S'
      	and domain_id = 'Measurement'
      	and vocabulary_id = 'SNOMED')
    and domain_id = 'Measurement') a
  where rnk = 1) x"

echo "MEASUREMENTS - SNOMED - add level 0"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (id,parent_id,type,subtype,code,name,is_group,is_selectable,domain_id,concept_id,has_attribute,path)
select row_number() over (order by t.id, b.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`),
  t.id,'SNOMED','MEAS',b.concept_code,b.concept_name,1,0,'Measurement',b.concept_id,1,
  CONCAT(t.path, '.',
    CAST(row_number() over (order by t.id, b.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`) AS STRING))
from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
  join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` b on t.code = b.p_concept_code
where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)
  and b.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)
  and t.type = 'SNOMED'
  and t.subtype = 'MEAS'"

# for each loop, add all items (children/parents) directly under the items that were previously added
# currently, there are only 6 levels, but we run it 7 times to be safe
for i in {1..7};
do
    echo "MEASUREMENTS - SNOMED - add level $i"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
      (id,parent_id,type,subtype,code,name,is_group,is_selectable,domain_id,concept_id,has_attribute,path)
    select row_number() over (order by t.id, b.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`),
      t.id,'SNOMED','MEAS',b.concept_code,b.concept_name,
      case when l.concept_code is null then 1 else 0 end,
      case when l.concept_code is null then 0 else 1 end,
      'Measurement',b.concept_id,1,
      CONCAT(t.path, '.',
        CAST(row_number() over (order by t.id, b.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as STRING))
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\` t
      join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` b on t.code = b.p_concept_code
      left join (select distinct a.CONCEPT_CODE
          from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` a
          left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` b on a.CONCEPT_ID = b.P_CONCEPT_ID
          where b.CONCEPT_ID is null) l on b.CONCEPT_CODE = l.CONCEPT_CODE
    where (id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)"
done

echo "MEASUREMENTS - SNOMED - generate counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` x
SET x.est_count = y.cnt
from
  (select ancestor_concept_id as concept_id, count(distinct person_id) cnt
  from
    (select *
    from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
    where ancestor_concept_id in
      (select distinct concept_id
      from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
      where type = 'SNOMED'
        and subtype = 'MEAS')) a
    join \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
  group by 1) y
where x.type = 'SNOMED'
  and x.subtype = 'MEAS'
  and x.concept_id = y.concept_id"

echo "MEASUREMENTS - SNOMED - add parents as children"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria\`
  (id,parent_id,type,subtype,code,name,is_group,is_selectable,est_count,domain_id,concept_id,has_attribute,path)
select (row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`)) as ID,
id as parent_id,type,subtype,code,name,cnt,0,1,domain_id,concept_id,1,CONCAT(path, '.',
  CAST(row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria\`) as STRING))
from
  (select *
  from \`$BQ_PROJECT.$BQ_DATASET.criteria\` a
    join (select measurement_concept_id, count(distinct person_id) cnt
        from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
        group by 1) b on a.concept_id = b.measurement_concept_id
  where type = 'SNOMED'
    and subtype = 'MEAS'
    and is_group = 1) x"


################################################
# CLEAN UP
################################################
# TODO: remove this as it is no longer needed
#echo "CLEAN UP - remove items that no longer exist from criteria_ancestor_count table"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"delete
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\`
#where DESCENDANT_ID in
#(select distinct DESCENDANT_ID
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\` a
#left join \`$BQ_PROJECT.$BQ_DATASET.criteria\` b on a.ANCESTOR_ID = b.ID
#left join \`$BQ_PROJECT.$BQ_DATASET.criteria\` c on a.DESCENDANT_ID = c.ID
#where b.ID is null or c.id is null)"

echo "CLEAN UP - set est_count = -1 where the count is currently NULL"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria\` set est_count = -1 where est_count is null"


################################################
# CRITERIA ANCESTOR
################################################
echo "CRITERIA_ANCESTOR - Drugs - add ingredients to drugs mapping"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor\` (ancestor_id, descendant_id)
select ancestor_concept_id, descendant_concept_id
from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
where ancestor_concept_id in
    (select distinct concept_id
    from \`$BQ_PROJECT.$BQ_DATASET.criteria\`
    where type = 'DRUG'
        and subtype = 'ATC'
        and is_group = 0
        and is_selectable = 1)
and descendant_concept_id in (select distinct drug_concept_id from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`)"


################################################
# CRITERIA ATTRIBUTES
################################################
echo "CRITERIA_ATTRIBUTES - Measurements - add numeric results"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute\` (id, concept_id, value_as_concept_Id, concept_name, type, est_count)
select ROW_NUMBER() OVER(order by measurement_concept_id), *
from
  (select measurement_concept_id, 0, 'MIN', 'NUM', CAST(min(VALUE_AS_NUMBER) as STRING)
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
    where measurement_concept_id in (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and is_selectable = 1)
    and VALUE_AS_NUMBER is not null
    group by 1
  UNION ALL
    select measurement_concept_id, 0, 'MAX', 'NUM', CAST(max(VALUE_AS_NUMBER) as STRING)
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
    where measurement_concept_id in (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and is_selectable = 1)
    and VALUE_AS_NUMBER is not null
    group by 1
  ) a"

echo "CRITERIA_ATTRIBUTES - Measurements - add categorical results"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute\` (id, concept_id, value_as_concept_Id, concept_name, type, est_count)
select ROW_NUMBER() OVER(order by measurement_concept_id) + (select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute\`), *
from
    (select measurement_concept_id, value_as_concept_id, b.concept_name, 'CAT' as type, CAST(count(*) as STRING) as est_count
    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_as_concept_Id = b.concept_id
    where value_as_concept_id != 0
        and value_as_concept_id is not null
        and measurement_concept_id in (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'MEAS' and is_selectable = 1)
    group by 1,2,3
    ) a"


################################################
# CRITERIA RELATIONSHIP
################################################
echo "CRITERIA_RELATIONSHIP - Drugs - add drug/ingredient relationships"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_relationship\` ( concept_id_1, concept_id_2 )
select cr.concept_id_1, cr.concept_id_2
from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr
join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on cr.concept_id_2 = c1.concept_id
where cr.concept_id_1 in (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria\` where type = 'DRUG' and subtype = 'BRAND')
and c1.concept_class_id = 'Ingredient'"


################################################
# DROPPED PREP TABLES AND VIEWS
################################################
echo "DROP - criteria_seed"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.criteria_seed\`"

echo "DROP - criteria_ancestor_count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_count\`"

echo "DROP - criteria_terms_nc"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.criteria_terms_nc\`"

echo "DROP - atc_rel_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`"

echo "DROP - loinc_rel_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`"

echo "DROP - snomed_rel_cm_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`"

echo "DROP - snomed_rel_pcs_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`"

echo "DROP - snomed_rel_meas_in_data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`"

echo "DROP - v_loinc_rel"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\`"

echo "DROP - v_snomed_rel_cm"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\`"

echo "DROP - v_snomed_rel_pcs"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\`"

echo "DROP - v_snomed_rel_meas"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\`"
