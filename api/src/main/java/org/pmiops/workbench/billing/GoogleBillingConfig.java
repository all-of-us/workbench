package org.pmiops.workbench.billing;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.pmiops.workbench.auth.UserAuthentication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleBillingConfig {

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Cloudbilling googleCloudBillingApi(UserAuthentication userAuthentication) {
    GoogleCredential credential = new GoogleCredential().setAccessToken(userAuthentication.getCredentials());
    // The createScopedRequired method returns true when running on GAE or a local developer
    // machine. In that case, the desired scopes must be passed in manually. When the code is
    // running in GCE, GKE or a Managed VM, the scopes are pulled from the GCE metadata server.
    // See https://developers.google.com/identity/protocols/application-default-credentials for more information.
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
    }

    Cloudbilling cloudBillingService = null;
    try {
      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      cloudBillingService = new Cloudbilling.Builder(httpTransport, jsonFactory,
          credential)
          .setApplicationName("Google Cloud Platform Sample")
          .build();
    } catch (GeneralSecurityException | IOException e) {
      
    }

    return cloudBillingService;
  }
}
