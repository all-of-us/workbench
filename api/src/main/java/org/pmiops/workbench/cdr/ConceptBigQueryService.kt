package org.pmiops.workbench.cdr

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.TableResult
import com.google.common.collect.ImmutableMap
import java.util.ArrayList
import java.util.HashMap
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.dao.ConceptService.ConceptIds
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns
import org.pmiops.workbench.model.SurveyAnswerResponse
import org.pmiops.workbench.model.SurveyQuestionsResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ConceptBigQueryService @Autowired
constructor(
        private val bigQueryService: BigQueryService,
        private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService,
        private val conceptService: ConceptService) {

    val surveyQuestionConceptIds: List<Long>
        get() {
            val result = bigQueryService.executeQuery(
                    bigQueryService.filterBigQueryConfig(buildSurveyQuestionConceptIdQuery()), 360000L)
            val conceptIdList = ArrayList<Long>()
            result
                    .values
                    .forEach { surveyValue -> conceptIdList.add(java.lang.Long.parseLong(surveyValue[0].value.toString())) }
            return conceptIdList
        }

    fun getParticipantCountForConcepts(omopTable: String, conceptIds: Set<Long>): Int {
        val conceptColumns = cdrBigQuerySchemaConfigService.getConceptColumns(omopTable)
        val classifiedConceptIds = conceptService.classifyConceptIds(conceptIds)
        if (classifiedConceptIds.sourceConceptIds.isEmpty() && classifiedConceptIds.standardConceptIds.isEmpty()) {
            return 0
        }
        val innerSql = StringBuilder("select count(distinct person_id) person_count\n")
        innerSql.append("from ")
        innerSql.append(String.format("`\${projectId}.\${dataSetId}.%s`", omopTable))
        innerSql.append(" where ")
        val paramMap = ImmutableMap.builder<String, QueryParameterValue>()
        if (!classifiedConceptIds.standardConceptIds.isEmpty()) {
            innerSql.append(conceptColumns.standardConceptColumn.name)
            innerSql.append(" in unnest(@standardConceptIds)")
            paramMap.put(
                    "standardConceptIds",
                    QueryParameterValue.array(
                            classifiedConceptIds.standardConceptIds.toTypedArray(), Long::class.java))
            if (!classifiedConceptIds.sourceConceptIds.isEmpty()) {
                innerSql.append(" or ")
            }
        }
        if (!classifiedConceptIds.sourceConceptIds.isEmpty()) {
            innerSql.append(conceptColumns.sourceConceptColumn.name)
            innerSql.append(" in unnest(@sourceConceptIds)")
            paramMap.put(
                    "sourceConceptIds",
                    QueryParameterValue.array(
                            classifiedConceptIds.sourceConceptIds.toTypedArray(), Long::class.java))
        }
        val jobConfiguration = QueryJobConfiguration.newBuilder(innerSql.toString())
                .setNamedParameters(paramMap.build())
                .setUseLegacySql(false)
                .build()
        val result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration))
        return result.iterateAll().iterator().next()[0].longValue.toInt()
    }

    private fun buildSurveyQuestionQuery(surveyName: String): QueryJobConfiguration {
        val finalSql = SURVEY_QUESTION_SQL_TEMPLATE
        val params = HashMap<String, QueryParameterValue>()
        params[SURVEY_PARAM] = QueryParameterValue.string(surveyName.toUpperCase())
        return QueryJobConfiguration.newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    private fun buildSurveyAnswerQuery(questionConceptId: Long?): QueryJobConfiguration {
        val finalSql = SURVEY_ANSWER_SQL_TEMPLATE
        val params = HashMap<String, QueryParameterValue>()
        params[QUESTION_PARAM] = QueryParameterValue.int64(questionConceptId)
        return QueryJobConfiguration.newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    private fun buildSurveyQuestionConceptIdQuery(): QueryJobConfiguration {
        val finalSql = SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE
        return QueryJobConfiguration.newBuilder(finalSql).setUseLegacySql(false).build()
    }

    fun getSurveyQuestions(surveyName: String): List<SurveyQuestionsResponse> {
        val responseList = ArrayList<SurveyQuestionsResponse>()

        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(buildSurveyQuestionQuery(surveyName)), 360000L)
        result
                .values
                .forEach { surveyValue ->
                    val question = surveyValue[0].value.toString()
                    val concept_id = java.lang.Long.parseLong(surveyValue[1].value.toString())
                    responseList.add(
                            SurveyQuestionsResponse().question(question).conceptId(concept_id))
                }
        return responseList
    }

    fun getSurveyAnswer(questionConceptId: Long?): List<SurveyAnswerResponse> {
        val answerList = ArrayList<SurveyAnswerResponse>()
        val result = bigQueryService.executeQuery(
                bigQueryService.filterBigQueryConfig(buildSurveyAnswerQuery(questionConceptId)),
                360000L)
        result
                .values
                .forEach { surveyValue ->
                    val answer = SurveyAnswerResponse()
                    if (surveyValue[2].value != null) {
                        answer.setParticipationCount(
                                java.lang.Long.parseLong(surveyValue[2].value.toString()))
                    } else {
                        answer.setParticipationCount(0L)
                    }
                    answer.setAnswer(surveyValue[0].value.toString())
                    answer.setConceptId(java.lang.Long.parseLong(surveyValue[1].value.toString()))
                    if (surveyValue[3].value != null) {
                        answer.setPercentAnswered(
                                java.lang.Double.parseDouble(surveyValue[3].value.toString()))
                    } else {
                        answer.setPercentAnswered(0.0)
                    }
                    answerList.add(answer)
                }
        return answerList
    }

    companion object {

        private val SURVEY_PARAM = "survey"
        private val QUESTION_PARAM = "question_concept_id"

        private val SURVEY_QUESTION_SQL_TEMPLATE = (
                "select DISTINCT(question) as question,\n"
                        + "question_concept_id as concept_id \n"
                        + "from `\${projectId}.\${dataSetId}.ds_survey`\n"
                        + "where UPPER(survey) = @"
                        + SURVEY_PARAM)

        private val SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE = "select DISTINCT(question_concept_id) as concept_id \n" + "from `\${projectId}.\${dataSetId}.ds_survey`\n"

        private val SURVEY_ANSWER_SQL_TEMPLATE = (
                "select a.answer, answer_concept_id, ans_part_count, "
                        + "round((ans_part_count/ques_part_cnt)*100,2) from "
                        + "(SELECT answer, answer_concept_id, question_concept_id, count(distinct person_id) ans_part_count "
                        + "FROM `\${projectId}.\${dataSetId}.ds_survey` GROUP BY 1,2,3) a join "
                        + "(SELECT question, question_concept_id, count(distinct person_id) ques_part_cnt "
                        + "FROM `\${projectId}.\${dataSetId}.ds_survey` GROUP BY 1,2) b on "
                        + "a.question_concept_id = b.question_concept_id WHERE a.question_concept_id = @"
                        + QUESTION_PARAM)
    }
}
