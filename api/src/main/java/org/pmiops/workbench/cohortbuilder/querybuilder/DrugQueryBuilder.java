package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.Validation.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.validation.ParameterPredicates.*;

@Service
public class DrugQueryBuilder extends AbstractQueryBuilder {

  private static final String DRUG_SQL_TEMPLATE =
    "select distinct person_id, drug_exposure_start_date as entry_date, drug_source_concept_id\n" +
      "from `${projectId}.${dataSetId}.drug_exposure`\n" +
      "where drug_concept_id in unnest(${conceptIds})\n" +
      "${encounterSql}";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    from(parametersEmpty()).test(parameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String namedParameter = "drug" + getUniqueNamedParameterPostfix();
    Long[] conceptIds = parameters.getParameters()
      .stream()
      .map(param -> {
        from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
        from(typeBlank().or(drugTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
        return param.getConceptId();
      })
      .toArray(Long[]::new);
    queryParams.put(namedParameter, QueryParameterValue.array(conceptIds, Long.class));
    String drugSql = DRUG_SQL_TEMPLATE.replace("${conceptIds}", "@" + namedParameter);
    String finalSql = buildModifierSql(drugSql, queryParams, parameters.getModifiers());

    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.DRUG;
  }
}
