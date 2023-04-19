package org.pmiops.workbench.firecloud;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.rawls.ApiClient;
import org.pmiops.workbench.rawls.api.BillingV2Api;
import org.pmiops.workbench.rawls.api.WorkspacesApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class RawlsConfig {
  public static final List<String> BILLING_SCOPES =
      ImmutableList.<String>builder()
          .addAll(FirecloudApiClientFactory.SCOPES)
          .add("https://www.googleapis.com/auth/cloud-billing")
          .build();

  // Bean names used to differentiate between an API client authenticated as the end user (via
  // UserAuthentication) and an API client authenticated as the service account user (via
  // the service account access token).
  //
  // Some groups of FireCloud APIs will use one, while some will use the other.
  //
  public static final String END_USER_API_CLIENT = "rawlsEndUserApiClient";
  public static final String END_USER_LENIENT_TIMEOUT_API_CLIENT =
      "rawlsEndUserLenientTimeoutApiClient";
  public static final String SERVICE_ACCOUNT_API_CLIENT = "rawlsServiceAccountApiClient";
  public static final String SERVICE_ACCOUNT_WORKSPACE_API = "workspaceAclsApi";
  public static final String END_USER_WORKSPACE_API = "workspacesApi";
  public static final String END_USER_LENIENT_TIMEOUT_WORKSPACE_API = "lenientTimeoutWorkspacesApi";
  public static final String SERVICE_ACCOUNT_BILLING_V2_API = "serviceAccountBillingV2Api";
  public static final String END_USER_STATIC_BILLING_V2_API = "endUserBillingV2Api";

  @Bean(name = END_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      UserAuthentication userAuthentication, FirecloudApiClientFactory factory) {
    ApiClient apiClient = factory.newRawlsApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = END_USER_LENIENT_TIMEOUT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserLenientTimeoutApiClient(
      UserAuthentication userAuthentication,
      FirecloudApiClientFactory factory,
      WorkbenchConfig config) {
    ApiClient apiClient = factory.newRawlsApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    apiClient
        .getHttpClient()
        .setReadTimeout(config.firecloud.lenientTimeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }

  @Bean(name = SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient allOfUsApiClient(FirecloudApiClientFactory factory) {
    ApiClient apiClient = factory.newRawlsApiClient();
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(BILLING_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = END_USER_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspacesApi workspacesApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = END_USER_LENIENT_TIMEOUT_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspacesApi lenientTimeoutWorkspacesApi(
      @Qualifier(END_USER_LENIENT_TIMEOUT_API_CLIENT) ApiClient apiClient) {
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

  @Bean(name = SERVICE_ACCOUNT_BILLING_V2_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public BillingV2Api serviceAccountBillingV2Api(
      @Qualifier(SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    // Billing calls are made by the AllOfUs service account, rather than using the end user's
    // credentials.
    BillingV2Api api = new BillingV2Api();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = END_USER_STATIC_BILLING_V2_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public BillingV2Api endUserBillingV2Api(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    // Billing calls are made by the user
    BillingV2Api api = new BillingV2Api();
    api.setApiClient(apiClient);
    return api;
  }
}
