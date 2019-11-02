package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.TableResult
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.ArrayList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.test.FakeClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.annotation.DirtiesContext.ClassMode
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OfflineAuditControllerTest {

    @Autowired
    internal var bigQueryService: BigQueryService? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var cdrVersionDao: CdrVersionDao? = null
    @Autowired
    internal var offlineAuditController: OfflineAuditController? = null
    @Autowired
    internal var workspaceDao: WorkspaceDao? = null

    @TestConfiguration
    @Import(OfflineAuditController::class)
    @MockBean(BigQueryService::class, WorkspaceDao::class)
    internal class Configuration {
        @Bean
        fun clock(): Clock {
            return CLOCK
        }
    }

    @Before
    fun setUp() {
        var user = User()
        user.email = USER_EMAIL
        user.userId = 123L
        user.disabled = false
        user = userDao!!.save(user)

        var cdrV1 = CdrVersion()
        cdrV1.bigqueryProject = CDR_V1_PROJECT_ID
        cdrV1 = cdrVersionDao!!.save(cdrV1)
        var cdrV2 = CdrVersion()
        cdrV2.bigqueryProject = CDR_V2_PROJECT_ID
        cdrV2 = cdrVersionDao!!.save(cdrV2)

        `when`(workspaceDao!!.findAllWorkspaceNamespaces()).thenReturn(ImmutableSet.of(FC_PROJECT_ID))

        CLOCK.setInstant(NOW)
    }

    // TODO(RW-350): This stubbing is awful, improve this.
    private fun stubBigQueryCalls(projectId: String, email: String, total: Long) {
        val queryResult = mock(TableResult::class.java)
        val testIterable = object : Iterable {
            override operator fun iterator(): Iterator<*> {
                val list = ArrayList<FieldValue>()
                list.add(null)
                return list.iterator()
            }
        }
        val rm = ImmutableMap.builder<String, Int>()
                .put("client_project_id", 0)
                .put("user_email", 1)
                .put("total", 2)
                .build()

        `when`(bigQueryService!!.executeQuery(any<QueryJobConfiguration>())).thenReturn(queryResult)
        `when`(bigQueryService!!.getResultMapper(queryResult)).thenReturn(rm)
        `when`<Iterable<FieldValueList>>(queryResult.iterateAll()).thenReturn(testIterable)
        `when`(bigQueryService!!.getString(null!!, 0)).thenReturn(projectId)
        `when`(bigQueryService!!.getString(null!!, 1)).thenReturn(email)
        `when`(bigQueryService!!.getLong(null!!, 2)).thenReturn(total)
    }

    @Test
    fun testAuditTableSuffix() {
        assertThat(OfflineAuditController.auditTableSuffix(Instant.parse("2007-01-03T00:00:00.00Z"), 0))
                .isEqualTo("20070103")
        assertThat(OfflineAuditController.auditTableSuffix(Instant.parse("2018-01-01T23:59:59.00Z"), 3))
                .isEqualTo("20171229")
    }

    @Test
    fun testAuditBigQueryCdrV1Queries() {
        stubBigQueryCalls(CDR_V1_PROJECT_ID, USER_EMAIL, 5)
        assertThat(offlineAuditController!!.auditBigQuery().body.getNumQueryIssues()).isEqualTo(0)
    }

    @Test
    fun testAuditBigQueryCdrV2Queries() {
        stubBigQueryCalls(CDR_V2_PROJECT_ID, USER_EMAIL, 5)
        assertThat(offlineAuditController!!.auditBigQuery().body.getNumQueryIssues()).isEqualTo(0)
    }

    @Test
    fun testAuditBigQueryFirecloudQueries() {
        stubBigQueryCalls(FC_PROJECT_ID, USER_EMAIL, 5)
        assertThat(offlineAuditController!!.auditBigQuery().body.getNumQueryIssues()).isEqualTo(0)
    }

    @Test
    fun testAuditBigQueryUnrecognizedProjectQueries() {
        stubBigQueryCalls("my-personal-gcp-project", USER_EMAIL, 5)
        // These stubs are hit once per CDR project, so the total number of issues is doubled.
        assertThat(offlineAuditController!!.auditBigQuery().body.getNumQueryIssues()).isEqualTo(10)
    }

    companion object {
        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private val CDR_V1_PROJECT_ID = "cdr1-project"
        private val CDR_V2_PROJECT_ID = "cdr2-project"
        private val FC_PROJECT_ID = "fc-project"
        private val USER_EMAIL = "falco@lombardi.com"
    }
}
