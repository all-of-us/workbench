package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.mockito.Mockito.`when`
import org.pmiops.workbench.api.ConceptsControllerTest.makeConcept

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Random
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.audit.adapters.WorkspaceAuditAdapterService
import org.pmiops.workbench.billing.BillingProjectBufferService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.cohorts.CohortFactoryImpl
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptSet
import org.pmiops.workbench.model.CreateConceptSetRequest
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Surveys
import org.pmiops.workbench.model.UpdateConceptSetRequest
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.notebooks.NotebooksService
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
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

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConceptSetsControllerTest {
    private var workspace: Workspace? = null
    private var workspace2: Workspace? = null
    private var testMockFactory: TestMockFactory? = null

    @Autowired
    internal var billingProjectBufferService: BillingProjectBufferService? = null

    @Autowired
    internal var workspaceService: WorkspaceService? = null

    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null

    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null

    @Autowired
    internal var conceptDao: ConceptDao? = null

    @Autowired
    internal var dataSetService: DataSetService? = null

    @Autowired
    internal var workspaceDao: WorkspaceDao? = null

    @Autowired
    internal var userDao: UserDao? = null

    @Autowired
    internal var cloudStorageService: CloudStorageService? = null

    @Autowired
    internal var notebooksService: NotebooksService? = null

    @Autowired
    internal var userService: UserService? = null

    @Autowired
    internal var fireCloudService: FireCloudService? = null

    private var conceptSetsController: ConceptSetsController? = null

    @Autowired
    internal var userRecentResourceService: UserRecentResourceService? = null

    @Autowired
    internal var conceptBigQueryService: ConceptBigQueryService? = null

    @Mock
    internal var userProvider: Provider<User>? = null

    @Autowired
    internal var workbenchConfigProvider: Provider<WorkbenchConfig>? = null

    @Autowired
    internal var workspaceAuditAdapterService: WorkspaceAuditAdapterService? = null

    @TestConfiguration
    @Import(WorkspaceServiceImpl::class, CohortCloningService::class, CohortFactoryImpl::class, UserService::class, ConceptSetsController::class, WorkspacesController::class, ConceptSetService::class)
    @MockBean(BillingProjectBufferService::class, CloudStorageService::class, ComplianceService::class, ConceptBigQueryService::class, ConceptSetService::class, DataSetService::class, DirectoryService::class, FireCloudService::class, NotebooksService::class, UserRecentResourceService::class, WorkspaceAuditAdapterService::class)
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

        conceptSetsController = ConceptSetsController(
                workspaceService,
                conceptSetDao,
                conceptDao,
                conceptBigQueryService,
                userRecentResourceService,
                userProvider,
                CLOCK)
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

        testMockFactory!!.stubBufferBillingProject(billingProjectBufferService)
        testMockFactory!!.stubCreateFcWorkspace(fireCloudService)

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
        workspace!!.setNamespace(WORKSPACE_NAMESPACE)
        workspace!!.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace!!.setResearchPurpose(ResearchPurpose())
        workspace!!.setCdrVersionId(cdrVersion.cdrVersionId.toString())

        workspace2 = Workspace()
        workspace2!!.setName(WORKSPACE_NAME_2)
        workspace2!!.setNamespace(WORKSPACE_NAMESPACE)
        workspace2!!.setDataAccessLevel(DataAccessLevel.PROTECTED)
        workspace2!!.setResearchPurpose(ResearchPurpose())
        workspace2!!.setCdrVersionId(cdrVersion.cdrVersionId.toString())

        workspacesController.setUserProvider(userProvider)
        conceptSetsController!!.setUserProvider(userProvider)

        workspace = workspacesController.createWorkspace(workspace!!).body
        workspace2 = workspacesController.createWorkspace(workspace2!!).body
        stubGetWorkspace(
                workspace!!.getNamespace(), WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspaceAcl(
                workspace!!.getNamespace(), WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspace(
                workspace2!!.getNamespace(), WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER)
        stubGetWorkspaceAcl(
                workspace2!!.getNamespace(), WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER)

        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name())
        `when`<Any>(fireCloudService!!.getWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME))
                .thenReturn(fcResponse)
        `when`<Any>(fireCloudService!!.getWorkspace(workspace2!!.getNamespace(), WORKSPACE_NAME_2))
                .thenReturn(fcResponse)
    }

    @Test
    fun testGetConceptSetsInWorkspaceEmpty() {
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME)
                        .body
                        .getItems())
                .isEmpty()
    }

    @Test(expected = NotFoundException::class)
    fun testGetConceptSetNotExists() {
        conceptSetsController!!.getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, 1L)
    }

    @Test(expected = NotFoundException::class)
    fun testUpdateConceptSetNotExists() {
        val conceptSet = ConceptSet()
        conceptSet.setDescription("desc 1")
        conceptSet.setName("concept set 1")
        conceptSet.setDomain(Domain.CONDITION)
        conceptSet.setId(1L)
        conceptSet.setEtag(Etags.fromVersion(1))

        conceptSetsController!!.updateConceptSet(
                workspace!!.getNamespace(), WORKSPACE_NAME, 1L, conceptSet)
    }

    @Test
    fun testCreateConceptSet() {
        var conceptSet = ConceptSet()
        conceptSet.setDescription("desc 1")
        conceptSet.setName("concept set 1")
        conceptSet.setDomain(Domain.CONDITION)
        conceptSet = conceptSetsController!!
                .createConceptSet(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        CreateConceptSetRequest()
                                .conceptSet(conceptSet)
                                .addAddedIdsItem(CLIENT_CONCEPT_1.getConceptId()))
                .body
        assertThat(conceptSet.getConcepts()).isNotNull()
        assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli())
        assertThat(conceptSet.getDescription()).isEqualTo("desc 1")
        assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION)
        assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1))
        assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli())
        assertThat(conceptSet.getName()).isEqualTo("concept set 1")
        assertThat(conceptSet.getParticipantCount()).isEqualTo(0)

        assertThat(
                conceptSetsController!!
                        .getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                        .body)
                .isEqualTo(conceptSet)
        // Get concept sets will not return the full information, because concepts can have a lot of
        // information.
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME)
                        .body
                        .getItems())
                .contains(conceptSet.concepts(null))
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace2!!.getNamespace(), WORKSPACE_NAME_2)
                        .body
                        .getItems())
                .isEmpty()
    }

    @Test
    fun testGetSurveyConceptSet() {
        val surveyConceptSet = makeSurveyConceptSet1()
        assertThat(
                conceptSetsController!!
                        .getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, surveyConceptSet.getId())
                        .body)
                .isEqualTo(surveyConceptSet)
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME)
                        .body
                        .getItems())
                .contains(surveyConceptSet.concepts(null))
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace2!!.getNamespace(), WORKSPACE_NAME_2)
                        .body
                        .getItems())
                .isEmpty()
    }

    @Test(expected = NotFoundException::class)
    fun testGetSurveyConceptSetWrongWorkspace() {
        val conceptSet = makeSurveyConceptSet1()
        conceptSetsController!!.getConceptSet(
                workspace2!!.getNamespace(), WORKSPACE_NAME_2, conceptSet.getId())
    }

    @Test
    fun testGetConceptSet() {
        val conceptSet = makeConceptSet1()
        assertThat(
                conceptSetsController!!
                        .getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                        .body)
                .isEqualTo(conceptSet)
        // Get concept sets will not return the full information, because concepts can have a lot of
        // information.
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME)
                        .body
                        .getItems())
                .contains(conceptSet.concepts(null))
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace2!!.getNamespace(), WORKSPACE_NAME_2)
                        .body
                        .getItems())
                .isEmpty()
    }

    @Test(expected = NotFoundException::class)
    fun testGetConceptSetWrongWorkspace() {
        val conceptSet = makeConceptSet1()
        conceptSetsController!!.getConceptSet(
                workspace2!!.getNamespace(), WORKSPACE_NAME_2, conceptSet.getId())
    }

    @Test
    fun testUpdateConceptSet() {
        val conceptSet = makeConceptSet1()
        conceptSet.setDescription("new description")
        conceptSet.setName("new name")
        val newInstant = NOW.plusMillis(1)
        CLOCK.setInstant(newInstant)
        val updatedConceptSet = conceptSetsController!!
                .updateConceptSet(
                        workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet)
                .body
        assertThat(updatedConceptSet.getCreator()).isEqualTo(USER_EMAIL)
        assertThat(updatedConceptSet.getConcepts()).isNotNull()
        assertThat(updatedConceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli())
        assertThat(updatedConceptSet.getDescription()).isEqualTo("new description")
        assertThat(updatedConceptSet.getDomain()).isEqualTo(Domain.CONDITION)
        assertThat(updatedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(2))
        assertThat(updatedConceptSet.getLastModifiedTime()).isEqualTo(newInstant.toEpochMilli())
        assertThat(updatedConceptSet.getParticipantCount()).isEqualTo(0)
        assertThat(conceptSet.getName()).isEqualTo("new name")

        assertThat(
                conceptSetsController!!
                        .getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                        .body)
                .isEqualTo(updatedConceptSet)
        // Get concept sets will not return the full information, because concepts can have a lot of
        // information.
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace!!.getNamespace(), WORKSPACE_NAME)
                        .body
                        .getItems())
                .contains(updatedConceptSet.concepts(null))
        assertThat(
                conceptSetsController!!
                        .getConceptSetsInWorkspace(workspace2!!.getNamespace(), WORKSPACE_NAME_2)
                        .body
                        .getItems())
                .isEmpty()
    }

    @Test(expected = BadRequestException::class)
    fun testUpdateConceptSetDomainChange() {
        val conceptSet = makeConceptSet1()
        conceptSet.setDescription("new description")
        conceptSet.setName("new name")
        conceptSet.setDomain(Domain.DEATH)
        conceptSetsController!!.updateConceptSet(
                workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet)
    }

    @Test(expected = ConflictException::class)
    fun testUpdateConceptSetWrongEtag() {
        val conceptSet = makeConceptSet1()
        conceptSet.setDescription("new description")
        conceptSet.setName("new name")
        conceptSet.setEtag(Etags.fromVersion(2))
        conceptSetsController!!.updateConceptSet(
                workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet)
    }

    @Test
    fun testUpdateConceptSetConceptsAddAndRemove() {
        saveConcepts()
        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence", ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId())))
                .thenReturn(123)
        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_3.getConceptId())))
                .thenReturn(246)
        val conceptSet = makeConceptSet1()
        val updated = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet.getId(),
                        addConceptsRequest(conceptSet.getEtag(), CLIENT_CONCEPT_3.getConceptId()))
                .body
        assertThat(updated.getConcepts()).contains(CLIENT_CONCEPT_3)
        assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag())
        assertThat(updated.getParticipantCount()).isEqualTo(246)

        val removed = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet.getId(),
                        removeConceptsRequest(updated.getEtag(), CLIENT_CONCEPT_3.getConceptId()))
                .body
        assertThat(removed.getConcepts().size()).isEqualTo(1)
        assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag())
        assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag())
        assertThat(removed.getParticipantCount()).isEqualTo(123)
    }

    @Test
    fun testUpdateConceptSetConceptsAddMany() {
        saveConcepts()
        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(
                        CLIENT_CONCEPT_1.getConceptId(),
                        CLIENT_CONCEPT_3.getConceptId(),
                        CLIENT_CONCEPT_4.getConceptId())))
                .thenReturn(456)
        val conceptSet = makeConceptSet1()
        val updated = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet.getId(),
                        addConceptsRequest(
                                conceptSet.getEtag(),
                                CLIENT_CONCEPT_1.getConceptId(),
                                CLIENT_CONCEPT_3.getConceptId(),
                                CLIENT_CONCEPT_4.getConceptId()))
                .body
        assertThat(updated.getConcepts())
                .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3)
        assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag())
        assertThat(updated.getParticipantCount()).isEqualTo(456)

        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence", ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId())))
                .thenReturn(123)
        val removed = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet.getId(),
                        removeConceptsRequest(
                                updated.getEtag(),
                                CLIENT_CONCEPT_3.getConceptId(),
                                CLIENT_CONCEPT_4.getConceptId()))
                .body
        assertThat(removed.getConcepts()).containsExactly(CLIENT_CONCEPT_1)
        assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag())
        assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag())
        assertThat(removed.getParticipantCount()).isEqualTo(123)
    }

    @Test
    fun testUpdateConceptSetConceptsAddManyOnCreate() {
        saveConcepts()
        `when`(conceptBigQueryService!!.getParticipantCountForConcepts(
                "condition_occurrence",
                ImmutableSet.of(
                        CLIENT_CONCEPT_1.getConceptId(),
                        CLIENT_CONCEPT_3.getConceptId(),
                        CLIENT_CONCEPT_4.getConceptId())))
                .thenReturn(456)
        val conceptSet = makeConceptSet1(
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())
        assertThat(conceptSet.getConcepts())
                .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3)
        assertThat(conceptSet.getParticipantCount()).isEqualTo(456)
    }

    @Test(expected = BadRequestException::class)
    fun testUpdateConceptSetConceptsAddTooMany() {
        saveConcepts()
        val conceptSet = makeConceptSet1()
        conceptSetsController!!.maxConceptsPerSet = 2
        conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet.getId(),
                        addConceptsRequest(
                                conceptSet.getEtag(),
                                CLIENT_CONCEPT_1.getConceptId(),
                                CLIENT_CONCEPT_3.getConceptId(),
                                CLIENT_CONCEPT_4.getConceptId()))
                .body
    }

    @Test(expected = ConflictException::class)
    fun testUpdateConceptSetConceptsWrongEtag() {
        saveConcepts()
        val conceptSet = makeConceptSet1()
        conceptSetsController!!.updateConceptSetConcepts(
                workspace!!.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                addConceptsRequest(Etags.fromVersion(2), CLIENT_CONCEPT_1.getConceptId()))
    }

    @Test(expected = BadRequestException::class)
    fun testUpdateConceptSetConceptsAddWrongDomain() {
        saveConcepts()
        val conceptSet = makeConceptSet1()
        conceptSetsController!!.updateConceptSetConcepts(
                workspace!!.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                addConceptsRequest(
                        conceptSet.getEtag(),
                        CLIENT_CONCEPT_1.getConceptId(),
                        CLIENT_CONCEPT_2.getConceptId()))
    }

    @Test
    fun testDeleteConceptSet() {
        saveConcepts()
        val conceptSet1 = makeConceptSet1()
        var conceptSet2 = makeConceptSet2()
        val updatedConceptSet = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet1.getId(),
                        addConceptsRequest(
                                conceptSet1.getEtag(),
                                CLIENT_CONCEPT_1.getConceptId(),
                                CLIENT_CONCEPT_3.getConceptId(),
                                CLIENT_CONCEPT_4.getConceptId()))
                .body
        val updatedConceptSet2 = conceptSetsController!!
                .updateConceptSetConcepts(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        conceptSet2.getId(),
                        addConceptsRequest(conceptSet2.getEtag(), CLIENT_CONCEPT_2.getConceptId()))
                .body
        assertThat(updatedConceptSet.getConcepts())
                .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_3, CLIENT_CONCEPT_4)
        assertThat(updatedConceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2)

        conceptSetsController!!.deleteConceptSet(
                workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet1.getId())
        try {
            conceptSetsController!!.getConceptSet(
                    workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet1.getId())
            fail("NotFoundException expected")
        } catch (e: NotFoundException) {
            // expected
        }

        conceptSet2 = conceptSetsController!!
                .getConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, conceptSet2.getId())
                .body
        assertThat(conceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2)
    }

    @Test(expected = NotFoundException::class)
    fun testDeleteConceptSetNotFound() {
        conceptSetsController!!.deleteConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, 1L)
    }

    private fun addConceptsRequest(etag: String, vararg conceptIds: Long): UpdateConceptSetRequest {
        val request = UpdateConceptSetRequest()
        request.setEtag(etag)
        request.setAddedIds(ImmutableList.copyOf<E>(conceptIds))
        return request
    }

    private fun removeConceptsRequest(etag: String, vararg conceptIds: Long): UpdateConceptSetRequest {
        val request = UpdateConceptSetRequest()
        request.setEtag(etag)
        request.setRemovedIds(ImmutableList.copyOf<E>(conceptIds))
        return request
    }

    private fun makeSurveyConceptSet1(vararg addedIds: Long): ConceptSet {
        val conceptSet = ConceptSet()
        conceptSet.setDescription("description 1")
        conceptSet.setName("Survey Concept set 1")
        conceptSet.setDomain(Domain.OBSERVATION)
        conceptSet.setSurvey(Surveys.THE_BASICS)
        var request = CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addAddedIdsItem(CLIENT_SURVEY_CONCEPT_1.getConceptId())
        if (addedIds.size > 0) {
            request = request.addedIds(ImmutableList.copyOf<E>(addedIds))
        }
        return conceptSetsController!!
                .createConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, request)
                .body
    }

    private fun makeConceptSet1(vararg addedIds: Long): ConceptSet {
        val conceptSet = ConceptSet()
        conceptSet.setDescription("desc 1")
        conceptSet.setName("concept set 1")
        conceptSet.setDomain(Domain.CONDITION)
        var request = CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addAddedIdsItem(CLIENT_CONCEPT_1.getConceptId())
        if (addedIds.size > 0) {
            request = request.addedIds(ImmutableList.copyOf<E>(addedIds))
        }
        return conceptSetsController!!
                .createConceptSet(workspace!!.getNamespace(), WORKSPACE_NAME, request)
                .body
    }

    private fun makeConceptSet2(): ConceptSet {
        val conceptSet = ConceptSet()
        conceptSet.setDescription("desc 2")
        conceptSet.setName("concept set 2")
        conceptSet.setDomain(Domain.MEASUREMENT)
        return conceptSetsController!!
                .createConceptSet(
                        workspace!!.getNamespace(),
                        WORKSPACE_NAME,
                        CreateConceptSetRequest()
                                .conceptSet(conceptSet)
                                .addAddedIdsItem(CLIENT_CONCEPT_2.getConceptId()))
                .body
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
    }

    private fun stubGetWorkspaceAcl(
            ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val workspaceAccessLevelResponse = WorkspaceACL()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(access.toString())
        val userEmailToAccessEntry = ImmutableMap.of<String, WorkspaceAccessEntry>(creator, accessLevelEntry)
        workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry)
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ns, name)).thenReturn(workspaceAccessLevelResponse)
    }

    private fun saveConcepts() {
        conceptDao!!.save(CONCEPT_1)
        conceptDao!!.save(CONCEPT_2)
        conceptDao!!.save(CONCEPT_3)
        conceptDao!!.save(CONCEPT_4)
    }

    companion object {

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
                .domainId("Measurement")
                .countValue(456L)
                .prevalence(0.3f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_CONCEPT_3 = Concept()
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

        private val CLIENT_CONCEPT_4 = Concept()
                .conceptId(7890L)
                .standardConcept(false)
                .conceptName("conceptD test concept")
                .standardConcept(true)
                .conceptCode("conceptE")
                .conceptClassId("classId5")
                .vocabularyId("V5")
                .domainId("Condition")
                .countValue(7890L)
                .prevalence(0.9f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_SURVEY_CONCEPT_1 = Concept()
                .conceptId(987L)
                .conceptName("a concept")
                .standardConcept(true)
                .conceptCode("conceptA")
                .conceptClassId("classId")
                .vocabularyId("V1")
                .domainId("Observation")
                .countValue(123L)
                .prevalence(0.2f)
                .conceptSynonyms(ArrayList<String>())

        private val CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1)
        private val CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2)
        private val CONCEPT_3 = makeConcept(CLIENT_CONCEPT_3)
        private val CONCEPT_4 = makeConcept(CLIENT_CONCEPT_4)

        private val USER_EMAIL = "bob@gmail.com"
        private val WORKSPACE_NAMESPACE = "ns"
        private val WORKSPACE_NAME = "name"
        private val WORKSPACE_NAME_2 = "name2"
        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private var currentUser: User? = null
    }
}
