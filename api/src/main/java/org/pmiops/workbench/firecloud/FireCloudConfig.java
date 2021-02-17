package org.pmiops.workbench.firecloud;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Duration;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.api.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
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
  public static final String WGS_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT = "wgsExtractionServiceAccountApiClient";
  public static final String SERVICE_ACCOUNT_GROUPS_API = "serviceAccountGroupsApi";
  public static final String SERVICE_ACCOUNT_WORKSPACE_API = "workspaceAclsApi";
  public static final String END_USER_WORKSPACE_API = "workspacesApi";
  public static final String SERVICE_ACCOUNT_STATIC_NOTEBOOKS_API =
      "serviceAccountStaticNotebooksApi";
  public static final String END_USER_STATIC_NOTEBOOKS_API = "endUserStaticNotebooksApi";

  public static final List<String> BILLING_SCOPES =
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
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(BILLING_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = WGS_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient wgsExtractionServiceAccountApiClient(WorkbenchConfig workbenchConfig, IamCredentialsClient iamCredentialsClient) {
    List<String> delegates = Arrays.asList("projects/-/serviceAccounts/all-of-us-workbench-test@appspot.gserviceaccount.com");
    Duration lifetime = Duration.newBuilder().setSeconds(60*60).build();
    // TODO : does this get created per request or once per service?

    String accessToken = iamCredentialsClient.generateAccessToken(
            "projects/-/serviceAccounts/wgs-cohort-extraction@all-of-us-workbench-test.iam.gserviceaccount.com",
            delegates,
            BILLING_SCOPES,
            lifetime
    ).getAccessToken();

    ApiClient apiClient = buildApiClient(workbenchConfig);
    apiClient.setAccessToken(accessToken);

    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public SubmissionsApi submissionsApi(@Qualifier(WGS_EXTRACTION_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    SubmissionsApi api = new SubmissionsApi();
    api.setApiClient(apiClient);
    return api;
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

  @Bean(name = END_USER_STATIC_NOTEBOOKS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public StaticNotebooksApi endUserStaticNotebooksApi(
      @Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    StaticNotebooksApi api = new StaticNotebooksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_ACCOUNT_STATIC_NOTEBOOKS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public StaticNotebooksApi serviceAccountStaticNotebooksApi(
      @Qualifier(SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
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

  @Bean
  @Lazy
  public IamCredentialsClient getIamCredentialsClient() throws IOException {
    return IamCredentialsClient.create();
  }
}
