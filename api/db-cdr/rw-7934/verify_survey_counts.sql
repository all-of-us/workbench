
select survey, count(*) from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
group by survey
order by 1;

/**
survey	f0_
Basics	156
Family History	735
FamilyHealthHistory	877
February2021COVID19Part	357
HealthCareAccessUtiliza	379
July2020Covid19Particip	747
Lifestyle	219
May2020Covid19Participa	9
OverallHealth	162
PersonalMedicalHistory	2441
SocialDeterminantsOfHea	585
WinterMinuteSurveyOnCOV	486
*/





select parent_id, b.code, b.name, count(*) as answer_counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig` a
left join (select id, code,name from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
      where survey in ('February2021COVID19Part') and subtype='QUESTION') b on a.parent_id=b.id
where survey in ('February2021COVID19Part')
and subtype='ANSWER'
group by 1,2,3
order by 1
;


select subtype, count(*) as counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
where survey in ('February2021COVID19Part')
group by subtype
order by 1;


select old_.survey, old_.subtype, old_.old_counts, new_.new_counts
from
(
select survey, subtype, count(*) as old_counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
group by survey, subtype
) old_
left join
(
select survey, subtype, count(*) as new_counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey`
group by survey, subtype
 ) new_ on old_.survey = new_.survey and old_.subtype = new_.subtype
order by 1, 2




-- PersonalMedicalHistory
select parent_id, b.code, b.name, count(*) as answer_counts_new
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_12000` a
left join (select id, code,name from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_12000`
      where survey in ('PersonalMedicalHistory') and subtype='QUESTION') b on a.parent_id=b.id
where survey in ('PersonalMedicalHistory')
and subtype='ANSWER'
group by 1,2,3
order by 1
;


select parent_id, b.code, b.name, count(*) as answer_counts_old
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig` a
left join (select id, code,name from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
      where survey in ('PersonalMedicalHistory') and subtype='QUESTION') b on a.parent_id=b.id
where survey in ('PersonalMedicalHistory')
and subtype='ANSWER'
group by 1,2,3
order by 1
;
-- ==============================================

select parent_id, b.code, b.name, count(*) as answer_counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_12000` a
left join (select id, code,name from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_12000`
      where survey in ('PersonalMedicalHistory') and subtype='QUESTION') b on a.parent_id=b.id
where survey in ('PersonalMedicalHistory')
and subtype='ANSWER'
group by 1,2,3
order by 1
;




select parent_id, b.code, b.name, count(*) as answer_counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_13000` a
left join (select id, code,name from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_13000`
      where survey in ('February2021COVID19Part') and subtype='QUESTION') b on a.parent_id=b.id
where survey in ('February2021COVID19Part')
and subtype='ANSWER'
group by 1,2,3
order by 1
;





select old_.survey, old_.counts as old_counts, new_.counts as new_counts from
(select survey, count(*) as counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_survey_orig`
where survey in ('July2020Covid19Particip','February2021COVID19Part','May2020Covid19Participa')
group by survey) old_
join (select survey, count(*) as counts
from `all-of-us-workbench-test.chenchals_survey_refactor.prep_temp_prep_survey_13000`
where survey in ('July2020Covid19Particip','February2021COVID19Part','May2020Covid19Participa')
group by survey) new_ on old_.survey=new_.survey
order by 1;
