package org.pmiops.workbench.cdr.dao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

import java.util.Arrays
import java.util.HashSet
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.FilterColumns
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CBCriteriaDaoTest {

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null
    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null
    private var surveyCriteria: CBCriteria? = null
    private var sourceCriteria: CBCriteria? = null
    private var standardCriteria: CBCriteria? = null
    private var icd9Criteria: CBCriteria? = null
    private var icd10Criteria: CBCriteria? = null
    private var measurementCriteria: CBCriteria? = null
    private var raceParent: CBCriteria? = null
    private var raceAsian: CBCriteria? = null
    private var raceWhite: CBCriteria? = null

    @Before
    fun setUp() {
        surveyCriteria = cbCriteriaDao!!.save(
                CBCriteria()
                        .domainId(DomainType.SURVEY.toString())
                        .type(CriteriaType.PPI.toString())
                        .subtype(CriteriaSubType.SURVEY.toString())
                        .group(false)
                        .selectable(true))
        sourceCriteria = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.CONDITION.toString())
                        .count("100")
                        .standard(false)
                        .code("120"))
        standardCriteria = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.CONDITION.toString())
                        .type(CriteriaType.SNOMED.toString())
                        .count("100")
                        .hierarchy(true)
                        .conceptId("1")
                        .standard(true)
                        .code("120")
                        .synonyms("myMatch[CONDITION_rank1]"))
        icd9Criteria = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.CONDITION.toString())
                        .type(CriteriaType.ICD9CM.toString())
                        .count("100")
                        .standard(false)
                        .code("001")
                        .synonyms("+[CONDITION_rank1]")
                        .path("1.5.99"))
        icd10Criteria = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.CONDITION.toString())
                        .type(CriteriaType.ICD10CM.toString())
                        .count("100")
                        .standard(false)
                        .conceptId("1")
                        .code("122")
                        .synonyms("+[CONDITION_rank1]"))
        measurementCriteria = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.MEASUREMENT.toString())
                        .type(CriteriaType.LOINC.toString())
                        .count("100")
                        .hierarchy(true)
                        .standard(true)
                        .code("LP123")
                        .synonyms("001[MEASUREMENT_rank1]"))
        raceParent = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.RACE.toString())
                        .name("Race")
                        .parentId(0))
        raceAsian = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.RACE.toString())
                        .name("Asian")
                        .parentId(raceParent!!.id))
        raceWhite = cbCriteriaDao.save(
                CBCriteria()
                        .domainId(DomainType.PERSON.toString())
                        .type(CriteriaType.RACE.toString())
                        .name("White")
                        .parentId(raceParent!!.id))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaLeavesByDomainAndTypeAndSubtype() {
        val criteriaList = cbCriteriaDao!!.findCriteriaLeavesByDomainAndTypeAndSubtype(
                DomainType.SURVEY.toString(),
                CriteriaType.PPI.toString(),
                CriteriaSubType.SURVEY.toString())
        assertEquals(1, criteriaList.size.toLong())
        assertEquals(surveyCriteria, criteriaList[0])
    }

    @Test
    @Throws(Exception::class)
    fun findExactMatchByCode() {
        // test that we match both source and standard codes
        val exactMatchByCode = cbCriteriaDao!!.findExactMatchByCode(DomainType.CONDITION.toString(), "120")
        assertEquals(2, exactMatchByCode.size.toLong())
        assertEquals(standardCriteria, exactMatchByCode[0])
        assertEquals(sourceCriteria, exactMatchByCode[1])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndTypeAndCode() {
        val page = PageRequest(0, 10)
        val criteriaList = cbCriteriaDao!!.findCriteriaByDomainAndTypeAndCode(
                DomainType.CONDITION.toString(),
                CriteriaType.ICD9CM.toString(),
                java.lang.Boolean.FALSE,
                "00",
                page)
        assertEquals(1, criteriaList.size.toLong())
        assertEquals(icd9Criteria, criteriaList[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndCode() {
        val page = PageRequest(0, 10)
        val criteriaList = cbCriteriaDao!!.findCriteriaByDomainAndCode(
                DomainType.CONDITION.toString(), java.lang.Boolean.FALSE, "001", page)
        assertEquals(1, criteriaList.size.toLong())
        assertEquals(icd9Criteria, criteriaList[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSynonyms() {
        val page = PageRequest(0, 10)
        val measurements = cbCriteriaDao!!.findCriteriaByDomainAndSynonyms(
                DomainType.MEASUREMENT.toString(), java.lang.Boolean.TRUE, "001", page)
        assertEquals(1, measurements.size.toLong())
        assertEquals(measurementCriteria, measurements[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc() {
        val actualIcd9 = cbCriteriaDao!!
                .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
                        DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)[0]
        assertEquals(icd9Criteria, actualIcd9)

        val actualIcd10 = cbCriteriaDao
                .findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
                        DomainType.CONDITION.toString(), CriteriaType.ICD10CM.toString(), false, 0L)[0]
        assertEquals(icd10Criteria, actualIcd10)
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndTypeOrderByIdAsc() {
        val demoList = cbCriteriaDao!!.findCriteriaByDomainAndTypeOrderByIdAsc(
                DomainType.PERSON.toString(), CriteriaType.RACE.toString())
        assertEquals(3, demoList.size.toLong())
        assertEquals(raceParent, demoList[0])
        assertEquals(raceAsian, demoList[1])
        assertEquals(raceWhite, demoList[2])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndTypeAndStandardAndCode() {
        val page = PageRequest(0, 10)
        val labs = cbCriteriaDao!!.findCriteriaByDomainAndTypeAndStandardAndCode(
                DomainType.MEASUREMENT.toString(), CriteriaType.LOINC.toString(), true, "LP123", page)
        assertEquals(1, labs.size.toLong())
        assertEquals(measurementCriteria, labs[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndTypeAndStandardAndSynonyms() {
        val page = PageRequest(0, 10)
        val conditions = cbCriteriaDao!!.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
                DomainType.CONDITION.toString(), CriteriaType.SNOMED.toString(), true, "myMatch", page)
        assertEquals(1, conditions.size.toLong())
        assertEquals(standardCriteria, conditions[0])
    }

    @Test
    @Throws(Exception::class)
    fun findConceptId2ByConceptId1() {
        jdbcTemplate!!.execute(
                "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)")
        jdbcTemplate.execute(
                "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)")
        assertEquals(1, cbCriteriaDao!!.findConceptId2ByConceptId1(12345L)[0].toInt().toLong())
        jdbcTemplate.execute("drop table cb_criteria_relationship")
    }

    @Test
    @Throws(Exception::class)
    fun findStandardCriteriaByDomainAndConceptId() {
        assertEquals(
                icd10Criteria,
                cbCriteriaDao!!
                        .findStandardCriteriaByDomainAndConceptId(
                                DomainType.CONDITION.toString(), false, Arrays.asList("1"))[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaParentsByDomainAndTypeAndParentConceptIds() {
        val parentConceptIds = HashSet<String>()
        parentConceptIds.add("1")
        val results = cbCriteriaDao!!.findCriteriaParentsByDomainAndTypeAndParentConceptIds(
                DomainType.CONDITION.toString(),
                CriteriaType.SNOMED.toString(),
                true,
                parentConceptIds)
        assertEquals(standardCriteria, results[0])
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaLeavesAndParentsByPath() {
        assertEquals(icd9Criteria, cbCriteriaDao!!.findCriteriaLeavesAndParentsByPath("5")[0])
    }

    @Test
    @Throws(Exception::class)
    fun findGenderRaceEthnicity() {
        val criteriaList = cbCriteriaDao!!.findGenderRaceEthnicity()
        assertEquals(2, criteriaList.size.toLong())
        assertTrue(criteriaList.contains(raceAsian))
        assertTrue(criteriaList.contains(raceWhite))
    }

    @Test
    @Throws(Exception::class)
    fun findByDomainIdAndTypeAndParentIdNotIn() {
        var sort = Sort(Direction.ASC, "name")
        var criteriaList = cbCriteriaDao!!.findByDomainIdAndTypeAndParentIdNotIn(
                DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort)
        assertEquals(2, criteriaList.size.toLong())
        assertEquals(raceAsian, criteriaList[0])
        assertEquals(raceWhite, criteriaList[1])

        // reverse
        sort = Sort(Direction.DESC, "name")
        criteriaList = cbCriteriaDao.findByDomainIdAndTypeAndParentIdNotIn(
                DomainType.PERSON.toString(), FilterColumns.RACE.toString(), 0L, sort)
        assertEquals(2, criteriaList.size.toLong())
        assertEquals(raceWhite, criteriaList[0])
        assertEquals(raceAsian, criteriaList[1])
    }
}
