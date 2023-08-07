#!/bin/bash

# This creates a variant table from tsv files located in  gs://prod-drc-broad/

set -e

BQ_PROJECT=$1        # project
BQ_DATASET=$2        # dataset
TSV_FILE_BUCKET=$3   # gcs bucket containing tsv files
TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT":"$BQ_DATASET")

if [[ "$TABLE_LIST" == *"prep_variant_sample"* ]]
then
  echo "Removing the prep_variant_sample table"
  bq rm -f -t --project_id="$BQ_PROJECT" "$BQ_DATASET".prep_variant_sample
fi

if [[ "$TABLE_LIST" == *"prep_variant_to_person"* ]]
then
  echo "Removing the prep_variant_to_person table"
  bq rm -f -t --project_id="$BQ_PROJECT" "$BQ_DATASET".prep_variant_to_person
fi

echo "Loading data into the prep_variant_sample table"
bq load --source_format=CSV --field_delimiter=tab --skip_leading_rows 1 \
--project_id="$BQ_PROJECT" "$BQ_DATASET".prep_variant_sample \
"$TSV_FILE_BUCKET" \
prep_variant_sample.json

echo "Creating prep_variant_to_person table"
bq --quiet --project_id="$BQ_PROJECT" mk --schema=prep_variant_to_person.json --clustering_fields vid "$BQ_DATASET".prep_variant_to_person

echo "Transforming prep_variant_sample to prep_variant_to_person"
bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
"INSERT INTO \`$BQ_PROJECT.$BQ_DATASET.prep_variant_to_person\`
 WITH converted_person_ids AS (
 SELECT vid, SAFE_CAST(person_id AS INT64) AS person_id
 FROM \`$BQ_PROJECT.$BQ_DATASET.prep_variant_sample\`
 )
 SELECT vid,
 ARRAY_AGG(DISTINCT person_id IGNORE NULLS) person_ids
 FROM converted_person_ids
 GROUP BY vid"

echo "Removing the prep_variant_sample table"
bq rm -f -t --project_id="$BQ_PROJECT" "$BQ_DATASET".prep_variant_sample