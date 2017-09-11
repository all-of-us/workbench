package org.pmiops.workbench.firecloud;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class FireCloudConfig {

  private static final String END_USER_API_CLIENT = "endUserApiClient";
  private static final String ALL_OF_US_API_CLIENT = "allOfUsApiClient";

  @Bean(name=END_USER_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient fireCloudApiClient(UserAuthentication userAuthentication) {
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken(userAuthentication.getCredentials());
    return apiClient;
  }

  @Bean(name=ALL_OF_US_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient allOfUsApiClient() {
    ApiClient apiClient = new ApiClient();
    try {
      String accessToken = GoogleCredential.getApplicationDefault().getAccessToken();
      apiClient.setAccessToken(accessToken);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }


  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ProfileApi profileApi(@Qualifier(END_USER_API_CLIENT) ApiClient apiClient) {
    ProfileApi api = new ProfileApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public BillingApi billingApi(@Qualifier(ALL_OF_US_API_CLIENT) ApiClient apiClient) {
    // Billing calls are made by the AllOfUs service account, rather than using the end user's
    // credentials.
    BillingApi api = new BillingApi();
    api.setApiClient(apiClient);
    return api;
  }
}
