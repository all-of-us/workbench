package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Arrays
import java.util.Date
import javax.inject.Provider
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.cohorts.CohortFactory
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.model.CohortChartData
import org.pmiops.workbench.model.CohortChartDataListResponse
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.CreateReviewRequest
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.PageFilterRequest
import org.pmiops.workbench.model.ParticipantChartData
import org.pmiops.workbench.model.ParticipantChartDataListResponse
import org.pmiops.workbench.model.ParticipantData
import org.pmiops.workbench.model.ParticipantDataListResponse
import org.pmiops.workbench.model.ReviewStatus
import org.pmiops.workbench.model.SortOrder
import org.pmiops.workbench.model.Vocabulary
import org.pmiops.workbench.model.VocabularyListResponse
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.SearchRequests
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.pmiops.workbench.testconfig.TestWorkbenchConfig
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope

@RunWith(BeforeAfterSpringTestRunner::class)
@Import(TestJpaConfig::class)
@ComponentScan(basePackages = ["org.pmiops.workbench.cohortreview.*", "org.pmiops.workbench.cohortbuilder.*"])
class CohortReviewControllerBQTest : BigQueryBaseTest() {
    private var cdrVersion: CdrVersion? = null
    private var workspace: Workspace? = null

    @Autowired
    private val controller: CohortReviewController? = null

    @Autowired
    private val testWorkbenchConfig: TestWorkbenchConfig? = null

    @Autowired
    private val cohortDao: CohortDao? = null

    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null

    @Autowired
    private val dataSetService: DataSetService? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null

    @Autowired
    private val mockFireCloudService: FireCloudService? = null

    @Autowired
    private val userDao: UserDao? = null

    @Mock
    private val userProvider: Provider<User>? = null

    private var cohort: Cohort? = null
    private var review: CohortReview? = null

    override val tableNames: List<String>
        get() = Arrays.asList(
                "cb_review_all_events", "person", "cb_search_person", "cb_search_all_events", "death")

    override val testDataDirectory: String
        get() = BigQueryBaseTest.CB_DATA

    @TestConfiguration
    @Import(WorkspaceServiceImpl::class, CohortReviewServiceImpl::class, CohortReviewController::class, BigQueryTestService::class, ReviewQueryBuilder::class, CohortCloningService::class, CohortQueryBuilder::class, SearchGroupItemQueryBuilder::class)
    @MockBean(FireCloudService::class, UserRecentResourceService::class, CohortFactory::class, ConceptSetService::class, DataSetService::class)
    internal class Configuration {

        @Bean
        @Scope("prototype")
        fun user(): User? {
            return currentUser
        }

        @Bean
        fun clock(): Clock {
            return CLOCK
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        var user = User()
        user.email = "bob@gmail.com"
        user.userId = 123L
        user.disabled = false
        user.emailVerificationStatusEnum = EmailVerificationStatus.SUBSCRIBED
        user = userDao!!.save(user)
        currentUser = user
        `when`(userProvider!!.get()).thenReturn(user)
        controller!!.setUserProvider(userProvider)

        `when`<Any>(mockFireCloudService!!.getWorkspaceAcl(anyString(), anyString()))
                .thenReturn(
                        WorkspaceACL()
                                .acl(
                                        ImmutableMap.of(
                                                currentUser!!.email!!, WorkspaceAccessEntry().accessLevel("OWNER"))))

        cdrVersion = CdrVersion()
        cdrVersion!!.bigqueryDataset = testWorkbenchConfig!!.bigquery!!.dataSetId
        cdrVersion!!.bigqueryProject = testWorkbenchConfig.bigquery!!.projectId
        cdrVersionDao!!.save<CdrVersion>(cdrVersion)

        workspace = Workspace()
        workspace!!.cdrVersion = cdrVersion
        workspace!!.workspaceNamespace = NAMESPACE
        workspace!!.firecloudName = NAME
        workspaceDao!!.save<Workspace>(workspace)
        stubMockFirecloudGetWorkspace()
        stubMockFirecloudGetWorkspaceAcl()

        val gson = Gson()
        cohort = Cohort()
        cohort!!.workspaceId = workspace!!.workspaceId
        cohort!!.criteria = gson.toJson(SearchRequests.males())
        cohortDao!!.save<Cohort>(cohort)

        review = CohortReview()
                .cdrVersionId(cdrVersion!!.cdrVersionId)
                .matchedParticipantCount(212)
                .creationTime(Timestamp(Date().time))
                .lastModifiedTime(Timestamp(Date().time))
                .cohortId(cohort!!.cohortId)
        cohortReviewDao!!.save<CohortReview>(review)

        val key = ParticipantCohortStatusKey()
                .participantId(PARTICIPANT_ID)
                .cohortReviewId(review!!.cohortReviewId)
        val participantCohortStatus = ParticipantCohortStatus().participantKey(key)
        participantCohortStatusDao!!.save(participantCohortStatus)

        val key2 = ParticipantCohortStatusKey()
                .participantId(PARTICIPANT_ID2)
                .cohortReviewId(review!!.cohortReviewId)
        val participantCohortStatus2 = ParticipantCohortStatus().participantKey(key2)
        participantCohortStatusDao.save(participantCohortStatus2)
    }

    @After
    fun tearDown() {
        workspaceDao!!.delete(workspace!!.workspaceId)
        cdrVersionDao!!.delete(cdrVersion!!.cdrVersionId)
    }

    @Test
    @Throws(Exception::class)
    fun getCohortReviewsInWorkspace() {
        val expectedReview = org.pmiops.workbench.model.CohortReview()
                .cohortReviewId(review!!.cohortReviewId)
                .reviewSize(review!!.reviewSize)
                .reviewStatus(review!!.reviewStatusEnum)
                .cdrVersionId(review!!.cdrVersionId)
                .cohortDefinition(review!!.cohortDefinition)
                .cohortName(review!!.cohortName)
                .cohortId(review!!.cohortId)
                .creationTime(review!!.creationTime!!.toString())
                .lastModifiedTime(review!!.lastModifiedTime!!.time)
                .matchedParticipantCount(review!!.matchedParticipantCount)
                .reviewedCount(review!!.reviewedCount)
                .etag(Etags.fromVersion(review!!.version))
        assertEquals(
                expectedReview,
                controller!!.getCohortReviewsInWorkspace(NAMESPACE, NAME).body.getItems().get(0))
    }

    @Test
    @Throws(Exception::class)
    fun createCohortReview() {
        val cohortWithoutReview = Cohort()
        cohortWithoutReview.workspaceId = workspace!!.workspaceId
        val criteria = ("{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
                + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
                + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1,\"value\":\"C34\"}],"
                + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}")
        cohortWithoutReview.criteria = criteria
        cohortDao!!.save(cohortWithoutReview)

        val cohortReview = controller!!
                .createCohortReview(
                        NAMESPACE,
                        NAME,
                        cohortWithoutReview.cohortId,
                        cdrVersion!!.cdrVersionId,
                        CreateReviewRequest().size(1))
                .body

        assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED)
        assertThat(cohortReview.getReviewSize()).isEqualTo(1)
        assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1)
        assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
                .isEqualTo(CohortStatus.NOT_REVIEWED)
        assertThat(cohortReview.getParticipantCohortStatuses().get(0).getDeceased()).isEqualTo(false)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantConditionsSorting() {
        val testFilter = PageFilterRequest().domain(DomainType.CONDITION)

        // no sort order or column
        var response = controller!!
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedCondition1(), expectedCondition2()), 2)

        // added sort order
        testFilter.sortOrder(SortOrder.DESC)
        response = controller
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedCondition2(), expectedCondition1()), 2)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantConditionsPagination() {
        stubMockFirecloudGetWorkspace()

        val testFilter = PageFilterRequest().domain(DomainType.CONDITION).page(0).pageSize(1)

        // page 1 should have 1 item
        var response = controller!!
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedCondition1()), 2)

        // page 2 should have 1 item
        testFilter.page(1)
        response = controller
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID, testFilter)
                .body
        assertResponse(response, Arrays.asList<ParticipantData>(expectedCondition2()), 2)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantAllEventsPagination() {
        val testFilter = PageFilterRequest().domain(DomainType.ALL_EVENTS).page(0).pageSize(1)

        // page 1 should have 1 item
        var response = controller!!
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID2, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedAllEvents1()), 2)

        // page 2 should have 1 item
        testFilter.page(1)
        response = controller
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID2, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedAllEvents2()), 2)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantAllEventsSorting() {
        val testFilter = PageFilterRequest().domain(DomainType.ALL_EVENTS)

        // no sort order or column
        var response = controller!!
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID2, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedAllEvents1(), expectedAllEvents2()), 2)

        // added sort order
        testFilter.sortOrder(SortOrder.DESC)
        response = controller
                .getParticipantData(
                        NAMESPACE, NAME, review!!.cohortReviewId, PARTICIPANT_ID2, testFilter)
                .body

        assertResponse(response, Arrays.asList<ParticipantData>(expectedAllEvents2(), expectedAllEvents1()), 2)
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantChartData() {
        val response = controller!!
                .getParticipantChartData(
                        NAMESPACE,
                        NAME,
                        review!!.cohortReviewId,
                        PARTICIPANT_ID,
                        DomainType.CONDITION.name(),
                        null)
                .body

        val expectedData1 = ParticipantChartData()
                .ageAtEvent(28)
                .rank(1)
                .standardName("SNOMED")
                .standardVocabulary("SNOMED")
                .startDate("2008-07-22")
        val expectedData2 = ParticipantChartData()
                .ageAtEvent(28)
                .rank(1)
                .standardName("SNOMED")
                .standardVocabulary("SNOMED")
                .startDate("2008-08-01")
        assertThat(response.getItems().size()).isEqualTo(2)
        assertThat(expectedData1).isIn(response.getItems())
        assertThat(expectedData2).isIn(response.getItems())
    }

    @Test
    @Throws(Exception::class)
    fun getParticipantChartDataBadLimit() {
        try {
            controller!!.getParticipantChartData(
                    NAMESPACE,
                    NAME,
                    review!!.cohortReviewId,
                    PARTICIPANT_ID,
                    DomainType.CONDITION.name(),
                    -1)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // Success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.")
        }

    }

    @Test
    @Throws(Exception::class)
    fun getParticipantChartDataBadLimitOverHundred() {
        try {
            controller!!.getParticipantChartData(
                    NAMESPACE,
                    NAME,
                    review!!.cohortReviewId,
                    PARTICIPANT_ID,
                    DomainType.CONDITION.name(),
                    101)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // Success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.")
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataBadLimit() {
        try {
            controller!!.getCohortChartData(
                    NAMESPACE, NAME, review!!.cohortReviewId, DomainType.CONDITION.name(), -1)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // Success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.")
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataBadLimitOverHundred() {
        try {
            controller!!.getCohortChartData(
                    NAMESPACE, NAME, review!!.cohortReviewId, DomainType.CONDITION.name(), 101)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // Success
            assertThat(bre.message)
                    .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.")
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataLab() {
        val response = controller!!
                .getCohortChartData(
                        NAMESPACE, NAME, review!!.cohortReviewId, DomainType.LAB.name(), 10)
                .body
        assertEquals(3, response.getItems().size())
        assertEquals(
                CohortChartData().name("name10").conceptId(10L).count(1L), response.getItems().get(0))
        assertEquals(
                CohortChartData().name("name3").conceptId(3L).count(1L), response.getItems().get(1))
        assertEquals(
                CohortChartData().name("name9").conceptId(9L).count(1L), response.getItems().get(2))
    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataDrug() {
        val response = controller!!
                .getCohortChartData(
                        NAMESPACE, NAME, review!!.cohortReviewId, DomainType.DRUG.name(), 10)
                .body
        assertEquals(1, response.getItems().size())
        assertEquals(
                CohortChartData().name("name11").conceptId(1L).count(1L), response.getItems().get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataCondition() {
        val response = controller!!
                .getCohortChartData(
                        NAMESPACE, NAME, review!!.cohortReviewId, DomainType.CONDITION.name(), 10)
                .body
        assertEquals(2, response.getItems().size())
        assertEquals(
                CohortChartData().name("name1").conceptId(1L).count(1L), response.getItems().get(0))
        assertEquals(
                CohortChartData().name("name7").conceptId(7L).count(1L), response.getItems().get(1))
    }

    @Test
    @Throws(Exception::class)
    fun getCohortChartDataProcedure() {
        val response = controller!!
                .getCohortChartData(
                        NAMESPACE, NAME, review!!.cohortReviewId, DomainType.PROCEDURE.name(), 10)
                .body
        assertEquals(3, response.getItems().size())
        assertEquals(
                CohortChartData().name("name2").conceptId(2L).count(1L), response.getItems().get(0))
        assertEquals(
                CohortChartData().name("name4").conceptId(4L).count(1L), response.getItems().get(1))
        assertEquals(
                CohortChartData().name("name8").conceptId(8L).count(1L), response.getItems().get(2))
    }

    @Test
    @Throws(Exception::class)
    fun getVocabularies() {
        val response = controller!!.getVocabularies(NAMESPACE, NAME, review!!.cohortReviewId).body
        assertEquals(20, response.getItems().size())
        assertEquals(
                Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("CPT4"),
                response.getItems().get(0))
        assertEquals(
                Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD10CM"),
                response.getItems().get(1))
        assertEquals(
                Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD9CM"),
                response.getItems().get(2))
    }

    private fun assertResponse(
            response: ParticipantDataListResponse, expectedData: List<ParticipantData>, totalCount: Int) {
        val data = response.getItems()
        assertThat(response.getCount()).isEqualTo(totalCount)
        assertThat(data.size).isEqualTo(expectedData.size)
        var i = 0
        for (actualData in data) {
            val expected = expectedData[i++]
            assertThat(actualData).isEqualTo(expected)
            assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate())
        }
    }

    private fun stubMockFirecloudGetWorkspace() {
        val workspaceResponse = WorkspaceResponse()
        workspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString())
        `when`<Any>(mockFireCloudService!!.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse)
    }

    @Throws(ApiException::class)
    private fun stubMockFirecloudGetWorkspaceAcl() {
        val workspaceAccessLevelResponse = WorkspaceACL()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(WorkspaceAccessLevel.WRITER.toString())
        val userEmailToAccessEntry = ImmutableMap.of<String, WorkspaceAccessEntry>(userProvider!!.get().email!!, accessLevelEntry)
        workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry)
        `when`<Any>(mockFireCloudService!!.getWorkspaceAcl(NAMESPACE, NAME))
                .thenReturn(workspaceAccessLevelResponse)
    }

    companion object {

        private val NAMESPACE = "aou-test"
        private val NAME = "test"
        private val PARTICIPANT_ID = 102246L
        private val PARTICIPANT_ID2 = 102247L
        private val CLOCK = FakeClock(Instant.now(), ZoneId.systemDefault())
        private var currentUser: User? = null

        private fun expectedAllEvents1(): ParticipantData {
            return ParticipantData()
                    .domain("Condition")
                    .standardVocabulary("SNOMED")
                    .standardCode("002")
                    .sourceCode("0020")
                    .sourceVocabulary("ICD9CM")
                    .sourceName("Typhoid and paratyphoid fevers")
                    .route("route")
                    .dose("1.0")
                    .strength("str")
                    .unit("unit")
                    .refRange("range")
                    .numMentions("2")
                    .firstMention("2008-07-22 05:00:00 UTC")
                    .lastMention("2008-07-22 05:00:00 UTC")
                    .visitType("visit")
                    .value("1.0")
                    .itemDate("2008-07-22 05:00:00 UTC")
                    .standardName("SNOMED")
                    .ageAtEvent(28)
                    .standardConceptId(1L)
                    .sourceConceptId(1L)
        }

        private fun expectedAllEvents2(): ParticipantData {
            return ParticipantData()
                    .domain("Condition")
                    .standardVocabulary("SNOMED")
                    .standardCode("002")
                    .sourceCode("0021")
                    .sourceVocabulary("ICD9CM")
                    .sourceName("Typhoid and paratyphoid fevers")
                    .route("route")
                    .dose("1.0")
                    .strength("str")
                    .unit("unit")
                    .refRange("range")
                    .numMentions("2")
                    .firstMention("2008-08-01 05:00:00 UTC")
                    .lastMention("2008-08-01 05:00:00 UTC")
                    .visitType("visit")
                    .value("1.0")
                    .itemDate("2008-08-01 05:00:00 UTC")
                    .standardName("SNOMED")
                    .ageAtEvent(28)
                    .standardConceptId(1L)
                    .sourceConceptId(1L)
        }

        private fun expectedCondition1(): ParticipantData {
            return ParticipantData()
                    .domain(DomainType.CONDITION.toString())
                    .value("1.0")
                    .visitType("visit")
                    .standardVocabulary("SNOMED")
                    .standardCode("002")
                    .sourceCode("0020")
                    .sourceVocabulary("ICD9CM")
                    .sourceName("Typhoid and paratyphoid fevers")
                    .itemDate("2008-07-22 05:00:00 UTC")
                    .standardName("SNOMED")
                    .ageAtEvent(28)
                    .standardConceptId(1L)
                    .sourceConceptId(1L)
                    .numMentions("2")
                    .firstMention("2008-07-22 05:00:00 UTC")
                    .lastMention("2008-07-22 05:00:00 UTC")
                    .unit("unit")
                    .dose("1.0")
                    .strength("str")
                    .route("route")
                    .refRange("range")
        }

        private fun expectedCondition2(): ParticipantData {
            return ParticipantData()
                    .domain(DomainType.CONDITION.toString())
                    .value("1.0")
                    .visitType("visit")
                    .standardVocabulary("SNOMED")
                    .standardCode("002")
                    .sourceCode("0021")
                    .sourceVocabulary("ICD9CM")
                    .sourceName("Typhoid and paratyphoid fevers")
                    .itemDate("2008-08-01 05:00:00 UTC")
                    .standardName("SNOMED")
                    .ageAtEvent(28)
                    .standardConceptId(1L)
                    .sourceConceptId(1L)
                    .numMentions("2")
                    .firstMention("2008-08-01 05:00:00 UTC")
                    .lastMention("2008-08-01 05:00:00 UTC")
                    .unit("unit")
                    .dose("1.0")
                    .strength("str")
                    .route("route")
                    .refRange("range")
        }
    }
}
