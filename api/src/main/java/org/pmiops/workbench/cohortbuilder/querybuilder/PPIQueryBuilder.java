package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdm.DomainTableEnum;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.conceptIdNull;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.domainBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.domainNotObservation;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.nameNotNumber;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.parametersEmpty;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.ppiTypeInvalid;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.typeBlank;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.ParameterPredicates.valueNotNumber;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.CONCEPT_ID;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.DOMAIN;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.EMPTY_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NAME;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.NOT_VALID_MESSAGE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETER;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.PARAMETERS;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.TYPE;
import static org.pmiops.workbench.cohortbuilder.querybuilder.util.QueryBuilderConstants.VALUE;
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
  public String buildQuery(Map<String, QueryParameterValue> queryParams, QueryParameters parameters) {
    from(parametersEmpty()).test(parameters.getParameters()).throwException(EMPTY_MESSAGE, PARAMETERS);
    List<String> queryParts = new ArrayList<String>();
    for (SearchParameter parameter : parameters.getParameters()) {
      validateSearchParameter(parameter);
      String namedParameterConceptId = addQueryParameterValue(queryParams,
          QueryParameterValue.int64(parameter.getConceptId()));
      boolean isValueAsNum = StringUtils.isBlank(parameter.getValue());
      String value = isValueAsNum ? parameter.getName() : parameter.getValue();
      String namedParameter = addQueryParameterValue(queryParams,
          QueryParameterValue.int64(new Long(value)));
      String domain = parameter.getDomainId().toLowerCase();
      String sqlTemplate = isValueAsNum ?
        PPI_SQL_TEMPLATE + VALUE_AS_NUMBER_SQL_TEMPLATE :
        PPI_SQL_TEMPLATE + VALUE_AS_CONCEPT_ID_SQL_TEMPLATE;

      queryParts.add(sqlTemplate
        .replace("${tableName}", DomainTableEnum.getTableName(domain))
        .replace("${tableConceptId}", DomainTableEnum.getSourceConceptId(domain))
        .replace("${conceptId}", "@" + namedParameterConceptId)
        .replace("${value}","@" + namedParameter));
    }
    return String.join(UNION_ALL, queryParts);
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
