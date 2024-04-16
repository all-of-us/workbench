package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;
import static org.pmiops.workbench.google.GoogleConfig.SERVICE_ACCOUNT_CLOUD_RESOURCE_MANAGER;

import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager.Builder;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManagerScopes;
import com.google.api.services.cloudresourcemanager.v3.model.GetIamPolicyRequest;
import com.google.api.services.cloudresourcemanager.v3.model.Policy;
import com.google.api.services.cloudresourcemanager.v3.model.Project;
import com.google.api.services.cloudresourcemanager.v3.model.SearchProjectsResponse;
import com.google.api.services.cloudresourcemanager.v3.model.SetIamPolicyRequest;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import jakarta.inject.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
      Collections.singletonList(CloudResourceManagerScopes.CLOUD_PLATFORM);

  private final Provider<WorkbenchConfig> configProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;
  private final IamCredentialsClient iamCredentialsClient;
  private final Provider<CloudResourceManager> serviceCloudResouceManager;

  @Autowired
  public CloudResourceManagerServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      HttpTransport httpTransport,
      GoogleRetryHandler retryHandler,
      IamCredentialsClient iamCredentialsClient,
      @Qualifier(SERVICE_ACCOUNT_CLOUD_RESOURCE_MANAGER)
          Provider<CloudResourceManager> serviceCloudResouceManager) {
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
    this.iamCredentialsClient = iamCredentialsClient;
    this.serviceCloudResouceManager = serviceCloudResouceManager;
  }

  private CloudResourceManager getCloudResourceManagerServiceWithImpersonation(DbUser user)
      throws IOException {
    final OAuth2Credentials delegatedCreds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail(
                ADMIN_SERVICE_ACCOUNT_NAME, configProvider.get().server.projectId),
            user.getUsername(),
            SCOPES,
            iamCredentialsClient,
            httpTransport);
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
            SearchProjectsResponse resp =
                getCloudResourceManagerServiceWithImpersonation(user)
                    .projects()
                    .search()
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

  @Override
  public Policy getIamPolicy(String googleProject) {
    return retryHandler.run(
        (context) -> {
          return serviceCloudResouceManager
              .get()
              .projects()
              .getIamPolicy("projects/" + googleProject, new GetIamPolicyRequest())
              .execute();
        });
  }

  @Override
  public Policy setIamPolicy(String googleProject, Policy policy) {
    return retryHandler.run(
        (context) -> {
          return serviceCloudResouceManager
              .get()
              .projects()
              .setIamPolicy(
                  "projects/" + googleProject, new SetIamPolicyRequest().setPolicy(policy))
              .execute();
        });
  }
}
