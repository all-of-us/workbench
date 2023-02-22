-- Use database - replace _SCHEMA_NAME_ with database name
USE _SCHEMA_NAME_;
-- SQL for project name
-- Replace _PROJECT_ with GOOGLE_PROJECT name _SCHEMA_NAME_ with database name
select ' **_PROJECT_ - _SCHEMA_NAME_** ' as project from dual;
-- SQL for cb_ and ds_ table counts
select 'cb_criteria' as table_name, count(*) as row_count from cb_criteria
union
select 'cb_criteria_ancestor' as table_name, count(*) as row_count from cb_criteria_ancestor
union
select 'cb_criteria_attribute' as table_name, count(*) as row_count from cb_criteria_attribute
union
select 'cb_criteria_menu' as table_name, count(*) as row_count from cb_criteria_menu
union
select 'cb_criteria_relationship' as table_name, count(*) as row_count from cb_criteria_relationship
union
select 'cb_data_filter' as table_name, count(*) as row_count from cb_data_filter
union
select 'cb_person' as table_name, count(*) as row_count from cb_person
union
select 'cb_survey_attribute' as table_name, count(*) as row_count from cb_survey_attribute
union
select 'cb_survey_version' as table_name, count(*) as row_count from cb_survey_version
union
select 'ds_data_dictionary' as table_name, count(*) as row_count from ds_data_dictionary
union
select 'ds_linking' as table_name, count(*) as row_count from ds_linking
union
select 'survey_module' as table_name, count(*) as row_count from survey_module
order by 2 desc;

