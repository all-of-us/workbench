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

#Get the list of tables in the dataset
tables=$(bq --project=$BQ_PROJECT --dataset=$BQ_DATASET ls)

# Next Populate achilles_results
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


# 6	Number of persons by age decile
echo "Getting age decile count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 6 as analysis_id,  CAST(floor((2019 - year_of_birth)/10) AS STRING) as stratum_2,
COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
where floor((2019 - year_of_birth)/10) >=3
group by stratum_2"

# 7 Gender identity count
echo "Getting gender identity count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, stratum_2, count_value,source_count_value)
select 0, 7 as analysis_id, CAST(ob.value_source_concept_id as STRING) as stratum_1, c.concept_name as stratum_2, count(distinct p.person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob on p.person_id=ob.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on c.concept_id=ob.value_source_concept_id
where ob.observation_source_concept_id=1585838
group by ob.value_source_concept_id, c.concept_name"

# 8 Getting race/ ethnicity counts from ppi data (Answers to gender/ ethnicity question in survey)
echo "Getting race ethnicity counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, stratum_2, count_value,source_count_value)
select 0, 8 as analysis_id, CAST(ob.value_source_concept_id as STRING) as stratum_1, c.concept_name as stratum_2, count(distinct p.person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p join \`${BQ_PROJECT}.${BQ_DATASET}.observation\` ob on p.person_id=ob.person_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on c.concept_id=ob.value_source_concept_id
where ob.observation_source_concept_id=1586140
group by ob.value_source_concept_id, c.concept_name"

# Age decile 1 count
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` (id, analysis_id, stratum_1, count_value,source_count_value)
select 0, 6 as analysis_id,  '2' as stratum_2,
COUNT(distinct person_id) as count_value, 0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
where (2019 - year_of_birth) >= 18 and (2019 - year_of_birth) < 30"

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
select 0, 12 as analysis_id, CAST(RACE_CONCEPT_ID AS STRING) as stratum_1, CAST(ETHNICITY_CONCEPT_ID AS STRING) as stratum_2, COUNT(distinct person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\`
group by RACE_CONCEPT_ID,ETHNICITY_CONCEPT_ID"

# 200	(3000 ) Number of persons with at least one visit occurrence, by visit_concept_id
echo "Getting visit count and source count"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0 as id,3000 as analysis_id,CAST(vo1.visit_concept_id AS STRING) as stratum_1,'Visit' as stratum_3,
COUNT(distinct vo1.PERSON_ID) as count_value,(select COUNT(distinct vo2.person_id) from
\`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo2 where vo2.visit_source_concept_id=vo1.visit_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo1
where vo1.visit_concept_id > 0
group by vo1.visit_concept_id
union all
select 0 as id,3000 as analysis_id,CAST(vo1.visit_source_concept_id AS STRING) as stratum_1,'Visit' as stratum_3,
COUNT(distinct vo1.PERSON_ID) as count_value,COUNT(distinct vo1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\` vo1
where vo1.visit_source_concept_id not in (select distinct visit_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.visit_occurrence\`)
group by vo1.visit_source_concept_id"


# 400 (3000)	Number of persons with at least one condition occurrence, by condition_concept_id
# There was weird data in combined20181025 that has rows in condition occurrence table with concept id 19 which was causing problem while fetching domain participant count
# so added check in there (Remove after checking the data is proper)
echo "Querying condition_occurrence ..."
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
CAST(co1.condition_CONCEPT_ID AS STRING) as stratum_1,'Condition' as stratum_3,
COUNT(distinct co1.PERSON_ID) as count_value, (select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co2
where co2.condition_source_concept_id=co1.condition_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
where co1.condition_concept_id > 0 and co1.condition_concept_id != 19
group by co1.condition_CONCEPT_ID
union all
select 0 as id,3000 as analysis_id,CAST(co1.condition_source_concept_id AS STRING) as stratum_1,'Condition' as stratum_3,
COUNT(distinct co1.PERSON_ID) as count_value,COUNT(distinct co1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co1
where co1.condition_source_concept_id not in (select distinct condition_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\`)
and co1.condition_source_concept_id != 19
group by co1.condition_source_concept_id"

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
## Death (3102)	Number of persons with a death by death cause concept id by  age decile  <20 yr old decile 1 */
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
COUNT(distinct po1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` po1 where
po1.procedure_source_CONCEPT_ID not in (select distinct procedure_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\`)
group by po1.procedure_source_CONCEPT_ID"

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
COUNT(distinct de1.PERSON_ID) as source_count_value from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` de1 where
de1.drug_source_concept_id not in (select distinct drug_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\`)
group by de1.drug_source_CONCEPT_ID"

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
from \`${BQ_PROJECT}.${BQ_DATASET}.observation\` co1 where co1.observation_source_concept_id > 0 and
co1.observation_source_concept_id not in (select distinct observation_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.observation\`)
group by co1.observation_source_CONCEPT_ID"

# 3000 Measurements that have numeric values - Number of persons with at least one measurement occurrence by measurement_concept_id, bin size of the measurement value for 10 bins, maximum and minimum from measurement value. Added value for measurement rows
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
select 0, 3000 as analysis_id,
	CAST(co1.measurement_concept_id AS STRING) as stratum_1,
  'Measurement' as stratum_3,
	COUNT(distinct co1.PERSON_ID) as count_value, (select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2
	where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_concept_id > 0
group by co1.measurement_concept_id
union all
 select 0 as id,3000 as analysis_id,CAST(co1.measurement_source_concept_id AS STRING) as stratum_1,
  'Measurement' as stratum_3,
 COUNT(distinct co1.PERSON_ID) as count_value,COUNT(distinct co1.PERSON_ID) as source_count_value
 from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
 where co1.measurement_source_concept_id not in (select distinct measurement_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`)
 group by co1.measurement_source_concept_id"

if [[ "$tables" == *"_mapping_"* ]]; then
    ### Mapping tables has the ehr fetched records linked to the dataset named 'ehr'. So, joining on the mapping tables to fetch only ehr concepts.
    # Condition Domain participant counts
    echo "Getting condition domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, stratum_5, count_value, source_count_value)
    with condition_concepts as
    (select condition_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.condition_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_condition_occurrence\` mm on co.condition_occurrence_id =mm.condition_occurrence_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_condition_occurrence\` where src_dataset_id like '%ehr%')),
    condition_source_concepts as
    (select condition_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.condition_source_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_condition_occurrence\` mm on co.condition_occurrence_id=mm.src_condition_occurrence_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_condition_occurrence\` where src_dataset_id like '%ehr%')
    and co.condition_source_concept_id not in (select distinct condition_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\`)),
    concepts as
    (select * from condition_concepts union all select * from condition_source_concepts)
    select 0 as id,3000 as analysis_id,'19' as stratum_1,'Condition' as stratum_3,
    CAST((select count(distinct person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\`) as STRING) as stratum_5,
    (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"

    echo "Getting drug domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, stratum_5, count_value, source_count_value)
    with drug_concepts as
    (select drug_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.drug_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_drug_exposure\` mde on mde.drug_exposure_id=co.drug_exposure_id
    where c.vocabulary_id != 'PPI' and mde.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_drug_exposure\` where src_dataset_id like '%ehr%') ),
    drug_source_concepts as
    (select drug_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.drug_source_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_drug_exposure\` mde on mde.src_drug_exposure_id=co.drug_exposure_id
    where c.vocabulary_id != 'PPI' and mde.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_drug_exposure\` where src_dataset_id like '%ehr%')
    and co.drug_source_concept_id not in (select distinct drug_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\`)),
    concepts as
    (select * from drug_concepts union all select * from drug_source_concepts)
    select 0 as id,3000 as analysis_id,'13' as stratum_1,'Drug' as stratum_3,
    CAST((select count(distinct person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\`) as STRING) as stratum_5,
    (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"

    echo "Getting measurement domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, stratum_5, count_value, source_count_value)
    with measurement_concepts as
    (select measurement_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_measurement\` mm on co.measurement_id=mm.measurement_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_measurement\` where src_dataset_id like '%ehr%')),
    measurement_source_concepts as
    (select measurement_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_source_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_measurement\` mm on co.measurement_id=mm.src_measurement_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_measurement\` where src_dataset_id like '%ehr%')
    and co.measurement_source_concept_id not in (select distinct measurement_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`)),
    concepts as
    (select * from measurement_concepts union all select * from measurement_source_concepts)
    select 0 as id,3000 as analysis_id,'21' as stratum_1,'Measurement' as stratum_3,
    CAST((select count(distinct person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`) as STRING) as stratum_5,
    (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"

    echo "Getting procedure domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, stratum_5, count_value, source_count_value)
    with procedure_concepts as
    (select procedure_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.procedure_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_procedure_occurrence\` mm on co.procedure_occurrence_id =mm.procedure_occurrence_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_procedure_occurrence\` where src_dataset_id like '%ehr%')),
    procedure_source_concepts as
    (select procedure_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.procedure_source_concept_id=c.concept_id
    join \`${BQ_PROJECT}.${BQ_DATASET}._mapping_procedure_occurrence\` mm on co.procedure_occurrence_id=mm.src_procedure_occurrence_id
    where c.vocabulary_id != 'PPI' and mm.src_dataset_id=(select distinct src_dataset_id from \`${BQ_PROJECT}.${BQ_DATASET}._mapping_procedure_occurrence\` where src_dataset_id like '%ehr%')
    and co.procedure_source_concept_id not in (select distinct procedure_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\`)),
    concepts as
    (select * from procedure_concepts union all select * from procedure_source_concepts)
    select 0 as id,3000 as analysis_id,'10' as stratum_1,'Procedure' as stratum_3,
    CAST((select count(distinct person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\`) as STRING) as stratum_5,
    (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"
else
    ### Test data does not have the mapping tables, so this else block lets the script to fetch domain counts for test data
    # Condition Domain participant counts
    echo "Getting condition domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
    with condition_concepts as
    (select condition_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.condition_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI'),
    condition_source_concepts as
    (select condition_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.condition_source_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI'
    and co.condition_source_concept_id not in (select distinct condition_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.condition_occurrence\`)),
    concepts as
    (select * from condition_concepts union all select * from condition_source_concepts)
    select 0 as id,3000 as analysis_id,'19' as stratum_1,'Condition' as stratum_3,(select count(distinct person) from concepts) as count_value,
    0 as source_count_value"

    echo "Getting drug domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
    with drug_concepts as
    (select drug_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.drug_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI'),
    drug_source_concepts as
    (select drug_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.drug_source_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI'
    and co.drug_source_concept_id not in (select distinct drug_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.drug_exposure\`)),
    concepts as
    (select * from drug_concepts union all select * from drug_source_concepts)
    select 0 as id,3000 as analysis_id,'13' as stratum_1,'Drug' as stratum_3, (select count(distinct person) from concepts) as count_value, 0 as source_count_value"

    echo "Getting measurement domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
    with measurement_concepts as
    (select measurement_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI' and co.measurement_concept_id not in (3036277,903118,903115,3025315,903135,903136,903126,903111,42528957)),
    measurement_source_concepts as
    (select measurement_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_source_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI' and co.measurement_source_concept_id not in (3036277,903133,903118,903115,3025315,903121,903135,903136,903126,903111,42528957,903120)
    and co.measurement_source_concept_id not in (select distinct measurement_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`)),
    concepts as
    (select * from measurement_concepts union all select * from measurement_source_concepts)
    select 0 as id,3000 as analysis_id,'21' as stratum_1,'Measurement' as stratum_3, (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"

    echo "Getting participant domain participant counts"
    bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
    "insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
    (id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
    with procedure_concepts as
    (select procedure_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.procedure_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI' and c.concept_id not in (3036277,903118,903115,3025315,903135,903136,903126,903111,42528957)),
    procedure_source_concepts as
    (select procedure_source_concept_id as concept,co.person_id as person
    from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.procedure_source_concept_id=c.concept_id
    where c.vocabulary_id != 'PPI' and c.concept_id not in (3036277,903133,903118,903115,3025315,903121,903135,903136,903126,903111,42528957,903120)
    and co.procedure_source_concept_id not in (select distinct procedure_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.procedure_occurrence\`)),
    concepts as
    (select * from procedure_concepts union all select * from procedure_source_concepts)
    select 0 as id,3000 as analysis_id,'10' as stratum_1,'Procedure' as stratum_3, (select count(distinct person) from concepts) as count_value,
    0 as source_count_value"
fi

echo "Getting physical measurements participant counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, count_value, source_count_value)
with pm_concepts as
(select measurement_concept_id as concept,co.person_id as person
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_concept_id=c.concept_id
where c.vocabulary_id != 'PPI' and c.concept_id not in (3036277,903118,903115,3025315,903135,903136,903126,903111,42528957)),
pm_source_concepts as
(select measurement_source_concept_id as concept,co.person_id as person
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on co.measurement_source_concept_id=c.concept_id
where c.vocabulary_id != 'PPI' and c.concept_id in (3036277,903133,903118,903115,3025315,903121,903135,903136,903126,903111,42528957,903120)
and co.measurement_source_concept_id not in (select distinct measurement_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\`)),
concepts as
(select * from pm_concepts union all select * from pm_source_concepts)
select 0 as id,3000 as analysis_id,'0' as stratum_1,'Physical Measurement' as stratum_3, (select count(distinct person) from concepts) as count_value, 0 as source_count_value"
