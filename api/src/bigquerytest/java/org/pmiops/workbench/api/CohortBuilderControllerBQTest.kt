package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`

import com.google.cloud.bigquery.QueryJobConfiguration
import java.util.ArrayList
import java.util.Arrays
import javax.inject.Provider
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner
import org.joda.time.DateTime
import org.joda.time.Period
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.cdr.CdrVersionService
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao
import org.pmiops.workbench.cdr.dao.CBCriteriaDao
import org.pmiops.workbench.cdr.model.CBCriteria
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.elasticsearch.ElasticSearchService
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.google.CloudStorageServiceImpl
import org.pmiops.workbench.model.AttrName
import org.pmiops.workbench.model.Attribute
import org.pmiops.workbench.model.CriteriaSubType
import org.pmiops.workbench.model.CriteriaType
import org.pmiops.workbench.model.DemoChartInfo
import org.pmiops.workbench.model.DemoChartInfoListResponse
import org.pmiops.workbench.model.DomainType
import org.pmiops.workbench.model.Modifier
import org.pmiops.workbench.model.ModifierType
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.SearchGroup
import org.pmiops.workbench.model.SearchGroupItem
import org.pmiops.workbench.model.SearchParameter
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.model.TemporalMention
import org.pmiops.workbench.model.TemporalTime
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.pmiops.workbench.testconfig.TestWorkbenchConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate

@RunWith(BeforeAfterSpringTestRunner::class)
// Note: normally we shouldn't need to explicitly import our own @TestConfiguration. This might be
// a bad interaction with BeforeAfterSpringTestRunner.
@Import(TestJpaConfig::class, CohortBuilderControllerBQTest.Configuration::class)
@ComponentScan(basePackages = ["org.pmiops.workbench.cohortbuilder.*"])
class CohortBuilderControllerBQTest : BigQueryBaseTest() {

    private var controller: CohortBuilderController? = null

    private var cdrVersion: CdrVersion? = null

    @Autowired
    private val bigQueryService: BigQueryService? = null

    @Autowired
    private val cloudStorageService: CloudStorageService? = null

    @Autowired
    private val cohortQueryBuilder: CohortQueryBuilder? = null

    @Autowired
    private val cdrVersionDao: CdrVersionDao? = null

    @Autowired
    private val cbCriteriaDao: CBCriteriaDao? = null

    @Autowired
    private val cdrVersionService: CdrVersionService? = null

    @Autowired
    private val cbCriteriaAttributeDao: CBCriteriaAttributeDao? = null

    @Autowired
    private val firecloudService: FireCloudService? = null

    @Autowired
    private val testWorkbenchConfig: TestWorkbenchConfig? = null

    @Mock
    private val configProvider: Provider<WorkbenchConfig>? = null

    @Autowired
    private val jdbcTemplate: JdbcTemplate? = null

    override val tableNames: List<String>
        get() = Arrays.asList("person", "death", "cb_search_person", "cb_search_all_events")

    override val testDataDirectory: String
        get() = BigQueryBaseTest.CB_DATA

    protected val tablePrefix: String
        get() {
            val cdrVersion = CdrVersionContext.getCdrVersion()
            return cdrVersion.bigqueryProject + "." + cdrVersion.bigqueryDataset
        }

    private val testPeriod: Period
        get() {
            val birthDate = DateTime(1980, 8, 1, 0, 0, 0, 0)
            val now = DateTime()
            return Period(birthDate, now)
        }

    @TestConfiguration
    @Import(BigQueryTestService::class, CloudStorageServiceImpl::class, CohortQueryBuilder::class, SearchGroupItemQueryBuilder::class, CdrVersionService::class)
    @MockBean(FireCloudService::class)
    internal class Configuration {
        @Bean
        fun user(): User {
            val user = User()
            user.email = "bob@gmail.com"
            return user
        }
    }

    @Before
    fun setUp() {
        val testConfig = WorkbenchConfig()
        testConfig.elasticsearch = WorkbenchConfig.ElasticsearchConfig()
        testConfig.elasticsearch.enableElasticsearchBackend = false
        `when`(configProvider!!.get()).thenReturn(testConfig)

        `when`(firecloudService!!.isUserMemberOfGroup(anyString(), anyString())).thenReturn(true)

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

        cdrVersion = CdrVersion()
        cdrVersion!!.cdrVersionId = 1L
        cdrVersion!!.bigqueryDataset = testWorkbenchConfig!!.bigquery!!.dataSetId
        cdrVersion!!.bigqueryProject = testWorkbenchConfig.bigquery!!.projectId
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)

        cdrVersionDao!!.save<CdrVersion>(cdrVersion)
    }

    private fun icd9(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .standard(false)
                .ancestorData(false)
                .group(false)
                .conceptId(1L)
    }

    private fun icd10(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD10CM.toString())
                .standard(false)
                .ancestorData(false)
                .group(false)
                .conceptId(9L)
    }

    private fun snomed(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.CONDITION.toString())
                .type(CriteriaType.SNOMED.toString())
                .standard(false)
                .ancestorData(false)
                .group(false)
                .conceptId(4L)
    }

    private fun drug(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .group(false)
                .ancestorData(true)
                .standard(true)
                .conceptId(11L)
    }

    private fun measurement(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.MEASUREMENT.toString())
                .type(CriteriaType.LOINC.toString())
                .subtype(CriteriaSubType.LAB.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .conceptId(3L)
    }

    private fun visit(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.VISIT.toString())
                .type(CriteriaType.VISIT.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
                .conceptId(1L)
    }

    private fun procedure(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PROCEDURE.toString())
                .type(CriteriaType.CPT4.toString())
                .group(false)
                .ancestorData(false)
                .standard(false)
                .conceptId(10L)
    }

    private fun bloodPressure(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.BP.toString())
                .ancestorData(false)
                .standard(false)
                .group(false)
    }

    private fun hrDetail(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.HR_DETAIL.toString())
                .ancestorData(false)
                .standard(false)
                .group(false)
                .conceptId(903126L)
    }

    private fun hr(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.HR.toString())
                .ancestorData(false)
                .standard(false)
                .group(false)
                .conceptId(1586218L)
    }

    private fun height(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.HEIGHT.toString())
                .group(false)
                .conceptId(903133L)
                .ancestorData(false)
                .standard(false)
    }

    private fun weight(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.WEIGHT.toString())
                .group(false)
                .conceptId(903121L)
                .ancestorData(false)
                .standard(false)
    }

    private fun bmi(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.BMI.toString())
                .group(false)
                .conceptId(903124L)
                .ancestorData(false)
                .standard(false)
    }

    private fun waistCircumference(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.WC.toString())
                .group(false)
                .conceptId(903135L)
                .ancestorData(false)
                .standard(false)
    }

    private fun hipCircumference(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.HC.toString())
                .group(false)
                .conceptId(903136L)
                .ancestorData(false)
                .standard(false)
    }

    private fun pregnancy(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.PREG.toString())
                .group(false)
                .conceptId(903120L)
                .ancestorData(false)
                .standard(false)
    }

    private fun wheelchair(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PHYSICAL_MEASUREMENT.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.WHEEL.toString())
                .group(false)
                .conceptId(903111L)
                .ancestorData(false)
                .standard(false)
    }

    private fun age(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.AGE.toString())
                .group(false)
                .ancestorData(false)
                .standard(true)
    }

    private fun male(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.GENDER.toString())
                .group(false)
                .standard(true)
                .ancestorData(false)
                .conceptId(8507L)
    }

    private fun race(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.RACE.toString())
                .group(false)
                .standard(true)
                .ancestorData(false)
                .conceptId(1L)
    }

    private fun ethnicity(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.ETHNICITY.toString())
                .group(false)
                .standard(true)
                .ancestorData(false)
                .conceptId(9898L)
    }

    private fun deceased(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.PERSON.toString())
                .type(CriteriaType.DECEASED.toString())
                .group(false)
                .standard(true)
                .ancestorData(false)
                .value("Deceased")
    }

    private fun survey(): SearchParameter {
        return SearchParameter()
                .domain(DomainType.SURVEY.toString())
                .type(CriteriaType.PPI.toString())
                .subtype(CriteriaSubType.SURVEY.toString())
                .ancestorData(false)
                .standard(false)
                .group(true)
                .conceptId(22L)
    }

    private fun ageModifier(): Modifier {
        return Modifier()
                .name(ModifierType.AGE_AT_EVENT)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList<T>("25"))
    }

    private fun visitModifier(): Modifier {
        return Modifier()
                .name(ModifierType.ENCOUNTERS)
                .operator(Operator.IN)
                .operands(Arrays.asList<T>("1"))
    }

    private fun occurrencesModifier(): Modifier {
        return Modifier()
                .name(ModifierType.NUM_OF_OCCURRENCES)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList<T>("1"))
    }

    private fun eventDateModifier(): Modifier {
        return Modifier()
                .name(ModifierType.EVENT_DATE)
                .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                .operands(Arrays.asList<T>("2009-12-03"))
    }

    private fun wheelchairAttributes(): List<Attribute> {
        return Arrays.asList(
                Attribute()
                        .name(AttrName.CAT)
                        .operator(Operator.IN)
                        .operands(Arrays.asList<T>("4023190")))
    }

    private fun bpAttributes(): List<Attribute> {
        return Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                        .operands(Arrays.asList<T>("90"))
                        .conceptId(903118L),
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.BETWEEN)
                        .operands(Arrays.asList<T>("60", "80"))
                        .conceptId(903115L))
    }

    private fun drugCriteriaParent(): CBCriteria {
        return CBCriteria()
                .parentId(99999)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .conceptId("21600932")
                .group(true)
                .standard(true)
                .selectable(true)
    }

    private fun drugCriteriaChild(parentId: Long): CBCriteria {
        return CBCriteria()
                .parentId(parentId)
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.RXNORM.toString())
                .conceptId("1520218")
                .group(false)
                .selectable(true)
    }

    private fun icd9CriteriaParent(): CBCriteria {
        return CBCriteria()
                .parentId(99999)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .standard(false)
                .code("001")
                .name("Cholera")
                .count("19")
                .group(true)
                .selectable(true)
                .ancestorData(false)
                .conceptId("2")
                .synonyms("[CONDITION_rank1]")
    }

    private fun icd9CriteriaChild(parentId: Long): CBCriteria {
        return CBCriteria()
                .parentId(parentId)
                .domainId(DomainType.CONDITION.toString())
                .type(CriteriaType.ICD9CM.toString())
                .standard(false)
                .code("001.1")
                .name("Cholera")
                .count("19")
                .group(false)
                .selectable(true)
                .ancestorData(false)
                .conceptId("1")
    }

    @Test
    @Throws(Exception::class)
    fun validateAttribute() {
        val demo = age()
        val attribute = Attribute().name(AttrName.NUM)
        demo.attributes(Arrays.asList<T>(attribute))
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(demo), ArrayList<Modifier>())
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: attribute operator null is not valid.", bre.message)
        }

        attribute.operator(Operator.BETWEEN)
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: attribute operands are empty.", bre.message)
        }

        attribute.operands(Arrays.asList<T>("20"))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: attribute NUM can only have 2 operands when using the BETWEEN operator",
                    bre.message)
        }

        attribute.operands(Arrays.asList<T>("s", "20"))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: attribute NUM operands must be numeric.", bre.message)
        }

        attribute.operands(Arrays.asList<T>("10", "20"))
        attribute.operator(Operator.EQUAL)
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: attribute NUM must have one operand when using the EQUAL operator.",
                    bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun validateModifiers() {
        val modifier = ageModifier().operator(null).operands(ArrayList<E>())
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), Arrays.asList<Modifier>(modifier))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: modifier operator null is not valid.", bre.message)
        }

        modifier.operator(Operator.BETWEEN)
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: modifier operands are empty.", bre.message)
        }

        modifier.operands(Arrays.asList<T>("20"))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: modifier AGE_AT_EVENT can only have 2 operands when using the BETWEEN operator",
                    bre.message)
        }

        modifier.operands(Arrays.asList<T>("s", "20"))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: modifier AGE_AT_EVENT operands must be numeric.", bre.message)
        }

        modifier.operands(Arrays.asList<T>("10", "20"))
        modifier.operator(Operator.EQUAL)
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: modifier AGE_AT_EVENT must have one operand when using the EQUAL operator.",
                    bre.message)
        }

        modifier.name(ModifierType.EVENT_DATE)
        modifier.operands(Arrays.asList<T>("10"))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals("Bad Request: modifier EVENT_DATE must be a valid date.", bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionOccurrenceChild() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun temporalGroupExceptions() {
        val icd9SGI = SearchGroupItem().type(DomainType.CONDITION.toString()).addSearchParametersItem(icd9())

        val temporalGroup = SearchGroup().items(Arrays.asList<T>(icd9SGI)).temporal(true)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: search group item temporal group null is not valid.", bre.message)
        }

        icd9SGI.temporalGroup(0)
        try {
            controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest)
            fail("Should have thrown BadRequestException!")
        } catch (bre: BadRequestException) {
            assertEquals(
                    "Bad Request: Search Group Items must provided for 2 different temporal groups(0 or 1).",
                    bre.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun firstMentionOfICD9WithModifiersOrSnomed5DaysAfterICD10WithModifiers() {
        val icd9SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd9())
                .temporalGroup(0)
                .addModifiersItem(ageModifier())
        val icd10SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd10())
                .temporalGroup(1)
                .addModifiersItem(visitModifier())
        val snomedSGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(snomed())
                .temporalGroup(0)

        // First Mention Of (ICD9 w/modifiers or Snomed) 5 Days After ICD10 w/modifiers
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(icd9SGI, snomedSGI, icd10SGI))
                .temporal(true)
                .mention(TemporalMention.FIRST_MENTION)
                .time(TemporalTime.X_DAYS_AFTER)
                .timeValue(5L)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        // matches icd9SGI in group 0 and icd10SGI in group 1
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun firstMentionOfDrug5DaysBeforeICD10WithModifiers() {
        val drugNode1 = drugCriteriaParent()
        saveCriteriaWithPath("0", drugNode1)
        val drugNode2 = drugCriteriaChild(drugNode1.id)
        saveCriteriaWithPath(drugNode1.path, drugNode2)
        insertCriteriaAncestor(1520218, 1520218)
        val drugSGI = SearchGroupItem()
                .type(DomainType.DRUG.toString())
                .addSearchParametersItem(drug().conceptId(21600932L))
                .temporalGroup(0)
        val icd10SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd10())
                .temporalGroup(1)
                .addModifiersItem(visitModifier())

        // First Mention Of Drug 5 Days Before ICD10 w/modifiers
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(drugSGI, icd10SGI))
                .temporal(true)
                .mention(TemporalMention.FIRST_MENTION)
                .time(TemporalTime.X_DAYS_BEFORE)
                .timeValue(5L)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(drugNode1.id)
        cbCriteriaDao.delete(drugNode2.id)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun anyMentionOfCPTParent5DaysAfterICD10Child() {
        val icd9Parent = CBCriteria()
                .ancestorData(false)
                .code("001")
                .conceptId("0")
                .domainId(DomainType.CONDITION.toString())
                .group(true)
                .selectable(true)
                .standard(false)
                .synonyms("+[CONDITION_rank1]")
        saveCriteriaWithPath("0", icd9Parent)
        val icd9Child = CBCriteria()
                .ancestorData(false)
                .code("001.1")
                .conceptId("1")
                .domainId(DomainType.CONDITION.toString())
                .group(false)
                .selectable(true)
                .standard(false)
                .synonyms("+[CONDITION_rank1]")
        saveCriteriaWithPath(icd9Parent.path, icd9Child)

        val icd9SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(
                        icd9().type(CriteriaType.CPT4.toString()).group(true).conceptId(0L))
                .temporalGroup(0)
                .addModifiersItem(visitModifier())
        val icd10SGI = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .addSearchParametersItem(icd10())
                .temporalGroup(1)

        // Any Mention Of ICD9 5 Days After ICD10
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(icd9SGI, icd10SGI))
                .temporal(true)
                .mention(TemporalMention.ANY_MENTION)
                .time(TemporalTime.X_DAYS_AFTER)
                .timeValue(5L)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(icd9Parent.id)
        cbCriteriaDao.delete(icd9Child.id)
    }

    @Test
    @Throws(Exception::class)
    fun anyMentionOfCPTWithIn5DaysOfVisit() {
        val cptSGI = SearchGroupItem()
                .type(DomainType.PROCEDURE.toString())
                .addSearchParametersItem(procedure())
                .temporalGroup(0)
        val visitSGI = SearchGroupItem()
                .type(DomainType.VISIT.toString())
                .addSearchParametersItem(visit())
                .temporalGroup(1)

        // Any Mention Of ICD10 Parent within 5 Days of visit
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(visitSGI, cptSGI))
                .temporal(true)
                .mention(TemporalMention.ANY_MENTION)
                .time(TemporalTime.WITHIN_X_DAYS_OF)
                .timeValue(5L)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun firstMentionOfDrugDuringSameEncounterAsMeasurement() {
        val drugCriteria = drugCriteriaChild(1).conceptId("11")
        saveCriteriaWithPath("0", drugCriteria)
        insertCriteriaAncestor(11, 11)

        val drugSGI = SearchGroupItem()
                .type(DomainType.DRUG.toString())
                .addSearchParametersItem(drug())
                .temporalGroup(0)
        val measurementSGI = SearchGroupItem()
                .type(DomainType.MEASUREMENT.toString())
                .addSearchParametersItem(measurement())
                .temporalGroup(1)

        // First Mention Of Drug during same encounter as measurement
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(drugSGI, measurementSGI))
                .temporal(true)
                .mention(TemporalMention.FIRST_MENTION)
                .time(TemporalTime.DURING_SAME_ENCOUNTER_AS)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(drugCriteria.id)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun lastMentionOfDrugDuringSameEncounterAsMeasurement() {
        val drugCriteria = drugCriteriaChild(1).conceptId("11")
        saveCriteriaWithPath("0", drugCriteria)
        insertCriteriaAncestor(11, 11)

        val drugSGI = SearchGroupItem()
                .type(DomainType.DRUG.toString())
                .addSearchParametersItem(drug())
                .temporalGroup(0)
        val measurementSGI = SearchGroupItem()
                .type(DomainType.MEASUREMENT.toString())
                .addSearchParametersItem(measurement())
                .temporalGroup(1)

        // Last Mention Of Drug during same encounter as measurement
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(drugSGI, measurementSGI))
                .temporal(true)
                .mention(TemporalMention.LAST_MENTION)
                .time(TemporalTime.DURING_SAME_ENCOUNTER_AS)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(drugCriteria.id)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun lastMentionOfDrugDuringSameEncounterAsMeasurementOrVisit() {
        val drugCriteria = CBCriteria()
                .domainId(DomainType.DRUG.toString())
                .type(CriteriaType.ATC.toString())
                .group(false)
                .ancestorData(true)
                .standard(true)
                .conceptId("11")
        saveCriteriaWithPath("0", drugCriteria)
        insertCriteriaAncestor(11, 11)

        val drugSGI = SearchGroupItem()
                .type(DomainType.DRUG.toString())
                .addSearchParametersItem(drug())
                .temporalGroup(0)
        val measurementSGI = SearchGroupItem()
                .type(DomainType.MEASUREMENT.toString())
                .addSearchParametersItem(measurement())
                .temporalGroup(1)
        val visitSGI = SearchGroupItem()
                .type(DomainType.VISIT.toString())
                .addSearchParametersItem(visit())
                .temporalGroup(1)

        // Last Mention Of Drug during same encounter as measurement
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(drugSGI, measurementSGI, visitSGI))
                .temporal(true)
                .mention(TemporalMention.LAST_MENTION)
                .time(TemporalTime.DURING_SAME_ENCOUNTER_AS)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(drugCriteria.id)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun lastMentionOfMeasurementOrVisit5DaysAfterDrug() {
        val drugCriteria1 = drugCriteriaParent()
        saveCriteriaWithPath("0", drugCriteria1)
        val drugCriteria2 = drugCriteriaChild(drugCriteria1.id)
        saveCriteriaWithPath(drugCriteria1.path, drugCriteria2)
        insertCriteriaAncestor(1520218, 1520218)

        val measurementSGI = SearchGroupItem()
                .type(DomainType.MEASUREMENT.toString())
                .addSearchParametersItem(measurement())
                .temporalGroup(0)
        val visitSGI = SearchGroupItem()
                .type(DomainType.VISIT.toString())
                .addSearchParametersItem(visit())
                .temporalGroup(0)
        val drugSGI = SearchGroupItem()
                .type(DomainType.DRUG.toString())
                .addSearchParametersItem(drug().group(true).conceptId(21600932L))
                .temporalGroup(1)

        // Last Mention Of Measurement or Visit 5 days after Drug
        val temporalGroup = SearchGroup()
                .items(Arrays.asList<T>(drugSGI, measurementSGI, visitSGI))
                .temporal(true)
                .mention(TemporalMention.LAST_MENTION)
                .time(TemporalTime.X_DAYS_AFTER)
                .timeValue(5L)

        val searchRequest = SearchRequest().includes(Arrays.asList<T>(temporalGroup))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(drugCriteria1.id)
        cbCriteriaDao.delete(drugCriteria2.id)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionChildAgeAtEvent() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), Arrays.asList<Modifier>(ageModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionChildEncounter() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), Arrays.asList<Modifier>(visitModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionChildAgeAtEventBetween() {
        val modifier = ageModifier().operator(Operator.BETWEEN).operands(Arrays.asList<T>("37", "39"))

        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), Arrays.asList<Modifier>(modifier))

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionOccurrenceChildAgeAtEventAndOccurrences() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList<SearchParameter>(icd9()),
                Arrays.asList(ageModifier(), occurrencesModifier().operands(Arrays.asList<T>("2"))))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditioChildAgeAtEventAndOccurrencesAndEventDate() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList(icd9(), icd9().conceptId(2L)),
                Arrays.asList<Modifier>(ageModifier(), occurrencesModifier(), eventDateModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionChildEventDate() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList<SearchParameter>(icd9()),
                Arrays.asList<Modifier>(eventDateModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionNumOfOccurrences() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList<SearchParameter>(icd9()),
                Arrays.asList<Modifier>(occurrencesModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9Child() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(icd9()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ConditionParent() {
        val criteriaParent = icd9CriteriaParent()
        saveCriteriaWithPath("0", criteriaParent)
        val criteriaChild = icd9CriteriaChild(criteriaParent.id)
        saveCriteriaWithPath(criteriaParent.path, criteriaChild)

        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList(icd9().group(true).conceptId(2L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(criteriaParent.id)
        cbCriteriaDao.delete(criteriaChild.id)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoGender() {
        val searchRequest = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(male()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoRace() {
        val searchRequest = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(race()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoEthnicity() {
        val searchRequest = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(ethnicity()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoDec() {
        val searchRequest = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(deceased()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoAge() {
        val lo = testPeriod.years - 1
        val hi = testPeriod.years + 1
        val demo = age()
        demo.attributes(
                Arrays.asList(
                        Attribute()
                                .name(AttrName.AGE)
                                .operator(Operator.BETWEEN)
                                .operands(Arrays.asList<T>(lo.toString(), hi.toString()))))
        val searchRequests = createSearchRequests(DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(demo), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequests), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9AndDemo() {
        val demoAgeSearchParam = age()
        val lo = testPeriod.years - 1
        val hi = testPeriod.years + 1

        demoAgeSearchParam.attributes(
                Arrays.asList(
                        Attribute()
                                .operator(Operator.BETWEEN)
                                .operands(Arrays.asList<T>(lo.toString(), hi.toString()))))

        val searchRequests = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(male()), ArrayList<Modifier>())

        val anotherSearchGroupItem = SearchGroupItem()
                .type(DomainType.CONDITION.toString())
                .searchParameters(Arrays.asList(icd9().conceptId(3L)))
                .modifiers(ArrayList<E>())

        val anotherNewSearchGroupItem = SearchGroupItem()
                .type(DomainType.PERSON.toString())
                .searchParameters(Arrays.asList<T>(demoAgeSearchParam))
                .modifiers(ArrayList<E>())

        searchRequests.getIncludes().get(0).addItemsItem(anotherSearchGroupItem)
        searchRequests.getIncludes().get(0).addItemsItem(anotherNewSearchGroupItem)

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequests), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDemoExcluded() {
        val excludeSearchGroupItem = SearchGroupItem()
                .type(DomainType.PERSON.toString())
                .searchParameters(Arrays.asList<T>(male()))
        val excludeSearchGroup = SearchGroup().addItemsItem(excludeSearchGroupItem)

        val searchRequests = createSearchRequests(
                DomainType.PERSON.toString(), Arrays.asList<SearchParameter>(male()), ArrayList<Modifier>())
        searchRequests.getExcludes().add(excludeSearchGroup)

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequests), 0)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsICD9ParentAndICD10ChildCondition() {
        val criteriaParent = icd9CriteriaParent()
        saveCriteriaWithPath("0", criteriaParent)
        val criteriaChild = icd9CriteriaChild(criteriaParent.id)
        saveCriteriaWithPath(criteriaParent.path, criteriaChild)

        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList(icd9().group(true).conceptId(2L), icd10().conceptId(6L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
        cbCriteriaDao!!.delete(criteriaParent.id)
        cbCriteriaDao.delete(criteriaChild.id)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsCPTProcedure() {
        val searchRequest = createSearchRequests(
                DomainType.PROCEDURE.toString(),
                Arrays.asList(procedure().conceptId(4L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsSnomedChildCondition() {
        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(),
                Arrays.asList(snomed().standard(true).conceptId(6L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsSnomedParentProcedure() {
        val snomed = snomed().group(true).standard(true).conceptId(4302541L)

        val criteriaParent = CBCriteria()
                .parentId(99999)
                .domainId(DomainType.PROCEDURE.toString())
                .type(CriteriaType.SNOMED.toString())
                .standard(true)
                .code("386637004")
                .name("Obstetric procedure")
                .count("36673")
                .group(true)
                .selectable(true)
                .ancestorData(false)
                .conceptId("4302541")
                .synonyms("+[PROCEDURE_rank1]")
        saveCriteriaWithPath("0", criteriaParent)
        val criteriaChild = CBCriteria()
                .parentId(criteriaParent.id)
                .domainId(DomainType.PROCEDURE.toString())
                .type(CriteriaType.SNOMED.toString())
                .standard(true)
                .code("386639001")
                .name("Termination of pregnancy")
                .count("50")
                .group(false)
                .selectable(true)
                .ancestorData(false)
                .conceptId("4")
                .synonyms("+[PROCEDURE_rank1]")
        saveCriteriaWithPath(criteriaParent.path, criteriaChild)

        val searchRequest = createSearchRequests(
                DomainType.CONDITION.toString(), Arrays.asList<SearchParameter>(snomed), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        cbCriteriaDao!!.delete(criteriaParent.id)
        cbCriteriaDao.delete(criteriaChild.id)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsVisit() {
        val searchRequest = createSearchRequests(
                DomainType.VISIT.toString(), Arrays.asList(visit().conceptId(10L)), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsVisitModifiers() {
        val searchRequest = createSearchRequests(
                DomainType.VISIT.toString(),
                Arrays.asList(visit().conceptId(10L)),
                Arrays.asList<Modifier>(occurrencesModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDrugChild() {
        insertCriteriaAncestor(11, 11)
        val searchRequest = createSearchRequests(DomainType.DRUG.toString(), Arrays.asList<SearchParameter>(drug()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDrugParent() {
        val drugNode1 = drugCriteriaParent()
        saveCriteriaWithPath("0", drugNode1)
        val drugNode2 = drugCriteriaChild(drugNode1.id)
        saveCriteriaWithPath(drugNode1.path, drugNode2)

        insertCriteriaAncestor(1520218, 1520218)
        val searchRequest = createSearchRequests(
                DomainType.DRUG.toString(),
                Arrays.asList(drug().group(true).conceptId(21600932L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
        cbCriteriaDao!!.delete(drugNode1.id)
        cbCriteriaDao.delete(drugNode2.id)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDrugParentAndChild() {
        val drugNode1 = drugCriteriaParent()
        saveCriteriaWithPath("0", drugNode1)
        val drugNode2 = drugCriteriaChild(drugNode1.id)
        saveCriteriaWithPath(drugNode1.path, drugNode2)

        insertCriteriaAncestor(1520218, 1520218)
        val searchRequest = createSearchRequests(
                DomainType.DRUG.toString(),
                Arrays.asList(drug().group(true).conceptId(21600932L), drug()),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
        cbCriteriaDao!!.delete(drugNode1.id)
        cbCriteriaDao.delete(drugNode2.id)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDrugChildEncounter() {
        insertCriteriaAncestor(11, 11)
        val searchRequest = createSearchRequests(
                DomainType.DRUG.toString(), Arrays.asList<SearchParameter>(drug()), Arrays.asList<Modifier>(visitModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsDrugChildAgeAtEvent() {
        insertCriteriaAncestor(11, 11)
        val searchRequest = createSearchRequests(
                DomainType.DRUG.toString(), Arrays.asList<SearchParameter>(drug()), Arrays.asList<Modifier>(ageModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
        jdbcTemplate!!.execute("drop table cb_criteria_ancestor")
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabEncounter() {
        val searchRequest = createSearchRequests(
                DomainType.MEASUREMENT.toString(),
                Arrays.asList<SearchParameter>(measurement()),
                Arrays.asList<Modifier>(visitModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabNumericalBetween() {
        val lab = measurement()
        lab.attributes(
                Arrays.asList(
                        Attribute()
                                .name(AttrName.NUM)
                                .operator(Operator.BETWEEN)
                                .operands(Arrays.asList<T>("0", "1"))))
        val searchRequest = createSearchRequests(
                DomainType.MEASUREMENT.toString(), Arrays.asList<SearchParameter>(lab), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabCategoricalIn() {
        val lab = measurement()
        lab.attributes(
                Arrays.asList(
                        Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList<T>("1"))))
        val searchRequest = createSearchRequests(lab.getType(), Arrays.asList<SearchParameter>(lab), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabBothNumericalAndCategorical() {
        val lab = measurement()
        val numerical = Attribute().name(AttrName.NUM).operator(Operator.EQUAL).operands(Arrays.asList<T>("0.1"))
        val categorical = Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList<T>("1", "2"))
        lab.attributes(Arrays.asList<T>(numerical, categorical))
        val searchRequest = createSearchRequests(lab.getType(), Arrays.asList<SearchParameter>(lab), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabCategoricalAgeAtEvent() {
        val searchRequest = createSearchRequests(
                DomainType.MEASUREMENT.toString(),
                Arrays.asList<SearchParameter>(measurement()),
                Arrays.asList<Modifier>(ageModifier()))
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabMoreThanOneSearchParameter() {
        val lab1 = measurement()
        val lab2 = measurement().conceptId(9L)
        val lab3 = measurement().conceptId(9L)
        val labCategorical = Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList<T>("77"))
        lab3.attributes(Arrays.asList<T>(labCategorical))
        val searchRequest = createSearchRequests(lab1.getDomain(), Arrays.asList<SearchParameter>(lab1, lab2, lab3), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsLabMoreThanOneSearchParameterSourceAndStandard() {
        val icd9 = icd9()
        val snomed = snomed().standard(true)
        val searchRequest = createSearchRequests(icd9.getDomain(), Arrays.asList<SearchParameter>(icd9, snomed), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsBloodPressure() {
        val pm = bloodPressure().attributes(bpAttributes())
        val searchRequest = createSearchRequests(pm.getType(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsBloodPressureAny() {
        val attributes = Arrays.asList(
                Attribute().name(AttrName.ANY).operands(ArrayList<E>()).conceptId(903118L),
                Attribute().name(AttrName.ANY).operands(ArrayList<E>()).conceptId(903115L))
        val pm = bloodPressure().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsBloodPressureOrHeartRateDetail() {
        val bpPm = bloodPressure().attributes(bpAttributes())

        val hrAttributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.EQUAL)
                        .operands(Arrays.asList<T>("71")))
        val hrPm = hrDetail().attributes(hrAttributes)

        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(),
                Arrays.asList<SearchParameter>(bpPm, hrPm),
                ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsBloodPressureOrHeartRateDetailOrHeartRateIrr() {
        val bpPm = bloodPressure().attributes(bpAttributes())
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(bpPm), ArrayList<Modifier>())

        val hrAttributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.EQUAL)
                        .operands(Arrays.asList<T>("71")))
        val hrPm = hrDetail().attributes(hrAttributes)
        val anotherSearchGroupItem = SearchGroupItem()
                .type(DomainType.PHYSICAL_MEASUREMENT.toString())
                .searchParameters(Arrays.asList<T>(hrPm))
                .modifiers(ArrayList<E>())

        searchRequest.getIncludes().get(0).addItemsItem(anotherSearchGroupItem)

        val irrAttributes = Arrays.asList(
                Attribute()
                        .name(AttrName.CAT)
                        .operator(Operator.IN)
                        .operands(Arrays.asList<T>("4262985")))
        val hrIrrPm = hr().attributes(irrAttributes)
        val heartRateIrrSearchGroupItem = SearchGroupItem()
                .type(DomainType.PHYSICAL_MEASUREMENT.toString())
                .searchParameters(Arrays.asList<T>(hrIrrPm))
                .modifiers(ArrayList<E>())

        searchRequest.getIncludes().get(0).addItemsItem(heartRateIrrSearchGroupItem)

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 3)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsHeartRateAny() {
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(),
                Arrays.asList(hrDetail().conceptId(1586218L)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsHeartRate() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.GREATER_THAN_OR_EQUAL_TO)
                        .operands(Arrays.asList<T>("45")))
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(),
                Arrays.asList(hrDetail().conceptId(1586218L).attributes(attributes)),
                ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsHeight() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                        .operands(Arrays.asList<T>("168")))
        val pm = height().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsWeight() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                        .operands(Arrays.asList<T>("201")))
        val pm = weight().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsBMI() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.LESS_THAN_OR_EQUAL_TO)
                        .operands(Arrays.asList<T>("263")))
        val pm = bmi().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectWaistCircumferenceAndHipCircumference() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.EQUAL)
                        .operands(Arrays.asList<T>("31")))
        val pm = waistCircumference().attributes(attributes)
        val pm1 = hipCircumference().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm, pm1), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectPregnant() {
        val attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.CAT)
                        .operator(Operator.IN)
                        .operands(Arrays.asList<T>("45877994")))
        val pm = pregnancy().attributes(attributes)
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectWheelChairUser() {
        val pm = wheelchair().attributes(wheelchairAttributes())
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 2)
    }

    @Test
    @Throws(Exception::class)
    fun countSubjectsPPI() {
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
                .conceptId("1585899")
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
        saveCriteriaWithPath(questionNode.path, answerNode)

        // Survey
        var searchRequest = createSearchRequests(
                DomainType.SURVEY.toString(), Arrays.asList<SearchParameter>(survey()), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)

        // Question
        val ppiQuestion = survey().subtype(CriteriaSubType.QUESTION.toString()).conceptId(1585899L)
        searchRequest = createSearchRequests(ppiQuestion.getType(), Arrays.asList<SearchParameter>(ppiQuestion), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)

        // value source concept id
        var attributes: List<Attribute> = Arrays.asList(
                Attribute().name(AttrName.CAT).operator(Operator.IN).operands(Arrays.asList<T>("7")))
        val ppiValueAsConceptId = survey().subtype(CriteriaSubType.ANSWER.toString()).conceptId(5L).attributes(attributes)
        searchRequest = createSearchRequests(
                ppiValueAsConceptId.getType(), Arrays.asList<SearchParameter>(ppiValueAsConceptId), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)

        // value as number
        attributes = Arrays.asList(
                Attribute()
                        .name(AttrName.NUM)
                        .operator(Operator.EQUAL)
                        .operands(Arrays.asList<T>("7")))
        val ppiValueAsNumer = survey().subtype(CriteriaSubType.ANSWER.toString()).conceptId(5L).attributes(attributes)
        searchRequest = createSearchRequests(
                ppiValueAsNumer.getType(), Arrays.asList<SearchParameter>(ppiValueAsNumer), ArrayList<Modifier>())
        assertParticipants(
                controller!!.countParticipants(cdrVersion!!.cdrVersionId, searchRequest), 1)

        cbCriteriaDao!!.delete(surveyNode.id)
        cbCriteriaDao.delete(questionNode.id)
        cbCriteriaDao.delete(answerNode.id)
    }

    @Test
    @Throws(Exception::class)
    fun getDemoChartInfo() {
        val pm = wheelchair().attributes(wheelchairAttributes())
        val searchRequest = createSearchRequests(
                DomainType.PHYSICAL_MEASUREMENT.toString(), Arrays.asList<SearchParameter>(pm), ArrayList<Modifier>())

        val response = controller!!.getDemoChartInfo(cdrVersion!!.cdrVersionId, searchRequest).body
        assertEquals(2, response.getItems().size())
        assertEquals(
                DemoChartInfo().gender("MALE").race("Asian").ageRange("45-64").count(1L),
                response.getItems().get(0))
        assertEquals(
                DemoChartInfo().gender("MALE").race("Caucasian").ageRange("19-44").count(1L),
                response.getItems().get(1))
    }

    @Test
    @Throws(Exception::class)
    fun filterBigQueryConfig_WithoutTableName() {
        val statement = "my statement \${projectId}.\${dataSetId}.myTableName"
        val queryJobConfiguration = QueryJobConfiguration.newBuilder(statement).setUseLegacySql(false).build()
        val expectedResult = "my statement $tablePrefix.myTableName"
        assertThat(expectedResult)
                .isEqualTo(bigQueryService!!.filterBigQueryConfig(queryJobConfiguration).query)
    }

    private fun insertCriteriaAncestor(ancestorId: Int, descendentId: Int) {
        jdbcTemplate!!.execute(
                "create table cb_criteria_ancestor(ancestor_id bigint, descendant_id bigint)")
        jdbcTemplate.execute(
                "insert into cb_criteria_ancestor(ancestor_id, descendant_id) values ("
                        + ancestorId
                        + ", "
                        + descendentId
                        + ")")
    }

    private fun createSearchRequests(
            type: String, parameters: List<SearchParameter>, modifiers: List<Modifier>): SearchRequest {
        val searchGroupItem = SearchGroupItem().type(type).searchParameters(parameters).modifiers(modifiers)

        val searchGroup = SearchGroup().addItemsItem(searchGroupItem)

        val groups = ArrayList<SearchGroup>()
        groups.add(searchGroup)
        return SearchRequest().includes(groups)
    }

    private fun assertParticipants(response: ResponseEntity<*>, expectedCount: Int?) {
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

        val participantCount = response.body as Long
        assertThat(participantCount).isEqualTo(expectedCount)
    }

    private fun saveCriteriaWithPath(path: String, criteria: CBCriteria) {
        cbCriteriaDao!!.save(criteria)
        val pathEnd = criteria.id.toString()
        criteria.path(if (path.isEmpty()) pathEnd else "$path.$pathEnd")
        cbCriteriaDao.save(criteria)
    }
}
