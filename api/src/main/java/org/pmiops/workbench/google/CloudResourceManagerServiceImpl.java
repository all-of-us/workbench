package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManager.Builder;
import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes;
import com.google.api.services.cloudresourcemanager.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.inject.Provider;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CloudResourceManagerServiceImpl implements CloudResourceManagerService {
  private static final String APPLICATION_NAME = "All of Us Researcher Workbench";

  public static final String ADMIN_SERVICE_ACCOUNT_NAME = "cloud-resource-admin";

  public static final List<String> SCOPES =
      Arrays.asList(CloudResourceManagerScopes.CLOUD_PLATFORM_READ_ONLY);

  private final Provider<ServiceAccountCredentials> credentialsProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;
  private final IamCredentialsClient iamCredentialsClient;

  @Autowired
  public CloudResourceManagerServiceImpl(
      @Qualifier(Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
          Provider<ServiceAccountCredentials> credentialsProvider,
      Provider<WorkbenchConfig> configProvider,
      HttpTransport httpTransport,
      GoogleRetryHandler retryHandler,
      IamCredentialsClient iamCredentialsClient) {
    this.credentialsProvider = credentialsProvider;
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
    this.iamCredentialsClient = iamCredentialsClient;
  }

  private CloudResourceManager getCloudResourceManagerServiceWithImpersonation(DbUser user)
      throws IOException {
    final OAuth2Credentials delegatedCreds;
    if (configProvider.get().featureFlags.useKeylessDelegatedCredentials) {
      delegatedCreds =
          new DelegatedUserCredentials(
              ServiceAccounts.getServiceAccountEmail(
                  ADMIN_SERVICE_ACCOUNT_NAME, configProvider.get().server.projectId),
              user.getUsername(),
              SCOPES,
              iamCredentialsClient,
              httpTransport);
    } else {
      delegatedCreds =
          credentialsProvider.get().createScoped(SCOPES).createDelegated(user.getUsername());
    }
    delegatedCreds.refreshIfExpired();

    return new Builder(
            httpTransport, getDefaultJsonFactory(), new HttpCredentialsAdapter(delegatedCreds))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  @Override
  public List<Project> getAllProjectsForUser(DbUser user) throws IOException {
    return retryHandler.runAndThrowChecked(
        (context) -> {
          List<Project> projects = new ArrayList<>();
          Optional<String> pageToken = Optional.empty();
          do {
            ListProjectsResponse resp =
                getCloudResourceManagerServiceWithImpersonation(user)
                    .projects()
                    .list()
                    .setPageToken(pageToken.orElse(null))
                    .execute();
            if (resp.getProjects() != null) {
              projects.addAll(resp.getProjects());
            }

            // The API does not specify null or empty string; treat both as empty to be safe.
            pageToken =
                Optional.ofNullable(resp.getNextPageToken())
                    .filter(((Predicate<String>) String::isEmpty).negate());
          } while (pageToken.isPresent());
          return projects;
        });
  }
}
