CREATE OR REPLACE TABLE `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute`
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
    FROM `all-of-us-ehr-dev.SC2022Q4R6.prep_vat`
    WHERE is_canonical_transcript OR transcript IS NULL
    ORDER BY vid, row_number),
    genes AS (
      SELECT vid, ARRAY_TO_STRING(ARRAY_AGG(DISTINCT gene_symbol IGNORE NULLS ORDER BY gene_symbol), ', ') AS genes
      FROM `all-of-us-ehr-dev.SC2022Q4R6.prep_vat`
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
  AND (sorted_transcripts.row_number =1 or sorted_transcripts.transcript is NULL);

CREATE OR REPLACE TABLE `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute_contig_position` CLUSTER BY contig, position AS
SELECT vid, contig, position
FROM `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute`;

CREATE OR REPLACE TABLE `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute_genes` CLUSTER BY gene_symbol AS
SELECT pv.vid, pv.gene_symbol
FROM `all-of-us-ehr-dev.SC2022Q4R6.prep_vat` pv
JOIN `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute` pva ON pva.vid = pv.vid
WHERE pv.gene_symbol IS NOT NULL
GROUP BY pv.vid, pv.gene_symbol;

CREATE OR REPLACE TABLE `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute_rs_number` CLUSTER BY rs_number AS
SELECT pv.vid, rs_number
FROM `all-of-us-ehr-dev.SC2022Q4R6.prep_vat` pv
CROSS JOIN UNNEST(dbsnp_rsid) AS rs_number
JOIN `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute` pva ON pva.vid = pv.vid
GROUP BY pv.vid, rs_number;

UPDATE `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_attribute` x
SET x.participant_count = y.size
    FROM (
  SELECT vid, ARRAY_LENGTH(person_ids) AS size
  FROM `all-of-us-ehr-dev.SC2022Q4R6.cb_variant_to_person`
) y
WHERE x.vid = y.vid;