package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SearchParameter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DrugQueryBuilder extends AbstractQueryBuilder {

  private static final String DRUG_SQL_TEMPLATE =
    "select distinct person_id, drug_exposure_start_date as entry_date\n" +
      "from `${projectId}.${dataSetId}.drug_exposure`\n" +
      "where drug_concept_id in unnest(${conceptIds})\n" +
      "${encounterSql}";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    if (parameters.getParameters().isEmpty()) {
      throw new BadRequestException("Please provide a valid search parameter.");
    }
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String namedParameter = "drug" + getUniqueNamedParameterPostfix();
    Long[] conceptIds = parameters.getParameters()
      .stream()
      .map(this::validateConceptId)
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

  private Long validateConceptId(SearchParameter searchParameter) {
    if (searchParameter.getConceptId() == null) {
      throw new BadRequestException("Please provide a search parameter with a valid conceptId.");
    }
    return searchParameter.getConceptId();
  }
}
