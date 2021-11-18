package org.pmiops.workbench.iam;

import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.sam.ApiClient;
import org.pmiops.workbench.sam.api.GoogleApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class SamConfig {
  public static final String SAM_END_USER_API_CLIENT = "samEndUserApiClient";
  public static final String SAM_END_USER_GOOGLE_API = "samEndUserGoogleApi";

  @Bean(name = SAM_END_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      UserAuthentication userAuthentication, SamApiClientFactory factory) {
    ApiClient apiClient = factory.newApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    System.out.println("~~~~!!!!");
    System.out.println(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = SAM_END_USER_GOOGLE_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public GoogleApi googleApi(@Qualifier(SAM_END_USER_API_CLIENT) ApiClient apiClient) {
    GoogleApi api = new GoogleApi();
    api.setApiClient(apiClient);
    return api;
  }
}
