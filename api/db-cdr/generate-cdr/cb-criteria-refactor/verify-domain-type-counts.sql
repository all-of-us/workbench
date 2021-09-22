-- ====== A COPY OF `all-of-us-ehr-dev.test_R2019q4r3` in `all-of-us-ehr-dev.ChenchalDummySrc` is used to VERIFY ======
-- ========== Below are SQL snippets for verifying counts for different domains/vocabularies/ SQL-BLOCKS ==============
-- CPT4 Counts - SQL-ORDER = 1
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PROCEDURE' and type='CPT4' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PROCEDURE' and type='CPT4' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PROCEDURE' and type='CPT4' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PROCEDURE' and type='CPT4' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, 'cpt4-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PROCEDURE' and type='CPT4' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- PPI - Physical Measurement - SQL-ORDER = 2
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT' and type='PPI' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- PPI - SURVEYS - SQL-ORDER = 3
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, 'survey-name' name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='SURVEY' and type='PPI' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, 'survey-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='SURVEY' and type='PPI' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, 'survey-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='SURVEY' and type='PPI' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, 'survey-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='SURVEY' and type='PPI' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, 'survey-name' name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='SURVEY' and type='PPI' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- PM - CONCEPT SETS - SQL-ORDER = 4
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PHYSICAL_MEASUREMENT_CSS' and type='PPI' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- FITBIT - SQL-ORDER = 5
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='FITBIT' and type='FITBIT' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='FITBIT' and type='FITBIT' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='FITBIT' and type='FITBIT' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='FITBIT' and type='FITBIT' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='FITBIT' and type='FITBIT' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- WHOLE GENOME VARIANT - SQL-ORDER = 6
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='WHOLE_GENOME_VARIANT' and type='WHOLE_GENOME_VARIANT' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- DEMOGRAPHICS - SQL-ORDER = 7
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='PERSON' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='PERSON' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='PERSON' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='PERSON' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='PERSON' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;

-- VISIT OCCURRENCE - SQL-ORDER = 8
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id='VISIT' group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id='VISIT' group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id='VISIT' group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id='VISIT' group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id='VISIT' group by 4,5,3,2
              )
order by domain_id, type, is_group, name, run_type;


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


--==========truncate...

-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummySeq.prep_concept_ancestor`;

-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyPar.prep_concept_ancestor`;

-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyMult.prep_concept_ancestor`;

-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_cpt_ancestor`;
-- truncate table `all-of-us-ehr-dev.ChenchalDummyOri.prep_concept_ancestor`;




