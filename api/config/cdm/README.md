# Steps for producing CDM configuration

* Set "version" attribute at the top level to the current CDM version.

* Find all the tables in https://github.com/all-of-us/curation/tree/master/data_steward/resources/fields that have person_id -- these tables belong in `cohortTables`. Copy their column metadata into the `columns` array for each table configuration. (See `cdm_5_2.json` for an example.)

* Find all the tables in the same directory that don't have person_id but are referenced transitively by the cohort tables -- these belong in `metadataTables`.

* Add `"primaryKey": true` for the primary key of each table.

* Add `"foreignKey": "TABLE_NAME"` for each foreign key column.

* Add the following columns to the observation table configuration (they are not a part of the standard OMOP schema, but were
added to support AllOfUs):

```
       {
          "type": "integer",
          "name": "value_source_concept_id",
          "mode": "nullable",
          "description": "A foreign key to a Concept for the value in the source data. This is applicable to observations where the result can be expressed as a non-standard concept.",
          "foreignKey": "concept"
        },
        {
          "type": "string",
          "name": "value_source_value",
          "mode": "nullable",
          "description": "The name of the concept referred to be value_source_concept_id. This is applicable to observations where the result can be expressed as a non-standard concept."
        },
        {
          "type": "integer",
          "name": "questionnaire_response_id",
          "mode": "nullable",
          "description": "An ID for a questionnaire response that produced this observation. This is applicable to AllOfUs questionnaire answers only. All answers with the same questionnaire response ID were submitted in the same response."
        }
```

* Replace all forward slashes with \/ (to allow for GSON deserialization.)

When we start supporting a new CDM version, create a new file (either from
scratch or by copying and modifying the copy, depending on how much has
changed). Change references in our build scripts to it, so that it gets loaded 
into the cdrSchema configuration entry in our database.

(In future we may support multiple simultaneous CDR schema configurations;
for now there's just one.)
