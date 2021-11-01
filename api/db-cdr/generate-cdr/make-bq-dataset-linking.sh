#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -ex

export BQ_PROJECT=$1  # project
export BQ_DATASET=$2  # dataset

################################################
# CREATE LINKING TABLE
################################################
schema_path=generate-cdr/bq-schemas
create_tables=(ds_linking)

for t in "${create_tables[@]}"
do
    bq --project_id=$BQ_PROJECT rm -f $BQ_DATASET.$t
    bq --quiet --project_id=$BQ_PROJECT mk --schema=$schema_path/$t.json $BQ_DATASET.$t
done

################################################
# INSERT DATA
################################################
echo "ds_linking - inserting condition data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    # We add the core table for domain row to ensure we have a single place to make certain we load in the base table.
    (1, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (2, 'PERSON_ID', 'c_occurrence.person_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (3, 'CONDITION_CONCEPT_ID', 'c_occurrence.condition_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (4, 'STANDARD_CONCEPT_NAME', 'c_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    (5, 'STANDARD_CONCEPT_CODE', 'c_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    (6, 'STANDARD_VOCABULARY', 'c_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    (7, 'CONDITION_START_DATETIME', 'c_occurrence.condition_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (8, 'CONDITION_END_DATETIME', 'c_occurrence.condition_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (9, 'CONDITION_TYPE_CONCEPT_ID', 'c_occurrence.condition_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (10, 'CONDITION_TYPE_CONCEPT_NAME', 'c_type.concept_name as condition_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_type ON c_occurrence.condition_type_concept_id = c_type.concept_id', 'Condition'),
    (11, 'STOP_REASON', 'c_occurrence.stop_reason', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (12, 'VISIT_OCCURRENCE_ID', 'c_occurrence.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (13, 'VISIT_OCCURRENCE_CONCEPT_NAME', 'visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON c_occurrence.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` visit ON v.visit_concept_id = visit.concept_id', 'Condition'),
    (14, 'CONDITION_SOURCE_VALUE', 'c_occurrence.condition_source_value', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (15, 'CONDITION_SOURCE_CONCEPT_ID', 'c_occurrence.condition_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (16, 'SOURCE_CONCEPT_NAME', 'c_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    (17, 'SOURCE_CONCEPT_CODE', 'c_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    (18, 'SOURCE_VOCABULARY', 'c_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    (19, 'CONDITION_STATUS_SOURCE_VALUE', 'c_occurrence.condition_status_source_value', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (20, 'CONDITION_STATUS_CONCEPT_ID', 'c_occurrence.condition_status_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    (21, 'CONDITION_STATUS_CONCEPT_NAME', 'c_status.concept_name as condition_status_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_status ON c_occurrence.condition_status_concept_id = c_status.concept_id', 'Condition')"

echo "ds_linking - inserting drug exposure data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (22, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (23, 'PERSON_ID', 'd_exposure.person_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (24, 'DRUG_CONCEPT_ID', 'd_exposure.drug_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (25, 'STANDARD_CONCEPT_NAME', 'd_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    (26, 'STANDARD_CONCEPT_CODE', 'd_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    (27, 'STANDARD_VOCABULARY', 'd_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    (28, 'DRUG_EXPOSURE_START_DATETIME', 'd_exposure.drug_exposure_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (29, 'DRUG_EXPOSURE_END_DATETIME', 'd_exposure.drug_exposure_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (30, 'VERBATIM_END_DATE', 'd_exposure.verbatim_end_date', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (31, 'DRUG_TYPE_CONCEPT_ID', 'd_exposure.drug_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (32, 'DRUG_TYPE_CONCEPT_NAME', 'd_type.concept_name as drug_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_type ON d_exposure.drug_type_concept_id = d_type.concept_id', 'Drug'),
    (33, 'STOP_REASON', 'd_exposure.stop_reason', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (34, 'REFILLS', 'd_exposure.refills', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (35, 'QUANTITY', 'd_exposure.quantity', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (36, 'DAYS_SUPPLY', 'd_exposure.days_supply', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (37, 'SIG', 'd_exposure.sig', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (38, 'ROUTE_CONCEPT_ID', 'd_exposure.route_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (39, 'ROUTE_CONCEPT_NAME', 'd_route.concept_name as route_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_route ON d_exposure.route_concept_id = d_route.concept_id', 'Drug'),
    (40, 'LOT_NUMBER', 'd_exposure.lot_number', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (41, 'VISIT_OCCURRENCE_ID', 'd_exposure.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (42, 'VISIT_OCCURRENCE_CONCEPT_NAME', 'd_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON d_exposure.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_visit ON v.visit_concept_id = d_visit.concept_id', 'Drug'),
    (43, 'DRUG_SOURCE_VALUE', 'd_exposure.drug_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (44, 'DRUG_SOURCE_CONCEPT_ID', 'd_exposure.drug_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (45, 'SOURCE_CONCEPT_NAME', 'd_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    (46, 'SOURCE_CONCEPT_CODE', 'd_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    (47, 'SOURCE_VOCABULARY', 'd_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    (48, 'ROUTE_SOURCE_VALUE', 'd_exposure.route_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    (49, 'DOSE_UNIT_SOURCE_VALUE', 'd_exposure.dose_unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug')"

echo "ds_linking - inserting measurement data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (50, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (51, 'PERSON_ID', 'measurement.person_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (52, 'MEASUREMENT_CONCEPT_ID', 'measurement.measurement_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (53, 'STANDARD_CONCEPT_NAME', 'm_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    (54, 'STANDARD_CONCEPT_CODE', 'm_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    (55, 'STANDARD_VOCABULARY', 'm_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    (56, 'MEASUREMENT_DATETIME', 'measurement.measurement_datetime', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (57, 'MEASUREMENT_TYPE_CONCEPT_ID', 'measurement.measurement_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (58, 'MEASUREMENT_TYPE_CONCEPT_NAME', 'm_type.concept_name as measurement_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_type ON measurement.measurement_type_concept_id = m_type.concept_id', 'Measurement'),
    (59, 'OPERATOR_CONCEPT_ID', 'measurement.operator_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (60, 'OPERATOR_CONCEPT_NAME', 'm_operator.concept_name as operator_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_operator ON measurement.operator_concept_id = m_operator.concept_id', 'Measurement'),
    (61, 'VALUE_AS_NUMBER', 'measurement.value_as_number', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (62, 'VALUE_AS_CONCEPT_ID', 'measurement.value_as_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (63, 'VALUE_AS_CONCEPT_NAME', 'm_value.concept_name as value_as_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_value ON measurement.value_as_concept_id = m_value.concept_id', 'Measurement'),
    (64, 'UNIT_CONCEPT_ID', 'measurement.unit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (65, 'UNIT_CONCEPT_NAME', 'm_unit.concept_name as unit_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_unit ON measurement.unit_concept_id = m_unit.concept_id', 'Measurement'),
    (66, 'RANGE_LOW', 'measurement.range_low', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (67, 'RANGE_HIGH', 'measurement.range_high', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (68, 'VISIT_OCCURRENCE_ID', 'measurement.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (69, 'VISIT_OCCURRENCE_CONCEPT_NAME', 'm_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIn \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON measurement.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_visit ON v.visit_concept_id = m_visit.concept_id', 'Measurement'),
    (70, 'MEASUREMENT_SOURCE_VALUE', 'measurement.measurement_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (71, 'MEASUREMENT_SOURCE_CONCEPT_ID', 'measurement.measurement_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (72, 'SOURCE_CONCEPT_NAME', 'm_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    (73, 'SOURCE_CONCEPT_CODE', 'm_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    (74, 'SOURCE_VOCABULARY', 'm_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    (75, 'UNIT_SOURCE_VALUE', 'measurement.unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    (76, 'VALUE_SOURCE_VALUE', 'measurement.value_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement')"

echo "ds_linking - inserting observation data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (77, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (78, 'PERSON_ID', 'observation.person_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (79, 'OBSERVATION_CONCEPT_ID', 'observation.observation_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (80, 'STANDARD_CONCEPT_NAME', 'o_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    (81, 'STANDARD_CONCEPT_CODE', 'o_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    (82, 'STANDARD_VOCABULARY', 'o_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    (83, 'OBSERVATION_DATETIME', 'observation.observation_datetime', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (84, 'OBSERVATION_TYPE_CONCEPT_ID', 'observation.observation_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (85, 'OBSERVATION_TYPE_CONCEPT_NAME', 'o_type.concept_name as observation_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_type ON observation.observation_type_concept_id = o_type.concept_id', 'Observation'),
    (86, 'VALUE_AS_NUMBER', 'observation.value_as_number', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (87, 'VALUE_AS_STRING', 'observation.value_as_string', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (88, 'VALUE_AS_CONCEPT_ID', 'observation.value_as_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (89, 'VALUE_AS_CONCEPT_NAME', 'o_value.concept_name as value_as_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_value ON observation.value_as_concept_id = o_value.concept_id', 'Observation'),
    (90, 'QUALIFIER_CONCEPT_ID', 'observation.qualifier_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (91, 'QUALIFIER_CONCEPT_NAME', 'o_qualifier.concept_name as qualifier_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_qualifier ON observation.qualifier_concept_id = o_qualifier.concept_id', 'Observation'),
    (92, 'UNIT_CONCEPT_ID', 'observation.unit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (93, 'UNIT_CONCEPT_NAME', 'o_unit.concept_name as unit_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_unit ON observation.unit_concept_id = o_unit.concept_id', 'Observation'),
    (94, 'VISIT_OCCURRENCE_ID', 'observation.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (95, 'VISIT_OCCURRENCE_CONCEPT_NAME', 'o_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON observation.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_visit ON v.visit_concept_id = o_visit.concept_id', 'Observation'),
    (96, 'OBSERVATION_SOURCE_VALUE', 'observation.observation_source_value', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (97, 'OBSERVATION_SOURCE_CONCEPT_ID', 'observation.observation_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (98, 'SOURCE_CONCEPT_NAME', 'o_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    (99, 'SOURCE_CONCEPT_CODE', 'o_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    (100, 'SOURCE_VOCABULARY', 'o_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    (101, 'UNIT_SOURCE_VALUE', 'observation.unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (102, 'QUALIFIER_SOURCE_VALUE', 'observation.qualifier_source_value', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (103, 'value_source_concept_id', 'observation.value_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (104, 'value_source_value', 'observation.value_source_value', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation'),
    (105, 'questionnaire_response_id', 'observation.questionnaire_response_id', 'FROM \`\${projectId}.\${dataSetId}.ds_observation\` observation', 'Observation')"


echo "ds_linking - inserting person data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (106, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (107, 'PERSON_ID', 'person.person_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (108, 'GENDER_CONCEPT_ID', 'person.gender_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (109, 'GENDER', 'p_gender_concept.concept_name as gender', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_gender_concept ON person.gender_concept_id = p_gender_concept.concept_id', 'Person'),
    (110, 'DATE_OF_BIRTH', 'person.birth_datetime as date_of_birth', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (111, 'RACE_CONCEPT_ID', 'person.race_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (112, 'RACE', 'p_race_concept.concept_name as race', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_race_concept ON person.race_concept_id = p_race_concept.concept_id', 'Person'),
    (113, 'ETHNICITY_CONCEPT_ID', 'person.ethnicity_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (114, 'ETHNICITY', 'p_ethnicity_concept.concept_name as ethnicity', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_ethnicity_concept ON person.ethnicity_concept_id = p_ethnicity_concept.concept_id', 'Person'),
    (115, 'SEX_AT_BIRTH_CONCEPT_ID', 'person.sex_at_birth_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    (116, 'SEX_AT_BIRTH', 'p_sex_at_birth_concept.concept_name as sex_at_birth', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_sex_at_birth_concept ON person.sex_at_birth_concept_id = p_sex_at_birth_concept.concept_id', 'Person')"

echo "ds_linking - inserting procedure data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (117, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (118, 'PERSON_ID', 'procedure.person_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (119, 'PROCEDURE_CONCEPT_ID', 'procedure.procedure_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (120, 'STANDARD_CONCEPT_NAME', 'p_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    (121, 'STANDARD_CONCEPT_CODE', 'p_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    (122, 'STANDARD_VOCABULARY', 'p_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    (123, 'PROCEDURE_DATETIME', 'procedure.procedure_datetime', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (124, 'PROCEDURE_TYPE_CONCEPT_ID', 'procedure.procedure_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (125, 'PROCEDURE_TYPE_CONCEPT_NAME', 'p_type.concept_name as procedure_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_type ON procedure.procedure_type_concept_id = p_type.concept_id', 'Procedure'),
    (126, 'MODIFIER_CONCEPT_ID', 'procedure.modifier_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (127, 'MODIFIER_CONCEPT_NAME', 'p_modifier.concept_name as modifier_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_modifier ON procedure.modifier_concept_id = p_modifier.concept_id', 'Procedure'),
    (128, 'QUANTITY', 'procedure.quantity', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (129, 'VISIT_OCCURRENCE_ID', 'procedure.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (130, 'VISIT_OCCURRENCE_CONCEPT_NAME', 'p_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON procedure.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_visit ON v.visit_concept_id = p_visit.concept_id', 'Procedure'),
    (131, 'PROCEDURE_SOURCE_VALUE', 'procedure.procedure_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (132, 'PROCEDURE_SOURCE_CONCEPT_ID', 'procedure.procedure_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    (133, 'SOURCE_CONCEPT_NAME', 'p_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    (134, 'SOURCE_CONCEPT_CODE', 'p_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    (135, 'SOURCE_VOCABULARY', 'p_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    (136, 'QUALIFIER_SOURCE_VALUE', 'procedure.qualifier_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure')"

echo "ds_linking - inserting survey data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (137, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', ' FROM \`\${projectId}.\${dataSetId}.ds_survey\` answer', 'Survey'),
    (138, 'PERSON_ID', 'answer.person_id', ' ', 'Survey'),
    (139, 'SURVEY_DATETIME', 'answer.survey_datetime', ' ', 'Survey'),
    (140, 'SURVEY', 'answer.survey', ' ', 'Survey'),
    (141, 'QUESTION_CONCEPT_ID', 'answer.question_concept_id', ' ', 'Survey'),
    (142, 'QUESTION', 'answer.question', ' ', 'Survey'),
    (143, 'ANSWER_CONCEPT_ID', 'answer.answer_concept_id', ' ', 'Survey'),
    (144, 'ANSWER', 'answer.answer', ' ', 'Survey'),
    (145, 'SURVEY_VERSION_CONCEPT_ID', 'answer.survey_version_concept_id', ' ', 'Survey'),
    (146, 'SURVEY_VERSION_NAME', 'answer.survey_version_name', ' ', 'Survey')"

echo "ds_linking - inserting visit data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    (148, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (149, 'PERSON_ID', 'visit.PERSON_ID', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (150, 'VISIT_CONCEPT_ID', 'visit.visit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (151, 'STANDARD_CONCEPT_NAME', 'v_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    (152, 'STANDARD_CONCEPT_CODE', 'v_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    (153, 'STANDARD_VOCABULARY', 'v_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    (154, 'VISIT_START_DATETIME', 'visit.visit_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (155, 'VISIT_END_DATETIME', 'visit.visit_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (156, 'VISIT_TYPE_CONCEPT_ID', 'visit.visit_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (157, 'VISIT_TYPE_CONCEPT_NAME', 'v_type.concept_name as visit_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_type ON visit.visit_type_concept_id = v_type.concept_id', 'Visit'),
    (158, 'VISIT_SOURCE_VALUE', 'visit.visit_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (159, 'VISIT_SOURCE_CONCEPT_ID', 'visit.visit_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (160, 'SOURCE_CONCEPT_NAME', 'v_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    (161, 'SOURCE_CONCEPT_CODE', 'v_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    (162, 'SOURCE_VOCABULARY', 'v_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    (163, 'ADMITTING_SOURCE_CONCEPT_ID', 'visit.admitting_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (164, 'ADMITTING_SOURCE_CONCEPT_NAME', 'v_admitting_source_concept.concept_name as admitting_source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_admitting_source_concept ON visit.admitting_source_concept_id = v_admitting_source_concept.concept_id', 'Visit'),
    (165, 'ADMITTING_SOURCE_VALUE', 'visit.admitting_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (166, 'DISCHARGE_TO_CONCEPT_ID', 'visit.discharge_to_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit'),
    (167, 'DISCHARGE_TO_CONCEPT_NAME', 'v_discharge.concept_name as discharge_to_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_discharge ON visit.discharge_to_concept_id = v_discharge.concept_id', 'Visit'),
    (168, 'DISCHARGE_TO_SOURCE_VALUE', 'visit.discharge_to_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` visit', 'Visit')"

echo "ds_linking - inserting fitbit heart_rate_summary data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  (169, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (170, 'PERSON_ID', 'heart_rate_summary.person_id', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (171, 'DATETIME', 'heart_rate_summary.datetime', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (172, 'ZONE_NAME', 'heart_rate_summary.zone_name', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (173, 'MIN_HEART_RATE', 'heart_rate_summary.min_heart_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (174, 'MAX_HEART_RATE', 'heart_rate_summary.max_heart_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (175, 'MINUTE_IN_ZONE', 'heart_rate_summary.minute_in_zone', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  (176, 'CALORIE_COUNT', 'heart_rate_summary.calorie_count', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary')"

echo "ds_linking - inserting fitbit heart_rate_level data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  (177, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  (178, 'PERSON_ID', 'heart_rate_minute_level.person_id', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  (179, 'DATETIME', 'CAST(heart_rate_minute_level.datetime AS DATE) as date', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  (180, 'HEART_RATE_VALUE', 'AVG(heart_rate_value) avg_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level')"

echo "ds_linking - inserting fitbit activity_summary data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  (181, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (182, 'PERSON_ID', 'activity_summary.person_id', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (183, 'DATE', 'activity_summary.date', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (184, 'ACTIVITY_CALORIES', 'activity_summary.activity_calories', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (185, 'CALORIES_BMR', 'activity_summary.calories_bmr', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (186, 'CALORIES_OUT', 'activity_summary.calories_out', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (187, 'ELEVATION', 'activity_summary.elevation', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (188, 'FAIRLY_ACTIVE_MINUTES', 'activity_summary.fairly_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (189, 'FLOOR', 'activity_summary.floor', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (190, 'LIGHTLY_ACTIVE_MINUTES', 'activity_summary.lightly_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (191, 'MARGINAL_CALORIES', 'activity_summary.marginal_calories', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (192, 'SEDENTARY_MINUTES', 'activity_summary.sedentary_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  (193, 'VERY_ACTIVE_MINUTES', 'activity_summary.very_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity')"


echo "ds_linking - inserting fitbit steps_intraday data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  (194, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  (195, 'PERSON_ID', 'steps_intraday.person_id', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  (196, 'DATETIME', 'CAST(steps_intraday.datetime AS DATE) as date', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  (197, 'STEPS', 'SUM(CAST(steps_intraday.steps AS INT64)) as sum_steps', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps')"

echo "ds_linking - inserting zip code socioeconimic data"

bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  (198, 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (199, 'PERSON_ID', 'observation.person_id', 'JOIN \`\${projectId}.\${dataSetId}.observation\` observation ON CAST(SUBSTR(observation.value_as_string, 0, STRPOS(observation.value_as_string, \'*\') - 1) AS INT64) = zip_code.zip3', 'Zip_code_socioeconomic'),
  (200, 'OBSERVATION_DATETIME', 'observation.observation_datetime', 'JOIN \`\${projectId}.\${dataSetId}.observation\` observation ON CAST(SUBSTR(observation.value_as_string, 0, STRPOS(observation.value_as_string, \'*\') - 1) AS INT64) = zip_code.zip3', 'Zip_code_socioeconomic'),
  (201, 'ZIP_CODE', 'zip_code.zip3_as_string as zip_code', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (202, 'ASSISTED_INCOME', 'zip_code.fraction_assisted_income as assisted_income', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (203, 'HIGH_SCHOOL_EDUCATION', 'zip_code.fraction_high_school_edu as high_school_education', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (204, 'MEDIAN_INCOME', 'zip_code.median_income', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (205, 'NO_HEALTH_INSURANCE', 'zip_code.fraction_no_health_ins as no_health_insurance', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (206, 'POVERTY', 'zip_code.fraction_poverty as poverty', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (207, 'VACANT_HOUSING', 'zip_code.fraction_vacant_housing as vacant_housing', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (208, 'DEPRIVATION_INDEX', 'zip_code.deprivation_index', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  (209, 'AMERICAN_COMMUNITY_SURVEY_YEAR', 'zip_code.acs as american_community_survey_year', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic')"

