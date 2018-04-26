package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.pmiops.workbench.cohortbuilder.querybuilder.QueryParameters;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortQueryBuilder {

  private static final String UNION_TEMPLATE = "union all\n";

  private static final String INCLUDE_SQL_TEMPLATE = "${mainTable}.person_id in (${includeSql})\n";

  private static final String PERSON_ID_WHITELIST_PARAM = "person_id_whitelist";
  private static final String PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist";

  private static final String PERSON_ID_WHITELIST_TEMPLATE = "${mainTable}.person_id in unnest(@" +
      PERSON_ID_WHITELIST_PARAM + ")\n";
  private static final String PERSON_ID_BLACKLIST_TEMPLATE = "${mainTable}.person_id not in unnest(@" +
      PERSON_ID_BLACKLIST_PARAM + ")\n";

  private static final String EXCLUDE_SQL_TEMPLATE =
      "not exists\n" +
          "(select 'x' from\n" +
          "(${excludeSql})\n" +
          "x where x.person_id = ${mainTable}.person_id)\n";

  private final DomainLookupService domainLookupService;

  @Autowired
  public CohortQueryBuilder(DomainLookupService domainLookupService) {
    this.domainLookupService = domainLookupService;
  }

  public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
      String sqlTemplate, String endSql, String mainTable,
      Map<String, QueryParameterValue> params) {
    StringBuilder queryBuilder = new StringBuilder(sqlTemplate.replace("${mainTable}", mainTable));
    addWhereClause(participantCriteria, mainTable, queryBuilder, params);
    queryBuilder.append(endSql.replace("${mainTable}", mainTable));

    return QueryJobConfiguration
        .newBuilder(queryBuilder.toString())
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public void addWhereClause(ParticipantCriteria participantCriteria, String mainTable,
      StringBuilder queryBuilder, Map<String, QueryParameterValue> params) {
    SearchRequest request = participantCriteria.getSearchRequest();
    if (request == null) {
      queryBuilder.append(PERSON_ID_WHITELIST_TEMPLATE.replace("${mainTable}", mainTable));
      params.put(PERSON_ID_WHITELIST_PARAM, QueryParameterValue.array(
          participantCriteria.getParticipantIdsToInclude().toArray(new Long[0]), Long.class));
    } else {
      domainLookupService.findCodesForEmptyDomains(request.getIncludes());
      domainLookupService.findCodesForEmptyDomains(request.getExcludes());

      if (request.getIncludes().isEmpty() && request.getExcludes().isEmpty()) {
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
        params.put(PERSON_ID_BLACKLIST_PARAM, QueryParameterValue.array(
            participantIdsToExclude.toArray(new Long[0]), Long.class));
      }
      queryBuilder.append(joiner.toString());
    }
  }

  private StringJoiner buildQuery(List<SearchGroup> groups, String mainTable,
      Map<String, QueryParameterValue> params, Boolean excludeSQL) {
    StringJoiner joiner = new StringJoiner("and ");
    List<String> queryParts = new ArrayList<>();
    for (SearchGroup includeGroup : groups) {
      for (SearchGroupItem includeItem : includeGroup.getItems()) {
        QueryJobConfiguration queryRequest = QueryBuilderFactory
            .getQueryBuilder(FactoryKey.getType(includeItem.getType()))
            .buildQueryJobConfig(new QueryParameters()
                .type(includeItem.getType())
                .parameters(includeItem.getSearchParameters())
                .modifiers(includeItem.getModifiers()));
        params.putAll(queryRequest.getNamedParameters());
        queryParts.add(queryRequest.getQuery());
      }
      if (excludeSQL) {
        joiner.add(EXCLUDE_SQL_TEMPLATE.replace("${mainTable}", mainTable)
            .replace("${excludeSql}", String.join(UNION_TEMPLATE, queryParts)));
      } else {
        joiner.add(INCLUDE_SQL_TEMPLATE.replace("${mainTable}", mainTable)
            .replace("${includeSql}", String.join(UNION_TEMPLATE, queryParts)));
      }
      queryParts = new ArrayList<>();
    }
    return joiner;
  }
}
