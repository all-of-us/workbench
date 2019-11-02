package org.pmiops.workbench.billing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.withinPercentage
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions

import com.google.cloud.PageImpl
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValue.Attribute
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.TableResult
import java.io.InputStream
import java.io.ObjectInputStream
import java.sql.Timestamp
import java.util.Arrays
import java.util.HashMap
import java.util.stream.Collectors
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.db.model.Workspace.BillingMigrationStatus
import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage
import org.pmiops.workbench.model.BillingAccountType
import org.pmiops.workbench.model.BillingStatus
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
class FreeTierBillingServiceTest {

    @Autowired
    internal var bigQueryService: BigQueryService? = null

    @Autowired
    internal var freeTierBillingService: FreeTierBillingService? = null

    @Autowired
    internal var userDao: UserDao? = null

    @Autowired
    internal var notificationService: NotificationService? = null

    @Autowired
    internal var workspaceDao: WorkspaceDao? = null

    @Autowired
    internal var workspaceFreeTierUsageDao: WorkspaceFreeTierUsageDao? = null

    @TestConfiguration
    @Import(FreeTierBillingService::class)
    @MockBean(BigQueryService::class, NotificationService::class)
    internal class Configuration {
        @Bean
        @Scope("prototype")
        fun workbenchConfig(): WorkbenchConfig? {
            return workbenchConfig
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        workbenchConfig = WorkbenchConfig.createEmptyConfig()

        // returns the contents of resources/bigquery/get_billing_project_costs.json
        val inputStream = javaClass.classLoader.getResourceAsStream("bigquery/get_billing_project_costs.ser")
        val tableResult = ObjectInputStream(inputStream).readObject() as TableResult

        doReturn(tableResult).`when`<BigQueryService>(bigQueryService).executeQuery(any<QueryJobConfiguration>())
    }

    @Test
    fun checkFreeTierBillingUsage_singleProjectExceedsLimit() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE)
    }

    @Test
    fun checkFreeTierBillingUsage_disabledUserNotIgnored() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        user.disabled = true
        userDao!!.save(user)
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE)
    }

    @Test
    fun checkFreeTierBillingUsage_deletedWorkspaceNotIgnored() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)
        workspace.workspaceActiveStatusEnum = WorkspaceActiveStatus.DELETED
        workspaceDao!!.save(workspace)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE)
    }

    @Test
    fun checkFreeTierBillingUsage_noAlert() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 500.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verifyZeroInteractions(notificationService)

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE)
    }

    @Test
    fun checkFreeTierBillingUsage_workspaceMissingCreator() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 500.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)
        createWorkspace(null, "rumney")

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verifyZeroInteractions(notificationService)

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE)
    }

    @Test
    fun checkFreeTierBillingUsage_override() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 10.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE)

        user.freeTierCreditsLimitOverride = 1000.0
        userDao!!.save(user)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verifyZeroInteractions(notificationService)

        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE)
    }

    // the relevant portion of the BigQuery response for this test:
    //
    //  {
    //    "id": "aou-test-f1-47",
    //    "cost": "397.5191679999987"
    //  },
    //  {
    //    "id": "aou-test-f1-26",
    //    "cost": "372.1294129999994"
    //  }

    @Test
    fun checkFreeTierBillingUsage_combinedProjectsExceedsLimit() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 500.0

        val user = createUser("test@test.com")
        val ws1 = createWorkspace(user, "aou-test-f1-26")
        val ws2 = createWorkspace(user, "aou-test-f1-47")
        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())

        val expectedCosts = HashMap<Long, Double>()
        expectedCosts[ws1.workspaceId] = 372.1294129999994
        expectedCosts[ws2.workspaceId] = 397.5191679999987

        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(2)

        for (ws in Arrays.asList(ws1, ws2)) {
            // retrieve from DB again to reflect update after cron
            val dbWorkspace = workspaceDao!!.findOne(ws.workspaceId)
            assertThat(dbWorkspace.billingStatus).isEqualTo(BillingStatus.INACTIVE)
            assertThat(dbWorkspace.billingAccountType).isEqualTo(BillingAccountType.FREE_TIER)

            val usage = workspaceFreeTierUsageDao!!.findOneByWorkspace(ws)
            assertThat<User>(usage.user).isEqualTo(user)
            assertThat<Workspace>(usage.workspace).isEqualTo(ws)
            assertThat(usage.cost)
                    .isCloseTo(expectedCosts[ws.workspaceId], withinPercentage(0.001))
        }
    }

    @Test
    fun checkFreeTierBillingUsage_twoUsers() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user1 = createUser("test@test.com")
        val ws1 = createWorkspace(user1, "aou-test-f1-26")
        val user2 = createUser("more@test.com")
        val ws2 = createWorkspace(user2, "aou-test-f1-47")
        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user1), any())
        verify<NotificationService>(notificationService).alertUser(eq(user2), any())

        val expectedCosts = HashMap<Long, Double>()
        expectedCosts[ws1.workspaceId] = 372.1294129999994
        expectedCosts[ws2.workspaceId] = 397.5191679999987

        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(2)

        for (ws in Arrays.asList(ws1, ws2)) {
            // retrieve from DB again to reflect update after cron
            val dbWorkspace = workspaceDao!!.findOne(ws.workspaceId)
            assertThat(dbWorkspace.billingStatus).isEqualTo(BillingStatus.INACTIVE)
            assertThat(dbWorkspace.billingAccountType).isEqualTo(BillingAccountType.FREE_TIER)

            val usage = workspaceFreeTierUsageDao!!.findOneByWorkspace(ws)
            assertThat<User>(usage.user).isEqualTo(ws.creator)
            assertThat<Workspace>(usage.workspace).isEqualTo(ws)
            assertThat(usage.cost)
                    .isCloseTo(expectedCosts[ws.workspaceId], withinPercentage(0.001))
        }
    }

    @Test
    fun checkFreeTierBillingUsage_ignoreOLDMigrationStatus() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.OLD)
        freeTierBillingService!!.checkFreeTierBillingUsage()

        verifyZeroInteractions(notificationService)
        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(0)
    }

    @Test
    fun checkFreeTierBillingUsage_ignoreMIGRATEDMigrationStatus() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.MIGRATED)
        freeTierBillingService!!.checkFreeTierBillingUsage()

        verifyZeroInteractions(notificationService)
        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(0)
    }

    @Test
    fun checkFreeTierBillingUsage_dbUpdate() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        val workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService).alertUser(eq(user), any())
        assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE)

        val t0 = workspaceFreeTierUsageDao!!.findAll().iterator().next().lastUpdateTime

        // time elapses, and this project incurs additional cost

        val newCosts = HashMap<String, Double>()
        newCosts[SINGLE_WORKSPACE_TEST_PROJECT] = SINGLE_WORKSPACE_TEST_COST * 2
        doReturn(mockBQTableResult(newCosts)).`when`<BigQueryService>(bigQueryService).executeQuery(any<QueryJobConfiguration>())

        // we alert again, and the cost field is updated in the DB

        freeTierBillingService!!.checkFreeTierBillingUsage()
        verify<NotificationService>(notificationService, times(2)).alertUser(eq(user), any())

        // retrieve from DB again to reflect update after cron
        val dbWorkspace = workspaceDao!!.findOne(workspace.workspaceId)
        assertThat(dbWorkspace.billingStatus).isEqualTo(BillingStatus.INACTIVE)
        assertThat(dbWorkspace.billingAccountType).isEqualTo(BillingAccountType.FREE_TIER)

        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(1)
        val dbEntry = workspaceFreeTierUsageDao!!.findAll().iterator().next()
        assertThat<User>(dbEntry.user).isEqualTo(user)
        assertThat<Workspace>(dbEntry.workspace).isEqualTo(workspace)
        assertThat(dbEntry.cost)
                .isCloseTo(SINGLE_WORKSPACE_TEST_COST * 2, withinPercentage(0.001))

        val t1 = dbEntry.lastUpdateTime
        assertThat(t1).isAfter(t0)
    }

    @Test
    fun getUserFreeTierLimit_default() {
        val user = createUser("test@test.com")

        workbenchConfig!!.billing.defaultFreeCreditsLimit = 1.0
        assertThat(freeTierBillingService!!.getUserFreeTierLimit(user))
                .isCloseTo(1.0, withinPercentage(0.001))

        workbenchConfig!!.billing.defaultFreeCreditsLimit = 123.456
        assertThat(freeTierBillingService!!.getUserFreeTierLimit(user))
                .isCloseTo(123.456, withinPercentage(0.001))
    }

    @Test
    fun getUserFreeTierLimit_override() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 123.456

        val user = createUser("test@test.com")
        user.freeTierCreditsLimitOverride = 100.0
        userDao!!.save(user)

        assertThat(freeTierBillingService!!.getUserFreeTierLimit(user))
                .isCloseTo(100.0, withinPercentage(0.001))

        user.freeTierCreditsLimitOverride = 200.0
        userDao!!.save(user)

        assertThat(freeTierBillingService!!.getUserFreeTierLimit(user))
                .isCloseTo(200.0, withinPercentage(0.001))
    }

    @Test
    fun getUserCachedFreeTierUsage() {
        workbenchConfig!!.billing.defaultFreeCreditsLimit = 100.0

        val user = createUser("test@test.com")
        createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT)

        // we have not yet had a chance to cache this usage
        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user)).isNull()

        freeTierBillingService!!.checkFreeTierBillingUsage()

        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user))
                .isCloseTo(SINGLE_WORKSPACE_TEST_COST, withinPercentage(0.001))

        createWorkspace(user, "another project")
        val newCosts = HashMap<String, Double>()
        newCosts["another project"] = 100.0
        newCosts[SINGLE_WORKSPACE_TEST_PROJECT] = 1000.0
        doReturn(mockBQTableResult(newCosts)).`when`<BigQueryService>(bigQueryService).executeQuery(any<QueryJobConfiguration>())

        // we have not yet cached the new workspace costs
        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user))
                .isCloseTo(SINGLE_WORKSPACE_TEST_COST, withinPercentage(0.001))

        freeTierBillingService!!.checkFreeTierBillingUsage()

        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user))
                .isCloseTo(1100.0, withinPercentage(0.001))

        val user2 = createUser("another user")
        createWorkspace(user2, "project 3")
        newCosts["project 3"] = 999.9
        doReturn(mockBQTableResult(newCosts)).`when`<BigQueryService>(bigQueryService).executeQuery(any<QueryJobConfiguration>())

        freeTierBillingService!!.checkFreeTierBillingUsage()

        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user))
                .isCloseTo(1100.0, withinPercentage(0.001))
        assertThat(freeTierBillingService!!.getUserCachedFreeTierUsage(user2))
                .isCloseTo(999.9, withinPercentage(0.001))
    }

    private fun mockBQTableResult(costMap: Map<String, Double>): TableResult {
        val idField = Field.of("id", LegacySQLTypeName.STRING)
        val costField = Field.of("cost", LegacySQLTypeName.FLOAT)
        val s = Schema.of(idField, costField)

        val tableRows = costMap.entries.stream()
                .map { e ->
                    val id = FieldValue.of(Attribute.PRIMITIVE, e.key)
                    val cost = FieldValue.of(Attribute.PRIMITIVE, e.value.toString())
                    FieldValueList.of(Arrays.asList(id, cost))
                }
                .collect<List<FieldValueList>, Any>(Collectors.toList())

        return TableResult(s, tableRows.size.toLong(), PageImpl({ null }, null, tableRows))
    }

    private fun assertSingleWorkspaceTestDbState(
            user: User, workspaceForQuerying: Workspace, billingStatus: BillingStatus) {

        val workspace = workspaceDao!!.findOne(workspaceForQuerying.workspaceId)
        assertThat(workspace.billingStatus).isEqualTo(billingStatus)
        assertThat(workspace.billingAccountType).isEqualTo(BillingAccountType.FREE_TIER)

        assertThat(workspaceFreeTierUsageDao!!.count()).isEqualTo(1)
        val dbEntry = workspaceFreeTierUsageDao!!.findAll().iterator().next()
        assertThat<User>(dbEntry.user).isEqualTo(user)
        assertThat<Workspace>(dbEntry.workspace).isEqualTo(workspace)
        assertThat(dbEntry.cost).isCloseTo(SINGLE_WORKSPACE_TEST_COST, withinPercentage(0.001))
    }

    private fun createUser(email: String): User {
        val user = User()
        user.email = email
        return userDao!!.save(user)
    }

    private fun createWorkspace(
            creator: User?, namespace: String, billingMigrationStatus: BillingMigrationStatus = BillingMigrationStatus.NEW): Workspace {
        val workspace = Workspace()
        workspace.creator = creator
        workspace.workspaceNamespace = namespace
        workspace.billingMigrationStatusEnum = billingMigrationStatus
        return workspaceDao!!.save(workspace)
    }

    companion object {

        private var workbenchConfig: WorkbenchConfig? = null

        // the relevant portion of the BigQuery response for the single-project tests:
        //
        //  {
        //    "id": "aou-test-f1-26",
        //    "cost": "372.1294129999994"
        //  }

        private val SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-f1-26"
        private val SINGLE_WORKSPACE_TEST_COST = 372.1294129999994
    }
}// we only alert/record for BillingMigrationStatus.NEW workspaces
