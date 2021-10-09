package org.pmiops.workbench.notebooks;

import com.google.auth.oauth2.OAuth2Credentials;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.leonardo.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LeonardoApiClientFactory {
  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public LeonardoApiClientFactory(
      FirecloudApiClientFactory firecloudApiClientFactory,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public ApiClient newImpersonatedApiClient(String userEmail) throws IOException {
    OAuth2Credentials delegatedCreds =
        firecloudApiClientFactory.getDelegatedUserCredentials(userEmail);
    ApiClient client = newApiClient();
    client.setAccessToken(delegatedCreds.getAccessToken().getTokenValue());
    return client;
  }

  public ApiClient newApiClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    final ApiClient apiClient =
        new ApiClient()
            .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
            .setDebugging(workbenchConfig.firecloud.debugEndpoints)
            .addDefaultHeader(
                FirecloudApiClientFactory.X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }

  public org.pmiops.workbench.notebooks.ApiClient newNotebooksClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    final org.pmiops.workbench.notebooks.ApiClient apiClient =
        new org.pmiops.workbench.notebooks.ApiClient()
            .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
            .setDebugging(workbenchConfig.firecloud.debugEndpoints)
            .addDefaultHeader(
                FirecloudApiClientFactory.X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }
}
