package org.pmiops.workbench.mailchimp;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.mailchimp.api.MailApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@org.springframework.context.annotation.Configuration
public class MailChimpConfig {

  private static final Logger log = Logger.getLogger(MailChimpConfig.class.getName());

  private static final String ALL_OF_US_API_CLIENT = "allOfUsApiClient";

  private static final String[] BILLING_SCOPES = new String[] {
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/cloud-billing"
  };

  @Bean(name=ALL_OF_US_API_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public ApiClient allOfUsApiClient(GoogleCredential googleCredential,
      WorkbenchConfig workbenchConfig) {
    ApiClient apiClient = new ApiClient();
    try {
      GoogleCredential credential = googleCredential.createScoped(Arrays.asList(BILLING_SCOPES));
      credential.refreshToken();
      String accessToken = credential.getAccessToken();
      apiClient.setAccessToken(accessToken);
      apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    } catch (IOException e) {
      throw new ServerErrorException(e);
    }
    return apiClient;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public MailApi mailApi(@Qualifier(ALL_OF_US_API_CLIENT) ApiClient apiClient) {
    // Billing calls are made by the AllOfUs service account, rather than using the end user's
    // credentials.
    MailApi api = new MailApi();
    api.setApiClient(apiClient);
    return api;
  }
}
