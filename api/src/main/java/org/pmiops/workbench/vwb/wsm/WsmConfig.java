package org.pmiops.workbench.vwb.wsm;

import java.io.IOException;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.vwb.common.VwbApiClientUtils;
import org.pmiops.workbench.wsmanager.ApiClient;
import org.pmiops.workbench.wsmanager.api.ControlledGcpResourceApi;
import org.pmiops.workbench.wsmanager.api.ResourceApi;
import org.pmiops.workbench.wsmanager.api.WorkspaceApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class WsmConfig {

  public static final String WSM_API_CLIENT = "WSM_API_CLIENT";
  public static final String WSM_WORKSPACE_API = "WSM_WORKSPACE_CLIENT";

  public static final String WSM_SERVICE_ACCOUNT_API_CLIENT = "WSM_SERVICE_ACCOUNT_API_CLIENT";
  public static final String WSM_SERVICE_ACCOUNT_WORKSPACE_API =
      "WSM_SERVICE_ACCOUNT_WORKSPACE_API";

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ControlledGcpResourceApi controlledGcpResourceApi(
      @Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new ControlledGcpResourceApi(apiClient);
  }

  @Bean(name = WSM_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspaceApi endUserWorkspaceClient(@Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new WorkspaceApi(apiClient);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ResourceApi resourceApi(@Qualifier(WSM_API_CLIENT) ApiClient apiClient) {
    return new ResourceApi(apiClient);
  }

  @Bean(name = WSM_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      UserAuthentication userAuthentication, WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = newApiClient(workbenchConfig);
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  /**
   * Creates a WSM API client, unauthenticated. Most clients should use an authenticated, request
   * scoped bean instead of calling this directly.
   */
  private ApiClient newApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.vwb.wsmBaseUrl);
    apiClient.setReadTimeout(60 * 1000);
    apiClient.setConnectTimeout(60 * 1000);
    apiClient.setWriteTimeout(60 * 1000);
    return apiClient;
  }

  @Bean(name = WSM_SERVICE_ACCOUNT_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient serviceAccountApiClient(WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = newApiClient(workbenchConfig);
    try {
      apiClient.setAccessToken(
          ServiceAccounts.getScopedServiceAccessToken(VwbApiClientUtils.SCOPES));
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean(name = WSM_SERVICE_ACCOUNT_WORKSPACE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public WorkspaceApi serviceAccountWorkspaceApi(
      @Qualifier(WSM_SERVICE_ACCOUNT_API_CLIENT) ApiClient apiClient) {
    return new WorkspaceApi(apiClient);
  }
}
