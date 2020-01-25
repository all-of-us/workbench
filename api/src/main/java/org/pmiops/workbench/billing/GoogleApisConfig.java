package org.pmiops.workbench.billing;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import javax.inject.Provider;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleApisConfig {

  public static final String USER_PROXY_CLOUD_BILLING = "USER_PROXY_CLOUD_BILLING";
  public static final String SERVICE_ACCOUNT_CLOUD_BILLING = "SERVICE_ACCOUNT_CLOUD_BILLING";

  @Bean(USER_PROXY_CLOUD_BILLING)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Cloudbilling userProxyGoogleCloudbillingApi(
      UserAuthentication userAuthentication,
      JsonFactory jsonFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    return createCloudbillingClient(userAuthentication.getCredentials(), jsonFactory, workbenchConfigProvider.get());
  }

  @Bean(SERVICE_ACCOUNT_CLOUD_BILLING)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Cloudbilling serviceAccountGoogleCloudbillingApi(
      JsonFactory jsonFactory, Provider<WorkbenchConfig> workbenchConfigProvider) {
    String accessToken = null;
    try {
      accessToken = ServiceAccounts.getScopedServiceAccessToken(
          Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    } catch (IOException e) {
      throw new RuntimeException("Could not create service account access token for cloud billing");
    }

    return createCloudbillingClient(accessToken, jsonFactory, workbenchConfigProvider.get());
  }

  private Cloudbilling createCloudbillingClient(String accessToken, JsonFactory jsonFactory, WorkbenchConfig workbenchConfig) {
    try {
      GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
      return new Cloudbilling.Builder(
          GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
          .setApplicationName(workbenchConfig.server.projectId)
          .build();
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException("Could not construct Cloudbilling API client");
    }
  }
}
