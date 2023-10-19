package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.VariantFilterRequest;

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

  public enum VatColumns {
    VARIANT_ID("Variant ID", "vid"),
    GENE("Gene", "genes"),
    CONSEQUENCE("Consequence", "cons_str"),
    PROTEIN_CHANGE("Protein Change", "protein_change"),
    CLINVAR_SIGNIFICANCE("ClinVar Significance", "clinical_significance_string"),
    ALLELE_COUNT("Allele Count", "allele_count"),
    ALLELE_NUMBER("Allele Number", "allele_number"),
    ALLELE_FREQUENCY("Allele Frequency", "allele_frequency"),
    PARTICIPANT_COUNT("Participant Count", "participant_count");
    private final String displayName;
    private final String columnName;

    VatColumns(String displayName, String columnName) {
      this.displayName = displayName;
      this.columnName = columnName;
    }

    public String getDisplayName() {
      return this.displayName;
    }

    public String getColumnName() {
      return this.columnName;
    }

    public static String getColumnNameFromDisplayName(String displayName) {
      return Arrays.stream(values())
          .filter(vatColumns -> vatColumns.getDisplayName().equals(displayName))
          .findFirst()
          .get()
          .getColumnName();
    }

    public static List<String> getDisplayNameList() {
      return Arrays.stream(values()).map(VatColumns::getDisplayName).collect(Collectors.toList());
    }
  }

  private static final String SELECT_ALL_COLUMNS =
      "SELECT DISTINCT vid, genes, cons_str, protein_change, clinical_significance_string, allele_count, allele_number, allele_frequency, participant_count\n";

  private static final String SELECT_COUNT = "SELECT COUNT(DISTINCT vid) AS count\n";

  private static final String SELECT_VID = "SELECT DISTINCT vid\n";

  private static final String DEFAULT_ORDER_BY = "ORDER BY participant_count DESC\n";

  private static final String LIMIT_OFFSET = "LIMIT @limit OFFSET @offset";

  private static final String PARTICIPANT_COUNT = "AND participant_count > 0\n";

  private static final String FROM_VAT = "FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n";

  private static final String CONSEQUENCE_LIKE = "cons_str LIKE @consStr";

  private static final String CONSEQUENCE_EMPTY = "cons_str = ''";

  private static final String CLINICAL_SIGNIFICANCE_LIKE =
      "clinical_significance_string LIKE @clinStr";

  private static final String CLINICAL_SIGNIFICANCE_EMPTY = "clinical_significance_string = ''";

  private static final String GENES_IN = "genes IN UNNEST(@genes)";
  private static final String GENES_IS_NULL = "genes IS NULL";

  private static final String ALLELE_COUNT = "AND allele_count BETWEEN @countMin AND @countMax\n";

  private static final String ALLELE_NUMBER =
      "AND allele_number BETWEEN @numberMin AND @numberMax\n";

  private static final String ALLELE_FREQ = "AND allele_frequency BETWEEN @freqMin AND @freqMax\n";

  private static final String ORDER_BY = "ORDER BY @orderBy\n";

  private static final String VID_SQL = "WHERE vid = @vid\n";
  private static final String NA = "n/a";

  private static final String CONTIG_POSITION_SQL =
      "WHERE vid IN (\n"
          + "SELECT vid\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_attribute_contig_position`\n"
          + "WHERE contig = @contig\n"
          + "AND position BETWEEN @start AND @end\n"
          + ")\n";

  private static final String GENE_SQL =
      "WHERE vid IN (\n"
          + "SELECT vid\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_attribute_genes`\n"
          + "WHERE gene_symbol = @gene\n"
          + ")\n";

  private static final String RS_NUMBER_SQL =
      "WHERE vid IN (\n"
          + "SELECT vid\n"
          + "FROM `${projectId}.${dataSetId}.cb_variant_attribute_rs_number`\n"
          + "WHERE rs_number = @rs_number\n"
          + ")\n";

  private static final String FILTERS_SQL =
      "WITH genes AS (\n"
          + "  SELECT ARRAY_AGG(DISTINCT genes ORDER BY genes) AS gene_list\n"
          + "  FROM (\n"
          + "    SELECT CASE WHEN genes IS NULL THEN '"
          + NA
          + "' ELSE genes END AS genes\n"
          + "    FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "    WHERE vid IN (\n"
          + "      @innerSQL"
          + "    )\n"
          + "  )\n"
          + "),\n"
          + "consequences AS (\n"
          + "  SELECT ARRAY_AGG(DISTINCT consequence_list ORDER BY consequence_list) AS consequence_list\n"
          + "  FROM (\n"
          + "    SELECT consequence_list\n"
          + "    FROM `${projectId}.${dataSetId}.cb_variant_attribute`,\n"
          + "    UNNEST(consequence) AS consequence_list\n"
          + "    WHERE vid IN (\n"
          + "      @innerSQL"
          + "    )\n"
          + "    UNION DISTINCT\n"
          + "    SELECT '"
          + NA
          + "' AS consequence_list\n"
          + "    FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "    WHERE vid IN (\n"
          + "       @innerSQL"
          + "    )\n"
          + "    AND ARRAY_LENGTH(consequence) = 0"
          + "  )\n"
          + "),\n"
          + "clinical_significance AS (\n"
          + "  SELECT ARRAY_AGG(DISTINCT clinical_significance_list ORDER BY clinical_significance_list) AS clinical_significance_list\n"
          + "  FROM (\n"
          + "    SELECT clinical_significance_list\n"
          + "    FROM `${projectId}.${dataSetId}.cb_variant_attribute`,\n"
          + "    UNNEST(clinical_significance) AS clinical_significance_list\n"
          + "    WHERE vid IN (\n"
          + "      @innerSQL"
          + "    )\n"
          + "    UNION DISTINCT\n"
          + "    SELECT '"
          + NA
          + "' AS clinical_significance_list\n"
          + "    FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "    WHERE vid IN (\n"
          + "       @innerSQL"
          + "    )\n"
          + "    AND ARRAY_LENGTH(clinical_significance) = 0"
          + "  )\n"
          + "),\n"
          + "allele AS (\n"
          + "  SELECT MIN(allele_count) AS count_min, MAX(allele_count) AS count_max, \n"
          + "  MIN(allele_number) AS number_min, MAX(allele_number) AS number_max,\n"
          + "  0 AS frequency_min, 1 AS frequency_max\n"
          + "  FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n"
          + "  WHERE vid IN (\n"
          + "    @innerSQL"
          + "  )\n"
          + ")\n"
          + "SELECT genes.gene_list, \n"
          + "       consequences.consequence_list, \n"
          + "       clinical_significance.clinical_significance_list, \n"
          + "       allele.count_min, \n"
          + "       allele.count_max, \n"
          + "       allele.number_min, \n"
          + "       allele.number_max,\n"
          + "       allele.frequency_min,\n"
          + "       allele.frequency_max\n"
          + "FROM genes, consequences, clinical_significance, allele";

  public static QueryJobConfiguration buildQuery(
      VariantFilterRequest filters, Integer limit, Integer offset) {
    return QueryJobConfiguration.newBuilder(generateSQL(Boolean.FALSE, filters))
        .setNamedParameters(generateParams(filters, limit, offset))
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildCountQuery(VariantFilterRequest filters) {
    return QueryJobConfiguration.newBuilder(generateSQL(Boolean.TRUE, filters))
        .setNamedParameters(generateParams(filters, null, null))
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildFiltersQuery(VariantFilterRequest filters) {
    String innerSQL = generateSQL(Boolean.TRUE, filters).replace(SELECT_COUNT, SELECT_VID);
    return QueryJobConfiguration.newBuilder(FILTERS_SQL.replace("@innerSQL", innerSQL))
        .setNamedParameters(generateParams(filters, null, null))
        .setUseLegacySql(false)
        .build();
  }

  @NotNull
  private static String generateSQL(boolean isCount, VariantFilterRequest filters) {
    switch (SearchTermType.fromValue(filters.getSearchTerm())) {
      case VID:
        // vid SQL only returns 1 variant so no need to add filters or pagination
        String sqlBody = FROM_VAT + VID_SQL + PARTICIPANT_COUNT;
        return isCount ? SELECT_COUNT + sqlBody : SELECT_ALL_COLUMNS + sqlBody;
      case CONTIG:
        return generateFilterSQL(
            isCount ? SELECT_COUNT : SELECT_ALL_COLUMNS, CONTIG_POSITION_SQL, filters);
      case GENE:
        return generateFilterSQL(isCount ? SELECT_COUNT : SELECT_ALL_COLUMNS, GENE_SQL, filters);
      case RS_NUMBER:
        return generateFilterSQL(
            isCount ? SELECT_COUNT : SELECT_ALL_COLUMNS, RS_NUMBER_SQL, filters);
      default:
        throw new BadRequestException("Search term not supported");
    }
  }

  @NotNull
  private static String generateFilterSQL(
      String selectSQL, String whereVidInSQL, VariantFilterRequest filters) {
    List<String> consequences = filters.getConsequenceList();
    List<String> clinicalSigns = filters.getClinicalSignificanceList();
    List<String> genes = filters.getGeneList();
    StringBuilder sqlBuilder = new StringBuilder(selectSQL).append(FROM_VAT);
    sqlBuilder.append(whereVidInSQL);
    appendEmptyOrLikeCondition(consequences, sqlBuilder, CONSEQUENCE_EMPTY, CONSEQUENCE_LIKE);
    appendEmptyOrLikeCondition(
        clinicalSigns, sqlBuilder, CLINICAL_SIGNIFICANCE_EMPTY, CLINICAL_SIGNIFICANCE_LIKE);
    if (isNotEmpty(genes)) {
      StringJoiner joiner = new StringJoiner(" OR ");
      if (genes.contains(NA)) {
        joiner.add(GENES_IS_NULL);
      }
      if (!genes.contains(NA) || genes.size() > 1) {
        joiner.add(GENES_IN);
      }
      sqlBuilder.append("AND (");
      sqlBuilder.append(joiner);
      sqlBuilder.append(")\n");
    }
    appendMinMaxCondition(
        sqlBuilder, filters.getCountMin() != null && filters.getCountMax() != null, ALLELE_COUNT);
    appendMinMaxCondition(
        sqlBuilder,
        filters.getNumberMin() != null && filters.getNumberMax() != null,
        ALLELE_NUMBER);
    appendMinMaxCondition(
        sqlBuilder,
        filters.getFrequencyMin() != null && filters.getFrequencyMax() != null,
        ALLELE_FREQ);
    sqlBuilder.append(PARTICIPANT_COUNT);
    if (selectSQL.equals(SELECT_ALL_COLUMNS)) {
      if (StringUtils.isNotEmpty(filters.getSortBy())) {
        sqlBuilder.append(
            ORDER_BY.replace(
                "@orderBy", VatColumns.getColumnNameFromDisplayName(filters.getSortBy())));
        sqlBuilder.append(LIMIT_OFFSET);
      } else {
        sqlBuilder.append(DEFAULT_ORDER_BY);
        sqlBuilder.append(LIMIT_OFFSET);
      }
    }
    return sqlBuilder.toString();
  }

  private static void appendEmptyOrLikeCondition(
      List<String> list, StringBuilder sqlBuilder, String empty, String like) {
    if (isNotEmpty(list)) {
      StringJoiner joiner = new StringJoiner(" OR ");
      int i = 0;
      for (String conStr : list) {
        if (conStr.equals(NA)) {
          joiner.add(empty);
        } else {
          joiner.add(like + i++);
        }
      }
      sqlBuilder.append("AND (");
      sqlBuilder.append(joiner);
      sqlBuilder.append(")\n");
    }
  }

  private static void appendMinMaxCondition(
      StringBuilder sqlBuilder, boolean filters, String alleleCount) {
    if (filters) {
      sqlBuilder.append(alleleCount);
    }
  }

  private static boolean isNotEmpty(List<String> list) {
    return CollectionUtils.isNotEmpty(list);
  }

  @NotNull
  private static Map<String, QueryParameterValue> generateParams(
      VariantFilterRequest filters, Integer limit, Integer offset) {
    String searchTerm = filters.getSearchTerm();
    Map<String, QueryParameterValue> params = new HashMap<>();
    switch (SearchTermType.fromValue(searchTerm)) {
      case VID:
        // vid SQL only returns 1 variant so no need to add filter params
        params.put("vid", QueryParameterValue.string(searchTerm.toUpperCase()));
        return params;
      case CONTIG:
        // example search term for contig -> chr13:32355000-32375000
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
        generateFilterParams(filters, params);
        return params;
      case GENE:
        params.put("gene", QueryParameterValue.string(searchTerm.toUpperCase()));
        if (limit != null) {
          params.put("limit", QueryParameterValue.int64(limit));
        }
        if (offset != null) {
          params.put("offset", QueryParameterValue.int64(offset));
        }
        generateFilterParams(filters, params);
        return params;
      case RS_NUMBER:
        params.put("rs_number", QueryParameterValue.string(searchTerm.toLowerCase()));
        if (limit != null) {
          params.put("limit", QueryParameterValue.int64(limit));
        }
        if (offset != null) {
          params.put("offset", QueryParameterValue.int64(offset));
        }
        generateFilterParams(filters, params);
        return params;
      default:
        throw new BadRequestException("Search term not supported");
    }
  }

  private static void generateFilterParams(
      VariantFilterRequest filters, Map<String, QueryParameterValue> params) {
    List<String> consequences = filters.getConsequenceList();
    List<String> clinicalSigns = filters.getClinicalSignificanceList();
    if (isNotEmpty(consequences)) {
      // Don't change the source consequence list as it's used for multiple queries.
      // Generate a new list without n/a to exclude from the IN clause.
      List<String> cons =
          consequences.stream().filter(s -> !s.equals(NA)).collect(Collectors.toList());
      for (int i = 0; i < cons.size(); i++) {
        params.put("consStr" + i, QueryParameterValue.string("%" + cons.get(i) + "%"));
      }
    }
    if (isNotEmpty(clinicalSigns)) {
      // Don't change the source consequence list as it's used for multiple queries.
      // Generate a new list without n/a to exclude from the IN clause.
      List<String> clinSigns =
          clinicalSigns.stream().filter(s -> !s.equals(NA)).collect(Collectors.toList());
      for (int i = 0; i < clinSigns.size(); i++) {
        params.put("clinStr" + i, QueryParameterValue.string("%" + clinSigns.get(i) + "%"));
      }
    }
    if (isNotEmpty(filters.getGeneList())) {
      // convert n/a to empty string when searching genes
      params.put(
          "genes",
          QueryParameterValue.array(
              filters.getGeneList().stream().filter(s -> !s.equals(NA)).toArray(String[]::new),
              String.class));
    }
    if (filters.getCountMin() != null && filters.getCountMax() != null) {
      params.put("countMin", QueryParameterValue.int64(filters.getCountMin()));
      params.put("countMax", QueryParameterValue.int64(filters.getCountMax()));
    }
    if (filters.getNumberMin() != null && filters.getNumberMax() != null) {
      params.put("numberMin", QueryParameterValue.int64(filters.getNumberMin()));
      params.put("numberMax", QueryParameterValue.int64(filters.getNumberMax()));
    }
    if (filters.getFrequencyMin() != null && filters.getFrequencyMax() != null) {
      params.put("freqMin", QueryParameterValue.bigNumeric(filters.getFrequencyMin()));
      params.put("freqMax", QueryParameterValue.bigNumeric(filters.getFrequencyMax()));
    }
  }
}
