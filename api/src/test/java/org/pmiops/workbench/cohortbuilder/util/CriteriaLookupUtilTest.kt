package org.pmiops.workbench.cohortbuilder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import com.google.common.collect.ImmutableMap
import java.util.Arrays
import java.util.HashSet
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
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
class CriteriaLookupUtilTest {

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    private var lookupUtil: CriteriaLookupUtil? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        lookupUtil = CriteriaLookupUtil(cbCriteriaDao)
    }

    private fun saveCriteriaWithPath(path: String, criteria: CBCriteria) {
        cbCriteriaDao!!.save(criteria)
        val pathEnd = criteria.id.toString()
        criteria.path(if (path.isEmpty()) pathEnd else "$path.$pathEnd")
        cbCriteriaDao.save(criteria)
    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapNoSearchParametersException() {
        val searchRequest = SearchRequest().addIncludesItem(SearchGroup().addItemsItem(SearchGroupItem()))
        try {
            lookupUtil!!.buildCriteriaLookupMap(searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: search parameters are empty.", bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapDrugCriteria_ATC() {
        val drugNode1 = CBCriteria()
                .parentId(99999)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .conceptId("21600002")
                .group(true)
                .selectable(true)
        saveCriteriaWithPath("0", drugNode1)
        val drugNode2 = CBCriteria()
                .parentId(drugNode1.id)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.RXNORM.toString())
                .conceptId("19069022")
                .group(false)
                .selectable(true)
        saveCriteriaWithPath(drugNode1.path, drugNode2)
        val drugNode3 = CBCriteria()
                .parentId(drugNode1.id)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.RXNORM.toString())
                .conceptId("1036094")
                .group(false)
                .selectable(true)
        saveCriteriaWithPath(drugNode1.path, drugNode3)

        // Use jdbcTemplate to create/insert data into the ancestor table
        // The codebase currently doesn't have a need to implement a DAO for this table
        jdbcTemplate!!.execute(
                "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (1036094, 1036094)")

        val childConceptIds = Arrays.asList(19069022L, 1036094L)
        val searchParameter = SearchParameter()
                .domain(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .group(true)
                .ancestorData(true)
                .conceptId(21600002L)
        val searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))
        jdbcTemplate.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapDrugCriteria_RXNORM() {
        val drugNode1 = CBCriteria()
                .parentId(99999)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .conceptId("21600002")
                .group(true)
                .selectable(true)
        saveCriteriaWithPath("0", drugNode1)
        val drugNode2 = CBCriteria()
                .parentId(drugNode1.id)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.RXNORM.toString())
                .conceptId("19069022")
                .group(false)
                .selectable(true)
        saveCriteriaWithPath(drugNode1.path, drugNode2)

        // Use jdbcTemplate to create/insert data into the ancestor table
        // The codebase currently doesn't have a need to implement a DAO for this table
        jdbcTemplate!!.execute(
                "create table cb_criteria_ancestor(ancestor_id integer, descendant_id integer)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 19069022)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values (19069022, 1666666)")

        val childConceptIds = Arrays.asList(1666666L)
        val searchParameter = SearchParameter()
                .domain(DomainType.DRUG.toString())
                .type(CriteriaType.RXNORM.toString())
                .group(true)
                .ancestorData(true)
                .conceptId(19069022L)
        val searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))
        jdbcTemplate.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapPPICriteria() {
        val surveyNode = CBCriteria()
                .parentId(0)
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.SURVEY.toString())
                .group(true)
                .selectable(true)
                .standard(false)
                .conceptId("22")
        saveCriteriaWithPath("0", surveyNode)
        val questionNode = CBCriteria()
                .parentId(surveyNode.id)
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.QUESTION.toString())
                .group(true)
                .selectable(true)
                .standard(false)
                .name("In what country were you born?")
                .conceptId("1586135")
                .synonyms("[SURVEY_rank1]")
        saveCriteriaWithPath(surveyNode.path, questionNode)
        val answerNode = CBCriteria()
                .parentId(questionNode.id)
                .domainId(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.ANSWER.toString())
                .group(false)
                .selectable(true)
                .standard(false)
                .name("USA")
                .conceptId("5")
                .synonyms("[SURVEY_rank1]")
        saveCriteriaWithPath(questionNode.path, answerNode)

        // Survey search
        var childConceptIds = Arrays.asList(5L, 1586135L)
        var searchParameter = SearchParameter()
                .domain(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.SURVEY.toString())
                .group(true)
                .standard(false)
                .ancestorData(false)
                .conceptId(22L)
        var searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))

        // Question search
        childConceptIds = Arrays.asList(5L)
        searchParameter = SearchParameter()
                .domain(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .group(true)
                .standard(false)
                .ancestorData(false)
                .conceptId(1586135L)
        searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))

        // Answer search
        searchParameter = SearchParameter()
                .domain(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .group(false)
                .standard(false)
                .ancestorData(false)
                .conceptId(5L)
        searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertTrue(lookupUtil!!.buildCriteriaLookupMap(searchRequest).isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapConditionSnomedCriteria() {
        val snomedParent1 = CBCriteria()
                .parentId(0)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .group(true)
                .selectable(true)
                .standard(true)
                .conceptId("132277")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath("0", snomedParent1)
        val snomedParent2 = CBCriteria()
                .parentId(snomedParent1.id)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .group(true)
                .selectable(true)
                .standard(true)
                .conceptId("27835")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath(snomedParent1.path, snomedParent2)
        val snomedChild = CBCriteria()
                .parentId(snomedParent2.id)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .group(false)
                .selectable(true)
                .standard(true)
                .conceptId("4099351")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath(snomedParent2.path, snomedChild)

        val childConceptIds = Arrays.asList(27835L, 4099351L)
        val searchParameter = SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .group(true)
                .standard(true)
                .ancestorData(false)
                .conceptId(132277L)
        val searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))
    }

    @Test
    @Throws(Exception::class)
    fun buildCriteriaLookupMapConditionICD9Criteria() {
        val icd9Parent = CBCriteria()
                .parentId(0)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .group(true)
                .selectable(true)
                .standard(false)
                .conceptId("44829696")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath("0", icd9Parent)
        val icd9Child1 = CBCriteria()
                .parentId(icd9Parent.id)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .group(false)
                .selectable(true)
                .standard(false)
                .conceptId("44829697")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath(icd9Parent.path, icd9Child1)
        val icd9Child2 = CBCriteria()
                .parentId(icd9Parent.id)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .group(false)
                .selectable(true)
                .standard(false)
                .conceptId("44835564")
                .synonyms("[CONDITION_rank1]")
        saveCriteriaWithPath(icd9Parent.path, icd9Child2)

        val childConceptIds = Arrays.asList(44829697L, 44835564L)
        val searchParameter = SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .group(true)
                .standard(false)
                .ancestorData(false)
                .conceptId(44829696L)
        val searchRequest = SearchRequest()
                .addIncludesItem(
                        SearchGroup()
                                .addItemsItem(SearchGroupItem().addSearchParametersItem(searchParameter)))
        assertEquals(
                ImmutableMap.of<Any, HashSet<Long>>(searchParameter, HashSet(childConceptIds)),
                lookupUtil!!.buildCriteriaLookupMap(searchRequest))
    }
}
