package org.pmiops.workbench.db.dao

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.db.model.ParticipantCohortAnnotation
import org.pmiops.workbench.model.AnnotationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ParticipantCohortAnnotationDaoTest {
    @Autowired
    private val participantCohortAnnotationDao: ParticipantCohortAnnotationDao? = null
    @Autowired
    private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null
    private var pca: ParticipantCohortAnnotation? = null
    private var pca1: ParticipantCohortAnnotation? = null
    private val cohortReviewId = 3L
    private val participantId = 4L

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val enumAnnotationDefinition = CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("enum")
                .annotationTypeEnum(AnnotationType.ENUM)
        val enumValue1 = CohortAnnotationEnumValue()
                .name("z")
                .order(0)
                .cohortAnnotationDefinition(enumAnnotationDefinition)
        val enumValue2 = CohortAnnotationEnumValue()
                .name("r")
                .order(1)
                .cohortAnnotationDefinition(enumAnnotationDefinition)
        val enumValue3 = CohortAnnotationEnumValue()
                .name("a")
                .order(2)
                .cohortAnnotationDefinition(enumAnnotationDefinition)
        enumAnnotationDefinition.enumValues.add(enumValue1)
        enumAnnotationDefinition.enumValues.add(enumValue2)
        enumAnnotationDefinition.enumValues.add(enumValue3)
        cohortAnnotationDefinitionDao!!.save(enumAnnotationDefinition)

        val booleanAnnotationDefinition = CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("boolean")
                .annotationTypeEnum(AnnotationType.BOOLEAN)

        pca = ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(
                        booleanAnnotationDefinition.cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueBoolean(java.lang.Boolean.TRUE)
        pca1 = ParticipantCohortAnnotation()
                .cohortAnnotationDefinitionId(
                        enumAnnotationDefinition.cohortAnnotationDefinitionId)
                .cohortReviewId(cohortReviewId)
                .participantId(participantId)
                .annotationValueEnum("test")
        pca1!!.cohortAnnotationEnumValue = enumValue1
        participantCohortAnnotationDao!!.save<ParticipantCohortAnnotation>(pca)
        participantCohortAnnotationDao.save<ParticipantCohortAnnotation>(pca1)
    }

    @Test
    @Throws(Exception::class)
    fun save() {
        assertEquals(pca, participantCohortAnnotationDao!!.findOne(pca!!.annotationId))
    }

    @Test
    @Throws(Exception::class)
    fun findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId() {
        assertEquals(
                pca,
                participantCohortAnnotationDao!!
                        .findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
                                cohortReviewId, pca!!.cohortAnnotationDefinitionId!!, participantId))
    }

    @Test
    @Throws(Exception::class)
    fun findByAnnotationIdAndCohortReviewIdAndParticipantId() {
        assertEquals(
                pca,
                participantCohortAnnotationDao!!.findByAnnotationIdAndCohortReviewIdAndParticipantId(
                        pca!!.annotationId!!, cohortReviewId, participantId))
    }

    @Test
    @Throws(Exception::class)
    fun findByCohortReviewIdAndParticipantId() {
        val annotations = participantCohortAnnotationDao!!.findByCohortReviewIdAndParticipantId(
                cohortReviewId, participantId)
        assertEquals(2, annotations.size.toLong())
        assertEquals(pca, annotations[0])
        assertEquals(pca1, annotations[1])
        assertEquals(
                pca1!!.cohortAnnotationEnumValue!!.cohortAnnotationDefinition!!.enumValues,
                annotations[1]
                        .cohortAnnotationEnumValue!!
                        .cohortAnnotationDefinition!!
                        .enumValues)
    }

    companion object {

        private val COHORT_ID: Long = 1
    }
}
