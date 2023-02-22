-- SQL for project name
select ' **@project - @schema_name** ' as project from dual;
-- SQL for cb_ and ds_ table counts
select 'cb_criteria' as table_name, count(*) as row_count from @schema_name.cb_criteria
union
select 'cb_criteria_ancestor' as table_name, count(*) as row_count from @schema_name.cb_criteria_ancestor
union
select 'cb_criteria_attribute' as table_name, count(*) as row_count from @schema_name.cb_criteria_attribute
union
select 'cb_criteria_menu' as table_name, count(*) as row_count from @schema_name.cb_criteria_menu
union
select 'cb_criteria_relationship' as table_name, count(*) as row_count from @schema_name.cb_criteria_relationship
union
select 'cb_data_filter' as table_name, count(*) as row_count from @schema_name.cb_data_filter
union
select 'cb_person' as table_name, count(*) as row_count from @schema_name.cb_person
union
select 'cb_survey_attribute' as table_name, count(*) as row_count from @schema_name.cb_survey_attribute
union
select 'cb_survey_version' as table_name, count(*) as row_count from @schema_name.cb_survey_version
union
select 'ds_data_dictionary' as table_name, count(*) as row_count from @schema_name.ds_data_dictionary
union
select 'ds_linking' as table_name, count(*) as row_count from @schema_name.ds_linking
union
select 'survey_module' as table_name, count(*) as row_count from @schema_name.survey_module
order by 2 desc;

