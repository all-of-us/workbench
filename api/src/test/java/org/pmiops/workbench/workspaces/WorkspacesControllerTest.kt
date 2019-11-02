package org.pmiops.workbench.workspaces

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.fail
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.pmiops.workbench.api.ConceptsControllerTest.makeConcept

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.TableResult
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.google.gson.Gson
import java.sql.Timestamp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.stream.Collectors
import javax.inject.Provider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.api.CohortAnnotationDefinitionController
import org.pmiops.workbench.api.CohortReviewController
import org.pmiops.workbench.api.CohortsController
import org.pmiops.workbench.api.ConceptSetsController
import org.pmiops.workbench.api.Etags
import org.pmiops.workbench.audit.adapters.WorkspaceAuditAdapterService
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.cohorts.CohortFactoryImpl
import org.pmiops.workbench.cohorts.CohortMaterializationService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.DataSetServiceImpl
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.BillingProjectBufferEntry
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.DataSet
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.FailedPreconditionException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdateResponseList
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.ArchivalStatus
import org.pmiops.workbench.model.CloneWorkspaceRequest
import org.pmiops.workbench.model.Cohort
import org.pmiops.workbench.model.CohortAnnotationDefinition
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse
import org.pmiops.workbench.model.CohortReview
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.CopyRequest
import org.pmiops.workbench.model.CreateConceptSetRequest
import org.pmiops.workbench.model.CreateReviewRequest
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.NotebookLockingMetadataResponse
import org.pmiops.workbench.model.NotebookRename
import org.pmiops.workbench.model.PageFilterRequest
import org.pmiops.workbench.model.ParticipantCohortAnnotation
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse
import org.pmiops.workbench.model.RecentWorkspace
import org.pmiops.workbench.model.RecentWorkspaceResponse
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.ResearchPurposeReviewRequest
import org.pmiops.workbench.model.ShareWorkspaceRequest
import org.pmiops.workbench.model.UpdateConceptSetRequest
import org.pmiops.workbench.model.UpdateWorkspaceRequest
import org.pmiops.workbench.model.UserRole
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.pmiops.workbench.model.WorkspaceUserRolesResponse
import org.pmiops.workbench.notebooks.NotebooksService
import org.pmiops.workbench.notebooks.NotebooksServiceImpl
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.SearchRequests
import org.pmiops.workbench.utils.TestMockFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
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
class WorkspacesControllerTest {

    @Autowired
    private val billingProjectBufferService: BillingProjectBufferService? = null
    @Autowired
    private val mockWorkspaceAuditAdapterService: WorkspaceAuditAdapterService? = null
    @Autowired
    private val cohortAnnotationDefinitionController: CohortAnnotationDefinitionController? = null
    @Autowired
    private val workspacesController: WorkspacesController? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null
    @Autowired
    private val workspaceService: WorkspaceService? = null
    @Autowired
    internal var cloudStorageService: CloudStorageService? = null
    @Autowired
    internal var bigQueryService: BigQueryService? = null
    @Autowired
    internal var workspaceDao: WorkspaceDao? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var conceptDao: ConceptDao? = null
    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var cohortReviewDao: CohortReviewDao? = null
    @Autowired
    internal var cohortsController: CohortsController? = null
    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null
    @Autowired
    internal var conceptSetService: ConceptSetService? = null
    @Autowired
    internal var conceptSetsController: ConceptSetsController? = null
    @Autowired
    internal var dataSetDao: DataSetDao? = null
    @Autowired
    internal var dataSetService: DataSetService? = null
    @Autowired
    internal var userRecentResourceService: UserRecentResourceService? = null
    @Autowired
    internal var cohortReviewController: CohortReviewController? = null
    @Autowired
    internal var conceptBigQueryService: ConceptBigQueryService? = null
    @Mock
    private val configProvider: Provider<WorkbenchConfig>? = null

    private var cdrVersion: CdrVersion? = null
    private var cdrVersionId: String? = null
    private var archivedCdrVersionId: String? = null

    private var testMockFactory: TestMockFactory? = null

    @TestConfiguration
    @Import(CdrVersionService::class, NotebooksServiceImpl::class, WorkspacesController::class, WorkspaceServiceImpl::class, CohortsController::class, CohortFactoryImpl::class, CohortCloningService::class, CohortReviewController::class, CohortAnnotationDefinitionController::class, CohortReviewServiceImpl::class, DataSetServiceImpl::class, ReviewQueryBuilder::class, ConceptSetService::class, ConceptSetsController::class)
    @MockBean(BillingProjectBufferService::class, CohortMaterializationService::class, ConceptBigQueryService::class, CdrBigQuerySchemaConfigService::class, FireCloudService::class, CloudStorageService::class, BigQueryService::class, CohortQueryBuilder::class, UserService::class, UserRecentResourceService::class, ConceptService::class, WorkspaceAuditAdapterService::class)
    internal class Configuration {

        @Bean
        fun clock(): Clock {
            return CLOCK
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
    fun setUp() {
        testMockFactory = TestMockFactory()
        currentUser = createUser(LOGGED_IN_USER_EMAIL)
        cdrVersion = CdrVersion()
        cdrVersion!!.name = "1"
        // set the db name to be empty since test cases currently
        // run in the workbench schema only.
        cdrVersion!!.cdrDbName = ""
        cdrVersion = cdrVersionDao!!.save<CdrVersion>(cdrVersion)
        cdrVersionId = java.lang.Long.toString(cdrVersion!!.cdrVersionId)

        var archivedCdrVersion = CdrVersion()
        archivedCdrVersion.name = "archived"
        archivedCdrVersion.cdrDbName = ""
        archivedCdrVersion.archivalStatusEnum = ArchivalStatus.ARCHIVED
        archivedCdrVersion = cdrVersionDao!!.save(archivedCdrVersion)
        archivedCdrVersionId = java.lang.Long.toString(archivedCdrVersion.cdrVersionId)

        conceptDao!!.save(CONCEPT_1)
        conceptDao!!.save(CONCEPT_2)
        conceptDao!!.save(CONCEPT_3)

        CLOCK.setInstant(NOW.toInstant())

        val testConfig = WorkbenchConfig()
        testConfig.firecloud = WorkbenchConfig.FireCloudConfig()
        testConfig.firecloud.registeredDomainName = "allUsers"
        testConfig.featureFlags = WorkbenchConfig.FeatureFlagsConfig()
        `when`(configProvider!!.get()).thenReturn(testConfig)

        workspacesController!!.setWorkbenchConfigProvider(configProvider)
        fcWorkspaceAcl = createWorkspaceACL()
        testMockFactory!!.stubBufferBillingProject(billingProjectBufferService)
        testMockFactory!!.stubCreateFcWorkspace(fireCloudService)
    }

    private fun createUser(email: String): User {
        val user = User()
        user.email = email
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        return userDao!!.save(user)
    }

    private fun createWorkspaceACL(): WorkspaceACL {
        return createWorkspaceACLWithPermission(WorkspaceAccessLevel.OWNER)
    }

    private fun createWorkspaceACL(acl: JSONObject): WorkspaceACL {
        return Gson().fromJson<Any>(JSONObject().put("acl", acl).toString(), WorkspaceACL::class.java)
    }

    private fun createWorkspaceACLWithPermission(permission: WorkspaceAccessLevel): WorkspaceACL {
        return createWorkspaceACL(
                JSONObject()
                        .put(
                                currentUser!!.email,
                                JSONObject()
                                        .put("accessLevel", permission.toString())
                                        .put("canCompute", true)
                                        .put("canShare", true)))
    }

    private fun createDemoCriteria(): JSONObject {
        val criteria = JSONObject()
        criteria.append("includes", JSONArray())
        criteria.append("excludes", JSONArray())
        return criteria
    }

    private fun mockBillingProjectBuffer(projectName: String) {
        val entry = mock(BillingProjectBufferEntry::class.java)
        doReturn(projectName).`when`(entry).fireCloudProjectName
        doReturn(entry).`when`<BillingProjectBufferService>(billingProjectBufferService).assignBillingProject(ArgumentMatchers.any())
    }

    private fun mockBlob(bucket: String, path: String): Blob {
        val blob = mock(Blob::class.java)
        `when`(blob.blobId).thenReturn(BlobId.of(bucket, path))
        `when`(blob.bucket).thenReturn(bucket)
        `when`(blob.name).thenReturn(path)
        `when`(blob.size).thenReturn(5_000L)
        return blob
    }

    private fun stubFcUpdateWorkspaceACL() {
        `when`<Any>(fireCloudService!!.updateWorkspaceACL(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), anyList<WorkspaceACLUpdate>()))
                .thenReturn(WorkspaceACLUpdateResponseList())
    }

    private fun stubFcGetWorkspaceACL(acl: WorkspaceACL? = fcWorkspaceAcl) {
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(acl)
    }

    private fun stubFcGetWorkspaceACLForWorkspace(
            workspaceNamespace: String, workspaceId: String, acl: WorkspaceACL) {
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(workspaceNamespace, workspaceId)).thenReturn(acl)
    }

    private fun stubFcGetGroup() {
        val testGrp = ManagedGroupWithMembers()
        testGrp.setGroupEmail("test@firecloud.org")
        `when`<Any>(fireCloudService!!.getGroup(ArgumentMatchers.anyString())).thenReturn(testGrp)
    }

    private fun stubGetWorkspace(
            ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        stubGetWorkspace(testMockFactory!!.createFcWorkspace(ns, name, creator), access)
    }

    private fun stubGetWorkspace(
            fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace, access: WorkspaceAccessLevel) {
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(fcWorkspace)
        fcResponse.setAccessLevel(access.toString())
        doReturn(fcResponse)
                .`when`<FireCloudService>(fireCloudService)
                .getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName())
        val workspaceResponses = fireCloudService!!.getWorkspaces(ArgumentMatchers.any())
        workspaceResponses.add(fcResponse)
        doReturn(workspaceResponses).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())
    }

    /**
     * Mocks out the FireCloud cloneWorkspace call with a FC-model workspace based on the provided
     * details. The mocked workspace object is returned so the caller can make further modifications
     * if needed.
     */
    private fun stubCloneWorkspace(
            ns: String, name: String, creator: String?): org.pmiops.workbench.firecloud.model.Workspace {
        val fcResponse = org.pmiops.workbench.firecloud.model.Workspace()
        fcResponse.setNamespace(ns)
        fcResponse.setName(name)
        fcResponse.setCreatedBy(creator)

        `when`<Any>(fireCloudService!!.cloneWorkspace(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), eq(ns), eq(name)))
                .thenReturn(fcResponse)

        return fcResponse
    }

    private fun stubBigQueryCohortCalls() {
        val queryResult = mock(TableResult::class.java)
        val testIterable = object : Iterable {
            override operator fun iterator(): Iterator<*> {
                val list = ArrayList<FieldValue>()
                list.add(null)
                return list.iterator()
            }
        }
        val rm = ImmutableMap.builder<String, Int>()
                .put("person_id", 0)
                .put("birth_datetime", 1)
                .put("gender_concept_id", 2)
                .put("race_concept_id", 3)
                .put("ethnicity_concept_id", 4)
                .put("count", 5)
                .put("deceased", 6)
                .build()

        `when`<QueryJobConfiguration>(bigQueryService!!.filterBigQueryConfig(null)).thenReturn(null)
        `when`(bigQueryService!!.executeQuery(null)).thenReturn(queryResult)
        `when`(bigQueryService!!.getResultMapper(queryResult)).thenReturn(rm)
        `when`<Iterable<FieldValueList>>(queryResult.iterateAll()).thenReturn(testIterable)
        `when`(bigQueryService!!.getLong(null!!, 0)).thenReturn(0L)
        `when`(bigQueryService!!.getString(null!!, 1)).thenReturn("1")
        `when`(bigQueryService!!.getLong(null!!, 2)).thenReturn(0L)
        `when`(bigQueryService!!.getLong(null!!, 3)).thenReturn(0L)
        `when`(bigQueryService!!.getLong(null!!, 4)).thenReturn(0L)
        `when`(bigQueryService!!.getLong(null!!, 5)).thenReturn(0L)
        `when`(bigQueryService!!.getBoolean(null!!, 6)).thenReturn(false)
    }

    // TODO(calbach): Clean up this test file to make better use of chained builders.
    private fun createWorkspace(workspaceNameSpace: String = "namespace", workspaceName: String = "name"): Workspace {
        val researchPurpose = ResearchPurpose()
        researchPurpose.setDiseaseFocusedResearch(true)
        researchPurpose.setDiseaseOfFocus("cancer")
        researchPurpose.setMethodsDevelopment(true)
        researchPurpose.setControlSet(true)
        researchPurpose.setAncestry(true)
        researchPurpose.setCommercialPurpose(true)
        researchPurpose.setSocialBehavioral(true)
        researchPurpose.setPopulationHealth(true)
        researchPurpose.setEducational(true)
        researchPurpose.setDrugDevelopment(true)
        researchPurpose.setPopulation(false)
        researchPurpose.setAdditionalNotes("additional notes")
        researchPurpose.setReasonForAllOfUs("reason for aou")
        researchPurpose.setIntendedStudy("intended study")
        researchPurpose.setAnticipatedFindings("anticipated findings")
        researchPurpose.setTimeRequested(1000L)
        researchPurpose.setTimeReviewed(1500L)
        researchPurpose.setReviewRequested(true)
        researchPurpose.setApproved(false)
        val workspace = Workspace()
        workspace.setId(workspaceName)
        workspace.setName(workspaceName)
        workspace.setNamespace(workspaceNameSpace)
        workspace.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace.setResearchPurpose(researchPurpose)
        workspace.setCdrVersionId(cdrVersionId)
        workspace.setGoogleBucketName(BUCKET_NAME)
        return workspace
    }

    fun createDefaultCohort(name: String): Cohort {
        val cohort = Cohort()
        cohort.setName(name)
        cohort.setCriteria(Gson().toJson(SearchRequests.males()))
        return cohort
    }

    fun convertUserRolesToUpdateAclRequestList(
            collaborators: List<UserRole>): ArrayList<WorkspaceACLUpdate> {
        val updateACLRequestList = ArrayList<WorkspaceACLUpdate>()
        for (userRole in collaborators) {
            var aclUpdate = WorkspaceACLUpdate().email(userRole.getEmail())
            aclUpdate = workspaceService!!.updateFirecloudAclsOnUser(userRole.getRole(), aclUpdate)
            updateACLRequestList.add(aclUpdate)
        }
        return updateACLRequestList
    }

    @Test
    fun getWorkspaces() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        verify<WorkspaceAuditAdapterService>(mockWorkspaceAuditAdapterService).fireCreateAction(ArgumentMatchers.any(Workspace::class.java!!), anyLong())

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(
                testMockFactory!!.createFcWorkspace(workspace.getNamespace(), workspace.getName(), null))
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())
        doReturn(listOf<Any>(fcResponse)).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())

        assertThat(workspacesController.workspaces.body.getItems().size()).isEqualTo(1)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateWorkspace() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        verify<FireCloudService>(fireCloudService).createWorkspace(workspace.getNamespace(), workspace.getName())

        stubGetWorkspace(
                workspace.getNamespace(),
                workspace.getName(),
                LOGGED_IN_USER_EMAIL,
                WorkspaceAccessLevel.OWNER)
        val workspace2 = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getWorkspace()
        assertThat(workspace2.getCreationTime()).isEqualTo(NOW_TIME)
        assertThat(workspace2.getLastModifiedTime()).isEqualTo(NOW_TIME)
        assertThat(workspace2.getCdrVersionId()).isEqualTo(cdrVersionId)
        assertThat(workspace2.getCreator()).isEqualTo(LOGGED_IN_USER_EMAIL)
        assertThat(workspace2.getDataAccessLevel()).isEqualTo(DataAccessLevel.PROTECTED)
        assertThat(workspace2.getId()).isEqualTo("name")
        assertThat(workspace2.getName()).isEqualTo("name")
        assertThat(workspace2.getResearchPurpose().getDiseaseFocusedResearch()).isTrue()
        assertThat(workspace2.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer")
        assertThat(workspace2.getResearchPurpose().getMethodsDevelopment()).isTrue()
        assertThat(workspace2.getResearchPurpose().getControlSet()).isTrue()
        assertThat(workspace2.getResearchPurpose().getAncestry()).isTrue()
        assertThat(workspace2.getResearchPurpose().getCommercialPurpose()).isTrue()
        assertThat(workspace2.getResearchPurpose().getSocialBehavioral()).isTrue()
        assertThat(workspace2.getResearchPurpose().getPopulationHealth()).isTrue()
        assertThat(workspace2.getResearchPurpose().getEducational()).isTrue()
        assertThat(workspace2.getResearchPurpose().getDrugDevelopment()).isTrue()
        assertThat(workspace2.getResearchPurpose().getPopulation()).isFalse()
        assertThat(workspace2.getResearchPurpose().getAdditionalNotes()).isEqualTo("additional notes")
        assertThat(workspace2.getResearchPurpose().getReasonForAllOfUs()).isEqualTo("reason for aou")
        assertThat(workspace2.getResearchPurpose().getIntendedStudy()).isEqualTo("intended study")
        assertThat(workspace2.getResearchPurpose().getAnticipatedFindings())
                .isEqualTo("anticipated findings")
        assertThat(workspace2.getNamespace()).isEqualTo(workspace.getNamespace())
        assertThat(workspace2.getResearchPurpose().getReviewRequested()).isTrue()
        assertThat(workspace2.getResearchPurpose().getTimeRequested()).isEqualTo(NOW_TIME)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateWorkspaceAlreadyApproved() {
        var workspace = createWorkspace()
        workspace.getResearchPurpose().setApproved(true)
        workspace = workspacesController!!.createWorkspace(workspace).body

        val workspace2 = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getWorkspace()
        assertThat(workspace2.getResearchPurpose().getApproved()).isNotEqualTo(true)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateWorkspace_createDeleteCycleSameName() {
        var workspace = createWorkspace()

        val uniqueIds = HashSet<String>()
        for (i in 0..2) {
            workspace = workspacesController!!.createWorkspace(workspace).body
            uniqueIds.add(workspace.getId())

            workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getName())
        }
        assertThat(uniqueIds.size).isEqualTo(1)
    }

    @Test(expected = FailedPreconditionException::class)
    @Throws(Exception::class)
    fun testCreateWorkspace_archivedCdrVersionThrows() {
        val workspace = createWorkspace()
        workspace.setCdrVersionId(archivedCdrVersionId)
        workspacesController!!.createWorkspace(workspace).body
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteWorkspace() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getName())
        verify<WorkspaceAuditAdapterService>(mockWorkspaceAuditAdapterService)
                .fireDeleteAction(ArgumentMatchers.any(org.pmiops.workbench.db.model.Workspace::class.java))
        try {
            workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName())
            fail("NotFoundException expected")
        } catch (e: NotFoundException) {
            // expected
        }

    }

    @Test
    @Throws(Exception::class)
    fun testApproveWorkspace() {
        var ws = createWorkspace()
        var researchPurpose = ws.getResearchPurpose()
        researchPurpose.setApproved(null)
        researchPurpose.setTimeReviewed(null)
        ws = workspacesController!!.createWorkspace(ws).body

        val request = ResearchPurposeReviewRequest()
        request.setApproved(true)
        workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request)
        ws = workspacesController.getWorkspace(ws.getNamespace(), ws.getName()).body.getWorkspace()
        researchPurpose = ws.getResearchPurpose()

        assertThat(researchPurpose.getApproved()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateWorkspace() {
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body
        stubFcGetWorkspaceACL()
        ws.setName("updated-name")
        val request = UpdateWorkspaceRequest()
        request.setWorkspace(ws)
        var updated = workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).body
        ws.setEtag(updated.getEtag())
        assertThat(updated).isEqualTo(ws)

        ws.setName("updated-name2")
        updated = workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).body
        ws.setEtag(updated.getEtag())
        assertThat(updated).isEqualTo(ws)
        val got = workspacesController.getWorkspace(ws.getNamespace(), ws.getId()).body.getWorkspace()
        assertThat(got).isEqualTo(ws)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateWorkspaceResearchPurpose() {
        stubFcGetWorkspaceACL()
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body

        val rp = ResearchPurpose()
                .diseaseFocusedResearch(false)
                .diseaseOfFocus(null)
                .methodsDevelopment(false)
                .controlSet(false)
                .ancestry(false)
                .commercialPurpose(false)
                .populationHealth(false)
                .socialBehavioral(false)
                .drugDevelopment(false)
                .additionalNotes(null)
                .reviewRequested(false)
        ws.setResearchPurpose(rp)
        val request = UpdateWorkspaceRequest()
        request.setWorkspace(ws)
        val updatedRp = workspacesController
                .updateWorkspace(ws.getNamespace(), ws.getId(), request)
                .body
                .getResearchPurpose()

        assertThat(updatedRp.getDiseaseFocusedResearch()).isFalse()
        assertThat(updatedRp.getDiseaseOfFocus()).isNull()
        assertThat(updatedRp.getMethodsDevelopment()).isFalse()
        assertThat(updatedRp.getControlSet()).isFalse()
        assertThat(updatedRp.getAncestry()).isFalse()
        assertThat(updatedRp.getCommercialPurpose()).isFalse()
        assertThat(updatedRp.getPopulationHealth()).isFalse()
        assertThat(updatedRp.getSocialBehavioral()).isFalse()
        assertThat(updatedRp.getDrugDevelopment()).isFalse()
        assertThat(updatedRp.getPopulation()).isFalse()
        assertThat(updatedRp.getAdditionalNotes()).isNull()
        assertThat(updatedRp.getReviewRequested()).isFalse()
    }

    @Test(expected = ForbiddenException::class)
    @Throws(Exception::class)
    fun testReaderUpdateWorkspaceThrows() {
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body

        ws.setName("updated-name")
        val request = UpdateWorkspaceRequest()
        request.setWorkspace(ws)
        stubFcGetWorkspaceACL(createWorkspaceACLWithPermission(WorkspaceAccessLevel.READER))
        stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.READER)
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request)
        stubFcGetWorkspaceACL(createWorkspaceACLWithPermission(WorkspaceAccessLevel.WRITER))
        stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.WRITER)
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request)
    }

    @Test(expected = ConflictException::class)
    @Throws(Exception::class)
    fun testUpdateWorkspaceStaleThrows() {
        stubFcGetWorkspaceACL()
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body
        val request = UpdateWorkspaceRequest()
        request.setWorkspace(Workspace().name("updated-name").etag(ws.getEtag()))
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).body

        // Still using the initial now-stale etag; this should throw.
        request.setWorkspace(Workspace().name("updated-name2").etag(ws.getEtag()))
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).body
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateWorkspaceInvalidEtagsThrow() {
        stubFcGetWorkspaceACL()
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body

        // TODO: Refactor to be a @Parameterized test case.
        val cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"")
        for (etag in cases) {
            try {
                val request = UpdateWorkspaceRequest()
                request.setWorkspace(Workspace().name("updated-name").etag(etag))
                workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request)
                fail(String.format("expected BadRequestException for etag: %s", etag))
            } catch (e: BadRequestException) {
                // expected
            }

        }
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testRejectAfterApproveThrows() {
        var ws = createWorkspace()
        ws = workspacesController!!.createWorkspace(ws).body

        val request = ResearchPurposeReviewRequest()
        request.setApproved(true)
        workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request)

        request.setApproved(false)
        workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request)
    }

    @Test
    @Throws(Exception::class)
    fun testListForApproval() {
        var forApproval = workspacesController!!.workspacesForReview.body.getItems()
        assertThat(forApproval).isEmpty()

        var ws: Workspace
        var researchPurpose: ResearchPurpose
        val nameForRequested = "requestedButNotApprovedYet"
        // requested approval, but not approved
        ws = createWorkspace()
        ws.setName(nameForRequested)
        researchPurpose = ws.getResearchPurpose()
        researchPurpose.setApproved(null)
        researchPurpose.setTimeReviewed(null)
        workspacesController.createWorkspace(ws)
        // already approved
        ws = createWorkspace()
        ws.setName("alreadyApproved")
        researchPurpose = ws.getResearchPurpose()
        ws = workspacesController.createWorkspace(ws).body
        val request = ResearchPurposeReviewRequest()
        request.setApproved(true)
        workspacesController.reviewWorkspace(ws.getNamespace(), ws.getId(), request)

        // no approval requested
        ws = createWorkspace()
        ws.setName("noApprovalRequested")
        researchPurpose = ws.getResearchPurpose()
        researchPurpose.setReviewRequested(false)
        researchPurpose.setTimeRequested(null)
        researchPurpose.setApproved(null)
        researchPurpose.setTimeReviewed(null)
        ws = workspacesController.createWorkspace(ws).body

        forApproval = workspacesController.workspacesForReview.body.getItems()
        assertThat(forApproval.size).isEqualTo(1)
        ws = forApproval.get(0)
        assertThat(ws.getName()).isEqualTo(nameForRequested)
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspace() {
        stubFcGetGroup()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        // The original workspace is shared with one other user.
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false

        writerUser = userDao!!.save(writerUser)
        val shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)
        val writer = UserRole()
        writer.setEmail(writerUser.email)
        writer.setRole(WorkspaceAccessLevel.WRITER)
        shareWorkspaceRequest.addItemsItem(writer)

        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()
        workspacesController.shareWorkspace(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)
        val clonedWorkspace = stubCloneWorkspace(
                modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)
        // Assign the same bucket name as the mock-factory's bucket name, so the clone vs. get equality
        // assertion below will pass.
        clonedWorkspace.setBucketName(TestMockFactory.BUCKET_NAME)

        mockBillingProjectBuffer("cloned-ns")
        val workspace2 = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()
        verify<WorkspaceAuditAdapterService>(mockWorkspaceAuditAdapterService)
                .fireDuplicateAction(
                        ArgumentMatchers.any(org.pmiops.workbench.db.model.Workspace::class.java),
                        ArgumentMatchers.any(org.pmiops.workbench.db.model.Workspace::class.java))

        // Stub out the FC service getWorkspace, since that's called by workspacesController.
        stubGetWorkspace(clonedWorkspace, WorkspaceAccessLevel.WRITER)
        assertWithMessage("get and clone responses are inconsistent")
                .that(workspace2)
                .isEqualTo(
                        workspacesController
                                .getWorkspace(workspace2.getNamespace(), workspace2.getId())
                                .body
                                .getWorkspace())

        assertThat(workspace2.getName()).isEqualTo(modWorkspace.getName())
        assertThat(workspace2.getNamespace()).isEqualTo(modWorkspace.getNamespace())
        assertThat(workspace2.getResearchPurpose()).isEqualTo(modPurpose)
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceWithCohortsAndConceptSets() {
        stubFcGetWorkspaceACL()
        val participantId = 1L
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        var c1 = createDefaultCohort("c1")
        c1 = cohortsController!!.createCohort(workspace.getNamespace(), workspace.getId(), c1).body
        var c2 = createDefaultCohort("c2")
        c2 = cohortsController!!.createCohort(workspace.getNamespace(), workspace.getId(), c2).body

        stubBigQueryCohortCalls()
        val reviewReq = CreateReviewRequest()
        reviewReq.setSize(1)
        val cr1 = cohortReviewController!!
                .createCohortReview(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c1.getId(),
                        cdrVersion!!.cdrVersionId,
                        reviewReq)
                .body
        val cad1EnumResponse = cohortAnnotationDefinitionController!!
                .createCohortAnnotationDefinition(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c1.getId(),
                        CohortAnnotationDefinition()
                                .cohortId(c1.getId())
                                .annotationType(AnnotationType.ENUM)
                                .columnName("cad")
                                .enumValues(Arrays.asList<T>("value")))
                .body
        val pca1EnumResponse = cohortReviewController!!
                .createParticipantCohortAnnotation(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cr1.getCohortReviewId(),
                        participantId,
                        ParticipantCohortAnnotation()
                                .cohortAnnotationDefinitionId(
                                        cad1EnumResponse.getCohortAnnotationDefinitionId())
                                .annotationValueEnum("value")
                                .participantId(participantId)
                                .cohortReviewId(cr1.getCohortReviewId()))
                .body
        val cad1StringResponse = cohortAnnotationDefinitionController
                .createCohortAnnotationDefinition(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c1.getId(),
                        CohortAnnotationDefinition()
                                .cohortId(c1.getId())
                                .annotationType(AnnotationType.STRING)
                                .columnName("cad1"))
                .body
        val pca1StringResponse = cohortReviewController!!
                .createParticipantCohortAnnotation(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cr1.getCohortReviewId(),
                        participantId,
                        ParticipantCohortAnnotation()
                                .cohortAnnotationDefinitionId(
                                        cad1StringResponse.getCohortAnnotationDefinitionId())
                                .annotationValueString("value1")
                                .participantId(participantId)
                                .cohortReviewId(cr1.getCohortReviewId()))
                .body

        reviewReq.setSize(2)
        val cr2 = cohortReviewController!!
                .createCohortReview(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c2.getId(),
                        cdrVersion!!.cdrVersionId,
                        reviewReq)
                .body
        val cad2EnumResponse = cohortAnnotationDefinitionController
                .createCohortAnnotationDefinition(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c2.getId(),
                        CohortAnnotationDefinition()
                                .cohortId(c2.getId())
                                .annotationType(AnnotationType.ENUM)
                                .columnName("cad")
                                .enumValues(Arrays.asList<T>("value")))
                .body
        val pca2EnumResponse = cohortReviewController!!
                .createParticipantCohortAnnotation(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cr2.getCohortReviewId(),
                        participantId,
                        ParticipantCohortAnnotation()
                                .cohortAnnotationDefinitionId(
                                        cad2EnumResponse.getCohortAnnotationDefinitionId())
                                .annotationValueEnum("value")
                                .participantId(participantId)
                                .cohortReviewId(cr2.getCohortReviewId()))
                .body
        val cad2BooleanResponse = cohortAnnotationDefinitionController
                .createCohortAnnotationDefinition(
                        workspace.getNamespace(),
                        workspace.getId(),
                        c2.getId(),
                        CohortAnnotationDefinition()
                                .cohortId(c2.getId())
                                .annotationType(AnnotationType.BOOLEAN)
                                .columnName("cad1"))
                .body
        val pca2BooleanResponse = cohortReviewController!!
                .createParticipantCohortAnnotation(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cr2.getCohortReviewId(),
                        participantId,
                        ParticipantCohortAnnotation()
                                .cohortAnnotationDefinitionId(
                                        cad2BooleanResponse.getCohortAnnotationDefinitionId())
                                .annotationValueBoolean(java.lang.Boolean.TRUE)
                                .participantId(participantId)
                                .cohortReviewId(cr2.getCohortReviewId()))
                .body

        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .thenReturn(123)
        var conceptSet1 = conceptSetsController!!
                .createConceptSet(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CreateConceptSetRequest()
                                .conceptSet(
                                        ConceptSet().name("cs1").description("d1").domain(Domain.CONDITION))
                                .addAddedIdsItem(CONCEPT_1.conceptId))
                .body
        val conceptSet2 = conceptSetsController!!
                .createConceptSet(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CreateConceptSetRequest()
                                .conceptSet(
                                        ConceptSet().name("cs2").description("d2").domain(Domain.MEASUREMENT))
                                .addAddedIdsItem(CONCEPT_3.conceptId))
                .body
        conceptSet1 = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace.getNamespace(),
                        workspace.getId(),
                        conceptSet1.getId(),
                        UpdateConceptSetRequest()
                                .etag(conceptSet1.getEtag())
                                .addedIds(
                                        ImmutableList.of(
                                                CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .body

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")

        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)

        mockBillingProjectBuffer("cloned-ns")
        val cloned = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()

        val cohorts = cohortsController!!
                .getCohortsInWorkspace(cloned.getNamespace(), cloned.getId())
                .body
                .getItems()
        val cohortsByName = Maps.uniqueIndex(cohorts) { c -> c.getName() }
        assertThat(cohortsByName.keys).containsAllOf("c1", "c2")
        assertThat(cohortsByName.keys.size).isEqualTo(2)
        assertThat(cohorts.stream().map({ c -> c.getId() }).collect(Collectors.toList<Any>()))
                .containsNoneOf(c1.getId(), c2.getId())

        val gotCr1 = cohortReviewController!!
                .getParticipantCohortStatuses(
                        cloned.getNamespace(),
                        cloned.getId(),
                        cohortsByName.get("c1").getId(),
                        cdrVersion!!.cdrVersionId,
                        PageFilterRequest())
                .body
        assertThat(gotCr1.getReviewSize()).isEqualTo(cr1.getReviewSize())
        assertThat(gotCr1.getParticipantCohortStatuses()).isEqualTo(cr1.getParticipantCohortStatuses())

        val clonedCad1List = cohortAnnotationDefinitionController
                .getCohortAnnotationDefinitions(
                        cloned.getNamespace(), cloned.getId(), cohortsByName.get("c1").getId())
                .body
        assertCohortAnnotationDefinitions(
                clonedCad1List,
                Arrays.asList<CohortAnnotationDefinition>(cad1EnumResponse, cad1StringResponse),
                cohortsByName.get("c1").getId())

        val clonedPca1List = cohortReviewController!!
                .getParticipantCohortAnnotations(
                        cloned.getNamespace(), cloned.getId(), gotCr1.getCohortReviewId(), participantId)
                .body
        assertParticipantCohortAnnotation(
                clonedPca1List,
                clonedCad1List,
                Arrays.asList<ParticipantCohortAnnotation>(pca1EnumResponse, pca1StringResponse),
                gotCr1.getCohortReviewId(),
                participantId)

        val gotCr2 = cohortReviewController!!
                .getParticipantCohortStatuses(
                        cloned.getNamespace(),
                        cloned.getId(),
                        cohortsByName.get("c2").getId(),
                        cdrVersion!!.cdrVersionId,
                        PageFilterRequest())
                .body
        assertThat(gotCr2.getReviewSize()).isEqualTo(cr2.getReviewSize())
        assertThat(gotCr2.getParticipantCohortStatuses()).isEqualTo(cr2.getParticipantCohortStatuses())

        val clonedCad2List = cohortAnnotationDefinitionController
                .getCohortAnnotationDefinitions(
                        cloned.getNamespace(), cloned.getId(), cohortsByName.get("c2").getId())
                .body
        assertCohortAnnotationDefinitions(
                clonedCad2List,
                Arrays.asList<CohortAnnotationDefinition>(cad2EnumResponse, cad2BooleanResponse),
                cohortsByName.get("c2").getId())

        val clonedPca2List = cohortReviewController!!
                .getParticipantCohortAnnotations(
                        cloned.getNamespace(), cloned.getId(), gotCr2.getCohortReviewId(), participantId)
                .body
        assertParticipantCohortAnnotation(
                clonedPca2List,
                clonedCad2List,
                Arrays.asList<ParticipantCohortAnnotation>(pca2EnumResponse, pca2BooleanResponse),
                gotCr2.getCohortReviewId(),
                participantId)

        assertThat(ImmutableSet.of(gotCr1.getCohortReviewId(), gotCr2.getCohortReviewId()))
                .containsNoneOf(cr1.getCohortReviewId(), cr2.getCohortReviewId())

        val conceptSets = conceptSetsController!!
                .getConceptSetsInWorkspace(cloned.getNamespace(), cloned.getId())
                .body
                .getItems()
        assertThat(conceptSets.size).isEqualTo(2)
        assertConceptSetClone(conceptSets.get(0), conceptSet1, cloned, 123)
        assertConceptSetClone(conceptSets.get(1), conceptSet2, cloned, 0)

        workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getId())
        try {
            workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName())
            fail("NotFoundException expected")
        } catch (e: NotFoundException) {
            // expected
        }

    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceWithConceptSetNewCdrVersionNewConceptSetCount() {
        stubFcGetWorkspaceACL()
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        var cdrVersion2 = CdrVersion()
        cdrVersion2.name = "2"
        cdrVersion2.cdrDbName = ""
        cdrVersion2 = cdrVersionDao!!.save(cdrVersion2)

        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .thenReturn(123)
        val conceptSet1 = conceptSetsController!!
                .createConceptSet(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CreateConceptSetRequest()
                                .conceptSet(
                                        ConceptSet().name("cs1").description("d1").domain(Domain.CONDITION))
                                .addedIds(
                                        ImmutableList.of(
                                                CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .body

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        modWorkspace.setCdrVersionId(cdrVersion2.cdrVersionId.toString())

        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)

        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)

        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
                .thenReturn(456)

        mockBillingProjectBuffer("cloned-ns")
        val cloned = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()
        val conceptSets = conceptSetsController!!
                .getConceptSetsInWorkspace(cloned.getNamespace(), cloned.getId())
                .body
                .getItems()
        assertThat(conceptSets.size).isEqualTo(1)
        assertConceptSetClone(conceptSets.get(0), conceptSet1, cloned, 456)
    }

    @Test
    fun testCloneWorkspace_Dataset() {
        stubFcGetWorkspaceACL()
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        val dbWorkspace = workspaceDao!!.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
                workspace.getNamespace(),
                workspace.getId(),
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)

        var cdrVersion2 = CdrVersion()
        cdrVersion2.name = "2"
        cdrVersion2.cdrDbName = ""
        cdrVersion2 = cdrVersionDao!!.save(cdrVersion2)

        val expectedConceptSetName = "cs1"
        val expectedConceptSetDescription = "d1"
        var originalConceptSet = org.pmiops.workbench.db.model.ConceptSet()
        originalConceptSet.name = expectedConceptSetName
        originalConceptSet.description = expectedConceptSetDescription
        originalConceptSet.domainEnum = Domain.CONDITION
        originalConceptSet.conceptIds = setOf(CLIENT_CONCEPT_1.getConceptId())
        originalConceptSet.workspaceId = dbWorkspace.workspaceId
        originalConceptSet = conceptSetDao!!.save(originalConceptSet)

        val expectedCohortName = "cohort name"
        val expectedCohortDescription = "cohort description"
        var originalCohort = org.pmiops.workbench.db.model.Cohort()
        originalCohort.name = expectedCohortName
        originalCohort.description = expectedCohortDescription
        originalCohort.workspaceId = dbWorkspace.workspaceId
        originalCohort = cohortDao!!.save(originalCohort)

        val expectedCohortReviewName = "cohort review"
        val expectedCohortReviewDefinition = "cohort definition"
        var originalCohortReview = org.pmiops.workbench.db.model.CohortReview()
        originalCohortReview.cohortName = expectedCohortReviewName
        originalCohortReview.cohortDefinition = expectedCohortReviewDefinition
        originalCohortReview.cohortId = originalCohort.cohortId
        originalCohortReview = cohortReviewDao!!.save(originalCohortReview)

        originalCohort.cohortReviews = setOf<CohortReview>(originalCohortReview)
        originalCohort = cohortDao!!.save(originalCohort)

        val expectedDatasetName = "data set name"
        val originalDataSet = DataSet()
        originalDataSet.name = expectedDatasetName
        originalDataSet.version = 1
        originalDataSet.conceptSetIds = listOf(originalConceptSet.conceptSetId)
        originalDataSet.cohortIds = listOf(originalCohort.cohortId)
        originalDataSet.workspaceId = dbWorkspace.workspaceId
        dataSetDao!!.save(originalDataSet)

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        modWorkspace.setCdrVersionId(cdrVersion2.cdrVersionId.toString())

        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)

        stubGetWorkspace(
                modWorkspace.getNamespace(),
                modWorkspace.getName(),
                LOGGED_IN_USER_EMAIL,
                WorkspaceAccessLevel.OWNER)
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)

        mockBillingProjectBuffer("cloned-ns")
        val cloned = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()

        val clonedDbWorkspace = workspaceDao!!.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
                cloned.getNamespace(),
                cloned.getId(),
                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)

        val dataSets = dataSetService!!.getDataSets(clonedDbWorkspace)
        assertThat(dataSets).hasSize(1)
        assertThat(dataSets[0].name).isEqualTo(expectedDatasetName)
        assertThat(dataSets[0].dataSetId).isNotEqualTo(originalDataSet.dataSetId)

        val conceptSets = dataSetService!!.getConceptSetsForDataset(dataSets[0])
        assertThat(conceptSets).hasSize(1)
        assertThat(conceptSets[0].name).isEqualTo(expectedConceptSetName)
        assertThat(conceptSets[0].description).isEqualTo(expectedConceptSetDescription)
        assertThat(conceptSets[0].domainEnum).isEqualTo(Domain.CONDITION)
        assertThat(conceptSets[0].conceptIds)
                .isEqualTo(setOf(CLIENT_CONCEPT_1.getConceptId()))
        assertThat(conceptSets[0].conceptSetId)
                .isNotEqualTo(originalConceptSet.conceptSetId)

        val cohorts = dataSetService!!.getCohortsForDataset(dataSets[0])
        assertThat(cohorts).hasSize(1)
        assertThat(cohorts[0].name).isEqualTo(expectedCohortName)
        assertThat(cohorts[0].description).isEqualTo(expectedCohortDescription)
        assertThat(cohorts[0].cohortId).isNotEqualTo(originalCohort.cohortId)

        val cohortReviews = cohortReviewDao!!.findAllByCohortId(cohorts[0].cohortId)
        assertThat(cohortReviews).hasSize(1)
        assertThat(cohortReviews.iterator().next().cohortName).isEqualTo(expectedCohortReviewName)
        assertThat(cohortReviews.iterator().next().cohortDefinition)
                .isEqualTo(expectedCohortReviewDefinition)
        assertThat(cohortReviews.iterator().next().cohortReviewId)
                .isNotEqualTo(originalCohortReview.cohortReviewId)
    }

    private fun assertConceptSetClone(
            clonedConceptSet: ConceptSet,
            originalConceptSet: ConceptSet,
            clonedWorkspace: Workspace,
            participantCount: Long) {
        var clonedConceptSet = clonedConceptSet
        // Get the full concept set in order to retrieve the concepts.
        clonedConceptSet = conceptSetsController!!
                .getConceptSet(
                        clonedWorkspace.getNamespace(), clonedWorkspace.getId(), clonedConceptSet.getId())
                .body
        assertThat(clonedConceptSet.getName()).isEqualTo(originalConceptSet.getName())
        assertThat(clonedConceptSet.getDomain()).isEqualTo(originalConceptSet.getDomain())
        assertThat(clonedConceptSet.getConcepts()).isEqualTo(originalConceptSet.getConcepts())
        assertThat(clonedConceptSet.getCreator()).isEqualTo(clonedWorkspace.getCreator())
        assertThat(clonedConceptSet.getCreationTime()).isEqualTo(clonedWorkspace.getCreationTime())
        assertThat(clonedConceptSet.getLastModifiedTime())
                .isEqualTo(clonedWorkspace.getLastModifiedTime())
        assertThat(clonedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(1))
        assertThat(clonedConceptSet.getParticipantCount()).isEqualTo(participantCount)
    }

    private fun assertCohortAnnotationDefinitions(
            responseList: CohortAnnotationDefinitionListResponse,
            expectedCads: List<CohortAnnotationDefinition>,
            cohortId: Long?) {
        assertThat(responseList.getItems().size()).isEqualTo(expectedCads.size)
        var i = 0
        for (clonedDefinition in responseList.getItems()) {
            val expectedCad = expectedCads[i++]
            assertThat(clonedDefinition.getCohortAnnotationDefinitionId())
                    .isNotEqualTo(expectedCad.getCohortAnnotationDefinitionId())
            assertThat(clonedDefinition.getCohortId()).isEqualTo(cohortId)
            assertThat(clonedDefinition.getColumnName()).isEqualTo(expectedCad.getColumnName())
            assertThat(clonedDefinition.getAnnotationType()).isEqualTo(expectedCad.getAnnotationType())
            assertThat(clonedDefinition.getEnumValues()).isEqualTo(expectedCad.getEnumValues())
        }
    }

    private fun assertParticipantCohortAnnotation(
            pcaResponseList: ParticipantCohortAnnotationListResponse,
            cadResponseList: CohortAnnotationDefinitionListResponse,
            expectedPcas: List<ParticipantCohortAnnotation>,
            cohortReviewId: Long?,
            participantId: Long?) {
        assertThat(pcaResponseList.getItems().size()).isEqualTo(expectedPcas.size)
        var i = 0
        for (clonedAnnotation in pcaResponseList.getItems()) {
            val expectedPca = expectedPcas[i]
            assertThat(clonedAnnotation.getAnnotationId()).isNotEqualTo(expectedPca.getAnnotationId())
            assertThat(clonedAnnotation.getAnnotationValueEnum())
                    .isEqualTo(expectedPca.getAnnotationValueEnum())
            assertThat(clonedAnnotation.getCohortAnnotationDefinitionId())
                    .isEqualTo(cadResponseList.getItems().get(i++).getCohortAnnotationDefinitionId())
            assertThat(clonedAnnotation.getCohortReviewId()).isEqualTo(cohortReviewId)
            assertThat(clonedAnnotation.getParticipantId()).isEqualTo(participantId)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceWithNotebooks() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")

        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)

        val fcWorkspace = stubCloneWorkspace(
                modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)
        fcWorkspace.setBucketName("bucket2")
        val f1 = NotebooksService.withNotebookExtension("notebooks/f1")
        val f2 = NotebooksService.withNotebookExtension("notebooks/f2 with spaces")
        val f3 = "foo/f3.vcf"
        // Note: mockBlob cannot be inlined into thenReturn() due to Mockito nuances.
        val blobs = ImmutableList.of(
                mockBlob(BUCKET_NAME, f1), mockBlob(BUCKET_NAME, f2), mockBlob(BUCKET_NAME, f3))
        `when`(cloudStorageService!!.getBlobList(BUCKET_NAME)).thenReturn(blobs)
        mockBillingProjectBuffer("cloned-ns")
        workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()
        verify<CloudStorageService>(cloudStorageService).copyBlob(BlobId.of(BUCKET_NAME, f1), BlobId.of("bucket2", f1))
        verify<CloudStorageService>(cloudStorageService).copyBlob(BlobId.of(BUCKET_NAME, f2), BlobId.of("bucket2", f2))
        verify<CloudStorageService>(cloudStorageService).copyBlob(BlobId.of(BUCKET_NAME, f3), BlobId.of("bucket2", f3))
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceDifferentOwner() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        val cloner = User()
        cloner.email = "cloner@gmail.com"
        cloner.userId = 456L
        cloner.disabled = false
        currentUser = userDao!!.save(cloner)

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com")

        mockBillingProjectBuffer("cloned-ns")

        val workspace2 = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()

        assertThat(workspace2.getCreator()).isEqualTo(cloner.email)
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceCdrVersion() {
        var cdrVersion2 = CdrVersion()
        cdrVersion2.name = "2"
        cdrVersion2.cdrDbName = ""
        cdrVersion2 = cdrVersionDao!!.save(cdrVersion2)
        val cdrVersionId2 = java.lang.Long.toString(cdrVersion2.cdrVersionId)

        val workspace = workspacesController!!.createWorkspace(createWorkspace()).body

        val modWorkspace = Workspace()
                .name("cloned")
                .namespace("cloned-ns")
                .researchPurpose(workspace.getResearchPurpose())
                .cdrVersionId(cdrVersionId2)
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com")

        mockBillingProjectBuffer("cloned-ns")

        val req = CloneWorkspaceRequest().workspace(modWorkspace)
        val workspace2 = workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()

        assertThat(workspace2.getCdrVersionId()).isEqualTo(cdrVersionId2)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testCloneWorkspaceBadCdrVersion() {
        val workspace = workspacesController!!.createWorkspace(createWorkspace()).body

        val modWorkspace = Workspace()
                .name("cloned")
                .namespace("cloned-ns")
                .researchPurpose(workspace.getResearchPurpose())
                .cdrVersionId("bad-cdr-version-id")
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com")
        mockBillingProjectBuffer("cloned-ns")
        workspacesController.cloneWorkspace(
                workspace.getNamespace(),
                workspace.getId(),
                CloneWorkspaceRequest().workspace(modWorkspace))
    }

    @Test(expected = FailedPreconditionException::class)
    @Throws(Exception::class)
    fun testCloneWorkspaceArchivedCdrVersionThrows() {
        val workspace = workspacesController!!.createWorkspace(createWorkspace()).body

        val modWorkspace = Workspace()
                .name("cloned")
                .namespace("cloned-ns")
                .researchPurpose(workspace.getResearchPurpose())
                .cdrVersionId(archivedCdrVersionId)
        stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com")
        mockBillingProjectBuffer("cloned-ns")
        workspacesController.cloneWorkspace(
                workspace.getNamespace(),
                workspace.getId(),
                CloneWorkspaceRequest().workspace(modWorkspace))
    }

    @Test
    @Throws(Exception::class)
    fun testCloneWorkspaceIncludeUserRoles() {
        stubFcGetGroup()
        val cloner = createUser("cloner@gmail.com")
        val reader = createUser("reader@gmail.com")
        val writer = createUser("writer@gmail.com")
        val workspace = workspacesController!!.createWorkspace(createWorkspace()).body
        val collaborators = ArrayList(
                Arrays.asList(
                        UserRole().email(cloner.email).role(WorkspaceAccessLevel.OWNER),
                        UserRole().email(LOGGED_IN_USER_EMAIL).role(WorkspaceAccessLevel.OWNER),
                        UserRole().email(reader.email).role(WorkspaceAccessLevel.READER),
                        UserRole().email(writer.email).role(WorkspaceAccessLevel.WRITER)))

        stubFcUpdateWorkspaceACL()
        val workspaceAclsFromCloned = createWorkspaceACL(
                JSONObject()
                        .put(
                                "cloner@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "OWNER")
                                        .put("canCompute", true)
                                        .put("canShare", true)))

        val workspaceAclsFromOriginal = createWorkspaceACL(
                JSONObject()
                        .put(
                                "cloner@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "READER")
                                        .put("canCompute", true)
                                        .put("canShare", true))
                        .put(
                                "reader@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "READER")
                                        .put("canCompute", false)
                                        .put("canShare", false))
                        .put(
                                "writer@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "WRITER")
                                        .put("canCompute", true)
                                        .put("canShare", false))
                        .put(
                                LOGGED_IN_USER_EMAIL,
                                JSONObject()
                                        .put("accessLevel", "OWNER")
                                        .put("canCompute", true)
                                        .put("canShare", true)))

        `when`<Any>(fireCloudService!!.getWorkspaceAcl("cloned-ns", "cloned"))
                .thenReturn(workspaceAclsFromCloned)
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(workspace.getNamespace(), workspace.getName()))
                .thenReturn(workspaceAclsFromOriginal)

        currentUser = cloner

        val modWorkspace = Workspace()
                .namespace("cloned-ns")
                .name("cloned")
                .researchPurpose(workspace.getResearchPurpose())

        stubCloneWorkspace("cloned-ns", "cloned", cloner.email)
        mockBillingProjectBuffer("cloned-ns")

        val workspace2 = workspacesController
                .cloneWorkspace(
                        workspace.getNamespace(),
                        workspace.getId(),
                        CloneWorkspaceRequest().includeUserRoles(true).workspace(modWorkspace))
                .body
                .getWorkspace()

        assertThat(workspace2.getCreator()).isEqualTo(cloner.email)
        val updateACLRequestList = convertUserRolesToUpdateAclRequestList(collaborators)

        verify<FireCloudService>(fireCloudService)
                .updateWorkspaceACL(
                        eq("cloned-ns"),
                        eq("cloned"),
                        // Accept the ACL update list in any order.
                        argThat<List<WorkspaceACLUpdate>> { arg -> HashSet(updateACLRequestList) == HashSet(arg) })
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testCloneWorkspaceBadRequest() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        req.setWorkspace(modWorkspace)
        // Missing research purpose.
        workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun testClonePermissionDenied() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        // Clone with a different user.
        val cloner = User()
        cloner.email = "cloner@gmail.com"
        cloner.userId = 456L
        cloner.disabled = false
        currentUser = userDao!!.save(cloner)

        // Permission denied manifests as a 404 in Firecloud.
        `when`<Any>(fireCloudService!!.getWorkspace(workspace.getNamespace(), workspace.getName()))
                .thenThrow(NotFoundException())
        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")
        req.setWorkspace(modWorkspace)
        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
    }

    @Test(expected = FailedPreconditionException::class)
    @Throws(Exception::class)
    fun testCloneWithMassiveNotebook() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body

        val req = CloneWorkspaceRequest()
        val modWorkspace = Workspace()
        modWorkspace.setName("cloned")
        modWorkspace.setNamespace("cloned-ns")

        val modPurpose = ResearchPurpose()
        modPurpose.setAncestry(true)
        modWorkspace.setResearchPurpose(modPurpose)
        req.setWorkspace(modWorkspace)
        val fcWorkspace = testMockFactory!!.createFcWorkspace(
                modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL)
        fcWorkspace.setBucketName("bucket2")
        stubGetWorkspace(fcWorkspace, WorkspaceAccessLevel.OWNER)
        val bigNotebook = mockBlob(BUCKET_NAME, NotebooksService.withNotebookExtension("notebooks/nb"))
        `when`(bigNotebook.size).thenReturn(5_000_000_000L) // 5 GB.
        `when`(cloudStorageService!!.getBlobList(BUCKET_NAME)).thenReturn(ImmutableList.of(bigNotebook))
        mockBillingProjectBuffer("cloned-ns")
        workspacesController
                .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
                .body
                .getWorkspace()
    }

    @Test
    @Throws(Exception::class)
    fun testShareWorkspace() {
        stubFcGetGroup()
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false

        writerUser = userDao!!.save(writerUser)
        var readerUser = User()
        readerUser.email = "readerfriend@gmail.com"
        readerUser.userId = 125L
        readerUser.disabled = false
        readerUser = userDao!!.save(readerUser)

        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)

        val reader = UserRole()
        reader.setEmail("readerfriend@gmail.com")
        reader.setRole(WorkspaceAccessLevel.READER)
        shareWorkspaceRequest.addItemsItem(reader)
        val writer = UserRole()
        writer.setEmail("writerfriend@gmail.com")
        writer.setRole(WorkspaceAccessLevel.WRITER)
        shareWorkspaceRequest.addItemsItem(writer)

        // Simulate time between API calls to trigger last-modified/@Version changes.
        CLOCK.increment(1000)
        stubFcUpdateWorkspaceACL()
        val shareResp = workspacesController
                .shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
                .body
        verify<WorkspaceAuditAdapterService>(mockWorkspaceAuditAdapterService).fireCollaborateAction(anyLong(), anyMap())
        val workspace2 = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getName())
                .body
                .getWorkspace()
        assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag())

        val updateACLRequestList = convertUserRolesToUpdateAclRequestList(shareWorkspaceRequest.getItems())
        verify<FireCloudService>(fireCloudService).updateWorkspaceACL(ArgumentMatchers.any(), ArgumentMatchers.any(), eq<ArrayList<WorkspaceACLUpdate>>(updateACLRequestList))
    }

    @Test
    @Throws(Exception::class)
    fun testShareWorkspaceAddBillingProjectUser() {
        stubFcGetGroup()
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false

        writerUser = userDao!!.save(writerUser)
        var ownerUser = User()
        ownerUser.email = "ownerfriend@gmail.com"
        ownerUser.userId = 125L
        ownerUser.disabled = false
        ownerUser = userDao!!.save(ownerUser)

        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val shareWorkspaceRequest = ShareWorkspaceRequest()
                .workspaceEtag(workspace.getEtag())
                .addItemsItem(
                        UserRole().email(LOGGED_IN_USER_EMAIL).role(WorkspaceAccessLevel.OWNER))
                .addItemsItem(
                        UserRole().email(writerUser.email).role(WorkspaceAccessLevel.WRITER))
                .addItemsItem(
                        UserRole().email(ownerUser.email).role(WorkspaceAccessLevel.OWNER))

        stubFcUpdateWorkspaceACL()
        workspacesController.shareWorkspace(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
        verify<FireCloudService>(fireCloudService, times(1))
                .addUserToBillingProject(ownerUser.email, workspace.getNamespace())
        verify<FireCloudService>(fireCloudService, never()).addUserToBillingProject(eq(writerUser.email), ArgumentMatchers.any())
        verify<FireCloudService>(fireCloudService, never()).removeUserFromBillingProject(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    @Throws(Exception::class)
    fun testShareWorkspaceRemoveBillingProjectUser() {
        stubFcGetGroup()
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false

        writerUser = userDao!!.save(writerUser)
        var ownerUser = User()
        ownerUser.email = "ownerfriend@gmail.com"
        ownerUser.userId = 125L
        ownerUser.disabled = false
        ownerUser = userDao!!.save(ownerUser)

        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(
                        createWorkspaceACL(
                                JSONObject()
                                        .put(
                                                currentUser!!.email,
                                                JSONObject()
                                                        .put("accessLevel", "OWNER")
                                                        .put("canCompute", true)
                                                        .put("canShare", true))
                                        .put(
                                                writerUser.email,
                                                JSONObject()
                                                        .put("accessLevel", "WRITER")
                                                        .put("canCompute", true)
                                                        .put("canShare", true))
                                        .put(
                                                ownerUser.email,
                                                JSONObject()
                                                        .put("accessLevel", "OWNER")
                                                        .put("canCompute", true)
                                                        .put("canShare", true))))

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val shareWorkspaceRequest = ShareWorkspaceRequest()
                .workspaceEtag(workspace.getEtag())
                .addItemsItem(
                        UserRole().email(LOGGED_IN_USER_EMAIL).role(WorkspaceAccessLevel.OWNER))
                // Removed WRITER, demoted OWNER to READER.
                .addItemsItem(
                        UserRole().email(ownerUser.email).role(WorkspaceAccessLevel.READER))

        stubFcUpdateWorkspaceACL()
        workspacesController.shareWorkspace(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
        verify<FireCloudService>(fireCloudService, times(1))
                .removeUserFromBillingProject(ownerUser.email, workspace.getNamespace())
        verify<FireCloudService>(fireCloudService, never())
                .removeUserFromBillingProject(eq(writerUser.email), ArgumentMatchers.any())
        verify<FireCloudService>(fireCloudService, never()).addUserToBillingProject(ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    @Test
    @Throws(Exception::class)
    fun testShareWorkspaceNoRoleFailure() {
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false

        writerUser = userDao!!.save(writerUser)

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)
        val writer = UserRole()
        writer.setEmail("writerfriend@gmail.com")
        shareWorkspaceRequest.addItemsItem(writer)

        // Simulate time between API calls to trigger last-modified/@Version changes.
        CLOCK.increment(1000)
        stubFcUpdateWorkspaceACL()
        try {
            workspacesController.shareWorkspace(
                    workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
            fail("expected bad request exception for no role")
        } catch (e: BadRequestException) {
            // Expected
        }

    }

    @Test
    @Throws(Exception::class)
    fun testUnshareWorkspace() {
        stubFcGetGroup()
        var writerUser = User()
        writerUser.email = "writerfriend@gmail.com"
        writerUser.userId = 124L
        writerUser.disabled = false
        writerUser = userDao!!.save(writerUser)
        var readerUser = User()
        readerUser.email = "readerfriend@gmail.com"
        readerUser.userId = 125L
        readerUser.disabled = false
        readerUser = userDao!!.save(readerUser)

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        var shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)
        val writer = UserRole()
        writer.setEmail("writerfriend@gmail.com")
        writer.setRole(WorkspaceAccessLevel.WRITER)
        shareWorkspaceRequest.addItemsItem(writer)
        val reader = UserRole()
        reader.setEmail("readerfriend@gmail.com")
        reader.setRole(WorkspaceAccessLevel.NO_ACCESS)
        shareWorkspaceRequest.addItemsItem(reader)

        // Mock firecloud ACLs
        val workspaceACLs = createWorkspaceACL(
                JSONObject()
                        .put(
                                LOGGED_IN_USER_EMAIL,
                                JSONObject()
                                        .put("accessLevel", "OWNER")
                                        .put("canCompute", true)
                                        .put("canShare", true))
                        .put(
                                "writerfriend@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "WRITER")
                                        .put("canCompute", true)
                                        .put("canShare", false))
                        .put(
                                "readerfriend@gmail.com",
                                JSONObject()
                                        .put("accessLevel", "READER")
                                        .put("canCompute", false)
                                        .put("canShare", false)))
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(workspaceACLs)

        CLOCK.increment(1000)
        stubFcUpdateWorkspaceACL()
        shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        shareWorkspaceRequest.addItemsItem(creator)
        shareWorkspaceRequest.addItemsItem(writer)

        val shareResp = workspacesController
                .shareWorkspace(workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
                .body
        val workspace2 = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getWorkspace()
        assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag())

        // add the reader with NO_ACCESS to mock
        shareWorkspaceRequest.addItemsItem(reader)
        val updateACLRequestList = convertUserRolesToUpdateAclRequestList(shareWorkspaceRequest.getItems())
        verify<FireCloudService>(fireCloudService)
                .updateWorkspaceACL(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        eq(
                                updateACLRequestList.stream()
                                        .sorted(Comparator.comparing(Function<WorkspaceACLUpdate, Any> { WorkspaceACLUpdate.getEmail() }))
                                        .collect(Collectors.toList<Any>())))
    }

    @Test
    @Throws(Exception::class)
    fun testStaleShareWorkspace() {
        stubFcGetGroup()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        var shareWorkspaceRequest = ShareWorkspaceRequest()
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)

        // Simulate time between API calls to trigger last-modified/@Version changes.
        CLOCK.increment(1000)
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()
        workspacesController.shareWorkspace(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)

        // Simulate time between API calls to trigger last-modified/@Version changes.
        CLOCK.increment(1000)
        shareWorkspaceRequest = ShareWorkspaceRequest()
        // Use the initial etag, not the updated value from shareWorkspace.
        shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag())
        try {
            workspacesController.shareWorkspace(
                    workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
            fail("expected conflict exception when sharing with stale etag")
        } catch (e: ConflictException) {
            // Expected
        }

    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testUnableToShareWithNonExistentUser() {
        val workspace = createWorkspace()
        workspacesController!!.createWorkspace(workspace)
        val shareWorkspaceRequest = ShareWorkspaceRequest()
        val creator = UserRole()
        creator.setEmail(LOGGED_IN_USER_EMAIL)
        creator.setRole(WorkspaceAccessLevel.OWNER)
        shareWorkspaceRequest.addItemsItem(creator)
        val writer = UserRole()
        writer.setEmail("writerfriend@gmail.com")
        writer.setRole(WorkspaceAccessLevel.WRITER)
        shareWorkspaceRequest.addItemsItem(writer)
        workspacesController.shareWorkspace(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testNotebookFileList() {
        `when`<Any>(fireCloudService!!.getWorkspace("project", "workspace"))
                .thenReturn(
                        org.pmiops.workbench.firecloud.model.WorkspaceResponse()
                                .workspace(
                                        org.pmiops.workbench.firecloud.model.Workspace().bucketName("bucket")))
        val mockBlob1 = mock(Blob::class.java)
        val mockBlob2 = mock(Blob::class.java)
        val mockBlob3 = mock(Blob::class.java)
        `when`(mockBlob1.name)
                .thenReturn(NotebooksService.withNotebookExtension("notebooks/mockFile"))
        `when`(mockBlob2.name).thenReturn("notebooks/mockFile.text")
        `when`(mockBlob3.name)
                .thenReturn(NotebooksService.withNotebookExtension("notebooks/two words"))
        `when`(cloudStorageService!!.getBlobListForPrefix("bucket", "notebooks"))
                .thenReturn(ImmutableList.of(mockBlob1, mockBlob2, mockBlob3))

        // Will return 1 entry as only python files in notebook folder are return
        val gotNames = workspacesController!!.getNoteBookList("project", "workspace").body.stream()
                .map<Any> { details -> details.getName() }
                .collect<List<String>, Any>(Collectors.toList<Any>())
        assertEquals(
                gotNames,
                ImmutableList.of(
                        NotebooksService.withNotebookExtension("mockFile"),
                        NotebooksService.withNotebookExtension("two words")))
    }

    @Test
    @Throws(Exception::class)
    fun testNotebookFileListOmitsExtraDirectories() {
        `when`<Any>(fireCloudService!!.getWorkspace("project", "workspace"))
                .thenReturn(
                        org.pmiops.workbench.firecloud.model.WorkspaceResponse()
                                .workspace(
                                        org.pmiops.workbench.firecloud.model.Workspace().bucketName("bucket")))
        val mockBlob1 = mock(Blob::class.java)
        val mockBlob2 = mock(Blob::class.java)
        `when`(mockBlob1.name)
                .thenReturn(NotebooksService.withNotebookExtension("notebooks/extra/nope"))
        `when`(mockBlob2.name).thenReturn(NotebooksService.withNotebookExtension("notebooks/foo"))
        `when`(cloudStorageService!!.getBlobListForPrefix("bucket", "notebooks"))
                .thenReturn(ImmutableList.of(mockBlob1, mockBlob2))

        val gotNames = workspacesController!!.getNoteBookList("project", "workspace").body.stream()
                .map<Any> { details -> details.getName() }
                .collect<List<String>, Any>(Collectors.toList<Any>())
        assertEquals(gotNames, ImmutableList.of(NotebooksService.withNotebookExtension("foo")))
    }

    @Test
    @Throws(Exception::class)
    fun testNotebookFileListNotFound() {
        `when`<Any>(fireCloudService!!.getWorkspace("mockProject", "mockWorkspace"))
                .thenThrow(NotFoundException())
        try {
            workspacesController!!.getNoteBookList("mockProject", "mockWorkspace")
            fail()
        } catch (ex: NotFoundException) {
            // Expected
        }

    }

    @Test
    @Throws(Exception::class)
    fun testEmptyFireCloudWorkspaces() {
        `when`<List<WorkspaceResponse>>(fireCloudService!!.getWorkspaces(ArgumentMatchers.any()))
                .thenReturn(ArrayList<org.pmiops.workbench.firecloud.model.WorkspaceResponse>())
        try {
            val response = workspacesController!!.workspaces
            assertThat(response.body.getItems()).isEmpty()
        } catch (ex: Exception) {
            fail()
        }

    }

    @Test
    @Throws(Exception::class)
    fun testRenameNotebookInWorkspace() {
        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val nb1 = NotebooksService.withNotebookExtension("notebooks/nb1")
        val newName = NotebooksService.withNotebookExtension("nb2")
        val newPath = NotebooksService.withNotebookExtension("notebooks/nb2")
        val fullPath = "gs://workspace-bucket/$newPath"
        val origFullPath = "gs://workspace-bucket/$nb1"
        val workspaceIdInDb: Long = 1
        val userIdInDb: Long = 1
        val rename = NotebookRename()
        rename.setName(NotebooksService.withNotebookExtension("nb1"))
        rename.setNewName(newName)
        workspacesController.renameNotebook(workspace.getNamespace(), workspace.getId(), rename)
        verify<CloudStorageService>(cloudStorageService)
                .copyBlob(BlobId.of(BUCKET_NAME, nb1), BlobId.of(BUCKET_NAME, newPath))
        verify<CloudStorageService>(cloudStorageService).deleteBlob(BlobId.of(BUCKET_NAME, nb1))
        verify<UserRecentResourceService>(userRecentResourceService)
                .updateNotebookEntry(workspaceIdInDb, userIdInDb, fullPath, NOW)
        verify<UserRecentResourceService>(userRecentResourceService)
                .deleteNotebookEntry(workspaceIdInDb, userIdInDb, origFullPath)
    }

    @Test
    @Throws(Exception::class)
    fun testRenameNotebookWoExtension() {
        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val nb1 = NotebooksService.withNotebookExtension("notebooks/nb1")
        val newName = "nb2"
        val newPath = NotebooksService.withNotebookExtension("notebooks/nb2")
        val fullPath = "gs://workspace-bucket/$newPath"
        val origFullPath = "gs://workspace-bucket/$nb1"
        val workspaceIdInDb: Long = 1
        val userIdInDb: Long = 1
        val rename = NotebookRename()
        rename.setName(NotebooksService.withNotebookExtension("nb1"))
        rename.setNewName(newName)
        workspacesController.renameNotebook(workspace.getNamespace(), workspace.getId(), rename)
        verify<CloudStorageService>(cloudStorageService)
                .copyBlob(BlobId.of(BUCKET_NAME, nb1), BlobId.of(BUCKET_NAME, newPath))
        verify<CloudStorageService>(cloudStorageService).deleteBlob(BlobId.of(BUCKET_NAME, nb1))
        verify<UserRecentResourceService>(userRecentResourceService)
                .updateNotebookEntry(workspaceIdInDb, userIdInDb, fullPath, NOW)
        verify<UserRecentResourceService>(userRecentResourceService)
                .deleteNotebookEntry(workspaceIdInDb, userIdInDb, origFullPath)
    }

    @Test
    fun copyNotebook() {
        stubFcGetWorkspaceACL()
        var fromWorkspace = createWorkspace()
        fromWorkspace = workspacesController!!.createWorkspace(fromWorkspace).body
        val fromNotebookName = "origin"

        var toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace")
        toWorkspace = workspacesController.createWorkspace(toWorkspace).body
        val newNotebookName = "new"
        val expectedNotebookName = newNotebookName + NotebooksService.NOTEBOOK_EXTENSION

        val copyNotebookRequest = CopyRequest()
                .toWorkspaceName(toWorkspace.getName())
                .toWorkspaceNamespace(toWorkspace.getNamespace())
                .newName(newNotebookName)

        workspacesController.copyNotebook(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest)

        verify<CloudStorageService>(cloudStorageService)
                .copyBlob(
                        BlobId.of(
                                BUCKET_NAME,
                                "notebooks/" + NotebooksService.withNotebookExtension(fromNotebookName)),
                        BlobId.of(BUCKET_NAME, "notebooks/$expectedNotebookName"))

        verify<UserRecentResourceService>(userRecentResourceService)
                .updateNotebookEntry(
                        2L, 1L, "gs://workspace-bucket/notebooks/$expectedNotebookName", NOW)
    }

    @Test
    fun copyNotebook_onlyAppendsSuffixIfNeeded() {
        stubFcGetWorkspaceACL()
        var fromWorkspace = createWorkspace()
        fromWorkspace = workspacesController!!.createWorkspace(fromWorkspace).body
        val fromNotebookName = "origin"

        var toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace")
        toWorkspace = workspacesController.createWorkspace(toWorkspace).body
        val newNotebookName = NotebooksService.withNotebookExtension("new")

        val copyNotebookRequest = CopyRequest()
                .toWorkspaceName(toWorkspace.getName())
                .toWorkspaceNamespace(toWorkspace.getNamespace())
                .newName(newNotebookName)

        workspacesController.copyNotebook(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest)

        verify<CloudStorageService>(cloudStorageService)
                .copyBlob(
                        BlobId.of(
                                BUCKET_NAME,
                                "notebooks/" + NotebooksService.withNotebookExtension(fromNotebookName)),
                        BlobId.of(BUCKET_NAME, "notebooks/$newNotebookName"))
    }

    @Test(expected = ForbiddenException::class)
    fun copyNotebook_onlyHasReadPermissionsToDestination() {
        stubFcGetWorkspaceACL(createWorkspaceACLWithPermission(WorkspaceAccessLevel.READER))
        var fromWorkspace = createWorkspace()
        fromWorkspace = workspacesController!!.createWorkspace(fromWorkspace).body
        val fromNotebookName = "origin"

        var toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace")
        toWorkspace = workspacesController.createWorkspace(toWorkspace).body
        stubGetWorkspace(
                toWorkspace.getNamespace(),
                toWorkspace.getName(),
                LOGGED_IN_USER_EMAIL,
                WorkspaceAccessLevel.READER)
        val newNotebookName = "new"

        val copyNotebookRequest = CopyRequest()
                .toWorkspaceName(toWorkspace.getName())
                .toWorkspaceNamespace(toWorkspace.getNamespace())
                .newName(newNotebookName)

        workspacesController.copyNotebook(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest)
    }

    @Test(expected = ForbiddenException::class)
    fun copyNotebook_noAccessOnSource() {
        var fromWorkspace = createWorkspace("fromWorkspaceNs", "fromworkspace")
        fromWorkspace = workspacesController!!.createWorkspace(fromWorkspace).body
        stubGetWorkspace(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                LOGGED_IN_USER_EMAIL,
                WorkspaceAccessLevel.NO_ACCESS)
        stubFcGetWorkspaceACLForWorkspace(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                createWorkspaceACLWithPermission(WorkspaceAccessLevel.NO_ACCESS))
        val fromNotebookName = "origin"

        var toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace")
        toWorkspace = workspacesController.createWorkspace(toWorkspace).body
        stubGetWorkspace(
                toWorkspace.getNamespace(),
                toWorkspace.getName(),
                LOGGED_IN_USER_EMAIL,
                WorkspaceAccessLevel.WRITER)
        stubFcGetWorkspaceACLForWorkspace(
                toWorkspace.getNamespace(),
                toWorkspace.getName(),
                createWorkspaceACLWithPermission(WorkspaceAccessLevel.WRITER))
        val newNotebookName = "new"

        val copyNotebookRequest = CopyRequest()
                .toWorkspaceName(toWorkspace.getName())
                .toWorkspaceNamespace(toWorkspace.getNamespace())
                .newName(newNotebookName)

        workspacesController.copyNotebook(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest)
    }

    @Test(expected = ConflictException::class)
    fun copyNotebook_alreadyExists() {
        stubFcGetWorkspaceACL()
        var fromWorkspace = createWorkspace()
        fromWorkspace = workspacesController!!.createWorkspace(fromWorkspace).body
        val fromNotebookName = "origin"

        var toWorkspace = createWorkspace("toWorkspaceNs", "toworkspace")
        toWorkspace = workspacesController.createWorkspace(toWorkspace).body
        val newNotebookName = NotebooksService.withNotebookExtension("new")

        val copyNotebookRequest = CopyRequest()
                .toWorkspaceName(toWorkspace.getName())
                .toWorkspaceNamespace(toWorkspace.getNamespace())
                .newName(newNotebookName)

        val newBlobId = BlobId.of(BUCKET_NAME, "notebooks/$newNotebookName")

        doReturn(setOf(newBlobId))
                .`when`<CloudStorageService>(cloudStorageService)
                .blobsExist(listOf(newBlobId))

        workspacesController.copyNotebook(
                fromWorkspace.getNamespace(),
                fromWorkspace.getName(),
                fromNotebookName,
                copyNotebookRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testCloneNotebook() {
        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val nb1 = NotebooksService.withNotebookExtension("notebooks/nb1")
        val newPath = NotebooksService.withNotebookExtension("notebooks/Duplicate of nb1")
        val fullPath = "gs://workspace-bucket/$newPath"
        val workspaceIdInDb: Long = 1
        val userIdInDb: Long = 1
        workspacesController.cloneNotebook(
                workspace.getNamespace(), workspace.getId(), NotebooksService.withNotebookExtension("nb1"))
        verify<CloudStorageService>(cloudStorageService)
                .copyBlob(BlobId.of(BUCKET_NAME, nb1), BlobId.of(BUCKET_NAME, newPath))
        verify<UserRecentResourceService>(userRecentResourceService)
                .updateNotebookEntry(workspaceIdInDb, userIdInDb, fullPath, NOW)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteNotebook() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        val nb1 = NotebooksService.withNotebookExtension("notebooks/nb1")
        val fullPath = "gs://workspace-bucket/$nb1"
        val workspaceIdInDb: Long = 1
        val userIdInDb: Long = 1
        workspacesController.deleteNotebook(
                workspace.getNamespace(), workspace.getId(), NotebooksService.withNotebookExtension("nb1"))
        verify<CloudStorageService>(cloudStorageService).deleteBlob(BlobId.of(BUCKET_NAME, nb1))
        verify<UserRecentResourceService>(userRecentResourceService).deleteNotebookEntry(workspaceIdInDb, userIdInDb, fullPath)
    }

    @Test
    fun testPublishUnpublishWorkspace() {
        stubFcGetGroup()
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        workspacesController.publishWorkspace(workspace.getNamespace(), workspace.getId())

        workspace = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getWorkspace()
        assertThat(workspace.getPublished()).isTrue()

        workspacesController.unpublishWorkspace(workspace.getNamespace(), workspace.getId())
        workspace = workspacesController
                .getWorkspace(workspace.getNamespace(), workspace.getId())
                .body
                .getWorkspace()
        assertThat(workspace.getPublished()).isFalse()
    }

    @Test
    fun testGetPublishedWorkspaces() {
        stubFcGetGroup()
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        workspacesController.publishWorkspace(workspace.getNamespace(), workspace.getId())

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(
                testMockFactory!!.createFcWorkspace(workspace.getNamespace(), workspace.getName(), null))
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())
        doReturn(listOf<Any>(fcResponse)).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())

        assertThat(workspacesController.publishedWorkspaces.body.getItems().size())
                .isEqualTo(1)
    }

    @Test
    fun testGetWorkspacesGetsPublishedIfOwner() {
        stubFcGetGroup()
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        workspacesController.publishWorkspace(workspace.getNamespace(), workspace.getId())

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(
                testMockFactory!!.createFcWorkspace(workspace.getNamespace(), workspace.getName(), null))
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())
        doReturn(listOf<Any>(fcResponse)).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())

        assertThat(workspacesController.workspaces.body.getItems().size()).isEqualTo(1)
    }

    @Test
    fun testGetWorkspacesGetsPublishedIfWriter() {
        stubFcGetGroup()
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        workspacesController.publishWorkspace(workspace.getNamespace(), workspace.getId())

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(
                testMockFactory!!.createFcWorkspace(workspace.getNamespace(), workspace.getName(), null))
        fcResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString())
        doReturn(listOf<Any>(fcResponse)).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())

        assertThat(workspacesController.workspaces.body.getItems().size()).isEqualTo(1)
    }

    @Test
    fun testGetWorkspacesDoesNotGetsPublishedIfReader() {
        stubFcGetGroup()
        stubFcUpdateWorkspaceACL()
        stubFcGetWorkspaceACL()

        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        workspacesController.publishWorkspace(workspace.getNamespace(), workspace.getId())

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(
                testMockFactory!!.createFcWorkspace(workspace.getNamespace(), workspace.getName(), null))
        fcResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString())
        doReturn(listOf<Any>(fcResponse)).`when`<FireCloudService>(fireCloudService).getWorkspaces(ArgumentMatchers.any())

        assertThat(workspacesController.workspaces.body.getItems().size()).isEqualTo(0)
    }

    @Test
    fun notebookLockingEmailHashTest() {
        val knownTestData = arrayOf(arrayOf("fc-bucket-id-1", "user@aou", "dc5acd54f734a2e2350f2adcb0a25a4d1978b45013b76d6bc0a2d37d035292fe"), arrayOf("fc-bucket-id-1", "another-user@aou", "bc90f9f740702e5e0408f2ea13fed9457a7ee9c01117820f5c541067064468c3"), arrayOf("fc-bucket-id-2", "user@aou", "a759e5aef091fd22bbf40bf8ee7cfde4988c668541c18633bd79ab84b274d622"),
                // catches an edge case where the hash has a leading 0
                arrayOf("fc-5ac6bde3-f225-44ca-ad4d-92eed68df7db", "brubenst2@fake-research-aou.org", "060c0b2ef2385804b7b69a4b4477dd9661be35db270c940525c2282d081aef56"))

        for (test in knownTestData) {
            val bucket = test[0]
            val email = test[1]
            val hash = test[2]

            assertThat(WorkspacesController.notebookLockingEmailHash(bucket, email)).isEqualTo(hash)
        }
    }

    private fun assertNotebookLockingMetadata(
            gcsMetadata: Map<String, String>?,
            expectedResponse: NotebookLockingMetadataResponse,
            acl: WorkspaceACL?) {

        val testWorkspaceNamespace = "test-ns"
        val testWorkspaceName = "test-ws"
        val testNotebook = NotebooksService.withNotebookExtension("test-notebook")

        val fcWorkspace = testMockFactory!!.createFcWorkspace(
                testWorkspaceNamespace, testWorkspaceName, LOGGED_IN_USER_EMAIL)
        fcWorkspace.setBucketName(BUCKET_NAME)
        stubGetWorkspace(fcWorkspace, WorkspaceAccessLevel.OWNER)
        stubFcGetWorkspaceACL(acl)

        val testNotebookPath = "notebooks/$testNotebook"
        doReturn(gcsMetadata).`when`<CloudStorageService>(cloudStorageService).getMetadata(BUCKET_NAME, testNotebookPath)

        assertThat(
                workspacesController!!
                        .getNotebookLockingMetadata(testWorkspaceNamespace, testWorkspaceName, testNotebook)
                        .body)
                .isEqualTo(expectedResponse)
    }

    @Test
    fun testNotebookLockingMetadata() {
        val lastLockedUser = LOGGED_IN_USER_EMAIL
        val lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli()

        val gcsMetadata = ImmutableMap.Builder<String, String>()
                .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
                .put(
                        LAST_LOCKING_USER_KEY,
                        WorkspacesController.notebookLockingEmailHash(BUCKET_NAME, lastLockedUser))
                .put("extraMetadata", "is not a problem")
                .build()

        // I can see that I have locked it myself, and when

        val expectedResponse = NotebookLockingMetadataResponse()
                .lockExpirationTime(lockExpirationTime)
                .lastLockedBy(lastLockedUser)

        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl)
    }

    @Test
    fun testNotebookLockingMetadataKnownUser() {
        val readerOnMyWorkspace = "some-reader@fake-research-aou.org"

        val workspaceACL = createWorkspaceACL(
                JSONObject()
                        .put(
                                currentUser!!.email,
                                JSONObject()
                                        .put("accessLevel", "OWNER")
                                        .put("canCompute", true)
                                        .put("canShare", true))
                        .put(
                                readerOnMyWorkspace,
                                JSONObject()
                                        .put("accessLevel", "READER")
                                        .put("canCompute", true)
                                        .put("canShare", true)))

        val lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli()

        val gcsMetadata = ImmutableMap.Builder<String, String>()
                .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
                .put(
                        LAST_LOCKING_USER_KEY,
                        WorkspacesController.notebookLockingEmailHash(BUCKET_NAME, readerOnMyWorkspace))
                .put("extraMetadata", "is not a problem")
                .build()

        // I'm the owner so I can see readers on my workspace

        val expectedResponse = NotebookLockingMetadataResponse()
                .lockExpirationTime(lockExpirationTime)
                .lastLockedBy(readerOnMyWorkspace)

        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, workspaceACL)
    }

    @Test
    fun testNotebookLockingMetadataUnknownUser() {
        val lastLockedUser = "a-stranger@fake-research-aou.org"
        val lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli()

        val gcsMetadata = ImmutableMap.Builder<String, String>()
                .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
                .put(
                        LAST_LOCKING_USER_KEY,
                        WorkspacesController.notebookLockingEmailHash(BUCKET_NAME, lastLockedUser))
                .put("extraMetadata", "is not a problem")
                .build()

        // This user is not listed in the Workspace ACL so I don't know them

        val expectedResponse = NotebookLockingMetadataResponse()
                .lockExpirationTime(lockExpirationTime)
                .lastLockedBy("UNKNOWN")

        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl)
    }

    @Test
    fun testNotebookLockingMetadataPlaintextUser() {
        val lastLockedUser = LOGGED_IN_USER_EMAIL
        val lockExpirationTime = Instant.now().plus(Duration.ofMinutes(1)).toEpochMilli()

        val gcsMetadata = ImmutableMap.Builder<String, String>()
                .put(LOCK_EXPIRE_TIME_KEY, lockExpirationTime.toString())
                // store directly in plaintext, to show that this does not work
                .put(LAST_LOCKING_USER_KEY, lastLockedUser)
                .put("extraMetadata", "is not a problem")
                .build()

        // in case of accidentally storing the user email in plaintext
        // it can't be retrieved by this endpoint

        val expectedResponse = NotebookLockingMetadataResponse()
                .lockExpirationTime(lockExpirationTime)
                .lastLockedBy("UNKNOWN")

        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl)
    }

    @Test
    fun testNotebookLockingNullMetadata() {
        val gcsMetadata: Map<String, String>? = null

        // This file has no metadata so the response is empty

        val expectedResponse = NotebookLockingMetadataResponse()
        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl)
    }

    @Test
    fun testNotebookLockingEmptyMetadata() {
        val gcsMetadata = HashMap<String, String>()

        // This file has no metadata so the response is empty

        val expectedResponse = NotebookLockingMetadataResponse()
        assertNotebookLockingMetadata(gcsMetadata, expectedResponse, fcWorkspaceAcl)
    }

    @Test
    fun getUserRecentWorkspaces() {
        var workspace = createWorkspace()
        workspace = workspacesController!!.createWorkspace(workspace).body
        stubFcGetWorkspaceACL()
        val dbWorkspace = workspaceService!!.get(workspace.getNamespace(), workspace.getId())
        workspaceService.updateRecentWorkspaces(dbWorkspace, currentUser!!.userId, NOW)
        val recentWorkspaceResponseEntity = workspacesController.userRecentWorkspaces
        val recentWorkspace = recentWorkspaceResponseEntity.body.get(0)
        assertThat(recentWorkspace.getWorkspace().getNamespace())
                .isEqualTo(dbWorkspace.workspaceNamespace)
        assertThat(recentWorkspace.getWorkspace().getName()).isEqualTo(dbWorkspace.name)
    }

    companion object {

        private val NOW = Timestamp.from(Instant.now())
        private val NOW_TIME = NOW.time
        private val CLOCK = FakeClock(NOW.toInstant(), ZoneId.systemDefault())
        private val LOGGED_IN_USER_EMAIL = "bob@gmail.com"
        private val BUCKET_NAME = "workspace-bucket"
        private val LOCK_EXPIRE_TIME_KEY = "lockExpiresAt"
        private val LAST_LOCKING_USER_KEY = "lastLockedBy"

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
                .conceptId(456L)
                .standardConcept(false)
                .conceptName("b concept")
                .conceptCode("conceptB")
                .conceptClassId("classId2")
                .vocabularyId("V2")
                .domainId("Condition")
                .countValue(456L)
                .prevalence(0.3f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_CONCEPT_3 = Concept()
                .conceptId(256L)
                .standardConcept(true)
                .conceptName("c concept")
                .conceptCode("conceptC")
                .conceptClassId("classId2")
                .vocabularyId("V3")
                .domainId("Measurement")
                .countValue(256L)
                .prevalence(0.4f)
                .conceptSynonyms(ArrayList<String>())
        private val CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1)
        private val CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2)
        private val CONCEPT_3 = makeConcept(CLIENT_CONCEPT_3)

        private var currentUser: User? = null
        private var fcWorkspaceAcl: WorkspaceACL? = null
    }
}
