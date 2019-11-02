package org.pmiops.workbench.db.dao

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.TableResult
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableList.Builder
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import java.sql.Timestamp
import java.util.ArrayList
import java.util.Collections
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.StreamSupport
import javax.transaction.Transactional
import org.apache.commons.lang3.StringUtils
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CommonStorageEnums
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.DataSet
import org.pmiops.workbench.db.model.DataSetValue
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.model.DataSetRequest
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainValuePair
import org.pmiops.workbench.model.KernelTypeEnum
import org.pmiops.workbench.model.PrePackagedConceptSetEnum
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DataSetServiceImpl @Autowired
@VisibleForTesting
constructor(
        private val bigQueryService: BigQueryService,
        private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService,
        private val cohortDao: CohortDao,
        private val conceptBigQueryService: ConceptBigQueryService,
        private val conceptSetDao: ConceptSetDao,
        private val cohortQueryBuilder: CohortQueryBuilder,
        private val dataSetDao: DataSetDao) : DataSetService {

    /*
   * Stores the associated set of selects and joins for values for the data set builder,
   * pulled out of the linking table in Big Query.
   */
    @VisibleForTesting
    private class ValuesLinkingPair private constructor(private val selects: List<String>, private val joins: List<String>) {

        fun formatJoins(): String {
            return joins.stream().distinct().collect<String, *>(Collectors.joining(" "))
        }

        companion object {

            internal fun emptyPair(): ValuesLinkingPair {
                return ValuesLinkingPair(emptyList(), emptyList())
            }

            internal val JOIN_VALUE_KEY = "JOIN_VALUE"
        }
    }

    /*
   * A subclass used to store a source and a standard concept ID column name.
   */
    private class DomainConceptIdInfo internal constructor(internal val sourceConceptIdColumn: String, internal val standardConceptIdColumn: String)

    @VisibleForTesting
    class QueryAndParameters internal constructor(internal val query: String, internal val namedParameterValues: Map<String, QueryParameterValue>)

    override fun saveDataSet(
            name: String,
            includesAllParticipants: Boolean?,
            description: String,
            workspaceId: Long,
            cohortIdList: List<Long>,
            conceptIdList: List<Long>,
            values: List<DataSetValue>,
            prePackagedConceptSetEnum: PrePackagedConceptSetEnum,
            creatorId: Long,
            creationTime: Timestamp): DataSet {
        val dataSetModel = DataSet()
        dataSetModel.name = name
        dataSetModel.version = DATA_SET_VERSION
        dataSetModel.includesAllParticipants = includesAllParticipants
        dataSetModel.description = description
        dataSetModel.workspaceId = workspaceId
        dataSetModel.invalid = false
        dataSetModel.creatorId = creatorId
        dataSetModel.creationTime = creationTime
        dataSetModel.cohortIds = cohortIdList
        dataSetModel.conceptSetIds = conceptIdList
        dataSetModel.values = values
        dataSetModel.prePackagedConceptSetEnum = prePackagedConceptSetEnum

        return dataSetDao.save(dataSetModel)
    }

    override fun generateQueryJobConfigurationsByDomainName(
            dataSetRequest: DataSetRequest): Map<String, QueryJobConfiguration> {
        val includesAllParticipants = getBuiltinBooleanFromNullable(dataSetRequest.getIncludesAllParticipants())
        val cohortsSelected = ImmutableList.copyOf(this.cohortDao.findAllByCohortIdIn(dataSetRequest.getCohortIds()))
        val domainValuePairs = ImmutableList.copyOf(dataSetRequest.getDomainValuePairs())

        val expandedSelectedConceptSets = getExpandedConceptSetSelections(
                dataSetRequest.getPrePackagedConceptSet(),
                dataSetRequest.getConceptSetIds(),
                cohortsSelected,
                includesAllParticipants,
                domainValuePairs)

        // Below constructs the union of all cohort queries
        val queryMapEntries = cohortsSelected.stream()
                .map<QueryAndParameters>(Function<Cohort, QueryAndParameters> { this.getCohortQueryStringAndCollectNamedParameters(it) })
                .collect<ImmutableList<QueryAndParameters>, Any>(ImmutableList.toImmutableList())

        val unionedCohortQueries = queryMapEntries.stream()
                .map<String>(Function<QueryAndParameters, String> { it.getQuery() })
                .collect<String, *>(Collectors.joining(" UNION DISTINCT "))

        val domainSet = domainValuePairs.stream()
                .map(Function<DomainValuePair, Any> { DomainValuePair.getDomain() })
                .collect(ImmutableSet.toImmutableSet<Any>())

        // now merge all the individual maps from each configuration
        val mergedQueryParameterValues = queryMapEntries.stream()
                .map<Map<String, QueryParameterValue>>(Function<QueryAndParameters, Map<String, QueryParameterValue>> { it.getNamedParameterValues() })
                .flatMap<Entry<String, QueryParameterValue>> { m -> m.entries.stream() }
                .collect<ImmutableMap<String, QueryParameterValue>, Any>(ImmutableMap.toImmutableMap<Entry<String, QueryParameterValue>, String, QueryParameterValue>(Function<Entry<String, QueryParameterValue>, String> { it.key }, Function<Entry<String, QueryParameterValue>, QueryParameterValue> { it.value }))

        return buildQueriesByDomain(
                domainSet,
                domainValuePairs,
                mergedQueryParameterValues,
                includesAllParticipants,
                expandedSelectedConceptSets,
                unionedCohortQueries)
    }

    // note: ImmutableList is OK return type on private methods, but should be avoided in public
    // signatures.
    private fun getExpandedConceptSetSelections(
            prePackagedConceptSet: PrePackagedConceptSetEnum,
            conceptSetIds: List<Long>,
            selectedCohorts: List<Cohort>,
            includesAllParticipants: Boolean,
            domainValuePairs: List<DomainValuePair>): ImmutableList<ConceptSet> {
        val initialSelectedConceptSets = ImmutableList.copyOf(this.conceptSetDao.findAllByConceptSetIdIn(conceptSetIds))
        val noCohortsIncluded = selectedCohorts.isEmpty() && !includesAllParticipants
        if (noCohortsIncluded || hasNoConcepts(prePackagedConceptSet, domainValuePairs, initialSelectedConceptSets)) {
            throw BadRequestException("Data Sets must include at least one cohort and concept.")
        }

        val selectedConceptSetsBuilder = ImmutableList.builder<org.pmiops.workbench.db.model.ConceptSet>()
        selectedConceptSetsBuilder.addAll(initialSelectedConceptSets)

        // If pre packaged all survey concept set is selected create a temp concept set with concept ids
        // of all survey questions
        if (CONCEPT_SETS_NEEDING_PREPACKAGED_SURVEY.contains(prePackagedConceptSet)) {
            selectedConceptSetsBuilder.add(buildPrePackagedSurveyConceptSet())
        }
        return selectedConceptSetsBuilder.build()
    }

    @VisibleForTesting
    fun getCohortQueryStringAndCollectNamedParameters(cohortDbModel: Cohort): QueryAndParameters {
        val cohortDefinition = cohortDbModel.criteria
                ?: throw NotFoundException(
                        String.format(
                                "Not Found: No Cohort definition matching cohortId: %s",
                                cohortDbModel.cohortId))
        val searchRequest = Gson().fromJson<Any>(cohortDefinition, SearchRequest::class.java)
        val participantIdQuery = cohortQueryBuilder.buildParticipantIdQuery(ParticipantCriteria(searchRequest))
        val participantQueryConfig = bigQueryService.filterBigQueryConfig(participantIdQuery)
        val participantQuery = AtomicReference(participantQueryConfig.query)
        val cohortNamedParametersBuilder = ImmutableMap.Builder<String, QueryParameterValue>()
        participantQueryConfig
                .namedParameters
                .forEach { (npKey, npValue) ->
                    val newKey = buildReplacementKey(cohortDbModel, npKey)
                    // replace the original key (when found as a word)
                    participantQuery.getAndSet(
                            participantQuery.get().replace("\\b$npKey\\b".toRegex(), newKey))
                    cohortNamedParametersBuilder.put(newKey, npValue)
                }
        return QueryAndParameters(participantQuery.get(), cohortNamedParametersBuilder.build())
    }

    // Build a snake_case parameter key from this named parameter key and cohort ID.
    private fun buildReplacementKey(cohort: Cohort, npKey: String): String {
        return String.format("%s_%d", npKey, cohort.cohortId)
    }

    private fun buildQueriesByDomain(
            uniqueDomains: ImmutableSet<Domain>,
            domainValuePairs: ImmutableList<DomainValuePair>,
            cohortParameters: ImmutableMap<String, QueryParameterValue>,
            includesAllParticipants: Boolean,
            conceptSetsSelected: ImmutableList<ConceptSet>,
            cohortQueries: String): Map<String, QueryJobConfiguration> {
        val bigQuerySchemaConfig = cdrBigQuerySchemaConfigService.config

        return uniqueDomains.stream()
                .collect(
                        ImmutableMap.toImmutableMap(
                                Function<Any, Any> { Domain.toString() },
                                { domain ->
                                    buildQueryJobConfigForDomain(
                                            domain,
                                            domainValuePairs,
                                            cohortParameters,
                                            includesAllParticipants,
                                            conceptSetsSelected,
                                            cohortQueries,
                                            bigQuerySchemaConfig)
                                }))
    }

    private fun buildQueryJobConfigForDomain(
            domain: Domain,
            domainValuePairs: List<DomainValuePair>,
            cohortParameters: Map<String, QueryParameterValue>,
            includesAllParticipants: Boolean,
            conceptSetsSelected: List<ConceptSet>,
            cohortQueries: String,
            bigQuerySchemaConfig: CdrBigQuerySchemaConfig): QueryJobConfiguration {
        validateConceptSetSelection(domain, conceptSetsSelected)

        val queryBuilder = StringBuilder("SELECT ")
        val personIdQualified = getQualifiedColumnName(domain, PERSON_ID_COLUMN_NAME)

        val domainValuePairsForCurrentDomain = domainValuePairs.stream()
                .filter { dvp -> dvp.getDomain() === domain }
                .collect(Collectors.toList<Any>())

        val valuesLinkingPair = getValueSelectsAndJoins(domainValuePairsForCurrentDomain)

        queryBuilder
                .append(valuesLinkingPair.selects.joinToString(", "))
                .append(" ")
                .append(valuesLinkingPair.formatJoins())

        val conceptSetSqlInClauseMaybe = buildConceptIdListClause(domain, conceptSetsSelected)

        if (supportsConceptSets(domain)) {
            val domainConceptIdInfoMaybe = bigQuerySchemaConfig.cohortTables.values.stream()
                    .filter { config -> domain.toString().equals(config.domain) }
                    .map { tableConfig ->
                        DomainConceptIdInfo(
                                getColumnName(tableConfig, "source"),
                                getColumnName(tableConfig, "standard"))
                    }
                    .findFirst()

            val domainConceptIdInfo = domainConceptIdInfoMaybe.orElseThrow {
                ServerErrorException(
                        String.format(
                                "Couldn't find source and standard columns for domain: %s",
                                domain.toString()))
            }

            // This adds the where clauses for cohorts and concept sets.
            conceptSetSqlInClauseMaybe.ifPresent { clause ->
                queryBuilder
                        .append(" WHERE \n(")
                        .append(domainConceptIdInfo.standardConceptIdColumn)
                        .append(clause)
                        .append(" OR \n")
                        .append(domainConceptIdInfo.sourceConceptIdColumn)
                        .append(clause)
                        .append(")")
            }

            if (!includesAllParticipants) {
                queryBuilder
                        .append(" \nAND (")
                        .append(personIdQualified)
                        .append(" IN (")
                        .append(cohortQueries)
                        .append("))")
            }
        } else if (!includesAllParticipants) {
            queryBuilder
                    .append(" \nWHERE ")
                    .append(personIdQualified)
                    .append(" IN (")
                    .append(cohortQueries)
                    .append(")")
        }

        val completeQuery = queryBuilder.toString()

        return buildQueryJobConfiguration(cohortParameters, completeQuery)
    }

    private fun validateConceptSetSelection(domain: Domain, conceptSetsSelected: List<ConceptSet>) {
        if (supportsConceptSets(domain) && !conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(conceptSetsSelected)) {
            throw BadRequestException("Concept Sets must contain at least one concept")
        }
    }

    // In some cases, we don't require concept IDs, and in others their absense is fatal.
    // Even if Concept IDs have been selected, these don't work with all domains.
    @VisibleForTesting
    fun buildConceptIdListClause(
            domain: Domain, conceptSetsSelected: List<ConceptSet>): Optional<String> {
        val conceptSetSqlInClauseMaybe: Optional<String>
        if (supportsConceptSets(domain)) {
            conceptSetSqlInClauseMaybe = buildConceptIdSqlInClause(domain, conceptSetsSelected)
        } else {
            conceptSetSqlInClauseMaybe = Optional.empty()
        }
        return conceptSetSqlInClauseMaybe
    }

    private fun supportsConceptSets(domain: Domain): Boolean {
        return domain !== Domain.PERSON
    }

    // Gather all the concept IDs from the ConceptSets provided, taking account of
    // domain-specific rules.
    private fun buildConceptIdSqlInClause(domain: Domain, conceptSets: List<ConceptSet>): Optional<String> {
        val conceptSetIDs = conceptSets.stream()
                .filter { cs -> domain === cs.domainEnum }
                .flatMap { cs -> cs.conceptIds.stream().map { cid -> java.lang.Long.toString(cid!!) } }
                .collect<String, *>(Collectors.joining(", "))
        return if (conceptSetIDs.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(String.format(" IN (%s)", conceptSetIDs))
        }
    }

    private fun buildQueryJobConfiguration(
            namedCohortParameters: Map<String, QueryParameterValue>, query: String): QueryJobConfiguration {
        return bigQueryService.filterBigQueryConfig(
                QueryJobConfiguration.newBuilder(query)
                        .setNamedParameters(namedCohortParameters)
                        .setUseLegacySql(false)
                        .build())
    }

    @VisibleForTesting
    fun conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
            conceptSetsSelected: List<ConceptSet>): Boolean {
        return if (conceptSetsSelected.isEmpty()) {
            false
        } else conceptSetsSelected.stream()
                .collect<Map<Short, List<ConceptSet>>, Any>(Collectors.groupingBy<ConceptSet, Short, Any, List<ConceptSet>>(Function<ConceptSet, Short> { it.getDomain() }, Collectors.toList()))
                .values
                .stream()
                .map { csl -> csl.stream().mapToLong { cs -> cs.conceptIds.size }.sum() }
                .allMatch { count -> count > 0 }
    }

    private fun getQualifiedColumnName(currentDomain: Domain, columnName: String): String {
        val tableAbbreviation = DOMAIN_TO_BASE_TABLE_SHORTHAND[currentDomain]
        return if (tableAbbreviation == null)
            columnName
        else
            String.format("%s.%s", tableAbbreviation, columnName)
    }

    override fun generateCodeCells(
            kernelTypeEnum: KernelTypeEnum,
            dataSetName: String,
            qualifier: String,
            queryJobConfigurationMap: Map<String, QueryJobConfiguration>): List<String> {
        val prerequisites: String
        when (kernelTypeEnum) {
            R -> prerequisites = ("if(! \"reticulate\" %in% installed.packages()) { install.packages(\"reticulate\") }\n"
                    + "library(reticulate)\n"
                    + "pd <- reticulate::import(\"pandas\")")
            PYTHON -> prerequisites = "import pandas"
            else -> throw BadRequestException(
                    "Kernel Type " + kernelTypeEnum.toString() + " not supported")
        }
        return queryJobConfigurationMap.entries.stream()
                .map { entry ->
                    (prerequisites
                            + "\n\n"
                            + generateNotebookUserCode(
                            entry.value,
                            Domain.fromValue(entry.key),
                            dataSetName,
                            qualifier,
                            kernelTypeEnum))
                }
                .collect<List<String>, Any>(Collectors.toList())
    }

    @Transactional
    override fun cloneDataSetToWorkspace(
            fromDataSet: DataSet, toWorkspace: Workspace, cohortIds: Set<Long>, conceptSetIds: Set<Long>): DataSet {
        val toDataSet = DataSet(fromDataSet)
        toDataSet.workspaceId = toWorkspace.workspaceId
        toDataSet.creatorId = toWorkspace.creator!!.userId
        toDataSet.lastModifiedTime = toWorkspace.lastModifiedTime
        toDataSet.creationTime = toWorkspace.creationTime

        toDataSet.conceptSetIds = ArrayList(conceptSetIds)
        toDataSet.cohortIds = ArrayList(cohortIds)
        return dataSetDao.save(toDataSet)
    }

    override fun getDataSets(workspace: Workspace): List<DataSet> {
        // Allows for fetching data sets for a workspace once its collection is no longer
        // bound to a session.
        return dataSetDao.findByWorkspaceId(workspace.workspaceId)
    }

    @Transactional
    override fun getConceptSetsForDataset(dataSet: DataSet): List<ConceptSet> {
        return conceptSetDao.findAllByConceptSetIdIn(
                dataSetDao.findOne(dataSet.dataSetId).cohortIds)
    }

    @Transactional
    override fun getCohortsForDataset(dataSet: DataSet): List<Cohort> {
        return cohortDao.findAllByCohortIdIn(dataSetDao.findOne(dataSet.dataSetId).cohortIds)
    }

    private fun getColumnName(config: CdrBigQuerySchemaConfig.TableConfig, type: String): String {
        val conceptColumn = config.columns.stream().filter { column -> type == column.domainConcept }.findFirst()
        if (!conceptColumn.isPresent) {
            throw ServerErrorException("Domain not supported")
        }
        return conceptColumn.get().name
    }

    private fun getValueSelectsAndJoins(domainValuePairs: List<DomainValuePair>): ValuesLinkingPair {
        val domainMaybe = domainValuePairs.stream().map(Function<DomainValuePair, Any> { DomainValuePair.getDomain() }).findFirst()
        if (!domainMaybe.isPresent()) {
            return ValuesLinkingPair.emptyPair()
        }

        val valuesUppercaseBuilder = Builder<String>()
        valuesUppercaseBuilder.add("CORE_TABLE_FOR_DOMAIN")
        valuesUppercaseBuilder.addAll(
                domainValuePairs.stream()
                        .map(Function<DomainValuePair, Any> { DomainValuePair.getValue() })
                        .map(Function<Any, Any> { toUpperCase() })
                        .collect(Collectors.toList<T>()))

        val domainName = domainMaybe.get().toString()
        val domainFirstCharacterCapitalized = capitalizeFirstCharacterOnly(domainName)

        val queryParameterValuesByDomain = ImmutableMap.of(
                "pDomain", QueryParameterValue.string(domainFirstCharacterCapitalized),
                "pValuesList",
                QueryParameterValue.array(
                        valuesUppercaseBuilder.build().toTypedArray(), String::class.java))

        val valuesLinkingTableResult = bigQueryService.executeQuery(
                buildQueryJobConfiguration(
                        queryParameterValuesByDomain,
                        SELECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST))

        val valueSelects = StreamSupport.stream<FieldValueList>(valuesLinkingTableResult.values.spliterator(), false)
                .filter { fieldValue -> fieldValue.get("OMOP_SQL").getStringValue() != "CORE_TABLE_FOR_DOMAIN" }
                .map<String> { fieldValue -> fieldValue.get("OMOP_SQL").getStringValue() }
                .collect<ImmutableList<String>, Any>(ImmutableList.toImmutableList())

        val valueJoins = StreamSupport.stream<FieldValueList>(valuesLinkingTableResult.values.spliterator(), false)
                .map<String> { fieldValue -> fieldValue.get(ValuesLinkingPair.JOIN_VALUE_KEY).getStringValue() }
                .collect<ImmutableList<String>, Any>(ImmutableList.toImmutableList())

        return ValuesLinkingPair(valueSelects, valueJoins)
    }

    private fun buildPrePackagedSurveyConceptSet(): ConceptSet {
        val conceptIds = ImmutableList.copyOf(conceptBigQueryService.surveyQuestionConceptIds)
        val surveyConceptSet = ConceptSet()
        surveyConceptSet.name = "All Surveys"
        surveyConceptSet.domain = CommonStorageEnums.domainToStorage(Domain.SURVEY)
        surveyConceptSet.conceptIds = ImmutableSet.copyOf(conceptIds)
        return surveyConceptSet
    }

    companion object {

        private val SELECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST = "SELECT * FROM `\${projectId}.\${dataSetId}.ds_linking` " + "WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME in unnest(@pValuesList)"
        private val CONCEPT_SETS_NEEDING_PREPACKAGED_SURVEY = ImmutableSet.of<PrePackagedConceptSetEnum>(PrePackagedConceptSetEnum.SURVEY, PrePackagedConceptSetEnum.BOTH)

        private val PERSON_ID_COLUMN_NAME = "PERSON_ID"
        private val DATA_SET_VERSION = 1

        // For domains for which we've assigned a base table in BigQuery, we keep a map here
        // so that we can use the base table name to qualify, for example, PERSON_ID in clauses
        // after SELECT. This map is nearly identical to
        // org.pmiops.workbench.db.dao.ConceptSetDao.DOMAIN_TO_TABLE_NAME,
        // but in some cases we use a different shorthand than the base table name.
        private val DOMAIN_TO_BASE_TABLE_SHORTHAND = ImmutableMap.Builder<Domain, String>()
                .put(Domain.CONDITION, "c_occurrence")
                .put(Domain.DEATH, "death")
                .put(Domain.DRUG, "d_exposure")
                .put(Domain.MEASUREMENT, "measurement")
                .put(Domain.OBSERVATION, "observation")
                .put(Domain.PERSON, "person")
                .put(Domain.PROCEDURE, "procedure")
                .put(Domain.SURVEY, "answer")
                .put(Domain.VISIT, "visit")
                .build()

        private fun hasNoConcepts(
                prePackagedConceptSet: PrePackagedConceptSetEnum,
                domainValuePairs: List<DomainValuePair>,
                initialSelectedConceptSets: ImmutableList<ConceptSet>): Boolean {
            return (initialSelectedConceptSets.isEmpty()
                    && domainValuePairs.isEmpty()
                    && prePackagedConceptSet.equals(PrePackagedConceptSetEnum.NONE))
        }

        // Capitalizes the first letter of a string and lowers the remaining ones.
        // Assumes a single word, so you'd get "A tale of two cities" instead of
        // "A Tale Of Two Cities"
        @VisibleForTesting
        fun capitalizeFirstCharacterOnly(text: String): String {
            return StringUtils.capitalize(text.toLowerCase())
        }

        private fun generateNotebookUserCode(
                queryJobConfiguration: QueryJobConfiguration,
                domain: Domain,
                dataSetName: String,
                qualifier: String,
                kernelTypeEnum: KernelTypeEnum): String {

            // Define [namespace]_sql, [namespace]_query_config, and [namespace]_df variables
            val domainAsString = domain.toString().toLowerCase()
            val namespace = "dataset_" + qualifier + "_" + domainAsString + "_"
            // Comments in R and Python have the same syntax
            val descriptiveComment = StringBuilder("# This query represents dataset \"")
                    .append(dataSetName)
                    .append("\" for domain \"")
                    .append(domainAsString)
                    .append("\"")
                    .toString()
            val sqlSection: String
            val namedParamsSection: String
            val dataFrameSection: String
            val displayHeadSection: String

            when (kernelTypeEnum) {
                PYTHON -> {
                    sqlSection = namespace + "sql = \"\"\"" + queryJobConfiguration.query + "\"\"\""
                    namedParamsSection = (namespace
                            + "query_config = {\n"
                            + "  \'query\': {\n"
                            + "  \'parameterMode\': \'NAMED\',\n"
                            + "  \'queryParameters\': [\n"
                            + queryJobConfiguration.namedParameters.entries.stream()
                            .map { entry ->
                                convertNamedParameterToString(
                                        entry.key, entry.value, KernelTypeEnum.PYTHON)
                            }
                            .collect<String, *>(Collectors.joining(",\n"))
                            + "\n"
                            + "    ]\n"
                            + "  }\n"
                            + "}\n\n")
                    dataFrameSection = (namespace
                            + "df = pandas.read_gbq("
                            + namespace
                            + "sql, dialect=\"standard\", configuration="
                            + namespace
                            + "query_config)")
                    displayHeadSection = namespace + "df.head(5)"
                }
                R -> {
                    sqlSection = namespace + "sql <- \"" + queryJobConfiguration.query + "\""
                    namedParamsSection = (namespace
                            + "query_config <- list(\n"
                            + "  query = list(\n"
                            + "    parameterMode = 'NAMED',\n"
                            + "    queryParameters = list(\n"
                            + queryJobConfiguration.namedParameters.entries.stream()
                            .map { entry ->
                                convertNamedParameterToString(
                                        entry.key, entry.value, KernelTypeEnum.R)
                            }
                            .collect<String, *>(Collectors.joining(",\n"))
                            + "\n"
                            + "    )\n"
                            + "  )\n"
                            + ")")
                    dataFrameSection = (namespace
                            + "df <- pd\$read_gbq("
                            + namespace
                            + "sql, dialect=\"standard\", configuration="
                            + namespace
                            + "query_config)")
                    displayHeadSection = "head(" + namespace + "df, 5)"
                }
                else -> throw BadRequestException("Language " + kernelTypeEnum.toString() + " not supported.")
            }

            return (descriptiveComment
                    + "\n"
                    + sqlSection
                    + "\n\n"
                    + namedParamsSection
                    + "\n\n"
                    + dataFrameSection
                    + "\n\n"
                    + displayHeadSection)
        }

        // BigQuery api returns parameter values either as a list or an object.
        // To avoid warnings on our cast to list, we suppress those warnings here,
        // as they are expected.
        private fun convertNamedParameterToString(
                key: String, namedParameterValue: QueryParameterValue?, kernelTypeEnum: KernelTypeEnum): String {
            if (namedParameterValue == null) {
                return ""
            }
            val isArrayParameter = namedParameterValue.arrayType != null

            val arrayValues = nullableListToEmpty(namedParameterValue.arrayValues)

            when (kernelTypeEnum) {
                PYTHON -> return buildPythonNamedParameterQuery(
                        key, namedParameterValue, isArrayParameter, arrayValues)
                R -> return ("      list(\n"
                        + "        name = \""
                        + key
                        + "\",\n"
                        + "        parameterType = list(type = \""
                        + namedParameterValue.type.toString()
                        + "\""
                        + (if (isArrayParameter)
                    ", arrayType = list(type = \"" + namedParameterValue.arrayType + "\")"
                else
                    "")
                        + "),\n"
                        + "        parameterValue = list("
                        + (if (isArrayParameter)
                    "arrayValues = list("
                            + arrayValues.stream()
                            .map { arrayValue -> "list(value = " + arrayValue.value + ")" }
                            .collect<String, *>(Collectors.joining(","))
                            + ")"
                else
                    "value = \"" + namedParameterValue.value + "\"")
                        + ")\n"
                        + "      )")
                else -> throw BadRequestException("Language not supported")
            }
        }

        private fun buildPythonNamedParameterQuery(
                key: String,
                namedParameterValue: QueryParameterValue,
                isArrayParameter: Boolean,
                arrayValues: List<QueryParameterValue>): String {
            return ("      {\n"
                    + "        'name': \""
                    + key
                    + "\",\n"
                    + "        'parameterType': {'type': \""
                    + namedParameterValue.type.toString()
                    + "\""
                    + (if (isArrayParameter)
                ",'arrayType': {'type': \"" + namedParameterValue.arrayType + "\"},"
            else
                "")
                    + "},\n"
                    + "        \'parameterValue\': {"
                    + (if (isArrayParameter)
                "\'arrayValues\': ["
                        + arrayValues.stream()
                        .map { arrayValue -> "{\'value\': " + arrayValue.value + "}" }
                        .collect<String, *>(Collectors.joining(","))
                        + "]"
            else
                "'value': \"" + namedParameterValue.value + "\"")
                    + "}\n"
                    + "      }")
        }

        private fun <T> nullableListToEmpty(nullableList: List<T>?): List<T> {
            return Optional.ofNullable(nullableList).orElse(ArrayList())
        }

        private fun getBuiltinBooleanFromNullable(boo: Boolean?): Boolean {
            return Optional.ofNullable(boo).orElse(false)
        }
    }
}
