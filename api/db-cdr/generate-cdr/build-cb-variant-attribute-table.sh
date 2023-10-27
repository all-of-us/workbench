#!/bin/bash

# This generates the cb menu for cohort builder.

set -e

export BQ_PROJECT=$1   # project
export BQ_DATASET=$2   # dataset

TABLE_LIST=$(bq ls -n 1000 "$BQ_PROJECT:$BQ_DATASET")

if [[ "$TABLE_LIST" == *"prep_vat"* ]]
then

  echo "Creating cb_variant_attribute table."
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute\`
       CLUSTER BY vid
   AS
       WITH sorted_transcripts AS (
           SELECT vid,
                  consequence,
                  aa_change AS protein_change,
                  contig,
                  position,
                  ref_allele,
                  alt_allele,
                  transcript,
                  ARRAY_TO_STRING(consequence, ', ') AS cons_str,
                  dna_change_in_transcript,
                  clinvar_classification AS clinical_significance,
                  ARRAY_TO_STRING(clinvar_classification, ', ') AS clinical_significance_string,
                  gvs_all_ac,
                  gvs_all_an,
                  gvs_all_af,
                  ROW_NUMBER() OVER(PARTITION BY vid ORDER BY CASE ARRAY_TO_STRING(consequence, ', ')
                   WHEN 'upstream_gene_variant'
                       THEN 4
                   WHEN 'downstream_gene_variant'
                       THEN 5
                   ELSE 1
                   END
                   ASC
                  )  AS row_number
       FROM \`$BQ_PROJECT.$BQ_DATASET.prep_vat\`
       WHERE is_canonical_transcript OR transcript IS NULL
       ORDER BY vid, row_number),
       genes AS (
         SELECT vid, ARRAY_TO_STRING(ARRAY_AGG(DISTINCT gene_symbol IGNORE NULLS ORDER BY gene_symbol), ', ') AS genes
         FROM \`$BQ_PROJECT.$BQ_DATASET.prep_vat\`
         GROUP BY vid
       )
   SELECT
       sorted_transcripts.vid,
       sorted_transcripts.protein_change,
       sorted_transcripts.dna_change_in_transcript as dna_change,
       sorted_transcripts.gvs_all_ac as allele_count,
       sorted_transcripts.gvs_all_an as allele_number,
       sorted_transcripts.gvs_all_af as allele_frequency,
       sorted_transcripts.clinical_significance,
       sorted_transcripts.clinical_significance_string,
       sorted_transcripts.transcript as transcript,
       sorted_transcripts.contig,
       sorted_transcripts.position,
       sorted_transcripts.ref_allele,
       sorted_transcripts.alt_allele,
       sorted_transcripts.consequence,
       sorted_transcripts.cons_str,
       genes.genes,
       0 as participant_count
   FROM sorted_transcripts, genes
   WHERE genes.vid = sorted_transcripts.vid
     AND (sorted_transcripts.row_number =1 or sorted_transcripts.transcript is NULL)"

  echo "Creating cb_variant_attribute_contig_position table."
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute_contig_position\` CLUSTER BY contig, position AS
   SELECT vid, contig, position
   FROM \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute\`"

  echo "Creating cb_variant_attribute_genes table."
  bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
  "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute_genes\` CLUSTER BY gene_symbol AS
   SELECT pv.vid, pv.gene_symbol
   FROM \`$BQ_PROJECT.$BQ_DATASET.prep_vat\` pv
   JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute\` pva ON pva.vid = pv.vid
   WHERE pv.gene_symbol IS NOT NULL
   GROUP BY pv.vid, pv.gene_symbol"

   echo "Creating cb_variant_attribute_rs_number table."
   bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
   "CREATE OR REPLACE TABLE \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute_rs_number\` CLUSTER BY rs_number AS
   SELECT pv.vid, rs_number
   FROM \`$BQ_PROJECT.$BQ_DATASET.prep_vat\` pv
   CROSS JOIN UNNEST(dbsnp_rsid) AS rs_number
   JOIN \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute\` pva ON pva.vid = pv.vid
   GROUP BY pv.vid, rs_number"

   echo "Updating cb_variant_attribute table with participant counts."
   bq --quiet --project_id="$BQ_PROJECT" query --batch --nouse_legacy_sql \
   "UPDATE \`$BQ_PROJECT.$BQ_DATASET.cb_variant_attribute\` x
    SET x.participant_count = y.size
    FROM (
     SELECT vid, ARRAY_LENGTH(person_ids) AS size
     FROM \`$BQ_PROJECT.$BQ_DATASET.cb_variant_to_person\`
    ) y
    WHERE x.vid = y.vid"

else
  echo "No VAT table exists in this dataset. Skipping build of cb_variant_attribute table."
fi