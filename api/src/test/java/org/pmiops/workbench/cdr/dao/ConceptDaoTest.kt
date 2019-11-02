package org.pmiops.workbench.cdr.dao

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.model.Concept
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
class ConceptDaoTest {

    @Autowired
    internal var conceptDao: ConceptDao? = null
    private var ethnicityConcept: Concept? = null
    private var genderConcept: Concept? = null
    private var raceConcept: Concept? = null

    @Before
    fun setUp() {

        ethnicityConcept = conceptDao!!.save(
                Concept().conceptId(1L).conceptName("ethnicity").vocabularyId("Ethnicity"))
        genderConcept = conceptDao!!.save(Concept().conceptId(2L).conceptName("gender").vocabularyId("Gender"))
        raceConcept = conceptDao!!.save(Concept().conceptId(3L).conceptName("race").vocabularyId("Race"))
    }

    @Test
    @Throws(Exception::class)
    fun findGenderRaceEthnicityFromConcept() {
        val concepts = conceptDao!!.findGenderRaceEthnicityFromConcept()
        assertEquals(ethnicityConcept, concepts[0])
        assertEquals(genderConcept, concepts[1])
        assertEquals(raceConcept, concepts[2])
    }
}
