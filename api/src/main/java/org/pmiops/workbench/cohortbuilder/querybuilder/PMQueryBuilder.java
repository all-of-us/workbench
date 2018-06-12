package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String SQL_TEMPLATE =
    "select 0\n" +
      "from `${projectId}.${dataSetId}.measurement`\n" +
      "limit :limit\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put("limit", QueryParameterValue.newBuilder().setValue("1").build());
    return QueryJobConfiguration
      .newBuilder(SQL_TEMPLATE)
      .setNamedParameters(params)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PM;
  }
}