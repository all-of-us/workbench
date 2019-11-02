package org.pmiops.workbench.google

import com.google.common.truth.Truth.assertThat

import org.junit.Before
import org.junit.Test
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.test.Providers

class CloudStorageServiceImplIntegrationTest {
    private var service: CloudStorageServiceImpl? = null
    private val workbenchConfig = createConfig()

    @Before
    fun setUp() {
        service = CloudStorageServiceImpl(Providers.of(workbenchConfig))
    }

    @Test
    fun testCanReadFile() {
        assertThat(service!!.readInvitationKey().length > 4).isTrue()
    }

    private fun createConfig(): WorkbenchConfig {
        val config = WorkbenchConfig()
        config.googleCloudStorageService = WorkbenchConfig.GoogleCloudStorageServiceConfig()
        config.googleCloudStorageService.credentialsBucketName = "all-of-us-workbench-test-credentials"
        return config
    }
}
