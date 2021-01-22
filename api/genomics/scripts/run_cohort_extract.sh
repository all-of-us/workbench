PROJECT="all-of-us-workbench-test"
DATASET="wgs_demo_5"
DEST_TABLE="cohort_extract"
TEMP_DATASET="wgs_demo_5_temp_tables"
COHORT_LIST_TABLE="cohort"

python ngs_cohort_extract.py \
  --fq_petvet_dataset ${PROJECT}.${DATASET} \
  --fq_temp_table_dataset ${PROJECT}.${TEMP_DATASET} \
  --fq_destination_dataset ${PROJECT}.${DATASET} \
  --destination_table ${DEST_TABLE} \
  --fq_cohort_sample_names ${PROJECT}.${DATASET}.${COHORT_LIST_TABLE} \
  --min_variant_samples 0 \
  --query_project ${PROJECT} \
  --fq_sample_mapping_table ${PROJECT}.${DATASET}.metadata
