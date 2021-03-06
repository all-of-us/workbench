version 1.0

workflow WgsCohortExtract {

  input {
    File participant_ids
    String query_project
    String wgs_dataset
    String wgs_extraction_cohorts_dataset
    String wgs_extraction_destination_dataset
    String wgs_extraction_temp_tables_dataset
    String extraction_uuid
  }

  call CreateCohortTable {
    input:
      participant_ids = participant_ids,
      wgs_dataset = wgs_dataset,
      wgs_extraction_cohorts_dataset = wgs_extraction_cohorts_dataset,
      query_project = query_project,
      table_name = extraction_uuid
  }

  call CreateCohortExtractTable {
    input:
      cohort_uuid = extraction_uuid,
      query_project = query_project,
      wgs_dataset = wgs_dataset,
      wgs_extraction_cohorts_dataset = wgs_extraction_cohorts_dataset,
      wgs_extraction_destination_dataset = wgs_extraction_destination_dataset,
      wgs_extraction_temp_tables_dataset = wgs_extraction_temp_tables_dataset
  }

  output {
    String cohort_extract_table = CreateCohortExtractTable.cohort_extract_table
  }

}

task CreateCohortTable {

  input {
    File participant_ids
    String wgs_dataset
    String wgs_extraction_cohorts_dataset
    String query_project
    String table_name
  }

  command <<<
    echo "SELECT
      sample_id,
      sample_name
    FROM
      \`~{wgs_dataset}.metadata\`
    WHERE
      sample_name IN " > create_cohort.sql

    PARTICIPANT_IDS=$(cat ~{participant_ids} | awk '{print "\""$0"\""}' | paste -sd ",")
    echo "($PARTICIPANT_IDS)" >> create_cohort.sql

    DESTINATION_TABLE=$(echo ~{wgs_extraction_cohorts_dataset} | tr '.' ':')

    bq query \
      --project_id ~{query_project} \
      --destination_table ${DESTINATION_TABLE}.~{table_name} \
      --use_legacy_sql=false \
      --max_rows=10000000 \
      --allow_large_results < create_cohort.sql
  >>>

  output { }

  runtime {
    memory: "3.75 GB"
    bootDiskSizeGb: "15"
    disks: "local-disk 10 HDD"
    preemptible: 3
    docker: "us.gcr.io/broad-gatk/gatk:4.1.8.0"
  }
}


task CreateCohortExtractTable {

  input {
    String cohort_uuid
    String wgs_dataset
    String wgs_extraction_cohorts_dataset
    String wgs_extraction_destination_dataset
    String wgs_extraction_temp_tables_dataset
    String query_project
  }

  command <<<
    set -e

    echo "Exporting to ~{wgs_extraction_destination_dataset}.~{cohort_uuid}"

    python /app/ngs_cohort_extract.py \
      --fq_petvet_dataset ~{wgs_dataset} \
      --fq_temp_table_dataset ~{wgs_extraction_temp_tables_dataset} \
      --fq_destination_dataset ~{wgs_extraction_destination_dataset} \
      --destination_table ~{cohort_uuid} \
      --fq_cohort_sample_names ~{wgs_extraction_cohorts_dataset}.~{cohort_uuid} \
      --min_variant_samples 0 \
      --query_project ~{query_project} \
      --fq_sample_mapping_table ~{wgs_dataset}.metadata

    echo ~{wgs_extraction_destination_dataset}.~{cohort_uuid} > cohort_extract_table.txt
  >>>

  output {
    String cohort_extract_table = read_string("cohort_extract_table.txt")
  }

  runtime {
    memory: "3.75 GB"
    bootDiskSizeGb: "15"
    disks: "local-disk 10 HDD"
    preemptible: 3
    # Temporary GATK that I'm using to get unblocked. We will realign with the mainstream GATK ah_var_store branch
    # once they incorporate my changes.
    docker: "gcr.io/all-of-us-workbench-test/variantstore-extract-prep:2"
  }
}

