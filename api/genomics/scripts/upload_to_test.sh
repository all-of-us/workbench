### README
# This is not meant to be run as is. Treat it more as documentation of what I ran to get the data uploaded.

# Generate Random samples (Takes several hours and consumes 600-700 GB)
# Run from workbench/api
./project.rb randomize-vcf --vcf ~/broad/variantstore/NA12878_204126160130_R01C01.vcf --number-of-copies 500 --output-dir /mnt/genomics/

# Generate ingest files and upload to GCS (Takes several hours)
for i in {1..500}
do
  VCF=/mnt/genomics/NA12878_204126160130_R01C01.vcf.${i}.vcf
  echo Processing sample $i
  ~/broad/gatk/gatk CreateArrayIngestFiles --sample-id $i -V $VCF --probe-info-file probe_info.csv --ref-version 37

  ls *.tsv
  gsutil cp *.tsv gs://all-of-us-workbench-test-genomics/eric/import/3/ready/
  rm *.tsv
done

# Uploads all files in import/3 into arrays_003 (Fairly quick, < 1h)
cd ingest
./bq_ingest_arrays.sh all-of-us-workbench-test microarray_data gs://all-of-us-workbench-test-genomics/eric/import 3

# Move into VPC-SC (Fairly quick, < 1h)
# FIRST - Modify api/db-cdr/generate-cdr/libproject/devstart.rb so that the config all-of-us-workbench-test.source_cdr_project = all-of-us-workbench-test
./project.rb publish-cdr --bq-dataset microarray_data --project all-of-us-workbench-test

# Notebooks should now be able to query the dataset at `fc-aou-cdr-synth-test.microarray_data`
