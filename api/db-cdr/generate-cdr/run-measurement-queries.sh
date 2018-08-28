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
with single_unit_measurement_concepts as
(select measurement_concept_id,count(distinct unit_source_value) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_concept_id != 0 group by measurement_concept_id having cnt=1
),
single_unit_measurement_source_concepts as
(select measurement_source_concept_id,count(distinct unit_source_value) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_source_concept_id != 0 and measurement_concept_id != measurement_source_concept_id group by measurement_source_concept_id having cnt=1
)
select 0, 3000 as analysis_id, CAST(co1.measurement_concept_id  AS STRING) as stratum_1,
cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) AS STRING) as stratum_2,
'Measurement' as stratum_3,
(case when co1.unit_concept_id != 0 then (select concept_name from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` where concept_id=co1.unit_concept_id) else
(case when co1.measurement_concept_id in (select measurement_concept_id from single_unit_measurement_concepts) then co1.unit_source_value else 'unknown' end)end)
as stratum_4,
COUNT(distinct co1.person_id) as count_value,
(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_concept_id > 0
and co1.value_as_number is not null
group by  co1.measurement_concept_id,stratum_4
union all
select 0, 3000 as analysis_id, CAST(co1.measurement_source_concept_id  AS STRING) as stratum_1,
cast(ceil((ceil(max(co1.value_as_number))-floor(min(co1.value_as_number)))/10) AS STRING) as stratum_2,
'Measurement' as stratum_3,
(case when co1.unit_concept_id != 0 then (select concept_name from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` where concept_id=co1.unit_concept_id) else
(case when co1.measurement_source_concept_id in (select measurement_source_concept_id from single_unit_measurement_source_concepts) then co1.unit_source_value else 'unknown' end)end)
as stratum_4,
COUNT(distinct co1.person_id) as count_value,
COUNT(distinct co1.person_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_source_concept_id > 0 and co1.measurement_concept_id != co1.measurement_source_concept_id
and co1.value_as_number is not null
group by  co1.measurement_source_concept_id,stratum_4"


# 3000 Measurements that have string values - Number of persons with at least one measurement occurrence by measurement_concept_id
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_3, stratum_4, count_value, source_count_value)
with single_unit_measurement_concepts as
(select measurement_concept_id,count(distinct unit_source_value) as cnt
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_concept_id != 0 group by measurement_concept_id having cnt=1
),
single_unit_measurement_source_concepts as
(select measurement_source_concept_id,count(distinct unit_source_value) as cnt
from  \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` where measurement_source_concept_id != 0 and measurement_concept_id != measurement_source_concept_id group by measurement_source_concept_id having cnt=1
)
select 0, 3000 as analysis_id, CAST(co1.measurement_concept_id  AS STRING) as stratum_1,
'Measurement' as stratum_3,
(case when co1.unit_concept_id != 0 then (select concept_name from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` where concept_id=co1.unit_concept_id) else
(case when co1.measurement_concept_id in (select measurement_concept_id from single_unit_measurement_concepts) then co1.unit_source_value else 'unknown' end)end)
as stratum_4,
COUNT(distinct co1.person_id) as count_value,
(select COUNT(distinct co2.person_id) from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co2 where co2.measurement_source_concept_id=co1.measurement_concept_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_concept_id > 0
and co1.value_as_number is null and co1.value_source_value is not null
group by  co1.measurement_concept_id,stratum_4
union all
select 0, 3000 as analysis_id, CAST(co1.measurement_source_concept_id  AS STRING) as stratum_1,
'Measurement' as stratum_3,
(case when co1.unit_concept_id != 0 then (select concept_name from \`${BQ_PROJECT}.${BQ_DATASET}.concept\` where concept_id=co1.unit_concept_id) else
(case when co1.measurement_concept_id in (select measurement_concept_id from single_unit_measurement_source_concepts) then co1.unit_source_value else 'unknown' end)end)
as stratum_4,
COUNT(distinct co1.person_id) as count_value,
COUNT(distinct co1.person_id) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` co1
where co1.measurement_source_concept_id > 0 and co1.measurement_concept_id != co1.measurement_source_concept_id
and co1.value_as_number is null and co1.value_source_value is not null
group by  co1.measurement_source_concept_id,stratum_4"


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


# 1900 Measurement numeric value counts (This query generates counts, source counts of the binned value and gender combination. It gets bin size from joining the achilles_results)
# We do net yet generate the binned source counts of standard concepts
echo "Getting measurements binned gender value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_4, count_value, source_count_value)
select 0,1900 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_concept_id AS STRING)=ar.stratum_1
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
group by m1.measurement_concept_id,stratum_2,stratum_4
union all
select 0, 1900 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(p1.gender_concept_id AS STRING) as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_source_concept_id AS STRING)=ar.stratum_1
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
group by m1.measurement_source_concept_id,stratum_2,stratum_4"

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
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
group by m1.measurement_concept_id,m1.value_source_value,p1.gender_concept_id
union all
SELECT 0,1900 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(p1.gender_concept_id AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,m1.value_source_value,p1.gender_concept_id"


# 1901 Measurement numeric value counts (This query generates counts, source counts of the binned value and (age decile > 2) combination. It gets bin size from joining the achilles_results)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurements binned age decile value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_4, count_value,source_count_value)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_concept_id AS STRING)=ar.stratum_1
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
group by m1.measurement_concept_id,stratum_2,stratum_4
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_source_concept_id AS STRING)=ar.stratum_1
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_source_concept_id,stratum_2,stratum_4"


# 1901 Measurement numeric value counts (This query generates counts, source counts of the binned value and age decile 2. It gets bin size from joining the achilles_results)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurements binned age decile 2 value counts"
bq --quiet --project=$BQ_PROJECT query --nouse_legacy_sql \
"insert into \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\`
(id, analysis_id, stratum_1, stratum_2, stratum_4, count_value,source_count_value)
select 0,1901 as analysis_id,
CAST(m1.measurement_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
count(distinct p1.person_id) as count_value,
0 as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_concept_id AS STRING)=ar.stratum_1
where m1.measurement_concept_id > 0
and m1.value_as_number is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
group by m1.measurement_concept_id,stratum_2,stratum_4
union all
select 0, 1901 as analysis_id,
CAST(m1.measurement_source_concept_id AS STRING) as stratum_1,
'2' as stratum_2,
CAST((case when cast(ar.stratum_2 as int64) > 0 then
(case when m1.value_as_number < cast(ar.stratum_2 as int64) then cast(ar.stratum_2 as int64) else
cast(ROUND(m1.value_as_number / cast(ar.stratum_2 as int64)) * cast(ar.stratum_2 as int64) as int64) end) else m1.value_as_number end) as STRING) as stratum_4,
COUNT(distinct p1.PERSON_ID) as count_value,
COUNT(distinct p1.PERSON_ID) as source_count_value
from \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 inner join
\`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
on p1.person_id = m1.person_id
join \`${WORKBENCH_PROJECT}.${WORKBENCH_DATASET}.achilles_results\` ar on
cast(m1.measurement_source_concept_id AS STRING)=ar.stratum_1
where m1.measurement_source_concept_id > 0 and m1.measurement_concept_id!=m1.measurement_source_concept_id
and m1.value_as_number is not null
and ar.analysis_id=3000 and ar.stratum_3 like '%Measurement%' and ar.stratum_2 is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
group by m1.measurement_source_concept_id,stratum_2,stratum_4"

#1901 Measurement string value counts (This query generates counts, source counts of the value and age decile > 2 combination. It gets bin size from joining the achilles_results)
# We do not yet generate the binned source counts of standard concepts
echo "Getting measurements unbinned age decile value counts"
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
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
group by m1.measurement_concept_id,m1.value_source_value,stratum_2
union all
SELECT 0,1901 as analysis_id,
cast(m1.measurement_source_concept_id as string) as stratum_1,CAST(floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) AS STRING) as stratum_2,'Measurement' as stratum_3,
m1.value_source_value as stratum_4,
count(distinct p1.person_id) as count_value,
count(distinct p1.person_id) as source_count_value
FROM \`${BQ_PROJECT}.${BQ_DATASET}.measurement\` m1
join \`${BQ_PROJECT}.${BQ_DATASET}.person\` p1 on p1.person_id = m1.person_id
where m1.value_as_number is null and m1.value_source_value is not null
and floor((extract(year from m1.measurement_date) - p1.year_of_birth)/10) >=3
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
where m1.value_as_number is null and m1.value_source_value is not null
and m1.measurement_concept_id > 0 and m1.measurement_concept_id not in (4091452,4065279,3027018)
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
where m1.value_as_number is null and m1.value_source_value is not null
and (extract(year from m1.measurement_date) - p1.year_of_birth) >= 18 and (extract(year from m1.measurement_date) - p1.year_of_birth) < 30
and m1.measurement_source_concept_id > 0 and m1.measurement_source_concept_id != m1.measurement_concept_id
group by m1.measurement_source_concept_id,m1.value_source_value,stratum_2"