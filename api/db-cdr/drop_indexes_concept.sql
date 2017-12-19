use ${CDR_DB_NAME};


ALTER TABLE ${CDR_DB_NAME}.concept
DROP INDEX idx_concept_name
DROP INDEX idx_domain_id,
DROP INDEX idx_vocabulary_id,
DROP INDEX concept_code_index,
DROP INDEX `idx-count-value`,
DROP INDEX idx_standard_concept,
DROP INDEX concept_name_index;

