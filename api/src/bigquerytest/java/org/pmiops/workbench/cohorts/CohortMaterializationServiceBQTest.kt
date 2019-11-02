package org.pmiops.workbench.cohorts

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.gson.Gson
import java.math.BigDecimal
import java.util.ArrayList
import java.util.Arrays
import java.util.stream.Collectors
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.api.BigQueryBaseTest
import org.pmiops.workbench.api.BigQueryTestService
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.model.Concept
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.config.WorkbenchConfig
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
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.ColumnFilter
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.FieldSet
import org.pmiops.workbench.model.MaterializeCohortRequest
import org.pmiops.workbench.model.MaterializeCohortResponse
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.ResultFilters
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.test.SearchRequests
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.pmiops.workbench.testconfig.TestWorkbenchConfig
import org.pmiops.workbench.utils.PaginationToken
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@RunWith(BeforeAfterSpringTestRunner::class)
@Import(BigQueryTestService::class, CohortQueryBuilder::class, CohortQueryBuilder::class, FieldSetQueryBuilder::class, TestJpaConfig::class, TestBigQueryCdrSchemaConfig::class, AnnotationQueryBuilder::class, CdrBigQuerySchemaConfigService::class, SearchGroupItemQueryBuilder::class)
@ComponentScan(basePackages = ["org.pmiops.workbench.cohortbuilder.*"])
class CohortMaterializationServiceBQTest : BigQueryBaseTest() {

    private var cohortMaterializationService: CohortMaterializationService? = null
    private var cohortReview: CohortReview? = null

    @Autowired
    private val testWorkbenchConfig: TestWorkbenchConfig? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    @Autowired
    private val cohortDao: CohortDao? = null

    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null

    @Autowired
    private val conceptDao: ConceptDao? = null

    @PersistenceContext
    private val entityManager: EntityManager? = null

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null

    @Autowired
    private val fieldSetQueryBuilder: FieldSetQueryBuilder? = null

    @Autowired
    private val annotationQueryBuilder: AnnotationQueryBuilder? = null

    @Autowired
    private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService? = null

    @Mock
    private val configProvider: Provider<WorkbenchConfig>? = null

    override val tableNames: List<String>
        get() = Arrays.asList(
                "person",
                "concept",
                "condition_occurrence",
                "observation",
                "vocabulary",
                "cb_search_all_events")

    override val testDataDirectory: String
        get() = BigQueryBaseTest.MATERIALIZED_DATA

    private fun makeStatus(
            cohortReviewId: Long, participantId: Long, status: CohortStatus): ParticipantCohortStatus {
        val key = ParticipantCohortStatusKey()
        key.cohortReviewId = cohortReviewId
        key.participantId = participantId
        val result = ParticipantCohortStatus()
        result.statusEnum = status
        result.participantKey = key
        return result
    }

    @Before
    fun setUp() {
        val cdrVersion = CdrVersion()
        cdrVersion.bigqueryDataset = testWorkbenchConfig!!.bigquery!!.dataSetId
        cdrVersion.bigqueryProject = testWorkbenchConfig.bigquery!!.projectId
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

        val conceptService = ConceptService(entityManager, conceptDao)

        this.cohortMaterializationService = CohortMaterializationService(
                fieldSetQueryBuilder,
                annotationQueryBuilder,
                participantCohortStatusDao,
                cdrBigQuerySchemaConfigService,
                conceptService,
                configProvider)
    }

    private fun makeRequest(pageSize: Int): MaterializeCohortRequest {
        val request = MaterializeCohortRequest()
        request.setPageSize(pageSize)
        return request
    }

    private fun makeRequest(fieldSet: FieldSet, pageSize: Int): MaterializeCohortRequest {
        val request = makeRequest(pageSize)
        request.setFieldSet(fieldSet)
        return request
    }

    @Test(expected = BadRequestException::class)
    fun testMaterializeCohortBadSpec() {
        cohortMaterializationService!!.materializeCohort(
                null, "badSpec", null, makeRequest(1000))
    }

    @Test
    fun testMaterializeCohortOneMaleSpec() {
        val response = cohortMaterializationService!!.materializeCohort(null, Gson().toJson(SearchRequests.males()), null, makeRequest(1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortOneMale() {
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortICD9Group() {
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.icd9Codes(), null, 0, makeRequest(1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortTemporalGroup() {
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.temporalRequest(), null, 0, makeRequest(1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewNullStatusFilter() {
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, makeRequest(2))
        // With a null status filter, everyone but excluded participants are returned.
        assertPersonIds(response, 1L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewNotExcludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(
                ImmutableList.of<E>(
                        CohortStatus.NOT_REVIEWED, CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        // With a not excluded status filter, ID 2 is not returned.
        assertPersonIds(response, 1L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewJustExcludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.EXCLUDED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewJustIncludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.INCLUDED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewIncludedAndExcludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.EXCLUDED, CohortStatus.INCLUDED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewJustNotReviewedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.NOT_REVIEWED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewNotReviewedAndIncludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 1L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewNotReviewedAndNeedsFurtherReviewFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(
                ImmutableList.of<E>(CohortStatus.NEEDS_FURTHER_REVIEW, CohortStatus.NOT_REVIEWED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewNotReviewedAndExcludedFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(ImmutableList.of<E>(CohortStatus.EXCLUDED, CohortStatus.NOT_REVIEWED))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWithReviewAllFilter() {
        val request = makeRequest(2)
        request.setStatusFilter(
                ImmutableList.of<E>(
                        CohortStatus.EXCLUDED,
                        CohortStatus.NOT_REVIEWED,
                        CohortStatus.INCLUDED,
                        CohortStatus.NEEDS_FURTHER_REVIEW))
        val response = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNotNull()
        request.setPageToken(response.getNextPageToken())
        val response2 = cohortMaterializationService!!.materializeCohort(
                cohortReview, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response2, 102246L)
        assertThat(response2.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPaging() {
        val request = makeRequest(2)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNotNull()
        request.setPageToken(response.getNextPageToken())
        val response2 = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, request)
        assertPersonIds(response2, 102246L)
        assertThat(response2.getNextPageToken()).isNull()

        try {
            // Pagination token doesn't match, this should fail.
            cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 1, request)
            fail("Exception expected")
        } catch (e: BadRequestException) {
            // expected
        }

        val token = PaginationToken.fromBase64(response.getNextPageToken())
        val invalidToken = PaginationToken(-1L, token.parameterHash)
        request.setPageToken(invalidToken.toBase64())
        try {
            // Invalid offset, this should fail.
            cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, request)
            fail("Exception expected")
        } catch (e: BadRequestException) {
            // expected
        }

    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdOnly() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setValueNumber(BigDecimal(1L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNotFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setValueNumber(BigDecimal(1L))
        val resultFilters = makeResultFilters(filter)
        resultFilters.setIfNot(true)
        tableQuery.setFilters(resultFilters)
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberGreaterThanFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.GREATER_THAN)
        filter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberGreaterThanOrEqualToFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO)
        filter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberLessThanFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.LESS_THAN)
        filter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberLessThanOrEqualToFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO)
        filter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumberNotEqualFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.NOT_EQUAL)
        filter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithNumbersFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_id")
        filter.setOperator(Operator.IN)
        filter.setValueNumbers(ImmutableList.of<E>(BigDecimal(1L), BigDecimal(2L)))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_source_value")
        filter.setValue("psv")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringFilterNullNonMatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setValue("esv")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringGreaterThanNullNonMatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setOperator(Operator.GREATER_THAN)
        filter.setValue("esf")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringLessThanNullNonMatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setOperator(Operator.LESS_THAN)
        filter.setValue("esv")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringIsNull() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setOperator(Operator.EQUAL)
        filter.setValueNull(true)
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringNotEqualNullNonMatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setOperator(Operator.NOT_EQUAL)
        filter.setValue("esv")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringIsNotNull() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("ethnicity_source_value")
        filter.setOperator(Operator.NOT_EQUAL)
        filter.setValueNull(true)
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetOrderByGenderConceptId() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id", "gender_concept_id"))
        tableQuery.setOrderBy(ImmutableList.of<E>("gender_concept_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.of<String, Any>("person_id", 1L, "gender_concept_id", 8507L)
        val p2Map = ImmutableMap.of<String, Any>("person_id", 2L, "gender_concept_id", 2L)
        val p3Map = ImmutableMap.of<String, Any>("person_id", 102246L, "gender_concept_id", 8532L)
        assertResults(response, p2Map, p1Map, p3Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetOrderByGenderConceptIdDescending() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id", "gender_concept_id"))
        tableQuery.setOrderBy(ImmutableList.of<E>("descending(gender_concept_id)"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.of<String, Any>("person_id", 1L, "gender_concept_id", 8507L)
        val p2Map = ImmutableMap.of<String, Any>("person_id", 2L, "gender_concept_id", 2L)
        val p3Map = ImmutableMap.of<String, Any>("person_id", 102246L, "gender_concept_id", 8532L)
        assertResults(response, p3Map, p1Map, p2Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringLikeFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_source_value")
        filter.setOperator(Operator.LIKE)
        filter.setValue("p%")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringLikeFilterNoMatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_source_value")
        filter.setOperator(Operator.LIKE)
        filter.setValue("p")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringsFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_source_value")
        filter.setOperator(Operator.IN)
        filter.setValues(ImmutableList.of<E>("foobar", "psv"))
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithStringNonMatchFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter = ColumnFilter()
        filter.setColumnName("person_source_value")
        filter.setValue("foobar")
        tableQuery.setFilters(makeResultFilters(filter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithAllOfFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter1 = ColumnFilter()
        filter1.setColumnName("person_source_value")
        filter1.setValue("psv")
        val filter2 = ColumnFilter()
        filter2.setColumnName("person_id")
        filter2.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeAllOf(filter1, filter2))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetAnyOfFilters() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter1 = ColumnFilter()
        filter1.setColumnName("year_of_birth")
        filter1.setValueNumber(BigDecimal(1980))
        val filter2 = ColumnFilter()
        filter2.setColumnName("person_id")
        filter2.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeAnyOf(filter1, filter2))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetPersonIdWithMultiLevelFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val filter1 = ColumnFilter()
        filter1.setColumnName("year_of_birth")
        filter1.setValueNumber(BigDecimal(1987))
        val filter2 = ColumnFilter()
        filter2.setColumnName("person_id")
        filter2.setValueNumber(BigDecimal(4L))
        val filter3 = ColumnFilter()
        filter3.setColumnName("person_id")
        filter3.setOperator(Operator.LESS_THAN_OR_EQUAL_TO)
        filter3.setValueNumber(BigDecimal(3L))
        val resultFilters = ResultFilters()
        val anyOf = makeAnyOf(filter1, filter2)
        anyOf.setIfNot(true)
        resultFilters.setAllOf(ImmutableList.of<E>(makeResultFilters(filter3), anyOf))
        tableQuery.setFilters(resultFilters)
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonFieldSetAllColumns() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_source_value", "1")
                .put("race_source_value", "1")
                .put("gender_concept_id", 8507L)
                .put("year_of_birth", 1980L)
                .put("month_of_birth", 8L)
                .put("day_of_birth", 1L)
                .put("race_concept_id", 1L)
                .put("ethnicity_concept_id", 1L)
                .put("location_id", 1L)
                .put("provider_id", 1L)
                .put("care_site_id", 1L)
                .put("person_source_value", "psv")
                .put("gender_source_concept_id", 1L)
                .put("race_source_concept_id", 1L)
                .put("ethnicity_source_value", "esv")
                .put("ethnicity_source_concept_id", 1L)
                .build()
        assertResults(response, p1Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFieldSetAllColumns() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("observation_id", 5L)
                .put("person_id", 1L)
                .put("observation_concept_id", 5L)
                .put("observation_date", "2009-12-03")
                .put("observation_datetime", "2009-12-03 05:00:00 UTC")
                .put("observation_type_concept_id", 5L)
                .put("value_as_number", 5.0)
                .put("value_as_string", "5")
                .put("value_as_concept_id", 5L)
                .put("qualifier_concept_id", 5L)
                .put("unit_concept_id", 5L)
                .put("provider_id", 5L)
                .put("visit_occurrence_id", 5L)
                .put("observation_source_value", "5")
                .put("observation_source_concept_id", 5L)
                .put("unit_source_value", "5")
                .put("qualifier_source_value", "5")
                .build()
        assertResults(response, p1Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationPersonNotFound() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.females(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateEqual() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setValueDate("2009-12-03")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateMismatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setValueDate("2009-12-04")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateGreaterThan() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setOperator(Operator.GREATER_THAN)
        columnFilter.setValueDate("2009-12-02")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateGreaterThanOrEqualTo() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO)
        columnFilter.setValueDate("2009-12-03")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateLessThan() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setOperator(Operator.LESS_THAN)
        columnFilter.setValueDate("2009-12-04")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDateLessThanOrEqualTo() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_date")
        columnFilter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO)
        columnFilter.setValueDate("2009-12-03")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeEqual() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setValueDate("2009-12-03 05:00:00 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeMismatch() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setValueDate("2009-12-03 05:00:01 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeGreaterThan() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setOperator(Operator.GREATER_THAN)
        columnFilter.setValueDate("2009-12-03 04:59:59 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeGreaterThanOrEqualTo() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setOperator(Operator.GREATER_THAN_OR_EQUAL_TO)
        columnFilter.setValueDate("2009-12-03 05:00:00 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeLessThan() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setOperator(Operator.LESS_THAN)
        columnFilter.setValueDate("2009-12-03 05:00:01 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortObservationFilterObservationDatetimeLessThanOrEqualTo() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("observation")
        tableQuery.setColumns(ImmutableList.of<E>("observation_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("observation_datetime")
        columnFilter.setOperator(Operator.LESS_THAN_OR_EQUAL_TO)
        columnFilter.setValueDate("2009-12-03 05:00:00 UTC")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.males(), null, 0, makeRequest(fieldSet, 1000))
        assertResults(response, ImmutableMap.of("observation_id", 5L))
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonConceptSelectColumns() {
        // Here person is in the inner query; gender_concept, gender_concept.vocabulary,
        // and gender_concept.vocabulary.vocabulary_concept are only referenced in the select clause
        // and are in the outer query.
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "gender_concept.concept_name",
                        "gender_concept.vocabulary_id",
                        "gender_concept.vocabulary.vocabulary_name",
                        "gender_concept.vocabulary.vocabulary_reference",
                        "gender_concept.vocabulary.vocabulary_concept.concept_name"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("person_id")
        columnFilter.setOperator(Operator.NOT_EQUAL)
        columnFilter.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_concept.concept_name", "MALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        val p2Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 102246L)
                .put("gender_concept.concept_name", "FEMALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        assertResults(response, p1Map, p2Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWhereInner() {
        // person and gender_concept are in the inner query (since gender_concept is in the where
        // clause.)
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "gender_concept.concept_name",
                        "gender_concept.vocabulary_id",
                        "gender_concept.vocabulary.vocabulary_name",
                        "gender_concept.vocabulary.vocabulary_reference",
                        "gender_concept.vocabulary.vocabulary_concept.concept_name"))
        val columnFilter1 = ColumnFilter()
        columnFilter1.setColumnName("person_id")
        columnFilter1.setOperator(Operator.NOT_EQUAL)
        columnFilter1.setValueNumber(BigDecimal(2L))
        val columnFilter2 = ColumnFilter()
        columnFilter2.setColumnName("gender_concept.vocabulary_id")
        columnFilter2.setValue("Gender")
        tableQuery.setFilters(makeAllOf(columnFilter1, columnFilter2))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_concept.concept_name", "MALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        val p2Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 102246L)
                .put("gender_concept.concept_name", "FEMALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        assertResults(response, p1Map, p2Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortWhereNestedInner() {
        // person and gender_concept.vocabulary are in the inner query, and thus so is gender_concept.
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "gender_concept.concept_name",
                        "gender_concept.vocabulary_id",
                        "gender_concept.vocabulary.vocabulary_name",
                        "gender_concept.vocabulary.vocabulary_reference",
                        "gender_concept.vocabulary.vocabulary_concept.concept_name"))
        val columnFilter1 = ColumnFilter()
        columnFilter1.setColumnName("person_id")
        columnFilter1.setOperator(Operator.NOT_EQUAL)
        columnFilter1.setValueNumber(BigDecimal(2L))
        val columnFilter2 = ColumnFilter()
        columnFilter2.setColumnName("gender_concept.vocabulary.vocabulary_name")
        columnFilter2.setValue("Gender vocabulary")
        tableQuery.setFilters(makeAllOf(columnFilter1, columnFilter2))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_concept.concept_name", "MALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        val p2Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 102246L)
                .put("gender_concept.concept_name", "FEMALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        assertResults(response, p1Map, p2Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortOrderByInner() {
        // person and gender_concept are in the inner query (since gender_concept is in order by).
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "gender_concept.concept_name",
                        "gender_concept.vocabulary_id",
                        "gender_concept.vocabulary.vocabulary_name",
                        "gender_concept.vocabulary.vocabulary_reference",
                        "gender_concept.vocabulary.vocabulary_concept.concept_name"))
        val columnFilter1 = ColumnFilter()
        columnFilter1.setColumnName("person_id")
        columnFilter1.setOperator(Operator.NOT_EQUAL)
        columnFilter1.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(columnFilter1))
        tableQuery.setOrderBy(ImmutableList.of<E>("gender_concept.concept_name"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_concept.concept_name", "MALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        val p2Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 102246L)
                .put("gender_concept.concept_name", "FEMALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        assertResults(response, p2Map, p1Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortOrderByNestedInner() {
        // person and gender_concept.vocabulary are in the inner query, and thus so is gender_concept.
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "gender_concept.concept_name",
                        "gender_concept.vocabulary_id",
                        "gender_concept.vocabulary.vocabulary_name",
                        "gender_concept.vocabulary.vocabulary_reference",
                        "gender_concept.vocabulary.vocabulary_concept.concept_name"))
        val columnFilter1 = ColumnFilter()
        columnFilter1.setColumnName("person_id")
        columnFilter1.setOperator(Operator.NOT_EQUAL)
        columnFilter1.setValueNumber(BigDecimal(2L))
        tableQuery.setFilters(makeResultFilters(columnFilter1))
        tableQuery.setOrderBy(
                ImmutableList.of<E>("DESCENDING(gender_concept.vocabulary.vocabulary_name)", "person_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        val p1Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 1L)
                .put("gender_concept.concept_name", "MALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        val p2Map = ImmutableMap.builder<String, Any>()
                .put("person_id", 102246L)
                .put("gender_concept.concept_name", "FEMALE")
                .put("gender_concept.vocabulary_id", "Gender")
                .put("gender_concept.vocabulary.vocabulary_name", "Gender vocabulary")
                .put("gender_concept.vocabulary.vocabulary_reference", "Gender reference")
                .put(
                        "gender_concept.vocabulary.vocabulary_concept.concept_name",
                        "Gender vocabulary concept")
                .build()
        assertResults(response, p1Map, p2Map)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonConceptFilter() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        val columnFilter = ColumnFilter()
        columnFilter.setColumnName("gender_concept.concept_name")
        columnFilter.setValue("FEMALE")
        tableQuery.setFilters(makeResultFilters(columnFilter))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 102246L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortPersonConceptOrderBy() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("person")
        tableQuery.setColumns(ImmutableList.of<E>("person_id"))
        tableQuery.setOrderBy(
                ImmutableList.of<E>("gender_concept.vocabulary_id", "descending(person_id)"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), null, 0, makeRequest(fieldSet, 1000))
        assertPersonIds(response, 102246L, 1L, 2L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test(expected = BadRequestException::class)
    fun testMaterializeCohortConceptSetNoConcepts() {
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(
                ImmutableList.of<E>("person_id", "condition_concept_id", "condition_source_concept_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(123456L),
                0,
                makeRequest(fieldSet, 1000))
    }

    @Test
    fun testMaterializeCohortConceptSetNoMatchingConcepts() {
        val concept = Concept()
        concept.conceptId = 2L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao!!.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(
                ImmutableList.of<E>("person_id", "condition_concept_id", "condition_source_concept_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null, SearchRequests.allGenders(), ImmutableSet.of(2L), 0, makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortConceptSetOneStandardConcept() {
        val concept = Concept()
        concept.conceptId = 192819L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao!!.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(ImmutableList.of<E>("condition_occurrence_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(192819L),
                0,
                makeRequest(fieldSet, 1000))
        assertConditionOccurrenceIds(response, 12751439L, 12751440L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortConceptSetOneStandardConceptMismatch() {
        val concept = Concept()
        concept.conceptId = 44829697L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao!!.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(ImmutableList.of<E>("condition_occurrence_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(44829697L),
                0,
                makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortConceptSetOneSourceConcept() {
        val concept = Concept()
        concept.conceptId = 44829697L
        conceptDao!!.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(ImmutableList.of<E>("condition_occurrence_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(44829697L),
                0,
                makeRequest(fieldSet, 1000))
        assertConditionOccurrenceIds(response, 12751439L, 12751440L)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortConceptSetOneSourceConceptMismatch() {
        val concept = Concept()
        concept.conceptId = 192819L
        conceptDao!!.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(ImmutableList.of<E>("condition_occurrence_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val response = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(192819L),
                0,
                makeRequest(fieldSet, 1000))
        assertResults(response)
        assertThat(response.getNextPageToken()).isNull()
    }

    @Test
    fun testMaterializeCohortConceptSetLotsOfConceptsPaging() {
        var concept = Concept()
        concept.conceptId = 1L
        conceptDao!!.save(concept)
        concept = Concept()
        concept.conceptId = 6L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao.save(concept)
        concept = Concept()
        concept.conceptId = 7L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao.save(concept)
        concept = Concept()
        concept.conceptId = 192819L
        concept.standardConcept = ConceptService.STANDARD_CONCEPT_CODE
        conceptDao.save(concept)
        concept = Concept()
        concept.conceptId = 44829697L
        conceptDao.save(concept)
        val tableQuery = TableQuery()
        tableQuery.setTableName("condition_occurrence")
        tableQuery.setColumns(ImmutableList.of<E>("condition_occurrence_id"))
        tableQuery.setOrderBy(ImmutableList.of<E>("condition_occurrence_id"))
        val fieldSet = FieldSet()
        fieldSet.setTableQuery(tableQuery)
        val request = makeRequest(fieldSet, 4)
        val response = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(1L, 6L, 7L, 192819L, 44829697L, 12345L),
                0,
                request)
        assertConditionOccurrenceIds(response, 1L, 6L, 7L, 12751439L)
        assertThat(response.getNextPageToken()).isNotNull()
        request.setPageToken(response.getNextPageToken())
        val response2 = cohortMaterializationService!!.materializeCohort(null,
                SearchRequests.allGenders(),
                ImmutableSet.of(1L, 6L, 7L, 192819L, 44829697L, 12345L),
                0,
                request)
        assertConditionOccurrenceIds(response2, 12751440L)
        assertThat(response2.getNextPageToken()).isNull()
    }

    private fun makeResultFilters(columnFilter: ColumnFilter): ResultFilters {
        val result = ResultFilters()
        result.setColumnFilter(columnFilter)
        return result
    }

    private fun makeAnyOf(vararg columnFilters: ColumnFilter): ResultFilters {
        val result = ResultFilters()
        result.setAnyOf(
                Arrays.asList<Array<ColumnFilter>>(*columnFilters).stream()
                        .map<Any> { columnFilter -> makeResultFilters(columnFilter) }
                        .collect<R, A>(Collectors.toList<T>()))
        return result
    }

    private fun makeAllOf(vararg columnFilters: ColumnFilter): ResultFilters {
        val result = ResultFilters()
        result.setAllOf(
                Arrays.asList<Array<ColumnFilter>>(*columnFilters).stream()
                        .map<Any> { columnFilter -> makeResultFilters(columnFilter) }
                        .collect<R, A>(Collectors.toList<T>()))
        return result
    }

    private fun assertResults(
            response: MaterializeCohortResponse, vararg results: ImmutableMap<String, Any>) {
        if (response.getResults().size() !== results.size) {
            fail(
                    "Expected "
                            + results.size
                            + ", got "
                            + response.getResults().size()
                            + "; actual results: "
                            + response.getResults())
        }
        for (i in 0 until response.getResults().size()) {
            val difference = Maps.difference(response.getResults().get(i) as Map<String, Any>, results[i])
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

    private fun assertPersonIds(response: MaterializeCohortResponse, vararg personIds: Long) {
        val expectedResults = ArrayList<Any>()
        for (personId in personIds) {
            expectedResults.add(ImmutableMap.of(CohortMaterializationService.PERSON_ID, personId))
        }
        assertThat(response.getResults()).isEqualTo(expectedResults)
    }

    private fun assertConditionOccurrenceIds(
            response: MaterializeCohortResponse, vararg conditionOccurrenceIds: Long) {
        val expectedResults = ArrayList<Any>()
        for (conditionOccurrenceId in conditionOccurrenceIds) {
            expectedResults.add(ImmutableMap.of("condition_occurrence_id", conditionOccurrenceId))
        }
        assertThat(response.getResults()).isEqualTo(expectedResults)
    }
}
