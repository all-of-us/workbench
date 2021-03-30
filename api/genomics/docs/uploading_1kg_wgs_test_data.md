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
- new dataset name: `bq mk all-of-us-workbench-test:1kg_wgs`
- input_vcfs_list: `1kg_reblocked_gvcfs_list.txt`
  - `gsutil ls gs://all-of-us-workbench-test-genomics/1kg_wgs/reblocked_gvcf/*.gz > 1kg_reblocked_gvcfs_list.txt`
- sample_map: `1kg_sample_map.txt`
  - The first column represents sample_ids and will be incrementing by 100 in order to hit more partitions as part of this test
  - The second column represents sample_names and it will match the names in the reblocked vcf files
  - `cat 1kg_reblocked_gvcfs_list.txt | pcregrep -o1 'gvcf/(.+).haplo' | awk '{print NR*100","$0}' > 1kg_sample_map.txt`
  - ImportGenomes wdl - https://github.com/broadinstitute/gatk/blob/75f0bd80a45ed46e171ba2125fcb6a6b825c1b74/scripts/variantstore/wdl/ImportGenomes.wdl

### Fixing sample_names
- Right now, the sample_names in the `metadata` table match the sample_names in the VCFs
- However, we need the sample_names to match participant_ids in AoU

- ```bq query --max_rows=1000000 --format=csv --nouse_legacy_sql 'SELECT person_id FROM `all-of-us-workbench-test.synth_r_2019q4_9.cb_person`;' | sed -e 1d > ehr_sample_names.txt```
- ```bq query --max_rows=1000000 --format=csv --nouse_legacy_sql 'SELECT sample_name FROM `all-of-us-workbench-test.wgs_demo.metadata`' | sed -e 1d > vcf_sample_names.txt```
- ```paste -d "," <(shuf -n $(cat vcf_sample_names.txt | wc -l) ehr_sample_names.txt) vcf_sample_names.txt > new_old_sample_names_map.csv```
- ```cat new_old_sample_names_map.csv | awk -F',' '{print "UPDATE `all-of-us-workbench-test.wgs_demo_7.metadata` set sample_name=\""$1"\" where sample_name=\""$2"\";"}'```
- Run update queries in BQ console or bq CLI

### Extracting cohort
- The first few runs of the extract failed until I tweaked some parameters
- The scatter count was increased to 1000 to let the extraction finish in a reasonable time frame (~3 hours). 
- The extraction time per shard varies a lot right now. Probably due to some more complex genomic intervals containing many more variants than less complex regions.
- Some of the shards were also failing until I turned down the parameter for max local records in memory from 10 million to 5 million.
- Moving extract shards into GCS bucket
  - `gsutil -m ls gs://fc-66b79f35-e7e8-4b0f-b8e8-ab57c9ce98ff/9b6bceb0-096e-400b-9b3e-825a36e74334/NgsCohortExtract/8f81c231-918b-43e0-b833-563bdeb833a0/call-ExtractTask/shard-*/1kg_wgs_*.vcf.gz > 1kg_extract_shards.txt`
  - `cat 1kg_extract_shards.txt | awk '{print "gsutil mv "$0" gs://all-of-us-workbench-test-genomics/1kg_wgs/extract/"}' | parallel --jobs 8`
