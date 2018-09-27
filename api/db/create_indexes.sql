/* Stub out of create_indexes.sql ..
 * importing goes much faster without indexes so we drop before import and create after import.
 */
use ${DB_NAME};
create index idx_concept_name on concept  (concept_name);
create index idx_concept_code on concept  (concept_code);