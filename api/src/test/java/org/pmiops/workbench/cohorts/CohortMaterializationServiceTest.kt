package org.pmiops.workbench.cohorts

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail

import com.google.cloud.bigquery.BigQuery
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.gson.Gson
import java.util.HashMap
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.CdrConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.AnnotationQuery
import org.pmiops.workbench.model.CdrQuery
import org.pmiops.workbench.model.CohortAnnotationsRequest
import org.pmiops.workbench.model.CohortAnnotationsResponse
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.DataTableSpecification
import org.pmiops.workbench.model.FieldSet
import org.pmiops.workbench.model.MaterializeCohortRequest
import org.pmiops.workbench.model.MaterializeCohortResponse
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.test.SearchRequests
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig
import org.pmiops.workbench.testconfig.CdrJpaConfig
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(LiquibaseAutoConfiguration::class, FieldSetQueryBuilder::class, AnnotationQueryBuilder::class, TestBigQueryCdrSchemaConfig::class, CohortQueryBuilder::class, CdrBigQuerySchemaConfigService::class, BigQueryService::class, SearchGroupItemQueryBuilder::class, CohortMaterializationService::class, ConceptService::class, TestJpaConfig::class, CdrJpaConfig::class)
@MockBean(BigQuery::class)
class CohortMaterializationServiceTest {

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    @Autowired
    private val cohortDao: CohortDao? = null

    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null

    @Autowired
    internal var cohortMaterializationService: CohortMaterializationService? = null

    private var cohortReview: CohortReview? = null

    @TestConfiguration
    internal class Configuration {

        @Bean
        fun workbenchConfig(): WorkbenchConfig {
            val workbenchConfig = WorkbenchConfig()
            workbenchConfig.cdr = CdrConfig()
            workbenchConfig.cdr.debugQueries = false
            return workbenchConfig
        }
    }

    @Before
    fun setUp() {
        val cdrVersion = CdrVersion()
        cdrVersion.bigqueryDataset = DATA_SET_ID
        cdrVersion.bigqueryProject = PROJECT_ID
        cdrVersionDao!!.save(cdrVersion)
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)

        val workspace = Workspace()
        workspace.cdrVersion = cdrVersion
        workspace.name = "name"
        workspace.dataAccessLevelEnum = DataAccessLevel.PROTECTED
        workspaceDao!!.save(workspace)

        val cohort = Cohort()
        cohort.workspaceId = workspace.workspaceId
        cohort.name = "males"
        cohort.type = "AOU"
        val gson = Gson()
        cohort.criteria = gson.toJson(SearchRequests.males())
        cohortDao!!.save(cohort)

        val cohort2 = Cohort()
        cohort2.workspaceId = workspace.workspaceId
        cohort2.name = "all genders"
        cohort2.type = "AOU"
        cohort2.criteria = gson.toJson(SearchRequests.allGenders())
        cohortDao.save(cohort2)

        cohortReview = CohortReview()
        cohortReview!!.cdrVersionId = cdrVersion.cdrVersionId
        cohortReview!!.cohortId = cohort2.cohortId
        cohortReview!!.matchedParticipantCount = 3
        cohortReview!!.reviewedCount = 2
        cohortReview!!.reviewSize = 3
        cohortReviewDao!!.save<CohortReview>(cohortReview)

        participantCohortStatusDao!!.save(
                makeStatus(cohortReview!!.cohortReviewId, 1L, CohortStatus.INCLUDED))
        participantCohortStatusDao.save(
                makeStatus(cohortReview!!.cohortReviewId, 2L, CohortStatus.EXCLUDED))
    }

    @Test
    fun testGetCdrQueryNoTableQuery() {
        val cdrQuery = cohortMaterializationService!!.getCdrQuery(
                SearchRequests.allGenders(), DataTableSpecification(), cohortReview, null)
        assertThat(cdrQuery.getBigqueryDataset()).isEqualTo(DATA_SET_ID)
        assertThat(cdrQuery.getBigqueryProject()).isEqualTo(PROJECT_ID)
        assertThat(cdrQuery.getColumns()).isEqualTo(ImmutableList.of<E>("person_id"))
        assertThat(cdrQuery.getSql())
                .isEqualTo(
                        "select person.person_id person_id\n"
                                + "from `project_id.data_set_id.person` person\n"
                                + "where\n"
                                + "person.person_id in (select person_id\n"
                                + "from `project_id.data_set_id.person` p\n"
                                + "where\n"
                                + "p.gender_concept_id in unnest(@p0)\n"
                                + ")\n"
                                + "and person.person_id not in unnest(@person_id_blacklist)\n\n"
                                + "order by person.person_id\n")
        val params = getParameters(cdrQuery)
        val genderParam = params["p0"]
        val personIdBlacklistParam = params["person_id_blacklist"]
        assertParameterArray(genderParam, 8507, 8532, 2)
        assertParameterArray(personIdBlacklistParam, 2L)
    }

    @Test
    fun testGetCdrQueryWithTableQuery() {
        val cdrQuery = cohortMaterializationService!!.getCdrQuery(
                SearchRequests.allGenders(),
                DataTableSpecification()
                        .tableQuery(
                                TableQuery()
                                        .tableName("measurement")
                                        .columns(
                                                ImmutableList.of<E>("person_id", "measurement_concept.concept_name"))),
                cohortReview, null)
        assertThat(cdrQuery.getBigqueryDataset()).isEqualTo(DATA_SET_ID)
        assertThat(cdrQuery.getBigqueryProject()).isEqualTo(PROJECT_ID)
        assertThat(cdrQuery.getColumns())
                .isEqualTo(ImmutableList.of<E>("person_id", "measurement_concept.concept_name"))
        assertThat(cdrQuery.getSql())
                .isEqualTo(
                        "select inner_results.person_id, "
                                + "measurement_concept.concept_name measurement_concept__concept_name\n"
                                + "from (select measurement.person_id person_id, "
                                + "measurement.measurement_concept_id measurement_measurement_concept_id, "
                                + "measurement.person_id measurement_person_id, "
                                + "measurement.measurement_id measurement_measurement_id\n"
                                + "from `project_id.data_set_id.measurement` measurement\n"
                                + "where\n"
                                + "measurement.person_id in (select person_id\n"
                                + "from `project_id.data_set_id.person` p\n"
                                + "where\n"
                                + "p.gender_concept_id in unnest(@p0)\n"
                                + ")\n"
                                + "and measurement.person_id not in unnest(@person_id_blacklist)\n"
                                + "\n"
                                + "order by measurement.person_id, measurement.measurement_id\n"
                                + ") inner_results\n"
                                + "LEFT OUTER JOIN `project_id.data_set_id.concept` measurement_concept ON "
                                + "inner_results.measurement_measurement_concept_id = measurement_concept.concept_id\n"
                                + "order by measurement_person_id, measurement_measurement_id")
        val params = getParameters(cdrQuery)
        val genderParam = params["p0"]
        val personIdBlacklistParam = params["person_id_blacklist"]
        assertParameterArray(genderParam, 8507, 8532, 2)
        assertParameterArray(personIdBlacklistParam, 2L)
    }

    private fun getParameters(cdrQuery: CdrQuery): Map<String, Map<String, Any>> {
        val configuration = cdrQuery.getConfiguration() as Map<String, Any>
        val queryParameters = (configuration["query"] as Map<String, Any>)["queryParameters"] as Array<Any>
        val result = HashMap<String, Map<String, Any>>()
        for (obj in queryParameters) {
            val param = obj as Map<String, Any>
            result[param["name"] as String] = param
        }
        return result
    }

    private fun assertParameterArray(param: Map<String, Any>, vararg values: Long) {
        val parameterTypeMap = param["parameterType"] as Map<String, Any>
        assertThat(parameterTypeMap["type"]).isEqualTo("ARRAY")
        assertThat((parameterTypeMap["arrayType"] as Map<String, Any>)["type"])
                .isEqualTo("INT64")

        val paramValues = (param["parameterValue"] as Map<String, Any>)["arrayValues"] as Array<Any>
        assertThat(paramValues.size).isEqualTo(values.size)
        for (i in values.indices) {
            assertThat((paramValues[i] as Map<String, Any>)["value"])
                    .isEqualTo(values[i].toString())
        }
    }

    @Test
    fun testMaterializeAnnotationQueryNoPagination() {
        val fieldSet = FieldSet().annotationQuery(AnnotationQuery())
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED")
        assertResults(response, p1Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeAnnotationQueryWithPagination() {
        val fieldSet = FieldSet().annotationQuery(AnnotationQuery())
        val request = makeRequest(fieldSet, 1)
                .statusFilter(ImmutableList.of<E>(CohortStatus.INCLUDED, CohortStatus.EXCLUDED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        val p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED")
        assertResults(response, p1Map)
        assertThat(response.getNextPageToken()).isNotNull()

        request.setPageToken(response.getNextPageToken())
        val response2 = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        val p2Map = ImmutableMap.of("person_id", 2L, "review_status", "EXCLUDED")
        assertResults(response2, p2Map)
        assertThat(response2.getNextPageToken()).isNull()
    }

    @Test
    fun testGetAnnotations() {
        val response = cohortMaterializationService!!.getAnnotations(
                cohortReview, CohortAnnotationsRequest().annotationQuery(AnnotationQuery()))
        val p1Map = ImmutableMap.of("person_id", 1L, "review_status", "INCLUDED")
        assertResults(response.getResults(), p1Map)
    }

    private fun makeRequest(pageSize: Int): MaterializeCohortRequest {
        return MaterializeCohortRequest().pageSize(pageSize)
    }

    private fun makeRequest(fieldSet: FieldSet, pageSize: Int): MaterializeCohortRequest {
        return makeRequest(pageSize).fieldSet(fieldSet)
    }

    private fun makeStatus(
            cohortReviewId: Long, participantId: Long, status: CohortStatus): ParticipantCohortStatus {
        val key = ParticipantCohortStatusKey()
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
        return ParticipantCohortStatus().statusEnum(status).participantKey(key)
    }

    private fun assertResults(
            actualResponse: MaterializeCohortResponse, vararg expectedResults: ImmutableMap<String, Any>) {
        assertResults(actualResponse.getResults(), expectedResults)
    }

    private fun assertResults(
            actualResults: List<Any>, vararg expectedResults: ImmutableMap<String, Any>) {
        if (actualResults.size != expectedResults.size) {
            fail(
                    "Expected "
                            + expectedResults.size
                            + ", got "
                            + actualResults.size
                            + "; actual results: "
                            + actualResults)
        }
        for (i in actualResults.indices) {
            val difference = Maps.difference(actualResults[i] as Map<String, Any>, expectedResults[i])
            if (!difference.areEqual()) {
                fail(
                        "Result "
                                + i
                                + " had difference: "
                                + difference.entriesDiffering()
                                + "; unexpected entries: "
                                + difference.entriesOnlyOnLeft()
                                + "; missing entries: "
                                + difference.entriesOnlyOnRight())
            }
        }
    }

    companion object {

        private val DATA_SET_ID = "data_set_id"
        private val PROJECT_ID = "project_id"
    }
}
