package org.pmiops.workbench.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.cdr.model.CBCriteriaAttribute
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.elasticsearch.ElasticSearchService
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.google.CloudStorageService
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
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CohortBuilderControllerTest {

    private var controller: CohortBuilderController? = null

    @Mock
    private val bigQueryService: BigQueryService? = null

    @Mock
    private val cloudStorageService: CloudStorageService? = null

    @Mock
    private val cohortQueryBuilder: CohortQueryBuilder? = null

    @Mock
    private val cdrVersionDao: CdrVersionDao? = null

    @Mock
    private val cdrVersionService: CdrVersionService? = null

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null

    @Autowired
    private val cbCriteriaAttributeDao: CBCriteriaAttributeDao? = null

    @Autowired
    private val conceptDao: ConceptDao? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    @Mock
    private val configProvider: Provider<WorkbenchConfig>? = null

    private val testConfig: WorkbenchConfig? = null

    @Before
    fun setUp() {
        val elasticSearchService = ElasticSearchService(cbCriteriaDao, cloudStorageService, configProvider)

        controller = CohortBuilderController(
                bigQueryService,
                cohortQueryBuilder,
                cbCriteriaDao,
                cbCriteriaAttributeDao,
                cdrVersionDao,
                cdrVersionService,
                elasticSearchService,
                configProvider)
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaBy() {
        val icd9CriteriaParent = CBCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .count("0")
                .hierarchy(true)
                .standard(false)
                .parentId(0L)
        cbCriteriaDao!!.save(icd9CriteriaParent)
        val icd9Criteria = CBCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .count("0")
                .hierarchy(true)
                .standard(false)
                .parentId(icd9CriteriaParent.id)
        cbCriteriaDao.save(icd9Criteria)

        assertEquals(
                createResponseCriteria(icd9CriteriaParent),
                controller!!
                        .getCriteriaBy(
                                1L, DomainType.CONDITION.toString(), CriteriaType.ICD9CM.toString(), false, 0L)
                        .body
                        .getItems()
                        .get(0))
        assertEquals(
                createResponseCriteria(icd9Criteria),
                controller!!
                        .getCriteriaBy(
                                1L,
                                DomainType.CONDITION.toString(),
                                CriteriaType.ICD9CM.toString(),
                                false,
                                icd9CriteriaParent.id)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaByExceptions() {
        try {
            controller!!.getCriteriaBy(1L, null, null, false, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid domain. null is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaBy(1L, "blah", null, false, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid type. null is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaBy(1L, "blah", "blah", false, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid domain. blah is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaBy(1L, DomainType.CONDITION.toString(), "blah", false, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid type. blah is not valid.", bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaByDemo() {
        val demoCriteria = CBCriteria()
                .domainId(DomainType.PERSON.toString())
                .type(CriteriaType.AGE.toString())
                .count("0")
                .parentId(0L)
        cbCriteriaDao!!.save(demoCriteria)

        assertEquals(
                createResponseCriteria(demoCriteria),
                controller!!
                        .getCriteriaBy(
                                1L, DomainType.PERSON.toString(), CriteriaType.AGE.toString(), false, null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaAutoCompleteMatchesSynonyms() {
        val criteria = CBCriteria()
                .domainId(DomainType.MEASUREMENT.toString())
                .type(CriteriaType.LOINC.toString())
                .count("0")
                .hierarchy(true)
                .standard(true)
                .synonyms("LP12*[MEASUREMENT_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .getCriteriaAutoComplete(
                                1L,
                                DomainType.MEASUREMENT.toString(),
                                "LP12",
                                CriteriaType.LOINC.toString(),
                                true, null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaAutoCompleteMatchesCode() {
        val criteria = CBCriteria()
                .domainId(DomainType.MEASUREMENT.toString())
                .type(CriteriaType.LOINC.toString())
                .count("0")
                .hierarchy(true)
                .standard(true)
                .code("LP123")
                .synonyms("+[MEASUREMENT_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .getCriteriaAutoComplete(
                                1L,
                                DomainType.MEASUREMENT.toString(),
                                "LP12",
                                CriteriaType.LOINC.toString(),
                                true, null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaAutoCompleteSnomed() {
        val criteria = CBCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .count("0")
                .hierarchy(true)
                .standard(true)
                .synonyms("LP12*[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .getCriteriaAutoComplete(
                                1L,
                                DomainType.CONDITION.toString(),
                                "LP12",
                                CriteriaType.SNOMED.toString(),
                                true, null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaAutoCompleteExceptions() {
        try {
            controller!!.getCriteriaAutoComplete(1L, null, "blah", null, null, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid domain. null is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaAutoComplete(1L, "blah", "blah", null, null, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid type. null is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaAutoComplete(1L, "blah", "blah", "blah", null, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid domain. blah is not valid.", bre.message)
        }

        try {
            controller!!.getCriteriaAutoComplete(
                    1L, DomainType.CONDITION.toString(), "blah", "blah", null, null)
            fail("Should have thrown a BadRequestException!")
        } catch (bre: BadRequestException) {
            // success
            assertEquals(
                    "Bad Request: Please provide a valid type. blah is not valid.", bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermMatchesSourceCode() {
        val criteria = CBCriteria()
                .code("001")
                .count("10")
                .conceptId("123")
                .domainId(DomainType.CONDITION.toString())
                .group(java.lang.Boolean.TRUE)
                .selectable(java.lang.Boolean.TRUE)
                .name("chol blah")
                .parentId(0)
                .type(CriteriaType.ICD9CM.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(false)
                .synonyms("[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "001", null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermLikeSourceCode() {
        val criteria = CBCriteria()
                .code("00")
                .count("10")
                .conceptId("123")
                .domainId(DomainType.CONDITION.toString())
                .group(java.lang.Boolean.TRUE)
                .selectable(java.lang.Boolean.TRUE)
                .name("chol blah")
                .parentId(0)
                .type(CriteriaType.ICD9CM.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(false)
                .synonyms("+[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)

        val results = controller!!
                .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "00", null)
                .body
                .getItems()

        assertEquals(1, results.size.toLong())
        assertEquals(createResponseCriteria(criteria), results.get(0))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermDrugMatchesStandardCodeBrand() {
        val criteria1 = CBCriteria()
                .code("672535")
                .count("-1")
                .conceptId("19001487")
                .domainId(DomainType.DRUG.toString())
                .group(java.lang.Boolean.FALSE)
                .selectable(java.lang.Boolean.TRUE)
                .name("4-Way")
                .parentId(0)
                .type(CriteriaType.BRAND.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(true)
                .synonyms("[DRUG_rank1]")
        cbCriteriaDao!!.save(criteria1)

        val results = controller!!
                .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "672535", null)
                .body
                .getItems()
        assertEquals(1, results.size.toLong())
        assertEquals(createResponseCriteria(criteria1), results.get(0))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermMatchesStandardCode() {
        val criteria = CBCriteria()
                .code("LP12")
                .count("10")
                .conceptId("123")
                .domainId(DomainType.CONDITION.toString())
                .group(java.lang.Boolean.TRUE)
                .selectable(java.lang.Boolean.TRUE)
                .name("chol blah")
                .parentId(0)
                .type(CriteriaType.LOINC.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(true)
                .synonyms("[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermMatchesSynonyms() {
        val criteria = CBCriteria()
                .code("001")
                .count("10")
                .conceptId("123")
                .domainId(DomainType.CONDITION.toString())
                .group(java.lang.Boolean.TRUE)
                .selectable(java.lang.Boolean.TRUE)
                .name("chol blah")
                .parentId(0)
                .type(CriteriaType.LOINC.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(true)
                .synonyms("LP12*[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .findCriteriaByDomainAndSearchTerm(1L, DomainType.CONDITION.name(), "LP12", null)
                        .body
                        .getItems()
                        .get(0))
    }

    @Test
    @Throws(Exception::class)
    fun findCriteriaByDomainAndSearchTermDrugMatchesSynonyms() {
        jdbcTemplate!!.execute(
                "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)")
        val criteria = CBCriteria()
                .code("001")
                .count("10")
                .conceptId("123")
                .domainId(DomainType.DRUG.toString())
                .group(java.lang.Boolean.TRUE)
                .selectable(java.lang.Boolean.TRUE)
                .name("chol blah")
                .parentId(0)
                .type(CriteriaType.ATC.toString())
                .attribute(java.lang.Boolean.FALSE)
                .standard(true)
                .synonyms("LP12*[DRUG_rank1]")
        cbCriteriaDao!!.save(criteria)

        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .findCriteriaByDomainAndSearchTerm(1L, DomainType.DRUG.name(), "LP12", null)
                        .body
                        .getItems()
                        .get(0))
        jdbcTemplate.execute("drop table cb_criteria_relationship")
    }

    @Test
    @Throws(Exception::class)
    fun getStandardCriteriaByDomainAndConceptId() {
        jdbcTemplate!!.execute(
                "create table cb_criteria_relationship(concept_id_1 integer, concept_id_2 integer)")
        jdbcTemplate.execute(
                "insert into cb_criteria_relationship(concept_id_1, concept_id_2) values (12345, 1)")
        val criteria = CBCriteria()
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD10CM.toString())
                .standard(true)
                .count("1")
                .conceptId("1")
                .synonyms("[CONDITION_rank1]")
        cbCriteriaDao!!.save(criteria)
        assertEquals(
                createResponseCriteria(criteria),
                controller!!
                        .getStandardCriteriaByDomainAndConceptId(1L, DomainType.CONDITION.toString(), 12345L)
                        .body
                        .getItems()
                        .get(0))
        jdbcTemplate.execute("drop table cb_criteria_relationship")
    }

    @Test
    @Throws(Exception::class)
    fun getDrugBrandOrIngredientByName() {
        val drugATCCriteria = CBCriteria()
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .parentId(0L)
                .code("LP12345")
                .name("drugName")
                .conceptId("12345")
                .group(true)
                .selectable(true)
                .count("12")
        cbCriteriaDao!!.save(drugATCCriteria)
        val drugBrandCriteria = CBCriteria()
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.BRAND.toString())
                .parentId(0L)
                .code("LP6789")
                .name("brandName")
                .conceptId("1235")
                .group(true)
                .selectable(true)
                .count("33")
        cbCriteriaDao.save(drugBrandCriteria)

        assertEquals(
                createResponseCriteria(drugATCCriteria),
                controller!!.getDrugBrandOrIngredientByValue(1L, "drugN", null).body.getItems().get(0))

        assertEquals(
                createResponseCriteria(drugBrandCriteria),
                controller!!.getDrugBrandOrIngredientByValue(1L, "brandN", null).body.getItems().get(0))

        assertEquals(
                createResponseCriteria(drugBrandCriteria),
                controller!!.getDrugBrandOrIngredientByValue(1L, "LP6789", null).body.getItems().get(0))
    }

    @Test
    @Throws(Exception::class)
    fun getCriteriaAttributeByConceptId() {
        val criteriaAttributeMin = cbCriteriaAttributeDao!!.save(
                CBCriteriaAttribute()
                        .conceptId(1L)
                        .conceptName("MIN")
                        .estCount("10")
                        .type("NUM")
                        .valueAsConceptId(0L))
        val criteriaAttributeMax = cbCriteriaAttributeDao.save(
                CBCriteriaAttribute()
                        .conceptId(1L)
                        .conceptName("MAX")
                        .estCount("100")
                        .type("NUM")
                        .valueAsConceptId(0L))

        val attrs = controller!!
                .getCriteriaAttributeByConceptId(1L, criteriaAttributeMin.conceptId)
                .body
                .getItems()
        assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMin)))
        assertTrue(attrs.contains(createResponseCriteriaAttribute(criteriaAttributeMax)))
    }

    @Test
    @Throws(Exception::class)
    fun isApproximate() {
        val inSearchParameter = SearchParameter()
        val exSearchParameter = SearchParameter()
        val inSearchGroupItem = SearchGroupItem().addSearchParametersItem(inSearchParameter)
        val exSearchGroupItem = SearchGroupItem().addSearchParametersItem(exSearchParameter)
        val inSearchGroup = SearchGroup().addItemsItem(inSearchGroupItem)
        val exSearchGroup = SearchGroup().addItemsItem(exSearchGroupItem)
        val searchRequest = SearchRequest().addIncludesItem(inSearchGroup).addExcludesItem(exSearchGroup)
        // Temporal includes
        inSearchGroup.temporal(true)
        assertTrue(controller!!.isApproximate(searchRequest))
        // BP includes
        inSearchGroup.temporal(false)
        inSearchParameter.subtype(CriteriaSubType.BP.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
        // Deceased includes
        inSearchParameter.type(CriteriaType.DECEASED.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
        // Temporal and BP includes
        inSearchGroup.temporal(true)
        inSearchParameter.subtype(CriteriaSubType.BP.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
        // No temporal/BP/Decease
        inSearchGroup.temporal(false)
        inSearchParameter.type(CriteriaType.ETHNICITY.toString()).subtype(null)
        assertFalse(controller!!.isApproximate(searchRequest))
        // Temporal excludes
        exSearchGroup.temporal(true)
        assertTrue(controller!!.isApproximate(searchRequest))
        // BP excludes
        exSearchGroup.temporal(false)
        exSearchParameter.subtype(CriteriaSubType.BP.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
        // Deceased excludes
        exSearchParameter.type(CriteriaType.DECEASED.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
        // Temporal and BP excludes
        exSearchGroup.temporal(true)
        exSearchParameter.subtype(CriteriaSubType.BP.toString())
        assertTrue(controller!!.isApproximate(searchRequest))
    }

    private fun createResponseCriteria(cbCriteria: CBCriteria): org.pmiops.workbench.model.Criteria {
        return org.pmiops.workbench.model.Criteria()
                .code(cbCriteria.code)
                .conceptId(if (cbCriteria.conceptId == null) null else Long(cbCriteria.conceptId))
                .count(Long(cbCriteria.count))
                .domainId(cbCriteria.domainId)
                .group(cbCriteria.group)
                .hasAttributes(cbCriteria.attribute)
                .id(cbCriteria.id)
                .name(cbCriteria.name)
                .parentId(cbCriteria.parentId)
                .selectable(cbCriteria.selectable)
                .subtype(cbCriteria.subtype)
                .type(cbCriteria.type)
                .path(cbCriteria.path)
                .hasAncestorData(cbCriteria.ancestorData)
                .hasHierarchy(cbCriteria.hierarchy)
                .isStandard(cbCriteria.standard)
                .value(cbCriteria.value)
    }

    private fun createResponseCriteriaAttribute(
            criteriaAttribute: CBCriteriaAttribute): org.pmiops.workbench.model.CriteriaAttribute {
        return org.pmiops.workbench.model.CriteriaAttribute()
                .id(criteriaAttribute.id)
                .valueAsConceptId(criteriaAttribute.valueAsConceptId)
                .conceptName(criteriaAttribute.conceptName)
                .type(criteriaAttribute.type)
                .estCount(criteriaAttribute.estCount)
    }
}
