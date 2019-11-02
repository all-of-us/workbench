package org.pmiops.workbench.cohortreview

import org.pmiops.workbench.model.FilterColumns.AGE_AT_EVENT
import org.pmiops.workbench.model.FilterColumns.ANSWER
import org.pmiops.workbench.model.FilterColumns.DOMAIN
import org.pmiops.workbench.model.FilterColumns.DOSE
import org.pmiops.workbench.model.FilterColumns.FIRST_MENTION
import org.pmiops.workbench.model.FilterColumns.LAST_MENTION
import org.pmiops.workbench.model.FilterColumns.NUM_OF_MENTIONS
import org.pmiops.workbench.model.FilterColumns.QUESTION
import org.pmiops.workbench.model.FilterColumns.REF_RANGE
import org.pmiops.workbench.model.FilterColumns.ROUTE
import org.pmiops.workbench.model.FilterColumns.SOURCE_CODE
import org.pmiops.workbench.model.FilterColumns.SOURCE_CONCEPT_ID
import org.pmiops.workbench.model.FilterColumns.SOURCE_NAME
import org.pmiops.workbench.model.FilterColumns.SOURCE_VOCAB
import org.pmiops.workbench.model.FilterColumns.STANDARD_CODE
import org.pmiops.workbench.model.FilterColumns.STANDARD_CONCEPT_ID
import org.pmiops.workbench.model.FilterColumns.STANDARD_NAME
import org.pmiops.workbench.model.FilterColumns.STANDARD_VOCAB
import org.pmiops.workbench.model.FilterColumns.START_DATE
import org.pmiops.workbench.model.FilterColumns.STRENGTH
import org.pmiops.workbench.model.FilterColumns.SURVEY_NAME
import org.pmiops.workbench.model.FilterColumns.UNIT
import org.pmiops.workbench.model.FilterColumns.VAL_AS_NUMBER
import org.pmiops.workbench.model.FilterColumns.VISIT_TYPE

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap
import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.DomainType
import org.springframework.stereotype.Service

/** TODO: delete when ui work is done.  */
@Service
class ReviewQueryBuilder {

    fun buildQuery(
            participantId: Long?, domain: DomainType, pageRequest: PageRequest): QueryJobConfiguration {
        return buildQueryJobConfiguration(participantId, domain, pageRequest, false)
    }

    fun buildCountQuery(
            participantId: Long?, domain: DomainType, pageRequest: PageRequest): QueryJobConfiguration {
        return buildQueryJobConfiguration(participantId, domain, pageRequest, true)
    }

    fun buildChartDataQuery(
            participantId: Long?, domain: DomainType, limit: Int?): QueryJobConfiguration {
        val params = HashMap<String, QueryParameterValue>()
        params[PART_ID] = QueryParameterValue.int64(participantId)
        params[DOMAIN_PARAM] = QueryParameterValue.string(domain.name())
        params[LIMIT] = QueryParameterValue.int64(limit)
        return QueryJobConfiguration.newBuilder(String.format(CHART_DATA_TEMPLATE, *CHART_DATA_ARGS))
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    fun buildVocabularyDataQuery(): QueryJobConfiguration {
        return QueryJobConfiguration.newBuilder(String.format(VOCAB_DATA_TEMPLATE, REVIEW_TABLE))
                .setUseLegacySql(false)
                .build()
    }

    private fun buildQueryJobConfiguration(
            participantId: Long?, domain: DomainType, pageRequest: PageRequest, isCountQuery: Boolean): QueryJobConfiguration {
        val finalSql: String
        val params = HashMap<String, QueryParameterValue>()
        params[PART_ID] = QueryParameterValue.int64(participantId)

        when (domain) {
            SURVEY -> finalSql = if (isCountQuery)
                String.format(COUNT_SURVEY_TEMPLATE, SURVEY_TABLE, PART_ID)
            else
                String.format(
                        SURVEY_SQL_TEMPLATE + ORDER_BY, *addOrderByArgs(SURVEY_ARGS, pageRequest))
            ALL_EVENTS -> finalSql = if (isCountQuery)
                String.format(COUNT_TEMPLATE, REVIEW_TABLE, PART_ID)
            else
                String.format(
                        BASE_SQL_TEMPLATE + ORDER_BY, *addOrderByArgs(BASE_SQL_ARGS, pageRequest))
            CONDITION, DEVICE, DRUG, LAB, OBSERVATION, PHYSICAL_MEASUREMENT, PROCEDURE, VISIT, VITAL -> {
                finalSql = if (isCountQuery)
                    String.format(COUNT_TEMPLATE + DOMAIN_SQL, REVIEW_TABLE, PART_ID)
                else
                    String.format(
                            BASE_SQL_TEMPLATE + DOMAIN_SQL + ORDER_BY,
                            *addOrderByArgs(BASE_SQL_ARGS, pageRequest))
                params[DOMAIN_PARAM] = QueryParameterValue.string(domain.name())
            }
            else -> throw BadRequestException("There is no domain named: " + domain.toString())
        }
        return QueryJobConfiguration.newBuilder(finalSql)
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    private fun addOrderByArgs(args: Array<Any>, pageRequest: PageRequest): Array<Any> {
        val newArgs = ArrayList(Arrays.asList(*args))
        newArgs.add(pageRequest.sortColumn)
        newArgs.add(pageRequest.sortOrder!!.toString())
        newArgs.add(pageRequest.pageSize.toString())
        newArgs.add((pageRequest.page!! * pageRequest.pageSize!!).toString())
        return newArgs.toTypedArray()
    }

    companion object {

        private val PART_ID = "participantId"
        private val DOMAIN_PARAM = "domain"
        private val LIMIT = "limit"
        private val REVIEW_TABLE = "cb_review_all_events"
        private val SURVEY_TABLE = "cb_review_survey"
        private val BASE_SQL_ARGS = arrayOf<Any>(START_DATE.toString(), DOMAIN.toString(), STANDARD_NAME.toString(), STANDARD_CODE.toString(), STANDARD_VOCAB.toString(), STANDARD_CONCEPT_ID.toString(), SOURCE_NAME.toString(), SOURCE_CODE.toString(), SOURCE_VOCAB.toString(), SOURCE_CONCEPT_ID.toString(), AGE_AT_EVENT.toString(), VISIT_TYPE.toString(), ROUTE.toString(), DOSE.toString(), STRENGTH.toString(), UNIT.toString(), REF_RANGE.toString(), NUM_OF_MENTIONS.toString(), FIRST_MENTION.toString(), LAST_MENTION.toString(), VAL_AS_NUMBER.toString(), REVIEW_TABLE, PART_ID)
        private val SURVEY_ARGS = arrayOf<Any>(START_DATE.toString(), SURVEY_NAME.toString(), QUESTION.toString(), ANSWER.toString(), SURVEY_TABLE, PART_ID)
        private val CHART_DATA_ARGS = arrayOf<Any>(REVIEW_TABLE, REVIEW_TABLE, PART_ID, DOMAIN_PARAM, LIMIT, PART_ID, DOMAIN_PARAM, LIMIT)

        private val DOMAIN_SQL = "and domain = @$DOMAIN_PARAM\n"
        private val ORDER_BY = "order by %s %s, dataId\n limit %s offset %s\n"
        private val BASE_SQL_TEMPLATE = (
                "select person_id as personId,\n"
                        + "data_id as dataId,\n"
                        + "start_datetime as %s,\n"
                        + "domain as %s,\n"
                        + "standard_name as %s,\n"
                        + "standard_code as %s,\n"
                        + "standard_vocabulary as %s,\n"
                        + "standard_concept_id as %s,\n"
                        + "source_name as %s,\n"
                        + "source_code as %s,\n"
                        + "source_vocabulary as %s,\n"
                        + "source_concept_id as %s,\n"
                        + "age_at_event as %s,\n"
                        + "visit_type as %s,\n"
                        + "route as %s,\n"
                        + "dose as %s,\n"
                        + "strength as %s,\n"
                        + "unit as %s,\n"
                        + "ref_range as %s,\n"
                        + "num_mentions as %s,\n"
                        + "first_mention as %s,\n"
                        + "last_mention as %s,\n"
                        + "value_as_number as %s\n"
                        + "from `\${projectId}.\${dataSetId}.%s`\n"
                        + "where person_id = @%s\n")
        private val SURVEY_SQL_TEMPLATE = (
                "select person_id as personId,\n"
                        + "data_id as dataId,\n"
                        + "start_datetime as %s,\n"
                        + "survey as %s,\n"
                        + "question as %s,\n"
                        + "answer as %s\n"
                        + "from `\${projectId}.\${dataSetId}.%s`\n"
                        + "where person_id = @%s\n")
        private val COUNT_TEMPLATE = (
                "select count(*) as count\n"
                        + "from `\${projectId}.\${dataSetId}.%s`\n"
                        + "where person_id = @%s\n")
        private val COUNT_SURVEY_TEMPLATE = (
                "select count(*) as count\n"
                        + "from `\${projectId}.\${dataSetId}.%s`\n"
                        + "where person_id = @%s\n")
        private val CHART_DATA_TEMPLATE = (
                "select distinct a.standard_name as standardName, a.standard_vocabulary as standardVocabulary, "
                        + "DATE(a.start_datetime) as startDate, a.age_at_event as ageAtEvent, rnk as rank\n"
                        + "from `\${projectId}.\${dataSetId}.%s` a\n"
                        + "left join (select standard_code, RANK() OVER(ORDER BY COUNT(*) DESC) as rnk\n"
                        + "from `\${projectId}.\${dataSetId}.%s`\n"
                        + "where person_id = @%s\n"
                        + "and domain = @%s\n"
                        + "and standard_concept_id != 0 \n"
                        + "group by standard_code\n"
                        + "LIMIT @%s) b on a.standard_code = b.standard_code\n"
                        + "where person_id = @%s\n"
                        + "and domain = @%s\n"
                        + "and rnk <= @%s\n"
                        + "order by rank, standardName, startDate\n")
        private val VOCAB_DATA_TEMPLATE = (
                "SELECT distinct 'Standard' as type, 'ALL_EVENTS' as domain, standard_vocabulary as vocabulary\n"
                        + "FROM `\${projectId}.\${dataSetId}.%1\$s`\n"
                        + "UNION ALL\n"
                        + "SELECT distinct 'Standard' as type, domain, standard_vocabulary as vocabulary\n"
                        + "FROM `\${projectId}.\${dataSetId}.%1\$s`\n"
                        + "UNION ALL\n"
                        + "SELECT distinct 'Source' as type, 'ALL_EVENTS' as domain, source_vocabulary as vocabulary\n"
                        + "FROM `\${projectId}.\${dataSetId}.%1\$s`\n"
                        + "where domain in ('CONDITION', 'PROCEDURE')\n"
                        + "UNION ALL\n"
                        + "SELECT distinct 'Source' as type, domain, source_vocabulary as vocabulary\n"
                        + "FROM `\${projectId}.\${dataSetId}.%1\$s`\n"
                        + "where domain in ('CONDITION', 'PROCEDURE')\n"
                        + "order by type, domain, vocabulary")
    }
}
