package org.pmiops.workbench

import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.mockito.Mock
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient
import org.pmiops.workbench.notebooks.LeonardoNotebooksClientImpl
import org.pmiops.workbench.notebooks.NotebooksRetryHandler
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.api.NotebooksApi
import org.pmiops.workbench.test.Providers
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.retry.backoff.NoBackOffPolicy

/** Created by brubenst on 5/8/18.  */
class NotebooksIntegrationTest {
    /*
   * Mocked service providers are (currently) not available for functional integration tests.
   * Integration tests with real projects/clusters against production Notebooks is not
   * recommended at this time.
   */

    @Mock
    private val clusterApi: ClusterApi? = null
    @Mock
    private val notebooksApi: NotebooksApi? = null
    @Mock
    private val workspaceService: WorkspaceService? = null

    private val leonardoNotebooksClient = LeonardoNotebooksClientImpl(
            Providers.of<ClusterApi>(clusterApi),
            Providers.of<NotebooksApi>(notebooksApi),
            Providers.of(createConfig()),
            Providers.of<User>(null),
            NotebooksRetryHandler(NoBackOffPolicy()),
            workspaceService)

    @Test
    fun testStatus() {
        assertThat(leonardoNotebooksClient.notebooksStatus).isTrue()
    }

    private fun createConfig(): WorkbenchConfig {
        val config = WorkbenchConfig()
        config.firecloud = WorkbenchConfig.FireCloudConfig()
        config.firecloud.debugEndpoints = true
        return config
    }
}
