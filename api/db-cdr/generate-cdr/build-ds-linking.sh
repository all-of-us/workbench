#!/bin/bash

# This generates the big query de-normalized tables for dataset builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

ID=1

################################################
# INSERT DATA
################################################
echo "ds_linking - inserting condition data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    # We add the core table for domain row to ensure we have a single place to make certain we load in the base table.
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'PERSON_ID', 'c_occurrence.person_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_CONCEPT_ID', 'c_occurrence.condition_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'c_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'c_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'c_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_standard_concept ON c_occurrence.condition_concept_id = c_standard_concept.concept_id', 'Condition'),
    ($((ID++)), 'CONDITION_START_DATETIME', 'c_occurrence.condition_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_END_DATETIME', 'c_occurrence.condition_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_TYPE_CONCEPT_ID', 'c_occurrence.condition_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_TYPE_CONCEPT_NAME', 'c_type.concept_name as condition_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_type ON c_occurrence.condition_type_concept_id = c_type.concept_id', 'Condition'),
    ($((ID++)), 'STOP_REASON', 'c_occurrence.stop_reason', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'VISIT_OCCURRENCE_ID', 'c_occurrence.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON c_occurrence.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` visit ON v.visit_concept_id = visit.concept_id', 'Condition'),
    ($((ID++)), 'CONDITION_SOURCE_VALUE', 'c_occurrence.condition_source_value', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_SOURCE_CONCEPT_ID', 'c_occurrence.condition_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'c_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'c_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'c_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_source_concept ON c_occurrence.condition_source_concept_id = c_source_concept.concept_id', 'Condition'),
    ($((ID++)), 'CONDITION_STATUS_SOURCE_VALUE', 'c_occurrence.condition_status_source_value', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_STATUS_CONCEPT_ID', 'c_occurrence.condition_status_concept_id', 'FROM \`\${projectId}.\${dataSetId}.condition_occurrence\` c_occurrence', 'Condition'),
    ($((ID++)), 'CONDITION_STATUS_CONCEPT_NAME', 'c_status.concept_name as condition_status_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` c_status ON c_occurrence.condition_status_concept_id = c_status.concept_id', 'Condition')"

echo "ds_linking - inserting drug exposure data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'PERSON_ID', 'd_exposure.person_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DRUG_CONCEPT_ID', 'd_exposure.drug_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'd_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'd_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'd_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON d_exposure.drug_concept_id = d_standard_concept.concept_id', 'Drug'),
    ($((ID++)), 'DRUG_EXPOSURE_START_DATETIME', 'd_exposure.drug_exposure_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DRUG_EXPOSURE_END_DATETIME', 'd_exposure.drug_exposure_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'VERBATIM_END_DATE', 'd_exposure.verbatim_end_date', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DRUG_TYPE_CONCEPT_ID', 'd_exposure.drug_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DRUG_TYPE_CONCEPT_NAME', 'd_type.concept_name as drug_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_type ON d_exposure.drug_type_concept_id = d_type.concept_id', 'Drug'),
    ($((ID++)), 'STOP_REASON', 'd_exposure.stop_reason', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'REFILLS', 'd_exposure.refills', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'QUANTITY', 'd_exposure.quantity', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DAYS_SUPPLY', 'd_exposure.days_supply', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'SIG', 'd_exposure.sig', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'ROUTE_CONCEPT_ID', 'd_exposure.route_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'ROUTE_CONCEPT_NAME', 'd_route.concept_name as route_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_route ON d_exposure.route_concept_id = d_route.concept_id', 'Drug'),
    ($((ID++)), 'LOT_NUMBER', 'd_exposure.lot_number', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'VISIT_OCCURRENCE_ID', 'd_exposure.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'd_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON d_exposure.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_visit ON v.visit_concept_id = d_visit.concept_id', 'Drug'),
    ($((ID++)), 'DRUG_SOURCE_VALUE', 'd_exposure.drug_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DRUG_SOURCE_CONCEPT_ID', 'd_exposure.drug_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'd_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'd_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'd_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON d_exposure.drug_source_concept_id = d_source_concept.concept_id', 'Drug'),
    ($((ID++)), 'ROUTE_SOURCE_VALUE', 'd_exposure.route_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug'),
    ($((ID++)), 'DOSE_UNIT_SOURCE_VALUE', 'd_exposure.dose_unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.drug_exposure\` d_exposure', 'Drug')"

echo "ds_linking - inserting measurement data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'PERSON_ID', 'measurement.person_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_CONCEPT_ID', 'measurement.measurement_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'm_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'm_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'm_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_standard_concept ON measurement.measurement_concept_id = m_standard_concept.concept_id', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_DATETIME', 'measurement.measurement_datetime', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_TYPE_CONCEPT_ID', 'measurement.measurement_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_TYPE_CONCEPT_NAME', 'm_type.concept_name as measurement_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_type ON measurement.measurement_type_concept_id = m_type.concept_id', 'Measurement'),
    ($((ID++)), 'OPERATOR_CONCEPT_ID', 'measurement.operator_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'OPERATOR_CONCEPT_NAME', 'm_operator.concept_name as operator_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_operator ON measurement.operator_concept_id = m_operator.concept_id', 'Measurement'),
    ($((ID++)), 'VALUE_AS_NUMBER', 'measurement.value_as_number', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'VALUE_AS_CONCEPT_ID', 'measurement.value_as_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'VALUE_AS_CONCEPT_NAME', 'm_value.concept_name as value_as_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_value ON measurement.value_as_concept_id = m_value.concept_id', 'Measurement'),
    ($((ID++)), 'UNIT_CONCEPT_ID', 'measurement.unit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'UNIT_CONCEPT_NAME', 'm_unit.concept_name as unit_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_unit ON measurement.unit_concept_id = m_unit.concept_id', 'Measurement'),
    ($((ID++)), 'RANGE_LOW', 'measurement.range_low', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'RANGE_HIGH', 'measurement.range_high', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'VISIT_OCCURRENCE_ID', 'measurement.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'm_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIn \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON measurement.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_visit ON v.visit_concept_id = m_visit.concept_id', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_SOURCE_VALUE', 'measurement.measurement_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'MEASUREMENT_SOURCE_CONCEPT_ID', 'measurement.measurement_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'm_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'm_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'm_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` m_source_concept ON measurement.measurement_source_concept_id = m_source_concept.concept_id', 'Measurement'),
    ($((ID++)), 'UNIT_SOURCE_VALUE', 'measurement.unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement'),
    ($((ID++)), 'VALUE_SOURCE_VALUE', 'measurement.value_source_value', 'FROM \`\${projectId}.\${dataSetId}.measurement\` measurement', 'Measurement')"

echo "ds_linking - inserting observation data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'PERSON_ID', 'observation.person_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'OBSERVATION_CONCEPT_ID', 'observation.observation_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'o_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'o_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'o_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_standard_concept ON observation.observation_concept_id = o_standard_concept.concept_id', 'Observation'),
    ($((ID++)), 'OBSERVATION_DATETIME', 'observation.observation_datetime', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'OBSERVATION_TYPE_CONCEPT_ID', 'observation.observation_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'OBSERVATION_TYPE_CONCEPT_NAME', 'o_type.concept_name as observation_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_type ON observation.observation_type_concept_id = o_type.concept_id', 'Observation'),
    ($((ID++)), 'VALUE_AS_NUMBER', 'observation.value_as_number', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'VALUE_AS_STRING', 'observation.value_as_string', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'VALUE_AS_CONCEPT_ID', 'observation.value_as_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'VALUE_AS_CONCEPT_NAME', 'o_value.concept_name as value_as_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_value ON observation.value_as_concept_id = o_value.concept_id', 'Observation'),
    ($((ID++)), 'QUALIFIER_CONCEPT_ID', 'observation.qualifier_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'QUALIFIER_CONCEPT_NAME', 'o_qualifier.concept_name as qualifier_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_qualifier ON observation.qualifier_concept_id = o_qualifier.concept_id', 'Observation'),
    ($((ID++)), 'UNIT_CONCEPT_ID', 'observation.unit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'UNIT_CONCEPT_NAME', 'o_unit.concept_name as unit_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_unit ON observation.unit_concept_id = o_unit.concept_id', 'Observation'),
    ($((ID++)), 'VISIT_OCCURRENCE_ID', 'observation.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'o_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON observation.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_visit ON v.visit_concept_id = o_visit.concept_id', 'Observation'),
    ($((ID++)), 'OBSERVATION_SOURCE_VALUE', 'observation.observation_source_value', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'OBSERVATION_SOURCE_CONCEPT_ID', 'observation.observation_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'o_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'o_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'o_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` o_source_concept ON observation.observation_source_concept_id = o_source_concept.concept_id', 'Observation'),
    ($((ID++)), 'UNIT_SOURCE_VALUE', 'observation.unit_source_value', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'QUALIFIER_SOURCE_VALUE', 'observation.qualifier_source_value', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'value_source_concept_id', 'observation.value_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'value_source_value', 'observation.value_source_value', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation'),
    ($((ID++)), 'questionnaire_response_id', 'observation.questionnaire_response_id', 'FROM \`\${projectId}.\${dataSetId}.observation\` observation', 'Observation')"

echo "ds_linking - inserting person data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'PERSON_ID', 'person.person_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'GENDER_CONCEPT_ID', 'person.gender_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'GENDER', 'p_gender_concept.concept_name as gender', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_gender_concept ON person.gender_concept_id = p_gender_concept.concept_id', 'Person'),
    ($((ID++)), 'DATE_OF_BIRTH', 'person.birth_datetime as date_of_birth', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'RACE_CONCEPT_ID', 'person.race_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'RACE', 'p_race_concept.concept_name as race', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_race_concept ON person.race_concept_id = p_race_concept.concept_id', 'Person'),
    ($((ID++)), 'ETHNICITY_CONCEPT_ID', 'person.ethnicity_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'ETHNICITY', 'p_ethnicity_concept.concept_name as ethnicity', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_ethnicity_concept ON person.ethnicity_concept_id = p_ethnicity_concept.concept_id', 'Person'),
    ($((ID++)), 'SEX_AT_BIRTH_CONCEPT_ID', 'person.sex_at_birth_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
    ($((ID++)), 'SEX_AT_BIRTH', 'p_sex_at_birth_concept.concept_name as sex_at_birth', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_sex_at_birth_concept ON person.sex_at_birth_concept_id = p_sex_at_birth_concept.concept_id', 'Person')"

echo "Getting self_reported_category_concept_id column count"
query="select count(column_name) as count from \`$BQ_PROJECT.$BQ_DATASET.INFORMATION_SCHEMA.COLUMNS\`
where table_name=\"person\" AND column_name = \"self_reported_category_concept_id\""
selfReportedCategoryDataCount=$(bq --quiet --project_id="$BQ_PROJECT" query --nouse_legacy_sql "$query" | tr -dc '0-9')

if [[ $selfReportedCategoryDataCount > 0 ]];
  then
    bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
    "INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
    VALUES
        ($((ID++)), 'SELF_REPORTED_CATEGORY_CONCEPT_ID', 'person.self_reported_category_concept_id', 'FROM \`\${projectId}.\${dataSetId}.person\` person', 'Person'),
        ($((ID++)), 'SELF_REPORTED_CATEGORY', 'p_self_reported_category_concept.concept_name as self_reported_category', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_self_reported_category_concept ON person.self_reported_category_concept_id = p_self_reported_category_concept.concept_id', 'Person')"
fi

echo "ds_linking - inserting procedure data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'PERSON_ID', 'procedure.person_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'PROCEDURE_CONCEPT_ID', 'procedure.procedure_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'p_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'p_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'p_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_standard_concept ON procedure.procedure_concept_id = p_standard_concept.concept_id', 'Procedure'),
    ($((ID++)), 'PROCEDURE_DATETIME', 'procedure.procedure_datetime', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'PROCEDURE_TYPE_CONCEPT_ID', 'procedure.procedure_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'PROCEDURE_TYPE_CONCEPT_NAME', 'p_type.concept_name as procedure_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_type ON procedure.procedure_type_concept_id = p_type.concept_id', 'Procedure'),
    ($((ID++)), 'MODIFIER_CONCEPT_ID', 'procedure.modifier_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'MODIFIER_CONCEPT_NAME', 'p_modifier.concept_name as modifier_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_modifier ON procedure.modifier_concept_id = p_modifier.concept_id', 'Procedure'),
    ($((ID++)), 'QUANTITY', 'procedure.quantity', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'VISIT_OCCURRENCE_ID', 'procedure.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'p_visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON procedure.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_visit ON v.visit_concept_id = p_visit.concept_id', 'Procedure'),
    ($((ID++)), 'PROCEDURE_SOURCE_VALUE', 'procedure.procedure_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'PROCEDURE_SOURCE_CONCEPT_ID', 'procedure.procedure_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'p_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'p_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'p_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` p_source_concept ON procedure.procedure_source_concept_id = p_source_concept.concept_id', 'Procedure'),
    ($((ID++)), 'MODIFIER_SOURCE_VALUE', 'procedure.modifier_source_value', 'FROM \`\${projectId}.\${dataSetId}.procedure_occurrence\` procedure', 'Procedure')"

echo "ds_linking - inserting survey data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', ' FROM \`\${projectId}.\${dataSetId}.ds_survey\` answer', 'Survey'),
    ($((ID++)), 'PERSON_ID', 'answer.person_id', ' ', 'Survey'),
    ($((ID++)), 'SURVEY_DATETIME', 'answer.survey_datetime', ' ', 'Survey'),
    ($((ID++)), 'SURVEY', 'answer.survey', ' ', 'Survey'),
    ($((ID++)), 'QUESTION_CONCEPT_ID', 'answer.question_concept_id', ' ', 'Survey'),
    ($((ID++)), 'QUESTION', 'answer.question', ' ', 'Survey'),
    ($((ID++)), 'ANSWER_CONCEPT_ID', 'answer.answer_concept_id', ' ', 'Survey'),
    ($((ID++)), 'ANSWER', 'answer.answer', ' ', 'Survey'),
    ($((ID++)), 'SURVEY_VERSION_CONCEPT_ID', 'answer.survey_version_concept_id', ' ', 'Survey'),
    ($((ID++)), 'SURVEY_VERSION_NAME', 'answer.survey_version_name', ' ', 'Survey')"

echo "ds_linking - inserting visit data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
    ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'PERSON_ID', 'visit.PERSON_ID', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'VISIT_CONCEPT_ID', 'visit.visit_concept_id', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'STANDARD_CONCEPT_NAME', 'v_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    ($((ID++)), 'STANDARD_CONCEPT_CODE', 'v_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    ($((ID++)), 'STANDARD_VOCABULARY', 'v_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_standard_concept ON visit.visit_concept_id = v_standard_concept.concept_id', 'Visit'),
    ($((ID++)), 'VISIT_START_DATETIME', 'visit.visit_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'VISIT_END_DATETIME', 'visit.visit_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'VISIT_TYPE_CONCEPT_ID', 'visit.visit_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'VISIT_TYPE_CONCEPT_NAME', 'v_type.concept_name as visit_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_type ON visit.visit_type_concept_id = v_type.concept_id', 'Visit'),
    ($((ID++)), 'VISIT_SOURCE_VALUE', 'visit.visit_source_value', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'VISIT_SOURCE_CONCEPT_ID', 'visit.visit_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'SOURCE_CONCEPT_NAME', 'v_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    ($((ID++)), 'SOURCE_CONCEPT_CODE', 'v_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    ($((ID++)), 'SOURCE_VOCABULARY', 'v_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_source_concept ON visit.visit_source_concept_id = v_source_concept.concept_id', 'Visit'),
    ($((ID++)), 'ADMITTING_SOURCE_CONCEPT_ID', 'visit.admitting_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'ADMITTING_SOURCE_CONCEPT_NAME', 'v_admitting_source_concept.concept_name as admitting_source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_admitting_source_concept ON visit.admitting_source_concept_id = v_admitting_source_concept.concept_id', 'Visit'),
    ($((ID++)), 'ADMITTING_SOURCE_VALUE', 'visit.admitting_source_value', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'DISCHARGE_TO_CONCEPT_ID', 'visit.discharge_to_concept_id', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit'),
    ($((ID++)), 'DISCHARGE_TO_CONCEPT_NAME', 'v_discharge.concept_name as discharge_to_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` v_discharge ON visit.discharge_to_concept_id = v_discharge.concept_id', 'Visit'),
    ($((ID++)), 'DISCHARGE_TO_SOURCE_VALUE', 'visit.discharge_to_source_value', 'FROM \`\${projectId}.\${dataSetId}.visit_occurrence\` visit', 'Visit')"

echo "ds_linking - inserting fitbit heart_rate_summary data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'PERSON_ID', 'heart_rate_summary.person_id', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'DATE', 'heart_rate_summary.date', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'ZONE_NAME', 'heart_rate_summary.zone_name', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'MIN_HEART_RATE', 'heart_rate_summary.min_heart_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'MAX_HEART_RATE', 'heart_rate_summary.max_heart_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'MINUTE_IN_ZONE', 'heart_rate_summary.minute_in_zone', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary'),
  ($((ID++)), 'CALORIE_COUNT', 'heart_rate_summary.calorie_count', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_summary\` heart_rate_summary', 'Fitbit_heart_rate_summary')"

echo "ds_linking - inserting fitbit heart_rate_level data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  ($((ID++)), 'PERSON_ID', 'heart_rate_minute_level.person_id', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  ($((ID++)), 'DATETIME', 'CAST(heart_rate_minute_level.datetime AS DATE) as date', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level'),
  ($((ID++)), 'HEART_RATE_VALUE', 'AVG(heart_rate_value) avg_rate', 'FROM \`\${projectId}.\${dataSetId}.heart_rate_minute_level\` heart_rate_minute_level', 'Fitbit_heart_rate_level')"

echo "ds_linking - inserting fitbit activity_summary data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'PERSON_ID', 'activity_summary.person_id', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'DATE', 'activity_summary.date', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'ACTIVITY_CALORIES', 'activity_summary.activity_calories', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'CALORIES_BMR', 'activity_summary.calories_bmr', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'CALORIES_OUT', 'activity_summary.calories_out', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'ELEVATION', 'activity_summary.elevation', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'FAIRLY_ACTIVE_MINUTES', 'activity_summary.fairly_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'FLOORS', 'activity_summary.floors', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'LIGHTLY_ACTIVE_MINUTES', 'activity_summary.lightly_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'MARGINAL_CALORIES', 'activity_summary.marginal_calories', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'SEDENTARY_MINUTES', 'activity_summary.sedentary_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'STEPS', 'activity_summary.steps', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity'),
  ($((ID++)), 'VERY_ACTIVE_MINUTES', 'activity_summary.very_active_minutes', 'FROM \`\${projectId}.\${dataSetId}.activity_summary\` activity_summary', 'Fitbit_activity')"

echo "ds_linking - inserting fitbit steps_intraday data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  ($((ID++)), 'PERSON_ID', 'steps_intraday.person_id', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  ($((ID++)), 'DATETIME', 'CAST(steps_intraday.datetime AS DATE) as date', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps'),
  ($((ID++)), 'STEPS', 'SUM(CAST(steps_intraday.steps AS INT64)) as sum_steps', 'FROM \`\${projectId}.\${dataSetId}.steps_intraday\` steps_intraday', 'Fitbit_intraday_steps')"

echo "ds_linking - inserting zip code socioeconimic data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'PERSON_ID', 'observation.person_id', 'JOIN \`\${projectId}.\${dataSetId}.observation\` observation ON CAST(SUBSTR(observation.value_as_string, 0, STRPOS(observation.value_as_string, \'*\') - 1) AS INT64) = zip_code.zip3', 'Zip_code_socioeconomic'),
  ($((ID++)), 'OBSERVATION_DATETIME', 'observation.observation_datetime', 'JOIN \`\${projectId}.\${dataSetId}.observation\` observation ON CAST(SUBSTR(observation.value_as_string, 0, STRPOS(observation.value_as_string, \'*\') - 1) AS INT64) = zip_code.zip3', 'Zip_code_socioeconomic'),
  ($((ID++)), 'ZIP3_AS_STRING', 'zip_code.zip3_as_string as zip_code', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'FRACTION_ASSISTED_INCOME', 'zip_code.fraction_assisted_income as assisted_income', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'FRACTION_HIGH_SCHOOL_EDU', 'zip_code.fraction_high_school_edu as high_school_education', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'MEDIAN_INCOME', 'zip_code.median_income', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'FRACTION_NO_HEALTH_INS', 'zip_code.fraction_no_health_ins as no_health_insurance', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'FRACTION_POVERTY', 'zip_code.fraction_poverty as poverty', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'FRACTION_VACANT_HOUSING', 'zip_code.fraction_vacant_housing as vacant_housing', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'DEPRIVATION_INDEX', 'zip_code.deprivation_index', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic'),
  ($((ID++)), 'ACS', 'zip_code.acs as american_community_survey_year', 'FROM \`\${projectId}.\${dataSetId}.zip3_ses_map\` zip_code', 'Zip_code_socioeconomic')"

echo "ds_linking - inserting device data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
 VALUES
     # We add the core table for domain row to ensure we have a single place to make certain we load in the base table.
     ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'PERSON_ID', 'device.person_id', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'DEVICE_CONCEPT_ID', 'device.device_concept_id', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'STANDARD_CONCEPT_NAME', 'd_standard_concept.concept_name as standard_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON device.device_concept_id = d_standard_concept.concept_id', 'Device'),
     ($((ID++)), 'STANDARD_CONCEPT_CODE', 'd_standard_concept.concept_code as standard_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON device.device_concept_id = d_standard_concept.concept_id', 'Device'),
     ($((ID++)), 'STANDARD_VOCABULARY', 'd_standard_concept.vocabulary_id as standard_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_standard_concept ON device.device_concept_id = d_standard_concept.concept_id', 'Device'),
     ($((ID++)), 'DEVICE_EXPOSURE_START_DATETIME', 'device.device_exposure_start_datetime', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'DEVICE_EXPOSURE_END_DATETIME', 'device.device_exposure_end_datetime', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'DEVICE_TYPE_CONCEPT_ID', 'device.device_type_concept_id', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'DEVICE_TYPE_CONCEPT_NAME', 'd_type.concept_name as device_type_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_type ON device.device_type_concept_id = d_type.concept_id', 'Device'),
     ($((ID++)), 'VISIT_OCCURRENCE_ID', 'device.visit_occurrence_id', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'VISIT_OCCURRENCE_CONCEPT_NAME', 'visit.concept_name as visit_occurrence_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.visit_occurrence\` v ON device.visit_occurrence_id = v.visit_occurrence_id LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` visit ON v.visit_concept_id = visit.concept_id', 'Device'),
     ($((ID++)), 'DEVICE_SOURCE_VALUE', 'device.device_source_value', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'DEVICE_SOURCE_CONCEPT_ID', 'device.device_source_concept_id', 'FROM \`\${projectId}.\${dataSetId}.device_exposure\` device', 'Device'),
     ($((ID++)), 'SOURCE_CONCEPT_NAME', 'd_source_concept.concept_name as source_concept_name', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON device.device_source_concept_id = d_source_concept.concept_id', 'Device'),
     ($((ID++)), 'SOURCE_CONCEPT_CODE', 'd_source_concept.concept_code as source_concept_code', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON device.device_source_concept_id = d_source_concept.concept_id', 'Device'),
     ($((ID++)), 'SOURCE_VOCABULARY', 'd_source_concept.vocabulary_id as source_vocabulary', 'LEFT JOIN \`\${projectId}.\${dataSetId}.concept\` d_source_concept ON device.device_source_concept_id = d_source_concept.concept_id', 'Device')"

echo "ds_linking - inserting fitbit sleep_daily_summary data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'PERSON_ID', 'sleep_daily_summary.person_id', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'SLEEP_DATE', 'sleep_daily_summary.sleep_date', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'IS_MAIN_SLEEP', 'sleep_daily_summary.is_main_sleep', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_IN_BED', 'sleep_daily_summary.minute_in_bed', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_ASLEEP', 'sleep_daily_summary.minute_asleep', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_AFTER_WAKEUP', 'sleep_daily_summary.minute_after_wakeup', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_AWAKE', 'sleep_daily_summary.minute_awake', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_RESTLESS', 'sleep_daily_summary.minute_restless', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_DEEP', 'sleep_daily_summary.minute_deep', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_LIGHT', 'sleep_daily_summary.minute_light', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_REM', 'sleep_daily_summary.minute_rem', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary'),
  ($((ID++)), 'MINUTE_WAKE', 'sleep_daily_summary.minute_wake', 'FROM \`\${projectId}.\${dataSetId}.sleep_daily_summary\` sleep_daily_summary', 'Fitbit_sleep_daily_summary')"

echo "ds_linking - inserting fitbit sleep_level data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'PERSON_ID', 'sleep_level.person_id', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'SLEEP_DATE', 'sleep_level.sleep_date', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'IS_MAIN_SLEEP', 'sleep_level.is_main_sleep', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'LEVEL', 'sleep_level.level', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'START_DATETIME', 'CAST(sleep_level.start_datetime AS DATE) as date', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level'),
  ($((ID++)), 'DURATION_IN_MIN', 'sleep_level.duration_in_min', 'FROM \`\${projectId}.\${dataSetId}.sleep_level\` sleep_level', 'Fitbit_sleep_level')"
  
echo "ds_linking - inserting fitbit device data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'PERSON_ID', 'device.person_id', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'DEVICE_ID', 'device.device_id', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'DEVICE_DATE', 'device.device_date', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'BATTERY', 'device.battery', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'BATTERY_LEVEL', 'device.battery_level', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'DEVICE_VERSION', 'device.device_version', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'DEVICE_TYPE', 'device.device_type', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'LAST_SYNC_TIME', 'CAST(device.last_sync_time AS DATE) as last_sync_time', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device'),
  ($((ID++)), 'SRC_ID', 'device.src_id', 'FROM \`\${projectId}.\${dataSetId}.device\` device', 'Fitbit_device')"
  
echo "ds_linking - inserting EtM delaydiscounting metadata"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'SITTING_ID', 'delay.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'PERSON_ID', 'delay.person_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'SRC_ID', 'delay.src_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'RESPONSE_DEVICE', 'delay.metadata.response_device', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'SCREEN_HEIGHT', 'delay.metadata.screen_height', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'SCREEN_WIDTH', 'delay.metadata.screen_width', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TOUCH', 'delay.metadata.touch', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'OPERATING_SYSTEM', 'delay.metadata.operating_system', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_DURATION', 'delay.metadata.test_duration', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_RESTARTED', 'delay.metadata.test_restarted', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_VERSION', 'delay.metadata.test_version', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_NAME', 'delay.metadata.test_name', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_SHORT_NAME', 'delay.metadata.test_short_name', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_LANGUAGE', 'delay.metadata.test_language', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'AOU_VERSION', 'delay.metadata.aou_version', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_PARAMS', 'delay.metadata.test_params', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'USER_UTC_OFFSET', 'delay.metadata.user_utc_offset', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_START_DATE_TIME', 'delay.metadata.test_start_date_time', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'TEST_END_DATE_TIME', 'delay.metadata.test_end_date_time', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata'),
  ($((ID++)), 'USER_AGENT', 'delay.metadata.user_agent', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_metadata')"
  
echo "ds_linking - inserting EtM delaydiscounting outcomes"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'SITTING_ID', 'delay.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'PERSON_ID', 'delay.person_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'SRC_ID', 'delay.src_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'SCORE', 'delay.outcomes.score', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'CATCH_SCORE', 'delay.outcomes.catch_score', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'LNK', 'delay.outcomes.lnk', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'TWO_WEEKS_LNK', 'delay.outcomes.two_weeks_lnk', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'ONE_MONTH_LNK', 'delay.outcomes.one_month_lnk', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'ONE_YEAR_LNK', 'delay.outcomes.one_year_lnk', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'TEN_YEARS_LNK', 'delay.outcomes.ten_years_lnk', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'FLAG_CATCH_TRIALS', 'delay.outcomes.flag_catch_trials', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'ANY_TIMEOUTS', 'delay.outcomes.any_timeouts', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'MEAN_RT', 'delay.outcomes.mean_rt', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'MEDIAN_RT', 'delay.outcomes.median_rt', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'SD_RT', 'delay.outcomes.sd_rt', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes'),
  ($((ID++)), 'FLAG_MEDIAN_RT', 'delay.outcomes.flag_median_rt', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` delay', 'Etm_delaydiscounting_outcomes')"
  
echo "ds_linking - inserting EtM delaydiscounting trial data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'SITTING_ID', 'sitting_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'PERSON_ID', 'person_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'SRC_ID', 'src_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'LINK_ID', 'delay.link_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'TRIAL_ID', 'delay.trial_id', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'TRIAL_TYPE', 'delay.trial_type', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'TYPE_NUM', 'delay.type_num', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'DELAY_TIME', 'delay.delay_time', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'DELAY_TIME_DAYS', 'delay.delay_time_days', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'RESPONSE', 'delay.response', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'PRESENT_AMOUNT', 'delay.present_amount', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'FUTURE_AMOUNT', 'delay.future_amount', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'CATCH_CORRECT', 'delay.catch_correct', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'CURRENT_K', 'delay.current_k', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'REACTION_TIME', 'delay.reaction_time', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'STATE', 'delay.state', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'REPEATED', 'delay.repeated', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data'),
  ($((ID++)), 'TRIAL_TIMESTAMP', 'delay.trial_timestamp', 'FROM \`\${projectId}.\${dataSetId}.delaydiscounting\` CROSS JOIN UNNEST(trial_data) delay', 'Etm_delaydiscounting_trial_data')"

echo "ds_linking - inserting EtM emorecog metadata"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'SITTING_ID', 'emore.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'PERSON_ID', 'emore.person_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'SRC_ID', 'emore.src_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'RESPONSE_DEVICE', 'emore.metadata.response_device', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'SCREEN_HEIGHT', 'emore.metadata.screen_height', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'SCREEN_WIDTH', 'emore.metadata.screen_width', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TOUCH', 'emore.metadata.touch', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'OPERATING_SYSTEM', 'emore.metadata.operating_system', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_DURATION', 'emore.metadata.test_duration', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_RESTARTED', 'emore.metadata.test_restarted', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_VERSION', 'emore.metadata.test_version', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_NAME', 'emore.metadata.test_name', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_SHORT_NAME', 'emore.metadata.test_short_name', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_LANGUAGE', 'emore.metadata.test_language', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'AOU_VERSION', 'emore.metadata.aou_version', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_PARAMS', 'emore.metadata.test_params', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'USER_UTC_OFFSET', 'emore.metadata.user_utc_offset', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_START_DATE_TIME', 'emore.metadata.test_start_date_time', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'TEST_END_DATE_TIME', 'emore.metadata.test_end_date_time', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata'),
  ($((ID++)), 'USER_AGENT', 'emore.metadata.user_agent', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_metadata')"
  
echo "ds_linking - inserting EtM emorecog outcomes"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SITTING_ID', 'emore.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'PERSON_ID', 'emore.person_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SRC_ID', 'emore.src_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SCORE', 'emore.outcomes.score', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'ACCURACY', 'emore.outcomes.accuracy', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'MEAN_RTC', 'emore.outcomes.mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'MEDIAN_RTC', 'emore.outcomes.median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SD_RTC', 'emore.outcomes.sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'HAPPY_ACCURACY', 'emore.outcomes.happy_accuracy', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'HAPPY_MEAN_RTC', 'emore.outcomes.happy_mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'HAPPY_MEDIAN_RTC', 'emore.outcomes.happy_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'HAPPY_SD_RTC', 'emore.outcomes.happy_sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'ANGRY_ACCURACY', 'emore.outcomes.angry_accuracy', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'ANGRY_MEAN_RTC', 'emore.outcomes.angry_mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'ANGRY_MEDIAN_RTC', 'emore.outcomes.angry_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'ANGRY_SD_RTC', 'emore.outcomes.angry_sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SAD_ACCURACY', 'emore.outcomes.sad_accuracy', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SAD_MEAN_RTC', 'emore.outcomes.sad_mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SAD_MEDIAN_RTC', 'emore.outcomes.sad_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'SAD_SD_RTC', 'emore.outcomes.sad_sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FEARFUL_ACCURACY', 'emore.outcomes.fearful_accuracy', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FEARFUL_MEAN_RTC', 'emore.outcomes.fearful_mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FEARFUL_MEDIAN_RTC', 'emore.outcomes.fearful_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FEARFUL_SD_RTC', 'emore.outcomes.fearful_sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FLAG_MEDIAN_RTC', 'emore.outcomes.flag_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FLAG_SAME_RESPONSE', 'emore.outcomes.flag_same_response', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'FLAG_TRIAL_FLAGS', 'emore.outcomes.flag_trial_flags', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes'),
  ($((ID++)), 'any_timeouts', 'emore.outcomes.any_timeouts', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` emore', 'Etm_emorecog_outcomes')"
  
echo "ds_linking - inserting EtM emorecog trial data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'SITTING_ID', 'sitting_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'PERSON_ID', 'person_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'SRC_ID', 'src_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'LINK_ID', 'emore.link_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'TRIAL_ID', 'emore.trial_id', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'EMOTION', 'emore.emotion', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'RESPONSE', 'emore.response', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'CORRECT', 'emore.correct', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'REACTION_TIME', 'emore.reaction_time', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'STATE', 'emore.state', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'REPEATED', 'emore.repeated', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'FLAGGED', 'emore.flagged', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'IMAGE', 'emore.image', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data'),
  ($((ID++)), 'TRIAL_TIMESTAMP', 'emore.trial_timestamp', 'FROM \`\${projectId}.\${dataSetId}.emorecog\` CROSS JOIN UNNEST(trial_data) emore', 'Etm_emorecog_trial_data')"

echo "ds_linking - inserting EtM flanker metadata"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'SITTING_ID', 'flank.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'PERSON_ID', 'flank.person_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'SRC_ID', 'flank.src_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'RESPONSE_DEVICE', 'flank.metadata.response_device', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'SCREEN_HEIGHT', 'flank.metadata.screen_height', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'SCREEN_WIDTH', 'flank.metadata.screen_width', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TOUCH', 'flank.metadata.touch', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'OPERATING_SYSTEM', 'flank.metadata.operating_system', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_DURATION', 'flank.metadata.test_duration', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_RESTARTED', 'flank.metadata.test_restarted', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_VERSION', 'flank.metadata.test_version', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_NAME', 'flank.metadata.test_name', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_SHORT_NAME', 'flank.metadata.test_short_name', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_LANGUAGE', 'flank.metadata.test_language', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'AOU_VERSION', 'flank.metadata.aou_version', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_PARAMS', 'flank.metadata.test_params', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'USER_UTC_OFFSET', 'flank.metadata.user_utc_offset', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_START_DATE_TIME', 'flank.metadata.test_start_date_time', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'TEST_END_DATE_TIME', 'flank.metadata.test_end_date_time', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata'),
  ($((ID++)), 'USER_AGENT', 'flank.metadata.user_agent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_metadata')"
  
echo "ds_linking - inserting EtM flanker outcomes"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'SITTING_ID', 'flank.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'PERSON_ID', 'flank.person_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'SRC_ID', 'flank.src_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'SCORE', 'flank.outcomes.score', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'ACCURACY', 'flank.outcomes.accuracy', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'MEAN_RTC', 'flank.outcomes.mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'MEDIAN_RTC', 'flank.outcomes.median_rtc', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'SD_RTC', 'flank.outcomes.sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'MEDIAN_RT_CONGRUENT', 'flank.outcomes.median_rt_congruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'ACCURACY_CONGRUENT', 'flank.outcomes.accuracy_congruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'RCS_CONGRUENT', 'flank.outcomes.rcs_congruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'MEDIAN_RT_INCONGRUENT', 'flank.outcomes.median_rt_incongruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'ACCURACY_INCONGRUENT', 'flank.outcomes.accuracy_incongruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'RCS_INCONGRUENT', 'flank.outcomes.rcs_incongruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'MEDIAN_RT_INTERFERENCE', 'flank.outcomes.median_rt_interference', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'ACCURACY_INTERFERENCE', 'flank.outcomes.accuracy_interference', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'RCS_INTERFERENCE', 'flank.outcomes.rcs_interference', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'FLAG_MEDIAN_RTC', 'flank.outcomes.flag_median_rtc', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'FLAG_ACCURACY', 'flank.outcomes.flag_accuracy', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'FLAG_TRIAL_FLAGS', 'flank.outcomes.flag_trial_flags', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes'),
  ($((ID++)), 'ANY_TIMEOUTS', 'flank.outcomes.any_timeouts', 'FROM \`\${projectId}.\${dataSetId}.flanker\` flank', 'Etm_flanker_outcomes')"
  
echo "ds_linking - inserting EtM flanker trial data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'SITTING_ID', 'sitting_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'PERSON_ID', 'person_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'SRC_ID', 'src_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'LINK_ID', 'flank.link_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'TRIAL_ID', 'flank.trial_id', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'TRIAL_TYPE', 'flank.trial_type', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'TRIAL_BLOCK', 'flank.trial_block', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'TARGET', 'flank.target', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'FLANKERS', 'flank.flankers', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'CONGRUENT', 'flank.congruent', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'RESPONSE', 'flank.response', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'CORRECT', 'flank.correct', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'REACTION_TIME', 'flank.reaction_time', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'STATE', 'flank.state', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'REPEATED', 'flank.repeated', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'FLAGGED', 'flank.flagged', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data'),
  ($((ID++)), 'TRIAL_TIMESTAMP', 'flank.trial_timestamp', 'FROM \`\${projectId}.\${dataSetId}.flanker\` CROSS JOIN UNNEST(trial_data) flank', 'Etm_flanker_trial_data')"
  
echo "ds_linking - inserting EtM gradcpt metadata"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'SITTING_ID', 'grad.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'PERSON_ID', 'grad.person_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'SRC_ID', 'grad.src_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'RESPONSE_DEVICE', 'grad.metadata.response_device', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'SCREEN_HEIGHT', 'grad.metadata.screen_height', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'SCREEN_WIDTH', 'grad.metadata.screen_width', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TOUCH', 'grad.metadata.touch', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'OPERATING_SYSTEM', 'grad.metadata.operating_system', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_DURATION', 'grad.metadata.test_duration', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_RESTARTED', 'grad.metadata.test_restarted', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_VERSION', 'grad.metadata.test_version', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_NAME', 'grad.metadata.test_name', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_SHORT_NAME', 'grad.metadata.test_short_name', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_LANGUAGE', 'grad.metadata.test_language', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'AOU_VERSION', 'grad.metadata.aou_version', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_PARAMS', 'grad.metadata.test_params', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'USER_UTC_OFFSET', 'grad.metadata.user_utc_offset', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_START_DATE_TIME', 'grad.metadata.test_start_date_time', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'TEST_END_DATE_TIME', 'grad.metadata.test_end_date_time', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata'),
  ($((ID++)), 'USER_AGENT', 'grad.metadata.user_agent', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_metadata')"
  
echo "ds_linking - inserting EtM gradcpt outcomes"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'SITTING_ID', 'grad.sitting_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'PERSON_ID', 'grad.person_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'SRC_ID', 'grad.src_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'CV_RTC', 'grad.outcomes.cv_rtc', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'ACCURACY', 'grad.outcomes.accuracy', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'MEAN_RTC', 'grad.outcomes.mean_rtc', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'MEDIAN_RTC', 'grad.outcomes.median_rtc', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'SD_RTC', 'grad.outcomes.sd_rtc', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'GO_ACCURACY', 'grad.outcomes.go_accuracy', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'NOGO_ACCURACY', 'grad.outcomes.nogo_accuracy', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'SCORE', 'grad.outcomes.score', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'DPRIME', 'grad.outcomes.dprime', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'CRIT', 'grad.outcomes.crit', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'FLAG_TRIAL_FLAGS', 'grad.outcomes.flag_trial_flags', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'FLAG_NON_RESPONSE', 'grad.outcomes.flag_non_response', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes'),
  ($((ID++)), 'FLAG_OMISSION_ERROR_RATE', 'grad.outcomes.flag_omission_error_rate', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` grad', 'Etm_gradcpt_outcomes')"
  
echo "ds_linking - inserting EtM gradcpt trial data"
bq --quiet --project_id=$BQ_PROJECT query --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.ds_linking\` (ID, DENORMALIZED_NAME, OMOP_SQL, JOIN_VALUE, DOMAIN)
VALUES
  ($((ID++)), 'CORE_TABLE_FOR_DOMAIN', 'CORE_TABLE_FOR_DOMAIN', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'SITTING_ID', 'sitting_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'PERSON_ID', 'person_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'SRC_ID', 'src_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'LINK_ID', 'grad.link_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'TRIAL_ID', 'grad.trial_id', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'TRIAL_TYPE', 'grad.trial_type', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'GO', 'grad.go', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'TRIAL_LENGTH', 'grad.trial_length', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'CORRECT', 'grad.correct', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'REACTION_TIME', 'grad.reaction_time', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'IMAGE_OPACITY', 'grad.image_opacity', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'RESPONSE_TYPE', 'grad.response_type', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'RESPONSE_CODE', 'grad.response_code', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'STATE', 'grad.state', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'FLAGGED', 'grad.flagged', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'IMAGE', 'grad.image', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'TRIAL_TIMESTAMP', 'grad.trial_timestamp', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data'),
  ($((ID++)), 'RESPONSE_TIMESTAMP', 'grad.response_timestamp', 'FROM \`\${projectId}.\${dataSetId}.gradcpt\` CROSS JOIN UNNEST(trial_data) grad', 'Etm_gradcpt_trial_data')"