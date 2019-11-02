package org.pmiops.workbench.firecloud

import com.google.common.collect.ImmutableList
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchEnvironment
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.firecloud.api.BillingApi
import org.pmiops.workbench.firecloud.api.GroupsApi
import org.pmiops.workbench.firecloud.api.NihApi
import org.pmiops.workbench.firecloud.api.ProfileApi
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi
import org.pmiops.workbench.firecloud.api.StatusApi
import org.pmiops.workbench.firecloud.api.WorkspacesApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.web.context.annotation.RequestScope

@org.springframework.context.annotation.Configuration
class FireCloudConfig {

    @Bean(name = [END_USER_API_CLIENT])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun endUserApiClient(
            userAuthentication: UserAuthentication, workbenchConfig: WorkbenchConfig): ApiClient {
        val apiClient = buildApiClient(workbenchConfig)
        apiClient.setAccessToken(userAuthentication.credentials)
        return apiClient
    }

    @Bean(name = [SERVICE_ACCOUNT_API_CLIENT])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun allOfUsApiClient(
            workbenchEnvironment: WorkbenchEnvironment,
            workbenchConfig: WorkbenchConfig,
            serviceAccounts: ServiceAccounts): ApiClient {
        val apiClient = buildApiClient(workbenchConfig)
        try {
            apiClient.setAccessToken(
                    serviceAccounts.workbenchAccessToken(workbenchEnvironment, BILLING_SCOPES))
        } catch (e: IOException) {
            throw ServerErrorException(e)
        }

        return apiClient
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun profileApi(@Qualifier(END_USER_API_CLIENT) apiClient: ApiClient): ProfileApi {
        val api = ProfileApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean(name = [END_USER_WORKSPACE_API])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun workspacesApi(@Qualifier(END_USER_API_CLIENT) apiClient: ApiClient): WorkspacesApi {
        val api = WorkspacesApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean(name = [SERVICE_ACCOUNT_WORKSPACE_API])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun workspacesApiAcls(
            @Qualifier(SERVICE_ACCOUNT_API_CLIENT) apiClient: ApiClient): WorkspacesApi {
        val api = WorkspacesApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun staticNotebooksApi(
            @Qualifier(END_USER_API_CLIENT) apiClient: ApiClient): StaticNotebooksApi {
        val api = StaticNotebooksApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun billingApi(@Qualifier(SERVICE_ACCOUNT_API_CLIENT) apiClient: ApiClient): BillingApi {
        // Billing calls are made by the AllOfUs service account, rather than using the end user's
        // credentials.
        val api = BillingApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean(name = [SERVICE_ACCOUNT_GROUPS_API])
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun groupsApi(@Qualifier(SERVICE_ACCOUNT_API_CLIENT) apiClient: ApiClient): GroupsApi {
        // Group/Auth Domain creation and addition are made by the AllOfUs service account
        val api = GroupsApi()
        api.setApiClient(apiClient)
        return api
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun nihApi(@Qualifier(END_USER_API_CLIENT) apiClient: ApiClient): NihApi {
        // When checking for NIH account information, we use the end user credentials.
        return NihApi(apiClient)
    }

    @Bean
    @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
    fun statusApi(workbenchConfig: WorkbenchConfig): StatusApi {
        val statusApi = StatusApi()
        statusApi.setApiClient(buildApiClient(workbenchConfig))
        return statusApi
    }

    companion object {

        val X_APP_ID_HEADER = "X-App-ID"

        // Bean names used to differentiate between an API client authenticated as the end user (via
        // UserAuthentication) and an API client authenticated as the service account user (via
        // the service account access token).
        //
        // Some groups of FireCloud APIs will use one, while some will use the other.
        //
        val END_USER_API_CLIENT = "endUserApiClient"
        val SERVICE_ACCOUNT_API_CLIENT = "serviceAccountApiClient"
        val SERVICE_ACCOUNT_GROUPS_API = "serviceAccountGroupsApi"
        val SERVICE_ACCOUNT_WORKSPACE_API = "workspaceAclsApi"
        val END_USER_WORKSPACE_API = "workspacesApi"

        private val BILLING_SCOPES = ImmutableList.of(
                "https://www.googleapis.com/auth/userinfo.profile",
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/cloud-billing")

        fun buildApiClient(workbenchConfig: WorkbenchConfig): ApiClient {
            val apiClient = FirecloudApiClientTracer()
            apiClient.setBasePath(workbenchConfig.firecloud.baseUrl)
            apiClient.addDefaultHeader(X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue)
            apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints)
            apiClient
                    .getHttpClient()
                    .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS)
            return apiClient
        }
    }
}
