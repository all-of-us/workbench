package org.pmiops.workbench.tools;

import java.io.IOException;
import java.util.Arrays;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.api.*;

public class ServiceAccountAPIClientFactory extends ApiClientFactory {

  public ServiceAccountAPIClientFactory(String apiUrl) {
    try {
      this.apiClient = newApiClient(apiUrl);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(
        ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(this.FC_SCOPES)));
    return apiClient;
  }
}
