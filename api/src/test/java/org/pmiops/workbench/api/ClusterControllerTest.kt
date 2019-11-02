package org.pmiops.workbench.api

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.cloud.Date
import com.google.common.collect.ImmutableList
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import java.util.Random
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.pmiops.workbench.compliance.ComplianceService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.FeatureFlagsConfig
import org.pmiops.workbench.db.dao.AdminActionHistoryDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.google.DirectoryService
import org.pmiops.workbench.model.Cluster
import org.pmiops.workbench.model.ClusterConfig
import org.pmiops.workbench.model.ClusterLocalizeRequest
import org.pmiops.workbench.model.ClusterLocalizeResponse
import org.pmiops.workbench.model.ClusterStatus
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.UpdateClusterConfigRequest
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.test.FakeLongRandom
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
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
class ClusterControllerTest {

    @Captor
    private val mapCaptor: ArgumentCaptor<Map<String, String>>? = null

    @Autowired
    internal var notebookService: LeonardoNotebooksClient? = null
    @Autowired
    internal var fireCloudService: FireCloudService? = null
    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var workspaceService: WorkspaceService? = null
    @Autowired
    internal var clusterController: ClusterController? = null
    @Autowired
    internal var userRecentResourceService: UserRecentResourceService? = null
    @Autowired
    internal var clock: Clock? = null

    private var cdrVersion: CdrVersion? = null
    private var testFcCluster: org.pmiops.workbench.notebooks.model.Cluster? = null
    private var testCluster: Cluster? = null
    private var testWorkspace: Workspace? = null

    private val clusterName: String
        get() = "all-of-us-" + java.lang.Long.toString(user.userId)

    @TestConfiguration
    @Import(ClusterController::class, UserService::class)
    @MockBean(FireCloudService::class, LeonardoNotebooksClient::class, WorkspaceService::class, UserRecentResourceService::class, ComplianceService::class, DirectoryService::class, AdminActionHistoryDao::class)
    internal class Configuration {

        @Bean
        @Scope("prototype")
        fun workbenchConfig(): WorkbenchConfig {
            return config
        }

        @Bean
        fun clock(): Clock {
            return CLOCK
        }

        @Bean
        @Scope("prototype")
        fun user(): User {
            return user
        }

        @Bean
        fun random(): Random {
            return FakeLongRandom(123)
        }
    }

    @Before
    fun setUp() {
        config = WorkbenchConfig()
        config.server = WorkbenchConfig.ServerConfig()
        config.server.apiBaseUrl = API_BASE_URL
        config.firecloud = WorkbenchConfig.FireCloudConfig()
        config.firecloud.registeredDomainName = ""
        config.firecloud.clusterDefaultMachineType = "n1-standard-4"
        config.firecloud.clusterDefaultDiskSizeGb = 50
        config.access = WorkbenchConfig.AccessConfig()
        config.access.enableComplianceTraining = true
        config.featureFlags = FeatureFlagsConfig()

        user = User()
        user.email = LOGGED_IN_USER_EMAIL
        user.userId = 123L

        createUser(OTHER_USER_EMAIL)

        cdrVersion = CdrVersion()
        cdrVersion!!.name = "1"
        // set the db name to be empty since test cases currently
        // run in the workbench schema only.
        cdrVersion!!.cdrDbName = ""
        cdrVersion!!.bigqueryDataset = BIGQUERY_DATASET

        val createdDate = Date.fromYearMonthDay(1988, 12, 26).toString()
        testFcCluster = org.pmiops.workbench.notebooks.model.Cluster()
                .clusterName(clusterName)
                .googleProject(BILLING_PROJECT_ID)
                .status(org.pmiops.workbench.notebooks.model.ClusterStatus.DELETING)
                .createdDate(createdDate)
        testCluster = Cluster()
                .clusterName(clusterName)
                .clusterNamespace(BILLING_PROJECT_ID)
                .status(ClusterStatus.DELETING)
                .createdDate(createdDate)

        testWorkspace = Workspace()
        testWorkspace!!.workspaceNamespace = WORKSPACE_NS
        testWorkspace!!.name = WORKSPACE_NAME
        testWorkspace!!.cdrVersion = cdrVersion
    }

    private fun createFcWorkspace(
            ns: String, name: String, creator: String): org.pmiops.workbench.firecloud.model.Workspace {
        return org.pmiops.workbench.firecloud.model.Workspace()
                .namespace(ns)
                .name(name)
                .createdBy(creator)
                .bucketName(BUCKET_NAME)
    }

    @Throws(Exception::class)
    private fun stubGetWorkspace(ns: String, name: String, creator: String) {
        val w = Workspace()
        w.workspaceNamespace = ns
        w.firecloudName = name
        w.cdrVersion = cdrVersion
        `when`(workspaceService!!.getRequired(ns, name)).thenReturn(w)
        stubGetFcWorkspace(createFcWorkspace(ns, name, creator))
    }

    @Throws(Exception::class)
    private fun stubGetFcWorkspace(fcWorkspace: org.pmiops.workbench.firecloud.model.Workspace) {
        val fcResponse = org.pmiops.workbench.firecloud.model.WorkspaceResponse()
        fcResponse.setWorkspace(fcWorkspace)
        fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString())
        `when`<Any>(fireCloudService!!.getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName()))
                .thenReturn(fcResponse)
    }

    private fun dataUriToJson(dataUri: String): JSONObject {
        val b64 = dataUri.substring(dataUri.indexOf(',') + 1)
        val raw = Base64.getUrlDecoder().decode(b64)
        return JSONObject(String(raw))
    }

    @Test
    @Throws(Exception::class)
    fun testListClusters() {
        `when`<Any>(notebookService!!.getCluster(BILLING_PROJECT_ID, clusterName))
                .thenReturn(testFcCluster)

        assertThat(
                clusterController!!
                        .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                        .body
                        .getDefaultCluster())
                .isEqualTo(testCluster)
    }

    @Test
    @Throws(Exception::class)
    fun testListClustersUnknownStatus() {
        `when`<Any>(notebookService!!.getCluster(BILLING_PROJECT_ID, clusterName))
                .thenReturn(testFcCluster!!.status(null))

        assertThat(
                clusterController!!
                        .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                        .body
                        .getDefaultCluster()
                        .getStatus())
                .isEqualTo(ClusterStatus.UNKNOWN)
    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testListClustersNullBillingProject() {
        clusterController!!.listClusters(null, WORKSPACE_NAME)
    }

    @Test
    @Throws(Exception::class)
    fun testListClustersLazyCreate() {
        `when`<Any>(notebookService!!.getCluster(BILLING_PROJECT_ID, clusterName))
                .thenThrow(NotFoundException())
        `when`<Any>(notebookService!!.createCluster(
                eq(BILLING_PROJECT_ID), eq(clusterName), eq(WORKSPACE_NAME)))
                .thenReturn(testFcCluster)
        stubGetWorkspace(WORKSPACE_NS, WORKSPACE_NAME, "test")

        assertThat(
                clusterController!!
                        .listClusters(BILLING_PROJECT_ID, WORKSPACE_NAME)
                        .body
                        .getDefaultCluster())
                .isEqualTo(testCluster)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteCluster() {
        clusterController!!.deleteCluster(BILLING_PROJECT_ID, "cluster")
        verify<LeonardoNotebooksClient>(notebookService).deleteCluster(BILLING_PROJECT_ID, "cluster")
    }

    @Test
    fun testSetClusterConfig() {
        val response = this.clusterController!!.updateClusterConfig(
                UpdateClusterConfigRequest()
                        .userEmail(OTHER_USER_EMAIL)
                        .clusterConfig(
                                ClusterConfig().machineType("n1-standard-16").masterDiskSize(100)))
        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        val updatedUser = userDao!!.findUserByEmail(OTHER_USER_EMAIL)
        assertThat(updatedUser.clusterConfigDefault!!.machineType).isEqualTo("n1-standard-16")
        assertThat(updatedUser.clusterConfigDefault!!.masterDiskSize).isEqualTo(100)
    }

    @Test
    fun testUpdateClusterConfigClear() {
        var response = this.clusterController!!.updateClusterConfig(
                UpdateClusterConfigRequest()
                        .userEmail(OTHER_USER_EMAIL)
                        .clusterConfig(
                                ClusterConfig().machineType("n1-standard-16").masterDiskSize(100)))
        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        response = this.clusterController!!.updateClusterConfig(
                UpdateClusterConfigRequest().userEmail(OTHER_USER_EMAIL).clusterConfig(null))
        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        val updatedUser = userDao!!.findUserByEmail(OTHER_USER_EMAIL)
        assertThat(updatedUser.clusterConfigDefault).isNull()
    }

    @Test(expected = NotFoundException::class)
    fun testUpdateClusterConfigUserNotFound() {
        this.clusterController!!.updateClusterConfig(
                UpdateClusterConfigRequest().userEmail("not-found@researchallofus.org"))
    }

    @Test
    @Throws(Exception::class)
    fun testLocalize() {
        val req = ClusterLocalizeRequest()
                .workspaceNamespace(WORKSPACE_NS)
                .workspaceId(WORKSPACE_ID)
                .notebookNames(ImmutableList.of<E>("foo.ipynb"))
                .playgroundMode(false)
        stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL)
        val resp = clusterController!!.localize(BILLING_PROJECT_ID, "cluster", req).body
        assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid")

        verify<LeonardoNotebooksClient>(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor!!.capture())
        val localizeMap = mapCaptor.value
        assertThat(localizeMap.keys)
                .containsExactly(
                        "workspaces/wsid/foo.ipynb",
                        "workspaces_playground/wsid/.all_of_us_config.json",
                        "workspaces/wsid/.all_of_us_config.json")
        assertThat(localizeMap)
                .containsEntry("workspaces/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb")
        val aouJson = dataUriToJson(localizeMap["workspaces/wsid/.all_of_us_config.json"])
        assertThat(aouJson.getString("WORKSPACE_ID")).isEqualTo(WORKSPACE_ID)
        assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo(BILLING_PROJECT_ID)
        assertThat(aouJson.getString("API_HOST")).isEqualTo(API_HOST)
        verify<UserRecentResourceService>(userRecentResourceService, times(1))
                .updateNotebookEntry(anyLong(), anyLong(), anyString(), any(Timestamp::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testLocalize_playgroundMode() {
        val req = ClusterLocalizeRequest()
                .workspaceNamespace(WORKSPACE_NS)
                .workspaceId(WORKSPACE_ID)
                .notebookNames(ImmutableList.of<E>("foo.ipynb"))
                .playgroundMode(true)
        stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL)
        val resp = clusterController!!.localize(BILLING_PROJECT_ID, "cluster", req).body
        assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces_playground/wsid")
        verify<LeonardoNotebooksClient>(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor!!.capture())
        val localizeMap = mapCaptor.value
        assertThat(localizeMap.keys)
                .containsExactly(
                        "workspaces_playground/wsid/foo.ipynb",
                        "workspaces_playground/wsid/.all_of_us_config.json",
                        "workspaces/wsid/.all_of_us_config.json")
        assertThat(localizeMap)
                .containsEntry(
                        "workspaces_playground/wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb")
    }

    @Test
    @Throws(Exception::class)
    fun testLocalize_differentNamespace() {
        val req = ClusterLocalizeRequest()
                .workspaceNamespace(WORKSPACE_NS)
                .workspaceId(WORKSPACE_ID)
                .notebookNames(ImmutableList.of<E>("foo.ipynb"))
                .playgroundMode(false)
        stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL)
        val resp = clusterController!!.localize("other-proj", "cluster", req).body
        verify<LeonardoNotebooksClient>(notebookService).localize(eq("other-proj"), eq("cluster"), mapCaptor!!.capture())

        val localizeMap = mapCaptor.value
        assertThat(localizeMap.keys)
                .containsExactly(
                        "workspaces/proj__wsid/foo.ipynb",
                        "workspaces/proj__wsid/.all_of_us_config.json",
                        "workspaces_playground/proj__wsid/.all_of_us_config.json")
        assertThat(localizeMap)
                .containsEntry(
                        "workspaces/proj__wsid/foo.ipynb", "gs://workspace-bucket/notebooks/foo.ipynb")
        assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/proj__wsid")
        val aouJson = dataUriToJson(localizeMap["workspaces/proj__wsid/.all_of_us_config.json"])
        assertThat(aouJson.getString("BILLING_CLOUD_PROJECT")).isEqualTo("other-proj")
    }

    @Test
    @Throws(Exception::class)
    fun testLocalize_noNotebooks() {
        val req = ClusterLocalizeRequest()
        req.setWorkspaceNamespace(WORKSPACE_NS)
        req.setWorkspaceId(WORKSPACE_ID)
        req.setPlaygroundMode(false)
        stubGetWorkspace(WORKSPACE_NS, WORKSPACE_ID, LOGGED_IN_USER_EMAIL)
        val resp = clusterController!!.localize(BILLING_PROJECT_ID, "cluster", req).body
        verify<LeonardoNotebooksClient>(notebookService).localize(eq(BILLING_PROJECT_ID), eq("cluster"), mapCaptor!!.capture())

        // Config files only.
        val localizeMap = mapCaptor.value
        assertThat(localizeMap.keys)
                .containsExactly(
                        "workspaces_playground/wsid/.all_of_us_config.json",
                        "workspaces/wsid/.all_of_us_config.json")
        assertThat(resp.getClusterLocalDirectory()).isEqualTo("workspaces/wsid")
    }

    private fun createUser(email: String) {
        val user = User()
        user.givenName = "first"
        user.familyName = "last"
        user.email = email
        userDao!!.save(user)
    }

    companion object {

        private val BILLING_PROJECT_ID = "proj"
        // a workspace's namespace is always its billing project ID
        private val WORKSPACE_NS = BILLING_PROJECT_ID
        private val WORKSPACE_ID = "wsid"
        private val WORKSPACE_NAME = "wsn"
        private val LOGGED_IN_USER_EMAIL = "bob@gmail.com"
        private val OTHER_USER_EMAIL = "alice@gmail.com"
        private val BUCKET_NAME = "workspace-bucket"
        private val NOW = Instant.now()
        private val CLOCK = FakeClock(NOW, ZoneId.systemDefault())
        private val API_HOST = "api.stable.fake-research-aou.org"
        private val API_BASE_URL = "https://$API_HOST"
        private val BIGQUERY_DATASET = "dataset-name"

        private var config = WorkbenchConfig()
        private var user = User()
    }
}
