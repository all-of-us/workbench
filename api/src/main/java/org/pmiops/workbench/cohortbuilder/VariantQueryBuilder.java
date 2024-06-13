package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ParticipantCountFilter;
import org.pmiops.workbench.model.VariantFilter;
import org.pmiops.workbench.model.VariantFilterRequest;
import org.pmiops.workbench.utils.OperatorUtils;

public final class VariantQueryBuilder {

  private static final String SELECT_ALL_COLUMNS =
      "SELECT DISTINCT vid, genes, cons_str, protein_change, clinical_significance_string, allele_count, allele_number, allele_frequency, participant_count\n";

  private static final String SELECT_COUNT = "SELECT COUNT(DISTINCT vid) AS count\n";

  private static final String SELECT_VID = "SELECT DISTINCT vid\n";

  private static final String DEFAULT_ORDER_BY = "ORDER BY participant_count DESC\n";

  private static final String LIMIT_OFFSET = "LIMIT @limit OFFSET @offset";

  private static final String DEFAULT_PARTICIPANT_COUNT = "AND participant_count > 0\n";

  private static final String PARTICIPANT_COUNT = "AND participant_count %s @participantCount\n";

  private static final String PARTICIPANT_COUNT_BETWEEN =
      "AND participant_count %s @participantCount1 AND @participantCount2\n";

  private static final String FROM_VAT = "FROM `${projectId}.${dataSetId}.cb_variant_attribute`\n";

  private static final String CONSEQUENCE_LIKE = "cons_str LIKE @consStr";

  private static final String CONSEQUENCE_EMPTY = "cons_str = ''";

  private static final String CLINICAL_SIGNIFICANCE_LIKE =
      "clinical_significance_string LIKE @clinStr";

  private static final String CLINICAL_SIGNIFICANCE_EMPTY = "clinical_significance_string = ''";

  private static final String GENES_IN = "genes IN unnest(@genes)";

  private static final String GENES_IS_NULL = "genes IS NULL";

  private static final String ALLELE_COUNT = "AND allele_count BETWEEN @countMin AND @countMax\n";

  private static final String ALLELE_NUMBER =
      "AND allele_number BETWEEN @numberMin AND @numberMax\n";

  private static final String ALLELE_FREQ = "AND allele_frequency BETWEEN @freqMin AND @freqMax\n";

  private static final String AND_EXCLUSION_LIST_SQL = "AND vid NOT IN unnest(@exclusionList)\n";

  private static final String ORDER_BY = "ORDER BY @orderBy\n";

  private static final String VID_SQL = "WHERE vid = @vid\n";

  private static final String NA = "n/a";

  private static final String PARTICIPANT_COUNT_GREATER_THAN_THRESHOLD =
      "AND participant_count > %d";
  private static final String PARTICIPANT_COUNT_LESS_THAN_OR_EQUAL_TO_THRESHOLD =
      "AND participant_count <= %d";

  public static final String SELECT_ZERO = "SELECT 0 as count";

  private static final String VARIANT_TO_PERSON_SQL =
      """
             SELECT COUNT(DISTINCT person_id) AS participant_count
             FROM `${projectId}.${dataSetId}.cb_variant_to_person`
             CROSS JOIN UNNEST(person_ids) AS person_id
             WHERE vid IN (
               %s
             )""";

  private static final String CONTIG_POSITION_SQL =
      """
             WHERE vid IN (
              SELECT vid
              FROM `${projectId}.${dataSetId}.cb_variant_attribute_contig_position`
              WHERE contig = @contig
              AND position BETWEEN @start AND @end
             )
             """;

  private static final String GENE_SQL =
      """
            WHERE vid IN (
              SELECT vid
              FROM `${projectId}.${dataSetId}.cb_variant_attribute_genes`
              WHERE UPPER(gene_symbol) = @singleGene
            )
            """;

  private static final String RS_NUMBER_SQL =
      """
            WHERE vid IN (
              SELECT vid
              FROM `${projectId}.${dataSetId}.cb_variant_attribute_rs_number`
              WHERE rs_number = @rs_number
            )
            """;

  private static final String FILTERS_SQL =
      """
            WITH genes AS (
              SELECT ARRAY_AGG(DISTINCT genes ORDER BY genes) AS gene_list
              FROM (
                SELECT CASE WHEN genes IS NULL THEN 'n/a' ELSE genes END AS genes
                FROM `${projectId}.${dataSetId}.cb_variant_attribute`
                WHERE vid IN (
                  @innerSQL
                )
              )
            ),
            consequences AS (
              SELECT ARRAY_AGG(DISTINCT consequence_list ORDER BY consequence_list) AS consequence_list
              FROM (
                SELECT consequence_list
                FROM `${projectId}.${dataSetId}.cb_variant_attribute`,
                UNNEST(consequence) AS consequence_list
                WHERE vid IN (
                  @innerSQL
                )
                UNION DISTINCT
                SELECT 'n/a' AS consequence_list
                FROM `${projectId}.${dataSetId}.cb_variant_attribute`
                WHERE vid IN (
                  @innerSQL
                )
                AND ARRAY_LENGTH(consequence) = 0
              )
            ),
            clinical_significance AS (
              SELECT ARRAY_AGG(DISTINCT clinical_significance_list ORDER BY clinical_significance_list) AS clinical_significance_list
              FROM (
                SELECT clinical_significance_list
                FROM `${projectId}.${dataSetId}.cb_variant_attribute`,
                UNNEST(clinical_significance) AS clinical_significance_list
                WHERE vid IN (
                  @innerSQL
                )
                UNION DISTINCT
                SELECT 'n/a' AS clinical_significance_list
                FROM `${projectId}.${dataSetId}.cb_variant_attribute`
                WHERE vid IN (
                  @innerSQL
                )
                AND ARRAY_LENGTH(clinical_significance) = 0
              )
            ),
            allele AS (
              SELECT MIN(allele_count) AS count_min, MAX(allele_count) AS count_max,\s
                     MIN(allele_number) AS number_min, MAX(allele_number) AS number_max,
                     0 AS frequency_min, 1 AS frequency_max
              FROM `${projectId}.${dataSetId}.cb_variant_attribute`
              WHERE vid IN (
                @innerSQL
              )
            )
            SELECT genes.gene_list,
                   consequences.consequence_list,
                   clinical_significance.clinical_significance_list,
                   allele.count_min,
                   allele.count_max,
                   allele.number_min,
                   allele.number_max,
                   allele.frequency_min,
                   allele.frequency_max
            FROM genes, consequences, clinical_significance, allele""";

  private static final String FILTER_INFO_SQL =
      """
              WITH vids_count AS (
                @vids_count_sql
              ),
              participant_count AS (
                @participant_count_sql
              ),
              less_than_or_equal_to_five_thousand AS (
                @less_than_or_equal_to_five_thousand_sql
              ),
              over_five_thousand AS (
                @over_five_thousand_sql
              ),
              over_ten_thousand AS (
                @over_ten_thousand_sql
              ),
              over_hundred_thousand AS (
                @over_hundred_thousand_sql
              ),
              over_two_hundred_thousand AS (
                @over_two_hundred_thousand_sql
              )
              SELECT vids_count.count,
                     participant_count.participant_count,
                     less_than_or_equal_to_five_thousand.count,
                     over_five_thousand.count,
                     over_ten_thousand.count,
                     over_hundred_thousand.count,
                     over_two_hundred_thousand.count
              FROM vids_count,
                   participant_count,
                   less_than_or_equal_to_five_thousand,
                   over_five_thousand,
                   over_ten_thousand,
                   over_hundred_thousand,
                   over_two_hundred_thousand""";

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

  public static QueryJobConfiguration buildQuery(
      VariantFilterRequest filters, Integer limit, Integer offset) {
    String sql = generateSQL(SELECT_ALL_COLUMNS, filters);

    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String finalSQL = generateAndAddParams(filters, sql, queryParams, limit, offset);

    return QueryJobConfiguration.newBuilder(finalSQL)
        .setNamedParameters(queryParams)
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildCountQuery(VariantFilterRequest filter) {
    String sql = generateSQL(SELECT_COUNT, filter);

    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String finalSQL = generateAndAddParams(filter, sql, queryParams, null, null);

    return QueryJobConfiguration.newBuilder(finalSQL)
        .setNamedParameters(queryParams)
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildFiltersQuery(VariantFilterRequest filter) {
    String innerSQL = generateSQL(SELECT_VID, filter);

    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String finalInnerSQL = generateAndAddParams(filter, innerSQL, queryParams, null, null);

    return QueryJobConfiguration.newBuilder(FILTERS_SQL.replace("@innerSQL", finalInnerSQL))
        .setNamedParameters(queryParams)
        .setUseLegacySql(false)
        .build();
  }

  public static QueryJobConfiguration buildFilterInfoQuery(VariantFilter filter) {
    String vidsCountSQL = generateSQL(SELECT_COUNT, filter);

    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String vidsCountWithParamsSQL =
        generateAndAddParams(filter, vidsCountSQL, queryParams, null, null);

    String participantCountSQL = buildCohortBuilderQuery(filter, queryParams);
    String participantCountWithParamsSQL =
        generateAndAddParams(filter, participantCountSQL, queryParams, null, null);

    boolean isParticipantCountRange = filter.getParticipantCountRange() != null;
    String lessThanOrEqualToFiveThousandSQL =
        isParticipantCountRange
            ? SELECT_ZERO
            : vidsCountWithParamsSQL
                + String.format(PARTICIPANT_COUNT_LESS_THAN_OR_EQUAL_TO_THRESHOLD, 5000);
    String overFiveThousandSQL =
        isParticipantCountRange
            ? SELECT_ZERO
            : vidsCountWithParamsSQL
                + String.format(PARTICIPANT_COUNT_GREATER_THAN_THRESHOLD, 5000);
    String overTenThousandSQL =
        isParticipantCountRange
            ? SELECT_ZERO
            : vidsCountWithParamsSQL
                + String.format(PARTICIPANT_COUNT_GREATER_THAN_THRESHOLD, 10000);
    String overHundredThousandSQL =
        isParticipantCountRange
            ? SELECT_ZERO
            : vidsCountWithParamsSQL
                + String.format(PARTICIPANT_COUNT_GREATER_THAN_THRESHOLD, 100000);
    String overTwoHundredThousandSQL =
        isParticipantCountRange
            ? SELECT_ZERO
            : vidsCountWithParamsSQL
                + String.format(PARTICIPANT_COUNT_GREATER_THAN_THRESHOLD, 200000);

    String finalSQL =
        FILTER_INFO_SQL
            .replace("@vids_count_sql", vidsCountWithParamsSQL.trim())
            .replace(
                "@participant_count_sql",
                String.format(VARIANT_TO_PERSON_SQL, participantCountWithParamsSQL.trim()))
            .replace("@less_than_or_equal_to_five_thousand_sql", lessThanOrEqualToFiveThousandSQL)
            .replace("@over_five_thousand_sql", overFiveThousandSQL)
            .replace("@over_ten_thousand_sql", overTenThousandSQL)
            .replace("@over_hundred_thousand_sql", overHundredThousandSQL)
            .replace("@over_two_hundred_thousand_sql", overTwoHundredThousandSQL);

    return QueryJobConfiguration.newBuilder(finalSQL)
        .setNamedParameters(queryParams)
        .setUseLegacySql(false)
        .build();
  }

  public static String buildCohortBuilderQuery(
      VariantFilter filter, Map<String, QueryParameterValue> params) {
    String sql = generateSQL(SELECT_VID, filter);
    return generateAndAddParams(filter, sql, params, null, null);
  }

  private static String generateSQL(String selectSQL, VariantFilter filter) {
    return switch (SearchTermType.fromValue(filter.getSearchTerm())) {
      case VID ->
      // vid SQL only returns 1 variant so no need to add filters or pagination
      selectSQL + FROM_VAT + VID_SQL + DEFAULT_PARTICIPANT_COUNT;
      case CONTIG -> generateFilterSQL(selectSQL, CONTIG_POSITION_SQL, filter);
      case GENE -> generateFilterSQL(selectSQL, GENE_SQL, filter);
      case RS_NUMBER -> generateFilterSQL(selectSQL, RS_NUMBER_SQL, filter);
    };
  }

  private static String generateFilterSQL(
      String selectSQL, String whereVidInSQL, VariantFilter filter) {
    List<String> consequences = filter.getConsequenceList();
    List<String> clinicalSigns = filter.getClinicalSignificanceList();
    List<String> genes = filter.getGeneList();
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
        sqlBuilder, filter.getCountMin() != null && filter.getCountMax() != null, ALLELE_COUNT);
    appendMinMaxCondition(
        sqlBuilder, filter.getNumberMin() != null && filter.getNumberMax() != null, ALLELE_NUMBER);
    appendMinMaxCondition(
        sqlBuilder,
        filter.getFrequencyMin() != null && filter.getFrequencyMax() != null,
        ALLELE_FREQ);

    // add participant count range if exists
    sqlBuilder.append(appendParticipantCountRange(filter));

    // add exclusion list if exists
    if (filter.getExclusionList() != null) {
      sqlBuilder.append(AND_EXCLUSION_LIST_SQL);
    }

    if (selectSQL.equals(SELECT_ALL_COLUMNS)) {
      String sortBy = ((VariantFilterRequest) filter).getSortBy();
      if (StringUtils.isNotEmpty(sortBy)) {
        if (sortBy.equals(VatColumns.PARTICIPANT_COUNT.getDisplayName())) {
          sqlBuilder.append(DEFAULT_ORDER_BY);
        } else {
          sqlBuilder.append(
              ORDER_BY.replace("@orderBy", VatColumns.getColumnNameFromDisplayName(sortBy)));
        }
        sqlBuilder.append(LIMIT_OFFSET);
      } else {
        sqlBuilder.append(DEFAULT_ORDER_BY);
        sqlBuilder.append(LIMIT_OFFSET);
      }
    }
    return sqlBuilder.toString();
  }

  private static String appendParticipantCountRange(VariantFilter filter) {
    if (filter.getParticipantCountRange() == null) {
      return DEFAULT_PARTICIPANT_COUNT;
    }
    return switch (filter.getParticipantCountRange().getOperator()) {
      case GREATER_THAN, GREATER_THAN_OR_EQUAL_TO -> String.format(
          PARTICIPANT_COUNT,
          OperatorUtils.getSqlOperator(filter.getParticipantCountRange().getOperator()));
      case BETWEEN, LESS_THAN, LESS_THAN_OR_EQUAL_TO -> String.format(
          PARTICIPANT_COUNT_BETWEEN, OperatorUtils.getSqlOperator(Operator.BETWEEN));
      default -> throw new BadRequestException(
          "Participant Count filter does not support the following operator: "
              + filter.getParticipantCountRange().getOperator());
    };
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

  private static boolean isNotEmpty(Collection<?> list) {
    return CollectionUtils.isNotEmpty(list);
  }

  private static String generateAndAddParams(
      VariantFilter filter,
      String sql,
      Map<String, QueryParameterValue> params,
      Integer limit,
      Integer offset) {
    Map<String, String> replaceParams = new HashMap<>();
    String searchTerm = filter.getSearchTerm();
    switch (SearchTermType.fromValue(searchTerm)) {
      case VID -> {
        // vid SQL only returns 1 variant so no need to add filter params
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string(searchTerm.toUpperCase()));
        replaceParams.put("@vid", namedParameter);
      }
      case CONTIG -> {
        // example search term for contig -> chr13:32355000-32375000
        String[] chr = searchTerm.split(":");
        String[] position = chr[1].split("-");

        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string(chr[0].toLowerCase()));
        replaceParams.put("@contig", namedParameter);

        namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.int64(Integer.valueOf(position[0])));
        replaceParams.put("@start", namedParameter);

        namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.int64(Integer.valueOf(position[1])));
        replaceParams.put("@end", namedParameter);
        generateFilterParams(filter, params, replaceParams);
        addLimitAndOffset(params, replaceParams, limit, offset);
      }
      case GENE -> {
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string(searchTerm.toUpperCase()));
        replaceParams.put("@singleGene", namedParameter);
        generateFilterParams(filter, params, replaceParams);
        addLimitAndOffset(params, replaceParams, limit, offset);
      }
      case RS_NUMBER -> {
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string(searchTerm.toLowerCase()));
        replaceParams.put("@rs_number", namedParameter);
        generateFilterParams(filter, params, replaceParams);
        addLimitAndOffset(params, replaceParams, limit, offset);
      }
      default -> throw new BadRequestException("Search term not supported");
    }
    return replaceParams.entrySet().stream()
        .reduce(
            sql,
            (result, entry) -> result.replaceAll(entry.getKey(), entry.getValue()),
            (s1, s2) -> s1);
  }

  private static void generateFilterParams(
      VariantFilter filter,
      Map<String, QueryParameterValue> params,
      Map<String, String> replaceParams) {
    List<String> consequences = filter.getConsequenceList();
    List<String> clinicalSigns = filter.getClinicalSignificanceList();
    if (isNotEmpty(consequences)) {
      // Don't change the source consequence list as it's used for multiple queries.
      // Generate a new list without n/a to exclude from the IN clause.
      List<String> cons = consequences.stream().filter(s -> !s.equals(NA)).toList();
      for (int i = 0; i < cons.size(); i++) {
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string("%" + cons.get(i) + "%"));
        replaceParams.put("@consStr" + i, namedParameter);
      }
    }
    if (isNotEmpty(clinicalSigns)) {
      // Don't change the source consequence list as it's used for multiple queries.
      // Generate a new list without n/a to exclude from the IN clause.
      List<String> clinSigns = clinicalSigns.stream().filter(s -> !s.equals(NA)).toList();
      for (int i = 0; i < clinSigns.size(); i++) {
        String namedParameter =
            QueryParameterUtil.addQueryParameterValue(
                params, QueryParameterValue.string("%" + clinSigns.get(i) + "%"));
        replaceParams.put("@clinStr" + i, namedParameter);
      }
    }
    if (isNotEmpty(filter.getGeneList())) {
      // convert n/a to empty string when searching genes
      String namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params,
              QueryParameterValue.array(
                  filter.getGeneList().stream().filter(s -> !s.equals(NA)).toArray(String[]::new),
                  String.class));
      replaceParams.put("@genes", namedParameter);
    }
    if (filter.getCountMin() != null && filter.getCountMax() != null) {
      String namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(filter.getCountMin()));
      replaceParams.put("@countMin", namedParameter);
      namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(filter.getCountMax()));
      replaceParams.put("@countMax", namedParameter);
    }
    if (filter.getNumberMin() != null && filter.getNumberMax() != null) {
      String namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(filter.getNumberMin()));
      replaceParams.put("@numberMin", namedParameter);
      namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.int64(filter.getNumberMax()));
      replaceParams.put("@numberMax", namedParameter);
    }
    if (filter.getFrequencyMin() != null && filter.getFrequencyMax() != null) {
      String namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.bigNumeric(filter.getFrequencyMin()));
      replaceParams.put("@freqMin", namedParameter);
      namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params, QueryParameterValue.bigNumeric(filter.getFrequencyMax()));
      replaceParams.put("@freqMax", namedParameter);
    }
    if (filter.getParticipantCountRange() != null) {
      String namedParameter1;
      String namedParameter2;
      ParticipantCountFilter range = filter.getParticipantCountRange();
      switch (range.getOperator()) {
        case GREATER_THAN, GREATER_THAN_OR_EQUAL_TO -> {
          namedParameter1 =
              QueryParameterUtil.addQueryParameterValue(
                  params, QueryParameterValue.int64(range.getOperands().get(0)));
          replaceParams.put("@participantCount", namedParameter1);
        }
        case BETWEEN -> {
          namedParameter1 =
              QueryParameterUtil.addQueryParameterValue(
                  params, QueryParameterValue.int64(range.getOperands().get(0)));
          namedParameter2 =
              QueryParameterUtil.addQueryParameterValue(
                  params, QueryParameterValue.int64(range.getOperands().get(1)));
          replaceParams.put("@participantCount1", namedParameter1);
          replaceParams.put("@participantCount2", namedParameter2);
        }
        case LESS_THAN, LESS_THAN_OR_EQUAL_TO -> {
          namedParameter1 =
              QueryParameterUtil.addQueryParameterValue(params, QueryParameterValue.int64(1));
          Integer upper =
              range.getOperator().equals(Operator.LESS_THAN)
                  ? range.getOperands().get(0) - 1
                  : range.getOperands().get(0);
          namedParameter2 =
              QueryParameterUtil.addQueryParameterValue(params, QueryParameterValue.int64(upper));
          replaceParams.put("@participantCount1", namedParameter1);
          replaceParams.put("@participantCount2", namedParameter2);
        }
        default -> throw new BadRequestException(
            "Participant Count filter does not support the following operator: "
                + filter.getParticipantCountRange().getOperator());
      }
    }
    if (isNotEmpty(filter.getExclusionList())) {
      String namedParameter =
          QueryParameterUtil.addQueryParameterValue(
              params,
              QueryParameterValue.array(
                  filter.getExclusionList().toArray(new String[0]), String.class));
      replaceParams.put("@exclusionList", namedParameter);
    }
  }

  private static void addLimitAndOffset(
      Map<String, QueryParameterValue> params,
      Map<String, String> replaceParams,
      Integer limit,
      Integer offset) {
    String namedParameter;
    if (limit != null) {
      namedParameter =
          QueryParameterUtil.addQueryParameterValue(params, QueryParameterValue.int64(limit));
      replaceParams.put("@limit", namedParameter);
    }
    if (offset != null) {
      namedParameter =
          QueryParameterUtil.addQueryParameterValue(params, QueryParameterValue.int64(offset));
      replaceParams.put("@offset", namedParameter);
    }
  }
}
