package org.pmiops.workbench.notebooks;

import com.google.common.collect.ImmutableList;

import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.util.List;

@org.springframework.context.annotation.Configuration
public class NotebooksConfig {
  public static final String SERVICE_CLUSTER_API = "svcClusterApi";
  private static final String NOTEBOOKS_CLIENT = "notebooksApiClient";
  private static final String NOTEBOOKS_SERVICE_CLIENT = "notebooksSvcApiClient";

  private static final List<String> NOTEBOOK_SCOPES = ImmutableList.of(
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email");

  @Bean(name=NOTEBOOKS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient notebooksApiClient(UserAuthentication userAuthentication,
      WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    return apiClient;
  }

  @Bean(name=NOTEBOOKS_SERVICE_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient workbenchServiceAccountClient(
      WorkbenchEnvironment workbenchEnvironment, WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    try {
      apiClient.setAccessToken(
          ServiceAccounts.workbenchAccessToken(workbenchEnvironment, NOTEBOOK_SCOPES));
      apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ClusterApi clusterApi(@Qualifier(NOTEBOOKS_CLIENT) ApiClient apiClient) {
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public NotebooksApi notebooksApi(@Qualifier(NOTEBOOKS_CLIENT) ApiClient apiClient) {
    NotebooksApi api = new NotebooksApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public JupyterApi jupyterApi(@Qualifier(NOTEBOOKS_CLIENT) ApiClient apiClient) {
    JupyterApi api = new JupyterApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean(name=SERVICE_CLUSTER_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ClusterApi serviceClusterApi(@Qualifier(NOTEBOOKS_SERVICE_CLIENT) ApiClient apiClient) {
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }
}
