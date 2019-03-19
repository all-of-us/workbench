#!/bin/bash

# This generates the criteria tables for the CDR

# PREP: upload all prep tables

# Example usage:
# ./project.rb generate-criteria-table-new --bq-project aou-res-curation-prod --bq-dataset deid_output_20181116
# ./project.rb generate-criteria-table-new --bq-project all-of-us-ehr-dev --bq-dataset synthetic_cdr20180606


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


#################################################
## CREATE TABLES
#################################################
#echo "CREATE TABLES - criteria_test"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#(
#    id                  INT64,
#    parent_id           INT64,
#    domain_id           STRING,
#    is_standard         INT64,
#    type                STRING,
#    subtype             STRING,
#    concept_id          INT64,
#    code                STRING,
#    name                STRING,
#    value               INT64,
#    est_count           INT64,
#    is_group            INT64,
#    is_selectable       INT64,
#    has_attribute       INT64,
#    has_hierarchy       INT64,
#    path                STRING,
#    synonyms            STRING
#)"
#
## # table that holds the ingredient --> coded drugs mapping
## echo "CREATE TABLES - criteria_ancestor_test"
## bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
## "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor_test\`
## (
##   ancestor_concept_id INT64,
##   descendant_concept_id INT64
## )"
#
## table that holds categorical results and min/max information about individual labs
#echo "CREATE TABLES - criteria_attribute_test"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute_test\`
#(
#    id                    INT64,
#    concept_id            INT64,
#    value_as_concept_id	  INT64,
#    concept_name          STRING,
#    type                  STRING,
#    est_count             STRING
#)"
#
## # table that holds the drug brands -> ingredients relationship mapping
## echo "CREATE TABLES - criteria_relationship_test"
## bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
## "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.criteria_relationship_test\`
## (
##   concept_id_1 INT64,
##   concept_id_2 INT64
## )"
#
#echo "CREATE TABLES - atc_rel_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#echo "CREATE TABLES - loinc_rel_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#echo "CREATE TABLES - snomed_rel_cm_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#echo "CREATE TABLES - snomed_rel_cm_src_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#echo "CREATE TABLES - snomed_rel_pcs_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#echo "CREATE TABLES - snomed_rel_meas_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
#(
#    p_concept_id    INT64,
#    p_concept_code  STRING,
#    p_concept_name  STRING,
#    p_domain_id     STRING,
#    concept_id      INT64,
#    concept_code    STRING,
#    concept_name    STRING,
#    domain_id       STRING
#)"
#
#################################################
## CREATE VIEWS
#################################################
#echo "CREATE VIEWS - v_loinc_rel"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` AS
#SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE,
#    C1.CONCEPT_NAME AS P_CONCEPT_NAME, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME
#FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
#    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
#WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
#    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
#    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
#    AND CR.RELATIONSHIP_ID = 'Subsumes'
#    AND R.IS_HIERARCHICAL = '1'
#    AND R.DEFINES_ANCESTRY = '1'
#    AND C1.VOCABULARY_ID = 'LOINC'
#    AND C2.VOCABULARY_ID = 'LOINC'
#    AND C1.STANDARD_CONCEPT IN ('S','C')
#    AND C2.STANDARD_CONCEPT IN ('S','C')
#    AND C1.CONCEPT_CLASS_ID IN ('LOINC Hierarchy', 'Lab Test')
#    AND C2.CONCEPT_CLASS_ID IN ('LOINC Hierarchy', 'Lab Test')"
#
#echo "CREATE VIEWS - v_snomed_rel_cm"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` AS
#SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
#    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
#FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
#    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
#WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
#    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
#    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
#    AND C1.VOCABULARY_ID = 'SNOMED'
#    AND C2.VOCABULARY_ID = 'SNOMED'
#    AND C1.STANDARD_CONCEPT = 'S'
#    AND C2.STANDARD_CONCEPT = 'S'
#    AND R.IS_HIERARCHICAL = '1'
#    AND R.DEFINES_ANCESTRY = '1'
#    AND C1.DOMAIN_ID = 'Condition'
#    AND C2.DOMAIN_ID = 'Condition'
#    AND CR.RELATIONSHIP_ID = 'Subsumes'"
#
#echo "CREATE VIEWS - v_snomed_rel_cm_src"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\` AS
#SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
#    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
#FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
#    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
#WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
#    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
#    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
#    AND C1.VOCABULARY_ID = 'SNOMED'
#    AND C2.VOCABULARY_ID = 'SNOMED'
#    AND R.IS_HIERARCHICAL = '1'
#    AND R.DEFINES_ANCESTRY = '1'
#    AND C1.DOMAIN_ID = 'Condition'
#    AND C2.DOMAIN_ID = 'Condition'
#    AND CR.RELATIONSHIP_ID = 'Subsumes'"
#
#echo "CREATE VIEWS - v_snomed_rel_pcs"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\` AS
#SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
#    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
#FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
#    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
#WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
#    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
#    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
#    AND C1.VOCABULARY_ID = 'SNOMED'
#    AND C2.VOCABULARY_ID = 'SNOMED'
#    AND C1.STANDARD_CONCEPT = 'S'
#    AND C2.STANDARD_CONCEPT = 'S'
#    AND R.IS_HIERARCHICAL = '1'
#    AND R.DEFINES_ANCESTRY = '1'
#    AND C1.DOMAIN_ID = 'Procedure'
#    AND C2.DOMAIN_ID = 'Procedure'
#    AND CR.RELATIONSHIP_ID = 'Subsumes'"
#
#echo "CREATE VIEWS - v_snomed_rel_meas"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"CREATE OR REPLACE VIEW \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` AS
#SELECT DISTINCT C1.CONCEPT_ID AS P_CONCEPT_ID, C1.CONCEPT_CODE AS P_CONCEPT_CODE, C1.CONCEPT_NAME AS P_CONCEPT_NAME,
#    C1.DOMAIN_ID AS P_DOMAIN_ID, C2.CONCEPT_ID, C2.CONCEPT_CODE, C2.CONCEPT_NAME, C2.DOMAIN_ID
#FROM \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` CR,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C1,
#    \`$BQ_PROJECT.$BQ_DATASET.concept\` C2,
#    \`$BQ_PROJECT.$BQ_DATASET.relationship\` R
#WHERE CR.CONCEPT_ID_1 = C1.CONCEPT_ID
#    AND CR.CONCEPT_ID_2 = C2.CONCEPT_ID
#    AND CR.RELATIONSHIP_ID = R.RELATIONSHIP_ID
#    AND C1.VOCABULARY_ID = 'SNOMED'
#    AND C2.VOCABULARY_ID = 'SNOMED'
#    AND C1.STANDARD_CONCEPT = 'S'
#    AND C2.STANDARD_CONCEPT = 'S'
#    AND R.IS_HIERARCHICAL = '1'
#    AND R.DEFINES_ANCESTRY = '1'
#    AND C1.DOMAIN_ID = 'Measurement'
#    AND C2.DOMAIN_ID = 'Measurement'
#    AND CR.RELATIONSHIP_ID = 'Subsumes'"
#
#
#################################################
## SOURCE ICD9
#################################################
#echo "ICD9 - add data (do not insert zero count children)"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select id, parent_id, a.domain_id, is_standard, type, subtype, a.concept_id, code, name,
#    case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
#    is_group, is_selectable, has_attribute, has_hierarchy, path
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
#left join
#	(
#		select *
#		from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#		where (vocabulary_id in ('ICD9CM', 'ICD9Proc') and concept_code != '92')
#			or (vocabulary_id = 'ICD9Proc' and concept_code = '92')
#	) b on a.CODE = b.CONCEPT_CODE
#left join
#	(
#		select concept_id, count(distinct person_id) cnt
#		from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#		where is_standard = 0
#			and concept_id in
#				(
#					select concept_id
#				    from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#				    where type = 'ICD9'
#			            and is_group = 0
#			            and is_selectable = 1
#				)
#		group by 1
#	) c on b.concept_id = c.concept_id
#where type = 'ICD9'
#    and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
#order by 1"
#
#echo "ICD9 - generate parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#	(
#		select e.id, count(distinct person_id) cnt
#		from
#			(
#				select *
#				from (select id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'ICD9' and is_group = 1 and is_selectable = 1) a
#				left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#			) e
#		left join
#    	  	(
#				select c.id, d.*
#				from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#				join
#					(
#						select a.person_id, a.concept_id
#						from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\` a
#						join
#							(
#    							select concept_id, path
#    					    	from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    					    	where type = 'ICD9'
#                                    and is_group = 0
#                                    and is_selectable = 1
#							) b on a.concept_id = b.concept_id
#						where is_standard = 0
#					) d on c.concept_id = d.concept_id
#    	  	) f on e.descendant_id = f.id
#		group by 1
#	) y
#where x.id = y.id"
#
#echo "ICD9 - delete zero count parents"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"delete
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#where type = 'ICD9'
#    and is_selectable = 1
#    and (est_count is null or est_count = 0)"
#
#################################################
## SOURCE ICD10
#################################################
#echo "ICD10 - CM - insert data (do not insert zero count children)"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select id, parent_id, a.domain_id, is_standard, type, subtype, a.concept_id, code, name,
#    case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
#    is_group, is_selectable, has_attribute, has_hierarchy, path
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
#left join
#	(
#		select *
#		from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#		where vocabulary_id in ('ICD10CM')
#	) b on a.CODE = b.CONCEPT_CODE
#left join
#	(
#		select concept_id, count(distinct person_id) cnt
#		from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#		where is_standard = 0
#			and concept_id in
#				(
#					select concept_id
#				    from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#				    where type = 'ICD10'
#						and subtype = 'CM'
#			            and is_group = 0
#			            and is_selectable = 1
#				)
#		group by 1
#	) c on b.concept_id = c.concept_id
#where type = 'ICD10'
#	and subtype = 'CM'
#	and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
#order by 1"
#
#echo "ICD10 - CM - generate parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#	(
#		select e.id, count(distinct person_id) cnt
#		from
#			(
#				select *
#				from
#                    (
#                        select id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                            where type = 'ICD10'
#                            and subtype = 'CM'
#                            and parent_id != 0
#                            and is_group = 1
#                    ) a
#				left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#			) e
#		left join
#            (
#                select c.id, d.*
#			    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#			    join
#                    (
#    					select a.person_id, a.concept_id
#    					from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\` a
#    					join
#    						(
#    							select concept_id, path
#    					    	from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    					    	where type = 'ICD10'
#    								and subtype = 'CM'
#    								and is_group = 0
#    								and is_selectable = 1
#    						) b on a.concept_id = b.concept_id
#                        where is_standard = 0
#                    ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#		group by 1
#	) y
#where x.id = y.id"
#
#echo "ICD10 - PCS - insert data (do not insert zero count children)"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select id, parent_id, a.domain_id, is_standard, type, subtype, a.concept_id, code, name,
#    case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
#    is_group, is_selectable, has_attribute, has_hierarchy, path
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
#left join
#	(
#		select *
#		from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#		where vocabulary_id in ('ICD10PCS')
#	) b on a.CODE = b.CONCEPT_CODE
#left join
#	(
#		select concept_id, count(distinct person_id) cnt
#		from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#		where is_standard = 0
#			and concept_id in
#				(
#					select concept_id
#				    from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#				    where type = 'ICD10'
#						and subtype = 'PCS'
#			            and is_group = 0
#			            and is_selectable = 1
#				)
#		group by 1
#	) c on b.concept_id = c.concept_id
#where type = 'ICD10'
#	and subtype = 'PCS'
#	and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
#order by 1"
#
#echo "ICD10 - PCS - generate parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select e.id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from
#                    (
#                        select id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where type = 'ICD10'
#                            and subtype = 'PCS'
#                            and parent_id != 0
#                            and is_group = 1
#                    ) a
#                left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#            ) e
#        left join
#            (
#                select c.id, d.*
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#                join
#                    (
#                        select a.person_id, a.concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\` a
#                        join
#                            (
#                                select concept_id, path
#                                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                                where type = 'ICD10'
#                                    and subtype = 'PCS'
#                                    and is_group = 0
#                                    and is_selectable = 1
#                            ) b on a.concept_id = b.concept_id
#                        where is_standard = 0
#                    ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#        group by 1
#    ) y
#where x.id = y.id"
#
#echo "ICD10 - delete zero count parents"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"delete
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#where type = 'ICD10'
#    and is_selectable = 1
#    and (est_count is null or est_count = 0)"
#
#
#################################################
## SOURCE CPT
#################################################
#echo "CPT - insert data (do not insert zero count children)"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select id, parent_id, a.domain_id, is_standard, type, subtype, a.concept_id, code, name,
#    case when is_group = 0 and is_selectable = 1 then c.cnt else null end as est_count,
#    is_group, is_selectable, has_attribute, has_hierarchy, path
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\` a
#left join
#	(
#		select *
#		from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#		where vocabulary_id in ('CPT4')
#	) b on a.CODE = b.CONCEPT_CODE
#left join
#	(
#		select concept_id, count(distinct person_id) cnt
#		from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#		where is_standard = 0
#			and concept_id in
#				(
#					select concept_id
#				    from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#				    where type = 'CPT'
#			            and is_group = 0
#			            and is_selectable = 1
#				)
#		group by 1
#	) c on b.concept_id = c.concept_id
#where type = 'CPT'
#	and (is_group = 1 or (is_group = 0 and is_selectable = 1 and (c.cnt != 0 or c.cnt is not null)))
#order by 1"
#
#echo "CPT - generate parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select e.id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from
#                    (
#                        select id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where type = 'CPT'
#                            and parent_id != 0
#                            and is_group = 1
#                    ) a
#                left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#            ) e
#        left join
#            (
#                select c.id, d.*
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#                join
#                    (
#                        select a.person_id, a.concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\` a
#                        join
#                            (
#                                select concept_id, path
#                                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                                where type = 'CPT'
#                                    and is_group = 0
#                                    and is_selectable = 1
#                            ) b on a.concept_id = b.concept_id
#                        where is_standard = 0
#                    ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#        group by 1
#    ) y
#where x.id = y.id"
#
#echo "CPT - delete zero count parents"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"delete
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#where type = 'CPT'
#    and
#		(
#			(parent_id != 0 and (est_count is null or est_count = 0))
#			or
#				(
#					is_group = 1
#					and id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'CPT' and est_count is not null)
#				)
#		)"
#
#
#################################################
## PPI SURVEYS
#################################################
#echo "PPI SURVEYS - insert data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,value,
#    case when is_selectable = 1 then 0 else null end as est_count,
#    is_group,is_selectable,has_attribute,has_hierarchy,path
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#where type = 'PPI'
#order by 1"
#
#echo "PPI SURVEYS - generate answer counts for all questions EXCEPT 1585747"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        select concept_id, value_as_concept_id as value, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#        where is_standard = 0
#            and concept_id in
#                (
#                    select concept_id
#                    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                    where type = 'PPI'
#                        and is_group = 0
#                        and is_selectable = 1
#                        and concept_id != 1585747
#                )
#        group by 1,2
#    ) y
#where x.type = 'PPI'
#    and x.concept_id = y.concept_id
#    and x.value = y.value"
#
#echo "PPI SURVEYS - generate answer counts for 1585747"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        select concept_id, cast(value_as_number as INT64) as value, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#        where is_standard = 0
#            and concept_id = 1585747
#        group by 1,2
#    ) y
#where x.type = 'PPI'
#    and x.is_group = 0
#    and x.concept_id = y.concept_id
#    and x.value = y.value"
#
#echo "PPI SURVEYS - generate question counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        SELECT concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\`
#        where is_standard = 0
#            and concept_id in
#                (
#                    select concept_id
#                    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                    where type = 'PPI'
#                        and is_group = 1
#                        and is_selectable = 1
#                )
#        group by 1
#    ) y
#where x.type = 'PPI'
#    and x.is_group = 1
#    and x.concept_id = y.concept_id"
#
#echo "PPI SURVEYS - generate survey counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select e.id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from
#                    (
#                        select id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where type = 'PPI'
#                            and parent_id = 0
#                    ) a
#                left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#            ) e
#        left join
#            (
#                select c.id, d.*
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#                join
#                    (
#                        SELECT a.person_id, a.concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.search_all_domains\` a
#                        join
#                            (
#                                select concept_id
#                                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                                where type = 'PPI'
#                                    and is_group = 1
#                                    and is_selectable = 1
#                            ) b on a.concept_id = b.concept_id
#                        where is_standard = 0
#                    ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#        group by 1
#    ) y
#where x.type = 'PPI'
#    and x.id = y.id"
#
#
#################################################
## PPI PHYSICAL MEASUREMENTS (PM)
#################################################
#echo "PM - insert data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,value,
#    case when is_selectable = 1 then 0 else null end as est_count,
#    is_group,is_selectable,has_attribute,has_hierarchy
#from \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`
#where type = 'PM'
#order by 1"
#
#echo "PM - counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        select measurement_source_concept_id as concept_id, count(distinct person_id) as cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#        where measurement_source_concept_id in (903126,903133,903121,903124,903135,903136)
#        group by 1
#    ) y
#where x.type = 'PM'
#and x.concept_id = y.concept_id"
#
#echo "PM - counts for heart rhythm, pregnancy, and wheelchair use"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        select measurement_source_concept_id as concept_id, value_as_concept_id as value, count(distinct person_id) as cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#        where measurement_source_concept_id IN (1586218, 903120, 903111)
#        group by 1,2
#    ) y
#where x.type = 'PM'
#    and x.concept_id = y.concept_id
#    and x.value = y.value"
#
##----- BLOOD PRESSURE -----
## !!!!!!! WILL WANT TO REWRITE TO USE RELATIONSHIP INFO WHEN WE HAVE IT!!!!!!---
#echo "PM - blood pressure  - hypotensive"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count =
#    (
#        select count(distinct person_id)
#        from
#            (
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903118
#                    and value_as_number <= 90
#                intersect distinct
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903115
#                    and value_as_number <= 60
#            )
#    )
#where type = 'PM'
#    and subtype = 'BP'
#    and name LIKE 'Hypotensive%'"
#
#echo "PM - blood pressure  - normal"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count =
#    (
#        select count(distinct person_id)
#        from
#            (
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903118
#                    and value_as_number <= 120
#                intersect distinct
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903115
#                    and value_as_number <= 80
#            )
#    )
#where type = 'PM'
#    and subtype = 'BP'
#    and name LIKE 'Normal%'"
#
#echo "PM - blood pressure  - pre-hypertensive"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count =
#    (
#        select count(distinct person_id)
#        from
#            (
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903118
#                    and value_as_number BETWEEN 120 AND 139
#                intersect distinct
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903115
#                    and value_as_number BETWEEN 81 AND 89
#            )
#    )
#where type = 'PM'
#    and subtype = 'BP'
#    and name LIKE 'Pre-Hypertensive%'"
#
#echo "PM - blood pressure  - hypertensive"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count =
#    (
#        select count(distinct person_id)
#        from
#            (
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903118
#                    and value_as_number >= 140
#                intersect distinct
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903115
#                    and value_as_number >= 90
#            )
#    )
#where type = 'PM'
#    and subtype = 'BP'
#    and name LIKE 'Hypertensive%'"
#
#echo "PM - blood pressure  - detail"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count =
#    (
#        select count(distinct person_id)
#        from
#            (
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903118
#                intersect distinct
#                select person_id, measurement_date
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                where measurement_source_concept_id = 903115
#            )
#    )
#where type = 'PM'
#    and subtype = 'BP'
#    and name = 'Blood Pressure Detail'"
#
#
#################################################
## DEMOGRAPHICS
#################################################
#echo "DEMO - Age parent"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Person',1,'DEMO','AGE','Age',1,0,0,0"
#
#echo "DEMO - Age Children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select row_num + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS ID,
#    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'DEMO' and subtype = 'AGE' and parent_id = 0) as parent_id,
#    'Person',1,'DEMO','AGE', CAST(row_num AS STRING) as name, row_num as value,
#    case when b.cnt is null then 0 else b.cnt end as est_count,
#    0,1,0,0
#from
#    (
#        select ROW_NUMBER() OVER(ORDER BY person_id) as row_num
#        from \`$BQ_PROJECT.$BQ_DATASET.person\`
#        order by person_id limit 120
#    ) a
#left join
#    (
#        select CAST(FLOOR(DATE_DIFF(CURRENT_DATE(), DATE(birth_datetime), MONTH)/12) as INT64) as age, count(*) as cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.person\`
#        where person_id not in (select person_id from \`$BQ_PROJECT.$BQ_DATASET.death\`)
#        group by 1
#    ) b on a.row_num = b.age
#order by 1"
#
#echo "DEMO - Deceased"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Person',1,'DEMO','DEC','Deceased',
#    (select count(distinct person_id) from \`$BQ_PROJECT.$BQ_DATASET.death\`),
#    0,1,0,0"
#
#echo "DEMO - Gender parent"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Person',1,'DEMO','GEN','Gender',1,0,0,0"
#
#echo "DEMO - Gender children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS ID,
#    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'DEMO' and subtype = 'GEN' and parent_id = 0) as parent_id,
#    'Person',1,'DEMO','GEN',concept_id,
#    CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) ) as name,
#    b.cnt,0,1,0,0
#from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
#left join
#    (
#        select gender_concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.person\`
#        group by 1
#    ) b on a.concept_id = b.gender_concept_id
#where domain_id = 'Gender'
#    and standard_concept = 'S'
#    and b.cnt is not null"
#
#echo "DEMO - Race parent"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Person',1,'DEMO','RACE','Race',1,0,0,0"
#
#echo "DEMO - Race children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS ID,
#    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'DEMO' and subtype = 'RACE' and parent_id = 0) as parent_id,
#    'Person',1,'DEMO','RACE',concept_id,
#    CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) ) as name,
#    b.cnt,0,1,0,0
#from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
#left join
#    (
#        select race_concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.person\`
#        group by 1
#    ) b on a.concept_id = b.race_concept_id
#where domain_id = 'Race'
#    and standard_concept = 'S'
#    and b.cnt is not null"
#
#echo "DEMO - Ethnicity parent"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Person',1,'DEMO','ETH','Ethnicity',1,0,0,0"
#
#echo "DEMO - Ethnicity children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select ROW_NUMBER() OVER(ORDER BY concept_id) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS ID,
#    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'DEMO' and subtype = 'ETH' and parent_id = 0) as parent_id,
#    'Person',1,'DEMO','ETH',concept_id,
#    CONCAT( UPPER(SUBSTR(concept_name, 1, 1)), LOWER(SUBSTR(concept_name, 2)) ) as name,
#    b.cnt,0,1,0,0
#from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
#left join
#    (
#        select ethnicity_concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.person\`
#        group by 1
#    ) b on a.concept_id = b.ethnicity_concept_id
#where domain_id = 'Ethnicity'
#    and standard_concept = 'S'
#    and b.cnt is not null"
#
#
#################################################
## VISITS
#################################################
#echo "VISITS - add items with counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy)
#select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    0,'Visit',1,'VISIT',concept_id,concept_name,b.cnt,0,1,0,0
#from \`$BQ_PROJECT.$BQ_DATASET.concept\` a
#left join
#    (
#        select visit_concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.visit_occurrence\`
#        group by 1
#    ) b on a.concept_id = b.visit_concept_id
#where a.vocabulary_id = 'Visit'"
#
#
#################################################
## CONDITIONS
#################################################
## ----- SOURCE SNOMED -----
#echo "CONDITIONS - SOURCE SNOMED - temp table level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`
#    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#select *
#from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\` a
#where concept_id in
#    (
#        select distinct condition_source_concept_id
#        from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_source_concept_id = b.concept_id
#        where condition_source_concept_id != 0
#            and b.domain_id = 'Condition'
#            and b.vocabulary_id = 'SNOMED'
#    )"
#
## currently, there are only 5 levels, but we run it 6 times to be safe
#for i in {1..6};
#do
#    echo "CONDITIONS - SOURCE SNOMED - temp table level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`
#        (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#    select *
#    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\` a
#    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`)
#        and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`)"
#done
#
#echo "CONDITIONS - SOURCE SNOMED - add root"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Condition',0,'SNOMED',concept_id,concept_code,concept_name,1,0,0,1,
#    CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 as STRING) as path
#from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#where concept_code = '404684003'"
#
#echo "CONDITIONS - SOURCE SNOMED - add level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#    p.id,'Condition',0,'SNOMED',c.concept_id,c.concept_code,c.concept_name,1,1,0,1,
#    CONCAT(p.path, '.',
#        CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING))
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\` c on p.code = c.p_concept_code
#where p.domain_id = 'Condition'
#    and p.type = 'SNOMED'
#    and p.is_standard = 0
#    and p.id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)
#    and c.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`)"
#
## currently, there are only 16 levels, but we run it 18 times to be safe (if changed, change number of joins in next query)
#for i in {1..18};
#do
#    echo "CONDITIONS - SOURCE SNOMED - add level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#        (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#    select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#        p.id,'Condition',0,'SNOMED',c.concept_id,c.concept_code,c.concept_name,
#        case when l.concept_code is null then 1 else 0 end,
#        1,0,1,
#        CONCAT(p.path, '.',
#            CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING))
#    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#    join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\` c on p.code = c.p_concept_code
#    left join
#        (
#            select distinct a.concept_code
#            from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\` a
#            left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\` b on a.concept_id = b.p_concept_id
#            where b.concept_id is null
#        ) l on c.concept_code = l.concept_code
#    where p.domain_id = 'Condition'
#        and p.type = 'SNOMED'
#        and p.is_standard = 0
#        and p.id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)"
#done
#
## if loop count above is changed, the number of JOINS below must be updated
#echo "CONDITIONS - SOURCE SNOMED - add data into ancestor table"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` (ancestor_id, descendant_id)
#select distinct a.ID as ancestor_id,
#    coalesce(t.id, s.id, r.id, q.id, p.id, o.id, n.ID, m.ID, k.ID, j.ID, i.ID, h.ID, g.ID, f.ID, e.ID, d.ID, c.ID, b.ID) as descendant_id
#from (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) a
#    join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) b on a.ID = b.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) c on b.ID = c.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) d on c.ID = d.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) e on d.ID = e.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) f on e.ID = f.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) g on f.ID = g.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) h on g.ID = h.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) i on h.ID = i.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) j on i.ID = j.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) k on j.ID = k.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) m on k.ID = m.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) n on m.ID = n.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) o on n.ID = o.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) p on o.ID = p.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) q on p.ID = q.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) r on q.ID = r.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) s on r.ID = s.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where domain_id = 'Condition' and type = 'SNOMED' and is_standard = 0) t on s.ID = t.PARENT_ID
#where a.parent_id != 0 and a.is_group = 1"
#
#echo "CONDITIONS - SOURCE SNOMED - child counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#        select condition_source_concept_id as concept_id, count(distinct person_id) cnt
#        from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
#        group by 1
#    ) y
#where x.concept_id = y.concept_id
#    and x.domain_id = 'Condition'
#    and x.type = 'SNOMED'
#    and x.is_standard = 0
#    and x.is_group = 0
#    and x.is_selectable = 1"
#
#echo "CONDITIONS - SOURCE SNOMED - parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select e.id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from
#                    (
#                        select id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where domain_id = 'Condition'
#                            and type = 'SNOMED'
#                            and is_standard = 0
#                            and parent_id != 0
#                            and is_group = 1
#                    ) a
#                left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#            ) e
#        left join
#            (
#                select c.id, d.*
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#                join
#                    (
#                        select a.person_id, a.condition_source_concept_id as concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
#                        join
#                            (
#                                select concept_id, path
#                                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                                where domain_id = 'Condition'
#                                    and type = 'SNOMED'
#                                    and is_standard = 0
#                                    and is_group = 0
#                                    and is_selectable = 1
#                            ) b on a.condition_source_concept_id = b.concept_id
#                    ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#        group by 1
#    ) y
#where x.id = y.id"

## ----- STANDARD SNOMED -----
#echo "CONDITIONS - STANDARD SNOMED - temp table level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`
#    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#select *
#from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
#where concept_id in
#    (
#        select distinct condition_concept_id
#        from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.condition_concept_id = b.concept_id
#        where condition_concept_id != 0
#            and b.domain_id = 'Condition'
#            and b.standard_concept = 'S'
#            and b.vocabulary_id = 'SNOMED'
#    )"
#
## currently, there are only 5 levels, but we run it 6 times to be safe
#for i in {1..6};
#do
#    echo "CONDITIONS - STANDARD SNOMED - temp table level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`
#        (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#    select *
#    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\` a
#    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)
#      and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)"
#done
#
#echo "CONDITIONS - STANDARD SNOMED - add root"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS ID,
#    0,'Condition',1,'SNOMED',concept_id,concept_code,concept_name,1,0,0,1,
#    CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 as STRING) as path
#from \`$BQ_PROJECT.$BQ_DATASET.concept\`
#where concept_code = '404684003'"

#echo "CONDITIONS - STANDARD SNOMED - add level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#    p.id,'Condition',1,'SNOMED',c.concept_id,c.concept_code,c.concept_name,1,1,0,1,
#    CONCAT(p.path, '.',
#        CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING))
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
#where p.domain_id = 'Condition'
#    and p.type = 'SNOMED'
#    and p.is_standard = 1
#    and p.id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)
#    and c.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`)"
#
## currently, there are only 17 levels, but we run it 18 times to be safe
#for i in {1..18};
#do
#    echo "CONDITIONS - STANDARD SNOMED - add level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#        (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#    select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#        p.id,'Condition',1,'SNOMED',c.concept_id,c.concept_code,c.concept_name,
#        case when l.concept_code is null then 1 else 0 end,
#        1,0,1,
#        CONCAT(p.path, '.',
#            CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING))
#    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#    join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` c on p.code = c.p_concept_code
#    left join
#        (
#            select distinct a.CONCEPT_CODE
#            from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` a
#            left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\` b on a.concept_id = b.p_concept_id
#            where b.concept_id is null
#        ) l on c.concept_code = l.concept_code
#    where p.domain_id = 'Condition'
#        and p.type = 'SNOMED'
#        and p.is_standard = 1
#        and p.id not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)"
#done

#echo "CONDITIONS - STANDARD SNOMED - child counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#set x.est_count = y.cnt
#from
#    (
#    select condition_concept_id as concept_id, count(distinct person_id) cnt
#    from \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\`
#    group by 1
#    ) y
#where x.concept_id = y.concept_id
#    and x.domain_id = 'Condition'
#    and x.type = 'SNOMED'
#    and x.is_standard = 1
#    and x.is_group = 0
#    and x.is_selectable = 1"
#
#echo "CONDITIONS - STANDARD SNOMED - parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select ancestor_concept_id as concept_id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
#                where ancestor_concept_id in
#                    (
#                        select distinct concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where domain_id = 'Condition'
#                            and type = 'SNOMED'
#                            and is_standard = 1
#                            and parent_id != 0
#                            and is_group = 1
#                    )
#            ) a
#        join \`$BQ_PROJECT.$BQ_DATASET.condition_occurrence\` b on a.descendant_concept_id = b.condition_concept_id
#        group by 1
#    ) y
#where x.concept_id = y.concept_id
#    and x.domain_id = 'Condition'
#    and x.type = 'SNOMED'
#    and x.is_standard = 1
#    and x.is_group = 1"


#################################################
## MEASUREMENTS
#################################################
#echo "MEASUREMENTS - add clinical root"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 as ID,
#    0,'Measurement',1,'LOINC','CLIN',36207527,'LP248771-0','Clinical',1,0,0,1,
#    CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS STRING)"
#
#echo "MEASUREMENTS - add lab root"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 as ID,
#    0,'Measurement',1,'LOINC','LAB',36206173,'LP29693-6','Lab',1,0,0,1,
#    CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS STRING)"
#
## ----- LOINC CLINICAL -----
#echo "MEASUREMENTS - clinical - add parents"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#SELECT ROW_NUMBER() OVER(order by name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    (SELECT ID FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'LOINC' and subtype = 'CLIN') as parent_id,
#    'Measurement',1,'LOINC','CLIN',name,1,0,0,1,
#    CONCAT( (SELECT PATH FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` WHERE type = 'LOINC' and subtype = 'CLIN'), '.',
#        CAST(ROW_NUMBER() OVER(order by name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING) )
#from
#    (
#        select distinct parent as name
#        from \`$BQ_PROJECT.$BQ_DATASET.prep_clinical_terms_nc\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b using (concept_id)
#        where b.concept_id in (select distinct measurement_concept_id from \`$BQ_PROJECT.$BQ_DATASET.measurement\`)
#    ) x"
#
## add items with vocabulary_id = 'LOINC' and concept_class_id = 'Clinical Observation' where we have categorized them
#echo "MEASUREMENTS - clinical - add children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select ROW_NUMBER() OVER(order by parent_id, concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    parent_id,'Measurement',1,'LOINC','CLIN',concept_id,concept_code,concept_name,est_count,0,1,0,1,
#    CONCAT(parent_path, '.',
#        CAST(ROW_NUMBER() OVER(order by parent_id, concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING))
#from
#    (
#        select z.*, y.id as parent_id, y.path as parent_path
#        from \`$BQ_PROJECT.$BQ_DATASET.prep_clinical_terms_nc\` x
#        join
#            (
#                select id,name,path
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                where type = 'LOINC'
#                    and subtype = 'CLIN'
#                    and is_group = 1
#            ) y on x.parent = y.name
#        join
#            (
#                select concept_name, concept_id, concept_code, count(distinct person_id) est_count
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#                left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
#                where standard_concept = 'S'
#                    and domain_id = 'Measurement'
#                    and vocabulary_id = 'LOINC'
#                group by 1,2,3
#            ) z on x.concept_id = z.concept_id
#    ) g"
#
##----- LOINC LABS -----
#echo "MEASUREMENTS - labs - load temp table 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`
#    (p_concept_id, p_concept_code, p_concept_name, concept_id, concept_code, concept_name)
#select *
#from \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` a
#where concept_id in
#    (select distinct MEASUREMENT_CONCEPT_ID
#    from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#    left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.MEASUREMENT_CONCEPT_ID = b.concept_id
#    where MEASUREMENT_CONCEPT_ID != 0
#        and b.vocabulary_id = 'LOINC'
#        and b.STANDARD_CONCEPT = 'S'
#        and b.domain_id = 'Measurement')"
#
## currently, there are only 4 levels, but we run it 5 times to be safe
#for i in {1..5};
#do
#    echo "MEASUREMENTS - labs - load temp table $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`
#        (p_concept_id, p_concept_code, p_concept_name, concept_id, concept_code, concept_name)
#    select *
#    from \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\` a
#    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)
#        and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)"
#done
#
#echo "MEASUREMENTS - labs - add roots - level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#    t.ID,'Measurement',1,'LOINC','LAB',b.CONCEPT_ID,b.CONCEPT_CODE,b.CONCEPT_NAME,1,0,0,1,
#    CONCAT( t.path, '.',
#        CAST(row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING) )
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` t
#join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on t.code = b.p_concept_code
#where (t.id) not in (select PARENT_ID from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)
#    and t.type = 'LOINC'
#    and t.subtype = 'LAB'
#    and b.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`)"
#
## for each loop, add all items (children/parents) directly under the items that were previously added
## currently, there are only 11 levels, but we run it 12 times to be safe
## if this number is changed, you will need to change the number of JOINS in the query below adding data to the ancestor table
#for i in {1..12};
#do
#    echo "MEASUREMENTS - labs - add level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#        (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#    select row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#        t.ID, 'Measurement',1,'LOINC','LAB',b.CONCEPT_ID,b.CONCEPT_CODE, b.CONCEPT_NAME,
#        case when l.CONCEPT_CODE is null then null else m.cnt end as est_count,
#        case when l.CONCEPT_CODE is null then 1 else 0 end as is_group,
#        case when l.CONCEPT_CODE is null then 0 else 1 end as is_selectable,
#        0,1,
#        CONCAT(t.path, '.',
#            CAST(row_number() over (order by t.ID, b.CONCEPT_NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING))
#    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` t
#    join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on t.code = b.p_concept_code
#    left join
#        (
#            select distinct a.CONCEPT_CODE
#            from \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` a
#            left join \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\` b on a.CONCEPT_ID = b.P_CONCEPT_ID
#            where b.CONCEPT_ID is null
#        ) l on b.CONCEPT_CODE = l.CONCEPT_CODE
#    left join
#        (
#            select measurement_concept_id, count(distinct person_id) cnt
#            from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#            group by 1
#        ) m on b.concept_id = m.measurement_concept_id
#    where
#        (id) not in
#            (
#                select parent_id
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                where type = 'LOINC'
#                    and subtype = 'LAB'
#            )
#        and parent_id != 0"
#done
#
#echo "MEASUREMENTS - labs - add parent for uncategorized labs"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#SELECT (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 as ID,
#    a.id as parent_id,'Measurement',1,'LOINC','LAB','Uncategorized',1,0,0,1,
#    CONCAT(a.path, '.', CAST((SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)+1 AS STRING))
#FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` a
#WHERE type = 'LOINC' and subtype = 'LAB' and parent_id = 0"
#
#echo "MEASUREMENTS - labs - add uncategorized labs"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as parent_id,
#    'Measurement',1,'LOINC','LAB',concept_id,concept_code,concept_name,est_count,0,1,0,1,
#    CONCAT( (select path from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB' and name = 'Uncategorized'), '.',
#        CAST(ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING) )
#from
#    (
#        select concept_id, concept_code, concept_name, count(distinct person_id) est_count
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
#        where standard_concept = 'S'
#            and domain_id = 'Measurement'
#            and vocabulary_id = 'LOINC'
#            and measurement_concept_id not in
#                (
#                    select concept_id
#                    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                    where type = 'LOINC'
#                    and concept_id is not null
#                )
#        group by 1,2,3
#    ) x"
#
## if loop count above is changed, the number of JOINS below must be updated
#echo "MEASUREMENTS - labs - add data into ancestor table"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\`
#    (ancestor_id, descendant_id)
#select distinct a.ID as ancestor_id,
#    coalesce(n.ID, m.ID, k.ID, j.ID, i.ID, h.ID, g.ID, f.ID, e.ID, d.ID, c.ID, b.ID) as descendant_id
#from (select id, parent_id, concept_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') a
#    join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') b on a.ID = b.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') c on b.ID = c.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') d on c.ID = d.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') e on d.ID = e.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') f on e.ID = f.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') g on f.ID = g.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') h on g.ID = h.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') i on h.ID = i.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') j on i.ID = j.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') k on j.ID = k.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') m on k.ID = m.PARENT_ID
#    left join (select id, parent_id, is_group, is_selectable from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'LOINC' and subtype = 'LAB') n on m.ID = n.PARENT_ID
#where a.is_selectable = 0
#    and a.parent_id != 0"
#
#echo "MEASUREMENTS - labs - generate parent counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select e.id, count(distinct person_id) cnt
#        from
#            (
#                select * from
#                (
#                    select id
#                    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                    where type = 'LOINC'
#                        and subtype = 'LAB'
#                        and is_group = 1
#                        and parent_id != 0
#                ) a
#                left join \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\` b on a.id = b.ancestor_id
#            ) e
#        left join
#            (
#                select c.id, d.*
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
#                join
#	                (
#		                SELECT person_id, concept_id
#		                FROM \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#						join
#			                (
#								select concept_id
#								from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#			                	where type = 'LOINC'
#									and subtype = 'LAB'
#									and is_selectable = 1
#			                ) b on a.measurement_concept_id = b.concept_id
#	                ) d on c.concept_id = d.concept_id
#            ) f on e.descendant_id = f.id
#        group by 1
#    ) y
#where x.id = y.id"
#
########## TODO: DO WE REALLY NEED TO DO THIS???? #########
## the first item in the measurement tree needs to have 'null' as a code so the logic works correctly
##echo "MEASUREMENTS - labs - clean up"
##bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
##UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
##SET code = null, concept_id = null
##where type = 'MEAS' and subtype = 'LAB' and parent_id = 0
########## TODO: DO WE REALLY NEED TO DO THIS???? #########
#
##----- OTHER VOCABULARIES -----
#echo "MEASUREMENTS - add other Standard Concepts (except SNOMED)"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    0, 'Measurement',1, vocabulary_id,concept_id,concept_code,concept_name,est_count,0,1,0,0,
#    CAST(ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING) as path
#from
#    (
#        select concept_name, vocabulary_id, concept_id, concept_code, count(distinct person_id) est_count
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
#        where standard_concept = 'S'
#            and domain_id = 'Measurement'
#            and vocabulary_id not in ('LOINC', 'SNOMED', 'PPI')
#        group by 1,2,3,4
#    ) x"
#
##----- SNOMED -----
#echo "MEASUREMENTS - SNOMED - temp table level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
#    (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#select *
#from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` a
#where concept_id in
#    (
#        select distinct measurement_concept_id
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
#        where measurement_concept_id != 0
#            and b.vocabulary_id = 'SNOMED'
#            and b.STANDARD_CONCEPT = 'S'
#            and b.domain_id = 'Measurement'
#    )"
#
## for each loop, add all items (children/parents) directly under the items that were previously added
## currently, there are only 3 levels, but we run it 4 times to be safe
#for i in {1..4};
#do
#    echo "MEASUREMENTS - SNOMED - temp table level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`
#        (p_concept_id, p_concept_code, p_concept_name, p_domain_id, concept_id, concept_code, concept_name, domain_id)
#    select *
#    from \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\` a
#    where concept_id in (select P_CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)
#        and concept_id not in (select CONCEPT_ID from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)"
#done
#
#echo "MEASUREMENTS - SNOMED - add roots"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as ID,
#    0, 'Measurement',1,'SNOMED',concept_id,concept_code,concept_name,1,0,0,1,
#    CAST(ROW_NUMBER() OVER(order by concept_name) + (SELECT MAX(ID) FROM \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING) as path
#from
#    (
#        select distinct concept_id, concept_name, concept_code
#        from
#            (
#                select *, rank() over (partition by descendant_concept_id order by MAX_LEVELS_OF_SEPARATION desc) rnk
#                from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\` a
#                left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.ANCESTOR_CONCEPT_ID = b.concept_id
#                where domain_id = 'Measurement'
#                    and descendant_concept_id in
#                        (
#                            select distinct concept_id
#                            from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#                            left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.measurement_concept_id = b.concept_id
#                            where standard_concept = 'S'
#                                and domain_id = 'Measurement'
#                                and vocabulary_id = 'SNOMED'
#                        )
#            ) a
#        where rnk = 1
#    ) x"
#
#echo "MEASUREMENTS - SNOMED - add level 0"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#    p.id, 'Measurement',1,'SNOMED',c.concept_id,c.concept_code,c.concept_name,1,0,0,1,
#    CONCAT(p.path, '.',
#        CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) AS STRING))
#from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
#where p.domain_id = 'Measurement'
#    and p.type = 'SNOMED'
#    and p.id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)
#    and c.concept_id in (select p_concept_id from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`)"
#
## for each loop, add all items (children/parents) directly under the items that were previously added
## currently, there are only 6 levels, but we run it 7 times to be safe
#for i in {1..7};
#do
#    echo "MEASUREMENTS - SNOMED - add level $i"
#    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#    "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#        (id,parent_id,domain_id,is_standard,type,concept_id,code,name,is_group,is_selectable,has_attribute,has_hierarchy,path)
#    select row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`),
#        p.id, 'Measurement',1,'SNOMED',c.concept_id,c.concept_code,c.concept_name,
#        case when l.concept_code is null then 1 else 0 end,
#        case when l.concept_code is null then 0 else 1 end,
#        0,1,
#        CONCAT(p.path, '.',
#            CAST(row_number() over (order by p.id, c.concept_name)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING))
#    from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` p
#    join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` c on p.code = c.p_concept_code
#    left join
#        (
#            select distinct a.concept_code
#            from \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` a
#            left join \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\` b on a.concept_id = b.p_concept_id
#            where b.concept_id is null
#        ) l on c.concept_code = l.concept_code
#    where p.domain_id = 'Measurement'
#        and p.type = 'SNOMED'
#        and p.id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)
#        and c.concept_id not in (select parent_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)"
#done
#
#echo "MEASUREMENTS - SNOMED - generate counts"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
#SET x.est_count = y.cnt
#from
#    (
#        select ancestor_concept_id as concept_id, count(distinct person_id) cnt
#        from
#            (
#                select *
#                from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
#                where ancestor_concept_id in
#                    (
#                        select distinct concept_id
#                        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                        where domain_id = 'Measurement'
#                            and type = 'SNOMED'
#                    )
#            ) a
#        join \`$BQ_PROJECT.$BQ_DATASET.measurement\` b on a.descendant_concept_id = b.measurement_concept_id
#        group by 1
#    ) y
#where x.concept_id = y.concept_id
#    and x.domain_id = 'Measurement'
#    and x.type = 'SNOMED'"
#
#echo "MEASUREMENTS - SNOMED - add parents as children"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#    (id,parent_id,domain_id,is_standard,type,concept_id,code,name,est_count,is_group,is_selectable,has_attribute,has_hierarchy,path)
#select (row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`)) as ID,
#    id as parent_id,domain_id,is_standard,type,concept_id,code,name,cnt,0,1,0,1,CONCAT(path, '.',
#    CAST(row_number() over (order by PARENT_ID, NAME)+(select max(id) from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`) as STRING))
#from
#    (
#        select *
#        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` a
#        join
#            (
#                select measurement_concept_id, count(distinct person_id) cnt
#                from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#                group by 1
#            ) b on a.concept_id = b.measurement_concept_id
#        where domain_id = 'Measurement'
#            and type = 'SNOMED'
#            and is_group = 1
#    ) x"


################################################
# SYNONYMS
################################################
echo "SYNONYMS - add synonym data to criteria"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
set x.synonyms = y.synonyms
from
    (
        select c.id,
        case
            when c.code is null and c.name is not null then c.name
            when c.code is not null and c.name is not null and string_agg(replace(cs.concept_synonym_name,'|','||'),'|') is null then concat(c.code,'|',c.name)
            else concat(c.code,'|',c.name,'|',string_agg(replace(cs.concept_synonym_name,'|','||'),'|'))
        end as synonyms
        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` c
        left join \`$BQ_PROJECT.$BQ_DATASET.concept_synonym\` cs on c.concept_id = cs.concept_id
        where domain_id not in ('Survey', 'PhysicalMeasurement', 'Person')
        group by c.id, c.name, c.code
    ) y
where x.id = y.id"

echo "SYNONYMS - add demographics synonym data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
set x.synonyms = y.synonyms
from
    (
        select id,
        case
            when subtype = 'AGE' and parent_id = 0 then 'Age'
            when subtype = 'DEC' and parent_id = 0 then 'Deceased'
            when subtype = 'GEN' and parent_id = 0 then 'Gender'
            when subtype = 'RACE' and parent_id = 0 then 'Race'
            when subtype = 'ETH' and parent_id = 0 then 'Ethnicity'
            when subtype = 'AGE' and parent_id != 0 then null
        else name
        end as synonyms
        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
        where domain_id = 'Person'
    ) y
where x.id = y.id"

echo "SYNONYMS - add PPI / Physical Measurement synonym data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
set x.synonyms = x.name
where domain_id in ('Survey', 'PhysicalMeasurement')"

# add [rank1] for all items. this is to deal with the poly-hierarchical issue in many trees
echo "SYNONYMS - add [rank1]"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` x
set x.synonyms = CONCAT(x.synonyms, '|', y.rnk)
from
    (
        select min(id) as id, '[rank1]' as rnk
        from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
        where synonyms is not null
        group by domain_id, is_standard, type, subtype, concept_id, name
    ) y
where x.id = y.id"


# ################################################
# # CRITERIA ANCESTOR
# ################################################
# echo "CRITERIA_ANCESTOR - Drugs - add ingredients to drugs mapping"
# bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
# "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_ancestor\` (ancestor_concept_id, descendant_concept_id)
# select ancestor_concept_id, descendant_concept_id
# from \`$BQ_PROJECT.$BQ_DATASET.concept_ancestor\`
# where ancestor_concept_id in
#     (select distinct concept_id
#     from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#     where type = 'DRUG'
#         and subtype = 'ATC'
#         and is_group = 0
#         and is_selectable = 1)
# and descendant_concept_id in (select distinct drug_concept_id from \`$BQ_PROJECT.$BQ_DATASET.drug_exposure\`)"


#################################################
## CRITERIA ATTRIBUTES
#################################################
## this code filters out any labs where all results = 0
#echo "CRITERIA_ATTRIBUTES - Measurements - add numeric results"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute_test\`
#    (id, concept_id, value_as_concept_id, concept_name, type, est_count)
#select ROW_NUMBER() OVER(order by measurement_concept_id), *
#from
#	(
#		select measurement_concept_id, 0, 'MIN', 'NUM', CAST(min(value_as_number) as STRING)
#		from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#		where measurement_concept_id in
#			(
#				select concept_id
#				from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#				where domain_id = 'Measurement'
#			)
#			and value_as_number is not null
#		group by 1
#		having not (min(value_as_number) = 0 and max(value_as_number) = 0)
#
#		UNION ALL
#
#		select measurement_concept_id, 0, 'MAX', 'NUM', CAST(max(value_as_number) as STRING)
#		from \`$BQ_PROJECT.$BQ_DATASET.measurement\`
#		where measurement_concept_id in
#			(
#				select concept_id
#				from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#				where domain_id = 'Measurement'
#			)
#			and value_as_number is not null
#		group by 1
#		having not (min(value_as_number) = 0 and max(value_as_number) = 0)
#	) a"
#
#echo "CRITERIA_ATTRIBUTES - Measurements - add categorical results"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute_test\`
#    (id, concept_id, value_as_concept_Id, concept_name, type, est_count)
#select ROW_NUMBER() OVER(order by measurement_concept_id), *
#from
#    (
#        select measurement_concept_id, value_as_concept_id, b.concept_name, 'CAT' as type, CAST(count(distinct person_id) as STRING)
#        from \`$BQ_PROJECT.$BQ_DATASET.measurement\` a
#        left join \`$BQ_PROJECT.$BQ_DATASET.concept\` b on a.value_as_concept_Id = b.concept_id
#        where measurement_concept_id in
#            (
#                select concept_id
#                from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#                where domain_id = 'Measurement'
#            )
#            and value_as_concept_id != 0
#            and value_as_concept_id is not null
#        group by 1,2,3
#    ) a"
#
## set has_attributes=1 for any criteria that has data in criteria_attribute
#echo "CRITERIA ATTRIBUTES - update has_attributes column for measurement criteria"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"update \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set has_attribute = 1
#where concept_id in
#    (
#        select distinct concept_id
#        from \`$BQ_PROJECT.$BQ_DATASET.criteria_attribute_test\`
#    )
#    and domain_id = 'Measurement'
#    and is_selectable = 1"
#
#
# ################################################
# # CRITERIA RELATIONSHIP
# ################################################
# echo "CRITERIA_RELATIONSHIP - Drugs - add drug/ingredient relationships"
# bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
# "insert into \`$BQ_PROJECT.$BQ_DATASET.criteria_relationship\` ( concept_id_1, concept_id_2 )
# select cr.concept_id_1, cr.concept_id_2
# from \`$BQ_PROJECT.$BQ_DATASET.concept_relationship\` cr
# join \`$BQ_PROJECT.$BQ_DATASET.concept\` c1 on cr.concept_id_2 = c1.concept_id
# where cr.concept_id_1 in (select concept_id from \`$BQ_PROJECT.$BQ_DATASET.criteria_test\` where type = 'DRUG' and subtype = 'BRAND')
# and c1.concept_class_id = 'Ingredient'"
#
#
#################################################
## DATA CLEAN UP
#################################################
#echo "CLEAN UP - set est_count = -1 where the count is NULL"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"UPDATE \`$BQ_PROJECT.$BQ_DATASET.criteria_test\`
#set est_count = -1
#where est_count is null"


################################################
# DATABASE CLEAN UP - drop tables/views
################################################
#echo "DROP - prep_criteria"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.prep_criteria\`"
#
#echo "DROP - prep_criteria_ancestor"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.prep_criteria_ancestor\`"
#
#echo "DROP - prep_clinical_terms_nc"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.prep_clinical_terms_nc\`"
#
#echo "DROP - atc_rel_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.atc_rel_in_data\`"
#
#echo "DROP - loinc_rel_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.loinc_rel_in_data\`"
#
#echo "DROP - snomed_rel_cm_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_in_data\`"
#
#echo "DROP - snomed_rel_cm_src_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_cm_src_in_data\`"
#
#echo "DROP - snomed_rel_pcs_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP TABLE IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_pcs_in_data\`"
#
#echo "DROP - snomed_rel_meas_in_data"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.snomed_rel_meas_in_data\`"
#
#echo "DROP - v_loinc_rel"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_loinc_rel\`"
#
#echo "DROP - v_snomed_rel_cm"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm\`"
#
#echo "DROP - v_snomed_rel_cm_src"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_cm_src\`"
#
#echo "DROP - v_snomed_rel_pcs"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_pcs\`"
#
#echo "DROP - v_snomed_rel_meas"
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"DROP VIEW IF EXISTS \`$BQ_PROJECT.$BQ_DATASET.v_snomed_rel_meas\`"
