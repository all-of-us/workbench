package org.pmiops.workbench

import com.google.common.truth.Truth.assertThat

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.io.Resources
import com.google.gson.Gson
import java.io.IOException
import java.nio.charset.Charset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.firecloud.ApiClient
import org.pmiops.workbench.firecloud.ApiException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.FireCloudServiceImpl
import org.pmiops.workbench.firecloud.FirecloudRetryHandler
import org.pmiops.workbench.firecloud.api.BillingApi
import org.pmiops.workbench.firecloud.api.GroupsApi
import org.pmiops.workbench.firecloud.api.NihApi
import org.pmiops.workbench.firecloud.api.ProfileApi
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi
import org.pmiops.workbench.firecloud.api.StatusApi
import org.pmiops.workbench.firecloud.api.WorkspacesApi
import org.pmiops.workbench.firecloud.model.Me
import org.pmiops.workbench.test.Providers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.backoff.NoBackOffPolicy
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [IntegrationTestConfig::class])
class FireCloudIntegrationTest {

    /*
   * Mocked service providers are (currently) not available for functional integration tests.
   * Integration tests with real users/workspaces against production FireCloud is not
   * recommended at this time.
   */
    @Mock
    private val billingApi: BillingApi? = null
    @Mock
    private val workspacesApi: WorkspacesApi? = null
    @Mock
    private val allOfUsGroupsApi: GroupsApi? = null
    @Mock
    private val workspaceAclsApi: WorkspacesApi? = null
    @Mock
    private val staticNotebooksApi: StaticNotebooksApi? = null
    @Mock
    private val profileApi: ProfileApi? = null
    @Mock
    private val nihApi: NihApi? = null

    // N.B. this will load the default service account credentials for whatever AoU environment
    // is set when running integration tests. This should be the test environment.
    @Autowired
    @Qualifier(Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
    private var serviceAccountCredential: GoogleCredential? = null

    @Autowired
    private val serviceAccounts: ServiceAccounts? = null

    @Autowired
    @Qualifier(Constants.FIRECLOUD_ADMIN_CREDS)
    private val fireCloudAdminCredential: GoogleCredential? = null

    private val testService: FireCloudService
        @Throws(Exception::class)
        get() = createService(loadConfig("config_test.json"))

    private val prodService: FireCloudService
        @Throws(Exception::class)
        get() = createService(loadConfig("config_prod.json"))

    @Before
    @Throws(IOException::class)
    fun setUp() {
        // Get a refreshed access token for the FireCloud service account credentials.
        serviceAccountCredential = serviceAccountCredential!!.createScoped(FireCloudServiceImpl.FIRECLOUD_API_OAUTH_SCOPES)
        serviceAccountCredential!!.refreshToken()
    }

    /**
     * Creates a FireCloudService instance with the FireCloud base URL corresponding to the given
     * WorkbenchConfig. Note that this will always use the test environment's default service account
     * credentials when making API calls. It shouldn't be possible to make authenticated calls to the
     * FireCloud prod environment.
     *
     *
     * This method mostly exists to allow us to run a status-check against both FC dev & prod
     * within the same integration test run.
     */
    private fun createService(config: WorkbenchConfig): FireCloudService {
        val apiClient = ApiClient()
                .setBasePath(config.firecloud.baseUrl)
                .setDebugging(config.firecloud.debugEndpoints)
        apiClient.setAccessToken(serviceAccountCredential!!.accessToken)

        return FireCloudServiceImpl(
                Providers.of(config),
                Providers.of<ProfileApi>(profileApi),
                Providers.of<BillingApi>(billingApi),
                Providers.of<GroupsApi>(allOfUsGroupsApi),
                Providers.of<NihApi>(nihApi),
                Providers.of<WorkspacesApi>(workspacesApi),
                Providers.of<WorkspacesApi>(workspaceAclsApi),
                Providers.of<StatusApi>(StatusApi(apiClient)),
                Providers.of<StaticNotebooksApi>(staticNotebooksApi),
                FirecloudRetryHandler(NoBackOffPolicy()),
                serviceAccounts,
                Providers.of(fireCloudAdminCredential))
    }

    @Throws(Exception::class)
    private fun loadConfig(filename: String): WorkbenchConfig {
        val testConfig = Resources.toString(Resources.getResource(filename), Charset.defaultCharset())
        val workbenchConfig = Gson().fromJson(testConfig, WorkbenchConfig::class.java)
        workbenchConfig.firecloud.debugEndpoints = true
        return workbenchConfig
    }

    @Test
    @Throws(Exception::class)
    fun testStatusProd() {
        assertThat(prodService.firecloudStatus).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testStatusDev() {
        assertThat(testService.firecloudStatus).isTrue()
    }

    /**
     * Ensures we can successfully use delegation of authority to make FireCloud API calls on behalf
     * of AoU users.
     *
     *
     * This test depends on there being an active account in FireCloud dev with the email address
     * integration-test-user@fake-research-aou.org.
     */
    @Test
    @Throws(Exception::class)
    fun testImpersonatedProfileCall() {
        val apiClient = testService
                .getApiClientWithImpersonation("integration-test-user@fake-research-aou.org")

        // Run the most basic API call against the /me/ endpoint.
        val profileApi = ProfileApi(apiClient)
        val me = profileApi.me()
        assertThat(me.getUserInfo().getUserEmail())
                .isEqualTo("integration-test-user@fake-research-aou.org")
        assertThat(me.getUserInfo().getUserSubjectId()).isEqualTo("101727030557929965916")

        // Run a test against a different FireCloud endpoint. This is important, because the /me/
        // endpoint is accessible even by service accounts whose subject IDs haven't been whitelisted
        // by FireCloud devops.
        //
        // If we haven't had our "firecloud-admin" service account whitelisted,
        // then the following API call would result in a 401 error instead of a 404.
        val nihApi = NihApi(apiClient)
        var responseCode = 0
        try {
            nihApi.nihStatus()
        } catch (e: ApiException) {
            responseCode = e.getCode()
        }

        assertThat(responseCode).isEqualTo(404)
    }
}
