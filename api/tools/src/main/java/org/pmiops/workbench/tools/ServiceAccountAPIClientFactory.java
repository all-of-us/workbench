package org.pmiops.workbench.tools;

import java.io.IOException;
import java.util.Arrays;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.firecloud.ApiClient;

public class ServiceAccountAPIClientFactory extends ApiClientFactory {

  public ServiceAccountAPIClientFactory(String apiUrl) throws IOException {
    super(newApiClient(apiUrl));
  }

  private static ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(
        ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(FC_SCOPES)));
    return apiClient;
  }
}
