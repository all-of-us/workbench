package org.pmiops.workbench.db.dao

import org.junit.Assert.assertEquals

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue
import org.pmiops.workbench.model.AnnotationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class CohortAnnotationDefinitionDaoTest {
    @Autowired
    internal var cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null
    private var cohortAnnotationDefinition: CohortAnnotationDefinition? = null

    @Before
    fun setUp() {
        cohortAnnotationDefinition = cohortAnnotationDefinitionDao!!.save(createCohortAnnotationDefinition())
    }

    @Test
    @Throws(Exception::class)
    fun saveNoEnumValues() {
        assertEquals(
                cohortAnnotationDefinition,
                cohortAnnotationDefinitionDao!!.findOne(
                        cohortAnnotationDefinition!!.cohortAnnotationDefinitionId))
    }

    @Test
    @Throws(Exception::class)
    fun saveWithEnumValues() {
        val cohortAnnotationDefinition = createCohortAnnotationDefinition()
        val enumValue1 = CohortAnnotationEnumValue()
                .name("z")
                .order(0)
                .cohortAnnotationDefinition(cohortAnnotationDefinition)
        val enumValue2 = CohortAnnotationEnumValue()
                .name("r")
                .order(1)
                .cohortAnnotationDefinition(cohortAnnotationDefinition)
        val enumValue3 = CohortAnnotationEnumValue()
                .name("a")
                .order(2)
                .cohortAnnotationDefinition(cohortAnnotationDefinition)
        cohortAnnotationDefinition.enumValues.add(enumValue1)
        cohortAnnotationDefinition.enumValues.add(enumValue2)
        cohortAnnotationDefinition.enumValues.add(enumValue3)

        cohortAnnotationDefinitionDao!!.save(cohortAnnotationDefinition)

        val cad = cohortAnnotationDefinitionDao!!.findOne(
                cohortAnnotationDefinition.cohortAnnotationDefinitionId)
        assertEquals(cohortAnnotationDefinition, cad)
        assertEquals(cohortAnnotationDefinition.enumValues, cad.enumValues)
    }

    @Test
    @Throws(Exception::class)
    fun findByCohortIdAndColumnName() {
        assertEquals(
                cohortAnnotationDefinition,
                cohortAnnotationDefinitionDao!!.findByCohortIdAndColumnName(
                        cohortAnnotationDefinition!!.cohortId, cohortAnnotationDefinition!!.columnName))
    }

    @Test
    @Throws(Exception::class)
    fun findByCohortIdOrderByEnumValuesAsc() {
        assertEquals(
                cohortAnnotationDefinition,
                cohortAnnotationDefinitionDao!!
                        .findByCohortId(cohortAnnotationDefinition!!.cohortId)[0])
    }

    private fun createCohortAnnotationDefinition(): CohortAnnotationDefinition {
        return CohortAnnotationDefinition()
                .cohortId(COHORT_ID)
                .columnName("annotation name")
                .annotationTypeEnum(AnnotationType.BOOLEAN)
    }

    companion object {

        private val COHORT_ID: Long = 1
    }
}
