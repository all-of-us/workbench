package org.pmiops.workbench.apiclients.leonardo;

import javax.inject.Provider;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiClient;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class NewLeonardoApiClientFactory {
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public NewLeonardoApiClientFactory(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Creates a Leonardo API client, unauthenticated. Most clients should use an authenticated,
   * request scoped bean instead of calling this directly.
   */
  public ApiClient newApiClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    final ApiClient apiClient =
        new ApiClient()
            .setBasePath(workbenchConfig.firecloud.leoBaseUrl)
            .setDebugging(workbenchConfig.firecloud.debugEndpoints)
            .addDefaultHeader(
                FirecloudApiClientFactory.X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient.setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds * 1000);
    return apiClient;
  }
}
