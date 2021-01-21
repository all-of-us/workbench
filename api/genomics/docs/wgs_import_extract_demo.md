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
	- WDL - api/genomics/wdl/ImportGenomes.wdl
	- Inputs - api/genomics/wdl/ImportGenomes.poc.inputs.json
	- This will create three tables: metadata, pet_?, and vet_? within the dataset

### Creating Filter
1. Run NgsFilterExtract
	- WDL - api/genomics/wdl/NgsFilterExtract.wdl
	- Inputs - api/genomics/wdl/NgsFilterExtract.poc.inputs.json

### Extracting cohort from BQ

1. Create cohort table of sample to extract (argument for --fq_cohort_sample_names in next step)
	- `bq mk --table wgs_demo_1.cohort sample_id:INTEGER,sample_name:STRING`
	- `echo '{"sample_id":97461, "sample_name":"NA12878"}' | bq insert wgs_demo_1.cohort`
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
