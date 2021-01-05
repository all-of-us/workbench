### Whole genome import and extraction demo

Note:  *.inputs.json files contain additional comments for documentation but they will need to be parsed out in order 
to create a valid JSON file. Run `cat [filename].inputs.json | grep -v '^[[:blank:]]\+//'` to filter out comments.

1. Run ReblockGVCF workflow on inputs
	- WDL - api/genomics/wdl/ReblockGVCF.wdl
	- Inputs - api/genomics/wdl/ReblockGVCF.poc.inputs.json 

2. Create an empty dataset to contain the genome data. I used `wgs_demo_1`.
   
3. Run ImportGenomes
	- WDL - api/genomics/wdl/ImportGenomes.wdl
	- Inputs - api/genomics/wdl/ImportGenomes.poc.inputs.json
	- This will create three tables: metadata, pet_?, and vet_? within the dataset

4. Create cohort table of sample to extract (argument for --fq_cohort_sample_names in next step)
	- `bq mk --table wgs_demo_1.cohort sample_id:INTEGER,sample_name:STRING`
	- `echo '{"sample_id":97461, "sample_name":"NA12878"}' | bq insert wgs_demo_1.cohort`
	- I used `cohort` as my table name but it can be anything as long as you adjust it in the following steps

5. Create an empty dataset for the next step. I used `wgs_demo_1_temp_tables`

6. Python script to create extract table
	- Install google-cloud-bigquery python library
    - Script located at api/genomics/ngs_cohort_extract.py
	- Running the script will create a table named ${DEST_TABLE} in the dataset
	
	```
	PROJECT="all-of-us-workbench-test"
	DATASET="wgs_demo_1"
	DEST_TABLE="cohort_extract"
	TEMP_DATASET="wgs_demo_1_temp_tables"
	COHORT_LIST_TABLE="cohort"
	
	python ngs_cohort_extract.py \
	  --fq_petvet_dataset ${PROJECT}.${DATASET} \
	  --fq_temp_table_dataset ${PROJECT}.${TEMP_DATASET} \
	  --fq_destination_dataset ${PROJECT}.${DATASET} \
	  --destination_table ${DEST_TABLE} \
	  --fq_cohort_sample_names ${PROJECT}.${DATASET}.{COHORT_LIST_TABLE} \
	  --min_variant_samples 0 \
	  --query_project ${PROJECT} \
	  --fq_sample_mapping_table ${PROJECT}.${DATASET}.metadata
	```

7. Extract WDL 
	- WDL - api/genomics/wdl/WgsCohortExtract.wdl
	- Inputs - api/genomics/wdl/WgsCohortExtract.poc.inputs.json
