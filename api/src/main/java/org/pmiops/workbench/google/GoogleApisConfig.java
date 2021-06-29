package org.pmiops.workbench.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import javax.inject.Provider;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleApisConfig {

  public static final String END_USER_CLOUD_BILLING = "END_USER_CLOUD_BILLING";
  public static final String SERVICE_ACCOUNT_CLOUD_BILLING = "SERVICE_ACCOUNT_CLOUD_BILLING";


}
