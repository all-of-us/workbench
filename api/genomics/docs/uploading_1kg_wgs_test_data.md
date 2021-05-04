## Notes from uploading the 1000 genomes dataset to test

### Reblock
- Data sourced from: https://app.terra.bio/#workspaces/anvil-datastorage/1000G-high-coverage-2019/data
- GVCFs and indexes matching the following pattern, `gs://fc-56ac46ea-efc4-4683-b6d5-6d95bed41c5e/CCDG_14151/Project_CCDG_14151_B01_GRM_WGS.gVCF.2020-02-12/Project_CCDG_14151_B01_GRM_WGS.gVCF.2020-02-12/Sample_*/analysis/*.haplotypeCalls.er.raw.vcf.gz*`, 
were copied into `gs://all-of-us-workbench-test-genomics/1kg_gvcfs/`
  - All files were renamed to add `.g.` in its file extension. Ex. `sample.haplotypeCalls.er.raw.vcf.gz` -> `sample.haplotypeCalls.er.raw.g.vcf.gz`. 
    Reblock needs this file pattern to name its output files correctly.
  - Data size - 698 samples, 3.67 TiB

- `gsutil ls gs://all-of-us-workbench-test-genomics/1kg_gvcfs/*.gz > 1kg_gvcfs.txt` to create gvcf list for Reblock input
- reblocked data size is 1.1 TiB

### Import Genomes
- Create empty dataset to import data into
  - `bq mk all-of-us-workbench-test:1kg_wgs`
- Download input_vcfs
  - `gsutil ls gs://all-of-us-workbench-test-genomics/1kg_wgs/reblocked_gvcf/*.gz > 1kg_reblocked_gvcfs_list.txt`
- Open a Terra Dev workspace
- Create a Terra Data Table with the following columns [1kg_sample_id, gvcf, gvcf_index]
  - `echo -e "entity:1kg_sample_id\tgvcf\tgvcf_index" > data_table.tsv`
  - `cat 1kg_reblocked_gvcfs_list.txt |  awk '{print NR"\t"$0"\t"$0".tbi"}' >> data_table.tsv`
  - Go to Terra Workspace -> Data -> Click Tables (+) -> Upload data_table.tsv
- Create sample_map
  - The first column represents sample_ids and will be incrementing by 100 in order to hit more partitions as part of this test
  - The second column represents sample_names and it will match the names in the reblocked vcf files
  - `cat 1kg_reblocked_gvcfs_list.txt | pcregrep -o1 'gvcf/(.+).haplo' | awk '{print NR*100","$0}' > 1kg_sample_map.txt`
  - This will be one of the workflow inputs
- Create a GATK jar
  - https://github.com/broadinstitute/gatk#building-gatk4 
  - I used https://github.com/broadinstitute/gatk/commit/66ae4b451fd327061ca90e59e759b94fd65f2650 but checkout a more recent commit if newer changes need to be pulled in. 
  - This will be one of the workflow inputs
- Import https://github.com/broadinstitute/gatk/blob/ac1a9b65f81cb24a7349ba6226b3f0f1d91d1c11/scripts/variantstore/wdl/GvsImportGenomes.wdl as a Terra Method
- Create Workflow with imported method
  - Run workflow(s) with inputs defined by data table
  - Root entity type: "1kg_sample_set"
  - Select Data -> Select all samples
  - Inputs (This is what I used but change what makes sense for you.)
    - Note: The output_directory can be any empty folder but it must change from run to run because of leftover files from preexisting runs.  
    - ```
      {
      "GvsImportGenomes.dataset_name": "1kg_wgs",
      "GvsImportGenomes.gatk_override": "upload and copy from 'Create a GATK jar' step",
      "GvsImportGenomes.input_vcf_indexes": "${this.1kg_samples.gvcf_index}",
      "GvsImportGenomes.input_vcfs": "${this.1kg_samples.gvcf}",
      "GvsImportGenomes.interval_list": "gs://gcp-public-data--broad-references/hg38/v0/wgs_calling_regions.hg38.noCentromeres.noTelomeres.interval_list",
      "GvsImportGenomes.output_directory": "gs://fc-56d2f6f5-3efa-46f7-8c01-0911fd77f888/import_genomes/9",
      "GvsImportGenomes.project_id": "all-of-us-workbench-test",
      "GvsImportGenomes.sample_map": "upload and copy from 'Create sample_map' step",
      }
      ```
- Some gotchas if it fails
  - Your Terra pet service account needs access to the input vcfs, you can grant this through Google Cloud Console
  - If the workflow fails towards the end and just a few shards fail, it might be a flaky error. Just try again. 

### Fixing sample_names
- Right now, the sample_names in the `sample_info` table match the sample_names in the VCFs.
- However, we need the sample_names to match participant_ids in AoU.
- Additionally, subsequent runs of updating the 1kg dataset should use the same sample_names from the first run so it's consistent with which participants the test CDR believes has WGS data.

- Grab sample_names (person_ids)
  - If running for the first time 
    - ```bq query --max_rows=1000000 --format=csv --nouse_legacy_sql 'SELECT person_id FROM `all-of-us-workbench-test.synth_r_2019q4_9.cb_person`;' | sed -e 1d > ehr_sample_names.txt```
  - If not, grab the list from the published CDR or the test dataset that was used to publish into the CDR.
- ```bq query --max_rows=1000000 --format=csv --nouse_legacy_sql 'SELECT sample_name FROM `all-of-us-workbench-test.1kg_wgs.sample_info`' | sed -e 1d > vcf_sample_names.txt```
- ```paste -d "," <(shuf -n $(cat vcf_sample_names.txt | wc -l) ehr_sample_names.txt) vcf_sample_names.txt > new_old_sample_names_map.csv```
- ```cat new_old_sample_names_map.csv | awk -F',' '{print "UPDATE `all-of-us-workbench-test.1kg_wgs.sample_info` set sample_name=\""$1"\" where sample_name=\""$2"\";"}'```
- Run update queries in BQ console or bq CLI

### Extracting cohort
- The first few runs of the extract failed until I tweaked some parameters
- The scatter count was increased to 1000 to let the extraction finish in a reasonable time frame (~3 hours). 
- The extraction time per shard varies a lot right now. Probably due to some more complex genomic intervals containing many more variants than less complex regions.
- Some of the shards were also failing until I turned down the parameter for max local records in memory from 10 million to 5 million.
- Moving extract shards into GCS bucket
  - `gsutil -m ls gs://fc-66b79f35-e7e8-4b0f-b8e8-ab57c9ce98ff/9b6bceb0-096e-400b-9b3e-825a36e74334/NgsCohortExtract/8f81c231-918b-43e0-b833-563bdeb833a0/call-ExtractTask/shard-*/1kg_wgs_*.vcf.gz > 1kg_extract_shards.txt`
  - `cat 1kg_extract_shards.txt | awk '{print "gsutil mv "$0" gs://all-of-us-workbench-test-genomics/1kg_wgs/extract/"}' | parallel --jobs 8`
