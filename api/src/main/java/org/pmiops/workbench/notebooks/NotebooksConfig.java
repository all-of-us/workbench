package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.leonardo.api.ClusterApi;
import org.pmiops.workbench.leonardo.api.ServiceInfoApi;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class NotebooksConfig {
  public static final String USER_CLUSTER_API = "userClusterApi";
  public static final String SERVICE_CLUSTER_API = "svcClusterApi";

  // Identifiers for the Swagger2 APIs for Jupyter and Welder, used for creating/localizing files.
  private static final String USER_NOTEBOOKS_CLIENT = "notebooksApiClient";
  private static final String SERVICE_NOTEBOOKS_CLIENT = "notebooksSvcApiClient";
  // Identifiers for the new OAS3 APIs from Leonardo. These should be used for cluster access.
  private static final String USER_LEONARDO_CLIENT = "leonardoApiClient";
  private static final String SERVICE_LEONARDO_CLIENT = "leonardoServiceAPiClient";

  private static final List<String> NOTEBOOK_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/userinfo.profile",
          "https://www.googleapis.com/auth/userinfo.email");

  @Bean(name = USER_NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient notebooksApiClient(
      UserAuthentication userAuthentication, WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = buildNotebooksApiClient(workbenchConfig);
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = SERVICE_LEONARDO_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.leonardo.ApiClient leoServiceApiClient(
      WorkbenchConfig workbenchConfig) {
    org.pmiops.workbench.leonardo.ApiClient apiClient = buildLeoApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(NOTEBOOK_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = USER_LEONARDO_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public org.pmiops.workbench.leonardo.ApiClient leoUserApiClient(
      UserAuthentication userAuthentication, WorkbenchConfig workbenchConfig) {
    org.pmiops.workbench.leonardo.ApiClient apiClient = buildLeoApiClient(workbenchConfig);
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = SERVICE_NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient workbenchServiceAccountClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = buildNotebooksApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(NOTEBOOK_SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = USER_CLUSTER_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ClusterApi clusterApi(
      @Qualifier(USER_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public NotebooksApi notebooksApi(@Qualifier(USER_NOTEBOOKS_CLIENT) ApiClient apiClient) {
    NotebooksApi api = new NotebooksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public JupyterApi jupyterApi(@Qualifier(USER_NOTEBOOKS_CLIENT) ApiClient apiClient) {
    JupyterApi api = new JupyterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name = SERVICE_CLUSTER_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ClusterApi serviceClusterApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ServiceInfoApi serviceInfoApi(
      @Qualifier(SERVICE_LEONARDO_CLIENT) org.pmiops.workbench.leonardo.ApiClient apiClient) {
    ServiceInfoApi api = new ServiceInfoApi();
    api.setApiClient(apiClient);
    return api;
  }

  private ApiClient buildNotebooksApiClient(WorkbenchConfig workbenchConfig) {
    final ApiClient apiClient =
        new ApiClient()
            .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
            .setDebugging(workbenchConfig.firecloud.debugEndpoints)
            .addDefaultHeader(
                org.pmiops.workbench.firecloud.FireCloudConfig.X_APP_ID_HEADER,
                workbenchConfig.firecloud.xAppIdValue);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }

  private org.pmiops.workbench.leonardo.ApiClient buildLeoApiClient(
      WorkbenchConfig workbenchConfig) {
    final org.pmiops.workbench.leonardo.ApiClient apiClient =
        new org.pmiops.workbench.leonardo.ApiClient()
            .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
            .setDebugging(workbenchConfig.firecloud.debugEndpoints)
            .addDefaultHeader(
                org.pmiops.workbench.firecloud.FireCloudConfig.X_APP_ID_HEADER,
                workbenchConfig.firecloud.xAppIdValue);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }
}
