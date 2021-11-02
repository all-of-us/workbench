package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortQueryBuilder {
  private static final String REVIEW_TABLE = "cb_review_all_events";
  private static final String SEARCH_PERSON_TABLE = "cb_search_person";
  private static final String PERSON_TABLE = "person";

  private static final String COUNT_SQL_TEMPLATE =
      "SELECT COUNT(*) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n"
          + "WHERE ";

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
      "GROUP BY name, race, ageRange\n" + "ORDER BY name, race, ageRange\n";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
      "SELECT standard_name as name, standard_concept_id as conceptId, COUNT(DISTINCT person_id) as count\n"
          + "FROM `${projectId}.${dataSetId}.cb_review_all_events` cb_review_all_events\n"
          + "WHERE cb_review_all_events.person_id IN (${innerSql})";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
      "AND domain = ${domain}\n"
          + "AND standard_concept_id != 0 \n"
          + "GROUP BY name, conceptId\n"
          + "ORDER BY count DESC, name ASC\n"
          + "LIMIT ${limit}\n";

  private static final String RANDOM_SQL_TEMPLATE =
      "SELECT RAND() as x, person.person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, sex_at_birth_concept_id, birth_datetime, CASE WHEN death.person_id IS NULL THEN false ELSE true END as deceased\n"
          + "FROM `${projectId}.${dataSetId}.person` person\n"
          + "LEFT JOIN `${projectId}.${dataSetId}.death` death ON (person.person_id = death.person_id)\n"
          + "WHERE person.person_id IN (${innerSql})";

  private static final String RANDOM_SQL_ORDER_BY = "ORDER BY x\nLIMIT";

  private static final String OFFSET_SUFFIX = " OFFSET ";

  private static final String ID_SQL_TEMPLATE =
      "SELECT person_id\n FROM `${projectId}.${dataSetId}.cb_search_person` cb_search_person\n WHERE\n";

  private static final String UNION_TEMPLATE = "UNION ALL\n";

  private static final String INCLUDE_SQL_TEMPLATE = "${mainTable}.person_id IN (${includeSql})\n";

  private static final String PERSON_ID_WHITELIST_PARAM = "person_id_whitelist";
  private static final String PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist";

  private static final String PERSON_ID_WHITELIST_TEMPLATE =
      "${mainTable}.person_id IN unnest(@" + PERSON_ID_WHITELIST_PARAM + ")\n";
  private static final String PERSON_ID_BLACKLIST_TEMPLATE =
      "${mainTable}.person_id NOT IN unnest(@" + PERSON_ID_BLACKLIST_PARAM + ")\n";

  private static final String EXCLUDE_SQL_TEMPLATE =
      "${mainTable}.person_id NOT IN\n" + "(${excludeSql})\n";

  private CBCriteriaDao cbCriteriaDao;
  private static final Logger log = Logger.getLogger(CohortQueryBuilder.class.getName());

  @Autowired
  public CohortQueryBuilder(CBCriteriaDao cbCriteriaDao) {
    this.cbCriteriaDao = cbCriteriaDao;
  }

  /** Provides counts of unique subjects defined by the provided {@link ParticipantCriteria}. */
  public QueryJobConfiguration buildParticipantCounterQuery(
      ParticipantCriteria participantCriteria) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(COUNT_SQL_TEMPLATE);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(participantCriteria.getSearchRequest().getDataFilters(), queryBuilder, params);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

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

  public QueryJobConfiguration buildRandomParticipantQuery(
      ParticipantCriteria participantCriteria, long resultSize, long offset) {
    String endSql = RANDOM_SQL_ORDER_BY + " " + resultSize;
    if (offset > 0) {
      endSql += OFFSET_SUFFIX + offset;
    }
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder = new StringBuilder(ID_SQL_TEMPLATE);
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    addDataFilters(participantCriteria.getSearchRequest().getDataFilters(), queryBuilder, params);
    String searchPersonSql = queryBuilder.toString();

    queryBuilder = new StringBuilder(RANDOM_SQL_TEMPLATE.replace("${innerSql}", searchPersonSql));
    queryBuilder.append(endSql);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  // This method supports the Data Set Builder, for further details see:
  // https://docs.google.com/document/d/1-wzSCHDM_LSaBRARyLFbsTGcBaKi5giRs-eDmaMBr0Y/edit
  public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria) {
    return buildUnionedParticipantIdQuery(ImmutableList.of(participantCriteria));
  }

  public QueryJobConfiguration buildUnionedParticipantIdQuery(
      List<ParticipantCriteria> participantCriteriaList) {
    List<String> queries = new ArrayList<>();
    Map<String, QueryParameterValue> params = new HashMap<>();

    for (ParticipantCriteria participantCriteria : participantCriteriaList) {
      StringBuilder queryBuilder = new StringBuilder(ID_SQL_TEMPLATE);
      addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
      queries.add(queryBuilder.toString());
    }
    return QueryJobConfiguration.newBuilder(String.join(UNION_TEMPLATE, queries))
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public void addWhereClause(
      ParticipantCriteria participantCriteria,
      String mainTable,
      StringBuilder queryBuilder,
      Map<String, QueryParameterValue> params) {
    SearchRequest request = participantCriteria.getSearchRequest();
    if (request == null) {
      queryBuilder.append(PERSON_ID_WHITELIST_TEMPLATE.replace("${mainTable}", mainTable));
      params.put(
          PERSON_ID_WHITELIST_PARAM,
          QueryParameterValue.array(
              participantCriteria.getParticipantIdsToInclude().toArray(new Long[0]), Long.class));
    } else {
      if (request.getIncludes().isEmpty() && request.getExcludes().isEmpty()) {
        log.log(
            Level.WARNING, "Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
        throw new BadRequestException(
            "Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
      }

      // build query for included search groups
      StringJoiner joiner = buildQuery(request.getIncludes(), mainTable, params, false);

      // if includes is empty then don't add the excludes clause
      if (joiner.toString().isEmpty()) {
        joiner.merge(buildQuery(request.getExcludes(), mainTable, params, false));
      } else {
        joiner.merge(buildQuery(request.getExcludes(), mainTable, params, true));
      }
      Set<Long> participantIdsToExclude = participantCriteria.getParticipantIdsToExclude();
      if (!participantIdsToExclude.isEmpty()) {
        joiner.add(PERSON_ID_BLACKLIST_TEMPLATE.replace("${mainTable}", mainTable));
        params.put(
            PERSON_ID_BLACKLIST_PARAM,
            QueryParameterValue.array(participantIdsToExclude.toArray(new Long[0]), Long.class));
      }
      queryBuilder.append(joiner.toString());
    }
  }

  private StringJoiner buildQuery(
      List<SearchGroup> groups,
      String mainTable,
      Map<String, QueryParameterValue> params,
      Boolean excludeSQL) {
    StringJoiner joiner = new StringJoiner("and ");
    List<String> queryParts = new ArrayList<>();
    for (SearchGroup includeGroup : groups) {
      SearchGroupItemQueryBuilder.buildQuery(params, queryParts, includeGroup);

      if (excludeSQL) {
        joiner.add(
            EXCLUDE_SQL_TEMPLATE
                .replace("${mainTable}", mainTable)
                .replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts)));
      } else {
        joiner.add(
            INCLUDE_SQL_TEMPLATE
                .replace("${mainTable}", mainTable)
                .replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts)));
      }
      queryParts = new ArrayList<>();
    }
    return joiner;
  }

  private void addDataFilters(
      List<String> dataFilters,
      StringBuilder queryBuilder,
      Map<String, QueryParameterValue> params) {
    dataFilters.stream()
        .forEach(
            df -> {
              String paramName =
                  QueryParameterUtil.addQueryParameterValue(params, QueryParameterValue.int64(1));
              queryBuilder.append(" AND " + df + " = " + paramName + "\n");
            });
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
