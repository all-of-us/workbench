package org.pmiops.workbench.cdr;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptService.ConceptIds;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.model.SurveyQuestionsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final ConceptService conceptService;

  @Autowired
  public ConceptBigQueryService(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      ConceptService conceptService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.conceptService = conceptService;
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
      paramMap.put(
          "standardConceptIds",
          QueryParameterValue.array(
              classifiedConceptIds.getStandardConceptIds().toArray(new Long[0]), Long.class));
      if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
        innerSql.append(" or ");
      }
    }
    if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
      innerSql.append(conceptColumns.getSourceConceptColumn().name);
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

  private static final String SURVEY_PARAM = "survey";

  private static final String SURVEY_SQL_TEMPLATE =
      "select DISTINCT(question) as question,\n"
          + "question_concept_id as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n"
          + "where survey = @"
          + SURVEY_PARAM;

  private QueryJobConfiguration buildSurveyQuestionQuery(String survey) {
    String finalSql = SURVEY_SQL_TEMPLATE;
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(SURVEY_PARAM, QueryParameterValue.string(survey));
    return QueryJobConfiguration.newBuilder(finalSql)
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  public List<SurveyQuestionsResponse> getSurveyQuestions(String survey) {
    List<SurveyQuestionsResponse> responseList = new ArrayList<>();

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(buildSurveyQuestionQuery(survey)), 360000L);
    result
        .getValues()
        .forEach(
            surveyValue -> {
              String question = surveyValue.get(0).getValue().toString();
              Long concept_id = Long.parseLong(surveyValue.get(1).getValue().toString());
              responseList.add(
                  new SurveyQuestionsResponse().question(question).conceptId(concept_id));
            });
    return responseList;
  }
}
