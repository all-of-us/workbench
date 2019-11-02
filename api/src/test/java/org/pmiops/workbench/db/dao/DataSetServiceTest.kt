package org.pmiops.workbench.db.dao

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.pmiops.workbench.db.model.CommonStorageEnums.domainToStorage

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import java.util.Collections
import java.util.Optional
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.cdr.ConceptBigQueryService
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.db.dao.DataSetServiceImpl.QueryAndParameters
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.DataSetRequest
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.model.SearchRequest
import org.pmiops.workbench.test.SearchRequests
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
class DataSetServiceTest {

    @Autowired
    private val bigQueryService: BigQueryService? = null
    @Autowired
    private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService? = null
    @Autowired
    private val cohortDao: CohortDao? = null
    @Autowired
    private val conceptSetDao: ConceptSetDao? = null
    @Autowired
    private val conceptBigQueryService: ConceptBigQueryService? = null
    @Autowired
    private val cohortQueryBuilder: CohortQueryBuilder? = null
    @Autowired
    private val dataSetDao: DataSetDao? = null

    private var dataSetServiceImpl: DataSetServiceImpl? = null

    @TestConfiguration
    @MockBean(BigQueryService::class, CdrBigQuerySchemaConfigService::class, CohortDao::class, ConceptBigQueryService::class, ConceptSetDao::class, CohortQueryBuilder::class, DataSetDao::class)
    internal class Configuration

    @Before
    fun setUp() {
        dataSetServiceImpl = DataSetServiceImpl(
                bigQueryService,
                cdrBigQuerySchemaConfigService,
                cohortDao,
                conceptBigQueryService,
                conceptSetDao,
                cohortQueryBuilder,
                dataSetDao)

        val cohort = buildSimpleCohort()
        `when`(cohortDao!!.findCohortByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(cohort)
        `when`(cohortQueryBuilder!!.buildParticipantIdQuery(any<ParticipantCriteria>()))
                .thenReturn(
                        QueryJobConfiguration.newBuilder(
                                "SELECT * FROM person_id from `\${projectId}.\${dataSetId}.person` person")
                                .build())
        `when`(bigQueryService!!.filterBigQueryConfig(any(QueryJobConfiguration::class.java)))
                .thenReturn(QUERY_JOB_CONFIGURATION_1)
    }

    private fun buildSimpleCohort(): Cohort {
        val searchRequest = SearchRequests.males()
        val cohortCriteria = Gson().toJson(searchRequest)

        val cohortDbModel = Cohort()
        cohortDbModel.cohortId = 101L
        cohortDbModel.type = "foo"
        cohortDbModel.workspaceId = 1L
        cohortDbModel.criteria = cohortCriteria
        return cohortDbModel
    }

    private fun buildConceptSet(conceptSetId: Long, domain: Domain, conceptIds: Set<Long>): ConceptSet {
        val result = ConceptSet()
        result.conceptSetId = conceptSetId
        result.domain = domainToStorage(domain)
        result.conceptIds = conceptIds
        return result
    }

    @Test(expected = BadRequestException::class)
    fun testThrowsForNoCohortOrConcept() {
        val invalidRequest = buildEmptyRequest()
        dataSetServiceImpl!!.generateQueryJobConfigurationsByDomainName(invalidRequest)
    }

    @Test
    fun testGetsCohortQueryStringAndCollectsNamedParameters() {
        val cohortDbModel = buildSimpleCohort()
        val queryAndParameters = dataSetServiceImpl!!.getCohortQueryStringAndCollectNamedParameters(cohortDbModel)
        assertThat(queryAndParameters.query).isNotEmpty()
        assertThat(queryAndParameters.namedParameterValues).isNotEmpty()
    }

    @Test
    fun testRejectsConceptSetListWithNoConcepts() {
        val conceptSet1 = ConceptSet()
        conceptSet1.domain = Domain.DEVICE.ordinal()
        conceptSet1.conceptIds = emptySet()
        val isValid = dataSetServiceImpl!!.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
                ImmutableList.of(conceptSet1))
        assertThat(isValid).isFalse()
    }

    @Test
    fun testAcceptsTwoDomainsWithConcepts() {
        val conceptSet1 = ConceptSet()
        conceptSet1.domain = Domain.DEVICE.ordinal()
        conceptSet1.conceptIds = ImmutableSet.of(1L, 2L, 3L)

        val conceptSet2 = ConceptSet()
        conceptSet2.domain = Domain.PERSON.ordinal()
        conceptSet2.conceptIds = ImmutableSet.of(4L, 5L, 6L)

        val isValid = dataSetServiceImpl!!.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
                ImmutableList.of(conceptSet1, conceptSet2))
        assertThat(isValid).isTrue()
    }

    @Test
    fun testRejectsSomeDomainsWithConceptsSomeWithout() {
        val conceptSet1 = ConceptSet()
        conceptSet1.domain = Domain.DEVICE.ordinal()
        conceptSet1.conceptIds = ImmutableSet.of(1L, 2L, 3L)

        val conceptSet2 = ConceptSet()
        conceptSet2.domain = Domain.PERSON.ordinal()
        conceptSet2.conceptIds = ImmutableSet.of(4L, 5L, 6L)

        val conceptSet3 = ConceptSet()
        conceptSet3.domain = Domain.DRUG.ordinal()
        conceptSet3.conceptIds = emptySet()

        val isValid = dataSetServiceImpl!!.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
                ImmutableList.of(conceptSet1, conceptSet2, conceptSet3))
        assertThat(isValid).isFalse()
    }

    @Test
    fun testAcceptsEmptyConceptSetIfDomainIsPopulated() {
        val conceptSet1 = ConceptSet()
        conceptSet1.domain = Domain.DEVICE.ordinal()
        conceptSet1.conceptIds = ImmutableSet.of(1L, 2L, 3L)

        val conceptSet2 = ConceptSet()
        conceptSet2.domain = Domain.PERSON.ordinal()
        conceptSet2.conceptIds = ImmutableSet.of(4L, 5L, 6L)

        val conceptSet3 = ConceptSet()
        conceptSet3.domain = Domain.DEVICE.ordinal()
        conceptSet3.conceptIds = emptySet()

        val isValid = dataSetServiceImpl!!.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
                ImmutableList.of(conceptSet1, conceptSet2, conceptSet3))
        assertThat(isValid).isTrue()
    }

    @Test
    fun testRejectsEmptyConceptSetList() {
        val isValid = dataSetServiceImpl!!.conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
                emptyList())
        assertThat(isValid).isFalse()
    }

    @Test
    fun testBuildConceptIdListClause_same() {
        val domain1 = Domain.CONDITION
        val conceptSet1 = buildConceptSet(1L, domain1, ImmutableSet.of(1L, 2L, 3L))
        val conceptSet2 = buildConceptSet(2L, domain1, ImmutableSet.of(4L, 5L, 6L))
        val listClauseMaybe = dataSetServiceImpl!!.buildConceptIdListClause(
                domain1, ImmutableList.of(conceptSet1, conceptSet2))
        assertThat(listClauseMaybe.isPresent).isTrue()
        assertThat(listClauseMaybe.get() == "1 2 3 4 5 6")
    }

    @Test
    fun testBuildConceptIdListClause_differentDomains() {
        val conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L))
        val conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L))
        val listClauseMaybe = dataSetServiceImpl!!.buildConceptIdListClause(
                Domain.CONDITION, ImmutableList.of(conceptSet1, conceptSet2))
        assertThat(listClauseMaybe.isPresent).isTrue()
        assertThat(listClauseMaybe.get() == "1 2 3")
    }

    @Test
    fun testBuildConceptIdListClause_noClauseForPersonDomain() {
        val conceptSet1 = buildConceptSet(1L, Domain.CONDITION, ImmutableSet.of(1L, 2L, 3L))
        val conceptSet2 = buildConceptSet(2L, Domain.DRUG, ImmutableSet.of(4L, 5L, 6L))
        val listClauseMaybe = dataSetServiceImpl!!.buildConceptIdListClause(
                Domain.PERSON, ImmutableList.of(conceptSet1, conceptSet2))
        assertThat(listClauseMaybe.isPresent).isFalse()
    }

    @Test
    fun testcapitalizeFirstCharacterOnly_uppercaseWord() {
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("QWERTY")).isEqualTo("Qwerty")
    }

    @Test
    fun testcapitalizeFirstCharacterOnly_mixedCaseString() {
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("aLl YouR baSE"))
                .isEqualTo("All your base")
    }

    @Test
    fun testcapitalizeFirstCharacterOnly_singleLetterStrings() {
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("a")).isEqualTo("A")
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("B")).isEqualTo("B")
    }

    @Test
    fun testcapitalizeFirstCharacterOnly_emptyString() {
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("")).isEqualTo("")
    }

    @Test
    fun testCapitalizeFirstCharacterOnly_emoji() {
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("\uD83D\uDCAF"))
                .isEqualTo("\uD83D\uDCAF")
        assertThat(DataSetServiceImpl.capitalizeFirstCharacterOnly("マリオに感謝しますが、私たちの王女は別の城にいます"))
                .isEqualTo("マリオに感謝しますが、私たちの王女は別の城にいます")
    }

    companion object {
        private val QUERY_JOB_CONFIGURATION_1 = QueryJobConfiguration.newBuilder(
                "SELECT * FROM person_id from `\${projectId}.\${dataSetId}.person` person")
                .addNamedParameter(
                        "foo",
                        QueryParameterValue.newBuilder()
                                .setType(StandardSQLTypeName.INT64)
                                .setValue(java.lang.Long.toString(101L))
                                .build())
                .build()

        private fun buildEmptyRequest(): DataSetRequest {
            val invalidRequest = DataSetRequest()
            invalidRequest.setDomainValuePairs(emptyList<T>())
            return invalidRequest
        }
    }
}
