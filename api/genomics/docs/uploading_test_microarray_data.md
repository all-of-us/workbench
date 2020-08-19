## Creating a randomized microarray dataset and uploading to BigQuery in test environment
- The following commands are not meant to be run as is. Treat it more as documentation of what I ran to get the data uploaded.
- All profiling was done on a laptop with an Intel Core i7-9850H CPU @ 2.60GHz, 32GB RAM, and an SSD
### Generate Randomized VCF samples
- Run from workbench/api  
`./project.rb randomize-vcf --vcf ~/broad/variantstore/NA12878_204126160130_R01C01.vcf --number-of-copies 1000 --output-path /mnt/genomics/randomized1000.vcf`

| Number of Samples | Clock time | Output file size |
| ----------------  | ---------- | ---------------- |
|1 | 53s | 1.4 GB |
|10|1m 26s|2.3 GB|
|100|6m 38s| 11 GB|
|500|27m|49 GB|
|1,000|59m|95 GB|
|10,000**|~ 10h|~ 1 TB|
|100,000**|~ 100h|~ 10 TB|
** Estimates assuming linear growth

### Compress VCF file 
- `bgzip -c randomized1000.vcf > randomized1000.vcf.gz`

| Number of Samples | Clock time | Compression |
| ----------------- | ---------- | ----------- |
|100|52s|11 G -> 544 M|
|500|4m 18s|49 G -> 1.3 G|
|1000|5m 44s|95G -> 2.1 G|

### Index VCF file
- `bcftools index randomized1000.vcf.gz`

| Number of Samples | Clock time |
| ----------------- | ---------- |
|100|20s|
|500|1m 20s|
|1000|2m 40s|

### Extracting a single sample VCF from multi-sample VCF
- `bcftools view -s 204126160130_R01C01.908 randomized1000.vcf.gz > 1000_sample.vcf`

| Number of Sample | Clock time |
| ---------------  | ---------- |
|100|3m 16s|
|500|14m|
|1,000|28m|
|10,000**|~ 4h 40m|
|100,000**|~ 46h 40m|
** Estimates assuming linear growth

### Generate ingest files and upload to GCS
```
  # Run from variantstore repo, https://github.com/broadinstitute/variantstore
  # rm/cp *.tsv is a hack I used to clean up already uploaded files while running the code in a loop
  rm *.tsv
  ~/broad/gatk/gatk CreateArrayIngestFiles --sample-id 908 -V 1000_sample.vcf --probe-info-file probe_info.csv --ref-version 37
  gsutil cp *.tsv gs://all-of-us-workbench-test-genomics/eric/import/3/ready/
```
- Every import/# folder can only contain up to 4k samples
- CreateArrayIngestFiles takes ~1m 20s and operates 1 sample at a time
- gsutil cp depends on upload speed


### GCS -> BigQuery 
```
  # Run from variantstore repo
  cd ingest
  ./bq_ingest_arrays.sh all-of-us-workbench-test microarray_data gs://all-of-us-workbench-test-genomics/eric/import 3
```
- This will have to be changed for >4k samples since a BigQuery table can only have 4k partitions

### Move into VPC-SC (Fairly quick, < 1h)
- FIRST - Modify api/db-cdr/generate-cdr/libproject/devstart.rb so that the config all-of-us-workbench-test.source_cdr_project = all-of-us-workbench-test
- `./project.rb publish-cdr --bq-dataset microarray_data --project all-of-us-workbench-test`
- Notebooks should now be able to query the dataset at `fc-aou-cdr-synth-test.microarray_data`
