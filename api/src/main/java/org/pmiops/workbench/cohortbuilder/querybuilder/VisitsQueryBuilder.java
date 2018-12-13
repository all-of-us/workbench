package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.visitTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

/**
 * VisitsQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery for visit criteria types.
 */
@Service
public class VisitsQueryBuilder extends AbstractQueryBuilder {

  //If the querybuilder will use modifiers then this sql statement has to have
  //the distinct and visit_start_date as entry_date
  private static final String VISIT_SELECT_CLAUSE_TEMPLATE =
    "select distinct person_id, visit_start_date as entry_date, visit_source_concept_id\n" +
      "from `${projectId}.${dataSetId}.visit_occurrence` a\n";

  private static final String VISIT_CHILD_CLAUSE_TEMPLATE =
    "where a.visit_concept_id in unnest(${visitConceptIds})\n";

  private static final String VISIT_PARENT_CLAUSE_TEMPLATE =
    "where a.visit_concept_id in (\n" +
      "select descendant_id\n" +
      "from `${projectId}.${dataSetId}.criteria_ancestor` \n" +
      "where ancestor_id in unnest(${parentIds}))\n";

  private static final String UNION_TEMPLATE = " union all\n";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams, QueryParameters inputParameters) {
    from(parametersEmpty()).test(inputParameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<>();

    List<Long> parentList = new ArrayList<>();
    List<Long> childList = new ArrayList<>();
    for (SearchParameter parameter : inputParameters.getParameters()) {
      from(typeBlank().or(visitTypeInvalid())).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, parameter.getType());
      from(conceptIdNull()).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, parameter.getConceptId());
      if (parameter.getGroup()) {
        parentList.add(parameter.getConceptId());
      } else {
        childList.add(parameter.getConceptId());
      }
    }

    // Collect all parent type queries to run them together
    if (!parentList.isEmpty()) {
      String namedParameter = addQueryParameterValue(queryParams,
          QueryParameterValue.array(parentList.stream().toArray(Long[]::new), Long.class));
      String parentSql = VISIT_SELECT_CLAUSE_TEMPLATE +
        VISIT_PARENT_CLAUSE_TEMPLATE.replace("${parentIds}", "@" + namedParameter);
      queryParts.add(parentSql);
    }

    // Collect all child type queries to run them together
    if (!childList.isEmpty()) {
      String namedParameter = addQueryParameterValue(queryParams,
          QueryParameterValue.array(childList.stream().toArray(Long[]::new), Long.class));
      String childSql = VISIT_SELECT_CLAUSE_TEMPLATE +
        VISIT_CHILD_CLAUSE_TEMPLATE.replace("${visitConceptIds}", "@" + namedParameter);
      queryParts.add(childSql);
    }

    // Combine the parent and child queries, or just use the one
    String visitSql = queryParts.size() > 1 ?
      String.join(UNION_TEMPLATE, queryParts) : queryParts.get(0);

    String finalSql = buildModifierSql(visitSql, queryParams, inputParameters.getModifiers());

    return finalSql;
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.VISIT;
  }
}
