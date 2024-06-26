package org.pmiops.workbench.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManagerScopes;
import com.google.api.services.iam.v1.Iam;
import com.google.api.services.iam.v1.IamScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import jakarta.inject.Provider;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class GoogleConfig {

  public static final String END_USER_CLOUD_BILLING = "END_USER_CLOUD_BILLING";
  public static final String SERVICE_ACCOUNT_CLOUD_BILLING = "SERVICE_ACCOUNT_CLOUD_BILLING";
  public static final String SERVICE_ACCOUNT_CLOUD_IAM = "SERVICE_ACCOUNT_CLOUD_IAM";
  public static final String SERVICE_ACCOUNT_CLOUD_RESOURCE_MANAGER =
      "SERVICE_ACCOUNT_CLOUD_RESOURCE_MANAGER";

  @Bean
  @Lazy
  public IamCredentialsClient getIamCredentialsClient() throws IOException {
    return IamCredentialsClient.create();
  }

  @Bean
  public MetricServiceClient getMetricServiceClient() throws IOException {
    return MetricServiceClient.create();
  }

  @Bean(END_USER_CLOUD_BILLING)
  @RequestScope
  public Cloudbilling endUserCloudbilling(
      UserAuthentication userAuthentication,
      JsonFactory jsonFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider)
      throws GeneralSecurityException, IOException {
    GoogleCredentials credentials =
        new GoogleCredentials(new AccessToken(userAuthentication.getCredentials(), null));

    return new Cloudbilling.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            new HttpCredentialsAdapter(credentials))
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }

  @Bean(SERVICE_ACCOUNT_CLOUD_BILLING)
  @RequestScope
  public Cloudbilling serviceAccountGoogleCloudbilling(
      JsonFactory jsonFactory, Provider<WorkbenchConfig> workbenchConfigProvider)
      throws IOException, GeneralSecurityException {
    GoogleCredentials credentials =
        ServiceAccounts.getScopedServiceCredentials(
            Collections.singletonList("https://www.googleapis.com/auth/cloud-billing"));

    return new Cloudbilling.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            new HttpCredentialsAdapter(credentials))
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }

  @Bean(SERVICE_ACCOUNT_CLOUD_IAM)
  @RequestScope
  public Iam serviceAccountGoogleCloudIam(
      JsonFactory jsonFactory, Provider<WorkbenchConfig> workbenchConfigProvider)
      throws IOException, GeneralSecurityException {
    GoogleCredentials credentials =
        ServiceAccounts.getScopedServiceCredentials(new ArrayList<>(IamScopes.all()));

    return new Iam.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            new HttpCredentialsAdapter(credentials))
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }

  @Bean(SERVICE_ACCOUNT_CLOUD_RESOURCE_MANAGER)
  @RequestScope
  public CloudResourceManager serviceAccountGoogleCloudResourceManager(
      JsonFactory jsonFactory, Provider<WorkbenchConfig> workbenchConfigProvider)
      throws IOException, GeneralSecurityException {
    GoogleCredentials credentials =
        ServiceAccounts.getScopedServiceCredentials(
            new ArrayList<>(CloudResourceManagerScopes.all()));

    return new CloudResourceManager.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            new HttpCredentialsAdapter(credentials))
        .setApplicationName(workbenchConfigProvider.get().server.projectId)
        .build();
  }
}
