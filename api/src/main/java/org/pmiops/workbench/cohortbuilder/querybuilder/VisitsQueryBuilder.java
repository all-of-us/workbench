package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisitsQueryBuilder extends AbstractQueryBuilder {

  private static final String VISIT_SQL_TEMPLATE =
    "select person_id\n" +
      "from `${projectId}.${dataSetId}.visit_occurrence` v\n";


  private static final String VISIT_CHILD_CLAUSE_TEMPLATE =
    "where v.visit_concept_id = ${visitConceptId}\n";

  private static final String VISIT_PARENT_CLAUSE_TEMPLATE =
    "where v.visit_concept_id in (\n" +
      "select descendant_concept_id\n" +
      "from `${projectId}.${dataSetId}.concept_ancestor` a\n" +
      "where a.ancestor_concept_id = ${parentId})\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String finalSql = "";

    for (SearchParameter parameter : parameters.getParameters()) {
      String namedParameter = "visit" + getUniqueNamedParameterPostfix();
      if (parameter.getGroup()) {
        queryParams.put(namedParameter, QueryParameterValue.int64(parameter.getConceptId()));
        finalSql = VISIT_SQL_TEMPLATE +
          VISIT_PARENT_CLAUSE_TEMPLATE.replace("${parentId}", "@" + namedParameter);

      } else {
        queryParams.put(namedParameter, QueryParameterValue.int64(parameter.getConceptId()));
        finalSql = VISIT_SQL_TEMPLATE +
          VISIT_CHILD_CLAUSE_TEMPLATE.replace("${visitConceptId}", "@" + namedParameter);
      }
    }

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
