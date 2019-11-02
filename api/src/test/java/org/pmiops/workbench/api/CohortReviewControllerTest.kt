package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.TableResult
import com.google.common.collect.ImmutableMap
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.SortedSet
import java.util.TreeSet
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.CreateReviewRequest
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.FilterColumns
import org.pmiops.workbench.model.ModifyCohortStatusRequest
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest
import org.pmiops.workbench.model.PageFilterRequest
import org.pmiops.workbench.model.ParticipantCohortAnnotation
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse
import org.pmiops.workbench.model.ReviewStatus
import org.pmiops.workbench.model.SortOrder
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = ["org.pmiops.workbench.cohortbuilder", "org.pmiops.workbench.cohortreview"])
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CohortReviewControllerTest {
    private var cdrVersion: CdrVersion? = null
    private var cohortReview: CohortReview? = null
    private var cohort: Cohort? = null
    private var cohortWithoutReview: Cohort? = null
    private var participantCohortStatus1: ParticipantCohortStatus? = null
    private var participantCohortStatus2: ParticipantCohortStatus? = null
    private var workspace: Workspace? = null

    private var stringAnnotationDefinition: CohortAnnotationDefinition? = null
    private var enumAnnotationDefinition: CohortAnnotationDefinition? = null
    private var dateAnnotationDefinition: CohortAnnotationDefinition? = null
    private var booleanAnnotationDefinition: CohortAnnotationDefinition? = null
    private var integerAnnotationDefinition: CohortAnnotationDefinition? = null
    private var participantAnnotation: org.pmiops.workbench.db.model.ParticipantCohortAnnotation? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    @Autowired
    private val cohortDao: CohortDao? = null

    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null

    @Autowired
    private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null

    @Autowired
    private val participantCohortAnnotationDao: ParticipantCohortAnnotationDao? = null

    @Autowired
    private val workspaceService: WorkspaceService? = null

    @Autowired
    private val userRecentResourceService: UserRecentResourceService? = null

    @Autowired
    private val cohortReviewController: CohortReviewController? = null

    @Autowired
    private val bigQueryService: BigQueryService? = null

    @Autowired
    private val userDao: UserDao? = null

    @Mock
    private val userProvider: Provider<User>? = null

    private enum class TestDemo private constructor(val name: String, val conceptId: Long) {
        ASIAN("Asian", 8515),
        WHITE("White", 8527),
        MALE("MALE", 8507),
        FEMALE("FEMALE", 8532),
        NOT_HISPANIC("Not Hispanic or Latino", 38003564)
    }

    @TestConfiguration
    @Import(CdrVersionService::class, CohortReviewController::class)
    @MockBean(BigQueryService::class, FireCloudService::class, UserRecentResourceService::class, WorkspaceService::class)
    internal class Configuration {
        @Bean
        fun clock(): Clock {
            return CLOCK
        }
    }

    @Before
    fun setUp() {
        var user = User()
        user.email = "bob@gmail.com"
        user.userId = 123L
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        user = userDao!!.save(user)
        `when`(userProvider!!.get()).thenReturn(user)
        cohortReviewController!!.setUserProvider(userProvider)

        cbCriteriaDao!!.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.RACE.toString())
                        .parentId(1L)
                        .conceptId(TestDemo.ASIAN.conceptId.toString())
                        .name(TestDemo.ASIAN.name))
        cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.GENDER.toString())
                        .parentId(1L)
                        .conceptId(TestDemo.FEMALE.conceptId.toString())
                        .name(TestDemo.FEMALE.name))
        cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.GENDER.toString())
                        .parentId(1L)
                        .conceptId(TestDemo.MALE.conceptId.toString())
                        .name(TestDemo.MALE.name))
        cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.ETHNICITY.toString())
                        .parentId(1L)
                        .conceptId(TestDemo.NOT_HISPANIC.conceptId.toString())
                        .name(TestDemo.NOT_HISPANIC.name))
        cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.RACE.toString())
                        .parentId(1L)
                        .conceptId(TestDemo.WHITE.conceptId.toString())
                        .name(TestDemo.WHITE.name))

        cdrVersion = CdrVersion()
        cdrVersion!!.bigqueryDataset = "dataSetId"
        cdrVersion!!.bigqueryProject = "projectId"
        cdrVersionDao!!.save<CdrVersion>(cdrVersion)
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)

        workspace = Workspace()
        workspace!!.cdrVersion = cdrVersion
        workspace!!.workspaceNamespace = WORKSPACE_NAMESPACE
        workspace!!.name = WORKSPACE_NAME
        workspace!!.firecloudName = WORKSPACE_NAME
        workspace!!.dataAccessLevelEnum = DataAccessLevel.PROTECTED
        workspaceDao!!.save<Workspace>(workspace)

        cohort = Cohort()
        cohort!!.workspaceId = workspace!!.workspaceId
        val criteria = ("{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
                + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
                + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1567486,\"value\":\"C34\"}],"
                + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}")
        cohort!!.criteria = criteria
        cohortDao!!.save<Cohort>(cohort)

        cohortWithoutReview = Cohort()
        cohortWithoutReview!!.workspaceId = workspace!!.workspaceId
        cohortWithoutReview!!.name = "test"
        cohortWithoutReview!!.description = "test desc"
        cohortWithoutReview!!.criteria = criteria
        cohortDao.save<Cohort>(cohortWithoutReview)

        val today = Timestamp(Date().time)
        cohortReview = cohortReviewDao!!.save(
                CohortReview()
                        .cohortId(cohort!!.cohortId)
                        .cdrVersionId(cdrVersion!!.cdrVersionId)
                        .reviewSize(2)
                        .creationTime(today))

        val key1 = ParticipantCohortStatusKey()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .participantId(1L)
        val key2 = ParticipantCohortStatusKey()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .participantId(2L)

        participantCohortStatus1 = ParticipantCohortStatus()
                .statusEnum(CohortStatus.NOT_REVIEWED)
                .participantKey(key1)
                .genderConceptId(TestDemo.MALE.conceptId)
                .gender(TestDemo.MALE.name)
                .raceConceptId(TestDemo.ASIAN.conceptId)
                .race(TestDemo.ASIAN.name)
                .ethnicityConceptId(TestDemo.NOT_HISPANIC.conceptId)
                .ethnicity(TestDemo.NOT_HISPANIC.name)
                .birthDate(java.sql.Date(today.time))
                .deceased(false)
        participantCohortStatus2 = ParticipantCohortStatus()
                .statusEnum(CohortStatus.NOT_REVIEWED)
                .participantKey(key2)
                .genderConceptId(TestDemo.FEMALE.conceptId)
                .gender(TestDemo.FEMALE.name)
                .raceConceptId(TestDemo.WHITE.conceptId)
                .race(TestDemo.WHITE.name)
                .ethnicityConceptId(TestDemo.NOT_HISPANIC.conceptId)
                .ethnicity(TestDemo.NOT_HISPANIC.name)
                .birthDate(java.sql.Date(today.time))
                .deceased(false)

        participantCohortStatusDao!!.save<ParticipantCohortStatus>(participantCohortStatus1)
        participantCohortStatusDao.save<ParticipantCohortStatus>(participantCohortStatus2)

        stringAnnotationDefinition = CohortAnnotationDefinition()
                .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.STRING))
                .columnName("test")
                .cohortId(cohort!!.cohortId)
        cohortAnnotationDefinitionDao!!.save<CohortAnnotationDefinition>(stringAnnotationDefinition)
        enumAnnotationDefinition = CohortAnnotationDefinition()
                .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.ENUM))
                .columnName("test")
                .cohortId(cohort!!.cohortId)
        val enumValues = TreeSet<CohortAnnotationEnumValue>()
        enumValues.add(
                CohortAnnotationEnumValue()
                        .name("test")
                        .cohortAnnotationDefinition(enumAnnotationDefinition))
        cohortAnnotationDefinitionDao.save(enumAnnotationDefinition!!.enumValues(enumValues))
        dateAnnotationDefinition = CohortAnnotationDefinition()
                .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.DATE))
                .columnName("test")
                .cohortId(cohort!!.cohortId)
        cohortAnnotationDefinitionDao.save<CohortAnnotationDefinition>(dateAnnotationDefinition)
        booleanAnnotationDefinition = CohortAnnotationDefinition()
                .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.BOOLEAN))
                .columnName("test")
                .cohortId(cohort!!.cohortId)
        cohortAnnotationDefinitionDao.save<CohortAnnotationDefinition>(booleanAnnotationDefinition)
        integerAnnotationDefinition = CohortAnnotationDefinition()
                .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.INTEGER))
                .columnName("test")
                .cohortId(cohort!!.cohortId)
        cohortAnnotationDefinitionDao.save<CohortAnnotationDefinition>(integerAnnotationDefinition)

        participantAnnotation = org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .participantId(participantCohortStatus1!!.participantKey!!.participantId)
                .annotationValueString("test")
                .cohortAnnotationDefinitionId(
                        stringAnnotationDefinition!!.cohortAnnotationDefinitionId)
        participantCohortAnnotationDao!!.save<org.pmiops.workbench.db.model.ParticipantCohortAnnotation>(participantAnnotation)
    }

    @Test
    @Throws(Exception::class)
    fun createCohortReviewLessThanMinSize() {
        try {
            cohortReviewController!!.createCohortReview(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_NAME,
                    cohort!!.cohortId,
                    cdrVersion!!.cdrVersionId,
                    CreateReviewRequest().size(0))
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000")
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortReviewMoreThanMaxSize() {
        try {
            cohortReviewController!!.createCohortReview(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_NAME,
                    cohort!!.cohortId,
                    cdrVersion!!.cdrVersionId,
                    CreateReviewRequest().size(10001))
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000")
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortReviewAlreadyExists() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        stubBigQueryCohortCalls()

        try {
            cohortReviewController!!.createCohortReview(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_NAME,
                    cohort!!.cohortId,
                    cdrVersion!!.cdrVersionId,
                    CreateReviewRequest().size(1))
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertThat(bre.message)
                    .isEqualTo(
                            "Bad Request: Cohort Review already created for cohortId: "
                                    + cohort!!.cohortId
                                    + ", cdrVersionId: "
                                    + cdrVersion!!.cdrVersionId)
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortReview() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        stubBigQueryCohortCalls()

        val cohortReview = cohortReviewController!!
                .createCohortReview(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortWithoutReview!!.cohortId,
                        cdrVersion!!.cdrVersionId,
                        CreateReviewRequest().size(1))
                .body

        assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED)
        assertThat(cohortReview.getCohortName()).isEqualTo(cohortWithoutReview!!.name)
        assertThat(cohortReview.getDescription()).isEqualTo(cohortWithoutReview!!.description)
        assertThat(cohortReview.getReviewSize()).isEqualTo(1)
        assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1)
        assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
                .isEqualTo(CohortStatus.NOT_REVIEWED)
    }

    @Test
    @Throws(Exception::class)
    fun createCohortReviewNoCohortException() {
        val cohortId: Long = 99
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        stubBigQueryCohortCalls()

        try {
            cohortReviewController!!
                    .createCohortReview(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_NAME,
                            cohortId,
                            cdrVersion!!.cdrVersionId,
                            CreateReviewRequest().size(1))
                    .body
            fail("Should have thrown NotFoundException!")
        } catch (nfe: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: $cohortId", nfe.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortReviewNoMatchingWorkspaceException() {
        val badWorkspaceName = WORKSPACE_NAME + "bad"
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        stubBigQueryCohortCalls()

        try {
            cohortReviewController!!
                    .createCohortReview(
                            WORKSPACE_NAMESPACE,
                            badWorkspaceName,
                            cohortWithoutReview!!.cohortId,
                            cdrVersion!!.cdrVersionId,
                            CreateReviewRequest().size(1))
                    .body
            fail("Should have thrown NotFoundException!")
        } catch (nfe: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + WORKSPACE_NAMESPACE
                            + ", workspaceId: "
                            + badWorkspaceName,
                    nfe.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateCohortReview() {
        `when`<Any>(workspaceService!!.enforceWorkspaceAccessLevel(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(WorkspaceAccessLevel.WRITER)

        val requestCohortReview = org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .etag(Etags.fromVersion(cohortReview!!.version))
        requestCohortReview.setCohortName("blahblah")
        requestCohortReview.setDescription("new desc")
        val responseCohortReview = cohortReviewController!!
                .updateCohortReview(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        requestCohortReview.getCohortReviewId(),
                        requestCohortReview)
                .body

        assertThat(responseCohortReview.getCohortName()).isEqualTo(requestCohortReview.getCohortName())
        assertThat(responseCohortReview.getDescription())
                .isEqualTo(requestCohortReview.getDescription())
        assertThat(responseCohortReview.getLastModifiedTime()).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun deleteCohortReview() {
        `when`<Any>(workspaceService!!.enforceWorkspaceAccessLevel(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(WorkspaceAccessLevel.WRITER)

        val requestCohortReview = org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .etag(Etags.fromVersion(cohortReview!!.version))
        val emptyResponse = cohortReviewController!!
                .deleteCohortReview(
                        WORKSPACE_NAMESPACE, WORKSPACE_NAME, requestCohortReview.getCohortReviewId())
                .body

        assertThat(emptyResponse).isNotNull()
    }

    @Test
    @Throws(Exception::class)
    fun createParticipantCohortAnnotationNoAnnotationDefinitionFound() {
        val participantId = participantCohortStatus1!!.participantKey!!.participantId

        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        try {
            cohortReviewController!!
                    .createParticipantCohortAnnotation(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_NAME,
                            cohortReview!!.cohortReviewId,
                            participantId,
                            ParticipantCohortAnnotation()
                                    .cohortReviewId(cohortReview!!.cohortReviewId)
                                    .participantId(participantId)
                                    .annotationValueString("test")
                                    .cohortAnnotationDefinitionId(9999L))
                    .body
            fail("Should have thrown a NotFoundException!")
        } catch (nfe: NotFoundException) {
            // Success
            assertThat(nfe.message)
                    .isEqualTo("Not Found: No cohort annotation definition found for id: 9999")
        }

    }

    @Test
    @Throws(Exception::class)
    fun createParticipantCohortAnnotationNoAnnotationValue() {
        val participantId = participantCohortStatus1!!.participantKey!!.participantId
        assertBadRequestExceptionForAnnotationType(
                participantId, stringAnnotationDefinition!!.cohortAnnotationDefinitionId, "STRING")
        assertBadRequestExceptionForAnnotationType(
                participantId, enumAnnotationDefinition!!.cohortAnnotationDefinitionId, "ENUM")
        assertBadRequestExceptionForAnnotationType(
                participantId, dateAnnotationDefinition!!.cohortAnnotationDefinitionId, "DATE")
        assertBadRequestExceptionForAnnotationType(
                participantId, booleanAnnotationDefinition!!.cohortAnnotationDefinitionId, "BOOLEAN")
        assertBadRequestExceptionForAnnotationType(
                participantId, integerAnnotationDefinition!!.cohortAnnotationDefinitionId, "INTEGER")
    }

    @Test
    @Throws(Exception::class)
    fun createParticipantCohortAnnotation() {
        val participantId = participantCohortStatus1!!.participantKey!!.participantId
        participantCohortAnnotationDao!!.delete(participantAnnotation)

        assertCreateParticipantCohortAnnotation(
                participantId,
                stringAnnotationDefinition!!.cohortAnnotationDefinitionId,
                ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview!!.cohortReviewId)
                        .participantId(participantId)
                        .annotationValueString("test")
                        .cohortAnnotationDefinitionId(
                                stringAnnotationDefinition!!.cohortAnnotationDefinitionId))
        assertCreateParticipantCohortAnnotation(
                participantId,
                enumAnnotationDefinition!!.cohortAnnotationDefinitionId,
                ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview!!.cohortReviewId)
                        .participantId(participantId)
                        .annotationValueEnum("test")
                        .cohortAnnotationDefinitionId(
                                enumAnnotationDefinition!!.cohortAnnotationDefinitionId))
        assertCreateParticipantCohortAnnotation(
                participantId,
                dateAnnotationDefinition!!.cohortAnnotationDefinitionId,
                ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview!!.cohortReviewId)
                        .participantId(participantId)
                        .annotationValueDate("2018-02-02")
                        .cohortAnnotationDefinitionId(
                                dateAnnotationDefinition!!.cohortAnnotationDefinitionId))
        assertCreateParticipantCohortAnnotation(
                participantId,
                booleanAnnotationDefinition!!.cohortAnnotationDefinitionId,
                ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview!!.cohortReviewId)
                        .participantId(participantId)
                        .annotationValueBoolean(true)
                        .cohortAnnotationDefinitionId(
                                booleanAnnotationDefinition!!.cohortAnnotationDefinitionId))
        assertCreateParticipantCohortAnnotation(
                participantId,
                integerAnnotationDefinition!!.cohortAnnotationDefinitionId,
                ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview!!.cohortReviewId)
                        .participantId(participantId)
                        .annotationValueInteger(1)
                        .cohortAnnotationDefinitionId(
                                integerAnnotationDefinition!!.cohortAnnotationDefinitionId))
    }

    @Test
    @Throws(Exception::class)
    fun deleteParticipantCohortAnnotation() {
        val annotation = org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
                .cohortReviewId(cohortReview!!.cohortReviewId)
                .participantId(participantCohortStatus1!!.participantKey!!.participantId)
                .annotationValueString("test")
                .cohortAnnotationDefinitionId(
                        stringAnnotationDefinition!!.cohortAnnotationDefinitionId)
        participantCohortAnnotationDao!!.save(annotation)

        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        cohortReviewController!!.deleteParticipantCohortAnnotation(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview!!.cohortReviewId,
                participantCohortStatus1!!.participantKey!!.participantId,
                annotation.annotationId)

        assertThat(participantCohortAnnotationDao.findOne(annotation.annotationId))
                .isEqualTo(null)
    }

    @Test
    @Throws(Exception::class)
    fun deleteParticipantCohortAnnotationNoAnnotation() {
        val participantId = participantCohortStatus1!!.participantKey!!.participantId
        val annotationId = 9999L

        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        try {
            cohortReviewController!!.deleteParticipantCohortAnnotation(
                    WORKSPACE_NAMESPACE,
                    WORKSPACE_NAME,
                    cohortReview!!.cohortReviewId,
                    participantId,
                    annotationId)
            fail("Should have thrown a NotFoundException!")
        } catch (nfe: NotFoundException) {
            // Success
            assertThat(nfe.message)
                    .isEqualTo(
                            "Not Found: No participant cohort annotation found for annotationId: "
                                    + annotationId
                                    + ", cohortReviewId: "
                                    + cohortReview!!.cohortReviewId
                                    + ", participantId: "
                                    + participantId)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getParticipantCohortAnnotations() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
                .thenReturn(workspace)

        val response = cohortReviewController!!
                .getParticipantCohortAnnotations(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortReview!!.cohortReviewId,
                        participantCohortStatus1!!.participantKey!!.participantId)
                .body

        assertThat(response.getItems().size()).isEqualTo(1)
        assertThat(response.getItems().get(0).getCohortReviewId())
                .isEqualTo(cohortReview!!.cohortReviewId)
        assertThat(response.getItems().get(0).getParticipantId())
                .isEqualTo(participantCohortStatus1!!.participantKey!!.participantId)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantCohortStatus() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
                .thenReturn(workspace)

        val response = cohortReviewController!!
                .getParticipantCohortStatus(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortReview!!.cohortReviewId,
                        participantCohortStatus1!!.participantKey!!.participantId)
                .body

        assertThat(response.getParticipantId())
                .isEqualTo(participantCohortStatus1!!.participantKey!!.participantId)
        assertThat(response.getStatus())
                .isEqualTo(StorageEnums.cohortStatusFromStorage(participantCohortStatus1!!.status))
        assertThat(response.getEthnicityConceptId())
                .isEqualTo(participantCohortStatus1!!.ethnicityConceptId)
        assertThat(response.getRaceConceptId()).isEqualTo(participantCohortStatus1!!.raceConceptId)
        assertThat(response.getGenderConceptId())
                .isEqualTo(participantCohortStatus1!!.genderConceptId)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantCohortStatuses() {
        val page = 0
        val pageSize = 25
        val expectedReview1 = createCohortReview(
                cohortReview,
                Arrays.asList<ParticipantCohortStatus>(participantCohortStatus1, participantCohortStatus2),
                page,
                pageSize,
                SortOrder.DESC,
                FilterColumns.STATUS)
        val expectedReview2 = createCohortReview(
                cohortReview,
                Arrays.asList<ParticipantCohortStatus>(participantCohortStatus2, participantCohortStatus1),
                page,
                pageSize,
                SortOrder.DESC,
                FilterColumns.PARTICIPANTID)
        val expectedReview3 = createCohortReview(
                cohortReview,
                Arrays.asList<ParticipantCohortStatus>(participantCohortStatus1, participantCohortStatus2),
                page,
                pageSize,
                SortOrder.ASC,
                FilterColumns.STATUS)
        val expectedReview4 = createCohortReview(
                cohortReview,
                Arrays.asList<ParticipantCohortStatus>(participantCohortStatus1, participantCohortStatus2),
                page,
                pageSize,
                SortOrder.ASC,
                FilterColumns.PARTICIPANTID)

        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
                .thenReturn(workspace)

        assertParticipantCohortStatuses(
                expectedReview1, page, pageSize, SortOrder.DESC, FilterColumns.STATUS)
        verify<UserRecentResourceService>(userRecentResourceService)
                .updateCohortEntry(anyLong(), anyLong(), anyLong(), any(Timestamp::class.java))
        assertParticipantCohortStatuses(
                expectedReview2, page, pageSize, SortOrder.DESC, FilterColumns.PARTICIPANTID)
        assertParticipantCohortStatuses(expectedReview3, null, null, null, FilterColumns.STATUS)
        assertParticipantCohortStatuses(expectedReview4, null, null, SortOrder.ASC, null)
        assertParticipantCohortStatuses(expectedReview4, null, pageSize, null, null)
        assertParticipantCohortStatuses(expectedReview4, page, null, null, null)
        assertParticipantCohortStatuses(expectedReview4, null, null, null, null)
    }

    @Test
    @Throws(Exception::class)
    fun updateParticipantCohortAnnotation() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        val participantCohortAnnotation = cohortReviewController!!
                .updateParticipantCohortAnnotation(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortReview!!.cohortReviewId,
                        participantCohortStatus1!!.participantKey!!.participantId,
                        participantAnnotation!!.annotationId,
                        ModifyParticipantCohortAnnotationRequest().annotationValueString("test1"))
                .body

        assertThat(participantCohortAnnotation.getAnnotationValueString()).isEqualTo("test1")
    }

    @Test
    @Throws(Exception::class)
    fun updateParticipantCohortAnnotationNoAnnotationForIdException() {
        val badAnnotationId: Long = 99
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        try {
            cohortReviewController!!
                    .updateParticipantCohortAnnotation(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_NAME,
                            cohortReview!!.cohortReviewId,
                            participantCohortStatus1!!.participantKey!!.participantId,
                            badAnnotationId,
                            ModifyParticipantCohortAnnotationRequest().annotationValueString("test1"))
                    .body
        } catch (nfe: NotFoundException) {
            assertEquals(
                    "Not Found: Participant Cohort Annotation does not exist for annotationId: "
                            + badAnnotationId
                            + ", cohortReviewId: "
                            + cohortReview!!.cohortReviewId
                            + ", participantId: "
                            + participantCohortStatus1!!.participantKey!!.participantId,
                    nfe.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateParticipantCohortStatus() {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        val participantCohortStatus = cohortReviewController!!
                .updateParticipantCohortStatus(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortReview!!.cohortReviewId,
                        participantCohortStatus1!!.participantKey!!.participantId,
                        ModifyCohortStatusRequest().status(CohortStatus.INCLUDED))
                .body

        assertThat(participantCohortStatus.getStatus()).isEqualTo(CohortStatus.INCLUDED)
    }

    /**
     * Helper method to consolidate assertions for all the [AnnotationType]s.
     *
     * @param participantId
     * @param annotationDefinitionId
     * @param request
     */
    private fun assertCreateParticipantCohortAnnotation(
            participantId: Long?, annotationDefinitionId: Long?, request: ParticipantCohortAnnotation) {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        val response = cohortReviewController!!
                .createParticipantCohortAnnotation(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohortReview!!.cohortReviewId,
                        participantId,
                        request)
                .body

        assertThat(response.getAnnotationValueString()).isEqualTo(request.getAnnotationValueString())
        assertThat(response.getAnnotationValueBoolean()).isEqualTo(request.getAnnotationValueBoolean())
        assertThat(response.getAnnotationValueEnum()).isEqualTo(request.getAnnotationValueEnum())
        assertThat(response.getAnnotationValueDate()).isEqualTo(request.getAnnotationValueDate())
        assertThat(response.getAnnotationValueInteger()).isEqualTo(request.getAnnotationValueInteger())
        assertThat(response.getParticipantId()).isEqualTo(participantId)
        assertThat(response.getCohortReviewId()).isEqualTo(cohortReview!!.cohortReviewId)
        assertThat(response.getCohortAnnotationDefinitionId()).isEqualTo(annotationDefinitionId)
    }

    /**
     * Helper method to consolidate assertions for [BadRequestException]s for all [ ]s.
     *
     * @param participantId
     * @param cohortAnnotationDefId
     * @param type
     */
    private fun assertBadRequestExceptionForAnnotationType(
            participantId: Long?, cohortAnnotationDefId: Long?, type: String) {
        `when`(workspaceService!!.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(workspace)

        try {
            cohortReviewController!!
                    .createParticipantCohortAnnotation(
                            WORKSPACE_NAMESPACE,
                            WORKSPACE_NAME,
                            cohortReview!!.cohortReviewId,
                            participantId,
                            ParticipantCohortAnnotation()
                                    .cohortReviewId(cohortReview!!.cohortReviewId)
                                    .participantId(participantId)
                                    .cohortAnnotationDefinitionId(cohortAnnotationDefId))
                    .body
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // Success
            assertThat(bre.message)
                    .isEqualTo(
                            "Bad Request: Please provide a valid "
                                    + type
                                    + " value for annotation defintion id: "
                                    + cohortAnnotationDefId)
        }

    }

    /**
     * Helper method to assert results for [ ][CohortReviewController.getParticipantCohortStatuses].
     *
     * @param expectedReview
     * @param page
     * @param pageSize
     * @param sortOrder
     * @param sortColumn
     */
    private fun assertParticipantCohortStatuses(
            expectedReview: org.pmiops.workbench.model.CohortReview,
            page: Int?,
            pageSize: Int?,
            sortOrder: SortOrder?,
            sortColumn: FilterColumns?) {
        val actualReview = cohortReviewController!!
                .getParticipantCohortStatuses(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        cohort!!.cohortId,
                        cdrVersion!!.cdrVersionId,
                        PageFilterRequest()
                                .sortColumn(sortColumn)
                                .page(page)
                                .pageSize(pageSize)
                                .sortOrder(sortOrder))
                .body

        assertThat(actualReview).isEqualTo(expectedReview)
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
        `when`(bigQueryService.executeQuery(null)).thenReturn(queryResult)
        `when`(bigQueryService.getResultMapper(queryResult)).thenReturn(rm)
        `when`<Iterable<FieldValueList>>(queryResult.iterateAll()).thenReturn(testIterable)
        `when`<Iterable<FieldValueList>>(queryResult.values).thenReturn(testIterable)
        `when`(bigQueryService.getLong(null!!, 0)).thenReturn(0L)
        `when`(bigQueryService.getString(null!!, 1)).thenReturn("1")
        `when`(bigQueryService.getLong(null!!, 2)).thenReturn(0L)
        `when`(bigQueryService.getLong(null!!, 3)).thenReturn(0L)
        `when`(bigQueryService.getLong(null!!, 4)).thenReturn(0L)
        `when`(bigQueryService.getLong(null!!, 5)).thenReturn(0L)
    }

    private fun createCohortReview(
            actualReview: CohortReview?,
            participantCohortStatusList: List<ParticipantCohortStatus>,
            page: Int?,
            pageSize: Int?,
            sortOrder: SortOrder,
            sortColumn: FilterColumns): org.pmiops.workbench.model.CohortReview {
        val newParticipantCohortStatusList = ArrayList<org.pmiops.workbench.model.ParticipantCohortStatus>()
        for (participantCohortStatus in participantCohortStatusList) {
            newParticipantCohortStatusList.add(
                    org.pmiops.workbench.model.ParticipantCohortStatus()
                            .birthDate(participantCohortStatus.birthDate!!.toString())
                            .ethnicityConceptId(participantCohortStatus.ethnicityConceptId)
                            .ethnicity(participantCohortStatus.ethnicity)
                            .genderConceptId(participantCohortStatus.genderConceptId)
                            .gender(participantCohortStatus.gender)
                            .participantId(participantCohortStatus.participantKey!!.participantId)
                            .raceConceptId(participantCohortStatus.raceConceptId)
                            .race(participantCohortStatus.race)
                            .status(participantCohortStatus.statusEnum)
                            .deceased(participantCohortStatus.deceased))
        }
        return org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(actualReview!!.cohortReviewId)
                .cohortId(actualReview.cohortId)
                .cdrVersionId(actualReview.cdrVersionId)
                .creationTime(actualReview.creationTime!!.toString())
                .matchedParticipantCount(actualReview.matchedParticipantCount)
                .reviewSize(actualReview.reviewSize)
                .reviewedCount(actualReview.reviewedCount)
                .queryResultSize(2L)
                .participantCohortStatuses(newParticipantCohortStatusList)
                .page(page)
                .pageSize(pageSize)
                .sortOrder(sortOrder.toString())
                .sortColumn(sortColumn.name())
    }

    companion object {

        private val WORKSPACE_NAMESPACE = "namespace"
        private val WORKSPACE_NAME = "name"
        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
    }
}
