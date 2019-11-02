package org.pmiops.workbench.cohortbuilder

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import java.util.ArrayList
import java.util.HashMap
import java.util.StringJoiner
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cohortbuilder.util.CriteriaLookupUtil
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CohortQueryBuilder @Autowired
constructor(private val cbCriteriaDao: CBCriteriaDao) {

    /** Provides counts of unique subjects defined by the provided [ParticipantCriteria].  */
    fun buildParticipantCounterQuery(
            participantCriteria: ParticipantCriteria): QueryJobConfiguration {
        val sqlTemplate = COUNT_SQL_TEMPLATE.replace("\${table}", SEARCH_PERSON_TABLE)
        val params = HashMap<String, QueryParameterValue>()
        val queryBuilder = StringBuilder(sqlTemplate.replace("\${mainTable}", SEARCH_PERSON_TABLE))
        addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params)

        return QueryJobConfiguration.newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    /**
     * Provides counts with demographic info for charts defined by the provided [ ].
     */
    fun buildDemoChartInfoCounterQuery(
            participantCriteria: ParticipantCriteria): QueryJobConfiguration {
        val sqlTemplate = DEMO_CHART_INFO_SQL_TEMPLATE.replace("\${table}", SEARCH_PERSON_TABLE)
        val params = HashMap<String, QueryParameterValue>()
        val queryBuilder = StringBuilder(sqlTemplate.replace("\${mainTable}", SEARCH_PERSON_TABLE))
        addWhereClause(participantCriteria, SEARCH_PERSON_TABLE, queryBuilder, params)
        queryBuilder.append(DEMO_CHART_INFO_SQL_GROUP_BY.replace("\${mainTable}", SEARCH_PERSON_TABLE))

        return QueryJobConfiguration.newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    /**
     * Provides counts with domain info for charts defined by the provided [ ].
     */
    fun buildDomainChartInfoCounterQuery(
            participantCriteria: ParticipantCriteria, domainType: DomainType, chartLimit: Int): QueryJobConfiguration {
        val endSqlTemplate = DOMAIN_CHART_INFO_SQL_GROUP_BY
                .replace("\${limit}", Integer.toString(chartLimit))
                .replace("\${tableId}", "standard_concept_id")
                .replace("\${domain}", domainType.name())
        val queryBuilder = StringBuilder(DOMAIN_CHART_INFO_SQL_TEMPLATE.replace("\${mainTable}", REVIEW_TABLE))
        val params = HashMap<String, QueryParameterValue>()
        addWhereClause(participantCriteria, REVIEW_TABLE, queryBuilder, params)
        queryBuilder.append(endSqlTemplate.replace("\${mainTable}", REVIEW_TABLE))

        return QueryJobConfiguration.newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    fun buildRandomParticipantQuery(
            participantCriteria: ParticipantCriteria, resultSize: Long, offset: Long): QueryJobConfiguration {
        var endSql = "$RANDOM_SQL_ORDER_BY $resultSize"
        if (offset > 0) {
            endSql += OFFSET_SUFFIX + offset
        }
        val sqlTemplate = RANDOM_SQL_TEMPLATE.replace("\${table}", PERSON_TABLE)
        val params = HashMap<String, QueryParameterValue>()
        val queryBuilder = StringBuilder(sqlTemplate.replace("\${mainTable}", PERSON_TABLE))
        addWhereClause(participantCriteria, PERSON_TABLE, queryBuilder, params)
        queryBuilder.append(endSql.replace("\${mainTable}", PERSON_TABLE))

        return QueryJobConfiguration.newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    // implemented for use with the Data Set Builder. Please remove if this does not become the
    // preferred solution
    // https://docs.google.com/document/d/1-wzSCHDM_LSaBRARyLFbsTGcBaKi5giRs-eDmaMBr0Y/edit#
    fun buildParticipantIdQuery(participantCriteria: ParticipantCriteria): QueryJobConfiguration {
        val sqlTemplate = ID_SQL_TEMPLATE.replace("\${table}", PERSON_TABLE)
        val params = HashMap<String, QueryParameterValue>()
        val queryBuilder = StringBuilder(sqlTemplate.replace("\${mainTable}", PERSON_TABLE))
        addWhereClause(participantCriteria, PERSON_TABLE, queryBuilder, params)

        return QueryJobConfiguration.newBuilder(queryBuilder.toString())
                .setNamedParameters(params)
                .setUseLegacySql(false)
                .build()
    }

    fun addWhereClause(
            participantCriteria: ParticipantCriteria,
            mainTable: String,
            queryBuilder: StringBuilder,
            params: MutableMap<String, QueryParameterValue>) {
        val request = participantCriteria.searchRequest
        if (request == null) {
            queryBuilder.append(PERSON_ID_WHITELIST_TEMPLATE.replace("\${mainTable}", mainTable))
            params[PERSON_ID_WHITELIST_PARAM] = QueryParameterValue.array(
                    participantCriteria.participantIdsToInclude!!.toTypedArray(), Long::class.java)
        } else {
            if (request.getIncludes().isEmpty() && request.getExcludes().isEmpty()) {
                throw BadRequestException(
                        "Invalid SearchRequest: includes[] and excludes[] cannot both be empty")
            }

            // produces a map of matching child concept ids.
            var criteriaLookup: Map<SearchParameter, Set<Long>> = HashMap<SearchParameter, Set<Long>>()
            try {
                criteriaLookup = CriteriaLookupUtil(cbCriteriaDao).buildCriteriaLookupMap(request)
            } catch (ex: IllegalArgumentException) {
                throw BadRequestException("Bad Request: " + ex.message)
            }

            // build query for included search groups
            val joiner = buildQuery(criteriaLookup, request.getIncludes(), mainTable, params, false)

            // if includes is empty then don't add the excludes clause
            if (joiner.toString().isEmpty()) {
                joiner.merge(buildQuery(criteriaLookup, request.getExcludes(), mainTable, params, false))
            } else {
                joiner.merge(buildQuery(criteriaLookup, request.getExcludes(), mainTable, params, true))
            }
            val participantIdsToExclude = participantCriteria.participantIdsToExclude
            if (!participantIdsToExclude!!.isEmpty()) {
                joiner.add(PERSON_ID_BLACKLIST_TEMPLATE.replace("\${mainTable}", mainTable))
                params[PERSON_ID_BLACKLIST_PARAM] = QueryParameterValue.array(participantIdsToExclude.toTypedArray(), Long::class.java)
            }
            queryBuilder.append(joiner.toString())
        }
    }

    private fun buildQuery(
            criteriaLookup: Map<SearchParameter, Set<Long>>,
            groups: List<SearchGroup>,
            mainTable: String,
            params: MutableMap<String, QueryParameterValue>,
            excludeSQL: Boolean?): StringJoiner {
        val joiner = StringJoiner("and ")
        var queryParts: MutableList<String> = ArrayList()
        for (includeGroup in groups) {
            SearchGroupItemQueryBuilder.buildQuery(criteriaLookup, params, queryParts, includeGroup)

            if (excludeSQL!!) {
                joiner.add(
                        EXCLUDE_SQL_TEMPLATE
                                .replace("\${mainTable}", mainTable)
                                .replace("\${excludeSql}", queryParts.joinToString(UNION_TEMPLATE)))
            } else {
                joiner.add(
                        INCLUDE_SQL_TEMPLATE
                                .replace("\${mainTable}", mainTable)
                                .replace("\${includeSql}", queryParts.joinToString(UNION_TEMPLATE)))
            }
            queryParts = ArrayList()
        }
        return joiner
    }

    companion object {
        private val REVIEW_TABLE = "cb_review_all_events"
        private val COUNT_SQL_TEMPLATE = (
                "select count(*) as count\n"
                        + "from `\${projectId}.\${dataSetId}.\${table}` \${table}\n"
                        + "where\n")

        private val SEARCH_PERSON_TABLE = "cb_search_person"
        private val PERSON_TABLE = "person"

        private val DEMO_CHART_INFO_SQL_TEMPLATE = (
                "select gender, \n"
                        + "race, \n"
                        + "case "
                        + getAgeRangeSql(0, 18)
                        + "\n"
                        + getAgeRangeSql(19, 44)
                        + "\n"
                        + getAgeRangeSql(45, 64)
                        + "\n"
                        + "else '> 65'\n"
                        + "end as ageRange,\n"
                        + "count(*) as count\n"
                        + "from `\${projectId}.\${dataSetId}.\${table}` \${table}\n"
                        + "where\n")

        private val DEMO_CHART_INFO_SQL_GROUP_BY = "group by gender, race, ageRange\n" + "order by gender, race, ageRange\n"

        private val DOMAIN_CHART_INFO_SQL_TEMPLATE = (
                "select standard_name as name, standard_concept_id as conceptId, count(distinct person_id) as count\n"
                        + "from `\${projectId}.\${dataSetId}."
                        + REVIEW_TABLE
                        + "` "
                        + REVIEW_TABLE
                        + "\n"
                        + "where\n")

        private val DOMAIN_CHART_INFO_SQL_GROUP_BY = (
                "and domain = '\${domain}'\n"
                        + "and standard_concept_id != 0 \n"
                        + "group by name, conceptId\n"
                        + "order by count desc, name asc\n"
                        + "limit \${limit}\n")

        private val RANDOM_SQL_TEMPLATE = (
                "select rand() as x, \${table}.person_id, race_concept_id, gender_concept_id, ethnicity_concept_id, birth_datetime, case when death.person_id is null then false else true end as deceased\n"
                        + "from `\${projectId}.\${dataSetId}.\${table}` \${table}\n"
                        + "left join `\${projectId}.\${dataSetId}.death` death on (\${table}.person_id = death.person_id)\n"
                        + "where\n")

        private val RANDOM_SQL_ORDER_BY = "order by x\nlimit"

        private val OFFSET_SUFFIX = " offset "

        private val ID_SQL_TEMPLATE =
                "select person_id\n" + "from `\${projectId}.\${dataSetId}.\${table}` \${table}\n" + "where\n"

        private val UNION_TEMPLATE = "union all\n"

        private val INCLUDE_SQL_TEMPLATE = "\${mainTable}.person_id in (\${includeSql})\n"

        private val PERSON_ID_WHITELIST_PARAM = "person_id_whitelist"
        private val PERSON_ID_BLACKLIST_PARAM = "person_id_blacklist"

        private val PERSON_ID_WHITELIST_TEMPLATE =
                "\${mainTable}.person_id in unnest(@$PERSON_ID_WHITELIST_PARAM)\n"
        private val PERSON_ID_BLACKLIST_TEMPLATE =
                "\${mainTable}.person_id not in unnest(@$PERSON_ID_BLACKLIST_PARAM)\n"

        private val EXCLUDE_SQL_TEMPLATE = "\${mainTable}.person_id not in\n" + "(\${excludeSql})\n"

        /**
         * Helper method to build sql snippet.
         *
         * @param lo - lower bound of the age range
         * @param hi - upper bound of the age range
         * @return
         */
        private fun getAgeRangeSql(lo: Int, hi: Int): String {
            return ("when CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE("
                    + SEARCH_PERSON_TABLE
                    + ".dob), MONTH)/12) as INT64) >= "
                    + lo
                    + " and CAST(FLOOR(DATE_DIFF(CURRENT_DATE, DATE("
                    + SEARCH_PERSON_TABLE
                    + ".dob), MONTH)/12) as INT64) <= "
                    + hi
                    + " then '"
                    + lo
                    + "-"
                    + hi
                    + "'")
        }
    }
}
