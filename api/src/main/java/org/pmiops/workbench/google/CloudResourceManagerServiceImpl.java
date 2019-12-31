package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.CloudResourceManagerScopes;
import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.auth.http.HttpCredentialsAdapter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloudResourceManagerServiceImpl implements CloudResourceManagerService {
  private static final String APPLICATION_NAME = "All of Us Researcher Workbench";

  public static final List<String> SCOPES =
      Arrays.asList(CloudResourceManagerScopes.CLOUD_PLATFORM_READ_ONLY);

  private final Provider<WorkbenchConfig> configProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;

  @Autowired
  public CloudResourceManagerServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      HttpTransport httpTransport,
      GoogleRetryHandler retryHandler) {
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
  }

  private CloudResourceManager getCloudResourceManagerServiceWithImpersonation(DbUser user)
      throws IOException {
    DelegatedUserCredentials delegatedCreds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail("cloud-resource-admin", configProvider.get()),
            user.getEmail(),
            SCOPES);

    return new CloudResourceManager.Builder(
            httpTransport, getDefaultJsonFactory(), new HttpCredentialsAdapter(delegatedCreds))
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
