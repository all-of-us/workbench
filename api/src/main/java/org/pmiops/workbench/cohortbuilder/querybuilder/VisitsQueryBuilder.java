package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.model.SearchGroupItem;
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
    "select distinct person_id, entry_date, concept_id\n" +
      "from `${projectId}.${dataSetId}.search_visit` a\n" +
      "where a.concept_id in unnest(${visitConceptIds})\n${ageDateAndEncounterSql}";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams,
                           SearchGroupItem inputParameters,
                           boolean temporal) {
    from(parametersEmpty()).test(inputParameters.getSearchParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<Long> conceptIdList = new ArrayList<>();
    for (SearchParameter parameter : inputParameters.getSearchParameters()) {
      from(typeBlank().or(visitTypeInvalid())).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, parameter.getType());
      from(conceptIdNull()).test(parameter).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, parameter.getConceptId());
      conceptIdList.add(parameter.getConceptId());
    }

    String visitSql = "";
    if (!conceptIdList.isEmpty()) {
      String namedParameter = addQueryParameterValue(queryParams,
          QueryParameterValue.array(conceptIdList.stream().toArray(Long[]::new), Long.class));
      visitSql = VISIT_SELECT_CLAUSE_TEMPLATE.replace("${visitConceptIds}", "@" + namedParameter);
    }

    return buildModifierSql(visitSql, queryParams, inputParameters.getModifiers());
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.VISIT;
  }
}
