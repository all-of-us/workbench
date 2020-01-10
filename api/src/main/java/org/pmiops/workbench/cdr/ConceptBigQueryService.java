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
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.concept.ConceptService.ConceptIds;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.model.SurveyAnswerResponse;
import org.pmiops.workbench.model.SurveyQuestions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConceptBigQueryService {

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final ConceptService conceptService;

  private static final String SURVEY_PARAM = "survey";
  private static final String QUESTION_PARAM = "question_concept_id";

  private static final String SURVEY_QUESTION_SQL_TEMPLATE =
      "select DISTINCT(question) as question,\n"
          + "question_concept_id as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n"
          + "where UPPER(survey) = @"
          + SURVEY_PARAM;

  private static final String SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE =
      "select DISTINCT(question_concept_id) as concept_id \n"
          + "from `${projectId}.${dataSetId}.ds_survey`\n";

  private static final String SURVEY_ANSWER_SQL_TEMPLATE =
      "select a.answer, answer_concept_id, ans_part_count, "
          + "round((ans_part_count/ques_part_cnt)*100,2) from "
          + "(SELECT answer, answer_concept_id, question_concept_id, count(distinct person_id) ans_part_count "
          + "FROM `${projectId}.${dataSetId}.ds_survey` GROUP BY 1,2,3) a join "
          + "(SELECT question, question_concept_id, count(distinct person_id) ques_part_cnt "
          + "FROM `${projectId}.${dataSetId}.ds_survey` GROUP BY 1,2) b on "
          + "a.question_concept_id = b.question_concept_id WHERE a.question_concept_id = @"
          + QUESTION_PARAM;

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

  private QueryJobConfiguration buildSurveyQuestionQuery(String surveyName) {
    String finalSql = SURVEY_QUESTION_SQL_TEMPLATE;
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(SURVEY_PARAM, QueryParameterValue.string(surveyName.toUpperCase()));
    return QueryJobConfiguration.newBuilder(finalSql)
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  private QueryJobConfiguration buildSurveyAnswerQuery(Long questionConceptId) {
    String finalSql = SURVEY_ANSWER_SQL_TEMPLATE;
    Map<String, QueryParameterValue> params = new HashMap<>();
    params.put(QUESTION_PARAM, QueryParameterValue.int64(questionConceptId));
    return QueryJobConfiguration.newBuilder(finalSql)
        .setNamedParameters(params)
        .setUseLegacySql(false)
        .build();
  }

  private QueryJobConfiguration buildSurveyQuestionConceptIdQuery() {
    String finalSql = SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE;
    return QueryJobConfiguration.newBuilder(finalSql).setUseLegacySql(false).build();
  }

  public List<SurveyQuestions> getSurveyQuestions(String surveyName) {
    List<SurveyQuestions> responseList = new ArrayList<>();

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(buildSurveyQuestionQuery(surveyName)), 360000L);
    result
        .getValues()
        .forEach(
            surveyValue -> {
              String question = surveyValue.get(0).getValue().toString();
              Long concept_id = Long.parseLong(surveyValue.get(1).getValue().toString());
              responseList.add(new SurveyQuestions().question(question).conceptId(concept_id));
            });
    return responseList;
  }

  public List<SurveyAnswerResponse> getSurveyAnswer(Long questionConceptId) {
    List<SurveyAnswerResponse> answerList = new ArrayList<>();
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(buildSurveyAnswerQuery(questionConceptId)),
            360000L);
    result
        .getValues()
        .forEach(
            surveyValue -> {
              SurveyAnswerResponse answer = new SurveyAnswerResponse();
              if (surveyValue.get(2).getValue() != null) {
                answer.setParticipationCount(
                    Long.parseLong(surveyValue.get(2).getValue().toString()));
              } else {
                answer.setParticipationCount(0l);
              }
              answer.setAnswer(surveyValue.get(0).getValue().toString());
              answer.setConceptId(Long.parseLong(surveyValue.get(1).getValue().toString()));
              if (surveyValue.get(3).getValue() != null) {
                answer.setPercentAnswered(
                    Double.parseDouble(surveyValue.get(3).getValue().toString()));
              } else {
                answer.setPercentAnswered(0.0);
              }
              answerList.add(answer);
            });
    return answerList;
  }

  public List<Long> getSurveyQuestionConceptIds() {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(buildSurveyQuestionConceptIdQuery()), 360000L);
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
