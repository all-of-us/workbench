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

### Fixing sample_names
- Right now, the sample_names in the `metadata` table match the sample_names in the VCFs
- However, we need the sample_names to match participant_ids in AoU


- Create (new_sample_name, old_sample_name) list file
  - `echo "1184442,HG00420" >> old_new_sample_names_map.txt` 
  - `echo "2473159,HG00423" >> old_new_sample_names_map.txt` 
  - `echo "3436708,HG00418" >> old_new_sample_names_map.txt` 
- Create update queries
  - `cat sample_map_6.txt | awk -F',' '{print "UPDATE `all-of-us-workbench-test.wgs_demo_7.metadata` set sample_name=\""$1"\" where sample_name=\""$2"\";"}'`
- Run update queries in BQ console or wherever


Extract should now work as is if you follow the demo but running this command to generate the new sample_map will be helpful.

`bq query --format=csv --nouse_legacy_sql 'SELECT sample_id,sample_name FROM `all-of-us-workbench-test.wgs_demo_7.metadata`' | sed -e 1d > sample_map_7.txt`

this sample_map file can feed into the `bq load` command in the demo doc.
