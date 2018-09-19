#!/bin/bash

# Runs measurement queries to populate count db of measurement data for cloudsql in BigQuery
set -xeuo pipefail
IFS=$'\n\t'

USAGE="./generate-clousql-cdr/run-measurement-queries.sh --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT>"
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
echo "Running measurement queries..."

# 3000 Measurements that have numeric values - Number of persons with at least one measurement occurrence by measurement_concept_id, bin size of the measurement value for 10 bins, maximum and minimum from measurement value. Added value for measurement rows
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, count_value, source_count_value)
with distinct_concepts as
(select distinct measurement_concept_id as m_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_concept_id != 0),
distinct_source_concepts as
(select distinct measurement_source_concept_id as m_s_concept_id from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_source_concept_id != 0 and measurement_concept_id != measurement_source_concept_id),
single_unit_concepts as
(select measurement_concept_id as m_concept_id,count(distinct unit_concept_id) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where (measurement_concept_id != 0 and unit_concept_id != 0) group by measurement_concept_id having cnt=1),
single_unit_source_concepts as
(select measurement_source_concept_id as m_s_concept_id,count(distinct unit_concept_id) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where (measurement_source_concept_id != 0 and measurement_concept_id != measurement_source_concept_id and unit_concept_id != 0)
group by measurement_source_concept_id having cnt=1),
single_unit_concept_name as
(select sc.m_concept_id as cid,c.concept_name as cname
from single_unit_concepts sc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on m.measurement_concept_id=sc.m_concept_id join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on c.concept_id=m.unit_concept_id where m.unit_concept_id != 0 group by sc.m_concept_id,c.concept_name),
single_unit_source_concept_name as
(select sc.m_s_concept_id as cid,c.concept_name as cname
from single_unit_source_concepts sc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on m.measurement_source_concept_id=sc.m_s_concept_id join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c
on c.concept_id=m.unit_concept_id where m.unit_concept_id != 0 group by sc.m_s_concept_id,c.concept_name),
single_unit_value_concepts as
(select measurement_concept_id as m_concept_id,count(distinct unit_source_value) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where (measurement_concept_id != 0 and unit_source_value is not null) group by measurement_concept_id having cnt=1),
single_unit_value_source_concepts as
(select measurement_source_concept_id as m_s_concept_id,count(distinct unit_source_value) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where (measurement_source_concept_id != 0 and measurement_concept_id != measurement_source_concept_id) group by measurement_source_concept_id having cnt=1),
single_unitvalue_concept_name as
(select sc.m_concept_id as cid,m.unit_source_value as cname
from single_unit_value_concepts sc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on m.measurement_concept_id=sc.m_concept_id group by sc.m_concept_id,m.unit_source_value),
single_unitvalue_source_concept_name as
(select sc.m_s_concept_id as cid,m.unit_source_value as cname
from single_unit_value_source_concepts sc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on m.measurement_source_concept_id=sc.m_s_concept_id group by sc.m_s_concept_id,m.unit_source_value),
measurement_units as
(select m_concept_id as concept,
(case when m_concept_id in (select distinct m_concept_id from single_unit_concepts) then (select distinct cname from single_unit_concept_name where cid=m_concept_id)
      when m_concept_id in (select distinct m_concept_id from single_unit_value_concepts) then (select distinct cname from single_unitvalue_concept_name where cid=m_concept_id)
      else 'unknown' end)
as unit
from distinct_concepts dc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on dc.m_concept_id=m.measurement_concept_id
group by m_concept_id,unit
union all
select m_s_concept_id as concept,
(case when m_s_concept_id in (select distinct m_s_concept_id from single_unit_source_concepts) then (select distinct cname from single_unit_source_concept_name where cid=m_s_concept_id)
      when m_s_concept_id in (select distinct m_s_concept_id from single_unit_value_source_concepts) then (select distinct cname from single_unitvalue_source_concept_name where cid=m_s_concept_id)
      else 'unknown' end)
as unit
from distinct_source_concepts dsc join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m on dsc.m_s_concept_id=m.measurement_source_concept_id
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on c.concept_id=m.unit_concept_id
group by m_s_concept_id,unit),
value_measurements as
(select distinct measurement_concept_id as concept from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where value_as_number is not null
union all
select distinct measurement_source_concept_id as concept from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where value_as_number is not null)
select 0,3000 as analysis_id,CAST(co1.measurement_concept_id  AS STRING) as stratum_1,
(case when co1.measurement_concept_id in (select distinct concept from value_measurements) then cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) as string)
      else '0' end) as stratum_2,
'Measurement' as stratum_3,unit as stratum_4,
COUNT(distinct co1.PERSON_ID) as count_value,
(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2
	where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1 join measurement_units on co1.measurement_concept_id=concept
where co1.measurement_concept_id > 0
group by  co1.measurement_concept_id,unit
union all
select 0, 3000 as analysis_id, CAST(co1.measurement_source_concept_id  AS STRING) as stratum_1,
(case when co1.measurement_source_concept_id in (select distinct concept from value_measurements) then cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) as string)
      else '0' end) as stratum_2,
'Measurement' as stratum_3,unit as stratum_4,
COUNT(distinct co1.PERSON_ID) as count_value,COUNT(distinct co1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1 join measurement_units on co1.measurement_source_concept_id=concept
where co1.measurement_source_concept_id > 0 and co1.measurement_concept_id != co1.measurement_source_concept_id
group by  co1.measurement_source_concept_id,unit"

# 1815 Measurement response by gender distribution
echo "Getting measurement response by gender distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1815 as
(select measurement_concept_id as subject_id, p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_concept_id != 0
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
union all
select measurement_source_concept_id as subject_id, p.gender_concept_id as gender,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
),
overallstats as
(select subject_id as stratum1_id, gender as stratum3_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1815 group by 1,2
),
statsview as
(select subject_id as stratum1_id, gender as stratum3_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,gender order by count_value) as rn from rawdata_1815 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id,s.stratum3_id as stratum3_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum3_id = p.stratum3_id
and p.rn <= s.rn
group by  s.stratum1_id, s.stratum3_id, s.count_value, s.total, s.rn
)
select 0 as id, 1815 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum3_id  AS STRING) as stratum3_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1814 Measurement response distribution
echo "Getting measurement response distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1814 as
(select measurement_concept_id as subject_id, cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_concept_id != 0
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
union all
select measurement_source_concept_id as subject_id, cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
),
overallstats as
(select subject_id as stratum1_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1814 group by 1
),
statsview as
(select subject_id as stratum1_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id order by count_value) as rn from rawdata_1814 group by 1,2
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id
and p.rn <= s.rn
group by  s.stratum1_id, s.count_value, s.total, s.rn
)
select 0 as id, 1814 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id
group by o.stratum1_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value"

# 1816 Measurement response by age at occurrence distribution
echo "Getting Measurement response by age at occurrence distribution"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_1816 as
(select measurement_concept_id as subject_id, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_concept_id != 0 and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
union all
select measurement_source_concept_id as subject_id, CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
union all
select measurement_concept_id as subject_id, '2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_concept_id != 0 and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
union all
select measurement_source_concept_id as subject_id, '2' as age_decile,
cast(value_as_number as float64) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.value_as_number is not null and m.measurement_source_concept_id != 0 and m.measurement_concept_id != m.measurement_source_concept_id
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
),
overallstats as
(select subject_id as stratum1_id, age_decile as stratum3_id, cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_1816 group by 1,2
),
statsview as
(select subject_id as stratum1_id, age_decile as stratum3_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, age_decile order by count_value) as rn from rawdata_1816 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id,s.stratum3_id as stratum3_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
join statsview p on s.stratum1_id = p.stratum1_id and s.stratum3_id = p.stratum3_id and p.rn <= s.rn
group by  s.stratum1_id, s.stratum3_id, s.count_value, s.total, s.rn
)
select 0 as id, 1816 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum3_id  AS STRING) as stratum3_id,
cast(o.total as int64) as count_value, round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2) as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
"

# 3005 (Distribution of counts per person by measurement_concept_id), 3006 (Distribution of counts per person by measurement_concept_id, gender), 3007 (Distribution of counts per person by measurement_concept_id and age at occurrence decile), 3008 (Distribution of counts per person by measurement_concept_id, gender, age at occurrence decile)
echo "Getting distribution of counts per person for each measurement concept that has unique unit"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"
insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,count_value,min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value)
with rawdata_3005 as
(select m.measurement_concept_id as subject_id, p.gender_concept_id as gender,
CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.measurement_concept_id != 0 and m.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
group by m.measurement_concept_id,2,3,m.person_id
union all
select m.measurement_source_concept_id as subject_id, p.gender_concept_id as gender,
CAST(floor((extract(year from m.measurement_date) - p.year_of_birth)/10) AS STRING) as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.measurement_source_concept_id != 0 and m.value_as_number is not null
and m.measurement_source_concept_id != m.measurement_concept_id
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
and floor((extract(year from m.measurement_date) - p.year_of_birth)/10) >=3
group by m.measurement_source_concept_id,2,3,m.person_id
union all
select m.measurement_concept_id as subject_id, p.gender_concept_id as gender,
'2' as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_concept_id as string)=ar.stratum_1
where m.measurement_concept_id = 3025315 and m.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
group by m.measurement_concept_id,2,3,m.person_id
union all
select m.measurement_source_concept_id as subject_id, p.gender_concept_id as gender,
'2' as age_decile, m.person_id, count(*) as count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p on p.person_id=m.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m.measurement_source_concept_id as string)=ar.stratum_1
where m.measurement_concept_id = 3025315 and m.value_as_number is not null
and m.measurement_concept_id != m.measurement_source_concept_id
and ar.analysis_id=3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
and (extract(year from m.measurement_date) - p.year_of_birth) >= 18 and (extract(year from m.measurement_date) - p.year_of_birth) < 30
group by m.measurement_source_concept_id,2,3,m.person_id
),
overallstats_a as
(select subject_id as stratum1_id,age_decile as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview_a as
(select subject_id as stratum1_id, age_decile as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id, age_decile order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats_a as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_a s
  join statsview_a p on s.stratum1_id = p.stratum1_id and s.stratum2_id = p.stratum2_id  and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
),
age_dist as
(
select 0 as id, 3007 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id,
'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_a p
join overallstats_a o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats_g as
(select subject_id as stratum1_id,gender as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview_g as
(select subject_id as stratum1_id, gender as stratum2_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,gender order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats_g as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_g s
  join statsview_g p on s.stratum1_id = p.stratum1_id  and s.stratum2_id = p.stratum2_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.count_value, s.total, s.rn
),
gender_dist as
(
select 0 as id, 3006 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,CAST(o.stratum2_id  AS STRING) as stratum2_id,
'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_g p
join overallstats_g o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats as
(select subject_id as stratum1_id,'' as stratum2_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2
),
statsview as
(select subject_id as stratum1_id, '' as stratum2_id,
count_value as count_value, count(*) as total, row_number() over
(partition by subject_id order by count_value) as rn from rawdata_3005 group by 1,2,3
),
priorstats as
(select  s.stratum1_id as stratum1_id, s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview s
  join statsview p on s.stratum1_id = p.stratum1_id  and p.rn <= s.rn
   group by  s.stratum1_id, s.count_value, s.total, s.rn
),
dist as
(
select 0 as id, 3005 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id,'' as stratum2_id,'' as stratum3_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats p
join overallstats o on p.stratum1_id = o.stratum1_id
group by o.stratum1_id, o.stratum2_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
overallstats_a_g as
(select subject_id as stratum1_id,age_decile as stratum2_id,gender as stratum3_id,
cast(avg(1.0 * count_value) as float64) as avg_value,
cast(stddev(count_value) as float64) as stdev_value, min(count_value) as min_value, max(count_value) as max_value,
count(*) as total from rawdata_3005 group by 1,2,3
),
statsview_a_g as
(select subject_id as stratum1_id, age_decile as stratum2_id, gender as stratum3_id, count_value as count_value, count(*) as total, row_number() over
(partition by subject_id,age_decile,gender order by count_value) as rn from rawdata_3005 group by 1,2,3,4
),
priorstats_a_g as
(select  s.stratum1_id as stratum1_id, s.stratum2_id as stratum2_id, s.stratum3_id as stratum3_id,
s.count_value as count_value, s.total as total, sum(p.total) as accumulated from  statsview_a_g s
  join statsview_a_g p on s.stratum1_id = p.stratum1_id  and s.stratum2_id = p.stratum2_id  and s.stratum3_id = p.stratum3_id and p.rn <= s.rn
   group by  s.stratum1_id, s.stratum2_id, s.stratum3_id, s.count_value, s.total, s.rn
),
age_gender_dist as
(
select 0 as id, 3008 as analysis_id, CAST(o.stratum1_id  AS STRING) as stratum1_id, CAST(o.stratum2_id  AS STRING) as stratum2_id,
CAST(o.stratum3_id  AS STRING) as stratum1_id,
round(o.total,2) as total,
round(o.min_value,2) as min_value, round(o.max_value,2) as max_value, round(o.avg_value,2)  as avg_value,
round(o.stdev_value,2) as stdev_value,
min(case when p.accumulated >= .50 * o.total then count_value else round(o.max_value,2) end) as median_value,
min(case when p.accumulated >= .10 * o.total then count_value else round(o.max_value,2) end) as p10_value,
min(case when p.accumulated >= .25 * o.total then count_value else round(o.max_value,2) end) as p25_value,
min(case when p.accumulated >= .75 * o.total then count_value else round(o.max_value,2) end) as p75_value,
min(case when p.accumulated >= .90 * o.total then count_value else round(o.max_value,2) end) as p90_value
FROM  priorstats_a_g p
join overallstats_a_g o on p.stratum1_id = o.stratum1_id and p.stratum2_id = o.stratum2_id and p.stratum3_id = o.stratum3_id
group by o.stratum1_id, o.stratum2_id, o.stratum3_id, o.total, o.min_value, o.max_value, o.avg_value, o.stdev_value
),
results as
(select * from age_dist
union all
select * from gender_dist
union all
select * from dist
union all
select * from age_gender_dist)
select id,analysis_id,stratum1_id,stratum2_id,stratum3_id,cast(total as int64),min_value,max_value,avg_value,stdev_value,median_value,p10_value,p25_value,p75_value,p90_value from results
"

# 1806 Measurement age by gender distribution
echo "Getting measurement age by gender distribution"
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


# 1900 Measurement numeric value counts (This query generates counts, source counts of the binned value and gender combination. It gets bin size from joining the achilles_results)
# We do net yet generate the binned source counts of standard concepts
echo "Getting measurements binned gender value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
with measurement_quartile_data_raw as
(
select cast(stratum_1 as int64) as concept,cast(stratum_3 as int64)as gender,min_value,max_value,p10_value,p25_value,p75_value,p90_value,(p75_value-p25_value) as iqr,
(case when (p25_value - 1.5*(p75_value-p25_value)) > min_value then (p25_value - 1.5*(p75_value-p25_value)) else min_value end) as iqr_min,
(case when (p75_value + 1.5*(p75_value-p25_value)) < max_value then (p75_value + 1.5*(p75_value-p25_value)) else max_value end) as iqr_max,
(((p75_value + 1.5*(p75_value-p25_value))-(p25_value - 1.5*(p75_value-p25_value)))/11) as bin_width
from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1815
),
measurement_quartile_data as
(
select concept,gender,min_value,max_value,p10_value,p25_value,p75_value,p90_value,iqr,iqr_min,iqr_max,
((iqr_max-iqr_min)/11) as bin_width from measurement_quartile_data_raw
)
select 0 as id,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
      when m1.value_as_number > iqr_max then iqr_max
      when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
      when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
      when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
      when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
      when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
      when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
      when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
      when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
      when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
      when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
      else iqr_max
     end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
      when m1.value_as_number > p90_value then p90_value
      when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
      else p90_value
     end),2)
     end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data on m1.measurement_concept_id=concept where m1.measurement_concept_id != 0
and m1.value_as_number is not null and p1.gender_concept_id=gender
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0 as id, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
      when m1.value_as_number > iqr_max then iqr_max
      when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
      when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
      when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
      when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
      when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
      when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
      when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
      when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
      when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
      when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
      else iqr_max
     end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
      when m1.value_as_number > p90_value then p90_value
      when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
      when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
      else p90_value
     end),2)
     end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data on m1.measurement_source_concept_id=concept
where m1.measurement_source_concept_id != 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_as_number is not null and p1.gender_concept_id=gender
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
"

# 1900 Measurement string value counts (This query generates counts, source counts of the value and gender combination. It gets bin size from joining the achilles_results)
# We do not yet generate the source counts of standard concepts
echo "Getting measurements unbinned gender value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1,stratum_2,stratum_3,stratum_4,count_value,source_count_value)
SELECT 0,1900 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on m1.measurement_concept_id = c.concept_id
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
and ar.analysis_id = 3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
group by m1.measurement_concept_id,m1.value_source_value,p1.gender_concept_id
union all
SELECT 0,1900 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_source_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and ar.analysis_id = 3000 and ar.stratum_3='Measurement' and ar.stratum_4 != 'unknown'
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,m1.value_source_value,p1.gender_concept_id"

# 1901 Measurement response, age decile histogram data (age decile > 2)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
with measurement_quartile_data_raw as
(
select cast(stratum_1 as int64) as concept,cast(stratum_3 as int64)as age_decile,min_value,max_value,p10_value,p25_value,p75_value,p90_value,(p75_value-p25_value) as iqr,
(case when (p25_value - 1.5*(p75_value-p25_value)) > min_value then (p25_value - 1.5*(p75_value-p25_value)) else min_value end) as iqr_min,
(case when (p75_value + 1.5*(p75_value-p25_value)) < max_value then (p75_value + 1.5*(p75_value-p25_value)) else max_value end) as iqr_max,
(((p75_value + 1.5*(p75_value-p25_value))-(p25_value - 1.5*(p75_value-p25_value)))/11) as bin_width
from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1816
),
measurement_quartile_data as
(
select concept,age_decile,min_value,max_value,p10_value,p25_value,p75_value,p90_value,iqr,iqr_min,iqr_max,
((iqr_max-iqr_min)/11) as bin_width from measurement_quartile_data_raw
)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_concept_id=ar.concept
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_source_concept_id=ar.concept
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5"


# 1901 Measurement response, age decile histogram data (age decile = 2)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id,analysis_id,stratum_1,stratum_2,stratum_3,stratum_4,stratum_5,count_value,source_count_value)
with measurement_quartile_data_raw as
(
select cast(stratum_1 as int64) as concept,cast(stratum_3 as int64)as age_decile,min_value,max_value,p10_value,p25_value,p75_value,p90_value,(p75_value-p25_value) as iqr,
(case when (p25_value - 1.5*(p75_value-p25_value)) > min_value then (p25_value - 1.5*(p75_value-p25_value)) else min_value end) as iqr_min,
(case when (p75_value + 1.5*(p75_value-p25_value)) < max_value then (p75_value + 1.5*(p75_value-p25_value)) else max_value end) as iqr_max,
(((p75_value + 1.5*(p75_value-p25_value))-(p25_value - 1.5*(p75_value-p25_value)))/11) as bin_width
from \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results_dist\` where analysis_id=1816
),
measurement_quartile_data as
(
select concept,age_decile,min_value,max_value,p10_value,p25_value,p75_value,p90_value,iqr,iqr_min,iqr_max,
((iqr_max-iqr_min)/11) as bin_width from measurement_quartile_data_raw
)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_concept_id=ar.concept
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_concept_id,stratum_2,stratum_3,stratum_4,stratum_5
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
cast(case when iqr_min = iqr_max then p10_value else iqr_min end as string) as stratum_3,
cast(
(case when iqr_min != iqr_max then
round((case when m1.value_as_number < iqr_min then iqr_min
       when m1.value_as_number > iqr_max then iqr_max
       when m1.value_as_number between iqr_min and iqr_min+bin_width then iqr_min+bin_width
       when m1.value_as_number between iqr_min+bin_width and iqr_min+2*bin_width then iqr_min+2*bin_width
       when m1.value_as_number between iqr_min+2*bin_width and iqr_min+3*bin_width then iqr_min+3*bin_width
       when m1.value_as_number between iqr_min+3*bin_width and iqr_min+4*bin_width then iqr_min+4*bin_width
       when m1.value_as_number between iqr_min+4*bin_width and iqr_min+5*bin_width then iqr_min+5*bin_width
       when m1.value_as_number between iqr_min+5*bin_width and iqr_min+6*bin_width then iqr_min+6*bin_width
       when m1.value_as_number between iqr_min+6*bin_width and iqr_min+7*bin_width then iqr_min+7*bin_width
       when m1.value_as_number between iqr_min+7*bin_width and iqr_min+8*bin_width then iqr_min+8*bin_width
       when m1.value_as_number between iqr_min+8*bin_width and iqr_min+9*bin_width then iqr_min+9*bin_width
       when m1.value_as_number between iqr_min+9*bin_width and iqr_min+10*bin_width then iqr_min+10*bin_width
       else iqr_max
      end),2)
else
round((case when m1.value_as_number < p10_value then p10_value
       when m1.value_as_number > p90_value then p90_value
       when m1.value_as_number between p10_value and p10_value+((p90_value-p10_value)/11) then p10_value+((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+((p90_value-p10_value)/11) and p10_value+2*((p90_value-p10_value)/11) then p10_value+2*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+2*((p90_value-p10_value)/11) and p10_value+3*((p90_value-p10_value)/11) then p10_value+3*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+3*((p90_value-p10_value)/11) and p10_value+4*((p90_value-p10_value)/11) then p10_value+4*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+4*((p90_value-p10_value)/11) and p10_value+5*((p90_value-p10_value)/11) then p10_value+5*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+5*((p90_value-p10_value)/11) and p10_value+6*((p90_value-p10_value)/11) then p10_value+6*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+6*((p90_value-p10_value)/11) and p10_value+7*((p90_value-p10_value)/11) then p10_value+7*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+7*((p90_value-p10_value)/11) and p10_value+8*((p90_value-p10_value)/11) then p10_value+8*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+8*((p90_value-p10_value)/11) and p10_value+9*((p90_value-p10_value)/11) then p10_value+9*((p90_value-p10_value)/11)
       when m1.value_as_number between p10_value+9*((p90_value-p10_value)/11) and p10_value+10*((p90_value-p10_value)/11) then p10_value+10*((p90_value-p10_value)/11)
       else p90_value
      end),2)
      end) as string) as stratum_4,
cast(case when iqr_min = iqr_max then p90_value else iqr_max end as string) as stratum_5,
COUNT(distinct p1.PERSON_ID) as count_value, COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1 on p1.person_id = m1.person_id
join measurement_quartile_data ar on
m1.measurement_source_concept_id=ar.concept
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10)=ar.age_decile
and m1.value_as_number is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_source_concept_id,stratum_2,stratum_3,stratum_4,stratum_5"

# 1901 Measurement response, age decile histogram data (For concepts that have text values)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurement response, age decile histogram data"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, count_value,source_count_value)
SELECT 0,1901 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on m1.measurement_concept_id = c.concept_id
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and ar.analysis_id = 3000 and ar.stratum_3 = 'Measurement' and ar.stratum_4 != 'unknown'
group by m1.measurement_concept_id,m1.value_source_value,stratum_2
union all
SELECT 0,1901 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_source_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and ar.analysis_id = 3000 and ar.stratum_3 = 'Measurement' and ar.stratum_4 != 'unknown'
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,m1.value_source_value,stratum_2"

#1901 Measurement string value counts (This query generates counts, source counts of the value and age decile 2. It gets bin size from joining the achilles_results)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurements unbinned age decile 2 value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_3, stratum_4, count_value,source_count_value)
SELECT 0,1901 as analysis_id,
cast(m1.measurement_concept_id as string) as stratum_1,'2' as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.concept\` c on m1.measurement_concept_id = c.concept_id
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
and ar.analysis_id = 3000 and ar.stratum_3 = 'Measurement' and ar.stratum_4 != 'unknown'
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_concept_id,m1.value_source_value,stratum_2
union all
SELECT 0,1901 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,'2' as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on cast(m1.measurement_source_concept_id as string)=ar.stratum_1
where m1.value_as_number is null and m1.value_source_value is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
and ar.analysis_id = 3000 and ar.stratum_3 = 'Measurement' and ar.stratum_4 != 'unknown'
group by m1.measurement_source_concept_id,m1.value_source_value,stratum_2"


# Set the counts > 0 and < 20 to 20
echo "Binning counts < 20"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"update \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
set count_value = 20, source_count_value = 20 where analysis_id in (1900,1901) and ((count_value>0 and count_value<20) or (source_count_value>0 and source_count_value<20))"