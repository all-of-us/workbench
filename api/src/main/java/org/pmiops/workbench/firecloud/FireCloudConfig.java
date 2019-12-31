package org.pmiops.workbench.firecloud;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class FireCloudConfig {

  public static final String X_APP_ID_HEADER = "X-App-ID";

  // Bean names used to differentiate between an API client authenticated as the end user (via
  // UserAuthentication) and an API client authenticated as the service account user (via
  // the service account access token).
  //
  // Some groups of FireCloud APIs will use one, while some will use the other.
  //
  public static final String END_USER_API_CLIENT = "endUserApiClient";
  public static final String SERVICE_ACCOUNT_API_CLIENT = "serviceAccountApiClient";
  public static final String SERVICE_ACCOUNT_GROUPS_API = "serviceAccountGroupsApi";
  public static final String SERVICE_ACCOUNT_WORKSPACE_API = "workspaceAclsApi";
  public static final String END_USER_WORKSPACE_API = "workspacesApi";

  private static final List<String> BILLING_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/cloud-billing");

  @Bean(name = END_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      UserAuthentication userAuthentication, WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = buildApiClient(workbenchConfig);
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient allOfUsApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = buildApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(ServiceAccounts.workbenchAccessToken(BILLING_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProfileApi profileApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = END_USER_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspacesApi workspacesApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_ACCOUNT_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspacesApi workspacesApiAcls(
      @Qualifier(SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public StaticNotebooksApi staticNotebooksApi(
      @Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    StaticNotebooksApi api = new StaticNotebooksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public BillingApi billingApi(@Qualifier(SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    // Billing calls are made by the AllOfUs service account, rather than using the end user's
    // credentials.
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_ACCOUNT_GROUPS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public GroupsApi groupsApi(@Qualifier(SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    // Group/Auth Domain creation and addition are made by the AllOfUs service account
    GroupsApi api = new GroupsApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public NihApi nihApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    // When checking for NIH account information, we use the end user credentials.
    return new NihApi(apiClient);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public StatusApi statusApi(WorkbenchConfig workbenchConfig) {
    StatusApi statusApi = new StatusApi();
    statusApi.setApiClient(buildApiClient(workbenchConfig));
    return statusApi;
  }

  public static ApiClient buildApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.firecloud.baseUrl);
    apiClient.addDefaultHeader(X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }
}
