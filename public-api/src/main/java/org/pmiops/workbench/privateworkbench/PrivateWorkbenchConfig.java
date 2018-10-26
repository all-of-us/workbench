package org.pmiops.workbench.privateworkbench;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.privateworkbench.api.ProfileApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletRequest;

@org.springframework.context.annotation.Configuration
public class PrivateWorkbenchConfig {

  private static final String END_USER_API_CLIENT = "endUserApiClient";

  @Bean(name=END_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient workbenchApiClient(HttpServletRequest request,
                                      WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.server.apiBaseUrl);
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    apiClient.setAccessToken(
        request.getHeader(HttpHeaders.AUTHORIZATION)
            .substring("Bearer".length()).trim());
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProfileApi profileApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }
}
