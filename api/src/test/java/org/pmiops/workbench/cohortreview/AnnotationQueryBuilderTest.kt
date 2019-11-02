package org.pmiops.workbench.cohortreview

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import java.sql.Date
import java.text.ParseException
import java.text.SimpleDateFormat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation
import org.pmiops.workbench.db.model.ParticipantCohortStatus
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.AnnotationQuery
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.CohortStatus
import org.pmiops.workbench.model.DataAccessLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
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
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AnnotationQueryBuilderTest {

    @Autowired
    private val annotationQueryBuilder: AnnotationQueryBuilder? = null

    @Autowired
    private val workspaceDao: WorkspaceDao? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val cohortDao: CohortDao? = null

    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null

    @Autowired
    private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null

    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null

    @Autowired
    private val participantCohortAnnotationDao: ParticipantCohortAnnotationDao? = null

    private var cohortReview: CohortReview? = null
    private var integerAnnotation: CohortAnnotationDefinition? = null
    private var stringAnnotation: CohortAnnotationDefinition? = null
    private var booleanAnnotation: CohortAnnotationDefinition? = null
    private var dateAnnotation: CohortAnnotationDefinition? = null
    private var enumAnnotation: CohortAnnotationDefinition? = null
    private var enumValueMap: Map<String, CohortAnnotationEnumValue>? = null
    private var expectedResult1: ImmutableMap<String, Any>? = null
    private var expectedResult2: ImmutableMap<String, Any>? = null
    private var allColumns: List<String>? = null

    @TestConfiguration
    @Import(AnnotationQueryBuilder::class)
    internal class Configuration

    @Before
    fun setUp() {
        val cdrVersion = CdrVersion()
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
        cohort.criteria = "blah"
        cohortDao!!.save(cohort)

        cohortReview = CohortReview()
        cohortReview!!.cdrVersionId = cdrVersion.cdrVersionId
        cohortReview!!.cohortId = cohort.cohortId
        cohortReview!!.matchedParticipantCount = 3
        cohortReview!!.reviewedCount = 2
        cohortReview!!.reviewSize = 3
        cohortReviewDao!!.save<CohortReview>(cohortReview)

        integerAnnotation = cohortAnnotationDefinitionDao!!.save(
                makeAnnotationDefinition(
                        cohort.cohortId, "integer annotation", AnnotationType.INTEGER))
        stringAnnotation = cohortAnnotationDefinitionDao.save(
                makeAnnotationDefinition(
                        cohort.cohortId, "string annotation", AnnotationType.STRING))
        booleanAnnotation = cohortAnnotationDefinitionDao.save(
                makeAnnotationDefinition(
                        cohort.cohortId, "boolean annotation", AnnotationType.BOOLEAN))
        dateAnnotation = cohortAnnotationDefinitionDao.save(
                makeAnnotationDefinition(cohort.cohortId, "date annotation", AnnotationType.DATE))
        enumAnnotation = cohortAnnotationDefinitionDao.save(
                makeAnnotationDefinition(
                        cohort.cohortId, "enum annotation", AnnotationType.ENUM, "zebra", "aardvark"))
        enumValueMap = Maps.uniqueIndex(enumAnnotation!!.enumValues, Function<CohortAnnotationEnumValue, String> { it.getName() })

        expectedResult1 = ImmutableMap.builder<String, Any>()
                .put("person_id", INCLUDED_PERSON_ID)
                .put("review_status", "INCLUDED")
                .put("integer annotation", 123)
                .put("string annotation", "foo")
                .put("boolean annotation", true)
                .put("date annotation", "2017-02-14")
                .put("enum annotation", "zebra")
                .build()
        expectedResult2 = ImmutableMap.builder<String, Any>()
                .put("person_id", EXCLUDED_PERSON_ID)
                .put("review_status", "EXCLUDED")
                .put("integer annotation", 456)
                .put("boolean annotation", false)
                .put("date annotation", "2017-02-15")
                .put("enum annotation", "aardvark")
                .build()
        allColumns = ImmutableList.copyOf(expectedResult1!!.keys)
    }

    private fun makeAnnotationDefinition(
            cohortId: Long, columnName: String, annotationType: AnnotationType, vararg enumValues: String): CohortAnnotationDefinition {
        val cohortAnnotationDefinition = CohortAnnotationDefinition()
        cohortAnnotationDefinition.annotationTypeEnum = annotationType
        cohortAnnotationDefinition.cohortId = cohortId
        cohortAnnotationDefinition.columnName = columnName
        if (enumValues.size > 0) {
            for (i in enumValues.indices) {
                val enumValue = CohortAnnotationEnumValue()
                enumValue.order = i
                enumValue.name = enumValues[i]
                cohortAnnotationDefinition.enumValues.add(enumValue)
            }
        }
        return cohortAnnotationDefinition
    }

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

    @Test
    fun testQueryEmptyReview() {
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, INCLUDED_ONLY, AnnotationQuery(), 10, 0),
                allColumns)
    }

    @Test
    fun testQueryOneIncluded() {
        saveReviewStatuses()
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, INCLUDED_ONLY, AnnotationQuery(), 10, 0),
                allColumns,
                ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"))
    }

    @Test
    fun testQueryAllStatuses() {
        saveReviewStatuses()
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, AnnotationQuery(), 10, 0),
                allColumns,
                ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"),
                ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"))
    }

    @Test
    fun testQueryAllStatusesReviewStatusOrder() {
        saveReviewStatuses()
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("review_status"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
                ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"))
    }

    @Test
    fun testQueryAllStatusesReviewPersonIdOrderDescending() {
        saveReviewStatuses()
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("DESCENDING(person_id)"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                ImmutableMap.of("person_id", EXCLUDED_PERSON_ID, "review_status", "EXCLUDED"),
                ImmutableMap.of("person_id", INCLUDED_PERSON_ID, "review_status", "INCLUDED"))
    }

    @Test
    @Throws(Exception::class)
    fun testQueryIncludedWithAnnotations() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        val expectedResult = ImmutableMap.builder<String, Any>()
                .put("person_id", INCLUDED_PERSON_ID)
                .put("review_status", "INCLUDED")
                .put("integer annotation", 123)
                .put("string annotation", "foo")
                .put("boolean annotation", true)
                .put("date annotation", "2017-02-14")
                .put("enum annotation", "zebra")
                .build()
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, INCLUDED_ONLY, AnnotationQuery(), 10, 0),
                allColumns,
                expectedResult)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryIncludedWithAnnotationsNoReviewStatus() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        val expectedResult = ImmutableMap.builder<String, Any>()
                .put("person_id", INCLUDED_PERSON_ID)
                .put("integer annotation", 123)
                .put("string annotation", "foo")
                .put("boolean annotation", true)
                .put("date annotation", "2017-02-14")
                .put("enum annotation", "zebra")
                .build()
        val annotationQuery = AnnotationQuery()
        annotationQuery.setColumns(
                ImmutableList.of<E>(
                        "person_id",
                        "integer annotation",
                        "string annotation",
                        "boolean annotation",
                        "date annotation",
                        "enum annotation"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, INCLUDED_ONLY, annotationQuery, 10, 0),
                annotationQuery.getColumns(),
                expectedResult)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotations() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")

        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, AnnotationQuery(), 10, 0),
                allColumns,
                expectedResult1,
                expectedResult2)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsLimit1() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")

        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, AnnotationQuery(), 1, 0),
                allColumns,
                expectedResult1!!)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsLimit1Offset1() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")

        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, AnnotationQuery(), 1, 1),
                allColumns,
                expectedResult2!!)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsOrderByIntegerDescending() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("DESCENDING(integer annotation)", "person_id"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                expectedResult2,
                expectedResult1)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsOrderByBoolean() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("boolean annotation", "person_id"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                expectedResult2,
                expectedResult1)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsOrderByDateDescending() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("DESCENDING(date annotation)", "person_id"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                expectedResult2,
                expectedResult1)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsOrderByString() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("string annotation", "person_id"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                expectedResult2,
                expectedResult1)
    }

    @Test
    @Throws(Exception::class)
    fun testQueryAllWithAnnotationsOrderByEnum() {
        saveReviewStatuses()
        saveAnnotations(INCLUDED_PERSON_ID, 123, "foo", true, "2017-02-14", "zebra")
        saveAnnotations(EXCLUDED_PERSON_ID, 456, null, false, "2017-02-15", "aardvark")
        val annotationQuery = AnnotationQuery()
        annotationQuery.setOrderBy(ImmutableList.of<E>("enum annotation", "person_id"))
        assertResults(
                annotationQueryBuilder!!.materializeAnnotationQuery(
                        cohortReview, ALL_STATUSES, annotationQuery, 10, 0),
                allColumns,
                expectedResult2,
                expectedResult1)
    }

    private fun saveReviewStatuses() {
        participantCohortStatusDao!!.save(
                makeStatus(cohortReview!!.cohortReviewId, INCLUDED_PERSON_ID, CohortStatus.INCLUDED))
        participantCohortStatusDao.save(
                makeStatus(cohortReview!!.cohortReviewId, EXCLUDED_PERSON_ID, CohortStatus.EXCLUDED))
    }

    @Throws(ParseException::class)
    private fun saveAnnotations(
            personId: Long,
            integerValue: Int?,
            stringValue: String?,
            booleanValue: Boolean?,
            dateValue: String?,
            enumValue: String?) {
        if (integerValue != null) {
            val annotation = ParticipantCohortAnnotation()
            annotation.cohortAnnotationDefinitionId = integerAnnotation!!.cohortAnnotationDefinitionId
            annotation.participantId = personId
            annotation.cohortReviewId = cohortReview!!.cohortReviewId
            annotation.annotationValueInteger = integerValue
            participantCohortAnnotationDao!!.save(annotation)
        }
        if (stringValue != null) {
            val annotation = ParticipantCohortAnnotation()
            annotation.cohortAnnotationDefinitionId = stringAnnotation!!.cohortAnnotationDefinitionId
            annotation.participantId = personId
            annotation.cohortReviewId = cohortReview!!.cohortReviewId
            annotation.annotationValueString = stringValue
            participantCohortAnnotationDao!!.save(annotation)
        }
        if (booleanValue != null) {
            val annotation = ParticipantCohortAnnotation()
            annotation.cohortAnnotationDefinitionId = booleanAnnotation!!.cohortAnnotationDefinitionId
            annotation.participantId = personId
            annotation.cohortReviewId = cohortReview!!.cohortReviewId
            annotation.annotationValueBoolean = booleanValue
            participantCohortAnnotationDao!!.save(annotation)
        }
        if (dateValue != null) {
            val annotation = ParticipantCohortAnnotation()
            annotation.cohortAnnotationDefinitionId = dateAnnotation!!.cohortAnnotationDefinitionId
            annotation.participantId = personId
            annotation.cohortReviewId = cohortReview!!.cohortReviewId
            val date = Date(DATE_FORMAT.parse(dateValue).time)
            annotation.annotationValueDate = date
            participantCohortAnnotationDao!!.save(annotation)
        }
        if (enumValue != null) {
            val annotation = ParticipantCohortAnnotation()
            annotation.cohortAnnotationDefinitionId = enumAnnotation!!.cohortAnnotationDefinitionId
            annotation.participantId = personId
            annotation.cohortReviewId = cohortReview!!.cohortReviewId
            annotation.cohortAnnotationEnumValue = enumValueMap!![enumValue]
            participantCohortAnnotationDao!!.save(annotation)
        }
    }

    private fun assertResults(
            results: AnnotationQueryBuilder.AnnotationResults,
            expectedColumns: List<String>?,
            vararg expectedResults: ImmutableMap<String, Any>) {
        assertThat(results.columns).isEqualTo(expectedColumns)
        val actualResults = Lists.newArrayList(results.results)
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
            val difference = Maps.difference(actualResults[i], expectedResults[i])
            if (!difference.areEqual()) {
                fail(
                        "Result "
                                + i
                                + " had difference: "
                                + difference.entriesDiffering()
                                + "; unexpected entries: "
                                + difference.entriesOnlyOnLeft()
                                + "; missing entries: "
                                + difference.entriesOnlyOnRight()
                                + "; actual result: "
                                + actualResults[i])
            }
        }
    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")

        private val INCLUDED_PERSON_ID = 1L
        private val EXCLUDED_PERSON_ID = 2L

        private val INCLUDED_ONLY = ImmutableList.of<CohortStatus>(CohortStatus.INCLUDED)

        private val ALL_STATUSES = ImmutableList.of<CohortStatus>(
                CohortStatus.INCLUDED, CohortStatus.EXCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW)
    }
}
