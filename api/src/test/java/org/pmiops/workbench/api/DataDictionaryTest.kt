package org.pmiops.workbench.api

import org.assertj.core.api.Assertions.assertThat

import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.pmiops.workbench.cdr.dao.ConceptDao
import org.pmiops.workbench.dataset.DataSetMapper
import org.pmiops.workbench.dataset.DataSetMapperImpl
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao
import org.pmiops.workbench.db.dao.DataSetDao
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.DataDictionaryEntry
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.Domain
import org.pmiops.workbench.notebooks.NotebooksService
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
class DataDictionaryTest {

    @Autowired
    internal var bigQueryService: BigQueryService? = null
    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var conceptDao: ConceptDao? = null
    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null
    @Autowired
    internal var dataDictionaryEntryDao: DataDictionaryEntryDao? = null
    @Autowired
    internal var dataSetDao: DataSetDao? = null
    @Autowired
    internal var dataSetMapper: DataSetMapper? = null
    @Autowired
    internal var dataSetService: DataSetService? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null
    @Autowired
    internal var notebooksService: NotebooksService? = null
    @Autowired
    internal var workspaceService: WorkspaceService? = null

    @Autowired
    internal var dataSetController: DataSetController? = null

    @Rule
    var expectedEx = ExpectedException.none()

    @TestConfiguration
    @Import(DataSetController::class, DataSetMapperImpl::class)
    @MockBean(BigQueryService::class, CohortDao::class, ConceptDao::class, ConceptSetDao::class, DataSetDao::class, DataSetService::class, FireCloudService::class, NotebooksService::class, WorkspaceService::class)
    internal class Configuration {
        @Bean
        fun clock(): Clock {
            return CLOCK
        }
    }

    @Before
    fun setUp() {
        val cdrVersion = CdrVersion()
        cdrVersionDao!!.save(cdrVersion)

        val dataDictionaryEntry = DataDictionaryEntry()
        dataDictionaryEntry.cdrVersion = cdrVersion
        dataDictionaryEntry.definedTime = Timestamp(CLOCK.millis())
        dataDictionaryEntry.relevantOmopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME[Domain.DRUG]
        dataDictionaryEntry.fieldName = "TEST FIELD"
        dataDictionaryEntry.omopCdmStandardOrCustomField = "A"
        dataDictionaryEntry.description = "B"
        dataDictionaryEntry.fieldType = "C"
        dataDictionaryEntry.dataProvenance = "D"
        dataDictionaryEntry.sourcePpiModule = "E"
        dataDictionaryEntry.transformedByRegisteredTierPrivacyMethods = true

        dataDictionaryEntryDao!!.save(dataDictionaryEntry)
    }

    @Test
    fun testGetDataDictionaryEntry() {
        val domain = Domain.DRUG
        val domainValue = "FIELD NAME / DOMAIN VALUE"

        val cdrVersion = CdrVersion()
        cdrVersionDao!!.save(cdrVersion)

        val dataDictionaryEntry = DataDictionaryEntry()
        dataDictionaryEntry.cdrVersion = cdrVersion
        dataDictionaryEntry.definedTime = Timestamp(CLOCK.millis())
        dataDictionaryEntry.relevantOmopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME[domain]
        dataDictionaryEntry.fieldName = domainValue
        dataDictionaryEntry.omopCdmStandardOrCustomField = "A"
        dataDictionaryEntry.description = "B"
        dataDictionaryEntry.fieldType = "C"
        dataDictionaryEntry.dataProvenance = "D"
        dataDictionaryEntry.sourcePpiModule = "E"
        dataDictionaryEntry.transformedByRegisteredTierPrivacyMethods = true

        dataDictionaryEntryDao!!.save(dataDictionaryEntry)

        val response = dataSetController!!
                .getDataDictionaryEntry(cdrVersion.cdrVersionId, domain.toString(), domainValue)
                .body

        assertThat(response.getCdrVersionId().longValue())
                .isEqualTo(dataDictionaryEntry.cdrVersion!!.cdrVersionId)
        assertThat(Timestamp(response.getDefinedTime()))
                .isEqualTo(dataDictionaryEntry.definedTime)
        assertThat(response.getRelevantOmopTable())
                .isEqualTo(dataDictionaryEntry.relevantOmopTable)
        assertThat(response.getFieldName()).isEqualTo(dataDictionaryEntry.fieldName)
        assertThat(response.getOmopCdmStandardOrCustomField())
                .isEqualTo(dataDictionaryEntry.omopCdmStandardOrCustomField)
        assertThat(response.getDescription()).isEqualTo(dataDictionaryEntry.description)
        assertThat(response.getFieldType()).isEqualTo(dataDictionaryEntry.fieldType)
        assertThat(response.getDataProvenance()).isEqualTo(dataDictionaryEntry.dataProvenance)
        assertThat(response.getSourcePpiModule()).isEqualTo(dataDictionaryEntry.sourcePpiModule)
        assertThat(response.getTransformedByRegisteredTierPrivacyMethods())
                .isEqualTo(dataDictionaryEntry.transformedByRegisteredTierPrivacyMethods)
    }

    @Test
    fun testGetDataDictionaryEntry_invalidCdr() {
        expectedEx.expect(BadRequestException::class.java)
        expectedEx.expectMessage("Invalid CDR Version")

        dataSetController!!.getDataDictionaryEntry(-1L, Domain.DRUG.toString(), "TEST FIELD")
    }

    @Test
    fun testGetDataDictionaryEntry_invalidDomain() {
        expectedEx.expect(BadRequestException::class.java)
        expectedEx.expectMessage("Invalid Domain")

        dataSetController!!.getDataDictionaryEntry(
                cdrVersionDao!!.findAll().iterator().next().cdrVersionId, "random", "TEST FIELD")
    }

    @Test
    fun testGetDataDictionaryEntry_notFound() {
        expectedEx.expect(NotFoundException::class.java)

        dataSetController!!.getDataDictionaryEntry(1L, Domain.DRUG.toString(), "random")
    }

    companion object {

        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
    }
}
