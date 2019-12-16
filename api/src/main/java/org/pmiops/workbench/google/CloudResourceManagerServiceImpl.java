package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes;
import com.google.api.services.cloudresourcemanager.model.Project;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CloudResourceManagerServiceImpl implements CloudResourceManagerService {
  private static final String APPLICATION_NAME = "All of Us Researcher Workbench";

  public static final List<String> SCOPES =
      Arrays.asList(CloudResourceManagerScopes.CLOUD_PLATFORM_READ_ONLY);

  private final Provider<GoogleCredential> cloudResourceManagerAdminCredsProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;
  private final ServiceAccounts serviceAccounts;

  @Autowired
  public CloudResourceManagerServiceImpl(
      @Qualifier(Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
          Provider<GoogleCredential> cloudResourceManagerAdminCredsProvider,
      HttpTransport httpTransport,
      GoogleRetryHandler retryHandler,
      ServiceAccounts serviceAccounts) {
    this.serviceAccounts = serviceAccounts;
    this.cloudResourceManagerAdminCredsProvider = cloudResourceManagerAdminCredsProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
  }

  private CloudResourceManager getCloudResourceManagerServiceWithImpersonation(DbUser user)
      throws IOException {
    // Load credentials for the cloud-resource-manager Service Account. This account has been
    // granted
    // domain-wide delegation for the OAuth scopes required by cloud apis.
    GoogleCredential googleCredential = cloudResourceManagerAdminCredsProvider.get();

    googleCredential =
        serviceAccounts.getImpersonatedCredential(googleCredential, user.getUsername(), SCOPES);

    return new CloudResourceManager.Builder(
            httpTransport, getDefaultJsonFactory(), googleCredential)
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  @Override
  public List<Project> getAllProjectsForUser(DbUser user) {
    try {
      return retryHandler.runAndThrowChecked(
          (context) ->
              getCloudResourceManagerServiceWithImpersonation(user)
                  .projects()
                  .list()
                  .execute()
                  .getProjects());
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }
}
