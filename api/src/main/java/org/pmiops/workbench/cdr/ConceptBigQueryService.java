package org.pmiops.workbench.cdr;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptService.ConceptIds;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private static final String DOMAIN_CONCEPT_SOURCE = "source";

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final ConceptService conceptService;

  @Autowired
  public ConceptBigQueryService(BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService, ConceptService conceptService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.conceptService = conceptService;
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
    ConceptColumns conceptColumns = cdrBigQuerySchemaConfigService.getConceptColumns(omopTable);
    ConceptIds classifiedConceptIds = conceptService.classifyConceptIds(conceptIds);
    if (classifiedConceptIds.getSourceConceptIds().isEmpty()
        && classifiedConceptIds.getStandardConceptIds().isEmpty()) {
      return 0;
    }
    StringBuilder innerSql = new StringBuilder("select count(distinct person_id) person_count\n");
    innerSql.append("from ");
    innerSql.append(String.format("`${projectId}.${dataSetId}.%s`", omopTable));
    innerSql.append(" where ");
    ImmutableMap.Builder<String, QueryParameterValue> paramMap = ImmutableMap.builder();
    if (!classifiedConceptIds.getStandardConceptIds().isEmpty()) {
      innerSql.append(conceptColumns.getStandardConceptColumn().name);
      innerSql.append(" in unnest(@standardConceptIds)");
      paramMap.put("standardConceptIds", QueryParameterValue.array(
          classifiedConceptIds.getStandardConceptIds().toArray(new Long[0]), Long.class));
      if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
        innerSql.append(" or ");
      }
    }
    if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
      innerSql.append(conceptColumns.getSourceConceptColumn().name);
      innerSql.append(" in unnest(@sourceConceptIds)");
      paramMap.put("sourceConceptIds", QueryParameterValue.array(
          classifiedConceptIds.getSourceConceptIds().toArray(new Long[0]), Long.class));
    }
    QueryJobConfiguration jobConfiguration = QueryJobConfiguration
        .newBuilder(innerSql.toString())
        .setNamedParameters(paramMap.build())
        .setUseLegacySql(false)
        .build();
    QueryResult result = bigQueryService.executeQuery(
        bigQueryService.filterBigQueryConfig(jobConfiguration));
    return (int) result.iterateAll().iterator().next().get(0).getLongValue();
  }

}
