# All of Us Workbench TOOLS

## Load Data Dictionary
To load/update data dictionary follow the steps:

1. Create 2 yaml files : one for PROD and  another for synthetic CDR, under
    <em>/api/tools/src/main/resources/data_dictionary_exports</em> with the following fields:
    
    a) meta_data -> id, name, version, created_time, modified_time,
       last_modifying_user_display_name and cdr_version (make sure cdr_version matches that of Environment else it will not load entries)
    b) transformations ->  available_fields
    
  
2. To verify on local
  a) run the command 
     ./gradlew loadDataDictionary -PappArgs={true for dry_run else false}
     NOTE:  For other environments : deploy command calls loadDataDictionary
  b) Confirm the new entries are added to table  data_dictionary_entry
  
