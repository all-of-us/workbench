-- ====== A COPY OF `all-of-us-ehr-dev.test_R2019q4r3` in `all-of-us-ehr-dev.ChenchalDummySrc` is used to VERIFY ======
-- ====== Verify after a complete run ===============
-- row counts for all tables

select * from (
                  select '01-sequential' run_type, table_id, row_count from `all-of-us-ehr-dev.ChenchalDummySeq.__TABLES__`
                  union all
                  select '02-parallel' run_type, table_id, row_count from `all-of-us-ehr-dev.ChenchalDummyPar.__TABLES__`
                  union all
                  select '03-parallel-multi' run_type, table_id, row_count from `all-of-us-ehr-dev.ChenchalDummyMult.__TABLES__`
                  where table_id in (select table_id from `all-of-us-ehr-dev.ChenchalDummySeq.__TABLES__`)   -- exclude prep_temp_* tables
                  union all
                  select '10-original' run_type, table_id, row_count from `all-of-us-ehr-dev.ChenchalDummyOri.__TABLES__`
                  union all
                  select '20-std-src' run_type, table_id, row_count from `all-of-us-ehr-dev.ChenchalDummySrc.__TABLES__`
                  where table_id in (select table_id from `all-of-us-ehr-dev.ChenchalDummySeq.__TABLES__`)  -- exclude tablesnot used/created by make-cb-criterta
              )
order by 2,1;

-- by domain_id
select * from (
              select '01-sequential' run_type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2
              union all
              select '02-parallel' run_type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2
              union all
              select '03-parallel-multi' run_type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2
              union all
              select '10-original' run_type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2
              union all
              select '20-std-src' run_type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2
              )
order by domain_id, run_type;
-- by type
select * from (
              select '01-sequential' run_type, type, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2
              union all
              select '02-parallel' run_type, type, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2
              union all
              select '03-parallel-multi' run_type, type, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2
              union all
              select '10-original' run_type, type, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2
              union all
              select '20-std-src' run_type, type, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2
              )
order by type, run_type;
-- by type and domain
select * from (
              select '01-sequential' run_type, type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2,3
              union all
              select '02-parallel' run_type, type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2,3
              union all
              select '03-parallel-multi' run_type, type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2,3
              union all
              select '10-original' run_type, type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2,3
              union all
              select '20-std-src' run_type, type, domain_id, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2,3
              )
order by domain_id, type, run_type;
-- by type, domain, is_group
select * from (
              select '01-sequential' run_type, type, domain_id, is_group, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2,3,4
              union all
              select '02-parallel' run_type, type, domain_id, is_group, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2,3,4
              union all
              select '03-parallel-multi' run_type, type, domain_id, is_group, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2,3,4
              union all
              select '10-original' run_type, type, domain_id, is_group, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2,3,4
              union all
              select '20-std-src' run_type, type, domain_id, is_group, count(*) row_count
              from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2,3,4
              )
order by domain_id, type, is_group, run_type;
-- by type, domain, is_group and sum_of_counts
select * from (
                  select '01-sequential' run_type, domain_id, type, is_standard, is_group, count(*) row_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2,3,4,5
                  union all
                  select '02-parallel' run_type, domain_id, type, is_standard, is_group, count(*) row_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2,3,4,5
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_standard, is_group, count(*) row_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2,3,4,5
                  union all
                  select '10-original' run_type, domain_id, type, is_standard, is_group, count(*) row_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2,3,4,5
                  union all
                  select '20-std-src' run_type, domain_id, type, is_standard, is_group, count(*) row_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2,3,4,5
              )
order by domain_id, type, is_standard, is_group, run_type;

-- CB_CRITERIA_ANCESTOR TABLE
select * from (
                  select '01-sequential' run_type, 'cb_criteria_ancestor' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria_ancestor`
                  union all
                  select '02-parallel' run_type, 'cb_criteria_ancestor' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria_ancestor`
                  union all
                  select '03-parallel-multi' run_type, 'cb_criteria_ancestor' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria_ancestor`
                  union all
                  select '10-original' run_type, 'cb_criteria_ancestor' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria_ancestor`
                  union all
                  select '20-std-src' run_type, 'cb_criteria_ancestor' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria_ancestor`
              )
order by run_type;

-- CB_CRITERIA_ATTRIBUTE TABLE
select * from (
                  select '01-sequential' run_type, 'cb_criteria_attribute' table_name
                       , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria_attribute`group by 3
                  union all
                  select '02-parallel' run_type, 'cb_criteria_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria_attribute`group by 3
                  union all
                  select '03-parallel-multi' run_type, 'cb_criteria_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria_attribute`group by 3
                  union all
                  select '10-original' run_type, 'cb_criteria_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria_attribute`group by 3
                  union all
                  select '20-std-src' run_type, 'cb_criteria_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria_attribute`group by 3
              )
order by type, run_type;

-- CB_SURVEY_ATTRIBUTE TABLE
select * from (
                  select '01-sequential' run_type, 'cb_survey_attribute' table_name
                       , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria_attribute`group by 3
                  union all
                  select '02-parallel' run_type, 'cb_survey_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria_attribute`group by 3
                  union all
                  select '03-parallel-multi' run_type, 'cb_survey_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria_attribute`group by 3
                  union all
                  select '10-original' run_type, 'cb_survey_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria_attribute`group by 3
                  union all
                  select '20-std-src' run_type, 'cb_survey_attribute' table_name
                          , type, cast(sum(cast(est_count as BIGNUMERIC)) as STRING) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria_attribute`group by 3
              )
order by type, run_type;

-- CB_CRITERIA_RELATIONSHIP TABLE
select * from (
                  select '01-sequential' run_type, 'cb_criteria_relationship' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria_relationship`
                  union all
                  select '02-parallel' run_type, 'cb_criteria_relationship' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria_relationship`
                  union all
                  select '03-parallel-multi' run_type, 'cb_criteria_relationship' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria_relationship`
                  union all
                  select '10-original' run_type, 'cb_criteria_relationship' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria_relationship`
                  union all
                  select '20-std-src' run_type, 'cb_criteria_relationship' table_name, count(*) num_rows
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria_relationship`
              )
order by run_type;

-- ========== Below are SQL snippets for verifying counts for different domains/vocabularies/ SQL-BLOCKS ==============
-- CPT4 Counts - SQL-ORDER = 1
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='CPT4' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='CPT4' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='CPT4' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='CPT4' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='CPT4' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PPI - Physical Measurement - SQL-ORDER = 2
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PPI - SURVEYS - SQL-ORDER = 3
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='PPI' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='PPI' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='PPI' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='PPI' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='PPI' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PM - CONCEPT SETS - SQL-ORDER = 4
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- FITBIT - SQL-ORDER = 5
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='FITBIT' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='FITBIT' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='FITBIT' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='FITBIT' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='FITBIT' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- WHOLE GENOME VARIANT - SQL-ORDER = 6
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- DEMOGRAPHICS - SQL-ORDER = 7
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PERSON' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PERSON' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PERSON' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PERSON' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PERSON' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- VISIT OCCURRENCE - SQL-ORDER = 8
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='VISIT' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='VISIT' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='VISIT' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='VISIT' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='VISIT' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- ICD9 SOURCE - SQL-ORDER = 9
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- ICD10CM SOURCE - SQL-ORDER = 10
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'ICD10CM' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'ICD10CM' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'ICD10CM' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'ICD10CM' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'ICD10CM' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- ICD10PCS SOURCE - SQL-ORDER = 11
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'ICD10PCS' group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'ICD10PCS' group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'ICD10PCS' group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'ICD10PCS' group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'ICD10PCS' group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- CONDITION_OCCURRENCE - SNOMED - SOURCE - SQL-ORDER = 12
select * from (

                  select '01-sequential' run_type, domain_id, type, is_group,  count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group,  count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group,  count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group,  count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group,  count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- CONDITION_OCCURRENCE - SNOMED - STANDARD - SQL-ORDER = 13
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- MEASUREMENT - Clinical - STANDARD LOINC - SQL-ORDER = 14
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'LOINC' and subtype= 'CLIN' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'LOINC' and subtype= 'CLIN' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'LOINC' and subtype= 'CLIN' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'LOINC' and subtype= 'CLIN' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'LOINC' and subtype= 'CLIN' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- MEASUREMENT - Labs - STANDARD LOINC - SQL-ORDER = 15
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'LOINC' and subtype= 'LAB' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'LOINC' and subtype= 'LAB' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'LOINC' and subtype= 'LAB' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'LOINC' and subtype= 'LAB' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'LOINC' and subtype= 'LAB' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- MEASUREMENT - SNOMED - STANDARD - SQL-ORDER = 16
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED'  and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED'  and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- DRUG_EXPOSURE - ATC/RXNORM - SQL-ORDER = 17
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'ATC' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'ATC' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'ATC' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'ATC' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'ATC' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PROCEDURE_OCCURRENCE - SNOMED - SOURCE - SQL-ORDER = 18
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 0 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PROCEDURE_OCCURRENCE - SNOMED - STANDARD - SQL-ORDER = 19
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- OBSERVATION - SNOMED - STANDARD - SQL-ORDER = 20
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'OBSERVATION' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'OBSERVATION' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'OBSERVATION' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'OBSERVATION' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'OBSERVATION' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;



-- OTHER_TABLES THAT UPDATE CB_CRITERIA
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`
                  where (domain_id ='MEASUREMENT' and has_attribute = 1) OR (domain_id ='SURVEY' and is_selectable = 1) group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`
                  where (domain_id ='MEASUREMENT' and has_attribute = 1) OR (domain_id ='SURVEY' and is_selectable = 1) group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`
                  where (domain_id ='MEASUREMENT' and has_attribute = 1) OR (domain_id ='SURVEY' and is_selectable = 1) group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria`
                  where (domain_id ='MEASUREMENT' and has_attribute = 1) OR (domain_id ='SURVEY' and is_selectable = 1) group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`
                  where (domain_id ='MEASUREMENT' and has_attribute = 1) OR (domain_id ='SURVEY' and is_selectable = 1) group by 4,3,2
              )
order by domain_id, type, is_group, run_type;



--============= COUNTS FOR ALL PROCESSED (DOMAIN_ID and TYPE) by IS_GROUP =============
--------------- all-of-us-ehr-dev.ChenchalDummySrc is a copy of all-of-us-ehr-dev.test_R2019q4r3 --------------
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`
                  where domain_id in (select distinct(domain_id) from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`)
                    and type in (select distinct(type) from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`)
                  group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- PROCEDURE_OCCURRENCE - SNOMED - STANDARD - SQL-ORDER = 19
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type = 'SNOMED' and is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;

-- OBSERVATION - SQL-ORDER = 20
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where is_standard = 1 group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where is_standard = 1 group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where is_standard = 1 group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where is_standard = 1 group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where is_standard = 1 group by 4,3,2
              )
order by domain_id, type, is_group, run_type;



--
--- Troubleshoot:
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type in ('SNOMED') group by 4,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type in ('SNOMED') group by 4,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type in ('SNOMED') group by 4,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type in ('SNOMED') group by 4,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type in ('SNOMED') group by 4,3,2
              )
order by domain_id, type, is_group, run_type;



-- Row counts
select * from (
                  select '01-sequential' run_type, type, domain_id, count(*) row_count from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`  group by 2,3
                  union all
                  select '02-parallel' run_type, type, domain_id, count(*) row_count from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`  group by 2,3
                  union all
                  select '03-parallel-multi' run_type, type, domain_id, count(*) row_count from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`  group by 2,3
                  union all
                  select '10-original' run_type, type, domain_id, count(*) row_count from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` group by 2,3
                  union all
                  select '20-std-src' run_type, type, domain_id, count(*) row_count from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`group by 2,3
              )
order by type, domain_id, run_type


--==========truncate...

-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_concept_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_ancestor_staging`;

-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_concept_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_ancestor_staging`;

-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.prep_concept_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_ancestor_staging`;
--
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_concept_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_ancestor_staging`;

-- select seq.pc s, par.pc p, mult.pc m, ori.pc o, src.pc std from
--    (select count(distinct path) pc from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='CPT4' ) seq,
--    (select count(distinct path) pc from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='CPT4' ) par,
--    (select count(distinct path) pc from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='CPT4') mult,
--    (select count(distinct path) pc from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='CPT4' ) ori,
--    (select count(distinct path) pc from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='CPT4' ) src
-- --where src.pc - seq.pc != 0 or src.pc - par.pc != 0 or src.pc - mult.pc != 0 or src.pc - ori.pc != 0
-- order by 1,2,3,4;
--



-- -- cleanup as you go:
-- echo "CLEAN UP - set rollup_count, item_count, est_count = -1 when value is NULL
-- set has_ancestor_data = 0 when value is null and
--     remove double-quotes from name, when present"
--     bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
--     "UPDATE \`$BQ_PROJECT.$BQ_DATASET.$TBL_CBC\`
-- SET
--     rollup_count = COALESCE(rollup_count, -1),
--     item_count = COALESCE(item_count, -1),
--     est_count = COALESCE(est_count, -1),
--     has_ancestor_data = COALESCE(has_ancestor_data, 0),
--     name = CASE WHEN REGEXP_CONTAINS(name, r'[\"]') THEN REGEXP_REPLACE(name, r'[\"]', '')  END
--     WHERE TRUE"
--===============================
