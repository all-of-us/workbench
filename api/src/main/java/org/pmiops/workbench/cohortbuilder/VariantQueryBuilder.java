package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.exceptions.BadRequestException;

public final class VariantQueryBuilder {

  private enum SearchTermType {
    VID,
    CONTIG,
    GENE,
    RS_NUMBER;

    public static SearchTermType fromValue(String searchTerm) {
      // rs number pattern ex: rs23346
      String rsNumberPattern = "^rs.*";
      // chromosome position pattern ex: chr20:955-1000
      String contigPattern = ".*:.*-.*";
      // variant id pattern ex: 1-101504524-G-A
      String vidPattern = ".*-.*-.*-.*";
      if (Pattern.matches(rsNumberPattern, searchTerm)) {
        return RS_NUMBER;
      } else if (Pattern.matches(contigPattern, searchTerm)) {
        return CONTIG;
      } else if (Pattern.matches(vidPattern, searchTerm)) {
        return VID;
      }
      // any patterns that don't match are consider a gene name
      return GENE;
    }
  }

  private static final String SELECT_ALL_COLUMNS =
      "SELECT vid, genes, cons_str, protein_change, clinical_significance, allele_count, allele_number, allele_frequency, participant_count\n";

  private static final String SELECT_COUNT = "SELECT COUNT(vid) AS count\n";

  private static final String ORDER_BY = "ORDER BY participant_count DESC\n";

  private static final String LIMIT_OFFSET = "LIMIT @limit\n OFFSET @offset";

  private static final String PARTICIPANT_COUNT = "AND participant_count > 0\n";

  private static final String VID_SQL =
      "FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n WHERE vid = @vid\n";

  private static final String CONTIG_POSITION_SQL =
      "FROM `${projectId}.${dataSetId}.cb_variant_attribute_contig_position`\n"
          + "WHERE contig = @contig\n"
          + "AND position BETWEEN @start AND @end\n";

  private static final String GENE_SQL =
      "FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "WHERE vid IN (\n"
          + "SELECT vid\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_attribute_genes`\n"
          + "WHERE gene_symbol = @gene\n"
          + ")\n";

  private static final String RS_NUMBER_SQL =
      "FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "WHERE vid IN (\n"
          + "SELECT vid\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_attribute_rs_number`\n"
          + "WHERE rs_number = @rs_number\n"
          + ")\n";

  public static QueryJobConfiguration buildQuery(String searchTerm, Integer limit, Integer offset) {
    return QueryJobConfiguration.newBuilder(generateSQL(searchTerm, Boolean.FALSE))
        .setNamedParameters(generateParams(searchTerm, limit, offset))
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildCountQuery(String searchTerm) {
    return QueryJobConfiguration.newBuilder(generateSQL(searchTerm, Boolean.TRUE))
        .setNamedParameters(generateParams(searchTerm, null, null))
        .setUseLegacySql(false)
        .build();
  }

  @NotNull
  private static String generateSQL(String searchTerm, boolean isCount) {
    switch (SearchTermType.fromValue(searchTerm)) {
      case VID:
        return isCount ? SELECT_COUNT + VID_SQL + PARTICIPANT_COUNT : SELECT_ALL_COLUMNS + VID_SQL + PARTICIPANT_COUNT;
      case CONTIG:
        return isCount
            ? SELECT_COUNT + CONTIG_POSITION_SQL + PARTICIPANT_COUNT
            : SELECT_ALL_COLUMNS + CONTIG_POSITION_SQL + PARTICIPANT_COUNT + ORDER_BY + LIMIT_OFFSET;
      case GENE:
        return isCount
            ? SELECT_COUNT + GENE_SQL + PARTICIPANT_COUNT
            : SELECT_ALL_COLUMNS + GENE_SQL + PARTICIPANT_COUNT + ORDER_BY + LIMIT_OFFSET;
      case RS_NUMBER:
        return isCount
            ? SELECT_COUNT + RS_NUMBER_SQL + PARTICIPANT_COUNT
            : SELECT_ALL_COLUMNS + RS_NUMBER_SQL + PARTICIPANT_COUNT + ORDER_BY + LIMIT_OFFSET;
      default:
        throw new BadRequestException("Search term not supported");
    }
  }

  @NotNull
  private static Map<String, QueryParameterValue> generateParams(
      String searchTerm, Integer limit, Integer offset) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    switch (SearchTermType.fromValue(searchTerm)) {
      case VID:
        params.put("vid", QueryParameterValue.string(searchTerm.toUpperCase()));
        return params;
      case CONTIG:
        // chr13:32355000-32375000
        String[] chr = searchTerm.split(":");
        String[] position = chr[1].split("-");
        params.put("contig", QueryParameterValue.string(chr[0].toLowerCase()));
        params.put("start", QueryParameterValue.int64(Integer.valueOf(position[0])));
        params.put("end", QueryParameterValue.int64(Integer.valueOf(position[1])));
        if (limit != null) {
          params.put("limit", QueryParameterValue.int64(limit));
        }
        if (offset != null) {
          params.put("offset", QueryParameterValue.int64(offset));
        }
        return params;
      case GENE:
        params.put("gene", QueryParameterValue.string(searchTerm.toUpperCase()));
        if (limit != null) {
          params.put("limit", QueryParameterValue.int64(limit));
        }
        if (offset != null) {
          params.put("offset", QueryParameterValue.int64(offset));
        }
        return params;
      case RS_NUMBER:
        params.put("rs_number", QueryParameterValue.string(searchTerm.toLowerCase()));
        if (limit != null) {
          params.put("limit", QueryParameterValue.int64(limit));
        }
        if (offset != null) {
          params.put("offset", QueryParameterValue.int64(offset));
        }
        return params;
      default:
        throw new BadRequestException("Search term not supported");
    }
  }
}
