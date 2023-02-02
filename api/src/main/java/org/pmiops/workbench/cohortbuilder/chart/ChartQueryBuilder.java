package org.pmiops.workbench.cohortbuilder.chart;
// TODO need to make ap-calls modular to just accept person_ids so apis can be called from notebook
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.QueryBuilder;
import org.pmiops.workbench.cohortbuilder.QueryParameterUtil;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.GenderOrSexType;
import org.springframework.stereotype.Service;

@Service
public class ChartQueryBuilder extends QueryBuilder {


  private static final String SEARCH_PERSON_TABLE = "cb_search_person";

  private static final String DEMO_CHART_INFO_SQL_TEMPLATE =
      "SELECT ${genderOrSex} as name,\n"
          + "race,\n"
          + "CASE ${ageRange1}\n"
          + "${ageRange2}\n"
          + "ELSE '> 65'\n"
          + "END as ageRange,\n"
          + "COUNT(*) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n"
          + "WHERE ";

  private static final String PERSON_IDS_IN_SQL = "${mainTable}.person_id in UNNEST(${personIds}) ";

  private static final String DEMO_CHART_INFO_SQL_GROUP_BY =
      "GROUP BY name, race, ageRange\n" + "ORDER BY name, race, ageRange\n";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
      "SELECT standard_name as name, standard_concept_id as conceptId, COUNT(DISTINCT person_id) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events` cb_review_all_events\n"
          + "WHERE ";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE_INNER_SQL =
      "cb_review_all_events.person_id IN (${innerSql})";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
      "AND domain = ${domain}\n"
          + "AND standard_concept_id != 0 \n"
          + "GROUP BY name, conceptId\n"
          + "ORDER BY count DESC, name ASC\n"
          + "LIMIT ${limit}\n";

  private static final String ETHNICITY_INFO_SQL_TEMPLATE =
      "SELECT ethnicity,\n"
          + "COUNT(*) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n"
          + "WHERE ";

  private static final String ETHNICITY_INFO_SQL_GROUP_BY =
      "GROUP BY ethnicity\n" + "ORDER BY ethnicity\n";

  private static final String ID_SQL_TEMPLATE =
      "SELECT distinct person_id\n FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n WHERE\n";

  private static final String PART_ID = "participantId";
  private static final String DOMAIN_PARAM = "domain";
  private static final String LIMIT = "limit";

  private static final String REVIEW_TABLE = "cb_review_all_events";

  private static final Object[] CHART_DATA_ARGS =
      new Object[] {
        REVIEW_TABLE, REVIEW_TABLE, PART_ID, DOMAIN_PARAM, LIMIT, PART_ID, DOMAIN_PARAM, LIMIT
      };
  private static final String CHART_DATA_TEMPLATE =
      "select distinct a.standard_name as standardName, a.standard_vocabulary as standardVocabulary, "
          + "DATE(a.start_datetime) as startDate, a.age_at_event as ageAtEvent, rnk as rank\n"
          + "from `${projectId}.${dataSetId}.%s` a\n"
          + "join (select standard_code, RANK() OVER (ORDER BY count DESC) AS rnk\n"
          + "from (select standard_code, count(*) as count\n"
          + "from `${projectId}.${dataSetId}.%s`\n"
          + "where person_id = @%s\n"
          + "and domain = @%s\n"
          + "and standard_concept_id != 0\n"
          + "group by standard_code\n"
          + "order by count(*) desc\n"
          + "LIMIT @%s)) b on a.standard_code = b.standard_code\n"
          + "where person_id = @%s\n"
          + "and domain = @%s\n"
          + "and standard_concept_id != 0\n"
          + "and rnk <= @%s\n"
          + "order by rank asc, standardName, startDate\n";

  private static final String COHORT_PIDS_SQL =
      "SELECT distinct person_id \n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n"
          + "WHERE ";
  private static final String AGE_BIN_SIZE_PARAM = "ageBin";
  private static final String TBL_TMP_COHORT_IDS = "tmp_cohort_ids";

  private static final String WITH_TMP_COHORT_IDS_SQL =
      "WITH " + TBL_TMP_COHORT_IDS + " AS\n" + "(%s)";
  private static final String TBL_TMP_DEMO = "tmp_demo";
  private static final String WITH_TMP_DEMO_SQL =
      TBL_TMP_DEMO
          + " AS \n"
          + "(\n"
          + "select person_id,\n"
          + "       gender,\n"
          + "       sex_at_birth,\n"
          + "       race,\n"
          + "       ethnicity,\n"
          + "       age_at_cdr,\n"
          + "       concat(cast(floor(age_at_cdr/@"
          + AGE_BIN_SIZE_PARAM
          + ") * @"
          + AGE_BIN_SIZE_PARAM
          + " as int64),'-',\n"
          + "       cast(((floor(age_at_cdr/@"
          + AGE_BIN_SIZE_PARAM
          + ")+1) * @"
          + AGE_BIN_SIZE_PARAM
          + ") - 1 as int64)) as age_bin \n"
          + " from `${projectId}.${dataSetId}.cb_search_person`\n"
          + " JOIN "
          + TBL_TMP_COHORT_IDS
          + " using (person_id) \n"
          + ")";

  private static final String TBL_TMP_TOP_N = "top_n";

  private static final String WITH_TEMP_TOP_N_SQL =
      TBL_TMP_TOP_N
          + " AS \n"
          + "(SELECT\n"
          + "       standard_name ,\n"
          + "       standard_concept_id ,\n"
          + "       COUNT(DISTINCT person_id) as count,\n"
          + "       row_number() over (order by COUNT(DISTINCT person_id) desc) as concept_rank\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events` \n"
          + "JOIN "
          + TBL_TMP_COHORT_IDS
          + " using (person_id)\n"
          + "WHERE\n"
          + "lower(domain) = lower(@"
          + DOMAIN_PARAM
          + ")\n"
          + "AND standard_concept_id != 0\n"
          + "GROUP BY standard_name, standard_concept_id\n"
          + "ORDER BY count DESC, standard_name ASC\n"
          + "LIMIT @"
          + LIMIT
          + "\n"
          + ")";

  private static final String TBL_TMP_TOP_N_CO_OCCUR = "top_n_co_occur";
  // replace %s,$s with domain_table and domain_table-concept_id column name
  // example condition_occurrence, condition_concept_id
  private static final String WITH_TEMP_TOP_N_CO_OCCUR =
      TBL_TMP_TOP_N_CO_OCCUR
          + " AS\n"
          + "(SELECT\n"
          + "       person_id,\n"
          + "       COUNT(distinct standard_concept_id) as n_concept_co_occur\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events`\n"
          + "JOIN "
          + TBL_TMP_COHORT_IDS
          + " using (person_id)\n"
          + "join "
          + TBL_TMP_TOP_N
          + " using(standard_concept_id)\n"
          + "GROUP BY 1\n"
          + "order by 2 desc\n"
          + ")";
  private static final String NEW_CHART_DEMO_SQL =
      "SELECT gender,\n"
          + "       sex_at_birth as sexAtBirth,\n"
          + "       race,\n"
          + "       ethnicity,\n"
          + "       age_bin as ageBin,\n"
          + "       count(distinct person_id) as count,\n"
          // + "       safe_divide(count(distinct person_id), sum(count(distinct person_id)) over
          // ()) as fract\n"
          + "FROM "
          + TBL_TMP_DEMO
          + " \n"
          + "group by 1,2,3,4,5\n"
          + "order by 1,2,3,4,5";

  private static final String NEW_CHART_DOMAIN_SQL =
      "\n"
          + "SELECT\n"
          + "      gender,\n"
          + "      sex_at_birth as sexAtBirth,\n"
          + "      race,\n"
          + "      ethnicity,\n"
          + "      age_bin as ageBin,\n"
          + "      cb.domain as domain,\n"
          + "      cb.standard_name as conceptName,\n"
          + "      cb.standard_concept_id as conceptId,\n"
          + "      concept_rank as conceptRank,\n"
          + "      n_concept_co_occur as numConceptsCoOccur,\n"
          + "      COUNT(DISTINCT cb.person_id) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events` cb\n"
          + "join "
          + TBL_TMP_TOP_N
          + " using (standard_concept_id)\n"
          + "JOIN "
          + TBL_TMP_DEMO
          + " using (person_id)\n"
          + "JOIN "
          + TBL_TMP_TOP_N_CO_OCCUR
          + " using(person_id)\n"
          + "group by 1,2,3,4,5,6,7,8,9,10\n"
          + "order by 1,2,3,4,5,6,7,8,9,10";

  private static final String TBL_TMP_DEMO_MAP = "tmp_demo_map";
  private static final String WITH_TMP_DEMO_MAP_SQL =
      TBL_TMP_DEMO_MAP
          + " AS \n"
          + "(\n"
          + "select person_id,\n"
          + "       gender,\n"
          + "       sex_at_birth,\n"
          + "       race,\n"
          + "       ethnicity,\n"
          + "       state_of_residence\n"
          + " from `${projectId}.${dataSetId}.cb_search_person`\n"
          + " JOIN "
          + TBL_TMP_COHORT_IDS
          + " using (person_id) \n"
          + ")";

  private static final String NEW_CHART_DEMO_MAP_SQL =
      "SELECT gender,\n"
          + "       sex_at_birth as sexAtBirth,\n"
          + "       race,\n"
          + "       ethnicity,\n"
          + "       state_of_residence as stateCode,\n"
          + "       count(distinct person_id) as count\n"
          + " FROM "
          + TBL_TMP_DEMO_MAP
          + " \n"
          + "group by 1,2,3,4,5\n"
          + "order by 1,2,3,4,5";
  /**
   * Provides counts with demographic info for charts defined by the provided {@link
   * ParticipantCriteria}.
   */
  public QueryJobConfiguration buildDemoChartInfoCounterQuery(
      ParticipantCriteria participantCriteria) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    String sqlTemplate =
        DEMO_CHART_INFO_SQL_TEMPLATE
            .replace("${genderOrSex}", participantCriteria.getGenderOrSexType().toString())
            .replace("${ageRange1}", getAgeRangeSql(18, 44, participantCriteria.getAgeType()))
            .replace("${ageRange2}", getAgeRangeSql(45, 64, participantCriteria.getAgeType()));
    StringBuilder queryBuilder = new StringBuilder(sqlTemplate);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(
        participantCriteria.getCohortDefinition().getDataFilters(), queryBuilder, params);
    queryBuilder.append(DEMO_CHART_INFO_SQL_GROUP_BY);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildDemoChartInfoCounterQuery(Set<Long> participantIds) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    String sqlTemplate =
        DEMO_CHART_INFO_SQL_TEMPLATE
            .replace("${genderOrSex}", GenderOrSexType.GENDER.toString())
            .replace("${ageRange1}", getAgeRangeSql(18, 44, AgeType.AGE))
            .replace("${ageRange2}", getAgeRangeSql(45, 64, AgeType.AGE));
    StringBuilder queryBuilder = new StringBuilder(sqlTemplate);

    addParticipantIds(params, participantIds, queryBuilder, SEARCH_PERSON_TABLE);

    queryBuilder.append(DEMO_CHART_INFO_SQL_GROUP_BY);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  /**
   * Provides counts with ethnicity info for cohort defined by the provided {@link
   * ParticipantCriteria}.
   */
  public QueryJobConfiguration buildEthnicityInfoCounterQuery(
      ParticipantCriteria participantCriteria) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(ETHNICITY_INFO_SQL_TEMPLATE);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(
        participantCriteria.getCohortDefinition().getDataFilters(), queryBuilder, params);
    queryBuilder.append(ETHNICITY_INFO_SQL_GROUP_BY);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildChartDataQuery(
      Long participantId, Domain domain, Integer limit) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(PART_ID, QueryParameterValue.int64(participantId));
    params.put(DOMAIN_PARAM, QueryParameterValue.string(domain.name()));
    params.put(LIMIT, QueryParameterValue.int64(limit));
    return QueryJobConfiguration.newBuilder(String.format(CHART_DATA_TEMPLATE, CHART_DATA_ARGS))
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }
  /**
   * Provides counts with domain info for charts defined by the provided {@link
   * ParticipantCriteria}.
   */
  public QueryJobConfiguration buildDomainChartInfoCounterQuery(
      ParticipantCriteria participantCriteria, Domain domain, int chartLimit) {
    StringBuilder queryBuilder = new StringBuilder(ID_SQL_TEMPLATE);
    Map<String, QueryParameterValue> params = new HashMap<>();
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(
        participantCriteria.getCohortDefinition().getDataFilters(), queryBuilder, params);
    String searchPersonSql = queryBuilder.toString();

    queryBuilder = new StringBuilder(DOMAIN_CHART_INFO_SQL_TEMPLATE);
    queryBuilder.append(
        DOMAIN_CHART_INFO_SQL_TEMPLATE_INNER_SQL.replace("${innerSql}", searchPersonSql));
    String paramName =
        QueryParameterUtil.addQueryParameterValue(
            params, QueryParameterValue.string(domain.name()));
    String endSqlTemplate =
        DOMAIN_CHART_INFO_SQL_GROUP_BY
            .replace("${limit}", Integer.toString(chartLimit))
            .replace("${tableId}", "standard_concept_id")
            .replace("${domain}", paramName);
    queryBuilder.append(endSqlTemplate);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildDomainChartInfoCounterQuery(
      Set<Long> participantIds, Domain domain, int chartLimit) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(DOMAIN_CHART_INFO_SQL_TEMPLATE);

    addParticipantIds(params, participantIds, queryBuilder, REVIEW_TABLE);

    String paramName2 =
        QueryParameterUtil.addQueryParameterValue(
            params, QueryParameterValue.string(domain.name()));
    String endSqlTemplate =
        DOMAIN_CHART_INFO_SQL_GROUP_BY
            .replace("${limit}", Integer.toString(chartLimit))
            .replace("${domain}", paramName2);
    queryBuilder.append(endSqlTemplate);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildNewChartDataQuery(
      ParticipantCriteria participantCriteria, Domain domain) {
    final int ageBin = 10; // maybe get it from input?
    final int limit = 10; // limit to top 10
    Map<String, QueryParameterValue> params = new HashMap<>();
    // 1. build cohort pids SQL
    StringBuilder personIdsSqlBuilder = new StringBuilder(COHORT_PIDS_SQL);
    if (participantCriteria.getCohortDefinition() == null) {
      // for whole CDR remove the 'WHERE' clause alternately add ' 1 = 1 \n'
      personIdsSqlBuilder.append(" 1 = 1 \n");
    } else {
      addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, personIdsSqlBuilder, params);
      addDataFilters(
          participantCriteria.getCohortDefinition().getDataFilters(), personIdsSqlBuilder, params);
    }
    // 2.build temp demographics table from cohort_pids sql - no grouping is done
    String withPersonIdsDemographicsSql =
        new StringBuilder(String.format(WITH_TMP_COHORT_IDS_SQL, personIdsSqlBuilder))
            .append(",\n")
            .append(WITH_TMP_DEMO_SQL)
            .toString();
    params.put(AGE_BIN_SIZE_PARAM, QueryParameterValue.int64(ageBin));
    // create final SQL query
    StringBuilder chartSql = new StringBuilder();
    if (domain == null) {
      // 3. For demographics chart: append sql for grouping using tables in WITH
      chartSql.append(withPersonIdsDemographicsSql).append("\n").append(NEW_CHART_DEMO_SQL);
    } else {
      // if domain then to withPersonIdsDemographicsSql append as temp tables:
      chartSql.append(withPersonIdsDemographicsSql);
      // 3. sql for top 10 concepts for domain
      chartSql.append(",\n").append(WITH_TEMP_TOP_N_SQL);
      params.put(DOMAIN_PARAM, QueryParameterValue.string(domain.toString()));
      params.put(LIMIT, QueryParameterValue.int64(limit));
      // 3a. if domain is condition: sql for count of n-distinct-top10-concepts per person-id
      chartSql.append(",\n").append(WITH_TEMP_TOP_N_CO_OCCUR);
      // 4. append grouping sql for chart
      chartSql.append("\n").append(NEW_CHART_DOMAIN_SQL);
    }

    String temp = domain != null ? domain.toString() : "Demographics";
    temp += participantCriteria.getCohortDefinition() == null ? " (for CDR)" : "(for Cohort)";
    System.out.println("*******Chart SQL and params******: " + temp);
    System.out.println(chartSql);
    System.out.println("*******Chart SQL - params******: " + temp);
    System.out.println(params);
    System.out.println("*******Chart SQL and params******\n\n" + temp);

    return QueryJobConfiguration.newBuilder(chartSql.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public QueryJobConfiguration buildNewChartDataMapQuery(ParticipantCriteria participantCriteria) {

    Map<String, QueryParameterValue> params = new HashMap<>();
    // 1. build cohort pids SQL
    StringBuilder personIdsSqlBuilder = new StringBuilder(COHORT_PIDS_SQL);
    if (participantCriteria.getCohortDefinition() == null) {
      // for whole CDR remove the 'WHERE' clause alternately add ' 1 = 1 \n'
      personIdsSqlBuilder.append(" 1 = 1 \n");
    } else {
      addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, personIdsSqlBuilder, params);
      addDataFilters(
          participantCriteria.getCohortDefinition().getDataFilters(), personIdsSqlBuilder, params);
    }
    // 2.build temp demographics table from cohort_pids sql - no grouping is done
    String withPersonIdsDemographicsSql =
        new StringBuilder(String.format(WITH_TMP_COHORT_IDS_SQL, personIdsSqlBuilder))
            .append(",\n")
            .append(WITH_TMP_DEMO_MAP_SQL)
            .toString();
    // create final SQL query
    StringBuilder chartSql = new StringBuilder();
    // 3. For demographics chart: append sql for grouping using tables in WITH
    chartSql.append(withPersonIdsDemographicsSql).append("\n").append(NEW_CHART_DEMO_MAP_SQL);

    System.out.println("*******MAP Chart SQL and params******: ");
    System.out.println(chartSql);
    System.out.println("*******MAP Chart SQL - params******: ");
    System.out.println(params);
    System.out.println("*******MAP Chart SQL and params******\n\n");

    return QueryJobConfiguration.newBuilder(chartSql.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  private static void addParticipantIds(
      Map<String, QueryParameterValue> params,
      Set<Long> participantIds,
      StringBuilder queryBuilder,
      String mainTable) {
    String paramName =
        QueryParameterUtil.addQueryParameterValue(
            params, QueryParameterValue.array(participantIds.toArray(new Long[0]), Long.class));
    queryBuilder.append(
        PERSON_IDS_IN_SQL.replace("${mainTable}", mainTable).replace("${personIds}", paramName));
  }

  /**
   * Helper method to build sql snippet.
   *
   * @param lo - lower bound of the age range
   * @param hi - upper bound of the age range
   * @param ageType - age type enum
   * @return
   */
  private static String getAgeRangeSql(int lo, int hi, AgeType ageType) {
    String ageSql =
        AgeType.AGE.equals(ageType)
            ? "DATE_DIFF(CURRENT_DATE,dob, YEAR) - IF(EXTRACT(MONTH FROM dob)*100 + EXTRACT(DAY FROM dob) > EXTRACT(MONTH FROM CURRENT_DATE)*100 + EXTRACT(DAY FROM CURRENT_DATE),1,0)"
            : ageType.toString();
    return "WHEN " + ageSql + " >= " + lo + " AND " + ageSql + " <= " + hi + " THEN '" + lo + "-"
        + hi + "'";
  }
}
