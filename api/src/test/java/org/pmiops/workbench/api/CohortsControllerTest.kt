package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.fail
import org.mockito.Mockito.`when`
import org.pmiops.workbench.api.ConceptsControllerTest.makeConcept

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Random
import javax.inject.Provider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.audit.adapters.WorkspaceAuditAdapterService
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.cohorts.CohortFactoryImpl
import org.pmiops.workbench.cohorts.CohortMaterializationService
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.Cohort
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.CreateConceptSetRequest
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DuplicateCohortRequest
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.FieldSet
import org.pmiops.workbench.model.MaterializeCohortRequest
import org.pmiops.workbench.model.MaterializeCohortResponse
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.pmiops.workbench.notebooks.NotebooksServiceImpl
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
import org.pmiops.workbench.test.SearchRequests
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
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CohortsControllerTest {

    @Autowired
    internal var workspacesController: WorkspacesController? = null
    @Autowired
    internal var cohortsController: CohortsController? = null
    @Autowired
    internal var conceptSetsController: ConceptSetsController? = null
    @Autowired
    internal var billingProjectBufferService: BillingProjectBufferService? = null

    internal var workspace: Workspace
    internal var workspace2: Workspace
    internal var cdrVersion: CdrVersion
    internal var searchRequest: SearchRequest
    internal var cohortCriteria: String
    private var testMockFactory: TestMockFactory? = null
    @Autowired
    internal var workspaceService: WorkspaceService? = null
    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null
    @Autowired
    internal var conceptDao: ConceptDao? = null
    @Autowired
    internal var cohortReviewDao: CohortReviewDao? = null
    @Autowired
    internal var dataSetService: DataSetService? = null
    @Autowired
    internal var userRecentResourceService: UserRecentResourceService? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var cohortMaterializationService: CohortMaterializationService? = null
    @Mock
    internal var userProvider: Provider<User>? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null
    @Autowired
    internal var userService: UserService? = null
    @Autowired
    internal var cloudStorageService: CloudStorageService? = null
    @Autowired
    internal var cdrVersionService: CdrVersionService? = null
    @Autowired
    internal var complianceService: ComplianceService? = null

    @TestConfiguration
    @Import(WorkspaceServiceImpl::class, CohortCloningService::class, CohortFactoryImpl::class, NotebooksServiceImpl::class, UserService::class, WorkspacesController::class, CohortsController::class, ConceptSetsController::class)
    @MockBean(BillingProjectBufferService::class, CdrVersionService::class, CloudStorageService::class, CohortMaterializationService::class, ComplianceService::class, ConceptBigQueryService::class, ConceptService::class, ConceptSetService::class, DataSetService::class, DirectoryService::class, FireCloudService::class, LeonardoNotebooksClient::class, UserRecentResourceService::class, WorkspaceAuditAdapterService::class)
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
        testMockFactory!!.stubBufferBillingProject(billingProjectBufferService)
        testMockFactory!!.stubCreateFcWorkspace(fireCloudService)
        var user = User()
        user.email = CREATOR_EMAIL
        user.userId = 123L
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        user = userDao!!.save(user)
        currentUser = user
        `when`(userProvider!!.get()).thenReturn(user)
        workspacesController!!.setUserProvider(userProvider)
        cohortsController!!.setUserProvider(userProvider)
        conceptSetsController!!.setUserProvider(userProvider)

        cdrVersion = CdrVersion()
        cdrVersion.name = CDR_VERSION_NAME
        cdrVersionDao!!.save(cdrVersion)

        searchRequest = SearchRequests.males()
        cohortCriteria = Gson().toJson(searchRequest)

        workspace = Workspace()
        workspace.setName(WORKSPACE_NAME)
        workspace.setNamespace(WORKSPACE_NAMESPACE)
        workspace.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace.setResearchPurpose(ResearchPurpose())
        workspace.setCdrVersionId(cdrVersion.cdrVersionId.toString())

        workspace2 = Workspace()
        workspace2.setName(WORKSPACE_NAME_2)
        workspace2.setNamespace(WORKSPACE_NAMESPACE)
        workspace2.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace2.setResearchPurpose(ResearchPurpose())
        workspace2.setCdrVersionId(cdrVersion.cdrVersionId.toString())

        CLOCK.setInstant(NOW)

        val cohort = Cohort()
        cohort.setName("demo")
        cohort.setDescription("demo")
        cohort.setType("demo")
        cohort.setCriteria(createDemoCriteria().toString())

        workspace = workspacesController!!.createWorkspace(workspace).body
        workspace2 = workspacesController!!.createWorkspace(workspace2).body

        stubGetWorkspace(
                workspace.getNamespace(), WORKSPACE_NAME, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspaceAcl(
                workspace.getNamespace(), WORKSPACE_NAME, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspace(
                workspace2.getNamespace(), WORKSPACE_NAME_2, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspaceAcl(
                workspace2.getNamespace(), WORKSPACE_NAME_2, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER)
    }

    private fun createDemoCriteria(): JSONObject {
        val criteria = JSONObject()
        criteria.append("includes", JSONArray())
        criteria.append("excludes", JSONArray())
        return criteria
    }

    @Throws(Exception::class)
    private fun stubGetWorkspace(ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val fcWorkspace = org.pmiops.workbench.firecloud.model.Workspace()
        fcWorkspace.setNamespace(ns)
        fcWorkspace.setName(name)
        fcWorkspace.setCreatedBy(creator)
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(fcWorkspace)
        fcResponse.setAccessLevel(access.toString())
        `when`<Any>(fireCloudService!!.getWorkspace(ns, name)).thenReturn(fcResponse)
        stubGetWorkspaceAcl(ns, name, creator, access)
    }

    private fun stubGetWorkspaceAcl(
            ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val workspaceAccessLevelResponse = WorkspaceACL()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(access.toString())
        val userEmailToAccessEntry = ImmutableMap.of<String, WorkspaceAccessEntry>(creator, accessLevelEntry)
        workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry)
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ns, name)).thenReturn(workspaceAccessLevelResponse)
    }

    fun createDefaultCohort(): Cohort {
        val cohort = Cohort()
        cohort.setName(COHORT_NAME)
        cohort.setCriteria(cohortCriteria)
        return cohort
    }

    @Test
    @Throws(Exception::class)
    fun testGetCohortsInWorkspace() {
        var c1 = createDefaultCohort()
        c1.setName("c1")
        c1 = cohortsController!!.createCohort(workspace.getNamespace(), workspace.getId(), c1).body
        var c2 = createDefaultCohort()
        c2.setName("c2")
        c2 = cohortsController!!.createCohort(workspace.getNamespace(), workspace.getId(), c2).body

        val cohorts = cohortsController!!
                .getCohortsInWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getItems()
        assertThat(cohorts).containsAllOf(c1, c2)
        assertThat(cohorts.size).isEqualTo(2)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateCohort() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        cohort.setName("updated-name")
        var updated = cohortsController!!
                .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
                .body
        cohort.setEtag(updated.getEtag())
        assertThat(updated).isEqualTo(cohort)

        cohort.setName("updated-name2")
        updated = cohortsController!!
                .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
                .body
        cohort.setEtag(updated.getEtag())
        assertThat(updated).isEqualTo(cohort)

        val got = cohortsController!!
                .getCohort(workspace.getNamespace(), workspace.getId(), cohort.getId())
                .body
        assertThat(got).isEqualTo(cohort)
    }

    @Test
    fun testDuplicateCohort() {
        var originalCohort = createDefaultCohort()
        originalCohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), originalCohort)
                .body

        val params = DuplicateCohortRequest()
        params.setNewName("New Cohort Name")
        params.setOriginalCohortId(originalCohort.getId())

        var newCohort = cohortsController!!
                .duplicateCohort(workspace.getNamespace(), workspace.getId(), params)
                .body
        newCohort = cohortsController!!
                .getCohort(workspace.getNamespace(), workspace.getId(), newCohort.getId())
                .body

        assertThat(newCohort.getName()).isEqualTo(params.getNewName())
        assertThat(newCohort.getCriteria()).isEqualTo(originalCohort.getCriteria())
        assertThat(newCohort.getType()).isEqualTo(originalCohort.getType())
        assertThat(newCohort.getDescription()).isEqualTo(originalCohort.getDescription())
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testGetCohortWrongWorkspace() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        cohortsController!!.getCohort(workspace2.getNamespace(), WORKSPACE_NAME_2, cohort.getId())
    }

    @Test(expected = ConflictException::class)
    @Throws(Exception::class)
    fun testUpdateCohortStaleThrows() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        cohortsController!!
                .updateCohort(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohort.getId(),
                        Cohort().name("updated-name").etag(cohort.getEtag()))
                .body

        // Still using the initial etag.
        cohortsController!!
                .updateCohort(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohort.getId(),
                        Cohort().name("updated-name2").etag(cohort.getEtag()))
                .body
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateCohortInvalidEtagsThrow() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        // TODO: Refactor to be a @Parameterized test case.
        val cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"")
        for (etag in cases) {
            try {
                cohortsController!!.updateCohort(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohort.getId(),
                        Cohort().name("updated-name").etag(etag))
                fail(String.format("expected BadRequestException for etag: %s", etag))
            } catch (e: BadRequestException) {
                // expected
            }

        }
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortWorkspaceNotFound() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        val owner = WorkspaceAccessLevel.OWNER
        val workspaceName = "badWorkspace"
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setAccessLevel(owner.toString())
        `when`<Any>(fireCloudService!!.getWorkspace(WORKSPACE_NAMESPACE, workspaceName)).thenReturn(fcResponse)
        stubGetWorkspaceAcl(
                WORKSPACE_NAMESPACE, workspaceName, CREATOR_EMAIL, WorkspaceAccessLevel.OWNER)
        `when`<Any>(workspaceService!!.getWorkspaceAccessLevel(WORKSPACE_NAMESPACE, workspaceName))
                .thenThrow(NotFoundException())
        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        cohortsController!!.materializeCohort(WORKSPACE_NAMESPACE, workspaceName, request)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortCdrVersionNotFound() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        request.setCdrVersionName("badCdrVersion")
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortCohortNotFound() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName("badCohort")
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortNoSpecOrCohortName() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortPageSizeTooSmall() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        request.setPageSize(-1)
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortPageSizeZero() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        request.setPageSize(0)
        val adjustedRequest = MaterializeCohortRequest()
        adjustedRequest.setCohortName(cohort.getName())
        adjustedRequest.setPageSize(CohortsController.DEFAULT_PAGE_SIZE)
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null, cohortCriteria, null, adjustedRequest))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortPageSizeTooLarge() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        request.setPageSize(CohortsController.MAX_PAGE_SIZE + 1)
        val adjustedRequest = MaterializeCohortRequest()
        adjustedRequest.setCohortName(cohort.getName())
        adjustedRequest.setPageSize(CohortsController.MAX_PAGE_SIZE)
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null, cohortCriteria, null, adjustedRequest))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortNamedCohort() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null, cohortCriteria, null, request))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortNamedCohortWithConceptSet() {
        conceptDao!!.save(CONCEPT_1)
        conceptDao!!.save(CONCEPT_2)
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        var conceptSet = ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME)
        conceptSet = conceptSetsController!!
                .createConceptSet(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CreateConceptSetRequest()
                                .conceptSet(conceptSet)
                                .addedIds(
                                        ImmutableList.of(
                                                CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        val tableQuery = TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME)
        request.setFieldSet(FieldSet().tableQuery(tableQuery))
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null,
                cohortCriteria,
                ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId()),
                request))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortNamedCohortWithConceptSetWrongTable() {
        conceptDao!!.save(CONCEPT_1)
        conceptDao!!.save(CONCEPT_2)
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        var conceptSet = ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME)
        conceptSet = conceptSetsController!!
                .createConceptSet(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CreateConceptSetRequest()
                                .conceptSet(conceptSet)
                                .addedIds(
                                        ImmutableList.of(
                                                CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        val tableQuery = TableQuery().tableName("observation").conceptSetName(CONCEPT_SET_NAME)
        request.setFieldSet(FieldSet().tableQuery(tableQuery))
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testMaterializeCohortNamedCohortWithConceptSetNotFound() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        val tableQuery = TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME)
        request.setFieldSet(FieldSet().tableQuery(tableQuery))
        cohortsController!!.materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortNamedCohortWithReview() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body
        val cohortReview = CohortReview()
        cohortReview.cohortId = cohort.getId()
        cohortReview.cdrVersionId = cdrVersion.cdrVersionId
        cohortReview.reviewSize = 2
        cohortReview.reviewedCount = 2
        cohortReviewDao!!.save(cohortReview)

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(
                cohortReview, cohortCriteria, null, request))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortWithSpec() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortSpec(cohort.getCriteria())
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null, cohortCriteria, null, request))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    @Test
    @Throws(Exception::class)
    fun testMaterializeCohortWithEverything() {
        var cohort = createDefaultCohort()
        cohort = cohortsController!!
                .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                .body

        val request = MaterializeCohortRequest()
        request.setCohortName(cohort.getName())
        request.setPageSize(123)
        request.setPageToken("token")
        request.setCdrVersionName(CDR_VERSION_NAME)
        val statuses = ImmutableList.of<CohortStatus>(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED)
        request.setStatusFilter(statuses)
        val response = MaterializeCohortResponse()
        `when`<Any>(cohortMaterializationService!!.materializeCohort(null, cohortCriteria, null, request))
                .thenReturn(response)
        assertThat(
                cohortsController!!
                        .materializeCohort(workspace.getNamespace(), WORKSPACE_NAME, request)
                        .body)
                .isEqualTo(response)
    }

    companion object {

        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private val CDR_VERSION_NAME = "cdrVersion"
        private val WORKSPACE_NAME = "workspace"
        private val WORKSPACE_NAME_2 = "workspace2"
        private val WORKSPACE_NAMESPACE = "ns"
        private val COHORT_NAME = "cohort"
        private val CONCEPT_SET_NAME = "concept_set"
        private val CREATOR_EMAIL = "bob@gmail.com"

        private val CLIENT_CONCEPT_1 = Concept()
                .conceptId(123L)
                .conceptName("a concept")
                .standardConcept(true)
                .conceptCode("conceptA")
                .conceptClassId("classId")
                .vocabularyId("V1")
                .domainId("Condition")
                .countValue(123L)
                .prevalence(0.2f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_CONCEPT_2 = Concept()
                .conceptId(789L)
                .standardConcept(false)
                .conceptName("multi word concept")
                .conceptCode("conceptC")
                .conceptClassId("classId3")
                .vocabularyId("V3")
                .domainId("Condition")
                .countValue(789L)
                .prevalence(0.4f)
                .conceptSynonyms(ArrayList<String>())

        private val CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1)
        private val CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2)
        private var currentUser: User? = null
    }
}
