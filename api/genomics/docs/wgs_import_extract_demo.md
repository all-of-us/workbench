## Whole genome workflows demo

Notes:
  - This demo is based off the quickstart written in this pull request, https://github.com/broadinstitute/gatk/pull/6993 
  - *.inputs.json files contain additional comments for documentation but they will need to be parsed out in order 
to create a valid JSON file. Run `cat [filename].inputs.json | grep -v '^[[:blank:]]\+//'` to filter out comments.
  - I've copied over most of the files I used by either committing them or uploading them to the `all-of-us-workbench-test-genomics` bucket for 
reproducability but note that the source for these files will probably change over time. 

### Importing Genomes into BQ
1. Run ReblockGVCF workflow on inputs
	- WDL - api/genomics/wdl/ReblockGVCF.wdl
	- Inputs - api/genomics/wdl/ReblockGVCF.poc.inputs.json 

2. Create an empty dataset to contain the genome data. I used `wgs_demo_1`.
   
3. Run ImportGenomes
	- WDL - https://github.com/broadinstitute/gatk/blob/75f0bd80a45ed46e171ba2125fcb6a6b825c1b74/scripts/variantstore/wdl/ImportGenomes.wdl
	- Inputs - api/genomics/wdl/ImportGenomes.poc.inputs.json
	- This will create three tables: metadata, pet_?, and vet_? within the dataset
4. Publish dataset into VPC-SC
	- ```./project.rb publish-cdr --project all-of-us-workbench-test --bq-dataset wgs_demo_5 --exclude-sa-acl --exclude-auth-domain-acl --additional-reader-group PROXY_118217329794842274136@dev.test.firecloud.org --source-cdr-project-override all-of-us-workbench-test```
	- Change the argument for `--additional-reader-group` to match the Proxy group for the extraction SA in the current environment

### Creating Filter
1. Run NgsFilterExtract
	- WDL - api/genomics/wdl/NgsFilterExtract.wdl
	- Inputs - api/genomics/wdl/NgsFilterExtract.poc.inputs.json

### Extracting cohort from BQ

1. Create cohort table of sample to extract (argument for --fq_cohort_sample_names in next step)
    - The following commands will use the entire metadata table as the cohort but you can create a subset by removing entries from sample_map.txt
    - ```bq query --max_rows=1000000 --format=csv --nouse_legacy_sql 'SELECT sample_id,sample_name FROM `all-of-us-workbench-test.wgs_demo.metadata`' | sed -e 1d > sample_map.txt```
    - `bq load --source_format=CSV all-of-us-workbench-test:wgs_demo.cohort sample_map.txt sample_id:INTEGER,sample_name:STRING`
	- I used `cohort` as my table name but it can be anything as long as you adjust it in the following steps

2. Create an empty dataset for the next step. I used `wgs_demo_1_temp_tables`

3. Python script to create extract table
	- Install google-cloud-bigquery python library
    - Script located at api/genomics/scripts/ngs_cohort_extract.py
	- Running the script will create a table named ${DEST_TABLE} in the dataset
	- I added a small bash script at api/genomics/scripts/run_cohort_extract.sh which
	makes it easy edit the many arguments that are passed in.

4. Extract WDL 
	- WDL - api/genomics/wdl/WgsCohortExtract.wdl
	- Inputs - api/genomics/wdl/WgsCohortExtract.poc.inputs.json
