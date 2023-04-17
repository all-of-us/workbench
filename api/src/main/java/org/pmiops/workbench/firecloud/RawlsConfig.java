package org.pmiops.workbench.firecloud;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Duration;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.rawls.api.MethodconfigsApi;
import org.pmiops.workbench.rawls.api.StatusApi;
import org.pmiops.workbench.rawls.ApiClient;
import org.pmiops.workbench.rawls.api.SubmissionsApi;
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
  public static final String END_USER_LENIENT_TIMEOUT_API_CLIENT = "rawlsEndUserLenientTimeoutApiClient";
  public static final String SERVICE_ACCOUNT_API_CLIENT = "rawlsServiceAccountApiClient";
  public static final String WGS_COHORT_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT =
      "rawlsWgsCohortExtractionServiceAccountApiClient";
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

  public static final String WGS_EXTRACTION_SA_CREDENTIALS = "WGS_EXTRACTION_SA_CREDENTIALS";

  @Bean(name = WGS_EXTRACTION_SA_CREDENTIALS)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public GoogleCredentials wgsExtractionAccessToken(
      WorkbenchConfig workbenchConfig, IamCredentialsClient iamCredentialsClient) {
    return GoogleCredentials.create(
        new AccessToken(
            iamCredentialsClient
                .generateAccessToken(
                    "projects/-/serviceAccounts/"
                        + workbenchConfig.wgsCohortExtraction.serviceAccount,
                    Collections.emptyList(),
                    ImmutableList.<String>builder()
                        .addAll(FirecloudApiClientFactory.SCOPES)
                        .add("https://www.googleapis.com/auth/devstorage.read_write")
                        .build(),
                    Duration.newBuilder().setSeconds(60 * 10).build())
                .getAccessToken(),
            null));
  }

  @Bean(name = WGS_COHORT_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient wgsExtractionServiceAccountApiClient(
      FirecloudApiClientFactory factory,
      @Qualifier(WGS_EXTRACTION_SA_CREDENTIALS) GoogleCredentials credentials) {
    ApiClient apiClient = factory.newRawlsApiClient();
    apiClient.setAccessToken(credentials.getAccessToken().getTokenValue());
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public SubmissionsApi submissionsApi(
      @Qualifier(WGS_COHORT_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    SubmissionsApi api = new SubmissionsApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public MethodconfigsApi methodConfigurationsApi(
      @Qualifier(WGS_COHORT_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    MethodconfigsApi api = new MethodconfigsApi();
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

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public StatusApi statusApi(FirecloudApiClientFactory factory) {
    StatusApi statusApi = new StatusApi();
    statusApi.setApiClient(factory.newRawlsApiClient());
    return statusApi;
  }
}
