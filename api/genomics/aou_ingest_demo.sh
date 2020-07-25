###
### This is not meant to be run as is.
### It's more of a scratchpad and history of what I ran to get the ingest working on my machine.
### All the commands should be valid but many of the arguments will have to be substituted.
###
### The "script" assumes you are in the root level directory of the broadinstitute/variantstore repository 
###
### The tools I ran should be sufficient to unblock the next step of genomics work
### which is uploading 100~1000 synthetic samples into the test VPC-SC project.
### There shouldn't be much to do if we are OK with a manual process but converting
### this script into something that can run inside project.rb may be some work.
### Notably, we would have to figure out how to build or run a non-master version of GATK.
### One possible solution is to create a docker image that contains all the necessary tools
### like the branched GATK, bcftools, etc. 
###
###

PROJECT_ID=all-of-us-workbench-test
DATASET_NAME=eric_aou_test
TABLE_NAME=probe_info

## cleanup
#gsutil rm gs://all-of-us-workbench-test-genomics/eric/import/2/done/*
#bq rm $PROJECT_ID:$DATASET_NAME.probe_info
#bq rm $PROJECT_ID:$DATASET_NAME.sample_list
#bq rm $PROJECT_ID:$DATASET_NAME.arrays_001

# This is from a Terra workspace - https://app.terra.bio/#workspaces/broad-genomics-data/All_of_Us_GDA_Array_change_control_data/
# I believe this is what contains the probe data, the ingest command should produce a probe_info table which we can import
gsutil cp gs://fc-b79a5ca7-28d7-48f4-8f92-d69e0c48a3ed/PDO-21032_85_ColorPGx_eMERGE_samples_03202020/GDA-8v1-0_A1.1.5.extended.csv .
./ingest/bq_ingest_arrays_manifest.sh $PROJECT_ID $DATASET_NAME $TABLE_NAME GDA-8v1-0_A1.1.5.extended.csv ingest/manifest_schema.json

# Stage probe_info from bucket
# probe_info table will be created by the bg_ingest_arrays_manifest command. Go to Google Cloud Console and export the probe_info table to a GCS bucket to download it
# gsutil cp gs://???/probe_info.csv .

# the argument name may be different depending on the version of the gatk branch.
INGEST_PROBE_CLAUSE="--probe-info-file probe_info.csv"

# Build the ah_var_store branch of GATK and use the binary that is produced in the build folder for the following commands

### Extracting a single sample and uploading

# Sample 0
# Extract single samples from multi sample vcf - Sample_name=204126160130_R01C01.0 sample_id=0
bcftools view -Ov -s 204126160130_R01C01.0 -o 204126160130_R01C01.0.subset.vcf NA12878_204126160130_R01C01.randomized.subset.combined.vcf
# This will create raw_000_204126160130_R01C01.0.tsv and metadata_001_204126160130_R01C01.tsv
./gatk CreateArrayIngestFiles --sample-id 0 -V 204126160130_R01C01.0.subset.vcf $INGEST_PROBE_CLAUSE --ref-version 37

gsutil cp metadata_000_204126160130_R01C01.0.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/
gsutil cp raw_000_204126160130_R01C01.0.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/

## Sample 1
bcftools view -Ov -s 204126160130_R01C01.1 -o 204126160130_R01C01.1.subset.vcf NA12878_204126160130_R01C01.randomized.subset.combined.vcf
./gatk CreateArrayIngestFiles --sample-id 1 -V 204126160130_R01C01.1.subset.vcf $INGEST_PROBE_CLAUSE --ref-version 37

gsutil cp metadata_000_204126160130_R01C01.1.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/
gsutil cp raw_000_204126160130_R01C01.1.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/

## Sample 2
bcftools view -Ov -s 204126160130_R01C01.2 -o 204126160130_R01C01.2.subset.vcf NA12878_204126160130_R01C01.randomized.subset.combined.vcf
./gatk CreateArrayIngestFiles --sample-id 2 -V 204126160130_R01C01.2.subset.vcf $INGEST_PROBE_CLAUSE --ref-version 37

gsutil cp metadata_000_204126160130_R01C01.2.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/
gsutil cp raw_000_204126160130_R01C01.2.tsv gs://all-of-us-workbench-test-genomics/eric/import/2/ready/

# Uploads all files in import/2 into arrays_002
cd ingest
./bq_ingest_arrays.sh $PROJECT_ID $DATASET_NAME gs://all-of-us-workbench-test-genomics/eric/import 2 
