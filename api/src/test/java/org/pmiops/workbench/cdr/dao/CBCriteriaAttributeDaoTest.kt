package org.pmiops.workbench.cdr.dao

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.model.CBCriteriaAttribute
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
class CBCriteriaAttributeDaoTest {

    @Autowired
    private val cbCriteriaAttributeDao: CBCriteriaAttributeDao? = null
    private var attribute: CBCriteriaAttribute? = null

    @Before
    fun onSetup() {
        attribute = cbCriteriaAttributeDao!!.save(
                CBCriteriaAttribute()
                        .conceptId(1L)
                        .conceptName("test")
                        .estCount("10")
                        .type("type")
                        .valueAsConceptId(12345678L))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaAttributeByConceptId() {
        val attributes = cbCriteriaAttributeDao!!.findCriteriaAttributeByConceptId(1L)
        assertEquals(1, attributes.size.toLong())
        assertEquals(attribute, attributes[0])
    }
}
