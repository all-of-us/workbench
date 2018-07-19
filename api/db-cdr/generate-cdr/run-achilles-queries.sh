#!/bin/bash

# Runs achilles queries to populate count db for cloudsql in BigQuery
set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-clousql-cdr/run-achilles-queries.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
USAGE="$USAGE --cdr-version=YYYYMMDD"

while [ $# -gt 0 ]; do
  echo "1 is $1"
  case "$1" in
    --bq-project) BQ_PROJECT=$2; shift 2;;
    --bq-dataset) BQ_DATASET=$2; shift 2;;
    --workbench-project) WORKBENCH_PROJECT=$2; shift 2;;
    --workbench-dataset) WORKBENCH_DATASET=$2; shift 2;;
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

if [ -z "${WORKBENCH_PROJECT}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

if [ -z "${WORKBENCH_DATASET}" ]
then
  echo "Usage: $USAGE"
  exit 1
fi

# TODO Next Populate achilles_results
echo "Running achilles queries..."

echo "Getting person count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, count_value,source_count_value) select 0 as id, 1 as analysis_id,  COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`"


# Gender count
echo "Getting gender count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 2 as analysis_id,  cast (gender_concept_id as STRING) as stratum_1, COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by GENDER_CONCEPT_ID"

# Age count
# 3	Number of persons by year of birth
echo "Getting age count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 3 as analysis_id,  CAST(year_of_birth AS STRING) as stratum_1, COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by YEAR_OF_BIRTH"


#  4	Number of persons by race
echo "Getting race count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 4 as analysis_id,  CAST(RACE_CONCEPT_ID AS STRING) as stratum_1, COUNT(distinct person_id) as count_value,0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by RACE_CONCEPT_ID"

# 5	Number of persons by ethnicity
echo "Getting ethnicity count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 5 as analysis_id,  CAST(ETHNICITY_CONCEPT_ID AS STRING) as stratum_1, COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by ETHNICITY_CONCEPT_ID"

# 10	Number of all persons by year of birth and by gender
echo "Getting year of birth , gender count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value,source_count_value)
select 0, 10 as analysis_id,  CAST(year_of_birth AS STRING) as stratum_1,
  CAST(gender_concept_id AS STRING) as stratum_2,
  COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by YEAR_OF_BIRTH, gender_concept_id"

# 12	Number of persons by race and ethnicity
echo "Getting race, ethnicity count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, count_value,source_count_value)
select 0, 12 as analysis_id, CAST(RACE_CONCEPT_ID AS STRING) as stratum_1, CAST(ETHNICITY_CONCEPT_ID AS STRING) as stratum_2, COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by RACE_CONCEPT_ID,ETHNICITY_CONCEPT_ID"

# 200	(3000 ) Number of persons with at least one visit occurrence, by visit_concept_id
echo "Getting visit count and source count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
 select 0 as id,3000 as analysis_id,CAST(vo1.visit_concept_id AS STRING) as stratum_1,'Visit' as stratum_3,
 	COUNT(distinct vo1.PERSON_ID) as count_value,(select COUNT(distinct vo2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo2 where vo2.visit_source_concept_id=vo1.visit_concept_id) as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo1
 where vo1.visit_concept_id > 0
 group by vo1.visit_concept_id
 union all
 select 0 as id,3000 as analysis_id,CAST(vo1.visit_source_concept_id AS STRING) as stratum_1,'Visit' as stratum_3,
 COUNT(distinct vo1.PERSON_ID) as count_value,COUNT(distinct vo1.PERSON_ID) as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo1
 where vo1.visit_concept_id != vo1.visit_source_concept_id
 group by vo1.visit_source_concept_id"


# 400 (3000)	Number of persons with at least one condition occurrence, by condition_concept_id
echo "Querying condition_occurrence ..."
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(co1.condition_CONCEPT_ID AS STRING) as stratum_1,'Condition' as stratum_3,
	COUNT(distinct co1.PERSON_ID) as count_value, (select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co2
	where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
where co1.condition_concept_id > 0
group by co1.condition_CONCEPT_ID
union all
 select 0 as id,3000 as analysis_id,CAST(co1.condition_source_concept_id AS STRING) as stratum_1,'Condition' as stratum_3,
 COUNT(distinct co1.PERSON_ID) as count_value,COUNT(distinct co1.PERSON_ID) as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
 where co1.condition_concept_id != co1.condition_source_concept_id
 group by co1.condition_source_concept_id"

# Condition gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3101 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Condition' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co2 on p2.person_id=co2.person_id where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
where co1.condition_concept_id > 0
group by co1.condition_concept_id, p1.gender_concept_id
union all
select 0, 3101 as analysis_id,CAST(co1.condition_source_concept_id AS STRING) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'Condition' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,COUNT(distinct p1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id where co1.condition_concept_id != co1.condition_source_concept_id
group by co1.condition_source_concept_id, p1.gender_concept_id"

# (400 age ) 3102 Number of persons with at least one condition occurrence, by condition_concept_id by age decile
# Age Deciles : They will be 18 - 29, 30 - 39, 40 - 49, 50 - 59, 60 - 69, 70 - 79, 80-89, 90+
#  children are 0-17 and we don't have children for now . Want all adults in a bucket thus 18 - 29 .
#Ex yob = 2000  , start date : 2017 -- , sd - yob = 17  / 10 = 1.7 floor(1.7 ) = 1
# 30 - 39 , 2017 - 1980 = 37 / 10 = 3

# Get the 30-39, 40 - 49 , ... groups
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
CAST(co1.condition_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from condition_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Condition' as stratum_3,
count(distinct p1.person_id) as count_value,(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co2
on p2.person_id=co2.person_id where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
where floor((extract(year from condition_start_date) - p1.year_of_birth)/10) >=3
and co1.condition_concept_id > 0
group by co1.condition_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,
CAST(co1.condition_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from condition_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Condition' as stratum_3,
COUNT(distinct p1.person_id) as count_value,
COUNT(distinct p1.person_id) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id where floor((extract(year from condition_start_date) - p1.year_of_birth)/10) >=3 and co1.condition_concept_id != co1.condition_source_concept_id
group by co1.condition_source_concept_id, stratum_2"

#Get conditions by age decile id 3102 for the 18-29 group labeled as 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value,source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.condition_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Condition' as stratum_3,
  count(distinct p1.person_id) as count_value,(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co2 on p2.person_id=co2.person_id where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
on p1.person_id = co1.person_id
where (extract(year from condition_start_date) - p1.year_of_birth) > 18 and (extract(year from condition_start_date) - p1.year_of_birth) < 30
and co1.condition_concept_id > 0
group by co1.condition_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.condition_source_concept_id AS STRING) as stratum_1,'2' as stratum_2,'Condition' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1 on p1.person_id = co1.person_id
where (extract(year from condition_start_date) - p1.year_of_birth) > 18 and (extract(year from condition_start_date) - p1.year_of_birth) < 30
and co1.condition_concept_id != co1.condition_source_concept_id
group by co1.condition_source_concept_id,stratum_2"

# No death data now per Kayla. Later when we have more data
# 500	(3000) Number of persons with death, by cause_concept_id
#echo "Querying death ..."
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
#(id, analysis_id, stratum_1, count_value)
#select 0, 3000 as analysis_id,
#	CAST(d1.cause_concept_id AS STRING) as stratum_1,
#	COUNT(distinct d1.PERSON_ID) as count_value
#from \`${BQ_PROJECT}.${BQ_DATASET}.death\` d1
#group by d1.cause_concept_id"
#
## Death (3101)	Number of persons with a death by death cause concept id by  gender concept id
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
#(id, analysis_id, stratum_1, stratum_2, count_value)
#select 0, 3101 as analysis_id,
#	CAST(co1.cause_concept_id AS STRING) as stratum_1,
#	CAST(p1.gender_concept_id AS STRING) as stratum_2,
#	COUNT(distinct p1.PERSON_ID) as count_value
#from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
#\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
#on p1.person_id = co1.person_id
#group by co1.cause_concept_id,
#	p1.gender_concept_id"
#
## Death (3102)	Number of persons with a death by death cause concept id by  age decile  30+ yr old deciles */
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
#(id, analysis_id, stratum_1, stratum_2, count_value)
#select 0, 3102 as analysis_id,
#	CAST(co1.cause_concept_id AS STRING) as stratum_1,
#	CAST(floor((extract(year from co1.death_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
#	COUNT(distinct p1.PERSON_ID) as count_value
#from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
#\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
#on p1.person_id = co1.person_id
#where floor((extract(year from co1.death_date) - p1.year_of_birth)/10) >=3
#group by co1.cause_concept_id,
#	stratum_2"
#
## Death (3102)	Number of persons with a death by death cause concept id by  age decile  18-29 yr old decile 2 */
#bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
#"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
#(id, analysis_id, stratum_1, stratum_2, count_value)
#select 0, 3102 as analysis_id, CAST(co1.cause_concept_id AS STRING) as stratum_1, '2' as stratum_2,
#	COUNT(distinct p1.PERSON_ID) as count_value
#from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
#\`${BQ_PROJECT}.${BQ_DATASET}.death\` co1
#on p1.person_id = co1.person_id
#where (extract(year from co1.death_date) - p1.year_of_birth) >= 18 and (extract(year from co1.death_date) - p1.year_of_birth) < 30
#group by co1.cause_concept_id,
#	stratum_2"

# 600	Number of persons with at least one procedure occurrence, by procedure_concept_id
echo "Querying procedure_occurrence"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(po1.procedure_CONCEPT_ID AS STRING) as stratum_1,'Procedure' as stratum_3,
	COUNT(distinct po1.PERSON_ID) as count_value,
	(select COUNT(distinct po2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` po2 where po2.procedure_source_CONCEPT_ID=po1.procedure_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` po1
where po1.procedure_concept_id > 0
group by po1.procedure_CONCEPT_ID
union all
select 0, 3000 as analysis_id,CAST(po1.procedure_source_CONCEPT_ID AS STRING) as stratum_1,'Procedure' as stratum_3,
COUNT(distinct po1.PERSON_ID) as count_value,
COUNT(distinct po1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` po1 where po1.procedure_CONCEPT_ID!=po1.procedure_source_CONCEPT_ID
group by po1.procedure_source_CONCEPT_ID"

#  600 Gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3101 as analysis_id,
	CAST(co1.procedure_CONCEPT_ID AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Procedure' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co2
	on p2.person_id = co2.person_id where co2.procedure_source_concept_id=co1.procedure_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where co1.procedure_concept_id > 0
group by co1.procedure_concept_id,
	p1.gender_concept_id
union all
select 0, 3101 as analysis_id,CAST(co1.procedure_source_CONCEPT_ID AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,'Procedure' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1 on p1.person_id = co1.person_id
where co1.procedure_concept_id != co1.procedure_source_concept_id
group by co1.procedure_source_concept_id,p1.gender_concept_id"


# 600 age
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value,source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.procedure_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Procedure' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co2 on p2.person_id = co2.person_id where co2.procedure_source_concept_id=co1.procedure_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) >=3
and co1.procedure_concept_id > 0
group by co1.procedure_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.procedure_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Procedure' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.procedure_date) - p1.year_of_birth)/10) >=3 and co1.procedure_concept_id != co1.procedure_source_concept_id
group by co1.procedure_source_concept_id, stratum_2"

# 600 age 18 to 29
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3,count_value,source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.procedure_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Procedure' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co2 on p2.person_id = co2.person_id where co2.procedure_source_concept_id=co1.procedure_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.procedure_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.procedure_date) - p1.year_of_birth) < 30
and co1.procedure_concept_id > 0
group by co1.procedure_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.procedure_source_concept_id AS STRING) as stratum_1,'2' as stratum_2,
'Procedure' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.procedure_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.procedure_date) - p1.year_of_birth) < 30
group by co1.procedure_source_concept_id, stratum_2
"

# Drugs
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(de1.drug_CONCEPT_ID AS STRING) as stratum_1,'Drug' as stratum_3,
	COUNT(distinct de1.PERSON_ID) as count_value,(select COUNT(distinct de2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` de2 where de2.drug_source_concept_id=de1.drug_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` de1
where de1.drug_concept_id > 0
group by de1.drug_CONCEPT_ID
union all
select 0, 3000 as analysis_id,CAST(de1.drug_source_CONCEPT_ID AS STRING) as stratum_1,'Drug' as stratum_3,
COUNT(distinct de1.PERSON_ID) as count_value,
COUNT(distinct de1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` de1 where de1.drug_concept_id != de1.drug_source_concept_id
group by de1.drug_source_CONCEPT_ID"

# Drug gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3101 as analysis_id,
	CAST(co1.drug_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Drug' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co2
	on p2.person_id = co2.person_id where co2.drug_source_concept_id=co1.drug_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where co1.drug_concept_id > 0
group by co1.drug_concept_id,
	p1.gender_concept_id
union all
select 0, 3101 as analysis_id,CAST(co1.drug_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,'Drug' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where co1.drug_concept_id != co1.drug_source_concept_id
group by co1.drug_source_concept_id,p1.gender_concept_id"

# Drug age
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,CAST(co1.drug_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Drug' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co2 on p2.person_id = co2.person_id where co2.drug_source_concept_id=co1.drug_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) >=3
and co1.drug_concept_id > 0
group by co1.drug_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.drug_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Drug' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where floor((extract(year from co1.drug_exposure_start_date) - p1.year_of_birth)/10) >=3 and co1.drug_concept_id != co1.drug_source_concept_id
group by co1.drug_source_concept_id, stratum_2"

# Drug age decile
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.drug_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Drug' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct p2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p2 inner join \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co2 on p2.person_id = co2.person_id where co2.drug_source_concept_id=co1.drug_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) < 30
and co1.drug_concept_id > 0
group by co1.drug_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.drug_source_concept_id AS STRING) as stratum_1,'2' as stratum_2,'Drug' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co1
on p1.person_id = co1.person_id
where (extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) >= 18 and
(extract(year from co1.drug_exposure_start_date) - p1.year_of_birth) < 30 and co1.drug_concept_id != drug_source_concept_id
group by co1.drug_source_concept_id, stratum_2"

# 800	(3000) Number of persons with at least one observation occurrence, by observation_concept_id
echo "Querying observation"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(co1.observation_CONCEPT_ID AS STRING) as stratum_1,'Observation' as stratum_3,
	COUNT(distinct co1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co2 where co2.observation_source_concept_id=co1.observation_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
where co1.observation_concept_id > 0
group by co1.observation_CONCEPT_ID
union all
select 0, 3000 as analysis_id,CAST(co1.observation_source_CONCEPT_ID AS STRING) as stratum_1,'Observation' as stratum_3,
COUNT(distinct co1.PERSON_ID) as count_value,
COUNT(distinct co1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1 where co1.observation_source_concept_id > 0 and co1.observation_concept_id != co1.observation_source_concept_id
group by co1.observation_source_CONCEPT_ID"

# Observation 3101 concept by gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3101 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Observation' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co2 where co2.observation_source_concept_id=co1.observation_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0
group by co1.observation_concept_id, p1.gender_concept_id
union all
select 0, 3101 as analysis_id,
	CAST(co1.observation_source_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Observation' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_source_concept_id > 0 and co1.observation_concept_id != co1.observation_source_concept_id
group by co1.observation_source_concept_id, p1.gender_concept_id"

# Observation (3102)	Number of persons with   concept id by  age decile  30+ yr old deciles
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Observation' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co2 where co2.observation_source_concept_id=co1.observation_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0 and floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) >=3
group by co1.observation_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.observation_source_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Observation' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_source_concept_id > 0 and floor((extract(year from co1.observation_date) - p1.year_of_birth)/10) >=3 and co1.observation_concept_id != co1.observation_source_concept_id
group by co1.observation_source_concept_id, stratum_2
"

# Observation (3102)	Number of persons with concept id by  age decile  18-29 yr old decile 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.observation_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Observation' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.PERSON_ID) from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co2 where co2.observation_source_concept_id=co1.observation_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0 and (extract(year from co1.observation_date) - p1.year_of_birth) >= 18 and (extract(year from co1.observation_date) - p1.year_of_birth) < 30
group by co1.observation_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,CAST(co1.observation_source_concept_id AS STRING) as stratum_1,
'2' as stratum_2,'Observation' as stratum_3,
COUNT(distinct p1.PERSON_ID) as count_value,COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1
on p1.person_id = co1.person_id
where co1.observation_concept_id > 0 and (extract(year from co1.observation_date) - p1.year_of_birth) >= 18 and (extract(year from co1.observation_date) - p1.year_of_birth) < 30 and co1.observation_concept_id != co1.observation_source_concept_id
group by co1.observation_source_concept_id, stratum_2
"

# 3000 Measurements - Number of persons with at least one measurement occurrence, by measurement_concept_id
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4,stratum_5, count_value, source_count_value)
select 0, 3000 as analysis_id, CAST(co1.measurement_concept_id  AS STRING) as stratum_1,
cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) AS STRING) as stratum_2,
'Measurement' as stratum_3,
cast(floor(min(co1.value_as_number)) AS STRING) as stratum_4,
cast(ceil(max(co1.value_as_number)) AS STRING) as stratum_5,
COUNT(distinct co1.person_id) as count_value,
(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_concept_id > 0
and co1.value_as_number is not null
group by  co1.measurement_concept_id
union all
select 0, 3000 as analysis_id, CAST(co1.measurement_source_concept_id  AS STRING) as stratum_1,
cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) AS STRING) as stratum_2,
'Measurement' as stratum_3,
cast(floor(min(co1.value_as_number)) AS STRING) as stratum_4,
cast(ceil(max(co1.value_as_number)) AS STRING) as stratum_5,
COUNT(distinct co1.person_id) as count_value,
COUNT(distinct co1.person_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_source_concept_id > 0 and co1.measurement_concept_id != co1.measurement_source_concept_id
and co1.value_as_number is not null
group by  co1.measurement_source_concept_id"

# Measurement concept by gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3101 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0
group by co1.measurement_concept_id, p1.gender_concept_id
union all
select 0, 3101 as analysis_id,
	CAST(co1.measurement_source_concept_id AS STRING) as stratum_1,
	CAST(p1.gender_concept_id AS STRING) as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_source_concept_id > 0 and co1.measurement_concept_id!=co1.measurement_source_concept_id
group by co1.measurement_source_concept_id, p1.gender_concept_id
"

# Measurement by age deciles
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0 and floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) >=3
group by co1.measurement_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,
	CAST(co1.measurement_source_concept_id AS STRING) as stratum_1,
	CAST(floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_source_concept_id > 0 and floor((extract(year from co1.measurement_date) - p1.year_of_birth)/10) >=3 and co1.measurement_concept_id!=co1.measurement_source_concept_id
group by co1.measurement_source_concept_id, stratum_2"

# Measurement  18-29 yr old decile 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, count_value, source_count_value)
select 0, 3102 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_concept_id > 0 and (extract(year from co1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from co1.measurement_date) - p1.year_of_birth) < 30
group by co1.measurement_concept_id, stratum_2
union all
select 0, 3102 as analysis_id,
	CAST(co1.measurement_source_concept_id AS STRING) as stratum_1,
	'2' as stratum_2,'Measurement' as stratum_3,
	COUNT(distinct p1.PERSON_ID) as count_value,
	COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
on p1.person_id = co1.person_id
where co1.measurement_source_concept_id > 0 and (extract(year from co1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from co1.measurement_date) - p1.year_of_birth) < 30 and co1.measurement_concept_id!=co1.measurement_source_concept_id
group by co1.measurement_source_concept_id, stratum_2
"

# Set the survey answer count for all the survey questions that belong to each module
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,count_value,source_count_value)
SELECT 0 as id, 3110 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,CAST(o.observation_source_concept_id as string) as stratum_2,
CAST(o.value_source_concept_id as string) as stratum_3,o.value_as_string as stratum_4,
Count(*) as count_value, 0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
On o.observation_source_concept_id=cr.concept_id_1 and cr.relationship_id = 'Has Module'
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2 = dbd.concept_id
Where (o.observation_source_concept_id > 0 and o.value_source_concept_id > 0) and (dbd.db_type = 'survey' and dbd.concept_id <> 0)
Group by o.observation_source_concept_id,o.value_source_concept_id,o.value_as_string,dbd.concept_id"

# Set the survey answer count for all the survey questions that belong to each module(value as number)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_4,count_value,source_count_value)
SELECT 0 as id, 3110 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,CAST(o.observation_source_concept_id as string) as stratum_2,
CAST(o.value_as_number as string) as stratum_4,Count(*) as count_value,0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr
On o.observation_source_concept_id=cr.concept_id_1 and cr.relationship_id = 'Has Module'
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2 = dbd.concept_id
Where (o.observation_source_concept_id > 0 and o.value_as_number is not null) and (dbd.db_type = 'survey' and dbd.concept_id <> 0)
and (cr.concept_id_1 != 1585966 and o.value_as_number > 0)
Group by o.observation_source_concept_id,o.value_as_number,dbd.concept_id"


# Survey question answers count by gender
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
select 0,3111 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,CAST(o.observation_source_concept_id as string) as stratum_2,
CAST(o.value_source_concept_id as string) as stratum_3,o.value_as_string as stratum_4,
CAST(p.gender_concept_id as string) as stratum_5,count(distinct p.person_id) as count_value,0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join  \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id=cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_source_concept_id > 0)
group by dbd.concept_id,o.observation_source_concept_id,o.value_source_concept_id,o.value_as_number,o.value_as_string,p.gender_concept_id"


# Survey question answers count by gender(value_as_number not null)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_4,stratum_5,count_value,source_count_value)
select 0,3111 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,CAST(o.observation_source_concept_id as string) as stratum_2,
CAST(o.value_as_number as string) as stratum_4,CAST(p.gender_concept_id as string) as stratum_5,count(distinct p.person_id) as count_value,0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join  \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id=cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id
where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_as_number is not null)
and (o.observation_source_concept_id != 1585966)
group by dbd.concept_id,o.observation_source_concept_id,o.value_as_number,p.gender_concept_id"


# Survey Question Answer Count by age decile  30+ yr old deciles
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
select 0, 3112 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,CAST(o.observation_source_concept_id as string) as stratum_2,
CAST(o.value_source_concept_id as string) as stratum_3,o.value_as_string as stratum_4,
CAST(floor((extract(year from o.observation_date) - p.year_of_birth)/10) AS STRING) as stratum_5,COUNT(distinct p.PERSON_ID) as count_value,0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id = cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_source_concept_id > 0)
and floor((extract(year from o.observation_date) - p.year_of_birth)/10) >=3
group by dbd.concept_id,o.observation_source_concept_id,o.value_source_concept_id,o.value_as_string,stratum_5"


# Survey Question Answer Count by age decile  18-29 yr old decile 2
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
select 0, 3112 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,
CAST(o.observation_source_concept_id as string) as stratum_2,CAST(o.value_source_concept_id as string) as stratum_3,
o.value_as_string as stratum_4,
'2' as stratum_5,COUNT(distinct p.PERSON_ID) as count_value,0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id = cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id
where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_source_concept_id > 0)
and ((extract(year from o.observation_date) - p.year_of_birth) >= 18 and (extract(year from o.observation_date) - p.year_of_birth) < 30)
group by dbd.concept_id,o.observation_source_concept_id,o.value_source_concept_id,o.value_as_string,stratum_5"


# Survey Question Answer Count by age decile  30+ yr old deciles(value as number not null)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_4,stratum_5,count_value,source_count_value)
select 0, 3112 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,
CAST(o.observation_source_concept_id as string) as stratum_2,CAST(o.value_as_number as string) as stratum_4,
CAST(floor((extract(year from o.observation_date) - p.year_of_birth)/10) AS STRING) as stratum_5,COUNT(distinct p.PERSON_ID) as count_value,0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id = cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id
where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_as_number is not null)
and (o.observation_source_concept_id != 1585966)
and floor((extract(year from o.observation_date) - p.year_of_birth)/10) >=3
group by dbd.concept_id,o.observation_source_concept_id,o.value_as_number,stratum_5"


# Survey Question Answer Count by age decile  18-29 yr old decile 2(value as number not null)
# Excluded Zipcode question (1585966)
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2,stratum_4,stratum_5,count_value,source_count_value)
select 0, 3112 as analysis_id,CAST(dbd.concept_id as string) as stratum_1,
CAST(o.observation_source_concept_id as string) as stratum_2,CAST(o.value_as_number as string) as stratum_4,
'2' as stratum_5,COUNT(distinct p.PERSON_ID) as count_value,0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p inner join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` o on p.person_id = o.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept_relationship\` cr on o.observation_source_concept_id = cr.concept_id_1
join  \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\` dbd on cr.concept_id_2=dbd.concept_id
where (dbd.db_type='survey' and dbd.concept_id <> 0) and (o.observation_source_concept_id > 0 and o.value_as_number is not null)
and (o.observation_source_concept_id != 1585966)
and ((extract(year from o.observation_date) - p.year_of_birth) >= 18 and (extract(year from o.observation_date) - p.year_of_birth) < 30)
group by dbd.concept_id,o.observation_source_concept_id,o.value_as_number,stratum_5"

#Change concept ids from 0 to null
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.db_domain\`
set concept_id=null where concept_id=0"

# Domain participant counts
echo "Getting domain participant counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
 select 0 as id,3000 as analysis_id,'19' as stratum_1,'Condition' as stratum_3, COUNT(distinct ob.person_id) as count_value, 0 as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
 select 0 as id,3000 as analysis_id,'13' as stratum_1,'Drug' as stratum_3, COUNT(distinct d.person_id) as count_value, 0 as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` d"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
 select 0 as id,3000 as analysis_id,'21' as stratum_1,'Measurement' as stratum_3, COUNT(distinct m.person_id) as count_value, 0 as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m"

bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
 (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
 select 0 as id,3000 as analysis_id,'10' as stratum_1,'Procedure' as stratum_3, COUNT(distinct p.person_id) as count_value, 0 as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` p"


# 1815 Measurement response distribution
echo "Getting measurement response distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id, analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value,
p75_value, p90_value)
with rawdata_1815 as
(select measurement_concept_id as subject_id, unit_concept_id, cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m where m.unit_concept_id is not null and m.value_as_number is not null
),
overallstats as
(select subject_id as stratum1_id, unit_concept_id as stratum2_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1815 group by 1,2
),
statsview as
(select subject_id as stratum1_id, unit_concept_id as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, unit_concept_id order by count_value) as rn from rawdata_1815 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
  join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
)
select 0 as id, 1815 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1815 Measurement response distribution
echo "Getting measurement response distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id, analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value,
p75_value, p90_value)
with rawdata_1815 as
(select measurement_source_concept_id as subject_id, unit_concept_id, cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m where m.unit_concept_id is not null and m.value_as_number is not null
and m.measurement_concept_id != m.measurement_source_concept_id
),
overallstats as
(select subject_id as stratum1_id, unit_concept_id as stratum2_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1815 group by 1,2
),
statsview as
(select subject_id as stratum1_id, unit_concept_id as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, unit_concept_id order by count_value) as rn from rawdata_1815 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
  join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
)
select 0 as id, 1815 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1806 Measurement response age distribution
echo "Getting measurement response age distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value)
WITH rawdata_1806 AS
(SELECT o1.measurement_concept_id as subject_id, p1.gender_concept_id, o1.measurement_start_year - p1.year_of_birth as count_value
FROM  \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
(select  person_id, measurement_concept_id, min(EXTRACT(YEAR from measurement_date)) as measurement_start_year from  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` group by  1, 2 ) o1
on p1.person_id = o1.person_id),
overallstats  as
( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, cast(avg(1.0 * count_value)  as float64)  as avg_value,
cast(STDDEV(count_value)  as float64)  as stdev_value, min(count_value)  as min_value, max(count_value)  as max_value,
COUNT(*)  as total   from  rawdata_1806 group by  1, 2 ),
statsview  as
(select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, count_value as count_value, COUNT(*)  as total, row_number() over (partition by subject_id, gender_concept_id order by count_value)  as rn   from  rawdata_1806
 group by  1, 2, 3 ),
priorstats  as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total)  as accumulated   from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn)
select 0 as id, 1806 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by  o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1806 Measurement response age distribution (source concepts)
echo "Getting measurement response age distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id, stratum_1, stratum_2, count_value, min_value, max_value, avg_value, stdev_value, median_value, p10_value, p25_value, p75_value, p90_value)
WITH rawdata_1806 AS
(SELECT o1.measurement_source_concept_id as subject_id, p1.gender_concept_id, o1.measurement_start_year - p1.year_of_birth as count_value
  FROM  \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1
inner join
(select  person_id, measurement_source_concept_id, min(EXTRACT(YEAR from measurement_date)) as measurement_start_year from  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`
where measurement_concept_id != measurement_source_concept_id group by  1, 2 ) o1
on p1.person_id = o1.person_id),
overallstats  as ( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, cast(avg(1.0 * count_value)  as float64)  as avg_value, cast(STDDEV(count_value)  as float64)  as stdev_value, min(count_value)  as min_value, max(count_value)  as max_value, COUNT(*)  as total   from  rawdata_1806 group by  1, 2 ),
statsview  as ( select  subject_id  as stratum1_id, gender_concept_id  as stratum2_id, count_value as count_value, COUNT(*)  as total, row_number() over (partition by subject_id, gender_concept_id order by count_value)  as rn   from  rawdata_1806
 group by  1, 2, 3 ),
priorstats  as ( select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.count_value as count_value, s.total as total, sum(p.total)  as accumulated   from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn)
select 0 as id, 1806 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by  o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"


# 1900 Measurement value counts
echo "Getting measurements value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value,source_count_value)
select 0,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_3,
CAST((case when cast(ar.stratum_4 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_4 as int64) then cast(ar.stratum_4 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_4 as int64)) * cast(ar.stratum_4 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
m1.unit_source_value as stratum_5,
count(distinct p1.person_id) as count_value,
(select COUNT(distinct m2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m2 where m2.measurement_source_concept_id=m1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c1 on
m1.measurement_concept_id = c1.concept_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_concept_id AS STRING)=ar.stratum_1
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and c1.domain_id='Measurement' and c1.vocabulary_id='PPI' and c1.concept_class_id not in ('Question','Answer')
and ar.analysis_id=3000 and ar.stratum_3='Measurement'
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_3,
CAST((case when cast(ar.stratum_4 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_4 as int64) then cast(ar.stratum_4 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_4 as int64)) * cast(ar.stratum_4 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
m1.unit_source_value as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c1 on m1.measurement_source_concept_id = c1.concept_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_source_concept_id AS STRING)=ar.stratum_1
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and m1.value_as_number is not null
and c1.domain_id = 'Measurement' and c1.vocabulary_id = 'PPI' and c1.concept_class_id not in ('Question','Answer')
and ar.analysis_id=3000 and ar.stratum_3='Measurement'
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5"

# 1900 Measurement value counts (18-29 yr old decile 2)
echo "Getting measurements value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value,source_count_value)
select 0,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
'2' as stratum_3,
CAST((case when cast(ar.stratum_4 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_4 as int64) then cast(ar.stratum_4 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_4 as int64)) * cast(ar.stratum_4 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
m1.unit_source_value as stratum_5,
count(distinct p1.person_id) as count_value,
(select COUNT(distinct m2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m2 where m2.measurement_source_concept_id=m1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c1 on
m1.measurement_concept_id = c1.concept_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar
on cast(m1.measurement_concept_id AS STRING)=ar.stratum_1
and ar.analysis_id=3000 and ar.stratum_3='Measurement'
and m1.value_as_number is not null
and c1.domain_id = 'Measurement' and c1.vocabulary_id = 'PPI' and c1.concept_class_id not in ('Question','Answer')
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_concept_id,stratum_2,stratum_4,stratum_5
union all
select 0, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
'2' as stratum_3,
CAST((case when cast(ar.stratum_4 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_4 as int64) then cast(ar.stratum_4 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_4 as int64)) * cast(ar.stratum_4 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
m1.unit_source_value as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c1 on
m1.measurement_source_concept_id = c1.concept_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar
on cast(m1.measurement_source_concept_id AS STRING)=ar.stratum_1
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and ar.analysis_id=3000 and ar.stratum_3='Measurement'
and m1.value_as_number is not null
and c1.domain_id = 'Measurement' and c1.vocabulary_id = 'PPI' and c1.concept_class_id not in ('Question','Answer')
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_source_concept_id,stratum_2,stratum_4,stratum_5"

# 1900 Measurement value counts (String value measurement response distribution)
echo "Getting measurements value counts for measurements that have string values"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value,source_count_value)
with concept_with_string_values as
(select distinct m.measurement_concept_id from
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on m.measurement_concept_id = c.concept_id
where c.domain_id = 'Measurement' and c.vocabulary_id = 'PPI'
and c.concept_class_id not in ('Question','Answer')
and m.value_as_number is null and m.value_source_value is not null
and m.measurement_concept_id > 0),
source_concept_with_string_values as
(select distinct m.measurement_source_concept_id from
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on m.measurement_source_concept_id = c.concept_id
where c.domain_id = 'Measurement' and c.vocabulary_id = 'PPI'
and c.concept_class_id not in ('Question','Answer')
and m.measurement_source_concept_id > 0 and m.measurement_concept_id != m.measurement_source_concept_id
and m.value_as_number is null and m.value_source_value is not null)
select 0,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_3,
m1.value_source_value as stratum_4,
m1.unit_source_value as stratum_5,
count(distinct p1.person_id) as count_value,
(select COUNT(distinct m2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m2 where m2.measurement_source_concept_id=m1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join concept_with_string_values cv on m1.measurement_concept_id = cv.measurement_concept_id
where floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and m1.value_source_value is not null
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_3,
m1.value_source_value as stratum_4,
m1.unit_source_value as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join source_concept_with_string_values scv on m1.measurement_source_concept_id = scv.measurement_source_concept_id
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_source_value is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5"

# 1900 Measurement value counts (String value measurement response distribution)
echo "Getting measurements value counts for measurements that have string values"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, stratum_5, count_value,source_count_value)
with concept_with_string_values as
(select distinct m.measurement_concept_id from
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on m.measurement_concept_id = c.concept_id
where c.domain_id = 'Measurement' and c.vocabulary_id = 'PPI'
and c.concept_class_id not in ('Question','Answer')
and m.value_as_number is null and m.value_source_value is not null
and m.measurement_concept_id > 0),
source_concept_with_string_values as
(select distinct m.measurement_source_concept_id from
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on m.measurement_source_concept_id = c.concept_id
where c.domain_id = 'Measurement' and c.vocabulary_id = 'PPI'
and c.concept_class_id not in ('Question','Answer')
and m.measurement_source_concept_id > 0 and m.measurement_concept_id != m.measurement_source_concept_id
and m.value_as_number is null and m.value_source_value is not null)
select 0,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
'2' as stratum_3,
m1.value_source_value as stratum_4,
m1.unit_source_value as stratum_5,
count(distinct p1.person_id) as count_value,
(select COUNT(distinct m2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m2 where m2.measurement_source_concept_id=m1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join concept_with_string_values cv on m1.measurement_concept_id = cv.measurement_concept_id
where (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
and m1.value_source_value is not null
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
'2' as stratum_3,
m1.value_source_value as stratum_4,
m1.unit_source_value as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join source_concept_with_string_values scv on m1.measurement_source_concept_id = scv.measurement_source_concept_id
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_source_value is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5"
