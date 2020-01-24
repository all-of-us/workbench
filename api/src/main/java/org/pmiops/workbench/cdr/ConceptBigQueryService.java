package org.pmiops.workbench.cdr;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.concept.ConceptService.ConceptIds;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.model.Domain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final ConceptService conceptService;

  private static final String SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE =
      "select DISTINCT(question_concept_id) as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n";

  @Autowired
  public ConceptBigQueryService(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      ConceptService conceptService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.conceptService = conceptService;
  }

  public int getParticipantCountForConcepts(Domain domain, String omopTable, Set<Long> conceptIds) {
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
      paramMap.put(
          "standardConceptIds",
          QueryParameterValue.array(
              classifiedConceptIds.getStandardConceptIds().toArray(new Long[0]), Long.class));
      if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
        innerSql.append(" or ");
      }
    }
    if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
      if (Domain.SURVEY.equals(domain)) {
        innerSql.append("observation_source_concept_id");
      } else {
        innerSql.append(conceptColumns.getSourceConceptColumn().name);
      }
      innerSql.append(" in unnest(@sourceConceptIds)");
      paramMap.put(
          "sourceConceptIds",
          QueryParameterValue.array(
              classifiedConceptIds.getSourceConceptIds().toArray(new Long[0]), Long.class));
    }
    QueryJobConfiguration jobConfiguration =
        QueryJobConfiguration.newBuilder(innerSql.toString())
            .setNamedParameters(paramMap.build())
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration));
    return (int) result.iterateAll().iterator().next().get(0).getLongValue();
  }

  public List<Long> getSurveyQuestionConceptIds() {
    QueryJobConfiguration qjc =
        QueryJobConfiguration.newBuilder(SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE)
            .setUseLegacySql(false)
            .build();
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc), 360000L);
    List<Long> conceptIdList = new ArrayList<>();
    result
        .getValues()
        .forEach(
            surveyValue -> {
              conceptIdList.add(Long.parseLong(surveyValue.get(0).getValue().toString()));
            });
    return conceptIdList;
  }
}
