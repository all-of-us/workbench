package org.pmiops.workbench.api

import com.google.common.collect.ImmutableList.toImmutableList

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import java.sql.Date
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Objects
import java.util.Optional
import java.util.function.Function
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.StreamSupport
import javax.inject.Provider
import javax.persistence.OptimisticLockException
import org.apache.commons.lang3.RandomStringUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.dataset.DataSetMapper
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao
import org.pmiops.workbench.db.dao.DataSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.model.*
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.GatewayTimeoutException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.DataDictionaryEntry
import org.pmiops.workbench.model.DataSet
import org.pmiops.workbench.model.DataSetCodeResponse
import org.pmiops.workbench.model.DataSetExportRequest
import org.pmiops.workbench.model.DataSetListResponse
import org.pmiops.workbench.model.DataSetPreviewRequest
import org.pmiops.workbench.model.DataSetPreviewResponse
import org.pmiops.workbench.model.DataSetPreviewValueList
import org.pmiops.workbench.model.DataSetRequest
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainValuePair
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.KernelTypeEnum
import org.pmiops.workbench.model.MarkDataSetRequest
import org.pmiops.workbench.model.PrePackagedConceptSetEnum
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.notebooks.NotebooksService
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DataSetController @Autowired
internal constructor(
        private val bigQueryService: BigQueryService,
        private val clock: Clock,
        private val cdrVersionDao: CdrVersionDao,
        private val cohortDao: CohortDao,
        private val conceptDao: ConceptDao,
        private val conceptSetDao: ConceptSetDao,
        private val dataDictionaryEntryDao: DataDictionaryEntryDao,
        private val dataSetDao: DataSetDao,
        private val dataSetMapper: DataSetMapper,
        private val dataSetService: DataSetService,
        private val fireCloudService: FireCloudService,
        private val notebooksService: NotebooksService,
        private val userProvider: Provider<User>,
        private val workspaceService: WorkspaceService) : DataSetApiDelegate {

    private val TO_CLIENT_DATA_SET = Function<org.pmiops.workbench.db.model.DataSet, Any> { dataSet ->
        val result = DataSet()
                .name(dataSet.name)
                .includesAllParticipants(dataSet.includesAllParticipants)
                .id(dataSet.dataSetId)
                .etag(Etags.fromVersion(dataSet.version))
                .description(dataSet.description)
                .prePackagedConceptSet(dataSet.prePackagedConceptSetEnum)
        if (dataSet.lastModifiedTime != null) {
            result.setLastModifiedTime(dataSet.lastModifiedTime!!.time)
        }
        result.setConceptSets(
                StreamSupport.stream(
                        conceptSetDao
                                .findAll(
                                        dataSet.conceptSetIds!!.stream()
                                                .filter(Predicate<Long> { Objects.nonNull(it) })
                                                .collect<List<Long>, Any>(Collectors.toList()))
                                .spliterator(),
                        false)
                        .map<Any> { conceptSet -> toClientConceptSet(conceptSet) }
                        .collect<R, A>(Collectors.toList<T>()))
        result.setCohorts(
                StreamSupport.stream(cohortDao.findAll(dataSet.cohortIds).spliterator(), false)
                        .map(CohortsController.TO_CLIENT_COHORT)
                        .collect<R, A>(Collectors.toList<T>()))
        result.setDomainValuePairs(
                dataSet.values!!.stream()
                        .map(TO_CLIENT_DOMAIN_VALUE)
                        .collect(Collectors.toList<T>()))
        result
    }

    fun createDataSet(
            workspaceNamespace: String, workspaceFirecloudName: String, dataSetRequest: DataSetRequest): ResponseEntity<DataSet> {
        validateDataSetCreateRequest(dataSetRequest)
        val now = Timestamp(clock.instant().toEpochMilli())
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceFirecloudName, WorkspaceAccessLevel.WRITER)
        val workspaceId = workspaceService.get(workspaceNamespace, workspaceFirecloudName).workspaceId
        val dataSetValueList = dataSetRequest.getDomainValuePairs().stream()
                .map(???({ this.getDataSetValuesFromDomainValueSet(it) }))
        .collect(toImmutableList<E>())
        try {
            val savedDataSet = dataSetService.saveDataSet(
                    dataSetRequest.getName(),
                    dataSetRequest.getIncludesAllParticipants(),
                    dataSetRequest.getDescription(),
                    workspaceId,
                    dataSetRequest.getCohortIds(),
                    dataSetRequest.getConceptSetIds(),
                    dataSetValueList,
                    dataSetRequest.getPrePackagedConceptSet(),
                    userProvider.get().userId,
                    now)
            return ResponseEntity.ok<DataSet>(TO_CLIENT_DATA_SET.apply(savedDataSet))
        } catch (ex: DataIntegrityViolationException) {
            throw ConflictException("Data set with the same name already exists")
        }

    }

    private fun getDataSetValuesFromDomainValueSet(domainValuePair: DomainValuePair): DataSetValue {
        return DataSetValue(
                CommonStorageEnums.domainToStorage(domainValuePair.getDomain())!!.toString(),
                domainValuePair.getValue())
    }

    private fun validateDataSetCreateRequest(dataSetRequest: DataSetRequest) {
        val includesAllParticipants = Optional.ofNullable(dataSetRequest.getIncludesAllParticipants()).orElse(false)
        if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
            throw BadRequestException("Missing name")
        } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().isEmpty() && dataSetRequest.getPrePackagedConceptSet().equals(PrePackagedConceptSetEnum.NONE)) {
            throw BadRequestException("Missing concept set ids")
        } else if ((dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().isEmpty()) && !includesAllParticipants) {
            throw BadRequestException("Missing cohort ids")
        } else if (dataSetRequest.getDomainValuePairs() == null || dataSetRequest.getDomainValuePairs().isEmpty()) {
            throw BadRequestException("Missing values")
        }
    }

    private fun toClientConceptSet(conceptSet: org.pmiops.workbench.db.model.ConceptSet): ConceptSet {
        val result = ConceptSetsController.TO_CLIENT_CONCEPT_SET.apply(conceptSet)
        if (!conceptSet.conceptIds.isEmpty()) {
            val concepts = conceptDao.findAll(conceptSet.conceptIds)
            result.setConcepts(
                    StreamSupport.stream<Concept>(concepts.spliterator(), false)
                            .map(ConceptsController.TO_CLIENT_CONCEPT)
                            .collect<R, A>(Collectors.toList<T>()))
        }
        return result
    }

    @VisibleForTesting
    fun generateRandomEightCharacterQualifier(): String {
        return RandomStringUtils.randomNumeric(8)
    }

    fun generateCode(
            workspaceNamespace: String,
            workspaceId: String,
            kernelTypeEnumString: String,
            dataSetRequest: DataSetRequest): ResponseEntity<DataSetCodeResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val kernelTypeEnum = KernelTypeEnum.fromValue(kernelTypeEnumString)

        // Generate query per domain for the selected concept set, cohort and values
        // TODO(jaycarlton): return better error information form this function for common validation
        // scenarios
        val bigQueryJobConfigsByDomain = dataSetService.generateQueryJobConfigurationsByDomainName(dataSetRequest)

        if (bigQueryJobConfigsByDomain.isEmpty()) {
            log.warning("Empty query map generated for this DataSetRequest")
        }

        val qualifier = generateRandomEightCharacterQualifier()

        val codeCells = ImmutableList.copyOf(
                dataSetService.generateCodeCells(
                        kernelTypeEnum, dataSetRequest.getName(), qualifier, bigQueryJobConfigsByDomain))
        val generatedCode = codeCells.joinToString("\n\n")

        return ResponseEntity.ok(
                DataSetCodeResponse().code(generatedCode).kernelType(kernelTypeEnum))
    }

    // TODO (srubenst): Delete this method and make generate query take the composite parts.
    private fun generateDataSetRequestFromPreviewRequest(
            dataSetPreviewRequest: DataSetPreviewRequest): DataSetRequest {
        return DataSetRequest()
                .name("Does not matter")
                .conceptSetIds(dataSetPreviewRequest.getConceptSetIds())
                .cohortIds(dataSetPreviewRequest.getCohortIds())
                .prePackagedConceptSet(dataSetPreviewRequest.getPrePackagedConceptSet())
                .includesAllParticipants(dataSetPreviewRequest.getIncludesAllParticipants())
                .domainValuePairs(
                        dataSetPreviewRequest.getValues().stream()
                                .map(
                                        { value ->
                                            DomainValuePair()
                                                    .domain(dataSetPreviewRequest.getDomain())
                                                    .value(value)
                                        })
                                .collect(Collectors.toList<T>()))
    }

    fun previewDataSetByDomain(
            workspaceNamespace: String, workspaceId: String, dataSetPreviewRequest: DataSetPreviewRequest): ResponseEntity<DataSetPreviewResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)
        val previewQueryResponse = DataSetPreviewResponse()
        val dataSetRequest = generateDataSetRequestFromPreviewRequest(dataSetPreviewRequest)
        val bigQueryJobConfig = dataSetService.generateQueryJobConfigurationsByDomainName(dataSetRequest)

        if (bigQueryJobConfig.size > 1) {
            throw BadRequestException(
                    "There should never be a preview request with more than one domain")
        }
        val valuePreviewList = ArrayList<DataSetPreviewValueList>()
        var queryJobConfiguration = bigQueryJobConfig[dataSetPreviewRequest.getDomain().toString()]

        val originalQuery = queryJobConfiguration.getQuery()
        val queryResponse: TableResult
        try {
            val query = "$originalQuery LIMIT $NO_OF_PREVIEW_ROWS"

            queryJobConfiguration = queryJobConfiguration.toBuilder().setQuery(query).build()

            /* Google appengine has a 60 second timeout, we want to make sure this endpoint completes
       * before that limit is exceeded, or we get a 500 error with the following type:
       * com.google.apphosting.runtime.HardDeadlineExceededError
       * See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
       */
            queryResponse = bigQueryService.executeQuery(
                    bigQueryService.filterBigQueryConfig(queryJobConfiguration),
                    APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC)
        } catch (ex: Exception) {
            if (ex.cause != null
                    && ex.cause.message != null
                    && ex.cause.message.contains("Read timed out")) {
                throw GatewayTimeoutException(
                        "Timeout while querying the CDR to pull preview information.")
            } else {
                throw ex
            }
        }

        valuePreviewList.addAll(
                queryResponse.schema.fields.stream()
                        .map<Any> { fields -> DataSetPreviewValueList().value(fields.name) }
                        .collect<List<Any>, Any>(Collectors.toList()))

        queryResponse
                .values
                .forEach { fieldValueList -> addFieldValuesFromBigQueryToPreviewList(valuePreviewList, fieldValueList) }

        queryResponse
                .schema
                .fields
                .forEach { fields -> formatTimestampValues(valuePreviewList, fields) }

        Collections.sort(
                valuePreviewList,
                Comparator.comparing<Any, Any> { item -> dataSetPreviewRequest.getValues().indexOf(item.getValue()) })

        previewQueryResponse.setDomain(dataSetPreviewRequest.getDomain())
        previewQueryResponse.setValues(valuePreviewList)
        return ResponseEntity.ok<DataSetPreviewResponse>(previewQueryResponse)
    }

    @VisibleForTesting
    fun addFieldValuesFromBigQueryToPreviewList(
            valuePreviewList: List<DataSetPreviewValueList>, fieldValueList: FieldValueList) {
        IntStream.range(0, fieldValueList.size)
                .forEach { columnNumber ->
                    valuePreviewList[columnNumber]
                            .addQueryValueItem(
                                    Optional.ofNullable(fieldValueList[columnNumber].value)
                                            .map { it.toString() }
                                            .orElse(EMPTY_CELL_MARKER))
                }
    }

    // Iterates through all values associated with a specific field, and converts all timestamps
    // to a timestamp formatted string.
    private fun formatTimestampValues(valuePreviewList: List<DataSetPreviewValueList>, field: Field) {
        val previewValue = valuePreviewList.stream()
                .filter { preview -> preview.getValue().equalsIgnoreCase(field.name) }
                .findFirst()
                .orElseThrow {
                    IllegalStateException(
                            "Value should be present when it is not in dataset preview request")
                }
        if (field.type === LegacySQLTypeName.TIMESTAMP) {
            val queryValues = ArrayList<String>()
            val dateFormat = SimpleDateFormat(DATE_FORMAT_STRING)
            previewValue
                    .getQueryValue()
                    .forEach { value ->
                        if (!value.equals(EMPTY_CELL_MARKER)) {
                            val fieldValue = java.lang.Double.parseDouble(value)
                            queryValues.add(dateFormat.format(Date(fieldValue.toLong())))
                        } else {
                            queryValues.add(value)
                        }
                    }
            previewValue.setQueryValue(queryValues)
        }
    }

    fun exportToNotebook(
            workspaceNamespace: String, workspaceId: String, dataSetExportRequest: DataSetExportRequest): ResponseEntity<EmptyResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        // This suppresses 'may not be initialized errors. We will always init to something else before
        // used.
        var notebookFile = JSONObject()
        val workspace = fireCloudService.getWorkspace(workspaceNamespace, workspaceId)
        val metaData = JSONObject()

        if (!dataSetExportRequest.getNewNotebook()) {
            notebookFile = notebooksService.getNotebookContents(
                    workspace.getWorkspace().getBucketName(), dataSetExportRequest.getNotebookName())
            try {
                val language = Optional.of(notebookFile.getJSONObject("metadata"))
                        .flatMap { metaDataObj -> Optional.of(metaDataObj.getJSONObject("kernelspec")) }
                        .map { kernelSpec -> kernelSpec.getString("language") }
                        .orElse("Python")
                if ("R" == language) {
                    dataSetExportRequest.setKernelType(KernelTypeEnum.R)
                } else {
                    dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON)
                }
            } catch (e: JSONException) {
                // If we can't find metadata to parse, default to python.
                dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON)
            }

        } else {
            when (dataSetExportRequest.getKernelType()) {
                PYTHON -> {
                }
                R -> metaData
                        .put(
                                "kernelspec",
                                JSONObject().put("display_name", "R").put("language", "R").put("name", "ir"))
                        .put(
                                "language_info",
                                JSONObject()
                                        .put("codemirror_mode", "r")
                                        .put("file_extension", ".r")
                                        .put("mimetype", "text/x-r-source")
                                        .put("name", "r")
                                        .put("pygments_lexer", "r")
                                        .put("version", "3.4.4"))
                else -> throw BadRequestException(
                        "Kernel Type " + dataSetExportRequest.getKernelType() + " is not supported")
            }
        }

        val queriesByDomain = dataSetService.generateQueryJobConfigurationsByDomainName(
                dataSetExportRequest.getDataSetRequest())

        val qualifier = generateRandomEightCharacterQualifier()

        val queriesAsStrings = dataSetService.generateCodeCells(
                dataSetExportRequest.getKernelType(),
                dataSetExportRequest.getDataSetRequest().getName(),
                qualifier,
                queriesByDomain)

        if (dataSetExportRequest.getNewNotebook()) {
            notebookFile = JSONObject()
                    .put("cells", JSONArray())
                    .put("metadata", metaData)
                    // nbformat and nbformat_minor are the notebook major and minor version we are
                    // creating.
                    // Specifically, here we create notebook version 4.2 (I believe)
                    // See https://nbformat.readthedocs.io/en/latest/api.html
                    .put("nbformat", 4)
                    .put("nbformat_minor", 2)
        }
        for (query in queriesAsStrings) {
            notebookFile.getJSONArray("cells").put(createNotebookCodeCellWithString(query))
        }

        notebooksService.saveNotebook(
                workspace.getWorkspace().getBucketName(),
                dataSetExportRequest.getNotebookName(),
                notebookFile)

        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun getDataSetsInWorkspace(
            workspaceNamespace: String, workspaceId: String): ResponseEntity<DataSetListResponse> {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        val dataSets = dataSetDao.findByWorkspaceIdAndInvalid(workspace.workspaceId, false)
        val response = DataSetListResponse()

        response.setItems(
                dataSets.stream()
                        .map(TO_CLIENT_DATA_SET)
                        .sorted(Comparator.comparing(Function<R, Any> { DataSet.getName() }))
                        .collect(Collectors.toList<T>()))
        return ResponseEntity.ok<DataSetListResponse>(response)
    }

    fun markDirty(
            workspaceNamespace: String, workspaceId: String, markDataSetRequest: MarkDataSetRequest): ResponseEntity<Boolean> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
        var dbDataSetList: List<org.pmiops.workbench.db.model.DataSet> = ArrayList()
        if (markDataSetRequest.getResourceType().equalsIgnoreCase(COHORT)) {
            dbDataSetList = dataSetDao.findDataSetsByCohortIds(markDataSetRequest.getId())
        } else if (markDataSetRequest.getResourceType().equalsIgnoreCase(CONCEPT_SET)) {
            dbDataSetList = dataSetDao.findDataSetsByConceptSetIds(markDataSetRequest.getId())
        }
        dbDataSetList = dbDataSetList.stream()
                .map { dataSet ->
                    dataSet.invalid = true
                    dataSet
                }
                .collect<List<DataSet>, Any>(Collectors.toList())
        try {
            dataSetDao.save(dbDataSetList)
        } catch (e: OptimisticLockException) {
            throw ConflictException("Failed due to concurrent data set modification")
        }

        return ResponseEntity.ok(true)
    }

    fun deleteDataSet(
            workspaceNamespace: String, workspaceId: String, dataSetId: Long?): ResponseEntity<EmptyResponse> {
        val dataSet = getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.WRITER)
        dataSetDao.delete(dataSet.dataSetId)
        return ResponseEntity.ok<EmptyResponse>(EmptyResponse())
    }

    fun updateDataSet(
            workspaceNamespace: String, workspaceId: String, dataSetId: Long?, request: DataSetRequest): ResponseEntity<DataSet> {
        var dbDataSet = getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.WRITER)
        if (Strings.isNullOrEmpty(request.getEtag())) {
            throw BadRequestException("missing required update field 'etag'")
        }
        val version = Etags.toVersion(request.getEtag())
        if (dbDataSet.version != version) {
            throw ConflictException("Attempted to modify outdated data set version")
        }
        val now = Timestamp(clock.instant().toEpochMilli())
        dbDataSet.lastModifiedTime = now
        dbDataSet.includesAllParticipants = request.getIncludesAllParticipants()
        dbDataSet.cohortIds = request.getCohortIds()
        dbDataSet.conceptSetIds = request.getConceptSetIds()
        dbDataSet.description = request.getDescription()
        dbDataSet.name = request.getName()
        dbDataSet.prePackagedConceptSetEnum = request.getPrePackagedConceptSet()
        dbDataSet.values = request.getDomainValuePairs().stream()
                .map(???({ this.getDataSetValuesFromDomainValueSet(it) }))
        .collect(Collectors.toList<T>())
        try {
            dbDataSet = dataSetDao.save(dbDataSet)
            // TODO: add recent resource entry for data sets
        } catch (e: OptimisticLockException) {
            throw ConflictException("Failed due to concurrent concept set modification")
        } catch (ex: DataIntegrityViolationException) {
            throw ConflictException("Data set with the same name already exists")
        }

        return ResponseEntity.ok<DataSet>(TO_CLIENT_DATA_SET.apply(dbDataSet))
    }

    fun getDataSet(
            workspaceNamespace: String, workspaceId: String, dataSetId: Long?): ResponseEntity<DataSet> {
        val dataSet = getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.READER)
        return ResponseEntity.ok<DataSet>(TO_CLIENT_DATA_SET.apply(dataSet))
    }

    fun getDataSetByResourceId(
            workspaceNamespace: String, workspaceId: String, resourceType: String, id: Long?): ResponseEntity<DataSetListResponse> {
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER)

        var dbDataSets: List<org.pmiops.workbench.db.model.DataSet> = ArrayList()
        if (resourceType == COHORT) {
            dbDataSets = dataSetDao.findDataSetsByCohortIds(id!!)
        } else if (resourceType == CONCEPT_SET) {
            dbDataSets = dataSetDao.findDataSetsByConceptSetIds(id!!)
        }
        val dataSetResponse = DataSetListResponse()
                .items(dbDataSets.stream().map(TO_CLIENT_DATA_SET).collect(Collectors.toList<T>()))
        return ResponseEntity.ok<DataSetListResponse>(dataSetResponse)
    }

    fun getDataDictionaryEntry(
            cdrVersionId: Long?, domain: String, domainValue: String): ResponseEntity<DataDictionaryEntry> {
        val cdrVersion = cdrVersionDao.findByCdrVersionId(cdrVersionId!!)
                ?: throw BadRequestException("Invalid CDR Version")

        val omopTable = conceptSetDao.DOMAIN_TO_TABLE_NAME.get(Domain.fromValue(domain))
                ?: throw BadRequestException("Invalid Domain")

        val dataDictionaryEntries = dataDictionaryEntryDao.findByFieldNameAndCdrVersion(domainValue, cdrVersion)

        if (dataDictionaryEntries.isEmpty()) {
            throw NotFoundException()
        }

        return ResponseEntity.ok<DataDictionaryEntry>(dataSetMapper.toApi(dataDictionaryEntries[0]))
    }

    // TODO(jaycarlton) create a class that knows about code cells and their properties,
    // then give it a toJson() method to replace this one.
    private fun createNotebookCodeCellWithString(cellInformation: String): JSONObject {
        return JSONObject()
                .put("cell_type", "code")
                .put("metadata", JSONObject())
                .put("execution_count", JSONObject.NULL)
                .put("outputs", JSONArray())
                .put("source", JSONArray().put(cellInformation))
    }

    private fun getDbDataSet(
            workspaceNamespace: String,
            workspaceId: String,
            dataSetId: Long?,
            workspaceAccessLevel: WorkspaceAccessLevel): org.pmiops.workbench.db.model.DataSet {
        val workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, workspaceAccessLevel)

        val dataSet = dataSetDao.findOne(dataSetId)
        if (dataSet == null || workspace.workspaceId != dataSet.workspaceId) {
            throw NotFoundException(
                    String.format(
                            "No data set with ID %s in workspace %s.", dataSet, workspace.firecloudName))
        }
        return dataSet
    }

    companion object {

        private val NO_OF_PREVIEW_ROWS = 20
        // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
        private val APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000L
        private val CONCEPT_SET = "conceptSet"
        private val COHORT = "cohort"

        private val DATE_FORMAT_STRING = "yyyy/MM/dd HH:mm:ss"
        val EMPTY_CELL_MARKER = ""

        private val log = Logger.getLogger(DataSetController::class.java.name)

        // TODO(jaycarlton): move into helper methods in one or both of these classes
        private val TO_CLIENT_DOMAIN_VALUE = { dataSetValue ->
            val domainValuePair = DomainValuePair()
            domainValuePair.setValue(dataSetValue.value)
            domainValuePair.setDomain(dataSetValue.domainEnum)
            domainValuePair
        }
    }
}
