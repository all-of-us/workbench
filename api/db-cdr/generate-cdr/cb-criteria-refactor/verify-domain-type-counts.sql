-- Row counts by domain_id and type
select * from (
                  select distinct '1-sequential' as run_type, domain_id, type, subtype, coalesce(count(*),0) as row_count from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` group by 2,3,4
                  union all
                  select distinct '2-parallel' as run_type, domain_id, type, subtype, coalesce(count(*),0) as row_count from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` group by 2,3,4
                  union all
                  select distinct '3-parallel-multi' as run_type, domain_id, type, subtype, coalesce(count(*),0) as row_count from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` group by 2,3,4
                  union all
                  select distinct '9-original' as run_type, domain_id, type, subtype, coalesce(count(*),0) as row_count from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`  group by 2,3,4
              )
where type='CPT4' and domain_id='PROCEDURE' and subtype is null
order by 1 desc;

-- Item Counts
select * from (
                  select distinct '1-sequential' as run_type, domain_id, type, id, coalesce(item_count,0) as item_count from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` --where rollup_count > 0
                  union all
                  select distinct '2-parallel' as run_type, domain_id, type, id-1000000000, coalesce(item_count,0) as item_count from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` --where rollup_count > 0
                  union all
                  select distinct '3-parallel-multi' as run_type, domain_id, type, id-1000000000, coalesce(item_count,0) as item_count from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` --where rollup_count > 0
                  union all
                  select distinct '9-original' as run_type, domain_id, type, id, coalesce(item_count,0) as item_count from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` --where rollup_count > 0
              )
where type='CPT4' and domain_id='PROCEDURE' and item_count >0
order by 4, 1 desc;

-- Rollup Counts
select * from (
  select distinct '1-sequential' as run_type, domain_id, type, id, coalesce(rollup_count,0) as rollup_count from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` --where rollup_count > 0
  union all
  select distinct '2-parallel' as run_type, domain_id, type, id-1000000000, coalesce(rollup_count,0) as rollup_count from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` --where rollup_count > 0
  union all
  select distinct '3-parallel-multi' as run_type, domain_id, type, id-1000000000, coalesce(rollup_count,0) as rollup_count from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` --where rollup_count > 0
  union all
  select distinct '9-original' as run_type, domain_id, type, id, coalesce(rollup_count,0) as rollup_count from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` --where rollup_count > 0
)
where type='CPT4' and domain_id='PROCEDURE' and rollup_count >0
order by 4, 1 desc;



select * from (
    select '1-sequential' as run_type, is_group, count(is_group) group_count, sum(item_count) sum_item_count,sum(rollup_count) sum_rollup_count,sum(est_count) sum_est_count from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` group by 2
    union all
    select '2-parallel' as run_type, is_group, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` group by 2
    union all
    select '3-parallel-multi' as run_type, is_group, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` group by 2
    union all
    select '9-original' as run_type, is_group, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2
)
order by 2,1

