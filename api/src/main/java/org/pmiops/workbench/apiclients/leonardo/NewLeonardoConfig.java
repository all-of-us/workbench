package org.pmiops.workbench.apiclients.leonardo;

import org.broadinstitute.dsde.workbench.client.leonardo.ApiClient;
import org.broadinstitute.dsde.workbench.client.leonardo.api.DisksApi;
import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class NewLeonardoConfig {

  public static final String USER_DISKS_API = "NEW userDisksApi";

  private static final String USER_LEONARDO_CLIENT = "NEW leonardoApiClient";

  @Bean(name = USER_LEONARDO_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient leoUserApiClient(
      UserAuthentication userAuthentication, NewLeonardoApiClientFactory factory) {
    ApiClient apiClient = factory.newApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = USER_DISKS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public DisksApi disksApi(@Qualifier(USER_LEONARDO_CLIENT) ApiClient apiClient) {
    DisksApi api = new DisksApi();
    api.setApiClient(apiClient);
    return api;
  }
}
