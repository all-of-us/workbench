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

-- ICD9 SOURCE - SQL-ORDER = 9
select * from (
                  select '01-sequential' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                       , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySeq.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,5,3,2
                  union all
                  select '02-parallel' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyPar.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,5,3,2
                  union all
                  select '03-parallel-multi' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyMult.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,5,3,2
                  union all
                  select '10-original' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummyOri.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,5,3,2
                  union all
                  select '20-std-src' run_type, domain_id, type, is_group, name, count(is_group) sum_grp_count
                          , sum(item_count) sum_item_count, sum(rollup_count) sum_rollup_count, sum(est_count) sum_est_count
                  from `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` where domain_id in ('CONDITION','PROCEDURE') and type in ('ICD9CM','ICD9Proc') group by 4,5,3,2
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






-- icd9 - level2
SELECT
    ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) AS id
     , p.id AS parent_id
     , p.domain_id
     , p.is_standard
     , p.type
     , c.concept_id AS concept_id
     , c.concept_code AS code
     , c.concept_name AS name
     , 1
     , 0
     , 0
     , 1
     ,CONCAT(p.path, '.',
             CAST(ROW_NUMBER() OVER (ORDER BY p.parent_id, c.concept_code) +
                  (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING))
-- in order to get level 2, we will link it from its level 1 parent
FROM
    (
        SELECT *
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`
        WHERE type in ('ICD9CM', 'ICD9Proc')
          and parent_id = 0
    ) p
        JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_relationship_merged`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
        JOIN `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_merged` c on x.concept_id_2 = c.concept_id

-- icd9 level 3
SELECT
        ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) AS id
     , p.id AS parent_id
     , p.domain_id
     , p.is_standard
     , p.type
     , c.concept_id AS concept_id
     , c.concept_code AS code
     , c.concept_name AS name
     , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
     , 1
     , 1
     , 0
     , 1
     , CONCAT(p.path, '.',
              CAST(ROW_NUMBER() OVER (ORDER BY p.id, c.concept_code) +
                   (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING))
-- in order to get level 3, we will link it from its level 2 parent
FROM
    (
        SELECT *
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`
        WHERE type in ('ICD9CM', 'ICD9Proc')
          and parent_id != 0
          and is_group = 1
          and is_selectable = 0
    ) p
        JOIN
    (
        SELECT concept_id_1, concept_id_2
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_relationship_merged`
        WHERE relationship_id = 'Subsumes'
    ) x on p.concept_id = x.concept_id_1
        JOIN `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_merged` c on  x.concept_id_2 = c.concept_id
    LEFT JOIN
    (
    -- get the count of distinct patients coded with each concept
    SELECT concept_id, COUNT(DISTINCT person_id) cnt
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_search_all_events`
    WHERE is_standard = 0
    and concept_id in
    (
    -- get all concepts
    SELECT concept_id
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_merged`
    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
    )
    GROUP BY 1
    ) d on c.concept_id = d.concept_id

-- icd9 level4
SELECT
        ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) AS id
     , b.id AS parent_id
     , b.domain_id
     , b.is_standard
     , a.vocabulary_id AS type
     , a.concept_id
     , a.concept_code AS code
     , a.concept_name AS name
     , CASE WHEN c.code is null THEN 0 ELSE null END AS rollup_count     -- c.code is null = child
     , CASE WHEN d.cnt is null THEN 0 ELSE d.cnt END AS item_count
     , CASE WHEN c.code is null THEN d.cnt ELSE null END AS est_count
     , CASE WHEN c.code is null THEN 0 ELSE 1 END as is_group
     , 1
     , 0
     , 1
     ,CONCAT(b.path, '.',
             CAST(ROW_NUMBER() OVER (ORDER BY b.id, a.concept_code) +
                  (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING))
-- in order to get level 4, we will link it to its level 3 parent
FROM
    (
        SELECT *
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.concept`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
        -- level 4 codes have a decimal with 1 digit after (ex: 98.0)
          and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d$')
    ) a
-- in order to find its parent, which is just its whole number (ex: 98.0's parent is 98), we will use regex to extract the whole number
        JOIN `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = b.code and a.vocabulary_id = b.type)
    LEFT JOIN
    (
    -- determine if this item is a parent or child by seeing if it has any child items
    -- ex: V09.8 > V09.80 so is_group = 1
    -- ex: E879.5 > nothing so is_group = 0
    SELECT distinct REGEXP_EXTRACT(concept_code, r'^\w{1,}\.\d') code
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.concept`
    WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
    and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) c on a.concept_code = c.code
    LEFT JOIN
    (
    -- get the count of distinct patients coded with each concept
    SELECT concept_id, COUNT(DISTINCT person_id) cnt
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_search_all_events`
    WHERE is_standard = 0
    and concept_id in
    (
    -- get all concepts
    SELECT concept_id
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_merged`
    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
    ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE
    (
-- get all parents OR get all children that have a count
    c.code is not null
   OR
    (
    c.code is null
  AND d.cnt is not null
    )
    )

-- icd9-level5
SELECT
        ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) AS id
     , CASE WHEN b.id is not null THEN b.id ELSE c.id END AS parent_id
     , CASE WHEN b.domain_id is not null THEN b.domain_id ELSE c.domain_id END as domain_id
     , 0
     , a.vocabulary_id AS type
     , a.concept_id,a.concept_code AS code
     , a.concept_name AS name
     , 0 as rollup_count
     , d.cnt AS item_count
     , d.cnt AS est_count
     , 0
     , 1
     , 0
     , 1
     , CASE
           WHEN b.id is not null THEN
                   b.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING)
           ELSE
                   c.path || '.' || CAST(ROW_NUMBER() OVER (ORDER BY b.id,a.concept_code) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING)
    END as path
-- in order to get level 5, we will link it to its level 4 parent
FROM
    (
        SELECT *
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.concept`
        WHERE vocabulary_id in ('ICD9CM','ICD9Proc')
        -- codes such as 98.01, V09.71, etc.
          and REGEXP_CONTAINS(concept_code, r'^\w{1,}\.\d{2}$')
    ) a
-- get any level 4 parents that link to this item
        LEFT JOIN `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` b on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}\.\d') = b.code and a.vocabulary_id = b.type)
-- get any level 3 parents that link to this item (this is because some level 5 items only link to a level 3 item)
    LEFT JOIN `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria` c on (REGEXP_EXTRACT(a.concept_code, r'^\w{1,}') = c.code and a.vocabulary_id = c.type)
    LEFT JOIN
    (
    -- get the count of distinct patients coded with each concept
    SELECT concept_id, COUNT(DISTINCT person_id) cnt
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_search_all_events`
    WHERE is_standard = 0
    and concept_id in
    (
    -- get all concepts
    SELECT concept_id
    FROM `all-of-us-ehr-dev.ChenchalDummySrc.prep_concept_merged`
    WHERE vocabulary_id in ('ICD9CM', 'ICD9Proc')
    ) GROUP BY 1
    ) d on a.concept_id = d.concept_id
WHERE d.cnt is not null

-- condition not already captured....
-- from #6014..
SELECT ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as ID,
       -1, 'CONDITION',1, vocabulary_id,concept_id,concept_code,concept_name,0,cnt,cnt,0,1,0,0,
       CAST(ROW_NUMBER() OVER(order by vocabulary_id,concept_name) + (SELECT MAX(id) FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`) as STRING) as path
FROM
    (
        SELECT concept_name, vocabulary_id, concept_id, concept_code, count(DISTINCT person_id) cnt
        FROM `all-of-us-ehr-dev.ChenchalDummySrc.condition_occurrence` a
            LEFT JOIN `all-of-us-ehr-dev.ChenchalDummySrc.concept` b on a.condition_concept_id = b.concept_id
        WHERE standard_concept = 'S'
          and domain_id = 'Condition'
          and condition_concept_id NOT IN
            (
            SELECT concept_id
            FROM `all-of-us-ehr-dev.ChenchalDummySrc.cb_criteria`
            WHERE domain_id = 'CONDITION'
          and is_standard = 1
          and concept_id is not null
            )
        GROUP BY 1,2,3,4
    ) x
