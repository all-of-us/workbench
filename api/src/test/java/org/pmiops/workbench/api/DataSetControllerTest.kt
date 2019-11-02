package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.*

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import java.io.FileReader
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Collections
import java.util.Random
import java.util.UUID
import java.util.stream.Collectors
import javax.inject.Provider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.pmiops.workbench.audit.adapters.WorkspaceAuditAdapterService
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.cohorts.CohortFactory
import org.pmiops.workbench.cohorts.CohortFactoryImpl
import org.pmiops.workbench.cohorts.CohortMaterializationService
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.dataset.DataSetMapper
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao
import org.pmiops.workbench.db.dao.DataSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.DataSetServiceImpl
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.Cohort
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.CreateConceptSetRequest
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.DataSetCodeResponse
import org.pmiops.workbench.model.DataSetExportRequest
import org.pmiops.workbench.model.DataSetPreviewValueList
import org.pmiops.workbench.model.DataSetRequest
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainValuePair
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.KernelTypeEnum
import org.pmiops.workbench.model.PrePackagedConceptSetEnum
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.notebooks.NotebooksService
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
import org.pmiops.workbench.test.SearchRequests
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig
import org.pmiops.workbench.utils.TestMockFactory
import org.pmiops.workbench.workspaces.WorkspaceService
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl
import org.pmiops.workbench.workspaces.WorkspacesController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// TODO(jaycarlton): many of the tests here are testing DataSetServiceImpl more than
//   DataSetControllerImpl, so move those tests and setup stuff into DataSetServiceTest
//   and mock out DataSetService here.
@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DataSetControllerTest {

    private var COHORT_ONE_ID: Long? = null
    private var CONCEPT_SET_ONE_ID: Long? = null
    private var COHORT_TWO_ID: Long? = null
    private var CONCEPT_SET_TWO_ID: Long? = null
    private var CONCEPT_SET_SURVEY_ID: Long? = null

    private var cohortCriteria: String? = null
    private var searchRequest: SearchRequest? = null
    private var testMockFactory: TestMockFactory? = null
    private var workspace: Workspace? = null

    @Autowired
    internal var billingProjectBufferService: BillingProjectBufferService? = null

    @Autowired
    internal var bigQueryService: BigQueryService? = null

    @Autowired
    internal var cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService? = null

    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null

    @Autowired
    internal var cdrVersionService: CdrVersionService? = null

    @Autowired
    internal var cloudStorageService: CloudStorageService? = null

    @Autowired
    internal var cohortDao: CohortDao? = null

    @Autowired
    internal var cohortFactory: CohortFactory? = null

    @Autowired
    internal var cohortMaterializationService: CohortMaterializationService? = null

    @Autowired
    internal var cohortReviewDao: CohortReviewDao? = null

    @Autowired
    internal var conceptBigQueryService: ConceptBigQueryService? = null

    @Autowired
    internal var conceptDao: ConceptDao? = null

    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null

    @Autowired
    internal var dataDictionaryEntryDao: DataDictionaryEntryDao? = null

    @Autowired
    internal var dataSetDao: DataSetDao? = null

    @Mock
    internal var dataSetMapper: DataSetMapper? = null

    @Autowired
    internal var dataSetService: DataSetService

    @Autowired
    internal var fireCloudService: FireCloudService? = null

    @Autowired
    internal var cohortQueryBuilder: CohortQueryBuilder? = null

    @Autowired
    internal var testBigQueryCdrSchemaConfig: TestBigQueryCdrSchemaConfig? = null

    @Autowired
    internal var userDao: UserDao? = null

    @Mock
    internal var userProvider: Provider<User>? = null

    @Autowired
    internal var workbenchConfigProvider: Provider<WorkbenchConfig>? = null

    @Autowired
    internal var notebooksService: NotebooksService? = null

    @Autowired
    internal var userRecentResourceService: UserRecentResourceService? = null

    @Autowired
    internal var userService: UserService? = null

    @Autowired
    internal var workspaceService: WorkspaceService? = null

    @Autowired
    internal var workspaceAuditAdapterService: WorkspaceAuditAdapterService? = null

    private var dataSetController: DataSetController? = null

    @Rule
    var expectedException = ExpectedException.none()

    @TestConfiguration
    @Import(CohortFactoryImpl::class, DataSetServiceImpl::class, TestBigQueryCdrSchemaConfig::class, UserService::class, WorkspacesController::class, WorkspaceServiceImpl::class)
    @MockBean(BillingProjectBufferService::class, BigQueryService::class, CdrBigQuerySchemaConfigService::class, CdrVersionService::class, CloudStorageService::class, CohortCloningService::class, CohortMaterializationService::class, ComplianceService::class, ConceptBigQueryService::class, ConceptSetService::class, DataSetService::class, DataSetMapper::class, FireCloudService::class, DirectoryService::class, NotebooksService::class, CohortQueryBuilder::class, UserRecentResourceService::class, WorkspaceAuditAdapterService::class)
    internal class Configuration {
        @Bean
        fun clock(): Clock {
            return CLOCK
        }

        @Bean
        fun random(): Random {
            return FakeLongRandom(123)
        }

        @Bean
        @Scope("prototype")
        fun user(): User? {
            return currentUser
        }

        @Bean
        fun workbenchConfig(): WorkbenchConfig {
            val workbenchConfig = WorkbenchConfig()
            workbenchConfig.featureFlags = WorkbenchConfig.FeatureFlagsConfig()
            return workbenchConfig
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        testMockFactory = TestMockFactory()
        dataSetService = DataSetServiceImpl(
                bigQueryService,
                cdrBigQuerySchemaConfigService,
                cohortDao,
                conceptBigQueryService,
                conceptSetDao,
                cohortQueryBuilder,
                dataSetDao)
        dataSetController = spy(
                DataSetController(
                        bigQueryService,
                        CLOCK,
                        cdrVersionDao,
                        cohortDao,
                        conceptDao,
                        conceptSetDao,
                        dataDictionaryEntryDao,
                        dataSetDao,
                        dataSetMapper,
                        dataSetService,
                        fireCloudService,
                        notebooksService,
                        userProvider,
                        workspaceService))
        val workspacesController = WorkspacesController(
                billingProjectBufferService,
                workspaceService,
                cdrVersionDao,
                userDao,
                userProvider,
                fireCloudService,
                cloudStorageService,
                CLOCK,
                notebooksService,
                userService,
                workbenchConfigProvider,
                workspaceAuditAdapterService)
        val cohortsController = CohortsController(
                workspaceService,
                cohortDao,
                cdrVersionDao,
                cohortFactory,
                cohortReviewDao,
                conceptSetDao,
                cohortMaterializationService,
                userProvider,
                CLOCK,
                cdrVersionService,
                userRecentResourceService)
        val conceptSetsController = ConceptSetsController(
                workspaceService,
                conceptSetDao,
                conceptDao,
                conceptBigQueryService,
                userRecentResourceService,
                userProvider,
                CLOCK)
        doAnswer { invocation ->
            val entry = mock(BillingProjectBufferEntry::class.java)
            doReturn(UUID.randomUUID().toString()).`when`(entry).fireCloudProjectName
            entry
        }
                .`when`<BillingProjectBufferService>(billingProjectBufferService)
                .assignBillingProject(ArgumentMatchers.any())
        testMockFactory!!.stubCreateFcWorkspace(fireCloudService)

        val gson = Gson()
        val cdrBigQuerySchemaConfig = gson.fromJson(FileReader("config/cdm/cdm_5_2.json"), CdrBigQuerySchemaConfig::class.java)

        `when`(cdrBigQuerySchemaConfigService!!.config).thenReturn(cdrBigQuerySchemaConfig)

        var user = User()
        user.email = USER_EMAIL
        user.userId = 123L
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        user = userDao!!.save(user)
        currentUser = user
        `when`(userProvider!!.get()).thenReturn(user)

        var cdrVersion = CdrVersion()
        cdrVersion.name = "1"
        // set the db name to be empty since test cases currently
        // run in the workbench schema only.
        cdrVersion.cdrDbName = ""
        cdrVersion = cdrVersionDao!!.save(cdrVersion)

        workspace = Workspace()
        workspace!!.setName(WORKSPACE_NAME)
        workspace!!.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace!!.setResearchPurpose(ResearchPurpose())
        workspace!!.setCdrVersionId(cdrVersion.cdrVersionId.toString())

        workspace = workspacesController.createWorkspace(workspace!!).body
        stubGetWorkspace(
                workspace!!.getNamespace(), workspace!!.getName(), USER_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspaceAcl(
                workspace!!.getNamespace(), WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER)

        searchRequest = SearchRequests.males()

        cohortCriteria = Gson().toJson(searchRequest)

        var cohort = Cohort().name(COHORT_ONE_NAME).criteria(cohortCriteria)
        cohort = cohortsController.createCohort(workspace!!.getNamespace(), WORKSPACE_NAME, cohort).body
        COHORT_ONE_ID = cohort.getId()

        var cohortTwo = Cohort().name(COHORT_TWO_NAME).criteria(cohortCriteria)
        cohortTwo = cohortsController
                .createCohort(workspace!!.getNamespace(), WORKSPACE_NAME, cohortTwo)
                .body
        COHORT_TWO_ID = cohortTwo.getId()

        var conceptList: MutableList<Concept> = ArrayList<Concept>()

        conceptList.add(
                Concept()
                        .conceptId(123L)
                        .conceptName("a concept")
                        .standardConcept(true)
                        .conceptCode("conceptA")
                        .conceptClassId("classId")
                        .vocabularyId("V1")
                        .domainId("Condition")
                        .countValue(123L)
                        .prevalence(0.2f)
                        .conceptSynonyms(emptyList<T>()))

        var conceptSet = ConceptSet()
                .id(CONCEPT_SET_ONE_ID)
                .name(CONCEPT_SET_ONE_NAME)
                .domain(Domain.CONDITION)
                .concepts(conceptList)

        val conceptSetRequest = CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addedIds(conceptList.stream().map(Function<Concept, Any> { Concept.getConceptId() }).collect(Collectors.toList<T>()))

        conceptSet = conceptSetsController
                .createConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSetRequest)
                .body
        CONCEPT_SET_ONE_ID = conceptSet.getId()

        conceptList = ArrayList<Concept>()

        conceptList.add(
                Concept()
                        .conceptId(456L)
                        .conceptName("a concept of type survey")
                        .standardConcept(true)
                        .conceptCode("conceptA")
                        .conceptClassId("classId")
                        .vocabularyId("V1")
                        .domainId("Observation")
                        .countValue(123L)
                        .prevalence(0.2f)
                        .conceptSynonyms(ArrayList<String>()))

        var conceptSurveySet = ConceptSet()
                .id(CONCEPT_SET_SURVEY_ID)
                .name(CONCEPT_SET_SURVEY_NAME)
                .domain(Domain.OBSERVATION)
                .concepts(conceptList)

        val conceptSetRequest1 = CreateConceptSetRequest()
                .conceptSet(conceptSurveySet)
                .addedIds(
                        conceptList.stream()
                                .map<Any> { concept -> concept.getConceptId() }
                                .collect<R, A>(Collectors.toList<T>()))

        conceptSurveySet = conceptSetsController
                .createConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSetRequest1)
                .body
        CONCEPT_SET_SURVEY_ID = conceptSurveySet.getId()

        var conceptSetTwo = ConceptSet()
                .id(CONCEPT_SET_TWO_ID)
                .name(CONCEPT_SET_TWO_NAME)
                .domain(Domain.DRUG)
                .concepts(conceptList)

        val conceptSetTwoRequest = CreateConceptSetRequest()
                .conceptSet(conceptSetTwo)
                .addedIds(
                        conceptList.stream()
                                .map<Any> { concept -> concept.getConceptId() }
                                .collect<R, A>(Collectors.toList<T>()))

        conceptSetTwo = conceptSetsController
                .createConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSetTwoRequest)
                .body
        CONCEPT_SET_TWO_ID = conceptSetTwo.getId()

        `when`(cohortQueryBuilder!!.buildParticipantIdQuery(ArgumentMatchers.any()))
                .thenReturn(
                        QueryJobConfiguration.newBuilder(
                                "SELECT * FROM person_id from `\${projectId}.\${dataSetId}.person` person")
                                .build())
        // This is not great, but due to the interaction of mocks and bigquery, it is
        // exceptionally hard to fix it so that it calls the real filterBitQueryConfig
        // but _does not_ call the real methods in the rest of the bigQueryService.
        // I tried .thenCallRealMethod() which ended up giving a null pointer from the mock,
        // as opposed to calling through.
        `when`(bigQueryService!!.filterBigQueryConfig(ArgumentMatchers.any()))
                .thenAnswer { invocation: InvocationOnMock ->
                    val args = invocation.arguments
                    val queryJobConfiguration = args[0] as QueryJobConfiguration

                    var returnSql = queryJobConfiguration.query.replace("\${projectId}", TEST_CDR_PROJECT_ID)
                    returnSql = returnSql.replace("\${dataSetId}", TEST_CDR_DATA_SET_ID)
                    queryJobConfiguration.toBuilder().setQuery(returnSql).build()
                }
        `when`(dataSetController!!.generateRandomEightCharacterQualifier()).thenReturn("00000000")
    }

    private fun buildEmptyDataSetRequest(): DataSetRequest {
        return DataSetRequest()
                .conceptSetIds(ArrayList<E>())
                .cohortIds(ArrayList<E>())
                .domainValuePairs(ArrayList<E>())
                .name("blah")
                .prePackagedConceptSet(PrePackagedConceptSetEnum.NONE)
    }

    @Throws(Exception::class)
    private fun stubGetWorkspace(ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val fcWorkspace = org.pmiops.workbench.firecloud.model.Workspace()
        fcWorkspace.setNamespace(ns)
        fcWorkspace.setName(name)
        fcWorkspace.setCreatedBy(creator)
        fcWorkspace.setBucketName(WORKSPACE_BUCKET_NAME)
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(fcWorkspace)
        fcResponse.setAccessLevel(access.toString())
        `when`<Any>(fireCloudService!!.getWorkspace(ns, name)).thenReturn(fcResponse)
    }

    private fun stubGetWorkspaceAcl(
            ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val workspaceAccessLevelResponse = WorkspaceACL()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(access.toString())
        val userEmailToAccessEntry = ImmutableMap.of<String, WorkspaceAccessEntry>(creator, accessLevelEntry)
        workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry)
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ns, name)).thenReturn(workspaceAccessLevelResponse)
    }

    private fun mockDomainValuePair(): MutableList<DomainValuePair> {
        val domainValues = ArrayList<DomainValuePair>()
        domainValues.add(DomainValuePair().domain(Domain.CONDITION).value("PERSON_ID"))
        return domainValues
    }

    private fun mockDomainValuePairWithPerson(): List<DomainValuePair> {
        val domainValues = ArrayList<DomainValuePair>()
        domainValues.add(DomainValuePair().domain(Domain.PERSON).value("PERSON_ID"))
        return domainValues
    }

    private fun mockSurveyDomainValuePair(): List<DomainValuePair> {
        val domainValues = ArrayList<DomainValuePair>()
        val domainValuePair = DomainValuePair()
        domainValuePair.setDomain(Domain.OBSERVATION)
        domainValuePair.setValue("PERSON_ID")
        domainValues.add(domainValuePair)
        return domainValues
    }

    private fun mockLinkingTableQuery(domainBaseTables: ArrayList<String>) {
        val tableResultMock = mock(TableResult::class.java)
        val values = ArrayList<FieldValueList>()
        domainBaseTables.forEach { domainBaseTable ->
            val schemaFields = ArrayList<Field>()
            schemaFields.add(Field.of("OMOP_SQL", LegacySQLTypeName.STRING))
            schemaFields.add(Field.of("JOIN_VALUE", LegacySQLTypeName.STRING))
            val schema = FieldList.of(schemaFields)
            val rows = ArrayList<FieldValue>()
            rows.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "PERSON_ID"))
            rows.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, domainBaseTable))
            val fieldValueList = FieldValueList.of(rows, schema)
            values.add(fieldValueList)
        }
        doReturn(values).`when`(tableResultMock).values
        doReturn(tableResultMock).`when`<BigQueryService>(bigQueryService).executeQuery(ArgumentMatchers.any())
    }

    @Test
    fun testAddFieldValuesFromBigQueryToPreviewListWorksWithNullValues() {
        val dataSetPreviewValueList = DataSetPreviewValueList()
        val valuePreviewList = ImmutableList.of<DataSetPreviewValueList>(dataSetPreviewValueList)
        val fieldValueListRows = ImmutableList.of(FieldValue.of(FieldValue.Attribute.PRIMITIVE, null))
        val fieldValueList = FieldValueList.of(fieldValueListRows)
        dataSetController!!.addFieldValuesFromBigQueryToPreviewList(valuePreviewList, fieldValueList)
        assertThat(valuePreviewList[0].getQueryValue().get(0))
                .isEqualTo(DataSetController.EMPTY_CELL_MARKER)
    }

    @Test(expected = BadRequestException::class)
    fun testGetQueryFailsWithNoCohort() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)

        dataSetController!!.generateCode(
                workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
    }

    @Test(expected = BadRequestException::class)
    fun testGetQueryFailsWithNoConceptSet() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)

        dataSetController!!.generateCode(
                workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
    }

    @Test
    fun testGetQueryDropsQueriesWithNoValue() {
        val dataSet = buildEmptyDataSetRequest()
                .addCohortIdsItem(COHORT_ONE_ID)
                .addConceptSetIdsItem(CONCEPT_SET_ONE_ID)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        assertThat(response.getCode()).isEmpty()
    }

    @Test
    fun testGetPythonQuery() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        val domainValuePairs = mockDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        verify<BigQueryService>(bigQueryService, times(1)).executeQuery(ArgumentMatchers.any())
        val prefix = "dataset_00000000_condition_"
        assertThat(response.getCode())
                .isEqualTo(
                        "import pandas\n\n"
                                + "# This query represents dataset \"blah\" for domain \"condition\"\n"
                                + prefix
                                + "sql = \"\"\"SELECT PERSON_ID FROM `"
                                + TEST_CDR_TABLE
                                + ".condition_occurrence` c_occurrence WHERE \n"
                                + "(condition_concept_id IN (123) OR \n"
                                + "condition_source_concept_id IN (123)) \n"
                                + "AND (c_occurrence.PERSON_ID IN (SELECT * FROM person_id from `"
                                + TEST_CDR_TABLE
                                + ".person` person))\"\"\"\n"
                                + "\n"
                                + prefix
                                + "query_config = {\n"
                                + "  'query': {\n"
                                + "  'parameterMode': 'NAMED',\n"
                                + "  'queryParameters': [\n\n"
                                + "    ]\n"
                                + "  }\n"
                                + "}\n"
                                + "\n"
                                + "\n"
                                + "\n"
                                + prefix
                                + "df = pandas.read_gbq("
                                + prefix
                                + "sql, dialect=\"standard\", configuration="
                                + prefix
                                + "query_config)"
                                + "\n"
                                + "\n"
                                + prefix
                                + "df.head(5)")
    }

    @Test
    fun testGetRQuery() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        val domainValuePairs = mockDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.R.toString(), dataSet)
                .body
        verify<BigQueryService>(bigQueryService, times(1)).executeQuery(ArgumentMatchers.any())
        val prefix = "dataset_00000000_condition_"
        assertThat(response.getCode())
                .isEqualTo(
                        "if(! \"reticulate\" %in% installed.packages()) { install.packages(\"reticulate\") }\n"
                                + "library(reticulate)\n"
                                + "pd <- reticulate::import(\"pandas\")\n\n"
                                + "# This query represents dataset \"blah\" for domain \"condition\"\n"
                                + prefix
                                + "sql <- \"SELECT PERSON_ID FROM `"
                                + TEST_CDR_TABLE
                                + ".condition_occurrence` c_occurrence WHERE \n"
                                + "(condition_concept_id IN (123) OR \n"
                                + "condition_source_concept_id IN (123)) \n"
                                + "AND (c_occurrence.PERSON_ID IN (SELECT * FROM person_id from `"
                                + TEST_CDR_TABLE
                                + ".person` person))\"\n"
                                + "\n"
                                + prefix
                                + "query_config <- list(\n"
                                + "  query = list(\n"
                                + "    parameterMode = 'NAMED',\n"
                                + "    queryParameters = list(\n\n"
                                + "    )\n"
                                + "  )\n"
                                + ")\n"
                                + "\n"
                                + prefix
                                + "df <- pd\$read_gbq("
                                + prefix
                                + "sql, dialect=\"standard\", configuration="
                                + prefix
                                + "query_config)"
                                + "\n"
                                + "\n"
                                + "head("
                                + prefix
                                + "df, 5)")
    }

    @Test
    fun testGetQueryTwoDomains() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_TWO_ID)
        val domainValuePairs = mockDomainValuePair()
        val drugDomainValue = DomainValuePair()
        drugDomainValue.setDomain(Domain.DRUG)
        drugDomainValue.setValue("PERSON_ID")
        domainValuePairs.add(drugDomainValue)
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")
        tables.add("FROM `$TEST_CDR_TABLE.drug_exposure` d_exposure")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        verify<BigQueryService>(bigQueryService, times(2)).executeQuery(ArgumentMatchers.any())
        assertThat(response.getCode()).contains("condition_df")
        assertThat(response.getCode()).contains("drug_df")
    }

    @Test
    fun testGetQuerySurveyDomains() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_SURVEY_ID)
        val domainValuePairs = mockSurveyDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.ds_survey`")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        verify<BigQueryService>(bigQueryService, times(1)).executeQuery(ArgumentMatchers.any())
        assertThat(response.getCode()).contains("observation_df")
        assertThat(response.getCode()).contains("ds_survey")
    }

    @Test
    fun testGetQueryTwoCohorts() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addCohortIdsItem(COHORT_TWO_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        val domainValuePairList = mockDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairList)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        assertThat(response.getCode()).contains("UNION DISTINCT")
    }

    @Test
    fun testGetQueryDemographic() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addCohortIdsItem(COHORT_TWO_ID)
        dataSet.setPrePackagedConceptSet(PrePackagedConceptSetEnum.DEMOGRAPHICS)
        val domainValuePairs = ArrayList<DomainValuePair>()
        domainValuePairs.add(DomainValuePair().domain(Domain.PERSON).value("GENDER"))
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.person` person")

        mockLinkingTableQuery(tables)

        val response = dataSetController!!
                .generateCode(
                        workspace!!.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataSet)
                .body
        /* this should produces the following query
       import pandas

       blah_person_sql = """SELECT PERSON_ID FROM `all-of-us-ehr-dev.synthetic_cdr20180606.person` person
       WHERE person.PERSON_ID IN (SELECT * FROM person_id from `all-of-us-ehr-dev.synthetic_cdr20180606.person`
       person UNION DISTINCT SELECT * FROM person_id from `all-of-us-ehr-dev.synthetic_cdr20180606.person` person)"""

       blah_person_query_config = {
         'query': {
         'parameterMode': 'NAMED',
         'queryParameters': [

           ]
         }
       }
    */
        assertThat(response.getCode())
                .contains(
                        "person_sql = \"\"\"SELECT PERSON_ID FROM `$TEST_CDR_TABLE.person` person")
        // For demographic unlike other domains WHERE should be followed by person.person_id rather than
        // concept_id
        assertThat(response.getCode().contains("WHERE person.PERSON_ID"))
    }

    @Test
    fun createDataSetMissingArguments() {
        val dataSet = buildEmptyDataSetRequest().name(null)

        val cohortIds = ArrayList<Long>()
        cohortIds.add(1L)

        val conceptIds = ArrayList<Long>()
        conceptIds.add(1L)

        val valuePairList = ArrayList<DomainValuePair>()
        val domainValue = DomainValuePair()
        domainValue.setDomain(Domain.DRUG)
        domainValue.setValue("DRUGS_VALUE")

        valuePairList.add(domainValue)

        dataSet.setDomainValuePairs(valuePairList)
        dataSet.setConceptSetIds(conceptIds)
        dataSet.setCohortIds(cohortIds)

        expectedException.expect(BadRequestException::class.java)
        expectedException.expectMessage("Missing name")

        dataSetController!!.createDataSet(workspace!!.getNamespace(), WORKSPACE_NAME, dataSet)

        dataSet.setName("dataSet")
        dataSet.setCohortIds(null)

        expectedException.expect(BadRequestException::class.java)
        expectedException.expectMessage("Missing cohort ids")

        dataSetController!!.createDataSet(workspace!!.getNamespace(), WORKSPACE_NAME, dataSet)

        dataSet.setCohortIds(cohortIds)
        dataSet.setConceptSetIds(null)

        expectedException.expect(BadRequestException::class.java)
        expectedException.expectMessage("Missing concept set ids")

        dataSetController!!.createDataSet(workspace!!.getNamespace(), WORKSPACE_NAME, dataSet)

        dataSet.setConceptSetIds(conceptIds)
        dataSet.setDomainValuePairs(null)

        expectedException.expect(BadRequestException::class.java)
        expectedException.expectMessage("Missing values")

        dataSetController!!.createDataSet(workspace!!.getNamespace(), WORKSPACE_NAME, dataSet)
    }

    @Test
    fun exportToNewNotebook() {
        var dataSet = buildEmptyDataSetRequest().name("blah")
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        val domainValuePairs = mockDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")

        mockLinkingTableQuery(tables)
        val notebookName = "Hello World"

        val request = DataSetExportRequest()
                .dataSetRequest(dataSet)
                .newNotebook(true)
                .notebookName(notebookName)
                .kernelType(KernelTypeEnum.PYTHON)

        dataSetController!!.exportToNotebook(workspace!!.getNamespace(), WORKSPACE_NAME, request).body
        verify<NotebooksService>(notebooksService, never()).getNotebookContents(ArgumentMatchers.any(), ArgumentMatchers.any())
        // I tried to have this verify against the actual expected contents of the json object, but
        // java equivalence didn't handle it well.
        verify<NotebooksService>(notebooksService, times(1))
                .saveNotebook(ArgumentMatchers.eq(WORKSPACE_BUCKET_NAME), ArgumentMatchers.eq(notebookName), ArgumentMatchers.any(JSONObject::class.java))
    }

    @Test
    fun exportToExistingNotebook() {
        var dataSet = buildEmptyDataSetRequest()
        dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID)
        dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
        val domainValuePairs = mockDomainValuePair()
        dataSet.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.condition_occurrence` c_occurrence")

        mockLinkingTableQuery(tables)

        val notebookName = "Hello World"

        `when`(notebooksService!!.getNotebookContents(WORKSPACE_BUCKET_NAME, notebookName))
                .thenReturn(
                        JSONObject()
                                .put("cells", JSONArray())
                                .put("metadata", JSONObject())
                                .put("nbformat", 4)
                                .put("nbformat_minor", 2))

        val request = DataSetExportRequest()
                .dataSetRequest(dataSet)
                .newNotebook(false)
                .notebookName(notebookName)

        dataSetController!!.exportToNotebook(workspace!!.getNamespace(), WORKSPACE_NAME, request).body
        verify<NotebooksService>(notebooksService, times(1)).getNotebookContents(WORKSPACE_BUCKET_NAME, notebookName)
        // I tried to have this verify against the actual expected contents of the json object, but
        // java equivalence didn't handle it well.
        verify<NotebooksService>(notebooksService, times(1))
                .saveNotebook(ArgumentMatchers.eq(WORKSPACE_BUCKET_NAME), ArgumentMatchers.eq(notebookName), ArgumentMatchers.any(JSONObject::class.java))
    }

    @Test
    fun testGetQueryPersonDomainNoConceptSets() {
        var dataSetRequest = buildEmptyDataSetRequest()
        dataSetRequest = dataSetRequest.addCohortIdsItem(COHORT_ONE_ID)
        val domainValuePairs = mockDomainValuePairWithPerson()
        dataSetRequest.setDomainValuePairs(domainValuePairs)

        val tables = ArrayList<String>()
        tables.add("FROM `$TEST_CDR_TABLE.person` person")

        mockLinkingTableQuery(tables)

        val result = dataSetService.generateQueryJobConfigurationsByDomainName(dataSetRequest)
        assertThat(result).isNotEmpty()
    }

    companion object {
        private val COHORT_ONE_NAME = "cohort"
        private val COHORT_TWO_NAME = "cohort two"
        private val CONCEPT_SET_ONE_NAME = "concept set"
        private val CONCEPT_SET_TWO_NAME = "concept set two"
        private val CONCEPT_SET_SURVEY_NAME = "concept survey set"
        private val WORKSPACE_NAME = "name"
        private val WORKSPACE_BUCKET_NAME = "fc://bucket-hash"
        private val USER_EMAIL = "bob@gmail.com"
        private val TEST_CDR_PROJECT_ID = "all-of-us-ehr-dev"
        private val TEST_CDR_DATA_SET_ID = "synthetic_cdr20180606"
        private val TEST_CDR_TABLE = "$TEST_CDR_PROJECT_ID.$TEST_CDR_DATA_SET_ID"
        private val NAMED_PARAMETER_NAME = "p1_706"
        private val NAMED_PARAMETER_VALUE = "ICD9"

        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private var currentUser: User? = null
    }
}
