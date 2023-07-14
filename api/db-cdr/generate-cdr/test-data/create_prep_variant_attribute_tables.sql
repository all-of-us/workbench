CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_attribute`
    CLUSTER BY vid
AS
    WITH sorted_transcripts AS (SELECT vid, consequence, aa_change AS protein_change,
        contig, position, ref_allele, alt_allele, transcript, ARRAY_TO_STRING(consequence, ',') AS cons_str, dna_change_in_transcript, clinvar_classification AS clinical_significance,
    gvs_all_ac, gvs_all_an, gvs_all_af,
    ROW_NUMBER() OVER(PARTITION BY vid ORDER BY CASE ARRAY_TO_STRING(consequence, ',')
    WHEN 'upstream_gene_variant'
    THEN 4
    WHEN 'downstream_gene_variant'
    THEN 5
    ELSE 1
    END
    ASC
    )  AS row_number
    FROM `aou-res-curation-output-prod.C2022Q4R9.prep_vat`
    WHERE is_canonical_transcript OR transcript IS NULL
    ORDER BY vid, row_number),
    genes AS (
      SELECT vid, ARRAY_TO_STRING(ARRAY_AGG(DISTINCT gene_symbol IGNORE NULLS ORDER BY gene_symbol), ', ') AS genes
      FROM `aou-res-curation-output-prod.C2022Q4R9.prep_vat`
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
    sorted_transcripts.transcript as transcript,
    sorted_transcripts.contig,
    sorted_transcripts.position,
    sorted_transcripts.ref_allele,
    sorted_transcripts.alt_allele,
    sorted_transcripts.cons_str,
    genes.genes
FROM sorted_transcripts, genes
WHERE genes.vid = sorted_transcripts.vid
  AND (sorted_transcripts.row_number =1 or sorted_transcripts.transcript is NULL);

CREATE MATERIALIZED VIEW `aou-res-curation-output-prod.C2022Q4R9.prep_variant_attribute_contig_position`
  CLUSTER BY contig, position
  AS
SELECT *
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_variant_attribute`;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_attribute_genes` CLUSTER BY gene_symbol AS
SELECT vid, gene_symbol
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_vat`
WHERE gene_symbol IS NOT NULL
GROUP BY vid, gene_symbol;

CREATE OR REPLACE TABLE `aou-res-curation-output-prod.C2022Q4R9.prep_variant_attribute_rs_number` CLUSTER BY rs_number AS
SELECT vid, rs_number
FROM `aou-res-curation-output-prod.C2022Q4R9.prep_vat`
CROSS JOIN UNNEST(dbsnp_rsid) AS rs_number
GROUP BY vid, rs_number;