package org.pmiops.workbench.cdr;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private static final String DOMAIN_CONCEPT_SOURCE = "source";

  private final BigQueryService bigQueryService;
  private final Provider<CdrBigQuerySchemaConfig> bigQuerySchemaConfigProvider;

  @Autowired
  public ConceptBigQueryService(BigQueryService bigQueryService,
      Provider<CdrBigQuerySchemaConfig> bigQuerySchemaConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.bigQuerySchemaConfigProvider = bigQuerySchemaConfigProvider;
  }

  private String getSourceConceptIdColumn(TableConfig tableConfig, String tableName) {
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (DOMAIN_CONCEPT_SOURCE.equals(columnConfig.domainConcept)) {
        return columnConfig.name;
      }
    }
    throw new ServerErrorException("Couldn't find source concept column for " + tableName);
  }

  public int getParticipantCountForConcepts(String omopTable, Set<Long> conceptIds) {
    TableConfig tableConfig = bigQuerySchemaConfigProvider.get().cohortTables.get(omopTable);
    if (tableConfig == null) {
      throw new BadRequestException("Invalid OMOP table: " + omopTable);
    }
    String sourceConceptIdColumn = getSourceConceptIdColumn(tableConfig, omopTable);

    StringBuilder innerSql = new StringBuilder("select count(distinct person_id) person_count\n");
    innerSql.append("from ");
    innerSql.append(String.format("`${projectId}.${dataSetId}.%s`", omopTable));
    innerSql.append(" where ");
    innerSql.append(sourceConceptIdColumn);
    innerSql.append(" in unnest(@conceptIds)");

    QueryJobConfiguration jobConfiguration = QueryJobConfiguration
        .newBuilder(innerSql.toString())
        .setNamedParameters(ImmutableMap.of("conceptIds",
            QueryParameterValue.array(conceptIds.toArray(new Long[0]), Long.class)))
        .setUseLegacySql(false)
        .build();
    QueryResult result = bigQueryService.executeQuery(
        bigQueryService.filterBigQueryConfig(jobConfiguration));
    return (int) result.iterateAll().iterator().next().get(0).getLongValue();
  }

}
