# Steps for producing CDM configuration

* Set "version" attribute at the top level to the current CDM version.

* Find all the tables in https://github.com/all-of-us/curation/tree/master/data_steward/resources/fields that have person_id -- these belong in cohortTables.

* Find all the tables that don't but they reference -- these belong in metadataTables.

* Add "primaryKey": true for the primary key of each table.

* Add "foreignKey": "<table name>" for each foreign key column.

* Replace all forward slashes with \/ (to allow for GSON deserialization.)

When we start supporting a new CDM version, create a new file (either from
scratch or by copying and modifying the copy, depending on how much has
changed). Change references in our build scripts to it, so that it gets loaded 
into the cdrSchema configuration entry in our database.

(In future we may support multiple simultaneous CDR schema configurations;
for now there's just one.)
