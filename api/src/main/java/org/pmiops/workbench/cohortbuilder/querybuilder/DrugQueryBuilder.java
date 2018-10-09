package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.drugTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

@Service
public class DrugQueryBuilder extends AbstractQueryBuilder {

  private static final String DRUG_SQL_TEMPLATE =
    "select distinct person_id, drug_exposure_start_date as entry_date, drug_source_concept_id\n" +
      "from `${projectId}.${dataSetId}.drug_exposure`\n" +
      "where drug_concept_id in unnest(${conceptIds})\n" +
      "${encounterSql}";

  @Override
  public String buildQuery(Map<String, QueryParameterValue> queryParams, QueryParameters parameters) {
    from(parametersEmpty()).test(parameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    Long[] conceptIds = parameters.getParameters()
      .stream()
      .map(param -> {
        from(typeBlank().or(drugTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
        from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
        return param.getConceptId();
      })
      .toArray(Long[]::new);
    String namedParameter = addQueryParameterValue(queryParams, QueryParameterValue.array(conceptIds, Long.class));
    String drugSql = DRUG_SQL_TEMPLATE.replace("${conceptIds}", "@" + namedParameter);
    return buildModifierSql(drugSql, queryParams, parameters.getModifiers());
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.DRUG;
  }
}
