package org.pmiops.workbench.notebooks

import com.google.common.collect.ImmutableList
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.notebooks.api.ClusterApi
import org.pmiops.workbench.notebooks.api.JupyterApi
import org.pmiops.workbench.notebooks.api.NotebooksApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@org.springframework.context.annotation.Configuration
class NotebooksConfig {

    @Bean(name = [USER_NOTEBOOKS_CLIENT])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun notebooksApiClient(
            userAuthentication: UserAuthentication, workbenchConfig: WorkbenchConfig): ApiClient {
        val apiClient = buildApiClient(workbenchConfig)
        apiClient.setAccessToken(userAuthentication.credentials)
        return apiClient
    }

    @Bean(name = [NOTEBOOKS_SERVICE_CLIENT])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun workbenchServiceAccountClient(
            workbenchEnvironment: WorkbenchEnvironment,
            workbenchConfig: WorkbenchConfig,
            serviceAccounts: ServiceAccounts): ApiClient {
        val apiClient = buildApiClient(workbenchConfig)
        try {
            apiClient.setAccessToken(
                    serviceAccounts.workbenchAccessToken(workbenchEnvironment, NOTEBOOK_SCOPES))
        } catch (e: IOException) {
            throw ServerErrorException(e)
        }

        return apiClient
    }

    @Bean(name = [USER_CLUSTER_API])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun clusterApi(@Qualifier(USER_NOTEBOOKS_CLIENT) apiClient: ApiClient): ClusterApi {
        val api = ClusterApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun notebooksApi(@Qualifier(USER_NOTEBOOKS_CLIENT) apiClient: ApiClient): NotebooksApi {
        val api = NotebooksApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun jupyterApi(@Qualifier(USER_NOTEBOOKS_CLIENT) apiClient: ApiClient): JupyterApi {
        val api = JupyterApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean(name = [SERVICE_CLUSTER_API])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun serviceClusterApi(@Qualifier(NOTEBOOKS_SERVICE_CLIENT) apiClient: ApiClient): ClusterApi {
        val api = ClusterApi()
        api.setApiClient(apiClient)
        return api
    }

    private fun buildApiClient(workbenchConfig: WorkbenchConfig): ApiClient {
        val apiClient = NotebooksApiClientTracer()
                .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
                .setDebugging(workbenchConfig.firecloud.debugEndpoints)
                .addDefaultHeader(
                        org.pmiops.workbench.firecloud.FireCloudConfig.X_APP_ID_HEADER,
                        workbenchConfig.firecloud.xAppIdValue)
        apiClient
                .getHttpClient()
                .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS)
        return apiClient
    }

    companion object {
        val USER_CLUSTER_API = "userClusterApi"
        val SERVICE_CLUSTER_API = "svcClusterApi"
        private val USER_NOTEBOOKS_CLIENT = "notebooksApiClient"
        private val NOTEBOOKS_SERVICE_CLIENT = "notebooksSvcApiClient"

        private val NOTEBOOK_SCOPES = ImmutableList.of(
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/userinfo.email")
    }
}
