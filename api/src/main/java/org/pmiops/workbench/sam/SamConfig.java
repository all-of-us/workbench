package org.pmiops.workbench.sam;

import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class SamConfig {
  public static final String END_USER_CLIENT = "samEndUserApiClient";
  public static final String TOS_API = "samTermsOfServiceApi";

  @Bean(name = END_USER_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient endUserApiClient(
      UserAuthentication userAuthentication, SamApiClientFactory factory) {
    ApiClient apiClient = factory.newApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name = TOS_API)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public TermsOfServiceApi termsOfServiceApi(@Qualifier(END_USER_CLIENT) ApiClient apiClient) {
    TermsOfServiceApi api = new TermsOfServiceApi();
    api.setApiClient(apiClient);
    return api;
  }
}
