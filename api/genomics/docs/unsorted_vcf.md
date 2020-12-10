## Answering some questions on how our tools interact with unsorted VCF files

### Does Plink require a VCF file to be sorted?
No, but another flag `--make-bed` needs to be added to the binary file creation step.
`plink --vcf-half-call m --const-fid 0 --vcf unsorted.vcf --out cohort --make-bed` ->  
`plink --vcf-half-call m --const-fid 0 --vcf unsorted.vcf --out cohort --make-bed`

### Does Hail require a VCF file to be sorted?
No, but it will automatically add a sorting step if it detects unsorted input. Example output
```
2020-12-10 02:36:41 Hail: INFO: Ordering unsorted dataset with network shuffle
2020-12-10 02:37:13 Hail: INFO: wrote matrix table with 1900033 rows and 10 columns in 4 partitions to gs://fc-secure-39dd112d-0de0-46fc-bdf4-77b3e5f8a41f/unsorted_cohort.mt
2020-12-10 02:37:13 Hail: INFO: Reading table to impute column types
2020-12-10 02:37:13 Hail: INFO: Finished type imputation
  Loading field 'sample_name' as type str (user-supplied type)
  Loading field 'phenotype1' as type int32 (imputed)
  Loading field 'phenotype2' as type int32 (imputed)
2020-12-10 02:37:17 Hail: INFO: linear_regression_rows: running on 10 samples for 1 response variable y,
    with input variable x, and 2 additional covariates...
```

### How do you sort a VCF file?
`bcftools sort unsorted.vcf > resorted.vcf`  
For testing, I unsorted the VCF file like so  
```
head -n 99 sorted.vcf > header.vcf
tail -n +100 sorted.vcf > body.vcf
shuf body.vcf > unsorted_body.vcf
cat header.vcf >> unsorted.vcf
cat unsorted_body.vcf >> unsorted.vcf
```
