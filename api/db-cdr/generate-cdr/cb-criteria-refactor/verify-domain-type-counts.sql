-- ========== Below are SQL snippets for verifying counts for different domains/vocabularies/ SQL-BLOCKS ==============
-- CPT4 Counts - SQL-ORDER = 1
select * from (
                  select '01-sequential' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='CPT4' group by is_group,name, type
                  union all
                  select '02-parallel' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='CPT4' group by is_group,name, type
                  union all
                  select '03-parallel-multi' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='CPT4' group by is_group,name, type
                  union all
                  select '10-original' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='CPT4' group by is_group, name, type
                  union all
                  select '20-src' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='CPT4' group by is_group, name, type
              )
order by 2,3,4,1;

-- PPI - Physical Measurement - SQL-ORDER = 2
select * from (
                  select '01-sequential' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where type='PPI' group by is_group,name, type
                  union all
                  select '02-parallel' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where type='PPI' group by is_group,name, type
                  union all
                  select '03-parallel-multi' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where type='PPI' group by is_group,name, type
                  union all
                  select '10-original' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where type='PPI' group by is_group, name, type
                  union all
                  select '20-std-src' as run_type, type, is_group, 'cpt-name' as name, count(is_group), sum(item_count),sum(rollup_count),sum(est_count) from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where type='PPI' group by is_group, name, type
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




