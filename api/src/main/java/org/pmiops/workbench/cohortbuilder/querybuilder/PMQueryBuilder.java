package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.springframework.stereotype.Service;

@Service
public class PMQueryBuilder extends AbstractQueryBuilder {

  private static final String SQL_TEMPLATE =
    "select 0\n" +
      "from `${projectId}.${dataSetId}.measurement`\n" +
      "limit 1\n";

  @Override
  public QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters) {
    return QueryJobConfiguration
      .newBuilder(SQL_TEMPLATE)
      .setUseLegacySql(false)
      .build();
  }

  @Override
  public FactoryKey getType() {
    return FactoryKey.PM;
  }
}
