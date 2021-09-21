-- ========== Below are SQL snippets for verifying counts for different domains/vocabularies/ SQL-BLOCKS ==============
-- CPT4 Counts - SQL-ORDER = 1
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                    , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                    from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='CPT4' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                    , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                    from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='CPT4' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                    , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                    from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='CPT4' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                    , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                    from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='CPT4' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                    , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                    from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='CPT4' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- PPI - Physical Measurement - SQL-ORDER = 2
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='PPI' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='PPI' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='PPI' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='PPI' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='PPI' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- All 'type' -- in progress... what to do with 'name'
select * from (
                  select '01-sequential' run_type, type, is_group, 'cpt-name' name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` group by is_group,name,type
                  union all
                  select '02-parallel' run_type, type, is_group, 'cpt-name' name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` group by is_group,name,type
                  union all
                  select '03-parallel-multi' run_type, type, is_group, 'cpt-name' name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` group by is_group,name,type
                  union all
                  select '10-original' run_type, type, is_group, 'cpt-name' name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria`  group by is_group,name,type
                  union all
                  select '20-src' run_type, type, is_group, 'cpt-name' name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` group by is_group,name,type
              )
order by 2,3,4,1;
--==========truncate...

-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_cpt_ancestor`;
--
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_cpt_ancestor`;
--
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.prep_cpt_ancestor`;
--
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_cpt_ancestor`;




