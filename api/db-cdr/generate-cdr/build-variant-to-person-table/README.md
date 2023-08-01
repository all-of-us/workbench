# Building the variant to person search table

This script should only be used with the POC being developed for the cohort builder genomic search. Moving forward we shouldn't have to do this since the Broad team will deliver a BigQuery table in the proper format.

## build-variant-to-person.sh

The script needs 3 args:

- Project name
- Dataset name
- GCS bucket location of tsv files - be sure to use (*) to load all files in directory- `example: rwb_export_*.tsv`

```
./build-variant-to-person.sh \
aou-res-curation-output-prod \
C2022Q4R9 \
"gs://prod-drc-broad/aou-wgs-delta/rwb-export/2023-05-12_01/chr20/raw_bigquery_shards/rwb_export_*.tsv"
```

## Script output

Should produce a table name prep_variant_to_person
