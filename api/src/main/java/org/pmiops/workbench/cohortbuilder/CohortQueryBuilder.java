package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cohortbuilder.util.CriteriaLookupUtil;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortQueryBuilder {
  private static final String REVIEW_TABLE = "cb_review_all_events";
  private static final String COUNT_SQL_TEMPLATE =
      "select count(*) as count\n"
          + "from `${projectId}.${dataSetId}.${table}` ${table}\n"
          + "where\n";

  private static final String SEARCH_PERSON_TABLE = "cb_search_person";
  private static final String PERSON_TABLE = "person";

  private static final String DEMO_CHART_INFO_SQL_TEMPLATE =
      "select gender, \n"
          + "race, \n"
          + "case "
          + getAgeRangeSql(18, 44)
          + "\n"
          + getAgeRangeSql(45, 64)
          + "\n"
          + "else '> 65'\n"
          + "end as ageRange,\n"
          + "count(*) as count\n"
          + "from `${projectId}.${dataSetId}.${table}` ${table}\n"
          + "where\n";

  private static final String DEMO_CHART_INFO_SQL_GROUP_BY =
      "group by gender, race, ageRange\n" + "order by gender, race, ageRange\n";

  private static final String DOMAIN_CHART_INFO_SQL_TEMPLATE =
      "select standard_name as name, standard_concept_id as conceptId, count(distinct person_id) as count\n"
          + "from `${projectId}.${dataSetId}."
          + REVIEW_TABLE
          + "` "
          + REVIEW_TABLE
          + "\n"
          + "where\n";

  private static final String DOMAIN_CHART_INFO_SQL_GROUP_BY =
      "and domain = '${domain}'\n"
          + "and standard_concept_id != 0 \n"
          + "group by name, conceptId\n"
          + "order by count desc, name asc\n"
          + "limit ${limit}\n";

  private static final String RANDOM_SQL_TEMPLATE =
      "select rand() as x, ${table}.person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime, case when death.person_id is null then false else true end as deceased\n"
          + "from `${projectId}.${dataSetId}.${table}` ${table}\n"
          + "left join `${projectId}.${dataSetId}.death` death on (${table}.person_id = death.person_id)\n"
          + "where\n";

  private static final String RANDOM_SQL_ORDER_BY = "order by x\nlimit";

  private static final String OFFSET_SUFFIX = " offset ";

  private static final String ID_SQL_TEMPLATE =
      "select person_id\n" + "from `${projectId}.${dataSetId}.${table}` ${table}\n" + "where\n";

  private static final String UNION_TEMPLATE = "union all\n";

  private static final String INCLUDE_SQL_TEMPLATE = "${mainTable}.person_id in (${includeSql})\n";

  private static final String PERSON_ID_WHITELIST_PARAM = "person_id_whitelist";
  private static final String PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist";

  private static final String PERSON_ID_WHITELIST_TEMPLATE =
      "${mainTable}.person_id in unnest(@" + PERSON_ID_WHITELIST_PARAM + ")\n";
  private static final String PERSON_ID_BLACKLIST_TEMPLATE =
      "${mainTable}.person_id not in unnest(@" + PERSON_ID_BLACKLIST_PARAM + ")\n";

  private static final String EXCLUDE_SQL_TEMPLATE =
      "${mainTable}.person_id not in\n" + "(${excludeSql})\n";

  private CBCriteriaDao cbCriteriaDao;
  private static final Logger log = Logger.getLogger(CohortQueryBuilder.class.getName());

  @Autowired
  public CohortQueryBuilder(CBCriteriaDao cbCriteriaDao) {
    this.cbCriteriaDao = cbCriteriaDao;
  }

  /** Provides counts of unique subjects defined by the provided {@link ParticipantCriteria}. */
  public QueryJobConfiguration buildParticipantCounterQuery(
      ParticipantCriteria participantCriteria) {
    String sqlTemplate = COUNT_SQL_TEMPLATE.replace("${table}", SEARCH_PERSON_TABLE);
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder =
        new StringBuilder(sqlTemplate.replace("${mainTable}", SEARCH_PERSON_TABLE));
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);

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
    String sqlTemplate = DEMO_CHART_INFO_SQL_TEMPLATE.replace("${table}", SEARCH_PERSON_TABLE);
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder =
        new StringBuilder(sqlTemplate.replace("${mainTable}", SEARCH_PERSON_TABLE));
    addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params);
    queryBuilder.append(DEMO_CHART_INFO_SQL_GROUP_BY.replace("${mainTable}", SEARCH_PERSON_TABLE));

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
      ParticipantCriteria participantCriteria, DomainType domainType, int chartLimit) {
    String endSqlTemplate =
        DOMAIN_CHART_INFO_SQL_GROUP_BY
            .replace("${limit}", Integer.toString(chartLimit))
            .replace("${tableId}", "standard_concept_id")
            .replace("${domain}", domainType.name());
    StringBuilder queryBuilder =
        new StringBuilder(DOMAIN_CHART_INFO_SQL_TEMPLATE.replace("${mainTable}", REVIEW_TABLE));
    Map<String, QueryParameterValue> params = new HashMap<>();
    addWhereClause(participantCriteria, REVIEW_TABLE, queryBuilder, params);
    queryBuilder.append(endSqlTemplate.replace("${mainTable}", REVIEW_TABLE));

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
    String sqlTemplate = RANDOM_SQL_TEMPLATE.replace("${table}", PERSON_TABLE);
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder =
        new StringBuilder(sqlTemplate.replace("${mainTable}", PERSON_TABLE));
    addWhereClause(participantCriteria, PERSON_TABLE, queryBuilder, params);
    queryBuilder.append(endSql.replace("${mainTable}", PERSON_TABLE));

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  // implemented for use with the Data Set Builder. Please remove if this does not become the
  // preferred solution
  // https://docs.google.com/document/d/1-wzSCHDM_LSaBRARyLFbsTGcBaKi5giRs-eDmaMBr0Y/edit#
  public QueryJobConfiguration buildParticipantIdQuery(ParticipantCriteria participantCriteria) {
    String sqlTemplate = ID_SQL_TEMPLATE.replace("${table}", PERSON_TABLE);
    Map<String, QueryParameterValue> params = new HashMap<>();
    StringBuilder queryBuilder =
        new StringBuilder(sqlTemplate.replace("${mainTable}", PERSON_TABLE));
    addWhereClause(participantCriteria, PERSON_TABLE, queryBuilder, params);

    return QueryJobConfiguration.newBuilder(queryBuilder.toString())
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

      // produces a map of matching child concept ids.
      Map<SearchParameter, Set<Long>> criteriaLookup = new HashMap<>();
      try {
        criteriaLookup = new CriteriaLookupUtil(cbCriteriaDao).buildCriteriaLookupMap(request);
      } catch (IllegalArgumentException ex) {
        log.log(Level.WARNING, "Unable to lookup criteria children", ex);
        throw new BadRequestException("Bad Request: " + ex.getMessage());
      }

      // build query for included search groups
      StringJoiner joiner =
          buildQuery(criteriaLookup, request.getIncludes(), mainTable, params, false);

      // if includes is empty then don't add the excludes clause
      if (joiner.toString().isEmpty()) {
        joiner.merge(buildQuery(criteriaLookup, request.getExcludes(), mainTable, params, false));
      } else {
        joiner.merge(buildQuery(criteriaLookup, request.getExcludes(), mainTable, params, true));
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
      Map<SearchParameter, Set<Long>> criteriaLookup,
      List<SearchGroup> groups,
      String mainTable,
      Map<String, QueryParameterValue> params,
      Boolean excludeSQL) {
    StringJoiner joiner = new StringJoiner("and ");
    List<String> queryParts = new ArrayList<>();
    for (SearchGroup includeGroup : groups) {
      SearchGroupItemQueryBuilder.buildQuery(criteriaLookup, params, queryParts, includeGroup);

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

  /**
   * Helper method to build sql snippet.
   *
   * @param lo - lower bound of the age range
   * @param hi - upper bound of the age range
   * @return
   */
  private static String getAgeRangeSql(int lo, int hi) {
    return "when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, "
        + SEARCH_PERSON_TABLE
        + ".dob, MONTH)/12) as INT64) >= "
        + lo
        + " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, "
        + SEARCH_PERSON_TABLE
        + ".dob, MONTH)/12) as INT64) <= "
        + hi
        + " then '"
        + lo
        + "-"
        + hi
        + "'";
  }
}
