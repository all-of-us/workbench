package org.pmiops.workbench.sam;

import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class SamConfig {

  private static final String SAM_API_CLIENT = "VWB_SAM_API_CLIENT";
  public static final String SAM_USERS_CLIENT = "VWB_SAM_USERS_CLIENT";

  @Bean(name = SAM_USERS_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public UsersApi endUserUsersClient(@Qualifier(SAM_API_CLIENT) ApiClient apiClient) {
    return new UsersApi(apiClient);
  }

  @Bean(name = SAM_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(UserAuthentication userAuthentication) {
    ApiClient apiClient = newApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  private ApiClient newApiClient() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath("https://terra-devel-sam.api.verily.com");
    return apiClient;
  }
}
