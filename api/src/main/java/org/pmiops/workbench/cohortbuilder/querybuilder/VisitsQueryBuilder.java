package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VisitsQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for visit criteria types.
 */
@Service
public class VisitsQueryBuilder extends AbstractQueryBuilder {

  private static final String VISIT_SELECT_CLAUSE_TEMPLATE =
    "select person_id \n" +
      "from `${projectId}.${dataSetId}.visit_occurrence` a\n";

  private static final String VISIT_CHILD_CLAUSE_TEMPLATE =
    "where a.visit_concept_id in unnest(${visitConceptIds})\n";

  private static final String VISIT_PARENT_CLAUSE_TEMPLATE =
    "where a.visit_concept_id in (\n" +
      "select descendant_concept_id\n" +
      "from `${projectId}.${dataSetId}.concept_ancestor` \n" +
      "where ancestor_concept_id in unnest(${parentIds}))\n";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters inputParameters) {
    List<String> queryParts = new ArrayList<>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();

    List<Long> parentList = new ArrayList<>();
    List<Long> childList = new ArrayList<>();
    for (SearchParameter parameter : inputParameters.getParameters()) {
      if (parameter.getConceptId() == null) {
        throw new BadRequestException("Please provide a valid concept Id");
      }
      if (parameter.getGroup()) {
        parentList.add(parameter.getConceptId());
      } else {
        childList.add(parameter.getConceptId());
      }
    }

    // Collect all parent type queries to run them together
    if (!parentList.isEmpty()) {
      String namedParameter = "visit" + getUniqueNamedParameterPostfix();
      queryParams.put(namedParameter, QueryParameterValue.array(parentList.toArray(new Long[0]), Long.class));
      String parentSql = VISIT_SELECT_CLAUSE_TEMPLATE +
        VISIT_PARENT_CLAUSE_TEMPLATE.replace("${parentIds}", "@" + namedParameter);
      queryParts.add(parentSql);
    }

    // Collect all child type queries to run them together
    if (!childList.isEmpty()) {
      String namedParameter = "visit" + getUniqueNamedParameterPostfix();
      queryParams.put(namedParameter, QueryParameterValue.array(childList.toArray(new Long[0]), Long.class));
      String childSql = VISIT_SELECT_CLAUSE_TEMPLATE +
        VISIT_CHILD_CLAUSE_TEMPLATE.replace("${visitConceptIds}", "@" + namedParameter);
      queryParts.add(childSql);
    }

    // Combine the parent and child queries, or just use the one
    String finalSql = queryParts.size() > 1 ?
      String.join(UNION_TEMPLATE, queryParts) : queryParts.get(0);

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.VISIT;
  }
}
