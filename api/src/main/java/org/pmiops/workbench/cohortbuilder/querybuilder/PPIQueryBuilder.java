package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.*;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.Validation.from;

@Service
public class PPIQueryBuilder extends AbstractQueryBuilder {

  private static final String UNION_ALL = " union all\n";
  private static final String PPI_SQL_TEMPLATE =
    "select person_id from `${projectId}.${dataSetId}.${tableName}`\n" +
      "where ${tableConceptId} = ${conceptId}\n";

  private static final String VALUE_AS_NUMBER_SQL_TEMPLATE =
    "and value_as_number = ${value}\n";

  private static final String VALUE_AS_CONCEPT_ID_SQL_TEMPLATE =
    "and value_as_concept_id = ${value}\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    from(parametersEmpty()).test(parameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    Map<String, QueryParameterValue> queryParams = new HashMap<>();

    for (SearchParameter parameter : parameters.getParameters()) {
      validateSearchParameter(parameter);
      String namedParameterConceptId = CONCEPTID_PREFIX + getUniqueNamedParameterPostfix();
      String namedParameter = VALUE_PREFIX + getUniqueNamedParameterPostfix();
      String domain = parameter.getDomainId().toLowerCase();
      boolean isValueAsNum = StringUtils.isBlank(parameter.getValue());
      String value = isValueAsNum ? parameter.getName() : parameter.getValue();
      String sqlTemplate = isValueAsNum ?
        PPI_SQL_TEMPLATE + VALUE_AS_NUMBER_SQL_TEMPLATE :
        PPI_SQL_TEMPLATE + VALUE_AS_CONCEPT_ID_SQL_TEMPLATE;

      queryParts.add(sqlTemplate
        .replace("${tableName}", DomainTableEnum.getTableName(domain))
        .replace("${tableConceptId}", DomainTableEnum.getSourceConceptId(domain))
        .replace("${conceptId}", "@" + namedParameterConceptId)
        .replace("${value}","@" + namedParameter));
      queryParams.put(namedParameterConceptId, QueryParameterValue.int64(parameter.getConceptId()));
      queryParams.put(namedParameter, QueryParameterValue.int64(new Long(value)));
    }

    String finalSql = String.join(UNION_ALL, queryParts);
    return QueryJobConfiguration
      .newBuilder(finalSql)
      .setNamedParameters(queryParams)
      .setUseLegacySql(false)
      .build();
  }

  private void validateSearchParameter(SearchParameter param) {
    from(typeBlank().or(ppiTypeInvalid())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, TYPE, param.getType());
    from(domainBlank().or(domainNotObservation())).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, DOMAIN, param.getDomainId());
    from(conceptIdNull()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, CONCEPT_ID, param.getConceptId());
    if (StringUtils.isBlank(param.getValue())) {
      from(nameNotNumber()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, NAME, param.getName());
    } else {
      from(valueNotNumber()).test(param).throwException(NOT_VALID_MESSAGE, PARAMETER, VALUE, param.getValue());
    }
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PPI;
  }
}
