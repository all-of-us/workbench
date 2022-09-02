package org.pmiops.workbench.cohortbuilder.chart;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.HashMap;
import java.util.Map;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.QueryBuilder;
import org.pmiops.workbench.cohortbuilder.QueryParameterUtil;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.Domain;
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

  private static final String DEMO_CHART_INFO_SQL_GROUP_BY =
      " GROUP BY name, race, ageRange\n" + "ORDER BY name, race, ageRange\n";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
      "SELECT standard_name as name, standard_concept_id as conceptId, COUNT(DISTINCT person_id) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events` cb_review_all_events\n"
          + "WHERE cb_review_all_events.person_id IN (${innerSql})";

  private static final String PERSON_ID_IN_SQL = " AND person_id in (${personIds}) ";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
      " AND domain = ${domain}\n"
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
      " GROUP BY ethnicity\n" + "ORDER BY ethnicity\n";

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
    addDataFilters(participantCriteria.getSearchRequest().getDataFilters(), queryBuilder, params);
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
    addDataFilters(participantCriteria.getSearchRequest().getDataFilters(), queryBuilder, params);
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
    addDataFilters(participantCriteria.getSearchRequest().getDataFilters(), queryBuilder, params);
    String searchPersonSql = queryBuilder.toString();
    queryBuilder =
        new StringBuilder(DOMAIN_CHART_INFO_SQL_TEMPLATE.replace("${innerSql}", searchPersonSql));
    String paramName =
        QueryParameterUtil.addQueryParameterValue(
            params, QueryParameterValue.string(domain.name()));

    if (!participantCriteria.getParticipantIdsToInclude().isEmpty()) {
      queryBuilder.append(
          PERSON_ID_IN_SQL.replace(
              "${personIds}", participantCriteria.getParticipantIdsToInclude().toString()));
    }

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
