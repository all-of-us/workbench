package org.pmiops.workbench.billing;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import javax.inject.Provider;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleApisConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Cloudbilling googleCloudBillingApi(
      UserAuthentication userAuthentication,
      JsonFactory jsonFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider)
      throws GeneralSecurityException, IOException {
    GoogleCredential credential =
        new GoogleCredential()
            .setAccessToken(userAuthentication.getCredentials())
            .createScoped(
                Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

    return new Cloudbilling.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }
}
