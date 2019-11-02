package org.pmiops.workbench.google

import com.google.common.truth.Truth.assertThat

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.apache.ApacheHttpTransport
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.time.Clock
import org.junit.Before
import org.junit.Test
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.test.Providers
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.backoff.NoBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

class DirectoryServiceImplIntegrationTest {
    private var service: DirectoryServiceImpl? = null
    private val googleCredential = getGoogleCredential()
    private val workbenchConfig = createConfig()
    private val httpTransport = ApacheHttpTransport()

    @Before
    fun setUp() {
        service = DirectoryServiceImpl(
                Providers.of(googleCredential),
                Providers.of(workbenchConfig),
                httpTransport,
                GoogleRetryHandler(NoBackOffPolicy()))
    }

    @Test
    fun testDummyUsernameIsNotTaken() {
        assertThat(service!!.isUsernameTaken("username-that-should-not-exist")).isFalse()
    }

    @Test
    fun testDirectoryServiceUsernameIsTaken() {
        assertThat(service!!.isUsernameTaken("directory-service")).isTrue()
    }

    @Test
    fun testCreateAndDeleteTestUser() {
        val userName = String.format("integration.test.%d", Clock.systemUTC().millis())
        service!!.createUser("Integration", "Test", userName, "notasecret@gmail.com")
        assertThat(service!!.isUsernameTaken(userName)).isTrue()

        // As of ~6/25/19, customSchemas are sometimes unavailable on the initial call to Gsuite. This
        // data is likely not written with strong consistency. Retry until it is available.
        val aouMeta = retryTemplate()
                .execute<Map<String, Any>, RuntimeException> { c ->
                    val schemas = service!!.getUserByUsername(userName)!!.customSchemas
                            ?: throw RuntimeException("custom schemas is still null")
                    schemas["All_of_Us_Workbench"]
                }
        // Ensure our two custom schema fields are correctly set & re-fetched from GSuite.
        assertThat(aouMeta).containsEntry("Institution", "All of Us Research Workbench")
        assertThat(service!!.getContactEmailAddress(userName) == "notasecret@gmail.com")
        service!!.deleteUser(userName)
        assertThat(service!!.isUsernameTaken(userName)).isFalse()
    }

    private fun retryTemplate(): RetryTemplate {
        val tmpl = RetryTemplate()
        val backoff = ExponentialRandomBackOffPolicy()
        tmpl.setBackOffPolicy(backoff)
        val retry = SimpleRetryPolicy()
        retry.maxAttempts = 10
        tmpl.setRetryPolicy(retry)
        tmpl.setThrowLastExceptionOnExhausted(true)
        return tmpl
    }

    private fun getGoogleCredential(): GoogleCredential {
        try {
            val saKeyPath = "src/main/webapp/WEB-INF/gsuite-admin-sa.json"
            return GoogleCredential.fromStream(FileInputStream(File(saKeyPath)))
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    private fun createConfig(): WorkbenchConfig {
        val config = WorkbenchConfig()
        config.googleDirectoryService = WorkbenchConfig.GoogleDirectoryServiceConfig()
        config.googleDirectoryService.gSuiteDomain = "fake-research-aou.org"
        return config
    }
}
