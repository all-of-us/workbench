package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.time.Clock
import java.util.ArrayList
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.dao.DomainInfoDao
import org.pmiops.workbench.cdr.dao.DomainVocabularyInfoDao
import org.pmiops.workbench.cdr.dao.SurveyModuleDao
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo.DomainVocabularyInfoId
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.model.Concept
import org.pmiops.workbench.model.ConceptListResponse
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.DomainCount
import org.pmiops.workbench.model.DomainInfo
import org.pmiops.workbench.model.DomainValue
import org.pmiops.workbench.model.EmailVerificationStatus
import org.pmiops.workbench.model.SearchConceptsRequest
import org.pmiops.workbench.model.StandardConceptFilter
import org.pmiops.workbench.model.VocabularyCount
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl
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
@Import(LiquibaseAutoConfiguration::class, BigQueryService::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConceptsControllerTest {

    @Autowired
    private val bigQueryService: BigQueryService? = null
    @Autowired
    private val conceptDao: ConceptDao? = null
    @Autowired
    private val dataSetService: DataSetService? = null
    @Autowired
    private val workspaceService: WorkspaceService? = null
    @Autowired
    private val workspaceDao: WorkspaceDao? = null
    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null
    @Autowired
    private val conceptBigQueryService: ConceptBigQueryService? = null
    @Autowired
    private val domainInfoDao: DomainInfoDao? = null
    @Autowired
    private val domainVocabularyInfoDao: DomainVocabularyInfoDao? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Mock
    internal var userProvider: Provider<User>? = null
    @Autowired
    internal var surveyModuleDao: SurveyModuleDao? = null

    @PersistenceContext
    private val entityManager: EntityManager? = null

    private var conceptsController: ConceptsController? = null

    @TestConfiguration
    @Import(WorkspaceServiceImpl::class)
    @MockBean(BigQueryService::class, FireCloudService::class, CohortCloningService::class, ConceptSetService::class, ConceptBigQueryService::class, Clock::class, DataSetService::class)
    internal class Configuration {
        @Bean
        @Scope("prototype")
        fun user(): User? {
            return currentUser
        }
    }

    @Before
    fun setUp() {
        // Injecting ConceptsController and ConceptService doesn't work well without using
        // SpringBootTest, which causes problems with CdrDbConfig. Just construct the service and
        // controller directly.
        val conceptService = ConceptService(entityManager, conceptDao)
        conceptsController = ConceptsController(
                bigQueryService,
                conceptService,
                conceptBigQueryService,
                workspaceService,
                domainInfoDao,
                domainVocabularyInfoDao,
                conceptDao,
                surveyModuleDao)

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

        val workspace = Workspace()
        workspace.workspaceId = 1L
        workspace.name = "name"
        workspace.firecloudName = "name"
        workspace.workspaceNamespace = "ns"
        workspace.cdrVersion = cdrVersion
        workspaceDao!!.save(workspace)
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name())
        `when`<Any>(fireCloudService!!.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)).thenReturn(fcResponse)
        stubGetWorkspaceAcl(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.WRITER)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQuery() {
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query(" ")))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithResults() {
        saveConcepts()
        saveDomains()
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query(" ")),
                CLIENT_CONCEPT_6,
                CLIENT_CONCEPT_5,
                CLIENT_CONCEPT_4,
                CLIENT_CONCEPT_3,
                CLIENT_CONCEPT_2,
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithVocabAllCounts() {
        saveConcepts()
        saveDomains()
        saveDomainVocabularyInfos()
        val response = conceptsController!!.searchConcepts(
                "ns",
                "name",
                SearchConceptsRequest().includeVocabularyCounts(true).domain(Domain.CONDITION))
        assertResultsWithCounts(
                response, null,
                ImmutableList.of<VocabularyCount>(
                        VocabularyCount().vocabularyId("V1").conceptCount(1L),
                        VocabularyCount().vocabularyId("V3").conceptCount(1L),
                        VocabularyCount().vocabularyId("V5").conceptCount(2L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_3, CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithVocabStandardCounts() {
        saveConcepts()
        saveDomains()
        saveDomainVocabularyInfos()
        val response = conceptsController!!.searchConcepts(
                "ns",
                "name",
                SearchConceptsRequest()
                        .includeVocabularyCounts(true)
                        .domain(Domain.CONDITION)
                        .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS))
        assertResultsWithCounts(
                response, null,
                ImmutableList.of<VocabularyCount>(
                        VocabularyCount().vocabularyId("V1").conceptCount(1L),
                        VocabularyCount().vocabularyId("V5").conceptCount(1L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5, CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithDomainAllCounts() {
        saveConcepts()
        saveDomains()
        // When no query is provided, domain concept counts come from domain info directly.
        val response = conceptsController!!.searchConcepts(
                "ns", "name", SearchConceptsRequest().includeDomainCounts(true))
        assertResultsWithCounts(
                response,
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, false),
                        toDomainCount(DRUG_DOMAIN, false),
                        toDomainCount(MEASUREMENT_DOMAIN, false),
                        toDomainCount(PROCEDURE_DOMAIN, false)), null,
                ImmutableList.of<Concept>(
                        CLIENT_CONCEPT_6,
                        CLIENT_CONCEPT_5,
                        CLIENT_CONCEPT_4,
                        CLIENT_CONCEPT_3,
                        CLIENT_CONCEPT_2,
                        CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithDomainStandardCounts() {
        saveConcepts()
        saveDomains()
        // When no query is provided, domain concept counts come from domain info directly.
        val response = conceptsController!!.searchConcepts(
                "ns",
                "name",
                SearchConceptsRequest()
                        .includeDomainCounts(true)
                        .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS))
        assertResultsWithCounts(
                response,
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, true),
                        toDomainCount(DRUG_DOMAIN, true),
                        toDomainCount(MEASUREMENT_DOMAIN, true),
                        toDomainCount(PROCEDURE_DOMAIN, true)), null,
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5, CLIENT_CONCEPT_4, CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryWithDomainAndVocabStandardCounts() {
        saveConcepts()
        saveDomains()
        saveDomainVocabularyInfos()
        // When no query is provided, domain concept counts come from domain info directly.
        val response = conceptsController!!.searchConcepts(
                "ns",
                "name",
                SearchConceptsRequest()
                        .includeDomainCounts(true)
                        .includeVocabularyCounts(true)
                        .domain(Domain.CONDITION)
                        .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS))
        assertResultsWithCounts(
                response,
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, true),
                        toDomainCount(DRUG_DOMAIN, true),
                        toDomainCount(MEASUREMENT_DOMAIN, true),
                        toDomainCount(PROCEDURE_DOMAIN, true)),
                ImmutableList.of<VocabularyCount>(
                        VocabularyCount().vocabularyId("V1").conceptCount(1L),
                        VocabularyCount().vocabularyId("V5").conceptCount(1L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5, CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryInDomain() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().domain(Domain.CONDITION).query(" ")),
                CLIENT_CONCEPT_6,
                CLIENT_CONCEPT_5,
                CLIENT_CONCEPT_3,
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsBlankQueryInDomainWithVocabularyIds() {
        saveConcepts()
        saveDomainVocabularyInfos()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .domain(Domain.CONDITION)
                                .vocabularyIds(ImmutableList.of<E>("V1", "V5"))
                                .query(" ")),
                CLIENT_CONCEPT_6,
                CLIENT_CONCEPT_5,
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchNoConcepts() {
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("a")))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsNameNoMatches() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("x")))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsNameOneMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("xyz")))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsNameTwoMatches() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("word")),
                CLIENT_CONCEPT_4,
                CLIENT_CONCEPT_3)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsCodeMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptA")),
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsConceptIdMatch() {
        saveConcepts()
        // ID matching currently includes substrings.
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("123")),
                CLIENT_CONCEPT_4,
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsVocabIdMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts("ns", "name", SearchConceptsRequest().query("V456")),
                CLIENT_CONCEPT_4)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsMatchOrder() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptD")),
                CLIENT_CONCEPT_6,
                CLIENT_CONCEPT_5,
                CLIENT_CONCEPT_4)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithDomainAllCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptD").includeDomainCounts(true)),
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, 2),
                        toDomainCount(DRUG_DOMAIN, 0),
                        toDomainCount(MEASUREMENT_DOMAIN, 0),
                        toDomainCount(PROCEDURE_DOMAIN, 0)), null,
                ImmutableList.of<Concept>(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithDomainStandardCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptD")
                                .includeDomainCounts(true)
                                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, 1),
                        toDomainCount(DRUG_DOMAIN, 0),
                        toDomainCount(MEASUREMENT_DOMAIN, 0),
                        toDomainCount(PROCEDURE_DOMAIN, 0)), null,
                // There's no domain filtering so we return CLIENT_CONCEPT_4 here even though it doesn't
                // show up in the counts.
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5, CLIENT_CONCEPT_4))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithVocabularyAllCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptD")
                                .domain(Domain.CONDITION)
                                .includeVocabularyCounts(true)), null,
                ImmutableList.of(VocabularyCount().vocabularyId("V5").conceptCount(2L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithVocabularyStandardCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptD")
                                .domain(Domain.CONDITION)
                                .includeVocabularyCounts(true)
                                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)), null,
                ImmutableList.of(VocabularyCount().vocabularyId("V5").conceptCount(1L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithDomainAndVocabularyStandardCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptD")
                                .domain(Domain.CONDITION)
                                .includeVocabularyCounts(true)
                                .includeDomainCounts(true)
                                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, 1),
                        toDomainCount(DRUG_DOMAIN, 0),
                        toDomainCount(MEASUREMENT_DOMAIN, 0),
                        toDomainCount(PROCEDURE_DOMAIN, 0)),
                ImmutableList.of(VocabularyCount().vocabularyId("V5").conceptCount(1L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_5))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithDomainAndVocabularyAllCounts() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptD")
                                .domain(Domain.CONDITION)
                                .includeVocabularyCounts(true)
                                .includeDomainCounts(true)
                                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, 2),
                        toDomainCount(DRUG_DOMAIN, 0),
                        toDomainCount(MEASUREMENT_DOMAIN, 0),
                        toDomainCount(PROCEDURE_DOMAIN, 0)),
                ImmutableList.of(VocabularyCount().vocabularyId("V5").conceptCount(2L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsWithDomainAndVocabularyAllCountsMatchAllConditions() {
        saveConcepts()
        saveDomains()
        assertResultsWithCounts(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("con")
                                .domain(Domain.CONDITION)
                                .includeVocabularyCounts(true)
                                .includeDomainCounts(true)
                                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
                ImmutableList.of<DomainCount>(
                        toDomainCount(CONDITION_DOMAIN, 4),
                        toDomainCount(DRUG_DOMAIN, 0),
                        // Although it doesn't match the domain filter, we still include the measurement concept
                        // in domain counts
                        toDomainCount(MEASUREMENT_DOMAIN, 1),
                        toDomainCount(PROCEDURE_DOMAIN, 0)),
                ImmutableList.of(
                        VocabularyCount().vocabularyId("V1").conceptCount(1L),
                        VocabularyCount().vocabularyId("V3").conceptCount(1L),
                        VocabularyCount().vocabularyId("V5").conceptCount(2L)),
                ImmutableList.of<Concept>(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_3, CLIENT_CONCEPT_1))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsNonStandard() {
        saveConcepts()
        val response = conceptsController!!.searchConcepts(
                "ns", "name", SearchConceptsRequest().query("conceptB"))
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptB")),
                CLIENT_CONCEPT_2)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsStandardConcept() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptA")
                                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsNotStandardConcept() {
        saveConcepts()
        val response = conceptsController!!.searchConcepts(
                "ns",
                "name",
                SearchConceptsRequest()
                        .query("conceptB")
                        .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS))
        val concept = response.body.getItems().get(0)
        assertThat(concept.getConceptCode()).isEqualTo("conceptB")
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsVocabularyIdNoMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest().query("con").vocabularyIds(ImmutableList.of<E>("x", "v"))))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsVocabularyIdMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("conceptB")
                                .vocabularyIds(ImmutableList.of<E>("V3", "V2"))),
                CLIENT_CONCEPT_2)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsDomainIdNoMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("zzz").domain(Domain.OBSERVATION)))
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsDomainIdMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptA").domain(Domain.CONDITION)),
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsMultipleMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("con")
                                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)
                                .vocabularyIds(ImmutableList.of<E>("V1"))
                                .domain(Domain.CONDITION)),
                CLIENT_CONCEPT_1)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsMultipleNoMatch() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns",
                        "name",
                        SearchConceptsRequest()
                                .query("con")
                                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS)
                                .vocabularyIds(ImmutableList.of<E>("V1"))
                                .domain(Domain.CONDITION)))
    }

    @Throws(Exception::class)
    fun testSearchConceptsOneResult() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("conceptC").maxResults(1)),
                CLIENT_CONCEPT_3)
    }

    @Test
    @Throws(Exception::class)
    fun testSearchConceptsSubstring() {
        saveConcepts()
        assertResults(
                conceptsController!!.searchConcepts(
                        "ns", "name", SearchConceptsRequest().query("est").maxResults(1000)),
                CLIENT_CONCEPT_6,
                CLIENT_CONCEPT_5,
                CLIENT_CONCEPT_4)
    }

    @Test
    @Throws(Exception::class)
    fun testGetDomainInfo() {
        saveConcepts()
        saveDomains()
        val domainInfos = conceptsController!!.getDomainInfo("ns", "name").body.getItems()
        assertThat(domainInfos)
                .containsExactly(
                        DomainInfo()
                                .domain(CONDITION_DOMAIN.domainEnum)
                                .name(CONDITION_DOMAIN.name)
                                .description(CONDITION_DOMAIN.description)
                                .participantCount(CONDITION_DOMAIN.participantCount)
                                .allConceptCount(CONDITION_DOMAIN.allConceptCount)
                                .standardConceptCount(CONDITION_DOMAIN.standardConceptCount),
                        DomainInfo()
                                .domain(DRUG_DOMAIN.domainEnum)
                                .name(DRUG_DOMAIN.name)
                                .description(DRUG_DOMAIN.description)
                                .participantCount(DRUG_DOMAIN.participantCount)
                                .allConceptCount(DRUG_DOMAIN.allConceptCount)
                                .standardConceptCount(DRUG_DOMAIN.standardConceptCount),
                        DomainInfo()
                                .domain(MEASUREMENT_DOMAIN.domainEnum)
                                .name(MEASUREMENT_DOMAIN.name)
                                .description(MEASUREMENT_DOMAIN.description)
                                .participantCount(MEASUREMENT_DOMAIN.participantCount)
                                .allConceptCount(MEASUREMENT_DOMAIN.allConceptCount)
                                .standardConceptCount(MEASUREMENT_DOMAIN.standardConceptCount),
                        DomainInfo()
                                .domain(PROCEDURE_DOMAIN.domainEnum)
                                .name(PROCEDURE_DOMAIN.name)
                                .description(PROCEDURE_DOMAIN.description)
                                .participantCount(PROCEDURE_DOMAIN.participantCount)
                                .allConceptCount(PROCEDURE_DOMAIN.allConceptCount)
                                .standardConceptCount(PROCEDURE_DOMAIN.standardConceptCount))
                .inOrder()
    }

    @Test
    fun testGetValuesFromDomain() {
        `when`(bigQueryService!!.getTableFieldsFromDomain(Domain.CONDITION))
                .thenReturn(
                        FieldList.of(
                                Field.of("FIELD_ONE", LegacySQLTypeName.STRING),
                                Field.of("FIELD_TWO", LegacySQLTypeName.STRING)))
        val domainValues = conceptsController!!
                .getValuesFromDomain("ns", "name", Domain.CONDITION.toString())
                .body
                .getItems()
        verify(bigQueryService).getTableFieldsFromDomain(Domain.CONDITION)

        assertThat(domainValues)
                .containsExactly(
                        DomainValue().value("FIELD_ONE"), DomainValue().value("FIELD_TWO"))
    }

    private fun saveConcepts() {
        conceptDao!!.save(CONCEPT_1)
        conceptDao.save(CONCEPT_2)
        conceptDao.save(CONCEPT_3)
        conceptDao.save(CONCEPT_4)
        conceptDao.save(CONCEPT_5)
        conceptDao.save(CONCEPT_6)
    }

    private fun saveDomains() {
        domainInfoDao!!.save(MEASUREMENT_DOMAIN)
        domainInfoDao.save(PROCEDURE_DOMAIN)
        domainInfoDao.save(CONDITION_DOMAIN)
        domainInfoDao.save(DRUG_DOMAIN)
    }

    private fun saveDomainVocabularyInfos() {
        domainVocabularyInfoDao!!.save(CONDITION_V1_INFO)
        domainVocabularyInfoDao.save(CONDITION_V3_INFO)
        domainVocabularyInfoDao.save(CONDITION_V5_INFO)
    }

    private fun toDomainCount(
            domainInfo: org.pmiops.workbench.cdr.model.DomainInfo, standardCount: Boolean): DomainCount {
        return toDomainCount(
                domainInfo,
                if (standardCount) domainInfo.standardConceptCount else domainInfo.allConceptCount)
    }

    private fun toDomainCount(
            domainInfo: org.pmiops.workbench.cdr.model.DomainInfo, conceptCount: Long): DomainCount {
        return DomainCount()
                .name(domainInfo.name)
                .conceptCount(conceptCount)
                .domain(domainInfo.domainEnum)
    }

    private fun assertResultsWithCounts(
            response: ResponseEntity<ConceptListResponse>,
            domainCounts: ImmutableList<DomainCount>?,
            vocabularyCounts: ImmutableList<VocabularyCount>?,
            concepts: ImmutableList<Concept>) {
        assertThat(response.getBody().getDomainCounts()).isEqualTo(domainCounts)
        assertThat(response.getBody().getVocabularyCounts()).isEqualTo(vocabularyCounts)
        assertThat(response.getBody().getItems()).isEqualTo(concepts)
    }

    private fun assertResults(
            response: ResponseEntity<ConceptListResponse>, vararg expectedConcepts: Concept) {
        assertThat(response.getBody().getItems()).isEqualTo(ImmutableList.copyOf(expectedConcepts))
        assertThat(response.getBody().getDomainCounts()).isNull()
        assertThat(response.getBody().getVocabularyCounts()).isNull()
    }

    private fun stubGetWorkspaceAcl(
            ns: String, name: String, creator: String, access: WorkspaceAccessLevel) {
        val workspaceAccessLevelResponse = WorkspaceACL()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(access.toString())
        val userEmailToAccessEntry = ImmutableMap.of<String, WorkspaceAccessEntry>(creator, accessLevelEntry)
        workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry)
        `when`<Any>(fireCloudService!!.getWorkspaceAcl(ns, name)).thenReturn(workspaceAccessLevelResponse)
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
                .conceptId(1234L)
                .conceptName("sample test con to test the multi word search")
                .standardConcept(true)
                .conceptCode("conceptD")
                .conceptClassId("classId4")
                .vocabularyId("V456")
                .domainId("Observation")
                .countValue(1250L)
                .prevalence(0.5f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_CONCEPT_5 = Concept()
                .conceptId(7890L)
                .conceptName("conceptD test concept")
                .standardConcept(true)
                .conceptCode("conceptE")
                .conceptClassId("classId5")
                .vocabularyId("V5")
                .domainId("Condition")
                .countValue(7890L)
                .prevalence(0.9f)
                .conceptSynonyms(ArrayList<String>())

        private val CLIENT_CONCEPT_6 = Concept()
                .conceptId(7891L)
                .conceptName("conceptD test concept 2")
                .standardConcept(false)
                .conceptCode("conceptD")
                .conceptClassId("classId6")
                .vocabularyId("V5")
                .domainId("Condition")
                .countValue(7891L)
                .prevalence(0.1f)
                .conceptSynonyms(ArrayList<String>())

        private val CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1)
        private val CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2)
        private val CONCEPT_3 = makeConcept(CLIENT_CONCEPT_3)
        private val CONCEPT_4 = makeConcept(CLIENT_CONCEPT_4)
        private val CONCEPT_5 = makeConcept(CLIENT_CONCEPT_5)
        private val CONCEPT_6 = makeConcept(CLIENT_CONCEPT_6)

        private val MEASUREMENT_DOMAIN = org.pmiops.workbench.cdr.model.DomainInfo()
                .domainEnum(Domain.MEASUREMENT)
                .domainId("Measurement")
                .name("Measurement!")
                .description("Measurements!!!")
                .conceptId(CONCEPT_1.conceptId)
                .participantCount(123)
                .standardConceptCount(3)
                .allConceptCount(5)

        private val CONDITION_DOMAIN = org.pmiops.workbench.cdr.model.DomainInfo()
                .domainEnum(Domain.CONDITION)
                .domainId("Condition")
                .name("Condition!")
                .description("Conditions!")
                .conceptId(CONCEPT_2.conceptId)
                .participantCount(456)
                .standardConceptCount(4)
                .allConceptCount(6)

        private val PROCEDURE_DOMAIN = org.pmiops.workbench.cdr.model.DomainInfo()
                .domainEnum(Domain.PROCEDURE)
                .domainId("Procedure")
                .name("Procedure!!!")
                .description("Procedures!!!")
                .conceptId(CONCEPT_3.conceptId)
                .participantCount(789)
                .standardConceptCount(1)
                .allConceptCount(2)

        private val DRUG_DOMAIN = org.pmiops.workbench.cdr.model.DomainInfo()
                .domainEnum(Domain.DRUG)
                .domainId("Drug")
                .name("Drug!")
                .description("Drugs!")
                .conceptId(CONCEPT_4.conceptId)
                .participantCount(3)
                .standardConceptCount(3)
                .allConceptCount(4)

        private val CONDITION_V1_INFO = org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
                .id(DomainVocabularyInfoId("Condition", "V1"))
                .standardConceptCount(1)
                .allConceptCount(1)
        private val CONDITION_V3_INFO = org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
                .id(DomainVocabularyInfoId("Condition", "V3"))
                .allConceptCount(1)
        private val CONDITION_V5_INFO = org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
                .id(DomainVocabularyInfoId("Condition", "V5"))
                .allConceptCount(2)
                .standardConceptCount(1)

        private val WORKSPACE_NAMESPACE = "ns"
        private val WORKSPACE_NAME = "name"
        private val USER_EMAIL = "bob@gmail.com"
        private var currentUser: User? = null

        fun makeConcept(concept: Concept): org.pmiops.workbench.cdr.model.Concept {
            val result = org.pmiops.workbench.cdr.model.Concept()
            result.conceptId = concept.getConceptId()
            result.conceptName = concept.getConceptName()
            result.standardConcept = if (concept.getStandardConcept() == null) null else if (concept.getStandardConcept()) "S" else null
            result.conceptCode = concept.getConceptCode()
            result.conceptClassId = concept.getConceptClassId()
            result.vocabularyId = concept.getVocabularyId()
            result.domainId = concept.getDomainId()
            result.countValue = concept.getCountValue()
            result.prevalence = concept.getPrevalence()
            result.synonymsStr = (String.valueOf(concept.getConceptId())
                    + '|'.toString()
                    + Joiner.on("|").join(concept.getConceptSynonyms()))
            return result
        }
    }
}
