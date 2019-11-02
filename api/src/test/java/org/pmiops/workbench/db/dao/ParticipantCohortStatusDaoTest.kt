package org.pmiops.workbench.db.dao

import org.junit.Assert.assertEquals
import org.junit.Assert.fail

import java.sql.Date
import java.util.ArrayList
import java.util.Arrays
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.model.Concept
import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.Filter
import org.pmiops.workbench.model.FilterColumns
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.SortOrder
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@Import(TestJpaConfig::class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class ParticipantCohortStatusDaoTest {

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null
    @Autowired
    private val conceptDao: ConceptDao? = null
    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Before
    fun onSetup() {
        val cdrVersion = CdrVersion()
        cdrVersion.cdrDbName = ""
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)

        val status1 = ParticipantCohortStatus()
                .statusEnum(CohortStatus.INCLUDED)
                .participantKey(
                        ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1))
                .genderConceptId(8507L)
                .birthDate(birthDate)
                .raceConceptId(8515L)
                .ethnicityConceptId(38003564L)
                .deceased(false)
        participantCohortStatusDao!!.save(status1)

        val status2 = ParticipantCohortStatus()
                .statusEnum(CohortStatus.EXCLUDED)
                .participantKey(
                        ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2))
                .genderConceptId(8507L)
                .birthDate(birthDate)
                .raceConceptId(8515L)
                .ethnicityConceptId(38003564L)
                .deceased(false)
        participantCohortStatusDao.save(status2)

        val male = Concept()
                .conceptId(8507)
                .conceptName("MALE")
                .domainId("3")
                .vocabularyId("Gender")
                .conceptClassId("1")
                .standardConcept("c")
                .conceptCode("c")
                .count(1)
                .prevalence(1f)
        conceptDao!!.save(male)

        val race = Concept()
                .conceptId(8515)
                .conceptName("Asian")
                .domainId("3")
                .vocabularyId("Race")
                .conceptClassId("1")
                .standardConcept("c")
                .conceptCode("c")
                .count(1)
                .prevalence(1f)
        conceptDao.save(race)

        val ethnicity = Concept()
                .conceptId(38003564)
                .conceptName("Not Hispanic or Latino")
                .domainId("3")
                .vocabularyId("Ethnicity")
                .conceptClassId("1")
                .standardConcept("c")
                .conceptCode("c")
                .count(1)
                .prevalence(1f)
        conceptDao.save(ethnicity)
    }

    @Test
    @Throws(Exception::class)
    fun findByParticipantKeyCohortReviewIdAndParticipantKeyParticipantId() {
        val participant1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        val actualParticipant = participantCohortStatusDao!!
                .findByParticipantKey_CohortReviewIdAndParticipantKey_ParticipantId(
                        COHORT_REVIEW_ID, participant1.participantKey!!.participantId)
        participant1.birthDate = actualParticipant.birthDate
        assertEquals(participant1, actualParticipant)
    }

    @Test
    @Throws(Exception::class)
    fun findAllSaveParticipantCohortStatuses() {
        val key1 = ParticipantCohortStatusKey().cohortReviewId(2).participantId(3)
        val key2 = ParticipantCohortStatusKey().cohortReviewId(2).participantId(4)
        val pcs1 = ParticipantCohortStatus()
                .participantKey(key1)
                .statusEnum(CohortStatus.INCLUDED)
                .birthDate(Date(System.currentTimeMillis()))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L)
                .deceased(false)
        val pcs2 = ParticipantCohortStatus()
                .participantKey(key2)
                .statusEnum(CohortStatus.EXCLUDED)
                .birthDate(Date(System.currentTimeMillis()))
                .ethnicityConceptId(1L)
                .genderConceptId(1L)
                .raceConceptId(1L)
                .deceased(false)

        participantCohortStatusDao!!.saveParticipantCohortStatusesCustom(Arrays.asList(pcs1, pcs2))

        val sql = "select count(*) from participant_cohort_status where cohort_review_id = ?"
        val sqlParams = arrayOf<Any>(key1.cohortReviewId)
        val expectedCount = Int("2")

        assertEquals(expectedCount, jdbcTemplate!!.queryForObject(sql, sqlParams, Int::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun findAllNoMatchingConcept() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val results = participantCohortStatusDao!!.findAll(COHORT_REVIEW_ID, pageRequest)

        assertEquals(2, results.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun findAllNoSearchCriteria() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        val participant1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        participant1.birthDate = results[0].birthDate
        val participant2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.EXCLUDED)
        participant2.birthDate = results[1].birthDate

        assertEquals(participant1, results[0])
        assertEquals(participant2, results[1])
    }

    @Test
    @Throws(Exception::class)
    fun findAllSearchCriteriaEqual() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val filters = ArrayList<Filter>()
        filters.add(
                Filter()
                        .property(FilterColumns.PARTICIPANTID)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("1")))
        filters.add(
                Filter()
                        .property(FilterColumns.STATUS)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList(CohortStatus.INCLUDED.toString())))
        filters.add(
                Filter()
                        .property(FilterColumns.BIRTHDATE)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>(Date(System.currentTimeMillis()).toString())))
        filters.add(
                Filter()
                        .property(FilterColumns.GENDER)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("8507")))
        filters.add(
                Filter()
                        .property(FilterColumns.RACE)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("8515")))
        filters.add(
                Filter()
                        .property(FilterColumns.ETHNICITY)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("38003564")))
        pageRequest.filters(filters)
        val results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(1, results.size.toLong())

        val expectedPCS = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS.birthDate = results[0].birthDate

        assertEquals(expectedPCS, results[0])
    }

    @Test
    @Throws(Exception::class)
    fun findAllSearchCriteriaIn() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val filters = ArrayList<Filter>()
        filters.add(
                Filter()
                        .property(FilterColumns.PARTICIPANTID)
                        .operator(Operator.IN)
                        .values(Arrays.asList<T>("1", "2")))
        filters.add(
                Filter()
                        .property(FilterColumns.STATUS)
                        .operator(Operator.IN)
                        .values(
                                Arrays.asList(CohortStatus.INCLUDED.toString(), CohortStatus.EXCLUDED.toString())))
        filters.add(
                Filter()
                        .property(FilterColumns.BIRTHDATE)
                        .operator(Operator.IN)
                        .values(Arrays.asList<T>(Date(System.currentTimeMillis()).toString())))
        filters.add(
                Filter()
                        .property(FilterColumns.GENDER)
                        .operator(Operator.IN)
                        .values(Arrays.asList<T>("8507", "8532")))
        filters.add(
                Filter()
                        .property(FilterColumns.RACE)
                        .operator(Operator.IN)
                        .values(Arrays.asList<T>("8515", "8527")))
        filters.add(
                Filter()
                        .property(FilterColumns.ETHNICITY)
                        .operator(Operator.IN)
                        .values(Arrays.asList<T>("38003564", "38003563")))
        pageRequest.filters(filters)
        val results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        val expectedPCS1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS1.birthDate = results[0].birthDate
        val expectedPCS2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS2.birthDate = results[0].birthDate

        assertEquals(expectedPCS1, results[0])
        assertEquals(expectedPCS2, results[1])
    }

    @Test
    @Throws(Exception::class)
    fun findAllPaging() {
        var pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(1)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        var results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(1, results.size.toLong())

        var expectedPCS = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS.birthDate = results[0].birthDate

        assertEquals(expectedPCS, results[0])

        pageRequest = PageRequest()
                .page(1)
                .pageSize(1)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        results = participantCohortStatusDao.findAll(1L, pageRequest)

        assertEquals(1, results.size.toLong())

        expectedPCS = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS.birthDate = results[0].birthDate

        assertEquals(expectedPCS, results[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCount() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(1)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val results = participantCohortStatusDao!!.findCount(1L, pageRequest)

        assertEquals(2L, results!!.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun findAllParticipantIdSorting() {
        var pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(2)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        var results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        var expectedPCS1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS1.birthDate = results[0].birthDate
        var expectedPCS2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS2.birthDate = results[1].birthDate

        assertEquals(expectedPCS1, results[0])
        assertEquals(expectedPCS2, results[1])

        pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(2)
                .sortOrder(SortOrder.DESC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        results = participantCohortStatusDao.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        expectedPCS1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS1.birthDate = results[0].birthDate
        expectedPCS2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS2.birthDate = results[1].birthDate

        assertEquals(expectedPCS1, results[0])
        assertEquals(expectedPCS2, results[1])
    }

    @Test
    @Throws(Exception::class)
    fun findAllStatusSorting() {
        var pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(2)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.STATUS.toString())
        var results = participantCohortStatusDao!!.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        var expectedPCS1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS1.birthDate = results[0].birthDate
        var expectedPCS2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS2.birthDate = results[1].birthDate

        assertEquals(expectedPCS1, results[0])
        assertEquals(expectedPCS2, results[1])

        pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(2)
                .sortOrder(SortOrder.DESC)
                .sortColumn(FilterColumns.STATUS.toString())
        results = participantCohortStatusDao.findAll(1L, pageRequest)

        assertEquals(2, results.size.toLong())

        expectedPCS1 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(1),
                CohortStatus.INCLUDED)
        expectedPCS1.birthDate = results[0].birthDate
        expectedPCS2 = createExpectedPCS(
                ParticipantCohortStatusKey().cohortReviewId(COHORT_REVIEW_ID).participantId(2),
                CohortStatus.EXCLUDED)
        expectedPCS2.birthDate = results[1].birthDate

        assertEquals(expectedPCS1, results[0])
        assertEquals(expectedPCS2, results[1])
    }

    @Test
    @Throws(Exception::class)
    fun findAllBadFilterTypes() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val filters = ArrayList<Filter>()

        filters.add(
                Filter()
                        .property(FilterColumns.PARTICIPANTID)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("z")))
        pageRequest.filters(filters)
        assertBadRequest(
                pageRequest, "Bad Request: Problems parsing PARTICIPANTID: For input string: \"z\"")

        filters.clear()
        filters.add(
                Filter()
                        .property(FilterColumns.STATUS)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("z")))
        pageRequest.filters(filters)
        assertBadRequest(
                pageRequest,
                "Bad Request: Problems parsing STATUS: No enum constant org.pmiops.workbench.model.CohortStatus.z")

        filters.clear()
        filters.add(
                Filter()
                        .property(FilterColumns.BIRTHDATE)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("z")))
        pageRequest.filters(filters)
        assertBadRequest(
                pageRequest, "Bad Request: Problems parsing BIRTHDATE: Unparseable date: \"z\"")
    }

    @Test
    @Throws(Exception::class)
    fun findAllBadFilterValuesSize() {
        val pageRequest = PageRequest()
                .page(PAGE)
                .pageSize(PAGE_SIZE)
                .sortOrder(SortOrder.ASC)
                .sortColumn(FilterColumns.PARTICIPANTID.toString())
        val filters = ArrayList<Filter>()

        filters.add(
                Filter()
                        .property(FilterColumns.PARTICIPANTID)
                        .operator(Operator.EQUAL)
                        .values(Arrays.asList<T>("1", "2")))
        pageRequest.filters(filters)
        assertBadRequest(
                pageRequest,
                "Bad Request: property PARTICIPANTID using operator EQUAL must have a single value.")

        filters.clear()
        filters.add(
                Filter()
                        .property(FilterColumns.STATUS)
                        .operator(Operator.EQUAL)
                        .values(ArrayList<E>()))
        pageRequest.filters(filters)
        assertBadRequest(pageRequest, "Bad Request: property STATUS is empty.")
    }

    private fun assertBadRequest(pageRequest: PageRequest, expectedException: String) {
        try {
            participantCohortStatusDao!!.findAll(1L, pageRequest)
            fail("Should have thrown BadRequestException!")
        } catch (e: BadRequestException) {
            assertEquals(expectedException, e.message)
        }

    }

    private fun createExpectedPCS(
            key: ParticipantCohortStatusKey, status: CohortStatus): ParticipantCohortStatus {
        return ParticipantCohortStatus()
                .participantKey(key)
                .statusEnum(status)
                .ethnicityConceptId(38003564L)
                .genderConceptId(8507L)
                .raceConceptId(8515L)
    }

    companion object {
        private val COHORT_REVIEW_ID = 1L
        private val birthDate = Date(System.currentTimeMillis())
        private val PAGE = 0
        private val PAGE_SIZE = 25
    }
}
