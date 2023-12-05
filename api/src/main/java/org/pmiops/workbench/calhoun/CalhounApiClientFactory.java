package org.pmiops.workbench.calhoun;

import java.util.concurrent.TimeUnit;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalhounApiClientFactory {
  public static final String X_APP_ID_HEADER = "X-App-ID";
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public CalhounApiClientFactory(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Creates a Calhoun API client, unauthenticated. Most clients should use an authenticated,
   * request scoped bean instead of calling this directly.
   */
  public ApiClient newCalhounApiClient() {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(workbenchConfig.firecloud.calhounBaseUrl);
    apiClient.addDefaultHeader(X_APP_ID_HEADER, workbenchConfig.firecloud.xAppIdValue);
    apiClient.setDebugging(workbenchConfig.firecloud.debugEndpoints);
    apiClient
        .getHttpClient()
        .setReadTimeout(workbenchConfig.firecloud.timeoutInSeconds, TimeUnit.SECONDS);
    return apiClient;
  }
}
