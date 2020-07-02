# All of Us Workbench TOOLS

## Load Data Dictionary
To load data dictionary follow the following steps:

1. Create yaml file from CDR yaml under /api/tools/src/main/resources/data_dictionary_exports with fields:
    a) meta_data -> id, name, version, created_time, modified_time, last_modifying_user_display_name and cdr_version
    b) transformations ->  available_fields:
2. Make sure the cdr_version in the file is same as that of the current environment cdr version (PROD will be different than other synthetic cdr versions).
3. To verify on local use the command ./gradlew loadDataDictionary -PappArgs={true for dry_run else false}
4. Running the command should create entries in the Database