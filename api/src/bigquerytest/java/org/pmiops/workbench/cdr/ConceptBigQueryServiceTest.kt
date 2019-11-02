package org.pmiops.workbench.cdr

import com.google.common.truth.Truth.assertThat

import com.google.common.collect.ImmutableSet
import java.util.Arrays
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.api.BigQueryBaseTest
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.api.BigQueryTestService
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.cdr.dao.ConceptService
import org.pmiops.workbench.cdr.model.Concept
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig
import org.pmiops.workbench.testconfig.TestJpaConfig
import org.pmiops.workbench.testconfig.TestWorkbenchConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@RunWith(BeforeAfterSpringTestRunner::class)
@Import(BigQueryTestService::class, TestBigQueryCdrSchemaConfig::class, TestJpaConfig::class, CdrBigQuerySchemaConfigService::class)
class ConceptBigQueryServiceTest : BigQueryBaseTest() {

    @Autowired
    private val testWorkbenchConfig: TestWorkbenchConfig? = null

    @PersistenceContext
    private val entityManager: EntityManager? = null

    @Autowired
    private val conceptDao: ConceptDao? = null

    @Autowired
    private val bigQueryService: BigQueryService? = null

    @Autowired
    private val cdrBigQuerySchemaConfigService: CdrBigQuerySchemaConfigService? = null

    private var conceptBigQueryService: ConceptBigQueryService? = null

    override val tableNames: List<String>
        get() = Arrays.asList("condition_occurrence")

    override val testDataDirectory: String
        get() = BigQueryBaseTest.MATERIALIZED_DATA

    @Before
    fun setUp() {
        val cdrVersion = CdrVersion()
        cdrVersion.bigqueryDataset = testWorkbenchConfig!!.bigquery!!.dataSetId
        cdrVersion.bigqueryProject = testWorkbenchConfig.bigquery!!.projectId
        CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion)

        val conceptService = ConceptService(entityManager, conceptDao)
        conceptBigQueryService = ConceptBigQueryService(bigQueryService, cdrBigQuerySchemaConfigService, conceptService)

        conceptDao!!.deleteAll()
    }

    @Test
    fun testGetConceptCountNoConceptsSaved() {
        assertThat(
                conceptBigQueryService!!.getParticipantCountForConcepts(
                        "condition_occurrence", ImmutableSet.of(1L, 6L, 13L, 192819L)))
                .isEqualTo(0)
    }

    @Test
    fun testGetConceptCountConceptsSaved() {
        saveConcept(1L, "S")
        saveConcept(6L, null)
        saveConcept(13L, null)
        saveConcept(192819L, "C")

        assertThat(
                conceptBigQueryService!!.getParticipantCountForConcepts(
                        "condition_occurrence", ImmutableSet.of(1L, 6L, 13L, 192819L)))
                .isEqualTo(2)
    }

    private fun saveConcept(conceptId: Long, standardConceptValue: String?) {
        val concept = Concept()
        concept.conceptId = conceptId
        concept.standardConcept = standardConceptValue
        concept.conceptCode = "concept$conceptId"
        concept.conceptName = "concept $conceptId"
        concept.vocabularyId = "V"
        concept.domainId = "D"
        conceptDao!!.save(concept)
    }
}
