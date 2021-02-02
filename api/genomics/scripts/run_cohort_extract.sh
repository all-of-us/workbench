PROJECT="all-of-us-workbench-test"
PETVET_DATASET="1kg_wgs"
DEST_TABLE="cohort_extract"
TEMP_DATASET="1kg_wgs_temp_tables"
EXTRACT_DATASET="1kg_wgs_extract"
COHORT_LIST_TABLE="cohort"

python ngs_cohort_extract.py \
  --fq_petvet_dataset ${PROJECT}.${PETVET_DATASET} \
  --fq_temp_table_dataset ${PROJECT}.${TEMP_DATASET} \
  --fq_destination_dataset ${PROJECT}.${EXTRACT_DATASET} \
  --destination_table ${DEST_TABLE} \
  --fq_cohort_sample_names ${PROJECT}.${EXTRACT_DATASET}.${COHORT_LIST_TABLE} \
  --min_variant_samples 0 \
  --query_project ${PROJECT} \
  --fq_sample_mapping_table ${PROJECT}.${PETVET_DATASET}.metadata
