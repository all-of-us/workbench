package org.pmiops.workbench.google

import com.google.common.truth.Truth.assertThat

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.apache.ApacheHttpTransport
import com.google.api.services.cloudresourcemanager.model.Project
import java.io.IOException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.IntegrationTestConfig
import org.pmiops.workbench.auth.Constants
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.test.Providers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.backoff.NoBackOffPolicy
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [IntegrationTestConfig::class])
class CloudResourceManagerServiceImplIntegrationTest {
    private var service: CloudResourceManagerServiceImpl? = null
    private val httpTransport = ApacheHttpTransport()

    // This is a single hand created user in the fake-research-aou.org gsuite.
    // It has one project that has been shared with it, AoU CRM Integration Test
    // in the firecloud dev domain.
    private val CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL = "cloud-resource-manager-integration-test@fake-research-aou.org"

    // N.B. this will load the default service account credentials for whatever AoU environment
    // is set when running integration tests. This should be the test environment.
    @Autowired
    @Qualifier(Constants.DEFAULT_SERVICE_ACCOUNT_CREDS)
    private var serviceAccountCredential: GoogleCredential? = null

    @Autowired
    private val serviceAccounts: ServiceAccounts? = null

    @Autowired
    @Qualifier(Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
    private val cloudResourceManagerAdminCredential: GoogleCredential? = null

    @Before
    @Throws(IOException::class)
    fun setUp() {
        // Get a refreshed access token for the CloudResourceManager service account credentials.
        serviceAccountCredential = serviceAccountCredential!!.createScoped(CloudResourceManagerServiceImpl.SCOPES)
        serviceAccountCredential!!.refreshToken()
        service = CloudResourceManagerServiceImpl(
                Providers.of(cloudResourceManagerAdminCredential),
                httpTransport,
                GoogleRetryHandler(NoBackOffPolicy()),
                serviceAccounts)
    }

    @Test
    fun testGetAllProjectsForUser() {
        val testUser = User()
        testUser.email = CLOUD_RESOURCE_MANAGER_TEST_USER_EMAIL
        val projectList = service!!.getAllProjectsForUser(testUser)
        assertThat(projectList.size).isEqualTo(1)
    }
}
