package org.pmiops.workbench.cohorts

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.math.BigDecimal
import java.util.ArrayList
import java.util.Arrays
import java.util.Objects
import java.util.function.Function
import java.util.stream.Collectors
import javax.inject.Provider
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.dao.ConceptService.ConceptIds
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria
import org.pmiops.workbench.cohortbuilder.TableQueryAndConfig
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus.Key
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.CdrQuery
import org.pmiops.workbench.model.CohortAnnotationsRequest
import org.pmiops.workbench.model.CohortAnnotationsResponse
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.ColumnFilter
import org.pmiops.workbench.model.DataTableSpecification
import org.pmiops.workbench.model.FieldSet
import org.pmiops.workbench.model.MaterializeCohortRequest
import org.pmiops.workbench.model.MaterializeCohortResponse
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.ResultFilters
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.utils.PaginationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CohortMaterializationService @Autowired
constructor(
        private val fieldSetQueryBuilder: FieldSetQueryBuilder,
        private val annotationQueryBuilder: AnnotationQueryBuilder,
        private val participantCohortStatusDao: ParticipantCohortStatusDao,
        private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService,
        private val conceptService: ConceptService,
        private val configProvider: Provider<WorkbenchConfig>) {

    private fun getParticipantIdsWithStatus(
            cohortReview: CohortReview?, statusFilter: List<CohortStatus>): Set<Long> {
        if (cohortReview == null) {
            return ImmutableSet.of()
        }
        val dbStatusFilter = statusFilter.stream().map { StorageEnums.cohortStatusToStorage(it) }.collect<List<Short>, Any>(Collectors.toList())
        return participantCohortStatusDao
                .findByParticipantKey_CohortReviewIdAndStatusIn(
                        cohortReview.cohortReviewId, dbStatusFilter)
                .stream()
                .map { it.participantKey }
                .map { it.participantId }
                .collect<Set<Long>, Any>(Collectors.toSet())
    }

    private fun findPrimaryKey(tableConfig: TableConfig): ColumnConfig {
        for (columnConfig in tableConfig.columns) {
            if (columnConfig.primaryKey != null && columnConfig.primaryKey) {
                return columnConfig
            }
        }
        throw IllegalStateException("Table lacks primary key!")
    }

    private fun getTableQueryAndConfig(
            tableQuery: TableQuery?, conceptIds: Set<Long>?): TableQueryAndConfig {
        var tableQuery = tableQuery
        if (tableQuery == null) {
            tableQuery = TableQuery()
            tableQuery!!.setTableName(PERSON_TABLE)
            tableQuery!!.setColumns(ImmutableList.of<E>(PERSON_ID))
        } else {
            val tableName = tableQuery!!.getTableName()
            if (Strings.isNullOrEmpty(tableName)) {
                throw BadRequestException("Table name must be specified in field sets")
            }
        }
        val cdrSchemaConfig = cdrBigQuerySchemaConfigService.config
        val tableConfig = cdrSchemaConfig.cohortTables[tableQuery!!.getTableName()]
                ?: throw BadRequestException(
                        "Table "
                                + tableQuery!!.getTableName()
                                + " is not a valid "
                                + "cohort table; valid tables are: "
                                + cdrSchemaConfig.cohortTables.keys.stream()
                                .sorted()
                                .collect<String, *>(Collectors.joining(",")))
        val columnMap = Maps.uniqueIndex(tableConfig.columns) { columnConfig -> columnConfig!!.name }

        val columnNames = tableQuery!!.getColumns()
        if (columnNames == null || columnNames!!.isEmpty()) {
            // By default, return all columns on the table in question in our configuration.
            tableQuery!!.setColumns(columnMap.keys.stream().collect<R, A>(Collectors.toList<T>()))
        }
        val orderBy = tableQuery!!.getOrderBy()
        if (orderBy == null || orderBy!!.isEmpty()) {
            val primaryKey = findPrimaryKey(tableConfig)
            if (PERSON_ID == primaryKey.name) {
                tableQuery!!.setOrderBy(ImmutableList.of<E>(PERSON_ID))
            } else {
                // TODO: consider having per-table default sort order based on e.g. timestamp
                tableQuery!!.setOrderBy(ImmutableList.of<E>(PERSON_ID, primaryKey.name))
            }
        }
        if (conceptIds != null) {
            addFilterOnConcepts(tableQuery!!, conceptIds, tableConfig)
        }
        return TableQueryAndConfig(tableQuery, cdrSchemaConfig)
    }

    private fun getParticipantCriteria(
            statusFilter: List<CohortStatus>,
            cohortReview: CohortReview?,
            searchRequest: SearchRequest?): ParticipantCriteria {
        if (statusFilter.contains(CohortStatus.NOT_REVIEWED)) {
            val participantIdsToExclude: Set<Long>
            if (statusFilter.size < CohortStatus.values().length) {
                // Find the participant IDs that have statuses which *aren't* in the filter.
                val statusesToExclude = Sets.difference(
                        ImmutableSet.copyOf(CohortStatus.values()), ImmutableSet.copyOf(statusFilter))
                participantIdsToExclude = getParticipantIdsWithStatus(cohortReview, ImmutableList.copyOf(statusesToExclude))
            } else {
                participantIdsToExclude = ImmutableSet.of()
            }
            return ParticipantCriteria(searchRequest, participantIdsToExclude)
        } else {
            val participantIds = getParticipantIdsWithStatus(cohortReview, statusFilter)
            return ParticipantCriteria(participantIds)
        }
    }

    private fun addFilterOnConcepts(
            tableQuery: TableQuery, conceptIds: Set<Long>, tableConfig: TableConfig) {
        val conceptColumns = cdrBigQuerySchemaConfigService.getConceptColumns(tableConfig, tableQuery.getTableName())
        val classifiedConceptIds = conceptService.classifyConceptIds(conceptIds)

        if (classifiedConceptIds.sourceConceptIds.isEmpty() && classifiedConceptIds.standardConceptIds.isEmpty()) {
            throw BadRequestException("Concept set contains no valid concepts")
        }
        var conceptFilters: ResultFilters? = null
        if (!classifiedConceptIds.standardConceptIds.isEmpty()) {
            val standardConceptFilter = ColumnFilter()
                    .columnName(conceptColumns.standardConceptColumn.name)
                    .operator(Operator.IN)
                    .valueNumbers(
                            classifiedConceptIds.standardConceptIds.stream()
                                    .map { id -> BigDecimal(id!!) }
                                    .collect<R, A>(Collectors.toList<T>()))
            conceptFilters = ResultFilters().columnFilter(standardConceptFilter)
        }
        if (!classifiedConceptIds.sourceConceptIds.isEmpty()) {
            val sourceConceptFilter = ColumnFilter()
                    .columnName(conceptColumns.sourceConceptColumn.name)
                    .operator(Operator.IN)
                    .valueNumbers(
                            classifiedConceptIds.sourceConceptIds.stream()
                                    .map { id -> BigDecimal(id!!) }
                                    .collect<R, A>(Collectors.toList<T>()))
            val sourceResultFilters = ResultFilters().columnFilter(sourceConceptFilter)
            if (conceptFilters == null) {
                conceptFilters = sourceResultFilters
            } else {
                // If both source and standard concepts are present, match either.
                conceptFilters = ResultFilters().anyOf(ImmutableList.of<E>(conceptFilters!!, sourceResultFilters))
            }
        }
        if (conceptFilters != null) {
            if (tableQuery.getFilters() == null) {
                tableQuery.setFilters(conceptFilters)
            } else {
                // If both concept filters and other filters are requested, require results to match both.
                tableQuery.setFilters(
                        ResultFilters().allOf(ImmutableList.of(tableQuery.getFilters(), conceptFilters!!)))
            }
        }
    }

    @VisibleForTesting
    internal fun getCdrQuery(
            searchRequest: SearchRequest?,
            dataTableSpecification: DataTableSpecification,
            cohortReview: CohortReview?,
            conceptIds: Set<Long>?): CdrQuery {
        val cdrVersion = CdrVersionContext.getCdrVersion()
        val cdrQuery = CdrQuery()
                .bigqueryDataset(cdrVersion.bigqueryDataset)
                .bigqueryProject(cdrVersion.bigqueryProject)
        var statusFilter = dataTableSpecification.getStatusFilter()
        if (statusFilter == null) {
            statusFilter = NOT_EXCLUDED
        }
        val criteria = getParticipantCriteria(statusFilter!!, cohortReview, searchRequest)
        val tableQueryAndConfig = getTableQueryAndConfig(dataTableSpecification.getTableQuery(), conceptIds)
        cdrQuery.setColumns(tableQueryAndConfig.tableQuery.getColumns())
        if (criteria.participantIdsToInclude != null && criteria.participantIdsToInclude!!.isEmpty()) {
            // There is no cohort review, or no participants matching the status filter;
            // return a query with no SQL, indicating there should be no results.
            return cdrQuery
        }
        val jobConfiguration = fieldSetQueryBuilder.getQueryJobConfiguration(
                criteria, tableQueryAndConfig, dataTableSpecification.getMaxResults())
        cdrQuery.setSql(jobConfiguration.query)
        val configurationMap = ImmutableMap.builder<String, Any>()
        val queryConfigurationMap = ImmutableMap.builder<String, Any>()
        queryConfigurationMap.put(
                "queryParameters",
                jobConfiguration.namedParameters.entries.stream()
                        .map<Map<String, Any>>(TO_QUERY_PARAMETER_MAP)
                        .toArray())
        configurationMap.put("query", queryConfigurationMap.build())
        cdrQuery.setConfiguration(configurationMap.build())
        return cdrQuery
    }

    fun getCdrQuery(
            cohortSpec: String,
            dataTableSpecification: DataTableSpecification,
            cohortReview: CohortReview?,
            conceptIds: Set<Long>?): CdrQuery {
        val searchRequest: SearchRequest
        try {
            searchRequest = Gson().fromJson<Any>(cohortSpec, SearchRequest::class.java)
        } catch (e: JsonSyntaxException) {
            throw BadRequestException("Invalid cohort spec")
        }

        return getCdrQuery(searchRequest, dataTableSpecification, cohortReview, conceptIds)
    }

    /**
     * Materializes a cohort.
     *
     * @param cohortReview [CohortReview] representing a manual review of participants in the
     * cohort.
     * @param cohortSpec JSON representing the cohort criteria.
     * @param conceptIds an optional set of IDs for concepts used to filter results by (in addition to
     * the filtering specified in {@param cohortSpec})
     * @param request [MaterializeCohortRequest] representing the request options
     * @return [MaterializeCohortResponse] containing the results of cohort materialization
     */
    fun materializeCohort(
            cohortReview: CohortReview?,
            cohortSpec: String,
            conceptIds: Set<Long>?,
            request: MaterializeCohortRequest): MaterializeCohortResponse {
        val searchRequest: SearchRequest
        try {
            searchRequest = Gson().fromJson<Any>(cohortSpec, SearchRequest::class.java)
        } catch (e: JsonSyntaxException) {
            throw BadRequestException("Invalid cohort spec")
        }

        return materializeCohort(
                cohortReview, searchRequest, conceptIds, Objects.hash(cohortSpec, conceptIds), request)
    }

    /**
     * Materializes a cohort.
     *
     * @param cohortReview [CohortReview] representing a manual review of participants in the
     * cohort.
     * @param searchRequest [SearchRequest] representing the cohort criteria
     * @param conceptIds an optional set of IDs for concepts used to filter results by * (in addition
     * to the filtering specified in {@param searchRequest})
     * @param requestHash a number representing a stable hash of the request; used to enforce that
     * pagination tokens are used appropriately
     * @param request [MaterializeCohortRequest] representing the request
     * @return [MaterializeCohortResponse] containing the results of cohort materialization
     */
    @VisibleForTesting
    internal fun materializeCohort(
            cohortReview: CohortReview?,
            searchRequest: SearchRequest?,
            conceptIds: Set<Long>?,
            requestHash: Int,
            request: MaterializeCohortRequest): MaterializeCohortResponse {
        var offset = 0L
        val fieldSet = request.getFieldSet()
        var statusFilter = request.getStatusFilter()
        val paginationToken = request.getPageToken()
        val pageSize = request.getPageSize()
        // TODO: add CDR version ID here
        // We require the client to specify requestHash here instead of hashing searchRequest itself,
        // and use String.valueOf(statusFilter) instead of just statusFilter;
        // both searchRequest and statusFilter contain enums, which do not have stable has codes across
        // JVMs (see [RW-1149]).
        val paginationParameters = arrayOf<Any>(requestHash, statusFilter.toString())

        if (paginationToken != null) {
            val token = PaginationToken.fromBase64(paginationToken)
            if (token.matchesParameters(*paginationParameters)) {
                offset = token.offset
            } else {
                throw BadRequestException(
                        String.format("Use of pagination token %s with new parameter values", paginationToken))
            }
        }
        // Grab the next pagination token here, because statusFilter can be modified below.
        // TODO: consider pagination based on cursor / values rather than offset
        val nextToken = PaginationToken.of(offset + pageSize, *paginationParameters).toBase64()

        val limit = pageSize + 1

        val response = MaterializeCohortResponse()
        val results: Iterable<Map<String, Any>>
        if (statusFilter == null) {
            statusFilter = NOT_EXCLUDED
        }
        if (fieldSet == null || fieldSet!!.getTableQuery() != null) {
            val criteria = getParticipantCriteria(statusFilter!!, cohortReview, searchRequest)
            if (criteria.participantIdsToInclude != null && criteria.participantIdsToInclude!!.isEmpty()) {
                // There is no cohort review, or no participants matching the status filter;
                // return an empty response.
                return response
            }
            val tableQuery = if (fieldSet == null) null else fieldSet!!.getTableQuery()
            results = fieldSetQueryBuilder.materializeTableQuery(
                    getTableQueryAndConfig(tableQuery, conceptIds), criteria, limit, offset)
        } else if (fieldSet!!.getAnnotationQuery() != null) {
            if (cohortReview == null) {
                // There is no cohort review, so there are no annotations; return empty results.
                return response
            }
            results = annotationQueryBuilder
                    .materializeAnnotationQuery(
                            cohortReview, statusFilter, fieldSet!!.getAnnotationQuery(), limit, offset)
                    .results
        } else {
            throw BadRequestException("Must specify tableQuery or annotationQuery")
        }
        var numResults = 0
        var hasMoreResults = false
        val responseResults = ArrayList<Any>()
        for (resultMap in results) {
            if (numResults == pageSize) {
                hasMoreResults = true
                break
            }
            responseResults.add(resultMap)
            numResults++
        }
        response.setResults(responseResults)
        if (hasMoreResults) {
            response.setNextPageToken(nextToken)
        }
        return response
    }

    fun getAnnotations(
            cohortReview: CohortReview, request: CohortAnnotationsRequest): CohortAnnotationsResponse {
        var statusFilter = request.getStatusFilter()
        if (statusFilter == null) {
            statusFilter = NOT_EXCLUDED
        }
        val results = annotationQueryBuilder.materializeAnnotationQuery(
                cohortReview, statusFilter, request.getAnnotationQuery(), null, 0L)
        return CohortAnnotationsResponse()
                .results(ImmutableList.copyOf<E>(results.results))
                .columns(results.columns)
    }

    companion object {

        // Transforms a name, query parameter value pair into a map that will be converted into a JSON
        // dictionary representing the named query parameter value, to be used as a part of a BigQuery
        // job configuration when executing SQL queries on the client.
        // See https://cloud.google.com/bigquery/docs/parameterized-queries for JSON configuration of
        // query parameters
        private val TO_QUERY_PARAMETER_MAP = { entry ->
            val value = entry.value
            val builder = ImmutableMap.builder<String, Any>().put("name", entry.key)
            val parameterTypeMap = ImmutableMap.builder<String, Any>()
            parameterTypeMap.put("type", value.getType().toString())
            val parameterValueMap = ImmutableMap.builder<String, Any>()
            if (value.getArrayType() == null) {
                parameterValueMap.put("value", value.getValue()!!)
            } else {
                val arrayTypeMap = ImmutableMap.builder<String, Any>()
                arrayTypeMap.put("type", value.getArrayType()!!.toString())
                parameterTypeMap.put("arrayType", arrayTypeMap.build())
                val values = ImmutableList.builder<Map<String, Any>>()
                for (arrayValue in value.getArrayValues()!!) {
                    val valueMap = ImmutableMap.builder<String, Any>()
                    valueMap.put("value", arrayValue.getValue()!!)
                    values.add(valueMap.build())
                }
                parameterValueMap.put("arrayValues", values.build().toTypedArray())
            }
            builder.put("parameterType", parameterTypeMap.build())
            builder.put("parameterValue", parameterValueMap.build())
            builder.build()
        }

        @VisibleForTesting
        internal val PERSON_ID = "person_id"
        @VisibleForTesting
        internal val PERSON_TABLE = "person"

        private val NOT_EXCLUDED = Arrays.asList<CohortStatus>(
                CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW, CohortStatus.NOT_REVIEWED)
    }
}
