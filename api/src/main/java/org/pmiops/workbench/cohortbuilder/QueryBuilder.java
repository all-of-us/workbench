package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.SearchGroup;

public class QueryBuilder {
  private static final String INCLUDE_SQL_TEMPLATE = "${mainTable}.person_id IN (${includeSql})\n";
  private static final String PERSON_ID_WHITELIST_PARAM = "person_id_whitelist";
  private static final String PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist";
  private static final String PERSON_ID_WHITELIST_TEMPLATE =
      "${mainTable}.person_id IN unnest(@" + PERSON_ID_WHITELIST_PARAM + ")\n";
  private static final String PERSON_ID_BLACKLIST_TEMPLATE =
      "${mainTable}.person_id NOT IN unnest(@" + PERSON_ID_BLACKLIST_PARAM + ")\n";
  private static final String EXCLUDE_SQL_TEMPLATE =
      "${mainTable}.person_id NOT IN\n" + "(${excludeSql})\n";
  private static final String UNION_TEMPLATE = "UNION DISTINCT\n";
  private static final Logger log = Logger.getLogger(QueryBuilder.class.getName());

  public void addWhereClause(
      ParticipantCriteria participantCriteria,
      String mainTable,
      StringBuilder queryBuilder,
      Map<String, QueryParameterValue> params) {
    CohortDefinition cohortDefinition = participantCriteria.getCohortDefinition();
    if (cohortDefinition == null) {
      queryBuilder.append(PERSON_ID_WHITELIST_TEMPLATE.replace("${mainTable}", mainTable));
      params.put(
          PERSON_ID_WHITELIST_PARAM,
          QueryParameterValue.array(
              participantCriteria.getParticipantIdsToInclude().toArray(new Long[0]), Long.class));
    } else {
      if (cohortDefinition.getIncludes().isEmpty() && cohortDefinition.getExcludes().isEmpty()) {
        log.log(
            Level.WARNING, "Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
        throw new BadRequestException(
            "Invalid SearchRequest: includes[] and excludes[] cannot both be empty");
      }

      // build query for included search groups
      StringJoiner joiner = buildQuery(cohortDefinition.getIncludes(), mainTable, params, false);

      // if includes is empty then don't add the excludes clause
      if (joiner.toString().isEmpty()) {
        joiner.merge(buildQuery(cohortDefinition.getExcludes(), mainTable, params, false));
      } else {
        joiner.merge(buildQuery(cohortDefinition.getExcludes(), mainTable, params, true));
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

  protected void addDataFilters(
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

  private StringJoiner buildQuery(
      List<SearchGroup> groups,
      String mainTable,
      Map<String, QueryParameterValue> params,
      Boolean excludeSQL) {
    StringJoiner joiner = new StringJoiner("AND ");
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
}
