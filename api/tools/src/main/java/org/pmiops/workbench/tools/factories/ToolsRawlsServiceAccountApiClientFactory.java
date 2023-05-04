package org.pmiops.workbench.tools.factories;

import java.io.IOException;
import java.util.Arrays;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.rawls.ApiClient;

public class ToolsRawlsServiceAccountApiClientFactory extends ToolsRawlsApiClientFactory {

  public ToolsRawlsServiceAccountApiClientFactory(String apiUrl) throws IOException {
    super(newApiClient(apiUrl));
  }

  private static ApiClient newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(FC_SCOPES)));
    return apiClient;
  }
}
