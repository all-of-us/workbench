package org.pmiops.workbench.firecloud;

import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class FireCloudConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient fireCloudApiClient(UserAuthentication userAuthentication) {
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProfileApi profileApi(ApiClient apiClient) {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }
}
